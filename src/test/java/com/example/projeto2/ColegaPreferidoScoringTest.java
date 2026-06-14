package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
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
 * Verifica que o bónus de colega preferido (componente 5 do avaliador) só dispara
 * quando o colega está confirmado no MESMO slot (mesmo dia e mesmo tipo de turno),
 * e não simplesmente por ter trabalhado em qualquer dia do mês.
 */
class ColegaPreferidoScoringTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(validator);

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

    @Test
    void bonusColegaNaoDisparaComColegaNoutroTurno() {
        // A prefere trabalhar com B. B já trabalhou este mês, mas num turno DIFERENTE
        // (tarde) do turno que estamos a avaliar para A (manhã). O bónus NÃO deve
        // disparar — o contexto antigo verificava só "B trabalhou algum dia" e disparava
        // incorrectamente; o contexto corrigido verifica co-presença no mesmo slot.

        LocalDate dia = inicio.plusDays(2);
        Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(14, 0));
        Turno tarde  = turno(2, "tarde", LocalTime.of(15, 0), LocalTime.of(20, 0));

        Lojautilizador ligA = colaborador(1, "Ana");
        Lojautilizador ligB = colaborador(2, "Bruno");

        EstadoColaborador estadoA = new EstadoColaborador(ligA, 9_600L, Set.of(), validator);

        // Histórico: B trabalhou hoje mas no turno da tarde (não da manhã)
        Horario hBTarde = new Horario();
        hBTarde.setIdLojautilizador(ligB);
        hBTarde.setIdTurno(tarde);
        hBTarde.setDataTurno(dia);
        List<Horario> historico = List.of(hBTarde);

        // Contexto sem colega no slot manhã de `dia`
        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(historico);

        // Pedido: A prefere trabalhar com B
        PedidoGeracao pedidoComPref = pedido(ligA, ligB, manha, tarde, Map.of(1, Set.of(2)));
        PedidoGeracao pedidoSemPref = pedido(ligA, ligB, manha, tarde, Map.of());

        long minutos = validator.calcularDuracaoEmMinutos(manha);
        double scoreComPref = avaliador.pontuar(estadoA, manha, minutos, dia, pedidoComPref, ctx);
        double scoreSemPref = avaliador.pontuar(estadoA, manha, minutos, dia, pedidoSemPref, ctx);

        // O bónus NÃO deve ter disparado: B está noutro turno, não no slot manhã
        assertTrue(Math.abs(scoreComPref - scoreSemPref) < 1.0,
                "Bónus de colega não deve disparar quando o colega está noutro tipo de turno."
                        + " comPref=" + scoreComPref + " semPref=" + scoreSemPref);
    }

    @Test
    void bonusColegaDisparaComColegaNoMesmoSlot() {
        // A prefere trabalhar com B. B já está confirmado no mesmo dia E no mesmo tipo
        // de turno (manhã) que estamos a avaliar para A. O bónus DEVE disparar.

        LocalDate dia = inicio.plusDays(2);
        Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(14, 0));
        Turno tarde  = turno(2, "tarde", LocalTime.of(15, 0), LocalTime.of(20, 0));

        Lojautilizador ligA = colaborador(1, "Ana");
        Lojautilizador ligB = colaborador(2, "Bruno");

        EstadoColaborador estadoA = new EstadoColaborador(ligA, 9_600L, Set.of(), validator);

        // Histórico: B trabalhou hoje DE MANHÃ (mesmo tipo de turno)
        Horario hBManha = new Horario();
        hBManha.setIdLojautilizador(ligB);
        hBManha.setIdTurno(manha);
        hBManha.setDataTurno(dia);
        List<Horario> historico = List.of(hBManha);

        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(historico);

        PedidoGeracao pedidoComPref = pedido(ligA, ligB, manha, tarde, Map.of(1, Set.of(2)));
        PedidoGeracao pedidoSemPref = pedido(ligA, ligB, manha, tarde, Map.of());

        long minutos = validator.calcularDuracaoEmMinutos(manha);
        double scoreComPref = avaliador.pontuar(estadoA, manha, minutos, dia, pedidoComPref, ctx);
        double scoreSemPref = avaliador.pontuar(estadoA, manha, minutos, dia, pedidoSemPref, ctx);

        // O bónus DEVE ter disparado: B está no mesmo slot → pontuação de A é melhor (menor)
        assertTrue(scoreComPref < scoreSemPref,
                "Bónus de colega deve disparar quando o colega está no mesmo slot (dia + turno)."
                        + " comPref=" + scoreComPref + " semPref=" + scoreSemPref);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome) {
        Utilizador u = new Utilizador(); u.setId(id); u.setNome(nome);
        Cargo c = new Cargo(); c.setNome("Assistente"); c.setTipo("parttime");
        Lojautilizador lig = new Lojautilizador(); lig.setId(id); lig.setIdUtilizador(u); lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime hi, LocalTime hf) {
        Turno t = new Turno(); t.setId(id); t.setTipo(tipo); t.setHoraInicio(hi); t.setHoraFim(hf);
        return t;
    }

    private PedidoGeracao pedido(Lojautilizador ligA, Lojautilizador ligB,
                                  Turno manha, Turno tarde,
                                  Map<Integer, Set<Integer>> paresPref) {
        return new PedidoGeracao(
                List.of(ligA, ligB),
                List.of(manha, tarde),
                inicio, inicio.plusDays(6),
                Map.of(1, 1, 2, 1),
                31, 11, 1, 1,
                false, Set.of(),
                Map.of(1, 9_600L, 2, 9_600L),
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS,
                Map.of(), paresPref, 42L);
    }
}
