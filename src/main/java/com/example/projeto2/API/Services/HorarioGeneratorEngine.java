package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.ConfiguracaoDia;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.FalhaGeracaoHorarioException;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
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

    public HorarioGeneratorEngine(HorarioValidatorService validator) {
        this.validator = validator;
        this.avaliador = new AvaliadorAtribuicao(validator);
        this.refinador = new RefinadorPlaneamento(validator);
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
        // Fase final: refinamento por pesquisa local (folgas preferidas + equilíbrio de carga)
        return refinador.refinar(pedido, horarios);
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
