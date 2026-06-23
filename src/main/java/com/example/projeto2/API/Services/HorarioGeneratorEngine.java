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
import com.example.projeto2.API.Services.geracao.PolisherHorario;
import com.example.projeto2.API.Services.geracao.RefinadorPlaneamento;
import com.example.projeto2.API.Services.geracao.SugestaoFalhaGeracao;
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
import java.util.Random;
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

    // Multi-start: desvio-padrão do ruído gaussiano adicionado aos scores do greedy
    // para explorar soluções alternativas. Pequeno o suficiente para não anular
    // preferências fortes (diferença típica > 10 pts), mas suficiente para criar
    // diversidade via desempate estocástico.
    private static final double SIGMA_RUIDO_GREEDY = 3.0;

    // RNG de ruído por thread — null no greedy base (sem ruído), instanciado nas
    // tentativas adicionais. ThreadLocal garante thread-safety num serviço singleton.
    private static final ThreadLocal<Random> GREEDY_NOISE_RND = new ThreadLocal<>();

    private final HorarioValidatorService validator;
    private final AvaliadorAtribuicao avaliador;
    private final RefinadorPlaneamento refinador;
    private final PolisherHorario polisher;
    private final PlaneadorFolgasPreferidas planeadorFolgas;
    private final PlaneadorFinsDeSemana planeadorFins;

    public HorarioGeneratorEngine(HorarioValidatorService validator) {
        this.validator = validator;
        this.avaliador = new AvaliadorAtribuicao(validator);
        this.refinador = new RefinadorPlaneamento(validator);
        this.polisher = new PolisherHorario(validator);
        this.planeadorFolgas = new PlaneadorFolgasPreferidas(validator);
        this.planeadorFins = new PlaneadorFinsDeSemana(validator);
    }

    // =========================================================================
    // API pública
    // =========================================================================

    /**
     * Ponto de entrada principal.
     *
     * <p>Executa o pipeline de geração até 3 vezes com ruído controlado no greedy
     * (multi-start estocástico), mantendo o melhor resultado por custo global.
     * A primeira tentativa é o baseline sem ruído — determinístico e reproduzível.
     * As tentativas adicionais só correm em produção (prazoLimite != null) quando
     * há tempo suficiente; se esgotarem o prazo, o melhor resultado anterior é mantido.
     *
     * @param pedido inputs imutáveis da geração.
     * @return lista de {@link Horario} gerados (sem ID — ainda não persistidos).
     * @throws IllegalArgumentException se não for possível satisfazer todas as restrições.
     */
    public List<Horario> gerar(PedidoGeracao pedido) {
        validarEntradasBasicas(pedido);
        validarCapacidadeGlobal(pedido);

        // Tentativa 0: baseline sem ruído (determinístico — semente para o jitter do avaliador)
        List<Horario> melhor = executarUmaGeracao(pedido, 0L);

        // Tentativas adicionais com ruído para explorar soluções alternativas.
        // Só em produção (prazoLimite definido) e apenas se há ≥ 4 s de margem.
        if (pedido.prazoLimite() != null) {
            long[] sementesExtras = {
                pedido.semente() ^ 0x9e3779b97f4a7c15L,
                pedido.semente() ^ 0x6c62272e07bb0142L
            };
            double melhorCusto = polisher.avaliarCusto(pedido, melhor);
            for (long sementeExtra : sementesExtras) {
                if (prazoProximo(pedido)) break;
                try {
                    List<Horario> resultado = executarUmaGeracao(pedido, sementeExtra);
                    double custo = polisher.avaliarCusto(pedido, resultado);
                    if (custo < melhorCusto) {
                        melhor = resultado;
                        melhorCusto = custo;
                        LOGGER.debug("Multi-start: custo melhorado para {}.", Math.round(custo));
                    }
                } catch (IllegalArgumentException e) {
                    // Prazo esgotado na tentativa extra — mantém o melhor resultado até agora.
                    break;
                }
            }
        }
        return melhor;
    }

    /**
     * Executa uma passagem completa do pipeline de geração (greedy → top-up → refinamento
     * → polimento). Quando {@code sementeGreedy != 0}, ativa um ruído gaussiano controlado
     * no avaliador de candidatos para explorar regiões do espaço de soluções diferentes
     * do baseline determinístico.
     */
    private List<Horario> executarUmaGeracao(PedidoGeracao pedido, long sementeGreedy) {
        if (sementeGreedy != 0L) {
            GREEDY_NOISE_RND.set(new Random(sementeGreedy));
        }
        try {
            return executarPipeline(pedido, sementeGreedy);
        } finally {
            GREEDY_NOISE_RND.remove();
        }
    }

    private List<Horario> executarPipeline(PedidoGeracao pedido, long sementeGreedy) {
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
        // A sementeGreedy (≠0 nas tentativas extra do multi-start) baralha a ordem de
        // designação dos FDS, criando designações alternativas para explorar.
        PlanoFinsDeSemana planoFins = planeadorFins.planear(pedido, sementeGreedy);
        if (planoFins.ativo()) {
            estadoPorColaborador.forEach((id, estado) ->
                    estado.designarFinsDeSemana(planoFins.designados(id), planoFins.chefiaEm(id)));
        }

        List<Horario> horarios = new ArrayList<>();
        Set<LocalDate> sabadosComChefia = new LinkedHashSet<>();

        // ── FASE 1 — Cobertura mínima de TODOS os dias do período ──────────────
        // Garante a cobertura obrigatória do mês inteiro ANTES de qualquer top-up.
        // Antes, o top-up era interleaved (mínimo do dia 1 → top-up do dia 1 → mínimo
        // do dia 2 → ...): os turnos extra do início do mês esgotavam a carga das
        // pessoas necessárias no fim do mês, e dias como o 30 ficavam sem candidatos,
        // mesmo havendo capacidade total de sobra. Separar as fases garante que a
        // capacidade é gasta primeiro no que é obrigatório, e só a folga sobrante
        // alimenta o top-up.
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
            // Completa imediatamente o 2º turno consecutivo para FT: assim a capacidade
            // é consumida em pares (8h/dia) e não em singles espalhados por muitos dias.
            completarTurnosFullTimeDia(data, turnosDoDia, pedido, horarios,
                    estadoPorColaborador, sabadosComChefia);
        }

        // ── FASE 1.5 — Sweep final para FT com 1 turno ainda por completar ────
        completarTurnosFullTime(pedido, horarios, estadoPorColaborador, sabadosComChefia);

        // ── FASE 2 — Preenchimento de capacidade (top-up) ─────────────────────
        // Com todos os mínimos garantidos, distribui a capacidade contratual ainda por
        // consumir. Dois modos:
        //  • COM alvo de pessoas por turno (gestor escolheu): o alvo é tratado como MÍNIMO
        //    RÍGIDO já na FASE 1 (ver PedidoGeracao#minimosEfetivosPorTurno) — todos os dias
        //    abertos saem da FASE 1 já com o alvo cumprido, ou a geração falhou no pré-flight
        //    de capacidade. Aqui, "levanta o piso" é só uma rede de segurança que não acrescenta
        //    nada (cobertura == alvo em todo o lado), garantindo nunca exceder o alvo.
        //  • SEM alvo: enche até à capacidade com orçamento diário acumulado (ver abaixo).
        if (pedido.alvoPorTurno() != null) {
            preencherAteAlvoUniforme(pedido, horarios, estadoPorColaborador.values(),
                    sabadosComChefia, pedido.alvoPorTurno());
        } else {
            // Orçamento ACUMULADO a uma taxa diária constante: a folga de capacidade que
            // sobrou da fase 1 reparte-se por igual pelos dias do mês. Sem a acumulação, um
            // orçamento diário menor que a duração de um turno (equipas pequenas) nunca
            // chegava para um turno extra cedo no mês — empurrando o reforço todo para o fim.
            // O teto contratual de cada colaborador (podeReceber) garante que o total nunca
            // excede a capacidade real da equipa.
            long capacidadeReforco = estadoPorColaborador.values().stream()
                    .mapToLong(EstadoColaborador::capacidadeRestanteMinutos)
                    .sum();
            long diasTopUp = 0;
            for (LocalDate data = pedido.dataInicio(); !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
                if (pedido.configuracoesPorData().get(data) == null) {
                    diasTopUp++;
                }
            }
            double taxaDiariaReforco = diasTopUp > 0 ? capacidadeReforco / (double) diasTopUp : 0;
            double orcamentoAcumulado = 0;

            for (LocalDate data = pedido.dataInicio(); !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
                ConfiguracaoDia configDia = pedido.configuracoesPorData().get(data);
                if (configDia != null) {
                    continue;
                }
                orcamentoAcumulado += taxaDiariaReforco;
                List<AtribuicaoDia> extras = reforcarCoberturaDoDia(
                        data, pedido.turnos(), estadoPorColaborador.values(), horarios, pedido, orcamentoAcumulado);
                for (AtribuicaoDia extra : extras) {
                    orcamentoAcumulado -= extra.minutosTurno();
                }
                registarAtribuicoes(data, extras, horarios, sabadosComChefia);
            }
        }

        // ── FASE 2.5 — 2º turno para FT que receberam 1º turno via top-up ─────────
        // O top-up (Fase 2) pode atribuir um 1º turno a um FT em dias que a Fase 1
        // não cobriu — e a Fase 1.5 já correu antes, pelo que esses turnos ficavam
        // como 4h simples. Este sweep apanha todos os FT que ainda têm exatamente
        // 1 turno e tenta completar o par consecutivo com a capacidade restante.
        completarTurnosFullTime(pedido, horarios, estadoPorColaborador, sabadosComChefia);

        // Fase final A: refinamento por pesquisa local (folgas preferidas + equilíbrio de carga)
        List<Horario> refinado = refinador.refinar(pedido, horarios);

        // Fase final B: polimento por hill-climbing (trocas entre colaboradores que reduzam
        // o custo global — equilíbrio, rotação de FDS e preferências não honradas). Preserva
        // a cobertura por construção (só muda quem cobre cada turno) e valida cada troca
        // contra todas as regras hard. Determinístico via semente de diversificação.
        return polisher.polir(pedido, refinado);
    }

    /** True se o prazo está a menos de 4 s de distância — sinal para não iniciar nova tentativa. */
    private boolean prazoProximo(PedidoGeracao pedido) {
        return pedido.prazoLimite() != null
                && Instant.now().isAfter(pedido.prazoLimite().minusSeconds(4));
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
    // Pré-flight de entradas degeneradas
    // =========================================================================

    /**
     * Casos degenerados falham imediatamente com a causa real: sem este guard,
     * uma equipa vazia caía na mensagem de capacidade ("a equipa só tem 0h") e
     * uma loja sem turnos base caía em "turnos compatíveis com a exceção" num
     * dia sem exceção nenhuma.
     */
    private void validarEntradasBasicas(PedidoGeracao pedido) {
        if (pedido.colaboradores().isEmpty()) {
            throw new FalhaGeracaoHorarioException(
                    "Não foi possível gerar o horário: não foi selecionado nenhum colaborador para a geração.",
                    "-",
                    DATA_FORMATTER.format(pedido.dataInicio()),
                    0,
                    "A geração arrancou sem colaboradores elegíveis.",
                    List.of(),
                    List.of(new SugestaoFalhaGeracao("equipa_vazia",
                            "Seleciona pelo menos um colaborador ativo com vínculo válido na loja.",
                            null)));
        }
        if (pedido.turnos().isEmpty()) {
            throw new FalhaGeracaoHorarioException(
                    "Não foi possível gerar o horário: a loja não tem turnos base configurados.",
                    "-",
                    DATA_FORMATTER.format(pedido.dataInicio()),
                    pedido.colaboradores().size(),
                    "Sem turnos base não há slots de trabalho para atribuir.",
                    List.of(),
                    List.of(new SugestaoFalhaGeracao("sem_turnos",
                            "Configura pelo menos um turno (hora de início e fim) nas definições da loja.",
                            null)));
        }
    }

    // =========================================================================
    // Pré-flight de capacidade global
    // =========================================================================

    /**
     * Verificação aritmética antes de começar: a soma das cargas contratuais da equipa
     * selecionada tem de chegar para a cobertura mínima de todos os dias do período.
     * Sem isto, a geração arrancava, consumia a carga ao longo do mês e falhava só nos
     * últimos dias com "carga contratual esgotada" — uma mensagem que não explica que o
     * problema é estrutural (equipa pequena ou mínimos altos), não pontual.
     */
    private void validarCapacidadeGlobal(PedidoGeracao pedido) {
        long minutosNecessarios = minutosMinimosNecessarios(pedido.dataInicio(), pedido);
        long capacidadeTotal = 0;
        for (Lojautilizador ligacao : pedido.colaboradores()) {
            Integer id = ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getId() : null;
            capacidadeTotal += Math.max(0, pedido.cargaMaximaPorColaborador().getOrDefault(id, 0L));
        }
        if (capacidadeTotal >= minutosNecessarios) {
            return;
        }

        long horasNecessarias = (minutosNecessarios + 59) / 60;
        long horasDisponiveis = capacidadeTotal / 60;
        long deficeHoras = horasNecessarias - horasDisponiveis;
        throw new FalhaGeracaoHorarioException(
                "Não foi possível gerar o horário: a cobertura mínima do período exige cerca de "
                        + horasNecessarias + "h de trabalho, mas a equipa selecionada só tem "
                        + horasDisponiveis + "h de carga contratual (faltam ~" + deficeHoras + "h). "
                        + "Este é um problema de capacidade, não de um dia específico.",
                "-",
                DATA_FORMATTER.format(pedido.dataInicio()),
                pedido.colaboradores().size(),
                "A capacidade contratual da equipa não chega para os mínimos do mês (faltam ~"
                        + deficeHoras + "h).",
                List.of(),
                List.of(
                        new SugestaoFalhaGeracao("capacidade_minimos",
                                "Reduz o mínimo de colaboradores por turno nas regras da loja — é o que mais alivia a necessidade total.",
                                null),
                        new SugestaoFalhaGeracao("capacidade_equipa",
                                "Inclui mais colaboradores na geração ou contrata reforço (faltam ~" + deficeHoras + "h de capacidade).",
                                "part-time"),
                        new SugestaoFalhaGeracao("capacidade_cargas",
                                "Aumenta a carga contratual dos perfis existentes nas regras da loja.",
                                null)
                )
        );
    }

    /**
     * Minutos de trabalho exigidos pela cobertura mínima de todos os dias a partir de
     * {@code desde} (inclusive): por dia, soma de mínimo × duração do turno representativo
     * de cada tipo. Dias encerrados contam zero; dias especiais usam os seus mínimos.
     */
    private long minutosMinimosNecessarios(LocalDate desde, PedidoGeracao pedido) {
        long total = 0;
        for (LocalDate data = desde; !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
            ConfiguracaoDia configDia = pedido.configuracoesPorData().get(data);
            if (configDia != null && configDia.lojaEncerrada()) {
                continue;
            }
            List<Turno> turnosDoDia = configDia != null ? configDia.turnosCompativeis() : pedido.turnos();
            if (turnosDoDia.isEmpty()) {
                continue;
            }
            for (SlotDia slot : construirSlots(turnosDoDia, configDia, pedido.minimosEfetivosPorTurno())) {
                total += validator.calcularDuracaoEmMinutos(slot.turno());
            }
        }
        return total;
    }

    // =========================================================================
    // Fase de preenchimento de capacidade (anti-desfalque)
    // =========================================================================

    /**
     * Reforça a cobertura de um dia para além do mínimo (FASE 2), usando a capacidade
     * contratual que sobrou depois de todos os mínimos do mês estarem garantidos.
     * Corre num segundo passo, depois da fase 1 ter coberto os mínimos de todos os
     * dias — pelo que <b>nenhum</b> turno extra pode comprometer a cobertura mínima.
     *
     * <p>Critérios, por colaborador:
     * <ul>
     *   <li><b>Capacidade restante:</b> só recebe turno extra quem ainda tem carga
     *       contratual por consumir; nunca ultrapassa o teto (via {@code podeReceber}).</li>
     *   <li><b>Orçamento acumulado:</b> recebe o orçamento de reforço disponível até hoje
     *       ({@code orcamentoHoje}), acumulado a uma taxa diária constante pela fase 2, para
     *       o reforço ser repartido por igual ao longo do mês em vez de se concentrar nos
     *       últimos dias.</li>
     *   <li><b>Regras hard intactas:</b> cada turno extra passa por
     *       {@link EstadoColaborador#podeReceber} sem qualquer relaxação.</li>
     *   <li><b>Folgas preferidas respeitadas:</b> um turno extra nunca cai num dia de
     *       folga preferida — a cobertura mínima já está garantida sem ele.</li>
     * </ul>
     *
     * <p>O turno escolhido para cada colaborador é o de melhor pontuação no
     * {@link AvaliadorAtribuicao}. Devolve no máximo um turno extra por colaborador.
     */
    private List<AtribuicaoDia> reforcarCoberturaDoDia(LocalDate data,
                                                       List<Turno> turnosDoDia,
                                                       Collection<EstadoColaborador> estados,
                                                       List<Horario> horariosJaGerados,
                                                       PedidoGeracao pedido,
                                                       double orcamentoHoje) {
        if (pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite())) {
            return List.of();
        }
        // Orçamento de hoje: folga de capacidade acumulada até hoje (taxa diária constante,
        // calculada pela fase 2 a partir da capacidade que sobrou dos mínimos). Como todos
        // os mínimos já estão colocados, esta folga é puro excedente.
        if (orcamentoHoje <= 0) {
            return List.of();
        }

        AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContexto(horariosJaGerados);

        // Melhor turno por colaborador, limite inverso ao mínimo por tipo — enche até à
        // capacidade disponível. Usado quando o gestor NÃO definiu alvo de pessoas/turno
        // (com alvo, o preenchimento é feito por preencherAteAlvoUniforme, que levanta o piso).
        // Ao fim de semana, tenta primeiro SEM relaxar a rotação de FDS — só relaxa (para
        // todo o dia) se nenhum colaborador elegível restar, espelhando a escalada por
        // tentativas da Fase 1 (tentativa 3/4) em vez de ignorar a rotação incondicionalmente.
        boolean fimDeSemana = validator.ehFimDeSemana(data);
        List<CandidatoPontuado> candidatos = construirCandidatosReforco(
                data, turnosDoDia, estados, pedido, contexto, false);
        if (candidatos.isEmpty() && fimDeSemana) {
            candidatos = construirCandidatosReforco(data, turnosDoDia, estados, pedido, contexto, true);
        }

        // Limita o top-up inversamente ao mínimo por tipo. Garante sempre pelo menos 1
        // (a divisão inteira podia produzir 0 quando minimoMaxGlobal > numTipos).
        int minimoMaxGlobal = pedido.minimosPorTurno().values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);
        long numTiposDistintos = turnosDoDia.stream()
                .map(TurnoClassifier::tipoNormalizado)
                .distinct()
                .count();
        long limiteTopUp = Math.max(1L, numTiposDistintos / Math.max(1, minimoMaxGlobal));

        List<AtribuicaoDia> selecionados = new ArrayList<>();
        double gastoHoje = 0;
        for (CandidatoPontuado candidato : candidatos.stream()
                .sorted(Comparator.comparingDouble(CandidatoPontuado::score))
                .toList()) {
            if (selecionados.size() >= limiteTopUp) {
                break;
            }
            long minutos = candidato.atribuicao().minutosTurno();
            if (gastoHoje + minutos > orcamentoHoje) {
                continue;
            }
            gastoHoje += minutos;
            selecionados.add(candidato.atribuicao());
        }
        return selecionados;
    }

    /** Melhor turno por colaborador para o top-up, com ou sem relaxação da rotação de FDS. */
    private List<CandidatoPontuado> construirCandidatosReforco(LocalDate data,
                                                                List<Turno> turnosDoDia,
                                                                Collection<EstadoColaborador> estados,
                                                                PedidoGeracao pedido,
                                                                AvaliadorAtribuicao.ContextoAvaliacao contexto,
                                                                boolean ignorarRotacaoFDS) {
        List<CandidatoPontuado> candidatos = new ArrayList<>();
        for (EstadoColaborador estado : estados) {
            if (estado.capacidadeRestanteMinutos() <= 0) continue;
            if (pedido.folgasPreferidasPorColaborador()
                    .getOrDefault(estado.idUtilizador(), Set.of()).contains(data)) continue;

            if (ignorarRotacaoFDS
                    && estado.ficariaSemFimDeSemanaLivre(data, pedido.dataInicio(), pedido.dataFim())) continue;

            CandidatoPontuado melhor = null;
            for (Turno turno : turnosDoDia) {
                long minutos = validator.calcularDuracaoEmMinutos(turno);
                if (!estado.podeReceber(data, turno, minutos, pedido, ignorarRotacaoFDS, false)) continue;
                double score = avaliador.pontuar(estado, turno, minutos, data, pedido, contexto);
                if (melhor == null || score < melhor.score()) {
                    melhor = new CandidatoPontuado(new AtribuicaoDia(estado, turno, minutos), score);
                }
            }
            if (melhor != null) {
                candidatos.add(melhor);
            }
        }
        return candidatos;
    }

    /**
     * Para cada FT (maxTurnosPorDia ≥ 2) com exatamente 1 turno em {@code data},
     * tenta atribuir o turno consecutivo (Manhã→Tarde ou Tarde→Noite).
     * Chamado dentro da Fase 1 imediatamente após cada dia — assim a capacidade
     * é consumida em pares e não dispersa por muitos dias de 1 turno.
     */
    private void completarTurnosFullTimeDia(LocalDate data,
                                             List<Turno> turnosDoDia,
                                             PedidoGeracao pedido,
                                             List<Horario> horarios,
                                             Map<Integer, EstadoColaborador> estadoPorColaborador,
                                             Set<LocalDate> sabadosComChefia) {
        // Itera em ordem DESCENDENTE de hora de início (Noite→Tarde→Manhã).
        // Assim, um FT que tem Tarde encontra Noite primeiro (→ T/N) em vez de
        // recuar para Manhã (→ M/T), equilibrando a cobertura noturna.
        boolean fimDeSemana = validator.ehFimDeSemana(data);
        List<Turno> turnos = turnosDoDia.stream()
                .sorted(Comparator.comparing(Turno::getHoraInicio, Comparator.reverseOrder()))
                .toList();
        for (EstadoColaborador estado : estadoPorColaborador.values()) {
            if (estado.maxTurnosPorDia() < 2) continue;
            if (estado.turnosDia(data) != 1) continue;
            for (Turno turno : turnos) {
                long minutos = validator.calcularDuracaoEmMinutos(turno);
                // O 2º turno é no MESMO dia que o 1º — não acrescenta um dia à semana,
                // por isso ignorarDescansoSemanal=true é sempre correto aqui.
                // Em FDS, usa a mesma relaxação de rotação que a Fase 1 (tentativa 3/4).
                if (!estado.podeReceber(data, turno, minutos, pedido, fimDeSemana, true)) continue;
                registarAtribuicoes(data, List.of(new AtribuicaoDia(estado, turno, minutos)),
                        horarios, sabadosComChefia);
                break;
            }
        }
    }

    /** Sweep final de Fase 1.5: apanha FT que ainda têm 1 turno (casos onde o par falhou na fase 1). */
    private void completarTurnosFullTime(PedidoGeracao pedido,
                                          List<Horario> horarios,
                                          Map<Integer, EstadoColaborador> estadoPorColaborador,
                                          Set<LocalDate> sabadosComChefia) {
        for (LocalDate data = pedido.dataInicio(); !data.isAfter(pedido.dataFim()); data = data.plusDays(1)) {
            if (pedido.configuracoesPorData().get(data) != null) continue;
            List<Turno> turnos = pedido.configuracoesPorData().get(data) != null
                    ? List.of() : pedido.turnos();
            completarTurnosFullTimeDia(data, turnos, pedido, horarios, estadoPorColaborador, sabadosComChefia);
        }
    }

    /**
     * Preenchimento com <b>alvo de pessoas por turno</b> "levantando o piso": leva todos os
     * dias abertos a 2 pessoas por tipo de turno antes de qualquer dia ir a 3, e assim por
     * diante até ao alvo. Em cada passo escolhe o par (dia, tipo) com MENOR cobertura atual
     * ainda abaixo do alvo e atribui-lhe o melhor candidato (pontuação do avaliador, que com
     * a política "Preferências" valoriza turnos/colegas preferidos). Para quando a capacidade
     * contratual da equipa se esgota — pelo que, se o alvo for inalcançável (equipa pequena),
     * a cobertura sobe o mais uniformemente possível em vez de se amontoar em alguns dias.
     *
     * <p>Respeita todas as regras hard ({@link EstadoColaborador#podeReceber}), folgas
     * preferidas, e ao fim de semana só reforça colaboradores designados pelo plano de FDS.
     * Dias com configuração especial cumprem apenas o mínimo (não recebem reforço).
     */
    private void preencherAteAlvoUniforme(PedidoGeracao pedido,
                                          List<Horario> horarios,
                                          Collection<EstadoColaborador> estados,
                                          Set<LocalDate> sabadosComChefia,
                                          int alvo) {
        List<LocalDate> diasAbertos = new ArrayList<>();
        for (LocalDate d = pedido.dataInicio(); !d.isAfter(pedido.dataFim()); d = d.plusDays(1)) {
            if (pedido.configuracoesPorData().get(d) == null) diasAbertos.add(d);
        }
        List<String> tipos = pedido.turnos().stream()
                .map(TurnoClassifier::tipoNormalizado).distinct().toList();
        Map<String, List<Turno>> turnosPorTipo = new LinkedHashMap<>();
        for (String tipo : tipos) {
            turnosPorTipo.put(tipo, pedido.turnos().stream()
                    .filter(t -> TurnoClassifier.tipoNormalizado(t).equals(tipo)).toList());
        }

        // Levanta o piso: repete enquanto houver um slot (dia, tipo) abaixo do alvo com
        // candidato válido, escolhendo sempre o de menor cobertura atual (desempate por score).
        while (true) {
            if (pedido.prazoLimite() != null && Instant.now().isAfter(pedido.prazoLimite())) break;

            Map<LocalDate, Map<String, Integer>> contagem = new HashMap<>();
            for (Horario h : horarios) {
                LocalDate d = h.getDataTurno();
                if (d == null || h.getIdTurno() == null) continue;
                contagem.computeIfAbsent(d, k -> new HashMap<>())
                        .merge(TurnoClassifier.tipoNormalizado(h.getIdTurno()), 1, Integer::sum);
            }

            AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContextoSemEquidade(horarios);
            LocalDate melhorData = null;
            CandidatoPontuado melhorCand = null;
            int melhorCobertura = Integer.MAX_VALUE;

            for (LocalDate dia : diasAbertos) {
                Map<String, Integer> contDia = contagem.getOrDefault(dia, Map.of());
                for (String tipo : tipos) {
                    int cobertura = contDia.getOrDefault(tipo, 0);
                    if (cobertura >= alvo) continue;
                    // Só interessa um slot que possa igualar/baixar o piso já encontrado.
                    if (cobertura > melhorCobertura) continue;
                    CandidatoPontuado cand = melhorCandidatoSlot(
                            dia, turnosPorTipo.get(tipo), estados, pedido, contexto);
                    if (cand == null) continue;
                    if (melhorCand == null || cobertura < melhorCobertura
                            || (cobertura == melhorCobertura && cand.score() < melhorCand.score())) {
                        melhorCobertura = cobertura;
                        melhorCand = cand;
                        melhorData = dia;
                    }
                }
            }

            if (melhorCand == null) break; // capacidade/regras esgotadas — nada mais a preencher
            registarAtribuicoes(melhorData, List.of(melhorCand.atribuicao()), horarios, sabadosComChefia);
        }
    }

    /**
     * Melhor candidato (menor pontuação) para um turno de um dado tipo num dia, ou null.
     * Ao fim de semana, tenta primeiro SEM relaxar a rotação de FDS — só relaxa se nenhum
     * colaborador elegível restar, espelhando a escalada por tentativas da Fase 1
     * (tentativa 3/4) em vez de ignorar a rotação incondicionalmente.
     */
    private CandidatoPontuado melhorCandidatoSlot(LocalDate dia,
                                                  List<Turno> turnosDoTipo,
                                                  Collection<EstadoColaborador> estados,
                                                  PedidoGeracao pedido,
                                                  AvaliadorAtribuicao.ContextoAvaliacao contexto) {
        CandidatoPontuado melhor = melhorCandidatoSlot(dia, turnosDoTipo, estados, pedido, contexto, false);
        if (melhor == null && validator.ehFimDeSemana(dia)) {
            melhor = melhorCandidatoSlot(dia, turnosDoTipo, estados, pedido, contexto, true);
        }
        return melhor;
    }

    private CandidatoPontuado melhorCandidatoSlot(LocalDate dia,
                                                  List<Turno> turnosDoTipo,
                                                  Collection<EstadoColaborador> estados,
                                                  PedidoGeracao pedido,
                                                  AvaliadorAtribuicao.ContextoAvaliacao contexto,
                                                  boolean ignorarRotacaoFDS) {
        CandidatoPontuado melhor = null;
        for (EstadoColaborador estado : estados) {
            if (estado.capacidadeRestanteMinutos() <= 0) continue;
            if (pedido.folgasPreferidasPorColaborador()
                    .getOrDefault(estado.idUtilizador(), Set.of()).contains(dia)) continue;
            if (ignorarRotacaoFDS
                    && estado.ficariaSemFimDeSemanaLivre(dia, pedido.dataInicio(), pedido.dataFim())) continue;
            for (Turno turno : turnosDoTipo) {
                long minutos = validator.calcularDuracaoEmMinutos(turno);
                if (!estado.podeReceber(dia, turno, minutos, pedido, ignorarRotacaoFDS, false)) continue;
                double score = avaliador.pontuar(estado, turno, minutos, dia, pedido, contexto);
                if (melhor == null || score < melhor.score()) {
                    melhor = new CandidatoPontuado(new AtribuicaoDia(estado, turno, minutos), score);
                }
            }
        }
        return melhor;
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

        List<SlotDia> slots = construirSlots(turnosDoDia, configDia, pedido.minimosEfetivosPorTurno());

        boolean precisaChefia = pedido.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && !sabadoJaTemChefia;

        if (precisaChefia && !existeChefiaPossivel(data, slots, estadoPorColaborador.values(), pedido)) {
            lancarFalhaChefia(data, slots, estadoPorColaborador.values(), pedido);
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
        // Com alvo por turno, o componente 11 (equidade por tipo) no greedy primário
        // altera quais colaboradores ficam em cada dia, perturbando a uniformidade de
        // cobertura em preencherAteAlvoUniforme. Usa contexto sem equidade nesse caso.
        AvaliadorAtribuicao.ContextoAvaliacao contexto = pedido.alvoPorTurno() != null
                ? avaliador.novoContextoSemEquidade(horariosJaGerados)
                : avaliador.novoContexto(horariosJaGerados);

        // Melhor turno (menor pontuação) por colaborador — score calculado uma única vez.
        // Nas tentativas adicionais do multi-start, aplica ruído gaussiano controlado para
        // explorar soluções diferentes por desempate estocástico (GREEDY_NOISE_RND != null).
        Random noiseRnd = GREEDY_NOISE_RND.get();
        Map<Integer, CandidatoPontuado> melhorPorColaborador = new LinkedHashMap<>();
        for (Turno turno : turnosCompativeis) {
            long minutos = validator.calcularDuracaoEmMinutos(turno);
            for (EstadoColaborador estado : estados) {
                if (!estado.podeReceber(data, turno, minutos, pedido,
                        ignorarRotacaoFDS, ignorarDescansoSemanal)) continue;

                double score = avaliador.pontuar(estado, turno, minutos, data, pedido, contexto);
                if (noiseRnd != null) {
                    score += noiseRnd.nextGaussian() * SIGMA_RUIDO_GREEDY;
                }
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
        // Relaxa rotação E descanso semanal — exatamente o que a tentativa 4 do motor
        // está disposta a relaxar ao fim de semana. Antes, o pré-check só relaxava a
        // rotação, pelo que desistia (com exceção) de sábados que a tentativa 4 ainda
        // conseguiria cobrir com uma chefia em 6.º dia da semana.
        for (EstadoColaborador estado : estados) {
            if (!estado.ehChefiaAoSabado()) continue;
            for (SlotDia slot : slots) {
                for (Turno t : slot.turnosCompativeis()) {
                    if (estado.podeReceber(data, t,
                            validator.calcularDuracaoEmMinutos(t), pedido, true, true)) {
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

    /**
     * Falha de chefia ao sábado com diagnóstico nominal: explica porque é que cada
     * gerente/subgerente está indisponível (mesmo com rotação e descanso semanal
     * relaxados) e o que o gestor pode fazer. Substitui a mensagem seca anterior
     * ("Nao foi possivel garantir presenca de gerente/subgerente...") que não dava
     * qualquer pista de resolução.
     */
    private void lancarFalhaChefia(LocalDate data,
                                   List<SlotDia> slots,
                                   Collection<EstadoColaborador> estados,
                                   PedidoGeracao pedido) {
        List<Turno> turnosDoDia = slots.stream()
                .flatMap(s -> s.turnosCompativeis().stream())
                .distinct()
                .toList();

        StringBuilder detalhe = new StringBuilder();
        List<com.example.projeto2.API.Services.geracao.MotivoFalhaGeracao> motivos = new ArrayList<>();
        for (EstadoColaborador estado : estados) {
            if (!estado.ehChefiaAoSabado()) continue;
            // Código do turno mais longo — o representativo do dia (mesma heurística do
            // DiagnosticoCobertura); um turno curto daria "turno_curto" pouco informativo.
            String codigo = null;
            long maiorDuracao = -1;
            for (Turno t : turnosDoDia) {
                long minutos = validator.calcularDuracaoEmMinutos(t);
                String codigoTurno = estado.diagnosticarExclusao(data, t, minutos, pedido);
                if (codigoTurno == null) { codigo = null; break; }
                if (minutos > maiorDuracao) {
                    maiorDuracao = minutos;
                    codigo = codigoTurno;
                }
            }
            String motivo = switch (codigo != null ? codigo : "desconhecido") {
                case "carga_esgotada"    -> "carga contratual esgotada";
                case "descanso_semanal"  -> "máximo de dias da semana atingido";
                case "dias_consecutivos" -> "máximo de dias consecutivos atingido";
                case "bloqueado"         -> "folga ou ausência aprovada neste dia";
                case "rotacao_fim_semana"-> "rotação de fins de semana";
                case "descanso_minimo"   -> "descanso mínimo entre turnos";
                case "turno_nao_consecutivo" -> "segundo turno não consecutivo com o primeiro";
                case "ja_escalado"       -> "já atingiu o máximo de turnos no dia";
                case "limite_diario_excedido" -> "limite legal de 8h de trabalho diário atingido";
                default                   -> "indisponível";
            };
            if (!detalhe.isEmpty()) detalhe.append("; ");
            detalhe.append(estado.nome()).append(" — ").append(motivo);
            motivos.add(new com.example.projeto2.API.Services.geracao.MotivoFalhaGeracao(
                    codigo != null ? codigo : "desconhecido", 1, motivo, List.of(estado.nome())));
        }

        throw new FalhaGeracaoHorarioException(
                "Não foi possível garantir gerente/subgerente no sábado "
                        + DATA_FORMATTER.format(data) + ". Estado das chefias: "
                        + (detalhe.isEmpty() ? "nenhuma chefia na equipa selecionada" : detalhe) + ".",
                "chefia ao sábado",
                DATA_FORMATTER.format(data),
                pedido.colaboradores().size(),
                detalhe.isEmpty()
                        ? "A equipa selecionada não tem gerente nem subgerente."
                        : "Nenhuma chefia disponível: " + detalhe + ".",
                motivos,
                List.of(
                        new SugestaoFalhaGeracao("chefia_equipa",
                                detalhe.isEmpty()
                                        ? "Inclui o gerente ou o subgerente na seleção de colaboradores da geração."
                                        : "Verifica as folgas/ausências aprovadas das chefias neste sábado e nos dias próximos.",
                                null),
                        new SugestaoFalhaGeracao("chefia_carga",
                                "Se a carga das chefias está esgotada, aumenta a carga contratual do perfil de gestão ou reduz os mínimos por turno para libertar os dias úteis.",
                                null),
                        new SugestaoFalhaGeracao("chefia_regra",
                                "Em último caso, desativa a regra de chefia obrigatória ao sábado nas regras da loja.",
                                null)
                )
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
