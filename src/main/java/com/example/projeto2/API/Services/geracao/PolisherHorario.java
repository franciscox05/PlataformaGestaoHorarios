package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.HorarioValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * Fase de <b>polimento por pesquisa local</b> (hill-climbing) do planeamento mensal.
 *
 * <p>O motor constrói o horário dia-a-dia (greedy + backtracking) e o
 * {@link RefinadorPlaneamento} faz movimentos 1-para-1 dirigidos (recuperar folgas
 * preferidas, equilibrar carga). Ambos são míopes: o greedy não retrocede e o refinador
 * só desloca turnos mantendo o mesmo dia/turno. Esta fase corre <em>depois</em> e procura
 * melhorias globais que nenhum dos anteriores alcança, através de <b>trocas</b> entre dois
 * colaboradores:
 *
 * <ul>
 *   <li>Escolhe duas atribuições (colaborador A no dia X, colaborador B no dia Y) e troca
 *       o dono de cada uma — A passa a fazer o turno de B no dia Y e vice-versa.</li>
 *   <li>A troca <b>preserva exatamente a cobertura</b>: cada par (dia, turno) continua
 *       coberto pelo mesmo número de pessoas — só muda <em>quem</em> o cobre.</li>
 *   <li>Aceita a troca apenas se reduzir o custo global (função-objetivo) e não violar
 *       nenhuma regra hard nem a presença mínima de chefia ao fim de semana.</li>
 * </ul>
 *
 * <p>A função-objetivo reutiliza o {@link MetricasPlaneamentoCalculator} (equilíbrio de
 * carga, utilização contratual, amplitude de fins de semana, idle streaks — o mesmo score
 * usado para recomendar a melhor alternativa) e acrescenta penalizações por preferências
 * não honradas (folga preferida trabalhada, turno preferido não cumprido), que o score de
 * recomendação não captura mas que são o valor central do produto.
 *
 * <p>Cada troca candidata é validada por <em>replay estrito</em> da agenda completa dos
 * dois colaboradores afetados (histórico + mês, por ordem cronológica) contra todas as
 * regras hard via {@link EstadoColaborador#podeReceber}. Determinístico: a sequência de
 * trocas é semeada por {@link PedidoGeracao#semente()}, pelo que cada alternativa explora
 * trocas diferentes e o resultado é reproduzível.
 */
public final class PolisherHorario {

    private static final Logger LOGGER = LoggerFactory.getLogger(PolisherHorario.class);

    /**
     * Orçamento máximo de avaliações de pares por sessão de polimento. Com a enumeração
     * sistemática (sem repetições), cobre todos os pares de um plano com até ~100
     * atribuições (C(100,2)=4950) num único passe. Para planos maiores, cobre um subconjunto
     * aleatório mas sem desperdiçar budget em duplicados.
     */
    private static final int MAX_ITERACOES = 5000;

    /** Penalização por folga preferida (soft) trabalhada — pesa como ~uma falha de regra leve. */
    private static final double PESO_FOLGA_PREFERIDA = 50.0;

    /** Penalização por turno preferido não honrado num dia em que a preferência está ativa. */
    private static final double PESO_TURNO_PREFERIDO = 12.0;

    /**
     * Penalização por co-presença perdida: dois colaboradores que se preferem estão ambos
     * escalados no mesmo dia mas em tipos de turno diferentes. Mantida abaixo de
     * PESO_TURNO_PREFERIDO para não dominar o custo, mas suficiente para que o polisher
     * prefira trocas que os juntem.
     */
    private static final double PESO_COLEGA_NAO_ALINHADO = 8.0;

    private final HorarioValidatorService validator;
    private final MetricasPlaneamentoCalculator metricas = new MetricasPlaneamentoCalculator();
    private final AvaliadorAtribuicao avaliador;

    public PolisherHorario(HorarioValidatorService validator) {
        this.validator = validator;
        this.avaliador = new AvaliadorAtribuicao(validator);
    }

    /**
     * Poli o plano por trocas que reduzam o custo global. Devolve a mesma lista (mutada);
     * em caso de erro inesperado, devolve o plano original intacto.
     */
    public List<Horario> polir(PedidoGeracao pedido, List<Horario> horarios) {
        if (pedido == null || horarios == null || horarios.size() < 2) {
            return horarios;
        }
        try {
            List<Horario> trocaveis = horarios.stream()
                    .filter(h -> idColab(h) != null && h.getDataTurno() != null && h.getIdTurno() != null)
                    .toList();
            if (trocaveis.size() < 2) {
                return horarios;
            }

            Random rnd = new Random(pedido.semente());
            double custoAtual = custo(horarios, pedido);
            int aceites = 0;
            int totalIteracoes = 0;
            boolean melhorou = true;

            // Enumeração sistemática: constrói todos os pares únicos por passe, baralha-os
            // e itera até não haver mais melhorias ou o orçamento estar esgotado.
            while (melhorou && totalIteracoes < MAX_ITERACOES) {
                int n = trocaveis.size();
                List<int[]> pares = new ArrayList<>(n * (n - 1) / 2);
                for (int i = 0; i < n; i++) {
                    for (int j = i + 1; j < n; j++) {
                        pares.add(new int[]{i, j});
                    }
                }
                Collections.shuffle(pares, rnd);

                melhorou = false;
                for (int[] par : pares) {
                    if (++totalIteracoes > MAX_ITERACOES) break;
                    if ((totalIteracoes & 63) == 0 && prazoEsgotado(pedido)) break;
                    Horario hA = trocaveis.get(par[0]);
                    Horario hB = trocaveis.get(par[1]);
                    if (tentarTroca(pedido, horarios, trocaveis, hA, hB, custoAtual)) {
                        custoAtual = custo(horarios, pedido);
                        aceites++;
                        melhorou = true;
                    }
                }
            }

            if (aceites > 0) {
                LOGGER.info("Polimento do planeamento: {} troca(s) aceite(s) (custo final {}).",
                        aceites, Math.round(custoAtual));
            }
            return horarios;
        } catch (Exception e) {
            LOGGER.warn("Polimento do planeamento falhou — mantém-se o plano original.", e);
            return horarios;
        }
    }

    /**
     * Tenta trocar o dono de {@code hA} com o de {@code hB}. Aplica a troca apenas se for
     * estruturalmente válida (sem conflitos de dia, chefia preservada), passar no replay
     * hard de ambos os colaboradores e reduzir o custo global. Devolve true se aceitou.
     */
    private boolean tentarTroca(PedidoGeracao pedido,
                                List<Horario> plano,
                                List<Horario> trocaveis,
                                Horario hA,
                                Horario hB,
                                double custoAtual) {
        Lojautilizador ligA = hA.getIdLojautilizador();
        Lojautilizador ligB = hB.getIdLojautilizador();
        Integer idA = idColab(hA);
        Integer idB = idColab(hB);
        if (idA == null || idB == null || idA.equals(idB)) {
            return false;
        }
        LocalDate diaX = hA.getDataTurno();
        LocalDate diaY = hB.getDataTurno();
        Turno turnoA = hA.getIdTurno();
        Turno turnoB = hB.getIdTurno();

        boolean mesmoDia = diaX.equals(diaY);
        // Troca no mesmo dia só vale a pena se forem turnos de período diferente.
        if (mesmoDia && TurnoClassifier.tipoNormalizado(turnoA)
                .equals(TurnoClassifier.tipoNormalizado(turnoB))) {
            return false;
        }

        // Conflito de agenda: depois da troca, A trabalha no dia Y e B no dia X — nenhum
        // deles pode já ter outro turno nesse dia (criaria dois turnos no mesmo dia).
        if (!mesmoDia && (jaTrabalhaEm(trocaveis, idA, diaY) || jaTrabalhaEm(trocaveis, idB, diaX))) {
            return false;
        }

        // Preservar a presença mínima de chefia ao fim de semana: se A e B têm estatuto de
        // chefia diferente, a troca move uma chefia entre dias. O dia que a perde tem de
        // continuar com pelo menos uma chefia (todos os dias de fim de semana exigem 1).
        boolean chefiaA = pedido.chefiasSabadoIds().contains(idA);
        boolean chefiaB = pedido.chefiasSabadoIds().contains(idB);
        if (!mesmoDia && chefiaA != chefiaB && !chefiaPreservada(trocaveis, diaX, diaY, chefiaA, chefiaB)) {
            return false;
        }

        // Aplica a troca (só muda o dono de cada turno; dia e turno ficam intactos).
        hA.setIdLojautilizador(ligB);
        hB.setIdLojautilizador(ligA);

        boolean valida = agendaValida(pedido, ligA, plano) && agendaValida(pedido, ligB, plano);
        if (valida && custo(plano, pedido) < custoAtual) {
            return true;
        }

        // Reverte.
        hA.setIdLojautilizador(ligA);
        hB.setIdLojautilizador(ligB);
        return false;
    }

    /** Após a troca, cada dia de fim de semana afetado mantém ≥ 1 chefia. */
    private boolean chefiaPreservada(List<Horario> trocaveis,
                                     LocalDate diaX,
                                     LocalDate diaY,
                                     boolean chefiaA,
                                     boolean chefiaB) {
        // Dia X: perde A, ganha B.
        if (validator.ehFimDeSemana(diaX)) {
            int depois = chefiasNoDia(trocaveis, diaX) - (chefiaA ? 1 : 0) + (chefiaB ? 1 : 0);
            if (depois < 1) return false;
        }
        // Dia Y: perde B, ganha A.
        if (validator.ehFimDeSemana(diaY)) {
            int depois = chefiasNoDia(trocaveis, diaY) - (chefiaB ? 1 : 0) + (chefiaA ? 1 : 0);
            if (depois < 1) return false;
        }
        return true;
    }

    private int chefiasNoDia(List<Horario> trocaveis, LocalDate dia) {
        int total = 0;
        for (Horario h : trocaveis) {
            if (dia.equals(h.getDataTurno())) {
                // chefia inferida pelo cargo da ligação atual do turno
                Lojautilizador lig = h.getIdLojautilizador();
                if (lig != null && lig.getIdCargo() != null
                        && validator.exigePresencaAoSabado(lig.getIdCargo().getNome())) {
                    total++;
                }
            }
        }
        return total;
    }

    private boolean jaTrabalhaEm(List<Horario> trocaveis, Integer id, LocalDate dia) {
        for (Horario h : trocaveis) {
            if (id.equals(idColab(h)) && dia.equals(h.getDataTurno())) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // Validação hard por replay estrito (mesma abordagem do RefinadorPlaneamento)
    // =========================================================================

    /**
     * Replay cronológico da agenda do colaborador (histórico + mês) contra todas as regras
     * hard. Conservador: se alguma atribuição existente não passar, a troca é rejeitada.
     */
    private boolean agendaValida(PedidoGeracao pedido, Lojautilizador ligacao, List<Horario> plano) {
        Integer id = idDe(ligacao);
        long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
        EstadoColaborador estado = new EstadoColaborador(
                ligacao, carga, pedido.chefiasSabadoIds(), validator);

        List<Horario> historico = new ArrayList<>();
        for (Horario h : pedido.historicoHorarios()) {
            if (Objects.equals(idColab(h), id)
                    && h.getDataTurno() != null && h.getIdTurno() != null) {
                historico.add(h);
            }
        }
        historico.sort(Comparator.comparing(Horario::getDataTurno));
        estado.inicializarComHistorico(historico);

        record Atribuicao(LocalDate data, Turno turno) {}
        List<Atribuicao> agenda = new ArrayList<>();
        for (Horario h : plano) {
            if (Objects.equals(idColab(h), id)
                    && h.getDataTurno() != null && h.getIdTurno() != null) {
                agenda.add(new Atribuicao(h.getDataTurno(), h.getIdTurno()));
            }
        }
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

    // =========================================================================
    // Função-objetivo (menor = melhor)
    // =========================================================================

    /** Avalia o custo global de um plano gerado. Menor = melhor. Usado pelo multi-start. */
    public double avaliarCusto(PedidoGeracao pedido, List<Horario> plano) {
        return custo(plano, pedido);
    }

    private double custo(List<Horario> plano, PedidoGeracao pedido) {
        Map<Integer, CargaColaborador> cargas = construirCargas(plano, pedido);
        MetricasPlaneamento m = metricas.calcular(plano, cargas, pedido.politica());
        return m.pontuacao() + penalizacaoPreferencias(plano, pedido);
    }

    /** Carga por colaborador (inclui quem não tem turnos, para contar subutilização/idle). */
    private Map<Integer, CargaColaborador> construirCargas(List<Horario> plano, PedidoGeracao pedido) {
        Map<Integer, Long> minutos = new HashMap<>();
        Map<Integer, Set<LocalDate>> fdsDistintos = new HashMap<>();
        for (Horario h : plano) {
            Integer id = idColab(h);
            if (id == null || h.getIdTurno() == null) continue;
            minutos.merge(id, validator.calcularDuracaoEmMinutos(h.getIdTurno()), Long::sum);
            if (h.getDataTurno() != null && validator.ehFimDeSemana(h.getDataTurno())) {
                fdsDistintos.computeIfAbsent(id, ignored -> new HashSet<>())
                        .add(validator.inicioFimDeSemana(h.getDataTurno()));
            }
        }

        Map<Integer, CargaColaborador> cargas = new LinkedHashMap<>();
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            Integer id = idDe(ligacao);
            if (id == null) continue;
            long carga = pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L);
            cargas.put(id, new CargaColaborador(
                    id, nomeDe(ligacao), minutos.getOrDefault(id, 0L), carga,
                    fdsDistintos.getOrDefault(id, Set.of()).size()));
        }
        return cargas;
    }

    /**
     * Penaliza preferências aprovadas não honradas: trabalhar num dia de folga preferida e
     * trabalhar um turno que não corresponde a uma preferência de turno ativa nesse dia.
     *
     * <p>A penalização de turno é <b>quadrática por colaborador</b> (triangular {@code n(n+1)/2}):
     * concentrar muitos turnos não-preferidos numa só pessoa custa mais do que reparti-los.
     * Assim, quando um turno é impopular (ex.: a noite, com toda a equipa a preferir manhã) e
     * <em>alguém tem mesmo de o fazer</em>, o polisher espalha-o por vários colaboradores em vez
     * de o despejar sempre no mesmo — mais justo, com o mesmo total de preferências cumpridas.
     */
    private double penalizacaoPreferencias(List<Horario> plano, PedidoGeracao pedido) {
        Map<Integer, Set<LocalDate>> folgas = pedido.folgasPreferidasPorColaborador();
        Map<Integer, List<Preferencia>> prefsTurno = pedido.preferenciasTurnos();
        double total = 0;
        Map<Integer, Integer> turnosNaoHonradosPorColab = new HashMap<>();
        for (Horario h : plano) {
            Integer id = idColab(h);
            LocalDate dia = h.getDataTurno();
            Turno turno = h.getIdTurno();
            if (id == null || dia == null || turno == null) continue;

            if (folgas.getOrDefault(id, Set.of()).contains(dia)) {
                total += PESO_FOLGA_PREFERIDA;
            }
            if (preferenciaTurnoAtiva(id, dia, prefsTurno)
                    && !avaliador.temPreferenciaTurnoFavoravel(id, turno, dia, prefsTurno)) {
                turnosNaoHonradosPorColab.merge(id, 1, Integer::sum);
            }
        }
        for (int n : turnosNaoHonradosPorColab.values()) {
            total += PESO_TURNO_PREFERIDO * (n * (n + 1) / 2.0);
        }

        // Co-presença perdida: C e colega preferido P ambos escalados no mesmo dia mas em
        // tipos de turno diferentes — podiam estar juntos, não estão.
        Map<Integer, Set<Integer>> paresPref = pedido.paresPreferisPorColaborador();
        if (!paresPref.isEmpty()) {
            Map<Integer, Map<LocalDate, String>> turnoPorDia = new HashMap<>();
            for (Horario h : plano) {
                Integer id = idColab(h);
                if (id == null || h.getDataTurno() == null || h.getIdTurno() == null) continue;
                turnoPorDia.computeIfAbsent(id, k -> new HashMap<>())
                           .put(h.getDataTurno(), TurnoClassifier.tipoNormalizado(h.getIdTurno()));
            }
            for (Map.Entry<Integer, Set<Integer>> entry : paresPref.entrySet()) {
                Integer idC = entry.getKey();
                Map<LocalDate, String> turnosC = turnoPorDia.getOrDefault(idC, Map.of());
                for (Integer idP : entry.getValue()) {
                    Map<LocalDate, String> turnosP = turnoPorDia.getOrDefault(idP, Map.of());
                    for (Map.Entry<LocalDate, String> diaC : turnosC.entrySet()) {
                        String tipoP = turnosP.get(diaC.getKey());
                        if (tipoP != null && !tipoP.equals(diaC.getValue())) {
                            total += PESO_COLEGA_NAO_ALINHADO;
                        }
                    }
                }
            }
        }

        return total;
    }

    /** Existe alguma preferência de turno aprovada deste colaborador ativa neste dia. */
    private boolean preferenciaTurnoAtiva(Integer id,
                                          LocalDate dia,
                                          Map<Integer, List<Preferencia>> prefsTurno) {
        for (Preferencia p : prefsTurno.getOrDefault(id, List.of())) {
            boolean dentroDoIntervalo = p.getDataInicio() != null
                    && !dia.isBefore(p.getDataInicio())
                    && (p.getDataFim() == null || !dia.isAfter(p.getDataFim()));
            if (dentroDoIntervalo) return true;
        }
        return false;
    }

    // =========================================================================
    // Utilitários
    // =========================================================================

    private boolean prazoEsgotado(PedidoGeracao pedido) {
        return pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite());
    }

    private static String nomeDe(Lojautilizador ligacao) {
        if (ligacao != null && ligacao.getIdUtilizador() != null
                && ligacao.getIdUtilizador().getNome() != null
                && !ligacao.getIdUtilizador().getNome().isBlank()) {
            return ligacao.getIdUtilizador().getNome();
        }
        return "Colaborador";
    }

    private static Integer idColab(Horario h) {
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
