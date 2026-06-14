package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Services.geracao.RegraGeracaoResolver;
import com.example.projeto2.API.Services.geracao.RegraAplicada;
import com.example.projeto2.API.Services.geracao.ParametrosGeracao;
import com.example.projeto2.API.Services.geracao.TurnoClassifier;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de regressão do bug noite→manhã: um colaborador escalado num turno de noite
 * (ex.: 16:00–00:00) não pode ser escalado de manhã (08:00) no dia seguinte — o gap é
 * de apenas 8 horas, abaixo das 11h legais (CT art. 214.º).
 *
 * <p>Cobre as três camadas do fix:
 * <ol>
 *   <li>{@link RegraGeracaoResolver}: default e clamp do descanso mínimo para 11h</li>
 *   <li>{@link EstadoColaborador#podeReceber}: bloqueio hard com 11h</li>
 *   <li>{@link HorarioGeneratorEngine}: nenhum par noite→manhã em dias consecutivos no plano final</li>
 *   <li>{@link AvaliadorAtribuicao}: rotação invertida penalizada mesmo quando legal</li>
 * </ol>
 */
class DescansoMinimoNoiteManhaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // Seg–Sex

    // ── Camada 1: resolver de regras ─────────────────────────────────────────

    @Test
    void resolverAplicaMinimoLegalDe11HorasQuandoSemRegra() {
        RegraGeracaoResolver resolver = new RegraGeracaoResolver(null, null, validator);
        ParametrosGeracao parametros = resolver.resolverParametrosGeracao(
                List.of(regra("Minimo de colaboradores por turno", 1)),
                List.of(turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0))));

        assertEquals(11, parametros.descansoMinimoHoras(),
                "Sem regra de descanso configurada, o default deve ser o mínimo legal de 11h.");
    }

    @Test
    void resolverCorrigeRegraConfiguradaAbaixoDoMinimoLegal() {
        RegraGeracaoResolver resolver = new RegraGeracaoResolver(null, null, validator);
        ParametrosGeracao parametros = resolver.resolverParametrosGeracao(
                List.of(
                        regra("Minimo de colaboradores por turno", 1),
                        regra("Descanso minimo de horas entre turnos", 8)),
                List.of(turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0))));

        assertEquals(11, parametros.descansoMinimoHoras(),
                "Uma regra configurada com 8h deve ser corrigida para o mínimo legal de 11h.");
    }

    @Test
    void resolverRespeitaRegraAcimaDoMinimoLegal() {
        RegraGeracaoResolver resolver = new RegraGeracaoResolver(null, null, validator);
        ParametrosGeracao parametros = resolver.resolverParametrosGeracao(
                List.of(
                        regra("Minimo de colaboradores por turno", 1),
                        regra("Descanso minimo de horas entre turnos", 12)),
                List.of(turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0))));

        assertEquals(12, parametros.descansoMinimoHoras(),
                "Uma regra mais exigente que a lei deve ser respeitada.");
    }

    // ── Camada 2: estado do colaborador ──────────────────────────────────────

    @Test
    void colaboradorComNoiteOntemNaoPodeReceberManhaHoje() {
        Lojautilizador lig = colaborador(1, "Henrique");
        EstadoColaborador estado = new EstadoColaborador(lig, 9_600L, Set.of(), validator);

        Turno noite = turno(2, "noite", LocalTime.of(16, 0), LocalTime.of(0, 0));
        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));

        LocalDate ontem = inicio;
        LocalDate hoje = inicio.plusDays(1);
        estado.registarAtribuicao(ontem, noite, validator.calcularDuracaoEmMinutos(noite));

        PedidoGeracao pedido = pedido(List.of(lig), List.of(manha, noite), Map.of(1, 1, 2, 1), 11);

        assertTrue(!estado.podeReceber(hoje, manha,
                        validator.calcularDuracaoEmMinutos(manha), pedido, false, false),
                "Noite (16:00–00:00) seguida de manhã (08:00) tem só 8h de gap — deve ser bloqueada com descanso de 11h.");
    }

    // ── Camada 3: motor end-to-end ───────────────────────────────────────────

    @Test
    void motorNuncaGeraNoiteSeguidaDeManha() {
        HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

        // 6 colaboradores, turnos manhã e noite com gap de 8h entre noite→manhã.
        List<Lojautilizador> colaboradores = List.of(
                colaborador(1, "A"), colaborador(2, "B"), colaborador(3, "C"),
                colaborador(4, "D"), colaborador(5, "E"), colaborador(6, "F"));

        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        Turno noite = turno(2, "noite", LocalTime.of(16, 0), LocalTime.of(0, 0));

        PedidoGeracao pedido = pedido(colaboradores, List.of(manha, noite),
                Map.of(1, 1, 2, 1), 11);

        List<Horario> horarios = engine.gerar(pedido);

        Map<Integer, List<Horario>> porColaborador = horarios.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId()));

        for (List<Horario> doColaborador : porColaborador.values()) {
            List<Horario> ordenados = doColaborador.stream()
                    .sorted(java.util.Comparator.comparing(Horario::getDataTurno))
                    .toList();
            for (int i = 1; i < ordenados.size(); i++) {
                Horario anterior = ordenados.get(i - 1);
                Horario atual = ordenados.get(i);
                if (!anterior.getDataTurno().plusDays(1).equals(atual.getDataTurno())) continue;

                long gap = gapHoras(anterior, atual);
                assertTrue(gap >= 11,
                        "Colaborador " + anterior.getIdLojautilizador().getIdUtilizador().getNome()
                                + " tem " + gap + "h de descanso entre "
                                + anterior.getDataTurno() + " ("
                                + anterior.getIdTurno().getTipo() + ") e "
                                + atual.getDataTurno() + " (" + atual.getIdTurno().getTipo()
                                + ") — viola as 11h legais.");
            }
        }
    }

    // ── Camada 4: avaliador penaliza rotação invertida ───────────────────────

    @Test
    void avaliadorPenalizaRotacaoInvertidaMesmoQuandoLegal() {
        AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(validator);

        // Turnos com gap legal: tarde 12:00–20:00 → manhã 08:00–16:00 = 12h (legal),
        // mas é rotação para trás — deve ser penalizada face a quem vem de manhã.
        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        Turno tarde = turno(2, "tarde", LocalTime.of(12, 0), LocalTime.of(20, 0));

        Lojautilizador ligA = colaborador(1, "A");
        Lojautilizador ligB = colaborador(2, "B");

        LocalDate ontem = inicio;
        LocalDate hoje = inicio.plusDays(1);
        long minutosManha = validator.calcularDuracaoEmMinutos(manha);

        // A vem de turno de tarde (rotação invertida se receber manhã hoje)
        EstadoColaborador estadoA = new EstadoColaborador(ligA, 9_600L, Set.of(), validator);
        estadoA.registarAtribuicao(ontem, tarde, validator.calcularDuracaoEmMinutos(tarde));

        // B vem de turno de manhã (consistente se receber manhã hoje)
        EstadoColaborador estadoB = new EstadoColaborador(ligB, 9_600L, Set.of(), validator);
        estadoB.registarAtribuicao(ontem, manha, minutosManha);

        PedidoGeracao pedido = pedido(List.of(ligA, ligB), List.of(manha, tarde),
                Map.of(1, 1, 2, 1), 11);
        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(List.of());

        double scoreA = avaliador.pontuar(estadoA, manha, minutosManha, hoje, pedido, ctx);
        double scoreB = avaliador.pontuar(estadoB, manha, minutosManha, hoje, pedido, ctx);

        assertTrue(scoreB < scoreA,
                "Quem vem de manhã deve ser preferido para a manhã seguinte face a quem vem da tarde "
                        + "(rotação invertida). ScoreA=" + scoreA + " ScoreB=" + scoreB);
    }

    @Test
    void ordemPeriodoClassificaCorretamente() {
        assertEquals(0, TurnoClassifier.ordemPeriodo(turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0))));
        assertEquals(2, TurnoClassifier.ordemPeriodo(turno(2, "tarde", LocalTime.of(12, 0), LocalTime.of(20, 0))));
        assertEquals(3, TurnoClassifier.ordemPeriodo(turno(3, "noite", LocalTime.of(16, 0), LocalTime.of(0, 0))));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private long gapHoras(Horario anterior, Horario atual) {
        LocalDateTime fimAnterior = anterior.getDataTurno().atTime(anterior.getIdTurno().getHoraFim());
        if (!anterior.getIdTurno().getHoraFim().isAfter(anterior.getIdTurno().getHoraInicio())) {
            fimAnterior = fimAnterior.plusDays(1); // turno atravessa a meia-noite
        }
        LocalDateTime inicioAtual = atual.getDataTurno().atTime(atual.getIdTurno().getHoraInicio());
        return Duration.between(fimAnterior, inicioAtual).toHours();
    }

    private RegraAplicada regra(String descricao, Integer valor) {
        return new RegraAplicada(descricao, "operacional", valor);
    }

    private Lojautilizador colaborador(int id, String nome) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome(nome);
        c.setTipo("fulltime");
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id);
        lig.setIdUtilizador(u);
        lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(horaInicio);
        t.setHoraFim(horaFim);
        return t;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores, List<Turno> turnos,
                                  Map<Integer, Integer> minimosPorTurno, int descansoMinimoHoras) {
        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), 9_600L);
        }
        return new PedidoGeracao(
                colaboradores,
                turnos,
                inicio,
                fim,
                minimosPorTurno,
                31,
                descansoMinimoHoras,
                2,
                2,
                false,
                Set.of(),
                cargas,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                null,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),
                Map.of(),
                42L);
    }
}
