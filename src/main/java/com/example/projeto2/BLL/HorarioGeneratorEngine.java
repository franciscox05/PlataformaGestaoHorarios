package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Turno;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
 * Responsabilidade única: dado um conjunto de colaboradores elegíveis,
 * restrições operacionais e preferências, produzir uma lista de {@link Horario}
 * que satisfaça todas as regras de negócio.
 *
 * Algoritmo: pesquisa por backtracking com poda heurística (greedy-first),
 * escalando através de três orçamentos progressivos:
 *   1. Tentativa base     — orçamento reduzido, preferências activas
 *   2. Tentativa alargada — orçamento médio, preferências activas
 *   3. Tentativas de relaxação — rotação / descanso semanal relaxados (só FDS)
 *
 * O motor delega todas as verificações de regras ao {@link HorarioValidatorService},
 * mantendo-se agnóstico em relação à lógica de validação.
 *
 * NOTA: Esta classe foi extraída da GeracaoHorariosBLL como parte da
 * refatorização SRP (Fase 2). A BLL original permanece intacta.
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

    public HorarioGeneratorEngine(HorarioValidatorService validator) {
        this.validator = validator;
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Ponto de entrada principal.
     *
     * @param pedido Objecto imutável com todos os inputs necessários à geração.
     * @return Lista de {@link Horario} gerados (sem ID — ainda não persistidos).
     * @throws IllegalArgumentException se não for possível satisfazer todas as restrições.
     */
    public List<Horario> gerar(PedidoGeracao pedido) {
        Map<Integer, EstadoColaborador> estadoPorColaborador = inicializarEstados(pedido);
        inicializarHistorico(estadoPorColaborador, pedido.historicoHorarios());

        List<Horario> horarios       = new ArrayList<>();
        Set<LocalDate> diasCobertos  = new LinkedHashSet<>();
        Set<LocalDate> sabadosComChefia = new LinkedHashSet<>();
        Map<LocalDate, ReservaFimDeSemana> reservasFDS = new HashMap<>();

        for (LocalDate data = pedido.dataInicio(); !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
            validarPrazo(pedido.prazoLimite(), pedido.dataInicio(), pedido.politica());

            ConfiguracaoDia configDia = pedido.configuracoesPorData().get(data);
            if (configDia != null && configDia.lojaEncerrada()) {
                continue;
            }

            LocalDate inicioSemana = validator.inicioSemana(data);
            ReservaFimDeSemana reservaFDS = reservasFDS.computeIfAbsent(
                    inicioSemana,
                    ignored -> identificarReservaFimDeSemana(estadoPorColaborador.values(),
                            pedido, inicioSemana)
            );

            ContextoPreservacaoFDS contextoFDS = construirContextoPreservacaoFDS(
                    data, pedido.dataFim(), pedido.turnos(), pedido, configDia);

            List<Turno> turnosDoDia = configDia != null ? configDia.turnosCompativeis() : pedido.turnos();
            if (turnosDoDia.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nao existem turnos compativeis com a excecao em " + data + ".");
            }

            List<AtribuicaoDia> atribuicoes = planearDia(
                    data, turnosDoDia, configDia, estadoPorColaborador,
                    reservaFDS, contextoFDS, horarios,
                    sabadosComChefia.contains(data), pedido);

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
                diasCobertos.add(data);
            }
        }
        return horarios;
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
                    new EstadoColaborador(ligacao, cargaMaxima, pedido.chefiasSabadoIds()));
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
                                           ReservaFimDeSemana reservaFDS,
                                           ContextoPreservacaoFDS contextoFDS,
                                           List<Horario> horariosJaGerados,
                                           boolean sabadoJaTemChefia,
                                           PedidoGeracao pedido) {

        List<SlotDia> slots = construirSlots(turnosDoDia, configDia, pedido.minimosPorTurno());

        boolean precisaChefia = pedido.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && !sabadoJaTemChefia;

        if (precisaChefia && !existeChefiaPossivel(data, slots, estadoPorColaborador.values(),
                pedido, reservaFDS)) {
            throw new IllegalArgumentException(
                    "Nao foi possivel garantir presenca de gerente/subgerente no sabado "
                            + DATA_FORMATTER.format(data) + ".");
        }

        int minimoChefiasNoDia = (validator.ehFimDeSemana(data) || precisaChefia) ? 1 : 0;

        // Tentativa 1: base
        ResultadoTentativa t1 = executarTentativa(data, slots, estadoPorColaborador, reservaFDS,
                contextoFDS, horariosJaGerados, pedido,
                true, false, false, minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_BASE, LIMITE_NOS_PESQUISA_BASE);
        if (t1.encontrou()) return t1.atribuicoes();
        registarTentativaLimitada(t1, data, "base");

        // Tentativa 2: alargada
        ResultadoTentativa t2 = executarTentativa(data, slots, estadoPorColaborador, reservaFDS,
                contextoFDS, horariosJaGerados, pedido,
                true, false, false, minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_ALARGADO, LIMITE_NOS_PESQUISA_ALARGADO);
        if (t2.encontrou()) return t2.atribuicoes();
        registarTentativaLimitada(t2, data, "alargada");

        ResultadoTentativa ultima = t2;

        // Tentativa 3: sem preservação FDS (se aplicável)
        boolean semCandidatosNalgumSlot = t1.candidatosPorSlot().values().stream().anyMatch(List::isEmpty);
        if (contextoFDS != null && (validator.ehFimDeSemana(data) || semCandidatosNalgumSlot)) {
            ResultadoTentativa t3 = executarTentativa(data, slots, estadoPorColaborador, reservaFDS,
                    null, horariosJaGerados, pedido,
                    false, false, false, minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_ALARGADO, LIMITE_NOS_PESQUISA_ALARGADO);
            ultima = t3;
            if (t3.encontrou()) return t3.atribuicoes();
            registarTentativaLimitada(t3, data, "sem-preservacao");
        }

        // Tentativa 4: rotação FDS relaxada
        if (validator.ehFimDeSemana(data) && pedido.janelaRotacaoFimDeSemana() >= 2) {
            ResultadoTentativa t4 = executarTentativa(data, slots, estadoPorColaborador, reservaFDS,
                    null, horariosJaGerados, pedido,
                    false, true, false, minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO, LIMITE_NOS_PESQUISA_EXCECAO);
            ultima = t4;
            if (t4.encontrou()) return t4.atribuicoes();
            registarTentativaLimitada(t4, data, "rotacao-relaxada");
        }

        // Tentativa 5: descanso semanal relaxado (FDS)
        if (validator.ehFimDeSemana(data)) {
            ResultadoTentativa t5 = executarTentativa(data, slots, estadoPorColaborador, reservaFDS,
                    null, horariosJaGerados, pedido,
                    false, true, true, minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO, LIMITE_NOS_PESQUISA_EXCECAO);
            ultima = t5;
            if (t5.encontrou()) return t5.atribuicoes();
            registarTentativaLimitada(t5, data, "excecao-operacional");
        }

        // Todas as tentativas falharam
        lancarFalhaCobertura(data, ultima, estadoPorColaborador.values(), pedido, reservaFDS);
        return List.of(); // nunca atingido
    }

    // =========================================================================
    // Execução de uma tentativa de distribuição
    // =========================================================================

    private ResultadoTentativa executarTentativa(LocalDate data,
                                                 List<SlotDia> slots,
                                                 Map<Integer, EstadoColaborador> estadoPorColaborador,
                                                 ReservaFimDeSemana reservaFDS,
                                                 ContextoPreservacaoFDS contextoFDS,
                                                 List<Horario> horariosJaGerados,
                                                 PedidoGeracao pedido,
                                                 boolean aplicarPreservacaoFDS,
                                                 boolean ignorarRotacaoFDS,
                                                 boolean ignorarDescansoSemanal,
                                                 int minimoChefiasNoDia,
                                                 int limiteCandidatos,
                                                 int limiteNos) {

        Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot = new LinkedHashMap<>();
        for (SlotDia slot : slots) {
            List<AtribuicaoDia> candidatos = limitarCandidatos(
                    resolverCandidatosOrdenados(data, slot.turnosCompativeis(),
                            estadoPorColaborador.values(), reservaFDS,
                            aplicarPreservacaoFDS ? contextoFDS : null,
                            horariosJaGerados, pedido,
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
        List<AtribuicaoDia> gulosa = tentativaGulosa(slotsOrdenados, minimoChefiasNoDia,
                candidatosPorSlot);
        if (gulosa != null) {
            return new ResultadoTentativa(true, List.copyOf(gulosa), candidatosPorSlot, slotsOrdenados, ctx);
        }

        // Backtracking completo
        List<AtribuicaoDia> atribuicoes = new ArrayList<>();
        boolean encontrou = backtrack(slotsOrdenados, 0, minimoChefiasNoDia, 0,
                candidatosPorSlot, new LinkedHashSet<>(), atribuicoes, ctx);

        return new ResultadoTentativa(encontrou, List.copyOf(atribuicoes),
                candidatosPorSlot, slotsOrdenados, ctx);
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
     * Backtracking recursivo com poda por:
     *   - orçamento de nós explorados
     *   - prazo de tempo
     *   - viabilidade de chefias restantes
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
            porTipo.computeIfAbsent(normalizarTurno(t), ignored -> new ArrayList<>()).add(t);
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
    // Resolução e ordenação de candidatos
    // =========================================================================

        private List<AtribuicaoDia> resolverCandidatosOrdenados(LocalDate data,
                                                            List<Turno> turnosCompativeis,
                                                            Collection<EstadoColaborador> estados,
                                                            ReservaFimDeSemana reservaFDS,
                                                            ContextoPreservacaoFDS contextoFDS,
                                                            List<Horario> horariosJaGerados,
                                                            PedidoGeracao pedido,
                                                            boolean ignorarRotacaoFDS,
                                                            boolean ignorarDescansoSemanal) {
        Map<Integer, AtribuicaoDia> melhorPorColaborador = new LinkedHashMap<>();
        for (Turno turno : turnosCompativeis) {
            long minutos = validator.calcularDuracaoEmMinutos(turno);
            for (EstadoColaborador estado : estados) {
                if (!estado.podeReceber(data, turno, minutos, pedido,
                        ignorarRotacaoFDS, ignorarDescansoSemanal)) continue;

                AtribuicaoDia proposta = new AtribuicaoDia(estado, turno, minutos);
                melhorPorColaborador.merge(estado.idUtilizador(), proposta,
                        (atual, novo) -> pontuarAtribuicao(novo, data, pedido, reservaFDS,
                                horariosJaGerados)
                                < pontuarAtribuicao(atual, data, pedido, reservaFDS,
                                horariosJaGerados) ? novo : atual);
            }
        }
        List<AtribuicaoDia> lista = new ArrayList<>(melhorPorColaborador.values());
        lista.sort(Comparator.comparingDouble(a -> pontuarAtribuicao(
                a, data, pedido, reservaFDS, horariosJaGerados)));
        return lista;
    }

    /**
     * Pontuação heurística de uma atribuição (menor = melhor candidato).
     * Critérios (por ordem de prioridade):
     *   1. Chefia obrigatória ao sábado (promovida)
     *   2. Utilização contratual projectada (equilíbrio de carga)
     *   3. Fins de semana trabalhados (rotação)
     *   4. Preferências de turno satisfeitas (qualidade)
     */
    private double pontuarAtribuicao(AtribuicaoDia a,
                                     LocalDate data,
                                     PedidoGeracao pedido,
                                     ReservaFimDeSemana reservaFDS,
                                     List<Horario> horariosJaGerados) {
        double pontuacao = 0;

        // Chefia: promover quem cumpre requisito de sábado
        if (pedido.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && a.estado().ehChefiaAoSabado()) {
            pontuacao -= 1000;
        }

        // Utilização contratual: preferir quem tem menos carga proporcional
        pontuacao += a.estado().utilizacaoProjetada(a.minutosTurno()) * 100;

        // Rotação fins de semana: preferir quem trabalhou menos FDS
        pontuacao += a.estado().totalFimDeSemanaTrabalhados() * 20;
        if (validator.ehFimDeSemana(data)) {
            if (a.estado().trabalhouFimDeSemanaAnterior(data)) {
                pontuacao += a.estado().ehApenasFimDeSemana() ? 220 : 140;
            } else if (a.estado().ehApenasFimDeSemana()) {
                // Usar reforco ao fim de semana preserva carga dos restantes perfis para dias uteis.
                pontuacao -= 25;
            }
        }

        // Preferências: recompensar correspondência
        if (temPreferenciaTurnoFavoravel(a.estado().idUtilizador(),
                a.turno(), data, pedido.preferenciasTurnos())) {
            pontuacao -= 30;
        }

        return pontuacao;
    }

    // =========================================================================
    // Chefia ao sábado
    // =========================================================================

    private boolean existeChefiaPossivel(LocalDate data,
                                         List<SlotDia> slots,
                                         Collection<EstadoColaborador> estados,
                                         PedidoGeracao pedido,
                                         ReservaFimDeSemana reservaFDS) {
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
    // Reserva operacional de fim de semana
    // =========================================================================

    private ReservaFimDeSemana identificarReservaFimDeSemana(Collection<EstadoColaborador> estados,
                                                              PedidoGeracao pedido,
                                                              LocalDate inicioSemana) {
        LocalDate sabado = inicioSemana.with(DayOfWeek.SATURDAY);
        LocalDate domingo = inicioSemana.with(DayOfWeek.SUNDAY);
        if (sabado.isAfter(pedido.dataFim())) return ReservaFimDeSemana.vazia();

        Set<Integer> nucleares = new LinkedHashSet<>();
        Set<Integer> apoio = new LinkedHashSet<>();

        for (EstadoColaborador estado : estados) {
            Set<LocalDate> bloqueios = pedido.bloqueiosPorColaborador()
                    .getOrDefault(estado.idUtilizador(), Set.of());

            if (bloqueios.contains(sabado)) continue;
            if (!estado.podeCobrir(sabado, domingo, pedido, bloqueios)) continue;

            if (estado.ehChefiaAoSabado()) {
                nucleares.add(estado.idUtilizador());
            } else {
                apoio.add(estado.idUtilizador());
            }
        }
        return new ReservaFimDeSemana(nucleares, apoio);
    }

    // =========================================================================
    // Contexto de preservação de fim de semana
    // =========================================================================

    private ContextoPreservacaoFDS construirContextoPreservacaoFDS(LocalDate data,
                                                                    LocalDate dataFim,
                                                                    List<Turno> turnosBase,
                                                                    PedidoGeracao pedido,
                                                                    ConfiguracaoDia configDia) {
        LocalDate sabado = data.with(DayOfWeek.SATURDAY);
        if (sabado.isAfter(dataFim) || data.getDayOfWeek() == DayOfWeek.SUNDAY) return null;

        LocalDate domingo = sabado.plusDays(1);
        List<Turno> turnosSabado = (configDia != null && data.getDayOfWeek() == DayOfWeek.SATURDAY)
                ? (configDia.turnosCompativeis().isEmpty() ? turnosBase : configDia.turnosCompativeis())
                : turnosBase;
        List<Turno> turnosDomingo = domingo.isAfter(dataFim) ? List.of() : turnosBase;

        int minimoSabado = turnosSabado.stream()
                .mapToInt(t -> pedido.minimosPorTurno().getOrDefault(t.getId(), 1))
                .max().orElse(1);
        int minimoDomingo = turnosDomingo.stream()
                .mapToInt(t -> pedido.minimosPorTurno().getOrDefault(t.getId(), 1))
                .max().orElse(0);

        return new ContextoPreservacaoFDS(sabado, turnosSabado, minimoSabado,
                domingo, turnosDomingo, minimoDomingo,
                minimoSabado + minimoDomingo);
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
    // Preferências
    // =========================================================================

    private boolean temPreferenciaTurnoFavoravel(Integer idColaborador,
                                                  Turno turno,
                                                  LocalDate data,
                                                  Map<Integer, List<Preferencia>> preferenciasTurnos) {
        List<Preferencia> prefs = preferenciasTurnos.getOrDefault(idColaborador, List.of());
        if (prefs.isEmpty()) return false;
        long duracaoMinutos = validator.calcularDuracaoEmMinutos(turno);
        for (Preferencia p : prefs) {
            if (p.getDataInicio() != null && !data.isBefore(p.getDataInicio())
                    && (p.getDataFim() == null || !data.isAfter(p.getDataFim()))) {
                String desc = p.getDescricao() != null ? p.getDescricao().toLowerCase() : "";
                boolean exigeCurto = desc.contains("curto");
                boolean exigeLongo = desc.contains("longo");
                boolean exigeManha = desc.contains("manha");
                boolean exigeTarde = desc.contains("tarde");
                boolean exigeIntermedio = desc.contains("intermedio");

                boolean temFiltroDuracao = exigeCurto || exigeLongo;
                boolean temFiltroPeriodo = exigeManha || exigeTarde || exigeIntermedio;

                boolean correspondeDuracao = !temFiltroDuracao
                        || (exigeCurto && duracaoMinutos < 300)
                        || (exigeLongo && duracaoMinutos >= 300);

                boolean correspondePeriodo = !temFiltroPeriodo || correspondePeriodo(turno, exigeManha, exigeTarde, exigeIntermedio);

                if (correspondeDuracao && correspondePeriodo) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean correspondePeriodo(Turno turno,
                                       boolean exigeManha,
                                       boolean exigeTarde,
                                       boolean exigeIntermedio) {
        String tipoNormalizado = normalizarTurno(turno);
        LocalTime horaInicio = turno.getHoraInicio();

        if (exigeManha && ("manha".equals(tipoNormalizado)
                || (horaInicio != null && horaInicio.isBefore(LocalTime.of(10, 0))))) {
            return true;
        }

        if (exigeIntermedio && ("intermedio".equals(tipoNormalizado)
                || (horaInicio != null
                && !horaInicio.isBefore(LocalTime.of(10, 0))
                && horaInicio.isBefore(LocalTime.NOON)))) {
            return true;
        }

        if (exigeTarde && ("tarde".equals(tipoNormalizado)
                || (horaInicio != null && !horaInicio.isBefore(LocalTime.NOON)))) {
            return true;
        }

        return false;
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
    // Normalização de turno
    // =========================================================================

    private String normalizarTurno(Turno turno) {
        if (turno == null) return "desconhecido";
        String tipo = turno.getTipo() != null ? turno.getTipo().toString().toLowerCase() : "";
        LocalTime inicio = turno.getHoraInicio();
        if (!tipo.isBlank()) return tipo;
        if (inicio == null) return "desconhecido";
        if (inicio.isBefore(LocalTime.of(10, 0))) return "manha";
        if (inicio.isBefore(LocalTime.NOON)) return "intermedio";
        if (inicio.isBefore(LocalTime.of(17, 0))) return "tarde";
        return "noite";
    }

    // =========================================================================
    // Mensagem de falha de cobertura
    // =========================================================================

    private void lancarFalhaCobertura(LocalDate data,
                                      ResultadoTentativa ultima,
                                      Collection<EstadoColaborador> estados,
                                      PedidoGeracao pedido,
                                      ReservaFimDeSemana reservaFDS) {
        List<SlotDia> slotsOrdenados = ultima.slotsOrdenados();
        SlotDia slotCritico = slotsOrdenados.isEmpty() ? null : slotsOrdenados.getFirst();
        String turnoDesc = slotCritico != null ? formatarTurno(slotCritico.turno()) : "desconhecido";

        LOGGER.warn("Falha ao cobrir turno {} em {}. Candidatos por slot: {}",
                turnoDesc, data,
                ultima.candidatosPorSlot().entrySet().stream()
                        .map(e -> formatarTurno(e.getKey().turno())
                                + "=" + e.getValue().size())
                        .toList());

        throw new GeracaoHorariosBLL.FalhaGeracaoHorarioException(
                "Não foi possível gerar o horário: o turno " + turnoDesc
                        + " de " + DATA_FORMATTER.format(data)
                        + " ficou sem colaboradores disponíveis. "
                        + "Foram considerados " + estados.size()
                        + " colaboradores. Revê folgas, limites de horas e mínimos por turno.",
                turnoDesc,
                DATA_FORMATTER.format(data),
                estados.size(),
                "Sem candidatos suficientes para o slot mais crítico.",
                List.of(),
                List.of()
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
    // Value objects internos
    // =========================================================================

    /**
     * Input imutável para o motor: tudo o que o algoritmo precisa, sem
     * dependências de repositórios.
     */
    public record PedidoGeracao(
            List<Lojautilizador>                    colaboradores,
            List<Turno>                             turnos,
            LocalDate                               dataInicio,
            LocalDate                               dataFim,
            Map<Integer, Integer>                   minimosPorTurno,
            int                                     maxDiasConsecutivos,
            int                                     descansoMinimoHoras,
            int                                     descansoSemanalMinimoDias,
            int                                     janelaRotacaoFimDeSemana,
            boolean                                 exigirChefiaAoSabado,
            Set<Integer>                            chefiasSabadoIds,
            Map<Integer, Long>                      cargaMaximaPorColaborador,
            Map<Integer, Set<LocalDate>>            bloqueiosPorColaborador,
            Map<Integer, List<Preferencia>>         preferenciasTurnos,
            Map<LocalDate, ConfiguracaoDia>         configuracoesPorData,
            List<Horario>                           historicoHorarios,
            Instant                                 prazoLimite,
            String                                  politica
    ) {}

    /** Configuração especial para um dia (feriado, horário especial de loja). */
    public record ConfiguracaoDia(
            boolean     lojaEncerrada,
            List<Turno> turnosCompativeis,
            Integer     minimoColaboradoresTurno,
            String      descricao
    ) {}

    private record SlotDia(Turno turno, List<Turno> turnosCompativeis, int ordem) {}

    private record AtribuicaoDia(EstadoColaborador estado, Turno turno, long minutosTurno) {}

    private record ResultadoTentativa(
            boolean encontrou,
            List<AtribuicaoDia> atribuicoes,
            Map<SlotDia, List<AtribuicaoDia>> candidatosPorSlot,
            List<SlotDia> slotsOrdenados,
            PesquisaContexto contextoPesquisa
    ) {}

    /** Reserva de colaboradores para cobertura de fim de semana. */
    private record ReservaFimDeSemana(Set<Integer> nucleares, Set<Integer> apoio) {
        static ReservaFimDeSemana vazia() {
            return new ReservaFimDeSemana(Set.of(), Set.of());
        }
        boolean ehNuclear(Integer id) {
            return id != null && nucleares.contains(id);
        }
    }

    /** Contexto para preservação de capacidade no fim de semana. */
    private record ContextoPreservacaoFDS(
            LocalDate sabado, List<Turno> turnosSabado, int minimoSabado,
            LocalDate domingo, List<Turno> turnosDomingo, int minimoDomingo,
            int minimoFimDeSemanacompleto
    ) {}

    // =========================================================================
    // Contexto de pesquisa (orçamento de nós e prazo)
    // =========================================================================

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

        int nosExplorados()   { return nosExplorados; }
        boolean prazoEsgotado() { return prazoEsgotado; }
        boolean atingiuLimite() { return limiteAtingido; }
    }

    // =========================================================================
    // Estado de um colaborador durante a geração
    // =========================================================================

    /**
     * Estado mutável de um colaborador ao longo da geração.
     * Encapsula a lógica de elegibilidade delegando as regras ao
     * {@link HorarioValidatorService}.
     */
    final class EstadoColaborador {
        private final Lojautilizador ligacao;
        private final long cargaMaximaMinutos;
        private final boolean chefiaAoSabado;
        private final boolean apenasFimDeSemana;
        private final boolean exigeTurnoMinimoOitoHoras;

        private LocalDate ultimaDataAtribuida;
        private int diasConsecutivos;
        private long minutosAtribuidos;
        private final Map<LocalDate, Turno> atribuicoesConhecidas = new HashMap<>();
        private final Map<LocalDate, Integer> diasTrabalhadosPorSemana = new HashMap<>();
        private final Map<LocalDate, LocalDate> ultimoFimDeSemana = new HashMap<>();
        private int totalFimDeSemanaTrabalhados;

        EstadoColaborador(Lojautilizador ligacao,
                          long cargaMaximaMinutos,
                          Set<Integer> chefiasSabadoIds) {
            this.ligacao = ligacao;
            this.cargaMaximaMinutos = cargaMaximaMinutos;
            this.chefiaAoSabado = ligacao.getIdUtilizador() != null
                    && chefiasSabadoIds.contains(ligacao.getIdUtilizador().getId());
            String tipoCargo = ligacao.getIdCargo() != null ? normalizarCargo(ligacao.getIdCargo().getTipo()) : "";
            this.apenasFimDeSemana = "reforco_parttime".equals(tipoCargo);
            this.exigeTurnoMinimoOitoHoras = "fulltime".equals(tipoCargo)
                    || "gerente".equals(tipoCargo)
                    || "subgerente".equals(tipoCargo)
                    || "supervisor".equals(tipoCargo);
        }

        Integer idUtilizador() {
            return ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getId() : null;
        }

        Lojautilizador ligacao() { return ligacao; }

        boolean ehChefiaAoSabado() { return chefiaAoSabado; }

        boolean ehApenasFimDeSemana() { return apenasFimDeSemana; }

        double utilizacaoProjetada(long minutosTurno) {
            if (cargaMaximaMinutos <= 0) return Double.MAX_VALUE;
            return (minutosAtribuidos + minutosTurno) / (double) cargaMaximaMinutos;
        }

        int totalFimDeSemanaTrabalhados() { return totalFimDeSemanaTrabalhados; }

        boolean podeReceber(LocalDate data,
                            Turno turno,
                            long minutosTurno,
                            PedidoGeracao pedido,
                            boolean ignorarRotacaoFDS,
                            boolean ignorarDescansoSemanal) {

            Set<LocalDate> bloqueios = pedido.bloqueiosPorColaborador()
                    .getOrDefault(idUtilizador(), Set.of());

            if ((apenasFimDeSemana && !validator.ehFimDeSemana(data))
                    || (exigeTurnoMinimoOitoHoras && minutosTurno < 8 * 60L)
                    || bloqueios.contains(data)
                    || atribuicoesConhecidas.containsKey(data)
                    || (minutosAtribuidos + minutosTurno) > cargaMaximaMinutos) {
                return false;
            }

            if (!ignorarDescansoSemanal) {
                int diasNaSemana = diasTrabalhadosPorSemana
                        .getOrDefault(validator.inicioSemana(data), 0);
                if (validator.excedeDiasTrabalhadosNaSemana(
                        data, diasNaSemana, pedido.descansoSemanalMinimoDias())) {
                    return false;
                }
            }

            if (!ignorarRotacaoFDS && validator.ehFimDeSemana(data)) {
                LocalDate ultimoFDS = ultimoFimDeSemanaInicio();
                if (validator.violaRotacaoDeFimDeSemana(data,
                        totalFimDeSemanaTrabalhados, ultimoFDS,
                        pedido.janelaRotacaoFimDeSemana())) {
                    return false;
                }
            }

            if (validator.violaMaximoDiasConsecutivos(ultimaDataAtribuida,
                    diasConsecutivos, data, pedido.maxDiasConsecutivos())) {
                return false;
            }

            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                Turno turnoAnterior = atribuicoesConhecidas.get(ultimaDataAtribuida);
                if (!validator.respeitaDescansoMinimo(ultimaDataAtribuida, turnoAnterior,
                        data, turno, pedido.descansoMinimoHoras())) {
                    return false;
                }
            }

            return true;
        }

        boolean podeCobrir(LocalDate sabado,
                           LocalDate domingo,
                           PedidoGeracao pedido,
                           Set<LocalDate> bloqueios) {
            if (bloqueios.contains(sabado) || atribuicoesConhecidas.containsKey(sabado)) {
                return false;
            }
            if (domingo == null) return true;
            return !bloqueios.contains(domingo) && !atribuicoesConhecidas.containsKey(domingo);
        }

        void registarAtribuicao(LocalDate data, Turno turno, long minutosTurno) {
            if (data == null || turno == null || atribuicoesConhecidas.containsKey(data)) return;

            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                diasConsecutivos++;
            } else {
                diasConsecutivos = 1;
            }
            ultimaDataAtribuida = data;
            atribuicoesConhecidas.put(data, turno);
            diasTrabalhadosPorSemana.merge(validator.inicioSemana(data), 1, Integer::sum);

            if (validator.ehFimDeSemana(data)) {
                totalFimDeSemanaTrabalhados++;
                ultimoFimDeSemana.put(validator.inicioFimDeSemana(data),
                        validator.inicioFimDeSemana(data));
            }
            minutosAtribuidos += minutosTurno;
        }

        void inicializarComHistorico(List<Horario> historico) {
            for (Horario h : historico) {
                if (h.getDataTurno() == null || h.getIdTurno() == null) continue;
                registarAtribuicao(h.getDataTurno(), h.getIdTurno(), 0);
            }
        }

        private LocalDate ultimoFimDeSemanaInicio() {
            return ultimoFimDeSemana.values().stream()
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }

        private boolean trabalhouFimDeSemanaAnterior(LocalDate data) {
            if (data == null || !validator.ehFimDeSemana(data)) {
                return false;
            }
            LocalDate fimDeSemanaAtual = validator.inicioFimDeSemana(data);
            return ultimoFimDeSemana.containsKey(fimDeSemanaAtual.minusWeeks(1));
        }

        private String normalizarCargo(String tipoCargo) {
            if (tipoCargo == null) {
                return "";
            }
            return tipoCargo.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
