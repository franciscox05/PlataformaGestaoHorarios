package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Fase de <b>refinamento por pesquisa local</b> do planeamento mensal.
 *
 * <p>O motor constrói o horário dia-a-dia (greedy + backtracking), o que é rápido mas
 * míope: decisões boas no início do mês podem forçar más decisões mais à frente
 * (folgas preferidas não honradas, carga desequilibrada). Esta fase corre <em>depois</em>
 * da construção e tenta melhorar o plano por movimentos 1-para-1 — reatribuir um turno
 * de um colaborador a outro no mesmo dia e turno — sem nunca violar regras hard:
 *
 * <ol>
 *   <li><b>Honrar folgas preferidas (soft):</b> para cada turno atribuído num dia de
 *       folga preferida do colaborador, procura outro colaborador que possa assumi-lo.</li>
 *   <li><b>Equilibrar carga contratual:</b> enquanto a diferença de utilização entre o
 *       colaborador mais e menos carregado exceder o alvo, move turnos do mais carregado
 *       para os menos carregados.</li>
 * </ol>
 *
 * <p>Cada movimento candidato é validado por <em>replay estrito</em> da agenda completa
 * do recetor (histórico + atribuições do mês + o novo turno, por ordem cronológica)
 * contra todas as regras hard via {@link EstadoColaborador#podeReceber}. Movimentos que
 * removeriam a única chefia de um dia de fim de semana são rejeitados. Como cada
 * movimento mantém o mesmo dia e o mesmo turno, a cobertura mínima por slot nunca é
 * afetada. Remover um turno a um colaborador nunca invalida a sua agenda (todas as
 * regras hard são limites superiores), pelo que só o recetor precisa de validação.
 */
public final class RefinadorPlaneamento {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefinadorPlaneamento.class);

    /** Máximo de movimentos na fase de equilíbrio de carga (guarda anti-loop). */
    private static final int MAX_MOVIMENTOS_EQUILIBRIO = 60;

    /** Diferença de utilização (0..1) entre extremos a partir da qual vale a pena equilibrar. */
    private static final double GAP_UTILIZACAO_ALVO = 0.10;

    private final HorarioValidatorService validator;

    public RefinadorPlaneamento(HorarioValidatorService validator) {
        this.validator = validator;
    }

    /**
     * Refina o plano gerado. Devolve a mesma lista (mutada) com eventuais turnos
     * reatribuídos; em caso de erro inesperado, devolve o plano original intacto.
     */
    public List<Horario> refinar(PedidoGeracao pedido, List<Horario> horarios) {
        if (pedido == null || horarios == null || horarios.isEmpty()) {
            return horarios;
        }
        try {
            int folgasRecuperadas = honrarFolgasPreferidas(pedido, horarios);
            int movimentosEquilibrio = equilibrarCarga(pedido, horarios);
            if (folgasRecuperadas > 0 || movimentosEquilibrio > 0) {
                LOGGER.info("Refinamento do planeamento: {} folga(s) preferida(s) recuperada(s), "
                                + "{} movimento(s) de equilíbrio de carga.",
                        folgasRecuperadas, movimentosEquilibrio);
            }
            return horarios;
        } catch (Exception e) {
            LOGGER.warn("Refinamento do planeamento falhou — mantém-se o plano original.", e);
            return horarios;
        }
    }

    // =========================================================================
    // Fase 1 — honrar folgas preferidas
    // =========================================================================

    private int honrarFolgasPreferidas(PedidoGeracao pedido, List<Horario> plano) {
        Map<Integer, Set<LocalDate>> folgasPreferidas = pedido.folgasPreferidasPorColaborador();
        if (folgasPreferidas == null || folgasPreferidas.isEmpty()) {
            return 0;
        }

        int recuperadas = 0;
        for (Horario h : List.copyOf(plano)) {
            if (prazoEsgotado(pedido)) {
                break;
            }
            Integer idAtual = idColaborador(h);
            if (idAtual == null || h.getDataTurno() == null || h.getIdTurno() == null) {
                continue;
            }
            if (!folgasPreferidas.getOrDefault(idAtual, Set.of()).contains(h.getDataTurno())) {
                continue;
            }
            if (tentarReatribuir(pedido, plano, h, true)) {
                recuperadas++;
            }
        }
        return recuperadas;
    }

    // =========================================================================
    // Fase 2 — equilíbrio de carga contratual
    // =========================================================================

    private int equilibrarCarga(PedidoGeracao pedido, List<Horario> plano) {
        int movimentos = 0;
        for (int i = 0; i < MAX_MOVIMENTOS_EQUILIBRIO; i++) {
            if (prazoEsgotado(pedido)) {
                break;
            }
            Map<Integer, Long> minutos = minutosAtribuidos(plano);
            Integer doador = null;
            double utilizacaoDoador = -1;
            double utilizacaoMinima = Double.MAX_VALUE;

            for (Lojautilizador ligacao : pedido.colaboradores()) {
                Integer id = idDe(ligacao);
                long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
                if (id == null || carga <= 0) {
                    continue;
                }
                double utilizacao = minutos.getOrDefault(id, 0L) / (double) carga;
                if (utilizacao > utilizacaoDoador) {
                    utilizacaoDoador = utilizacao;
                    doador = id;
                }
                utilizacaoMinima = Math.min(utilizacaoMinima, utilizacao);
            }

            if (doador == null || (utilizacaoDoador - utilizacaoMinima) <= GAP_UTILIZACAO_ALVO) {
                break;
            }

            Horario movido = tentarMoverUmTurnoDe(pedido, plano, doador, utilizacaoDoador, minutos);
            if (movido == null) {
                break;
            }
            movimentos++;
        }
        return movimentos;
    }

    /**
     * Tenta mover um turno do {@code doador} para um colaborador menos carregado.
     * Devolve o {@link Horario} movido, ou {@code null} se nenhum movimento for válido.
     */
    private Horario tentarMoverUmTurnoDe(PedidoGeracao pedido,
                                         List<Horario> plano,
                                         Integer doador,
                                         double utilizacaoDoador,
                                         Map<Integer, Long> minutos) {
        List<Horario> turnosDoDoador = plano.stream()
                .filter(h -> Objects.equals(idColaborador(h), doador))
                .sorted(Comparator.comparing(Horario::getDataTurno,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (Horario h : turnosDoDoador) {
            if (h.getDataTurno() == null || h.getIdTurno() == null) {
                continue;
            }
            Integer recetor = procurarRecetor(pedido, plano, h, true,
                    candidato -> melhoraEquilibrio(pedido, minutos, h, doador,
                            utilizacaoDoador, candidato));
            if (recetor != null) {
                reatribuir(pedido, h, recetor);
                return h;
            }
        }
        return null;
    }

    /** O movimento só vale a pena se o recetor ficar abaixo da utilização atual do doador. */
    private boolean melhoraEquilibrio(PedidoGeracao pedido,
                                      Map<Integer, Long> minutos,
                                      Horario h,
                                      Integer doador,
                                      double utilizacaoDoador,
                                      Integer candidato) {
        long cargaCandidato = pedido.cargaMaximaPorColaborador().getOrDefault(candidato, 0L);
        if (cargaCandidato <= 0) {
            return false;
        }
        long minutosTurno = validator.calcularDuracaoEmMinutos(h.getIdTurno());
        double utilizacaoDepois = (minutos.getOrDefault(candidato, 0L) + minutosTurno)
                / (double) cargaCandidato;
        return utilizacaoDepois < utilizacaoDoador;
    }

    // =========================================================================
    // Movimento 1-para-1: procurar recetor e reatribuir
    // =========================================================================

    /** Tenta reatribuir {@code h} a outro colaborador. Devolve true se conseguiu. */
    private boolean tentarReatribuir(PedidoGeracao pedido,
                                     List<Horario> plano,
                                     Horario h,
                                     boolean evitarFolgaPreferidaDoRecetor) {
        Integer recetor = procurarRecetor(pedido, plano, h, evitarFolgaPreferidaDoRecetor, id -> true);
        if (recetor == null) {
            return false;
        }
        reatribuir(pedido, h, recetor);
        return true;
    }

    /**
     * Procura o melhor recetor (menor utilização primeiro) para assumir o turno {@code h},
     * garantindo: não escalado nesse dia, sem folga preferida nesse dia (opcional),
     * chefia do dia preservada, e agenda completa válida por replay estrito.
     */
    private Integer procurarRecetor(PedidoGeracao pedido,
                                    List<Horario> plano,
                                    Horario h,
                                    boolean evitarFolgaPreferidaDoRecetor,
                                    java.util.function.Predicate<Integer> criterioExtra) {
        Integer idAtual = idColaborador(h);
        LocalDate dia = h.getDataTurno();
        Map<Integer, Long> minutos = minutosAtribuidos(plano);
        Map<Integer, List<Horario>> porColaborador = agruparPorColaborador(plano);
        Set<Integer> escaladosNoDia = new java.util.LinkedHashSet<>();
        for (Horario outro : plano) {
            if (dia.equals(outro.getDataTurno())) {
                escaladosNoDia.add(idColaborador(outro));
            }
        }

        boolean diaExigeChefia = validator.ehFimDeSemana(dia)
                || (pedido.exigirChefiaAoSabado() && dia.getDayOfWeek() == java.time.DayOfWeek.SATURDAY);
        boolean doadorEhChefia = pedido.chefiasSabadoIds().contains(idAtual);
        long chefiasNoDia = escaladosNoDia.stream()
                .filter(id -> pedido.chefiasSabadoIds().contains(id))
                .count();

        List<Lojautilizador> candidatos = new ArrayList<>(pedido.colaboradores());
        candidatos.sort(Comparator.comparingDouble(ligacao -> {
            Integer id = idDe(ligacao);
            long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
            return carga > 0 ? minutos.getOrDefault(id, 0L) / (double) carga : Double.MAX_VALUE;
        }));

        for (Lojautilizador ligacao : candidatos) {
            Integer id = idDe(ligacao);
            if (id == null || id.equals(idAtual) || escaladosNoDia.contains(id)) {
                continue;
            }
            if (evitarFolgaPreferidaDoRecetor
                    && pedido.folgasPreferidasPorColaborador().getOrDefault(id, Set.of()).contains(dia)) {
                continue;
            }
            // Preservar a chefia mínima do dia: não tirar a única chefia de um FDS
            if (diaExigeChefia && doadorEhChefia && chefiasNoDia <= 1
                    && !pedido.chefiasSabadoIds().contains(id)) {
                continue;
            }
            if (!criterioExtra.test(id)) {
                continue;
            }
            if (agendaValidaComNovoTurno(pedido, ligacao,
                    porColaborador.getOrDefault(id, List.of()), dia, h.getIdTurno())) {
                return id;
            }
        }
        return null;
    }

    /**
     * Replay estrito da agenda do colaborador (histórico + mês + novo turno, por ordem
     * cronológica) contra todas as regras hard. Conservador: se alguma atribuição
     * existente não passar (p.ex. foi criada com relaxação de fim de semana), o
     * movimento é rejeitado.
     */
    private boolean agendaValidaComNovoTurno(PedidoGeracao pedido,
                                             Lojautilizador ligacao,
                                             List<Horario> atribuicoesDoMes,
                                             LocalDate novoDia,
                                             Turno novoTurno) {
        Integer id = idDe(ligacao);
        long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
        EstadoColaborador estado = new EstadoColaborador(
                ligacao, carga, pedido.chefiasSabadoIds(), validator);

        List<Horario> historico = new ArrayList<>();
        for (Horario h : pedido.historicoHorarios()) {
            if (Objects.equals(idColaborador(h), id)
                    && h.getDataTurno() != null && h.getIdTurno() != null) {
                historico.add(h);
            }
        }
        historico.sort(Comparator.comparing(Horario::getDataTurno));
        estado.inicializarComHistorico(historico);

        record Atribuicao(LocalDate data, Turno turno) {
        }
        List<Atribuicao> agenda = new ArrayList<>();
        for (Horario h : atribuicoesDoMes) {
            if (h.getDataTurno() != null && h.getIdTurno() != null) {
                agenda.add(new Atribuicao(h.getDataTurno(), h.getIdTurno()));
            }
        }
        agenda.add(new Atribuicao(novoDia, novoTurno));
        agenda.sort(Comparator.comparing(Atribuicao::data));

        for (Atribuicao a : agenda) {
            long minutos = validator.calcularDuracaoEmMinutos(a.turno());
            if (!estado.podeReceber(a.data(), a.turno(), minutos, pedido, false, false)) {
                return false;
            }
            estado.registarAtribuicao(a.data(), a.turno(), minutos);
        }
        return true;
    }

    private void reatribuir(PedidoGeracao pedido, Horario h, Integer recetor) {
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            if (Objects.equals(idDe(ligacao), recetor)) {
                h.setIdLojautilizador(ligacao);
                return;
            }
        }
    }

    // =========================================================================
    // Utilitários
    // =========================================================================

    private boolean prazoEsgotado(PedidoGeracao pedido) {
        return pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite());
    }

    private Map<Integer, Long> minutosAtribuidos(List<Horario> plano) {
        Map<Integer, Long> minutos = new HashMap<>();
        for (Horario h : plano) {
            Integer id = idColaborador(h);
            if (id == null || h.getIdTurno() == null) {
                continue;
            }
            minutos.merge(id, validator.calcularDuracaoEmMinutos(h.getIdTurno()), Long::sum);
        }
        return minutos;
    }

    private Map<Integer, List<Horario>> agruparPorColaborador(List<Horario> plano) {
        Map<Integer, List<Horario>> mapa = new LinkedHashMap<>();
        for (Horario h : plano) {
            Integer id = idColaborador(h);
            if (id != null) {
                mapa.computeIfAbsent(id, k -> new ArrayList<>()).add(h);
            }
        }
        return mapa;
    }

    private static Integer idColaborador(Horario h) {
        if (h == null || h.getIdLojautilizador() == null
                || h.getIdLojautilizador().getIdUtilizador() == null) {
            return null;
        }
        return h.getIdLojautilizador().getIdUtilizador().getId();
    }

    private static Integer idDe(Lojautilizador ligacao) {
        return ligacao != null && ligacao.getIdUtilizador() != null
                ? ligacao.getIdUtilizador().getId()
                : null;
    }
}
