package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que o motor atribui dois turnos consecutivos por dia a colaboradores FT
 * (fulltime/gerente/subgerente/supervisor), através das Fases 1.5 e 2.5. A capacidade
 * contratual do FT é suficiente para 2 turnos/dia; os turnos são consecutivos (Manhã
 * termina onde Tarde começa).
 */
class FullTimeDuplaTurnoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Semana de segunda a sexta — sem fins de semana para isolar a lógica FT.
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4);

    // Turnos CONSECUTIVOS: Manhã termina onde Tarde começa → eTurnoConsecutivo=true.
    private final Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(14, 0));
    private final Turno tarde = turno(2, "tarde", LocalTime.of(14, 0), LocalTime.of(19, 0));

    @Test
    void gerenteFTRecebeSegundoTurnoConsecutivoNoMesmoDia() {
        // 1 Gerente (FT) + 2 parttime para cobrir os mínimos.
        // Gerente tem capacidade para 2 turnos/dia × 5 dias = 50h.
        // PT têm capacidade para 1 turno/dia × 5 dias = 25h.
        Lojautilizador gerente = colaborador(1, "Gerente", "gerente");
        Lojautilizador pt1    = colaborador(2, "PT1", "parttime");
        Lojautilizador pt2    = colaborador(3, "PT2", "parttime");

        // Capacidade em minutos: Gerente 3000 min (50h), PT 1500 min (25h).
        Map<Integer, Long> cargas = Map.of(1, 3_000L, 2, 1_500L, 3, 1_500L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(gerente, pt1, pt2),
                List.of(manha, tarde),
                inicio, fim,
                Map.of(1, 1, 2, 1), // mínimo 1 por turno por dia
                31, 11, 2, 2,
                false,
                Set.of(1), // gerente é chefia ao sábado
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        // Conta quantos dias o Gerente tem 2 turnos.
        Map<LocalDate, Long> turnosPorDia = new HashMap<>();
        for (Horario h : horarios) {
            if (h.getIdLojautilizador().getIdUtilizador().getId() == 1) {
                turnosPorDia.merge(h.getDataTurno(), 1L, Long::sum);
            }
        }

        long diasComDoisTurnos = turnosPorDia.values().stream().filter(n -> n == 2).count();
        assertTrue(diasComDoisTurnos >= 3,
                "Gerente FT deve ter 2 turnos consecutivos em pelo menos 3 dos 5 dias úteis. "
                        + "Dias com 2 turnos: " + diasComDoisTurnos
                        + ", distribuição: " + turnosPorDia);
    }

    @Test
    void ftComCapacidadeEsgotadaNaoRecebeSegundoTurno() {
        // FT com capacidade apenas para 1 turno total no mês: não deve receber o 2º turno.
        Lojautilizador ft = colaborador(1, "FT-Minimo", "fulltime");
        Lojautilizador pt = colaborador(2, "PT1", "parttime");

        // Capacidade FT = 300 min (5h = exatamente 1 turno); PT = 3000 min.
        Map<Integer, Long> cargas = Map.of(1, 300L, 2, 3_000L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(ft, pt),
                List.of(manha, tarde),
                inicio, inicio, // apenas 1 dia
                Map.of(1, 1, 2, 1),
                31, 11, 2, 2, false, Set.of(), cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        // FT só tem capacidade para 1 turno (300 min = 5h = 1 turno de 5h).
        long turnosFT = horarios.stream()
                .filter(h -> h.getIdLojautilizador().getIdUtilizador().getId() == 1)
                .count();
        assertTrue(turnosFT <= 1,
                "FT com capacidade para apenas 1 turno não deve receber o 2º. Turnos atribuídos: " + turnosFT);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome(nome);
        c.setTipo(tipoCargo);
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
}
