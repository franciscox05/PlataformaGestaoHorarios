package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.DayOffRepository;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.PermutaRepository;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DayOffService {

    private final DayOffRepository dayOffRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final UtilizadorRepository utilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final PermutaRepository permutaRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    public DayOffService(DayOffRepository dayOffRepository,
                     LojautilizadorHelper lojautilizadorHelper,
                     UtilizadorRepository utilizadorRepository,
                     HorarioRepository horarioRepository,
                     PermutaRepository permutaRepository,
                     HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository) {
        this.dayOffRepository = dayOffRepository;
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.utilizadorRepository = utilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.permutaRepository = permutaRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
    }

    @Transactional
    public DayOff registarPedidoFolga(DayOff pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("O pedido de folga nao pode ser nulo.");
        }

        if (pedido.getIdUtilizador().getId() == null) {
            throw new IllegalArgumentException("O utilizador do pedido e obrigatorio.");
        }

        if (pedido.getDataAusencia() == null) {
            throw new IllegalArgumentException("A data de ausencia e obrigatoria.");
        }

        if (pedido.getDataAusencia().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de ausencia nao pode estar no passado.");
        }

        if (pedido.getTipo() == null || pedido.getTipo().isBlank()) {
            throw new IllegalArgumentException("O tipo de ausencia e obrigatorio.");
        }

        // Folgas no mês atual não são permitidas — o horário já está gerado.
        // Tipo "baixa" (baixa médica) é sempre permitido; os restantes tipos de folga
        // só podem ser pedidos para meses futuros (usa Permuta para ausências no mês corrente).
        if (!"baixa".equalsIgnoreCase(pedido.getTipo())) {
            java.time.YearMonth mesAtual = java.time.YearMonth.now();
            if (java.time.YearMonth.from(pedido.getDataAusencia()).equals(mesAtual)) {
                throw new IllegalArgumentException(
                        "Pedidos de folga no mes atual nao sao permitidos — o horario ja esta gerado. "
                        + "Se precisas faltar a um turno deste mes, pede uma Permuta.");
            }
        }

        if (pedido.getMotivo() != null && pedido.getMotivo().isBlank()) {
            pedido.setMotivo(null);
        }

        // Ausências urgentes (emergências) são aceites sem antecedência mínima
        if (!"urgente".equalsIgnoreCase(pedido.getTipo())) {
            validarAntecedenciaMinimaDoTurno(pedido.getIdUtilizador().getId(), pedido.getDataAusencia());
        }

        pedido.setEstado("pendente");

        return dayOffRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<DayOff> listarPedidosPorUtilizador(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O id do utilizador e obrigatorio.");
        }

        return dayOffRepository.findByIdUtilizadorId(idUtilizador).stream()
                .sorted(Comparator
                        .comparing(DayOff::getDataAusencia, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DayOff::getIdDayoff, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAprovarFolgas(Integer idUtilizador) {
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.APROVACAO);
    }

    @Transactional(readOnly = true)
    public List<DayOff> listarPedidosPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, LojautilizadorHelper.APROVACAO,
                "Este utilizador nao tem permissao para aprovar folgas.");

        return dayOffRepository.findPedidosPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador) {
        return lojautilizadorHelper.findLigacaoAtivaComCargo(idUtilizador, LojautilizadorHelper.APROVACAO)
                .map(lu -> (int) dayOffRepository.countPedidosPendentesDaLoja(
                        lu.getIdLoja().getId(), idUtilizador))
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> listarNomesUtilizadores(Collection<Integer> idsUtilizadores) {
        if (idsUtilizadores == null || idsUtilizadores.isEmpty()) {
            return Map.of();
        }

        return utilizadorRepository.findAllById(idsUtilizadores).stream()
                .collect(Collectors.toMap(Utilizador::getId, Utilizador::getNome, (nome1, nome2) -> nome1));
    }

    @Transactional
    public void cancelarPedidoProprio(Integer idDayOff, Integer idUtilizador) {
        if (idDayOff == null) {
            throw new IllegalArgumentException("O pedido selecionado e obrigatorio.");
        }
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador e obrigatorio.");
        }

        DayOff pedido = dayOffRepository.findById(idDayOff)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de folga nao encontrado."));

        if (!idUtilizador.equals(pedido.getIdUtilizador().getId())) {
            throw new IllegalArgumentException("Nao podes cancelar um pedido que nao e teu.");
        }

        if (!"pendente".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("So e possivel cancelar pedidos pendentes.");
        }

        pedido.setEstado("cancelado");
        dayOffRepository.save(pedido);
    }

    @Transactional
    public DayOff aprovarPedidoFolga(Integer idDayOff, Integer idUtilizadorAprovador) {
        return atualizarEstadoPedido(idDayOff, idUtilizadorAprovador, "aprovado");
    }

    @Transactional
    public DayOff rejeitarPedidoFolga(Integer idDayOff, Integer idUtilizadorAprovador) {
        return atualizarEstadoPedido(idDayOff, idUtilizadorAprovador, "rejeitado");
    }

    private DayOff atualizarEstadoPedido(Integer idDayOff, Integer idUtilizadorAprovador, String novoEstado) {
        if (idDayOff == null) {
            throw new IllegalArgumentException("O pedido selecionado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, LojautilizadorHelper.APROVACAO,
                "Este utilizador nao tem permissao para aprovar folgas.");

        DayOff pedido = dayOffRepository.findById(idDayOff)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de folga nao encontrado."));

        if (!"pendente".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("Este pedido ja foi tratado.");
        }

        boolean pedidoVisivelAoAprovador = dayOffRepository.findPedidosPendentesDaLoja(
                        ligacaoAtiva.getIdLoja().getId(),
                        idUtilizadorAprovador)
                .stream()
                .anyMatch(dayOff -> dayOff.getIdDayoff().equals(idDayOff));

        if (!pedidoVisivelAoAprovador) {
            throw new IllegalArgumentException("Nao tens permissao para gerir este pedido.");
        }

        pedido.setEstado(novoEstado);
        DayOff pedidoAtualizado = dayOffRepository.save(pedido);

        if ("aprovado".equalsIgnoreCase(novoEstado)) {
            retirarTurnosDoColaboradorNoDia(pedidoAtualizado, ligacaoAtiva);
        }

        return pedidoAtualizado;
    }

    private void validarAntecedenciaMinimaDoTurno(Integer idUtilizador, LocalDate dataAusencia) {
        List<Horario> turnosDoDia = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idUtilizador,
                dataAusencia,
                dataAusencia
        );

        if (turnosDoDia.isEmpty()) {
            return;
        }

        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(24);
        boolean existeTurnoComMenosDe24Horas = turnosDoDia.stream()
                .filter(horario -> horario.getIdTurno() != null && horario.getIdTurno().getHoraInicio() != null)
                .map(horario -> LocalDateTime.of(horario.getDataTurno(), horario.getIdTurno().getHoraInicio()))
                .anyMatch(inicioTurno -> inicioTurno.isBefore(limiteMinimo));

        if (existeTurnoComMenosDe24Horas) {
            throw new IllegalArgumentException("Os pedidos de folga precisam de ser feitos com pelo menos 24 horas de antecedencia em relacao ao turno.");
        }
    }

    private void retirarTurnosDoColaboradorNoDia(DayOff pedido, Lojautilizador ligacaoAprovador) {
        List<Horario> horariosAfetados = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                pedido.getIdUtilizador().getId(),
                pedido.getDataAusencia(),
                pedido.getDataAusencia()
        ).stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdLoja() != null)
                .filter(horario -> ligacaoAprovador.getIdLoja().getId().equals(horario.getIdLojautilizador().getIdLoja().getId()))
                .toList();

        if (horariosAfetados.isEmpty()) {
            return;
        }

        Set<Integer> idsHorariosAfetados = horariosAfetados.stream()
                .map(Horario::getId)
                .collect(Collectors.toSet());

        var permutasConflitantes = permutaRepository.findPedidosPendentesConflitantes(
                -1,
                idsHorariosAfetados
        );
        permutasConflitantes.forEach(permuta -> permuta.setEstado(com.example.projeto2.API.Enums.EstadoPermuta.rejeitado));
        permutaRepository.saveAll(permutasConflitantes);

        horariosAfetados.forEach(horario -> horario.setEstado(com.example.projeto2.API.Enums.EstadoHorario.recusado));
        horarioRepository.saveAll(horariosAfetados);

        List<HistoricoHorarioEstado> historicos = horariosAfetados.stream()
                .map(horario -> {
                    HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
                    historico.setIdHorario(horario);
                    historico.setEstadoNovo("recusado");
                    historico.setDataRegisto(Instant.now());
                    historico.setObservacoes("Turno removido automaticamente apos aprovacao de folga.");
                    return historico;
                })
                .toList();
        historicoHorarioEstadoRepository.saveAll(historicos);
    }

    /**
     * Resultado enriquecido da aprovação de uma folga, com aviso de cobertura.
     * Usado quando o gestor aprova uma ausência e precisa de saber o impacto na equipa.
     */
    public record ResultadoAprovacaoFolga(
            DayOff pedido,
            boolean temAvisoCobertura,
            String avisoCobertura,
            int trabalhadoresRestantesNoTurno
    ) {}

    /**
     * Aprova um pedido de folga e devolve o resultado com informação de impacto de cobertura.
     * Útil para ausências urgentes em que o gestor precisa de agir rapidamente.
     */
    @Transactional
    public ResultadoAprovacaoFolga aprovarPedidoFolgaComCobertura(Integer idDayOff, Integer idUtilizadorAprovador) {
        DayOff pedidoAprovado = aprovarPedidoFolga(idDayOff, idUtilizadorAprovador);

        // Calcular cobertura restante no turno do colaborador nesse dia
        int trabalhadoresRestantes = 0;
        String aviso = null;
        try {
            List<Horario> turnosDoDia = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                    pedidoAprovado.getIdUtilizador().getId(),
                    pedidoAprovado.getDataAusencia(),
                    pedidoAprovado.getDataAusencia()
            );
            Lojautilizador ligacao = lojautilizadorHelper.obterLigacaoAtiva(idUtilizadorAprovador);
            List<com.example.projeto2.API.Modules.Horario> todosNoDia = horarioRepository.findHorariosDaLojaNoDia(
                    ligacao.getIdLoja().getId(),
                    pedidoAprovado.getDataAusencia()
            );
            Integer idAusente = pedidoAprovado.getIdUtilizador().getId();
            trabalhadoresRestantes = (int) todosNoDia.stream()
                    .filter(h -> h.getIdLojautilizador() != null
                            && h.getIdLojautilizador().getIdUtilizador() != null
                            && !idAusente.equals(h.getIdLojautilizador().getIdUtilizador().getId()))
                    .count();

            if (trabalhadoresRestantes == 0) {
                aviso = "⚠️ ATENÇÃO: Não ficam trabalhadores escalados para " + pedidoAprovado.getDataAusencia() + ". Reatribui um turno urgentemente.";
            } else if (trabalhadoresRestantes == 1) {
                aviso = "⚠️ Apenas 1 trabalhador fica escalado em " + pedidoAprovado.getDataAusencia() + ". Considera reforçar a equipa.";
            }
        } catch (Exception e) {
            // Falha na análise de cobertura não deve impedir a aprovação
            aviso = "Não foi possível calcular o impacto de cobertura.";
        }

        return new ResultadoAprovacaoFolga(
                pedidoAprovado,
                aviso != null,
                aviso != null ? aviso : "Cobertura mantida (" + trabalhadoresRestantes + " trabalhador(es) escalado(s)).",
                trabalhadoresRestantes
        );
    }
}
