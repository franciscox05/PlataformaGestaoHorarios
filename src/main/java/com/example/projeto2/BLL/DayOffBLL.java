package com.example.projeto2.BLL;

import com.example.projeto2.Modules.HistoricoHorarioEstado;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.PermutaRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
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
public class DayOffBLL {

    private static final Set<String> CARGOS_COM_APROVACAO = Set.of("gerente", "subgerente", "supervisor");

    private final DayOffRepository dayOffRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final PermutaRepository permutaRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    public DayOffBLL(DayOffRepository dayOffRepository,
                     LojautilizadorRepository lojautilizadorRepository,
                     UtilizadorRepository utilizadorRepository,
                     HorarioRepository horarioRepository,
                     PermutaRepository permutaRepository,
                     HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository) {
        this.dayOffRepository = dayOffRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
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
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getTipo() != null && CARGOS_COM_APROVACAO.contains(cargo.getTipo().toLowerCase()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DayOff> listarPedidosPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

        return dayOffRepository.findPedidosPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .filter(lu -> lu.getIdCargo() != null
                        && lu.getIdCargo().getTipo() != null
                        && CARGOS_COM_APROVACAO.contains(lu.getIdCargo().getTipo().toLowerCase()))
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

        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

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
        permutasConflitantes.forEach(permuta -> permuta.setEstado(com.example.projeto2.Enums.EstadoPermuta.rejeitado));
        permutaRepository.saveAll(permutasConflitantes);

        horariosAfetados.forEach(horario -> horario.setEstado(com.example.projeto2.Enums.EstadoHorario.recusado));
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

    private Lojautilizador obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador."));
    }

    private void validarPermissaoDeAprovacao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        if (tipoCargo == null || !CARGOS_COM_APROVACAO.contains(tipoCargo.toLowerCase())) {
            throw new IllegalArgumentException("Este utilizador nao tem permissao para aprovar folgas.");
        }
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
            // Contar trabalhadores restantes na loja nesse dia (após a ausência)
            Lojautilizador ligacao = obterLigacaoAtiva(idUtilizadorAprovador);
            List<com.example.projeto2.Modules.Horario> todosNoDia = horarioRepository.findHorariosDaLojaNoDia(
                    ligacao.getIdLoja().getId(),
                    pedidoAprovado.getDataAusencia()
            );
            // Excluir o colaborador ausente
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
