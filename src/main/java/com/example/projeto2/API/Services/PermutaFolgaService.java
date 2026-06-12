package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.PermutaFolgaRepository;
import com.example.projeto2.API.Repositories.PermutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PermutaFolgaService {

    private static final int DESCANSO_MINIMO_HORAS = 11;

    private final PermutaFolgaRepository permutaFolgaRepository;
    private final HorarioRepository horarioRepository;
    private final PermutaRepository permutaRepository;
    private final LojautilizadorHelper lojautilizadorHelper;

    public PermutaFolgaService(PermutaFolgaRepository permutaFolgaRepository,
                               HorarioRepository horarioRepository,
                               PermutaRepository permutaRepository,
                               LojautilizadorHelper lojautilizadorHelper) {
        this.permutaFolgaRepository = permutaFolgaRepository;
        this.horarioRepository      = horarioRepository;
        this.permutaRepository      = permutaRepository;
        this.lojautilizadorHelper   = lojautilizadorHelper;
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
        return horarioRepository.findTurnosElegiveisParaPermutaFolga(idFunc1, idHorarioD);
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

        // Regra dos 11 h de descanso após a troca. Os dois horários permutados são
        // ignorados nos vizinhos: depois da troca deixam de pertencer ao colaborador.
        Set<Integer> idsPermutados = Set.of(horarioD.getId(), horarioY.getId());
        validarDescanso(idDonoY, horarioD.getIdTurno(), diaD, idsPermutados); // Func2 passa a trabalhar no dia D
        validarDescanso(idFunc1, horarioY.getIdTurno(), diaY, idsPermutados); // Func1 passa a trabalhar no dia Y

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

        validarDescanso(idFunc2, horarioD.getIdTurno(), diaD, idsPermutados);
        validarDescanso(idFunc1, horarioY.getIdTurno(), diaY, idsPermutados);
    }

    private boolean temTurnoNoDia(Integer idColaborador, LocalDate dia, Set<Integer> idsHorariosAIgnorar) {
        return horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idColaborador, dia, dia).stream()
                .anyMatch(h -> !idsHorariosAIgnorar.contains(h.getId()));
    }

    private void validarDescanso(Integer idColaborador,
                                 com.example.projeto2.API.Modules.Turno turnoNovo,
                                 LocalDate data,
                                 Set<Integer> idsHorariosAIgnorar) {
        if (turnoNovo == null || turnoNovo.getHoraInicio() == null || turnoNovo.getHoraFim() == null) return;

        for (Horario h : horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.minusDays(1), data.minusDays(1))) {
            if (idsHorariosAIgnorar.contains(h.getId())) continue;
            if (h.getIdTurno() == null || h.getIdTurno().getHoraFim() == null) continue;
            long gap = Duration.between(
                    LocalDateTime.of(data.minusDays(1), h.getIdTurno().getHoraFim()),
                    LocalDateTime.of(data, turnoNovo.getHoraInicio())
            ).toHours();
            if (gap < DESCANSO_MINIMO_HORAS) {
                throw new IllegalArgumentException(
                        "Esta permuta viola o descanso minimo de " + DESCANSO_MINIMO_HORAS
                        + "h entre turnos consecutivos (gap: " + gap + "h).");
            }
        }

        for (Horario h : horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.plusDays(1), data.plusDays(1))) {
            if (idsHorariosAIgnorar.contains(h.getId())) continue;
            if (h.getIdTurno() == null || h.getIdTurno().getHoraInicio() == null) continue;
            long gap = Duration.between(
                    LocalDateTime.of(data, turnoNovo.getHoraFim()),
                    LocalDateTime.of(data.plusDays(1), h.getIdTurno().getHoraInicio())
            ).toHours();
            if (gap < DESCANSO_MINIMO_HORAS) {
                throw new IllegalArgumentException(
                        "Esta permuta viola o descanso minimo de " + DESCANSO_MINIMO_HORAS
                        + "h entre turnos consecutivos (gap: " + gap + "h).");
            }
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
