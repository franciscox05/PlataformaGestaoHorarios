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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cenário minimalista: 1 gerente + 1 fulltime + 1 parttime, 1 turno por dia, semana útil.
 * Verifica que o motor gera sempre mesmo com a equipa no limite de capacidade.
 */
class MotorEquipaMinimaIntegrationTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // segunda a sexta

    private final Turno manha = turno(1, "manha", LocalTime.of(10, 0), LocalTime.of(14, 0));

    @Test
    void geracaoNaoFalhaComEquipaMinima() {
        // 1 gerente + 1 FT + 1 PT — mínimo 1 pessoa/turno → 5 dias → 5 atribuições necessárias.
        // Capacidade: gerente 176h=10560min, FT 176h, PT 96h=5760min — mais do que suficiente.
        Lojautilizador gerente = colaborador(1, "Gerente", "gerente");
        Lojautilizador ft      = colaborador(2, "FT",      "fulltime");
        Lojautilizador pt      = colaborador(3, "PT",      "parttime");

        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 5_760L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(gerente, ft, pt),
                List.of(manha),
                inicio, fim,
                Map.of(1, 1),          // mínimo 1/turno
                6, 11, 2, 2,
                false, Set.of(1),
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        assertFalse(horarios.isEmpty(), "Motor falhou com equipa mínima mas capacidade suficiente.");
        // O motor gera mínimos + top-up; com 3 pessoas e carga alta o total ultrapassa o mínimo de 5.
        assertTrue(horarios.size() >= 5,
                "Com 1 turno/dia e 5 dias úteis, o motor deve gerar pelo menos 5 atribuições. Obtido: " + horarios.size());
    }

    @Test
    void cargaContratualNaoEUltrapassada() {
        Lojautilizador gerente = colaborador(1, "Gerente", "gerente");
        Lojautilizador ft      = colaborador(2, "FT",      "fulltime");
        Lojautilizador pt      = colaborador(3, "PT",      "parttime");

        // PT tem apenas capacidade para 2 turnos (480 min = 2 × 240 min).
        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 480L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(gerente, ft, pt),
                List.of(manha),
                inicio, fim,
                Map.of(1, 1),
                6, 11, 2, 2,
                false, Set.of(1),
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        long turnosPT = horarios.stream()
                .filter(h -> h.getIdLojautilizador().getIdUtilizador().getId() == 3)
                .count();
        assertTrue(turnosPT <= 2,
                "PT com 480 min não deve exceder 2 turnos de 240 min. Atribuídos: " + turnosPT);
    }

    @Test
    void coberturaMinimaCumpridaTodosOsDias() {
        Lojautilizador gerente = colaborador(1, "Gerente", "gerente");
        Lojautilizador ft      = colaborador(2, "FT",      "fulltime");
        Lojautilizador pt      = colaborador(3, "PT",      "parttime");

        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 5_760L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(gerente, ft, pt),
                List.of(manha),
                inicio, fim,
                Map.of(1, 1),
                6, 11, 2, 2,
                false, Set.of(1),
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            final LocalDate d = dia;
            long cobertura = horarios.stream().filter(h -> d.equals(h.getDataTurno())).count();
            assertTrue(cobertura >= 1, "Dia " + d + " ficou sem cobertura mínima.");
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador(); u.setId(id); u.setNome(nome);
        Cargo c = new Cargo(); c.setNome(nome); c.setTipo(tipoCargo);
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id); lig.setIdUtilizador(u); lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime ini, LocalTime fim2) {
        Turno t = new Turno(); t.setId(id); t.setTipo(tipo);
        t.setHoraInicio(ini); t.setHoraFim(fim2);
        return t;
    }
}
