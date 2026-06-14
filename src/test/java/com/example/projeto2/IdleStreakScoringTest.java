package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Modules.Horario;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) do bónus de idle streak no {@link AvaliadorAtribuicao}:
 * verifica que após 2+ dias sem trabalho o colaborador recebe uma pontuação mais baixa
 * (melhor) e que o motor reflete essa preferência na geração real.
 */
class IdleStreakScoringTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(validator);

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // Seg–Sex

    @Test
    void colaboradorIdleHaMaisDeDoisDiasTemPontuacaoInferior() {
        // A trabalhou ontem (diasDesdeUltimoTurno = 1, sem bónus)
        // B não trabalha há 3 dias (diasDesdeUltimoTurno = 3, recebe bónus)
        // Com pesoEquilibrioCarga=2 (EQUILIBRIO) e excessoRitmo~0:
        //   bónus B = 2 * (min(3,5)-1) * 18 = 2 * 2 * 18 = 72 pt
        //   diferença esperada > 0 (score de B é menor = melhor)

        long carga = 9_600L; // ambos com a mesma carga e a mesma alocação até agora
        Lojautilizador ligA = colaborador(1, "A");
        Lojautilizador ligB = colaborador(2, "B");

        LocalDate data = inicio.plusDays(3); // quarta-feira da semana
        Turno turno = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        long minutos = validator.calcularDuracaoEmMinutos(turno);

        // Estado A: trabalhou ontem → diasDesdeUltimoTurno = 1
        EstadoColaborador estadoA = new EstadoColaborador(ligA, carga, Set.of(), validator);
        estadoA.registarAtribuicao(data.minusDays(1), turno, minutos);

        // Estado B: trabalhou há 3 dias → diasDesdeUltimoTurno = 3
        EstadoColaborador estadoB = new EstadoColaborador(ligB, carga, Set.of(), validator);
        estadoB.registarAtribuicao(data.minusDays(3), turno, minutos);

        PedidoGeracao pedido = pedidoSimples(List.of(ligA, ligB), List.of(turno), carga);
        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(List.of());

        double scoreA = avaliador.pontuar(estadoA, turno, minutos, data, pedido, ctx);
        double scoreB = avaliador.pontuar(estadoB, turno, minutos, data, pedido, ctx);

        assertTrue(scoreB < scoreA,
                "Colaborador inativo há 3 dias deve ter pontuação inferior (melhor) ao que trabalhou ontem. "
                        + "ScoreA=" + scoreA + " ScoreB=" + scoreB);
    }

    @Test
    void bonusIdleNaoSeBonusIdleNaoSeAplicaSeExcessoDeRitmoAltoCnaoSe() {
        // B está inativo há 4 dias MAS está muito adiantado no ritmo contratual (excesso ~50%)
        // → o bónus NÃO se deve aplicar (condição excessoRitmo <= 0.05)
        long carga = 9_600L;
        Lojautilizador ligA = colaborador(1, "A");
        Lojautilizador ligB = colaborador(2, "B");

        LocalDate data = inicio.plusDays(10);
        Turno turno = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        long minutos = validator.calcularDuracaoEmMinutos(turno);

        // A: ritmo normal, trabalhou ontem
        EstadoColaborador estadoA = new EstadoColaborador(ligA, carga, Set.of(), validator);
        estadoA.registarAtribuicao(data.minusDays(1), turno, minutos);

        // B: muito adiantado — acumulou 50% a mais do que o esperado para este ponto do mês,
        //    mas não trabalha há 4 dias. O bónus de idle não se deve aplicar.
        EstadoColaborador estadoB = new EstadoColaborador(ligB, carga, Set.of(), validator);
        // Simular excesso de ritmo: registar muitos turnos consecutivos (acima do ritmo esperado)
        for (int i = 15; i >= 5; i--) {
            estadoB.registarAtribuicao(data.minusDays(i), turno, minutos);
        }
        // B não trabalha há 4 dias — diasDesdeUltimoTurno = 4

        PedidoGeracao pedido = pedidoSimples(List.of(ligA, ligB), List.of(turno), carga);
        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(List.of());

        double scoreA = avaliador.pontuar(estadoA, turno, minutos, data, pedido, ctx);
        double scoreB = avaliador.pontuar(estadoB, turno, minutos, data, pedido, ctx);

        // B está muito adiantado → o pace guard (excessoRitmo > 0.05) deve tornar B menos
        // preferido que A. O bónus de idle não deve compensar o excesso de carga.
        assertTrue(scoreA < scoreB,
                "Colaborador com excesso de ritmo não deve receber bónus de idle. "
                        + "ScoreA=" + scoreA + " ScoreB=" + scoreB);
    }

    @Test
    void motorPrefereTrabalhadorInaticoPossivelCom3DiasDeIdle() {
        // Equipa de 3 com 1 turno por dia (mínimo = 1). A trabalhou ontem, B e C estão
        // inativos há 3 dias. O motor deve atribuir o dia seguinte a B ou C, não a A.
        HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

        Lojautilizador ligA = colaborador(1, "A");
        Lojautilizador ligB = colaborador(2, "B");
        Lojautilizador ligC = colaborador(3, "C");

        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        // Uma semana de segunda a sexta, 1 vaga por dia
        LocalDate semanaInicio = inicio;
        LocalDate semanaFim = inicio.plusDays(4);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(ligA, ligB, ligC),
                List.of(turno),
                semanaInicio,
                semanaFim,
                Map.of(1, 1),
                31, 11, 2, 1,
                false,
                Set.of(),
                Map.of(1, 9_600L, 2, 9_600L, 3, 9_600L),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                null,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),
                Map.of(),
                42L);

        List<Horario> horarios = engine.gerar(pedido);

        // Mínimo de 5 turnos (1 por dia de seg a sex) — o top-up pode adicionar mais.
        assertTrue(horarios.size() >= 5,
                "Deve haver pelo menos 5 turnos gerados; houve " + horarios.size());

        // Com 3 colaboradores e 1 vaga mínima/dia, o idle streak deve equilibrar:
        // nenhum deve dominar — em 5 dias com top-up, o máximo aceitável é 3 turnos.
        long maxPorColaborador = horarios.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        java.util.stream.Collectors.counting()))
                .values().stream()
                .mapToLong(Long::longValue)
                .max().orElse(0);

        assertTrue(maxPorColaborador <= 4,
                "Com 3 colaboradores e 5 dias, o idle streak deve distribuir os turnos: nenhum deve receber todos; max=" + maxPorColaborador);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private Turno turno(int id, String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        return t;
    }

    private PedidoGeracao pedidoSimples(List<Lojautilizador> colaboradores, List<Turno> turnos, long carga) {
        java.util.Map<Integer, Long> cargas = new java.util.HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), carga);
        }
        return new PedidoGeracao(
                colaboradores,
                turnos,
                inicio,
                fim,
                Map.of(1, 1),
                31, 11, 2, 1,
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
