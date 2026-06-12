package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.ConfiguracaoDia;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.FalhaGeracaoHorarioException;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PlaneadorFinsDeSemana;
import com.example.projeto2.API.Services.geracao.PlaneadorFolgasPreferidas;
import com.example.projeto2.API.Services.geracao.PlanoFinsDeSemana;
import com.example.projeto2.API.Services.geracao.RefinadorPlaneamento;
import com.example.projeto2.API.Services.geracao.TurnoClassifier;
import com.example.projeto2.API.Services.geracao.diagnostico.DiagnosticoCobertura;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Motor de geração de horários mensais.
 *
 * <p>Responsabilidade única: dado um {@link PedidoGeracao} (colaboradores elegíveis,
 * restrições operacionais e preferências aprovadas), produzir uma lista de
 * {@link Horario} que satisfaça todas as regras hard, otimizando segundo a política ativa.
 *
 * <p>Algoritmo: pesquisa por backtracking com poda heurística (greedy-first), escalando
 * por orçamentos progressivos e relaxando, em último recurso e apenas ao fim de semana,
 * as restrições <em>soft-relaxáveis</em> (rotação de fins de semana e descanso semanal):
 * <ol>
 *   <li>Orçamento base — sem relaxação</li>
 *   <li>Orçamento alargado — sem relaxação</li>
 *   <li>Rotação de fins de semana relaxada (só fim de semana)</li>
 *   <li>Descanso semanal relaxado (só fim de semana)</li>
 * </ol>
 *
 * <p>Cumprido o mínimo de cada dia, corre a fase de <b>preenchimento de capacidade</b>
 * ({@link #reforcarCoberturaDoDia}): atribui turnos extra aos colaboradores atrasados
 * face ao ritmo contratual, para que a cobertura diária use a capacidade real da equipa
 * em vez de ficar pelo mínimo — sem relaxações e sem ultrapassar a carga contratual.
 *
 * <p>No final da construção corre uma fase de <b>refinamento por pesquisa local</b>
 * ({@link RefinadorPlaneamento}): movimentos 1-para-1 que recuperam folgas preferidas
 * não honradas e equilibram a carga contratual entre colaboradores, sempre validados
 * contra todas as regras hard.
 *
 * <p>O motor delega: as regras hard ao {@link HorarioValidatorService}, a pontuação de
 * candidatos ao {@link AvaliadorAtribuicao}, e o estado por colaborador ao
 * {@link EstadoColaborador}. Mantém-se assim focado na pesquisa.
 */
@Service
public class HorarioGeneratorEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(HorarioGeneratorEngine.class);
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Orçamentos progressivos de pesquisa
    private static final int LIMITE_CANDIDATOS_POR_SLOT_BASE     = 8;
    private static final int LIMITE_CANDIDATOS_POR_SLOT_ALARGADO = 12;
    private static final int LIMITE_CANDIDATOS_POR_SLOT_EXCECAO  = 16;
    private static final int LIMITE_NOS_PESQUISA_BASE     = 12_000;
    private static final int LIMITE_NOS_PESQUISA_ALARGADO = 24_000;
    private static final int LIMITE_NOS_PESQUISA_EXCECAO  = 40_000;

    private final HorarioValidatorService validator;
    private final AvaliadorAtribuicao avaliador;
    private final RefinadorPlaneamento refinador;
    private final PlaneadorFolgasPreferidas planeadorFolgas;
    private final PlaneadorFinsDeSemana planeadorFins;

    public HorarioGeneratorEngine(HorarioValidatorService validator) {
        this.validator = validator;
        this.avaliador = new AvaliadorAtribuicao(validator);
        this.refinador = new RefinadorPlaneamento(validator);
        this.planeadorFolgas = new PlaneadorFolgasPreferidas(validator);
        this.planeadorFins = new PlaneadorFinsDeSemana(validator);
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Ponto de entrada principal.
     *
     * @param pedido inputs imutáveis da geração.
     * @return lista de {@link Horario} gerados (sem ID — ainda não persistidos).
     * @throws IllegalArgumentException se não for possível satisfazer todas as restrições.
     */
    public List<Horario> gerar(PedidoGeracao pedido) {
        Map<Integer, EstadoColaborador> estadoPorColaborador = inicializarEstados(pedido);
        inicializarHistorico(estadoPorColaborador, pedido.historicoHorarios());

        // Pré-planeamento: reserva proativamente as folgas preferidas que cabem sem
        // comprometer a cobertura (dias úteis), garantindo-as antes do greedy começar.
        Map<Integer, Set<LocalDate>> folgasReservadas = planeadorFolgas.reservar(pedido);
        folgasReservadas.forEach((id, dias) -> {
            EstadoColaborador estado = estadoPorColaborador.get(id);
            if (estado != null) {
                estado.reservarFolgasPreferidas(dias);
            }
        });

        // Lookahead de fins de semana: designação global (consultiva) de quem trabalha
        // cada FDS, para o avaliador planear a rotação em vez de a resolver reativamente.
        PlanoFinsDeSemana planoFins = planeadorFins.planear(pedido);
        if (planoFins.ativo()) {
            estadoPorColaborador.forEach((id, estado) ->
                    estado.designarFinsDeSemana(planoFins.designados(id), planoFins.chefiaEm(id)));
        }

        List<Horario> horarios = new ArrayList<>();
        Set<LocalDate> sabadosComChefia = new LinkedHashSet<>();

        for (LocalDate data = pedido.dataInicio(); !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
            validarPrazo(pedido.prazoLimite(), data, pedido.politica().nome());

            ConfiguracaoDia configDia = pedido.configuracoesPorData().get(data);
            if (configDia != null && configDia.lojaEncerrada()) {
                continue;
            }

            List<Turno> turnosDoDia = configDia != null ? configDia.turnosCompativeis() : pedido.turnos();
            if (turnosDoDia.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nao existem turnos compativeis com a excecao em " + data + ".");
            }

            List<AtribuicaoDia> atribuicoes = planearDia(
                    data, turnosDoDia, configDia, estadoPorColaborador,
                    horarios, sabadosComChefia.contains(data), pedido);
            registarAtribuicoes(data, atribuicoes, horarios, sabadosComChefia);

            // Fase de preenchimento de capacidade: com o mínimo do dia garantido,
            // reforça a cobertura com colaboradores atrasados face ao ritmo contratual.
            // Dias com configuração especial cumprem exatamente o mínimo definido pelo gerente.
            if (configDia == null) {
                List<AtribuicaoDia> extras = reforcarCoberturaDoDia(
                        data, turnosDoDia, estadoPorColaborador.values(), horarios, pedido);
                registarAtribuicoes(data, extras, horarios, sabadosComChefia);
            }
        }
        // Fase final: refinamento por pesquisa local (folgas preferidas + equilíbrio de carga)
        return refinador.refinar(pedido, horarios);
    }

    private void registarAtribuicoes(LocalDate data,
                                     List<AtribuicaoDia> atribuicoes,
                                     List<Horario> horarios,
                                     Set<LocalDate> sabadosComChefia) {
        for (AtribuicaoDia a : atribuicoes) {
            Horario h = new Horario();
            h.setIdLojautilizador(a.estado().ligacao());
            h.setIdTurno(a.turno());
            h.setDataTurno(data);
            horarios.add(h);

            a.estado().registarAtribuicao(data, a.turno(), a.minutosTurno());
            if (data.getDayOfWeek() == DayOfWeek.SATURDAY && a.estado().ehChefiaAoSabado()) {
                sabadosComChefia.add(data);
            }
        }
    }

    // =========================================================================
    // Fase de preenchimento de capacidade (anti-desfalque)
    // =========================================================================

    /**
     * Reforça a cobertura de um dia para além do mínimo, usando a capacidade
     * contratual ainda por consumir. Sem isto, o motor tratava o mínimo por turno
     * como alvo exato e os colaboradores ficavam muito aquém da carga contratual —
     * horários "desfalcados" com a equipa em casa.
     *
     * <p>Critérios, por colaborador:
     * <ul>
     *   <li><b>Ritmo contratual:</b> só recebe turno extra quem está atrasado face ao
     *       ritmo {@code carga × diasDecorridos / diasTotais} — distribui a carga
     *       uniformemente pelo mês e nunca ultrapassa o teto contratual.</li>
     *   <li><b>Regras hard intactas:</b> cada turno extra passa por
     *       {@link EstadoColaborador#podeReceber} sem qualquer relaxação.</li>
     *   <li><b>Folgas preferidas respeitadas:</b> um turno extra nunca cai num dia de
     *       folga preferida — a cobertura mínima já está garantida sem ele.</li>
     *   <li><b>Margem para o fim de semana:</b> em dias úteis, quem ainda pode vir a
     *       ser necessário ao fim de semana guarda um dia de folga semanal de reserva,
     *       para que o preenchimento não esgote os candidatos de sábado/domingo.</li>
     * </ul>
     *
     * <p>O turno escolhido para cada colaborador é o de melhor pontuação no
     * {@link AvaliadorAtribuicao} (respeita preferências de turno aprovadas, política
     * de otimização ativa, etc.). Devolve no máximo um turno extra por colaborador.
     */
    private List<AtribuicaoDia> reforcarCoberturaDoDia(LocalDate data,
                                                       List<Turno> turnosDoDia,
                                                       Collection<EstadoColaborador> estados,
                                                       List<Horario> horariosJaGerados,
                                                       PedidoGeracao pedido) {
        if (pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite())) {
            return List.of();
        }
        // Top-up ao fim de semana: outrora bloqueado por completo — sem visão dos fins de
        // semana seguintes, encher um FDS via top-up esgotava o grupo elegível e a rotação
        // bloqueava o FDS seguinte. Com o lookahead de FDS ativo passa a ser permitido, mas
        // restrito aos colaboradores DESIGNADOS para este FDS (filtro no ciclo abaixo): esses
        // já estão comprometidos com este fim de semana pelo plano, pelo que reforçá-los não
        // rouba candidatos aos fins de semana seguintes. Sem plano ativo, mantém-se o
        // bloqueio total (comportamento seguro anterior).
        boolean fimDeSemana = validator.ehFimDeSemana(data);
        boolean planoFinsDeSemanaAtivo = estados.stream()
                .anyMatch(EstadoColaborador::temPlanoFinsDeSemana);
        if (fimDeSemana && !planoFinsDeSemanaAtivo) {
            return List.of();
        }
        long diasTotais = ChronoUnit.DAYS.between(pedido.dataInicio(), pedido.dataFim()) + 1;
        long diasDecorridos = ChronoUnit.DAYS.between(pedido.dataInicio(), data) + 1;
        boolean diaUtil = !fimDeSemana;
        int maxDiasTrabalhadosNaSemana = 7 - pedido.descansoSemanalMinimoDias();

        AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContexto(horariosJaGerados);
        List<CandidatoPontuado> candidatos = new ArrayList<>();
        for (EstadoColaborador estado : estados) {
            // Exige pelo menos um dia completo de atraso antes de reforçar:
            // evita que workers marginalmente abaixo do ritmo esgotem a carga
            // antes do fim do mês, deixando os últimos dias sem cobertura.
            if (!estado.abaixoDoRitmoContratual(diasDecorridos - 1, diasTotais)) continue;
            if (pedido.folgasPreferidasPorColaborador()
                    .getOrDefault(estado.idUtilizador(), Set.of()).contains(data)) continue;
            // Ao fim de semana, só reforça quem o plano designou para este FDS — preserva
            // os candidatos dos fins de semana seguintes (a rotação não os bloqueia cedo).
            if (fimDeSemana && !estado.designadoParaFimDeSemana(data)) continue;
            if (diaUtil
                    && estado.podeVirASerPrecisoNoFimDeSemana(data, pedido)
                    && estado.diasTrabalhadosNaSemana(data) >= maxDiasTrabalhadosNaSemana - 1) {
                continue; // reserva de margem semanal para a cobertura do fim de semana
            }

            CandidatoPontuado melhor = null;
            for (Turno turno : turnosDoDia) {
                long minutos = validator.calcularDuracaoEmMinutos(turno);
                if (!estado.podeReceber(data, turno, minutos, pedido, false, false)) continue;
                double score = avaliador.pontuar(estado, turno, minutos, data, pedido, contexto);
                if (melhor == null || score < melhor.score()) {
                    melhor = new CandidatoPontuado(new AtribuicaoDia(estado, turno, minutos), score);
                }
            }
            if (melhor != null) {
                candidatos.add(melhor);
            }
        }

        // Limita o top-up inversamente ao mínimo por tipo: com mínimo=1, acrescenta
        // até 1 por tipo (3 total); com mínimo=2, o mínimo já absorve metade da carga
        // contratual, por isso o top-up é reduzido para evitar exaustão antes do final
        // do mês. Garante sempre pelo menos 1 (fix: a divisão inteira podia produzir 0
        // quando minimoMaxGlobal > numTipos, bloqueando por completo o top-up).
        int minimoMaxGlobal = pedido.minimosPorTurno().values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);
        long numTiposDistintos = turnosDoDia.stream()
                .map(TurnoClassifier::tipoNormalizado)
                .distinct()
                .count();
        long limiteTopUp = Math.max(1L, numTiposDistintos / Math.max(1, minimoMaxGlobal));

        return candidatos.stream()
                .sorted(Comparator.comparingDouble(CandidatoPontuado::score))
                .limit(limiteTopUp)
                .map(CandidatoPontuado::atribuicao)
                .toList();
    }

    // =========================================================================
    // Inicialização de estados
    // =========================================================================

    private Map<Integer, EstadoColaborador> inicializarEstados(PedidoGeracao pedido) {
        Map<Integer, EstadoColaborador> mapa = new LinkedHashMap<>();
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            long cargaMaxima = pedido.cargaMaximaPorColaborador()
                    .getOrDefault(ligacao.getIdUtilizador().getId(), 0L);
            mapa.put(ligacao.getIdUtilizador().getId(),
                    new EstadoColaborador(ligacao, cargaMaxima, pedido.chefiasSabadoIds(), validator));
        }
        return mapa;
    }

    private void inicializarHistorico(Map<Integer, EstadoColaborador> estadoPorColaborador,
                                      List<Horario> historicoHorarios) {
        Map<Integer, List<Horario>> historicoPorColaborador = new HashMap<>();
        for (Horario h : historicoHorarios) {
            if (h.getIdLojautilizador() == null
                    || h.getIdLojautilizador().getIdUtilizador() == null
                    || h.getDataTurno() == null
                    || h.getIdTurno() == null) {
                continue;
            }
            historicoPorColaborador
                    .computeIfAbsent(h.getIdLojautilizador().getIdUtilizador().getId(),
                            ignored -> new ArrayList<>())
                    .add(h);
        }
        for (Map.Entry<Integer, List<Horario>> entry : historicoPorColaborador.entrySet()) {
            entry.getValue().sort(Comparator.comparing(Horario::getDataTurno)
                    .thenComparing(h -> h.getIdTurno().getHoraInicio(),
                            Comparator.nullsLast(Comparator.naturalOrder())));
            EstadoColaborador estado = estadoPorColaborador.get(entry.getKey());
            if (estado != null) {
                estado.inicializarComHistorico(entry.getValue());
            }
        }
    }

    // =========================================================================
    // Planeamento de um dia — orçamento progressivo com relaxações
    // =========================================================================

    private List<AtribuicaoDia> planearDia(LocalDate data,
                                           List<Turno> turnosDoDia,
                                           ConfiguracaoDia configDia,
                                           Map<Integer, EstadoColaborador> estadoPorColaborador,
                                           List<Horario> horariosJaGerados,
                                           boolean sabadoJaTemChefia,
                                           PedidoGeracao pedido) {

        List<SlotDia> slots = construirSlots(turnosDoDia, configDia, pedido.minimosPorTurno());

        boolean precisaChefia = pedido.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && !sabadoJaTemChefia;

        if (precisaChefia && !existeChefiaPossivel(data, slots, estadoPorColaborador.values(), pedido)) {
            throw new IllegalArgumentException(
                    "Nao foi possivel garantir presenca de gerente/subgerente no sabado "
                            + DATA_FORMATTER.format(data) + ".");
        }

        int minimoChefiasNoDia = (validator.ehFimDeSemana(data) || precisaChefia) ? 1 : 0;
        boolean fimDeSemana = validator.ehFimDeSemana(data);

        // Tentativa 1: orçamento base, sem relaxação
        ResultadoTentativa t1 = executarTentativa(data, slots, estadoPorColaborador,
                horariosJaGerados, pedido, false, false, minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_BASE, LIMITE_NOS_PESQUISA_BASE);
        if (t1.encontrou()) return t1.atribuicoes();
        registarTentativaLimitada(t1, data, "base");

        // Tentativa 2: orçamento alargado, sem relaxação
        ResultadoTentativa t2 = executarTentativa(data, slots, estadoPorColaborador,
                horariosJaGerados, pedido, false, false, minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_ALARGADO, LIMITE_NOS_PESQUISA_ALARGADO);
        if (t2.encontrou()) return t2.atribuicoes();
        registarTentativaLimitada(t2, data, "alargada");

        ResultadoTentativa ultima = t2;

        // Tentativa 3: rotação de fim de semana relaxada (só ao fim de semana)
        if (fimDeSemana && pedido.janelaRotacaoFimDeSemana() >= 2) {
            ResultadoTentativa t3 = executarTentativa(data, slots, estadoPorColaborador,
                    horariosJaGerados, pedido, true, false, minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO, LIMITE_NOS_PESQUISA_EXCECAO);
            ultima = t3;
            if (t3.encontrou()) return t3.atribuicoes();
            registarTentativaLimitada(t3, data, "rotacao-relaxada");
        }

        // Tentativa 4: descanso semanal relaxado (só ao fim de semana)
        if (fimDeSemana) {
            ResultadoTentativa t4 = executarTentativa(data, slots, estadoPorColaborador,
                    horariosJaGerados, pedido, true, true, minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO, LIMITE_NOS_PESQUISA_EXCECAO);
            ultima = t4;
            if (t4.encontrou()) return t4.atribuicoes();
            registarTentativaLimitada(t4, data, "excecao-operacional");
        }

        lancarFalhaCobertura(data, ultima, estadoPorColaborador.values(), pedido);
        return List.of(); // nunca atingido
    }

    // =========================================================================
    // Execução de uma tentativa de distribuição
    // =========================================================================

    private ResultadoTentativa executarTentativa(LocalDate data,
                                                 List<SlotDia> slots,
                                                 Map<Integer, EstadoColaborador> estadoPorColaborador,
                                                 List<Horario> horariosJaGerados,
                                                 PedidoGeracao pedido,
                                                 boolean ignorarRotacaoFDS,
                                                 boolean ignorarDescansoSemanal,
                                                 int minimoChefiasNoDia,
                                                 int limiteCandidatos,
                                                 int limiteNos) {

        Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot = new LinkedHashMap<>();
        for (SlotDia slot : slots) {
            List<AtribuicaoDia> candidatos = limitarCandidatos(
                    resolverCandidatosOrdenados(data, slot.turnosCompativeis(),
                            estadoPorColaborador.values(), horariosJaGerados, pedido,
                            ignorarRotacaoFDS, ignorarDescansoSemanal),
                    limiteCandidatos);
            candidatosPorSlot.put(slot, candidatos);
        }

        List<SlotDia> slotsOrdenados = ordenarPorEscassez(slots, candidatosPorSlot);
        long candidatosDistintos = candidatosPorSlot.values().stream()
                .flatMap(Collection::stream)
                .map(a -> a.estado().idUtilizador())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        PesquisaContexto ctx = new PesquisaContexto(pedido.prazoLimite(), limiteNos);

        if (candidatosDistintos < slotsOrdenados.size()) {
            return new ResultadoTentativa(false, List.of(), candidatosPorSlot, slotsOrdenados, ctx);
        }

        // Tentativa gulosa (O(n) — rápida)
        List<AtribuicaoDia> gulosa = tentativaGulosa(slotsOrdenados, minimoChefiasNoDia, candidatosPorSlot);
        if (gulosa != null) {
            return new ResultadoTentativa(true, List.copyOf(gulosa), candidatosPorSlot, slotsOrdenados, ctx);
        }

        // Backtracking completo
        List<AtribuicaoDia> atribuicoes = new ArrayList<>();
        boolean encontrou = backtrack(slotsOrdenados, 0, minimoChefiasNoDia, 0,
                candidatosPorSlot, new LinkedHashSet<>(), atribuicoes, ctx);

        return new ResultadoTentativa(encontrou, List.copyOf(atribuicoes), candidatosPorSlot, slotsOrdenados, ctx);
    }

    // =========================================================================
    // Algoritmo de distribuição: greedy + backtracking
    // =========================================================================

    /**
     * Tentativa gulosa O(n): percorre os slots por ordem de escassez e atribui
     * o melhor candidato disponível sem retroceder.
     */
    private List<AtribuicaoDia> tentativaGulosa(List<SlotDia> slotsOrdenados,
                                                int minimoChefiasNoDia,
                                                Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot) {
        Set<Integer> usados = new LinkedHashSet<>();
        List<AtribuicaoDia> atribuicoes = new ArrayList<>();
        int chefias = 0;

        for (int i = 0; i < slotsOrdenados.size(); i++) {
            SlotDia slot = slotsOrdenados.get(i);
            boolean atribuido = false;

            for (AtribuicaoDia candidato : candidatosPorSlot.getOrDefault(slot, List.of())) {
                if (usados.contains(candidato.estado().idUtilizador())) continue;

                usados.add(candidato.estado().idUtilizador());
                atribuicoes.add(candidato);

                int proximasChefias = chefias + (candidato.estado().ehChefiaAoSabado() ? 1 : 0);
                int chefiasRestantes = contarChefiasNosSlotsSeguintes(
                        slotsOrdenados, i + 1, candidatosPorSlot, usados);

                if (proximasChefias + chefiasRestantes >= minimoChefiasNoDia) {
                    chefias = proximasChefias;
                    atribuido = true;
                    break;
                }

                atribuicoes.removeLast();
                usados.remove(candidato.estado().idUtilizador());
            }

            if (!atribuido) return null;
        }

        return chefias >= minimoChefiasNoDia ? atribuicoes : null;
    }

    /**
     * Backtracking recursivo com poda por orçamento de nós, prazo e viabilidade
     * de chefias restantes.
     */
    private boolean backtrack(List<SlotDia> slots,
                              int indice,
                              int minimoChefiasNoDia,
                              int chefiasAcumuladas,
                              Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot,
                              Set<Integer> colaboradoresUsados,
                              List<AtribuicaoDia> atribuicoes,
                              PesquisaContexto ctx) {
        if (indice == slots.size()) {
            return chefiasAcumuladas >= minimoChefiasNoDia;
        }
        if (!ctx.permiteContinuar()) return false;

        SlotDia slot = slots.get(indice);
        for (AtribuicaoDia candidato : candidatosPorSlot.getOrDefault(slot, List.of())) {
            if (colaboradoresUsados.contains(candidato.estado().idUtilizador())) continue;

            int novasChefiasAcumuladas = chefiasAcumuladas
                    + (candidato.estado().ehChefiaAoSabado() ? 1 : 0);
            int chefiasAindaPossiveis = contarChefiasNosSlotsSeguintes(
                    slots, indice + 1, candidatosPorSlot, colaboradoresUsados);

            // Poda: se nem com todas as chefias restantes se satisfaz o mínimo, abandona
            if (novasChefiasAcumuladas + chefiasAindaPossiveis < minimoChefiasNoDia) continue;

            colaboradoresUsados.add(candidato.estado().idUtilizador());
            atribuicoes.add(candidato);

            if (backtrack(slots, indice + 1, minimoChefiasNoDia, novasChefiasAcumuladas,
                    candidatosPorSlot, colaboradoresUsados, atribuicoes, ctx)) {
                return true;
            }

            atribuicoes.removeLast();
            colaboradoresUsados.remove(candidato.estado().idUtilizador());
        }
        return false;
    }

    // =========================================================================
    // Construção de slots
    // =========================================================================

    private List<SlotDia> construirSlots(List<Turno> turnos,
                                         ConfiguracaoDia configDia,
                                         Map<Integer, Integer> minimosPorTurno) {
        Map<String, List<Turno>> porTipo = new LinkedHashMap<>();
        for (Turno t : turnos) {
            porTipo.computeIfAbsent(TurnoClassifier.tipoNormalizado(t), ignored -> new ArrayList<>()).add(t);
        }

        List<SlotDia> slots = new ArrayList<>();
        for (List<Turno> grupo : porTipo.values()) {
            if (grupo.isEmpty()) continue;
            Turno rep = selecionarRepresentativo(grupo);
            int minimo = resolverMinimo(grupo, configDia, minimosPorTurno);
            if (minimo <= 0) throw new IllegalArgumentException(
                    "Regra mínima inválida para um dos turnos.");
            for (int i = 0; i < minimo; i++) {
                slots.add(new SlotDia(rep, List.copyOf(grupo), i));
            }
        }
        return slots;
    }

    private int resolverMinimo(List<Turno> grupo,
                               ConfiguracaoDia configDia,
                               Map<Integer, Integer> minimosPorTurno) {
        if (configDia != null && configDia.minimoColaboradoresTurno() != null) {
            return configDia.minimoColaboradoresTurno();
        }
        return grupo.stream()
                .map(t -> minimosPorTurno.getOrDefault(t.getId(), 0))
                .max(Integer::compareTo)
                .orElse(0);
    }

    private Turno selecionarRepresentativo(List<Turno> grupo) {
        return grupo.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingLong(validator::calcularDuracaoEmMinutos)
                        .thenComparing(Turno::getHoraInicio,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Turno::getHoraFim,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Turno::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não foi possível determinar um turno representativo."));
    }

    // =========================================================================
    // Resolução e ordenação de candidatos (pontuação delegada ao avaliador)
    // =========================================================================

    private List<AtribuicaoDia> resolverCandidatosOrdenados(LocalDate data,
                                                            List<Turno> turnosCompativeis,
                                                            Collection<EstadoColaborador> estados,
                                                            List<Horario> horariosJaGerados,
                                                            PedidoGeracao pedido,
                                                            boolean ignorarRotacaoFDS,
                                                            boolean ignorarDescansoSemanal) {
        AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContexto(horariosJaGerados);

        // Melhor turno (menor pontuação) por colaborador — score calculado uma única vez.
        Map<Integer, CandidatoPontuado> melhorPorColaborador = new LinkedHashMap<>();
        for (Turno turno : turnosCompativeis) {
            long minutos = validator.calcularDuracaoEmMinutos(turno);
            for (EstadoColaborador estado : estados) {
                if (!estado.podeReceber(data, turno, minutos, pedido,
                        ignorarRotacaoFDS, ignorarDescansoSemanal)) continue;

                double score = avaliador.pontuar(estado, turno, minutos, data, pedido, contexto);
                CandidatoPontuado candidato = new CandidatoPontuado(
                        new AtribuicaoDia(estado, turno, minutos), score);
                melhorPorColaborador.merge(estado.idUtilizador(), candidato,
                        (atual, novo) -> novo.score() < atual.score() ? novo : atual);
            }
        }

        return melhorPorColaborador.values().stream()
                .sorted(Comparator.comparingDouble(CandidatoPontuado::score))
                .map(CandidatoPontuado::atribuicao)
                .toList();
    }

    // =========================================================================
    // Chefia ao sábado
    // =========================================================================

    private boolean existeChefiaPossivel(LocalDate data,
                                         List<SlotDia> slots,
                                         Collection<EstadoColaborador> estados,
                                         PedidoGeracao pedido) {
        for (EstadoColaborador estado : estados) {
            if (!estado.ehChefiaAoSabado()) continue;
            for (SlotDia slot : slots) {
                for (Turno t : slot.turnosCompativeis()) {
                    if (estado.podeReceber(data, t,
                            validator.calcularDuracaoEmMinutos(t), pedido, true, false)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // =========================================================================
    // Ordenação de slots por escassez
    // =========================================================================

    private List<SlotDia> ordenarPorEscassez(List<SlotDia> slots,
                                             Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot) {
        List<SlotDia> ordenados = new ArrayList<>(slots);
        ordenados.sort(Comparator
                .comparingInt((SlotDia s) -> candidatosPorSlot.getOrDefault(s, List.of()).size())
                .thenComparing(s -> s.turno().getHoraInicio(),
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return ordenados;
    }

    private List<AtribuicaoDia> limitarCandidatos(List<AtribuicaoDia> candidatos, int limite) {
        if (candidatos == null || candidatos.isEmpty()
                || limite <= 0 || candidatos.size() <= limite) return candidatos;
        return List.copyOf(candidatos.subList(0, limite));
    }

    private int contarChefiasNosSlotsSeguintes(List<SlotDia> slots,
                                               int fromIndex,
                                               Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot,
                                               Set<Integer> usados) {
        int count = 0;
        for (int i = fromIndex; i < slots.size(); i++) {
            boolean temChefiaDisponivel = candidatosPorSlot
                    .getOrDefault(slots.get(i), List.of()).stream()
                    .anyMatch(a -> !usados.contains(a.estado().idUtilizador())
                            && a.estado().ehChefiaAoSabado());
            if (temChefiaDisponivel) count++;
        }
        return count;
    }

    // =========================================================================
    // Validação de prazo
    // =========================================================================

    private void validarPrazo(Instant prazo, LocalDate dataRef, String politica) {
        if (prazo != null && Instant.now().isAfter(prazo)) {
            throw new IllegalArgumentException(
                    "A geração com a política '" + politica
                            + "' ultrapassou o tempo máximo para "
                            + dataRef.getMonthValue() + "/" + dataRef.getYear() + ".");
        }
    }

    // =========================================================================
    // Mensagem e diagnóstico de falha de cobertura
    // =========================================================================

    private void lancarFalhaCobertura(LocalDate data,
                                      ResultadoTentativa ultima,
                                      Collection<EstadoColaborador> estados,
                                      PedidoGeracao pedido) {
        List<SlotDia> slotsOrdenados = ultima.slotsOrdenados();
        SlotDia slotCritico = slotsOrdenados.isEmpty() ? null : slotsOrdenados.getFirst();
        String turnoDesc = slotCritico != null ? formatarTurno(slotCritico.turno()) : "desconhecido";

        LOGGER.warn("Falha ao cobrir turno {} em {}. Candidatos por slot: {}",
                turnoDesc, data,
                ultima.candidatosPorSlot().entrySet().stream()
                        .map(e -> formatarTurno(e.getKey().turno()) + "=" + e.getValue().size())
                        .toList());

        // Diagnóstico explicativo: porque é que cada colaborador ficou de fora do slot crítico.
        List<Turno> turnosCriticos = slotCritico != null ? slotCritico.turnosCompativeis() : List.of();
        DiagnosticoCobertura diagnostico = DiagnosticoCobertura.analisar(
                data, turnosCriticos, estados, pedido, validator);

        throw new FalhaGeracaoHorarioException(
                "Não foi possível gerar o horário: o turno " + turnoDesc
                        + " de " + DATA_FORMATTER.format(data)
                        + " ficou sem colaboradores disponíveis. "
                        + "Foram considerados " + estados.size()
                        + " colaboradores. " + diagnostico.motivoPrincipal(),
                turnoDesc,
                DATA_FORMATTER.format(data),
                estados.size(),
                diagnostico.motivoPrincipal(),
                diagnostico.motivos(),
                diagnostico.sugestoes()
        );
    }

    private void registarTentativaLimitada(ResultadoTentativa t, LocalDate data, String ctx) {
        if (t == null || t.encontrou() || t.contextoPesquisa() == null) return;
        if (t.contextoPesquisa().atingiuLimite() || t.contextoPesquisa().prazoEsgotado()) {
            LOGGER.warn("Tentativa '{}' em {} não concluiu dentro do orçamento (nós={}, prazo={}).",
                    ctx, DATA_FORMATTER.format(data),
                    t.contextoPesquisa().nosExplorados(),
                    t.contextoPesquisa().prazoEsgotado());
        }
    }

    private String formatarTurno(Turno turno) {
        if (turno == null) return "-";
        String inicio = turno.getHoraInicio() != null ? turno.getHoraInicio().toString() : "?";
        String fim    = turno.getHoraFim()    != null ? turno.getHoraFim().toString()    : "?";
        return inicio + "-" + fim;
    }

    // =========================================================================
    // Value objects internos (implementação da pesquisa)
    // =========================================================================

    private record SlotDia(Turno turno, List<Turno> turnosCompativeis, int ordem) {}

    private record AtribuicaoDia(EstadoColaborador estado, Turno turno, long minutosTurno) {}

    private record ResultadoTentativa(
            boolean encontrou,
            List<AtribuicaoDia> atribuicoes,
            Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot,
            List<SlotDia> slotsOrdenados,
            PesquisaContexto contextoPesquisa
    ) {}

    private record CandidatoPontuado(AtribuicaoDia atribuicao, double score) {}

    // Contexto de pesquisa (orçamento de nós e prazo)
    private static final class PesquisaContexto {
        private final Instant prazo;
        private final int limiteNos;
        private int nosExplorados;
        private boolean prazoEsgotado;
        private boolean limiteAtingido;

        PesquisaContexto(Instant prazo, int limiteNos) {
            this.prazo = prazo;
            this.limiteNos = limiteNos;
        }

        boolean permiteContinuar() {
            if (prazo != null && Instant.now().isAfter(prazo)) {
                prazoEsgotado = true;
                return false;
            }
            nosExplorados++;
            if (limiteNos > 0 && nosExplorados > limiteNos) {
                limiteAtingido = true;
                return false;
            }
            return true;
        }

        int nosExplorados()     { return nosExplorados; }
        boolean prazoEsgotado() { return prazoEsgotado; }
        boolean atingiuLimite() { return limiteAtingido; }
    }
}
