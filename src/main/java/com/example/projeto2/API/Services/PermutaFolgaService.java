package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.PermutaFolgaRepository;
import com.example.projeto2.API.Repositories.PermutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Service
public class PermutaFolgaService {

    private static final int DESCANSO_MINIMO_HORAS = 11;

    private final PermutaFolgaRepository permutaFolgaRepository;
    private final HorarioRepository horarioRepository;
    private final PermutaRepository permutaRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final HorarioValidatorService horarioValidatorService;

    public PermutaFolgaService(PermutaFolgaRepository permutaFolgaRepository,
                               HorarioRepository horarioRepository,
                               PermutaRepository permutaRepository,
                               LojautilizadorHelper lojautilizadorHelper,
                               HorarioValidatorService horarioValidatorService) {
        this.permutaFolgaRepository  = permutaFolgaRepository;
        this.horarioRepository       = horarioRepository;
        this.permutaRepository       = permutaRepository;
        this.lojautilizadorHelper    = lojautilizadorHelper;
        this.horarioValidatorService = horarioValidatorService;
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Horario> listarTurnosParaCederFolga(Integer idFunc1) {
        // Reutiliza a mesma lista de turnos disponíveis para permuta normal,
        // excluindo também turnos com permuta_folga pendente.
        return horarioRepository.findTurnosDisponiveisParaPermutaPorUtilizador(idFunc1)
                .stream()
                .filter(h -> !permutaFolgaRepository.existsPendentePorHorario(h.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Horario> listarTurnosElegiveisCompensacao(Integer idFunc1, Integer idHorarioD) {
        // A query base já restringe a: mesma loja do turno a ceder, colegas com turno ativo,
        // dias em que o solicitante está de folga, turnos futuros e sem permutas pendentes.
        List<Horario> elegiveis = horarioRepository.findTurnosElegiveisParaPermutaFolga(idFunc1, idHorarioD);

        // Regra das 24h de antecedência (timeline): a query base usa dataTurno >= CURRENT_DATE,
        // que ainda INCLUI hoje. Um turno de compensação a decorrer hoje nunca cumpre as 24h,
        // por isso exigimos estritamente datas FUTURAS (> hoje) — mantém o dropdown 100% exato.
        LocalDate hoje = LocalDate.now();
        elegiveis = elegiveis.stream()
                .filter(h -> h.getDataTurno() != null && h.getDataTurno().isAfter(hoje))
                .toList();

        // Reforço da regra de mesmo mês/ano (RN2): o dropdown só pode oferecer compensações
        // no mesmo mês do turno a ceder, mantendo a coerência com a validação de submissão.
        Horario horarioD = horarioRepository.findById(idHorarioD).orElse(null);
        if (horarioD == null || horarioD.getDataTurno() == null) {
            return elegiveis;
        }
        YearMonth mesD = YearMonth.from(horarioD.getDataTurno());
        return elegiveis.stream()
                .filter(h -> YearMonth.from(h.getDataTurno()).equals(mesD))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermutaFolga> listarPedidosPorUtilizador(Integer idUtilizador) {
        return permutaFolgaRepository.findPedidosEnviadosPorUtilizador(idUtilizador);
    }

    @Transactional(readOnly = true)
    public List<PermutaFolga> listarPendentesParaAprovacao(Integer idAprovador) {
        Lojautilizador lu = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idAprovador, LojautilizadorHelper.APROVACAO,
                "Este utilizador nao tem permissao para aprovar permutas de folga.");
        return permutaFolgaRepository.findPedidosPendentesDaLoja(lu.getIdLoja().getId(), idAprovador);
    }

    @Transactional(readOnly = true)
    public boolean podeAprovar(Integer idUtilizador) {
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.APROVACAO);
    }

    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador) {
        return lojautilizadorHelper.findLigacaoAtivaComCargo(idUtilizador, LojautilizadorHelper.APROVACAO)
                .map(lu -> (int) permutaFolgaRepository.countPedidosPendentesDaLoja(
                        lu.getIdLoja().getId(), idUtilizador))
                .orElse(0);
    }

    // ── Registo ──────────────────────────────────────────────────────────────

    @Transactional
    public PermutaFolga registarPedido(Integer idFunc1, Horario horarioD, Horario horarioY) {
        validar(idFunc1, horarioD, horarioY);

        PermutaFolga pf = new PermutaFolga();
        pf.setIdHorarioD(horarioD);
        pf.setIdHorarioY(horarioY);
        pf.setEstado("pendente");
        pf.setDataPedido(Instant.now());
        return permutaFolgaRepository.save(pf);
    }

    // ── Aprovação ────────────────────────────────────────────────────────────

    @Transactional
    public PermutaFolga aprovar(Integer idPermutaFolga, Integer idAprovador) {
        PermutaFolga pf = obterPendente(idPermutaFolga, idAprovador);

        Horario horarioD = pf.getIdHorarioD();
        Horario horarioY = pf.getIdHorarioY();

        // Revalidar com o estado atual do horário — entre o pedido e a aprovação
        // pode ter mudado (outra permuta aprovada, edição manual): cada lado tem
        // de continuar de folga no dia que vai receber, e o descanso de 11h tem
        // de continuar a cumprir-se.
        revalidarEstadoAtual(horarioD, horarioY);

        // Guardar lojautilizadors antes da troca
        Lojautilizador luFunc1 = horarioD.getIdLojautilizador();
        Lojautilizador luFunc2 = horarioY.getIdLojautilizador();

        // Efetuar troca
        horarioD.setIdLojautilizador(luFunc2);
        horarioY.setIdLojautilizador(luFunc1);
        horarioRepository.save(horarioD);
        horarioRepository.save(horarioY);

        pf.setEstado("aprovado");
        permutaFolgaRepository.save(pf);

        // Rejeitar permutas normais e permutas_folga pendentes que usem os mesmos horários
        List<Integer> ids = List.of(horarioD.getId(), horarioY.getId());
        permutaRepository.findPedidosPendentesConflitantes(Integer.MAX_VALUE, ids)
                .forEach(p -> {
                    p.setEstado(com.example.projeto2.API.Enums.EstadoPermuta.rejeitado);
                    permutaRepository.save(p);
                });
        permutaFolgaRepository.findPendentesConflitantes(pf.getId(), ids)
                .forEach(c -> {
                    c.setEstado("rejeitado");
                    permutaFolgaRepository.save(c);
                });

        return pf;
    }

    @Transactional
    public PermutaFolga rejeitar(Integer idPermutaFolga, Integer idAprovador) {
        PermutaFolga pf = obterPendente(idPermutaFolga, idAprovador);
        pf.setEstado("rejeitado");
        return permutaFolgaRepository.save(pf);
    }

    @Transactional
    public void cancelar(Integer idPermutaFolga, Integer idSolicitante) {
        PermutaFolga pf = permutaFolgaRepository.findDetalhadaById(idPermutaFolga)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de permuta de folga nao encontrado."));

        if (!"pendente".equalsIgnoreCase(pf.getEstado())) {
            throw new IllegalArgumentException("Só pedidos pendentes podem ser cancelados.");
        }
        Integer idDono = pf.getIdHorarioD().getIdLojautilizador().getIdUtilizador().getId();
        if (!idDono.equals(idSolicitante)) {
            throw new IllegalArgumentException("Nao podes cancelar um pedido que nao e teu.");
        }
        pf.setEstado("cancelado");
        permutaFolgaRepository.save(pf);
    }

    // ── Validação ────────────────────────────────────────────────────────────

    private void validar(Integer idFunc1, Horario horarioD, Horario horarioY) {
        if (idFunc1 == null || horarioD == null || horarioY == null) {
            throw new IllegalArgumentException("Dados insuficientes para registar o pedido.");
        }
        if (horarioD.getId().equals(horarioY.getId())) {
            throw new IllegalArgumentException("Os dois horarios selecionados nao podem ser o mesmo.");
        }

        Integer idDonoD = horarioD.getIdLojautilizador().getIdUtilizador().getId();
        Integer idDonoY = horarioY.getIdLojautilizador().getIdUtilizador().getId();

        if (!idFunc1.equals(idDonoD)) {
            throw new IllegalArgumentException("O turno a ceder tem de ser teu.");
        }
        if (idFunc1.equals(idDonoY)) {
            throw new IllegalArgumentException("O turno de compensacao tem de pertencer a outro colaborador.");
        }

        Integer idLojaD = horarioD.getIdLojautilizador().getIdLoja().getId();
        Integer idLojaY = horarioY.getIdLojautilizador().getIdLoja().getId();
        if (!idLojaD.equals(idLojaY)) {
            throw new IllegalArgumentException("Ambos os turnos devem ser da mesma loja.");
        }

        LocalDate diaD = horarioD.getDataTurno();
        LocalDate diaY = horarioY.getDataTurno();
        if (diaD == null || diaY == null || diaD.equals(diaY)) {
            throw new IllegalArgumentException("Os dois turnos nao podem ser no mesmo dia.");
        }

        // RN2 — Mesmo mês e ano: preserva o cálculo de horas contratuais mensais.
        if (!YearMonth.from(diaD).equals(YearMonth.from(diaY))) {
            throw new IllegalArgumentException(
                    "Ambos os turnos devem pertencer ao mesmo mes e ano para preservar o calculo de horas contratuais.");
        }

        // Func2 não pode ter turno aprovado no dia D (deve ter folga)
        if (temTurnoNoDia(idDonoY, diaD, Set.of())) {
            throw new IllegalArgumentException(
                    "O colega selecionado nao tem folga no dia " + diaD + " — nao e possivel fazer esta permuta.");
        }

        // Func1 não pode ter turno aprovado no dia Y (deve ter folga)
        if (temTurnoNoDia(idFunc1, diaY, Set.of())) {
            throw new IllegalArgumentException(
                    "Nao tens folga no dia " + diaY + " — nao e possivel usar esse dia como compensacao.");
        }

        // Antecedência mínima de 24 h para cada turno
        LocalDateTime limite = LocalDateTime.now().plusHours(24);
        if (inicioDoTurno(horarioD).isBefore(limite)) {
            throw new IllegalArgumentException(
                    "O turno a ceder precisa de ter pelo menos 24 horas de antecedencia.");
        }
        if (inicioDoTurno(horarioY).isBefore(limite)) {
            throw new IllegalArgumentException(
                    "O turno de compensacao precisa de ter pelo menos 24 horas de antecedencia.");
        }

        // RN3 — Guarda de pré-validação SIMULADA: constrói conceptualmente o estado
        // pós-troca (Func2 assume o turno do dia D, Func1 assume o turno do dia Y) e
        // corre-o pelas guardas do sistema — sobreposição (Allen, inclui entre lojas)
        // e descanso mínimo de 11h via HorarioValidatorService. Falha = exceção limpa.
        simularEValidarTroca(idFunc1, horarioD, horarioY);

        // Sem permutas pendentes nos mesmos horários
        if (permutaRepository.existsPedidoPendentePorHorario(horarioD.getId())
                || permutaRepository.existsPedidoPendentePorHorario(horarioY.getId())) {
            throw new IllegalArgumentException(
                    "Um dos turnos selecionados esta envolvido num pedido de permuta pendente.");
        }
        if (permutaFolgaRepository.existsPendentePorHorario(horarioD.getId())
                || permutaFolgaRepository.existsPendentePorHorario(horarioY.getId())) {
            throw new IllegalArgumentException(
                    "Um dos turnos selecionados ja esta envolvido noutra permuta de folga pendente.");
        }
    }

    /**
     * Revalidação na aprovação: o estado do horário pode ter mudado desde o pedido
     * (outra permuta aprovada entretanto, edição manual da escala). Confirma que
     * cada colaborador continua de folga no dia que vai receber — sem isto, a
     * aprovação podia deixá-lo com dois turnos no mesmo dia — e que o descanso
     * mínimo de 11h continua a cumprir-se com os vizinhos atuais.
     */
    private void revalidarEstadoAtual(Horario horarioD, Horario horarioY) {
        Integer idFunc1 = horarioD.getIdLojautilizador().getIdUtilizador().getId();
        Integer idFunc2 = horarioY.getIdLojautilizador().getIdUtilizador().getId();
        LocalDate diaD = horarioD.getDataTurno();
        LocalDate diaY = horarioY.getDataTurno();
        Set<Integer> idsPermutados = Set.of(horarioD.getId(), horarioY.getId());

        if (temTurnoNoDia(idFunc2, diaD, idsPermutados)) {
            throw new IllegalArgumentException(
                    "O colega ja nao tem folga no dia " + diaD
                            + " — o horario mudou desde que o pedido foi feito.");
        }
        if (temTurnoNoDia(idFunc1, diaY, idsPermutados)) {
            throw new IllegalArgumentException(
                    "O solicitante ja nao tem folga no dia " + diaY
                            + " — o horario mudou desde que o pedido foi feito.");
        }

        validarDescanso(idFunc2, horarioD.getIdTurno(), diaD, idsPermutados, nomeColaborador(horarioY));
        validarDescanso(idFunc1, horarioY.getIdTurno(), diaY, idsPermutados, nomeColaborador(horarioD));
    }

    private boolean temTurnoNoDia(Integer idColaborador, LocalDate dia, Set<Integer> idsHorariosAIgnorar) {
        return horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idColaborador, dia, dia).stream()
                .anyMatch(h -> !idsHorariosAIgnorar.contains(h.getId()));
    }

    /**
     * Guarda de pré-validação simulada (RN3). Modela o estado pós-troca — Func2 assume o
     * turno do dia D e Func1 assume o turno do dia Y — e corre cada colaborador pelas
     * guardas do sistema, sem persistir nada:
     *   • Sobreposição de turnos (Allen), incluindo turnos noutras lojas.
     *   • Descanso mínimo de 11h entre dias adjacentes (via HorarioValidatorService).
     * Lança IllegalArgumentException com texto limpo identificando o colaborador afetado.
     */
    private void simularEValidarTroca(Integer idFunc1, Horario horarioD, Horario horarioY) {
        Integer idFunc2 = horarioY.getIdLojautilizador().getIdUtilizador().getId();
        String nomeFunc1 = nomeColaborador(horarioD);
        String nomeFunc2 = nomeColaborador(horarioY);
        LocalDate diaD = horarioD.getDataTurno();
        LocalDate diaY = horarioY.getDataTurno();
        // Os dois horários permutados deixam de pertencer aos donos originais após a troca,
        // por isso são ignorados ao avaliar os vizinhos de cada colaborador.
        Set<Integer> idsPermutados = Set.of(horarioD.getId(), horarioY.getId());

        // Func2 passa a trabalhar o turno do dia D; Func1 passa a trabalhar o turno do dia Y.
        validarSemSobreposicao(idFunc2, horarioD.getIdTurno(), diaD, nomeFunc2);
        validarSemSobreposicao(idFunc1, horarioY.getIdTurno(), diaY, nomeFunc1);

        validarDescanso(idFunc2, horarioD.getIdTurno(), diaD, idsPermutados, nomeFunc2);
        validarDescanso(idFunc1, horarioY.getIdTurno(), diaY, idsPermutados, nomeFunc1);
    }

    /**
     * Guarda de sobreposição (Allen's Interval Algebra) sobre o estado simulado: confirma que
     * o colaborador não fica com dois turnos a sobreporem-se no mesmo dia — mesmo entre lojas,
     * porque countGlobalOverlappingShifts não filtra por loja.
     */
    private void validarSemSobreposicao(Integer idColaborador, Turno turno, LocalDate dia, String nome) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) return;
        long sobreposicoes = horarioRepository.countGlobalOverlappingShifts(
                idColaborador, dia, turno.getHoraInicio(), turno.getHoraFim());
        if (sobreposicoes > 0) {
            throw new IllegalArgumentException(
                    "Esta troca criaria uma sobreposicao de turnos para " + nome
                            + " no dia " + dia + " (incluindo turnos noutras lojas).");
        }
    }

    /**
     * Descanso mínimo de 11h entre o turno assumido e os turnos dos dias adjacentes,
     * delegando o cálculo das horas de descanso ao HorarioValidatorService (RFS06).
     */
    private void validarDescanso(Integer idColaborador, Turno turnoNovo, LocalDate data,
                                 Set<Integer> idsHorariosAIgnorar, String nomeColaborador) {
        if (turnoNovo == null || turnoNovo.getHoraInicio() == null || turnoNovo.getHoraFim() == null) return;

        // Véspera (data-1) → turno novo
        for (Horario h : horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.minusDays(1), data.minusDays(1))) {
            if (idsHorariosAIgnorar.contains(h.getId()) || h.getIdTurno() == null) continue;
            if (!horarioValidatorService.respeitaDescansoMinimo(
                    data.minusDays(1), h.getIdTurno(), data, turnoNovo, DESCANSO_MINIMO_HORAS)) {
                throw descansoViolado(nomeColaborador);
            }
        }

        // Turno novo → dia seguinte (data+1)
        for (Horario h : horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.plusDays(1), data.plusDays(1))) {
            if (idsHorariosAIgnorar.contains(h.getId()) || h.getIdTurno() == null) continue;
            if (!horarioValidatorService.respeitaDescansoMinimo(
                    data, turnoNovo, data.plusDays(1), h.getIdTurno(), DESCANSO_MINIMO_HORAS)) {
                throw descansoViolado(nomeColaborador);
            }
        }
    }

    private IllegalArgumentException descansoViolado(String nome) {
        return new IllegalArgumentException(
                "Esta troca viola o descanso minimo de " + DESCANSO_MINIMO_HORAS
                        + "h entre turnos consecutivos para " + nome + ".");
    }

    private String nomeColaborador(Horario h) {
        try {
            String nome = h.getIdLojautilizador().getIdUtilizador().getNome();
            return (nome != null && !nome.isBlank()) ? nome : "o colaborador";
        } catch (Exception e) {
            return "o colaborador";
        }
    }

    private LocalDateTime inicioDoTurno(Horario h) {
        if (h.getDataTurno() == null || h.getIdTurno() == null
                || h.getIdTurno().getHoraInicio() == null) {
            throw new IllegalArgumentException("Nao foi possivel determinar a hora de inicio do turno.");
        }
        return LocalDateTime.of(h.getDataTurno(), h.getIdTurno().getHoraInicio());
    }

    private PermutaFolga obterPendente(Integer idPermutaFolga, Integer idAprovador) {
        if (idPermutaFolga == null) {
            throw new IllegalArgumentException("Pedido de permuta de folga obrigatorio.");
        }
        Lojautilizador lu = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idAprovador, LojautilizadorHelper.APROVACAO,
                "Este utilizador nao tem permissao para gerir permutas de folga.");

        PermutaFolga pf = permutaFolgaRepository.findDetalhadaById(idPermutaFolga)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de permuta de folga nao encontrado."));

        if (!"pendente".equalsIgnoreCase(pf.getEstado())) {
            throw new IllegalArgumentException("Este pedido ja foi tratado.");
        }

        Integer idLojaPedido = pf.getIdHorarioD().getIdLojautilizador().getIdLoja().getId();
        if (!idLojaPedido.equals(lu.getIdLoja().getId())) {
            throw new IllegalArgumentException("Nao tens permissao para gerir este pedido.");
        }
        return pf;
    }
}
