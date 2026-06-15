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
import com.example.projeto2.API.Services.geracao.TurnoClassifier;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O gestor pode definir, no passo "Configurar", o nº alvo de pessoas por turno. O motor
 * tenta atingi-lo no preenchimento de capacidade, sem nunca descer abaixo do mínimo das
 * regras nem ultrapassar o alvo por tipo de turno.
 */
class AlvoPorTurnoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Semana de segunda a sexta (sem fins de semana, para isolar a cobertura).
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // Seg–Sex

    private final Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));
    private final Turno tarde = turno(2, "tarde", LocalTime.of(13, 0), LocalTime.of(21, 0));

    @Test
    void alvoDoisEnchecadaTipoDeTurnoAteDois() {
        // 6 colaboradores, capacidade folgada → o alvo de 2/turno é alcançável todos os dias.
        List<Lojautilizador> colaboradores = criarColaboradores(6);
        PedidoGeracao pedido = pedido(colaboradores, Map.of(1, 1, 2, 1), 9_600L, 2);

        List<Horario> horarios = engine.gerar(pedido);

        Map<LocalDate, Map<String, Integer>> contagem = contarPorDiaTipo(horarios);
        for (Map<String, Integer> porTipo : contagem.values()) {
            assertTrue(porTipo.getOrDefault("manha", 0) <= 2, "Nunca ultrapassa o alvo (manhã).");
            assertTrue(porTipo.getOrDefault("tarde", 0) <= 2, "Nunca ultrapassa o alvo (tarde).");
        }
        // Capacidade folgada: cada dia útil deve mesmo chegar a 2+2.
        long diasCom4 = contagem.values().stream()
                .filter(m -> m.getOrDefault("manha", 0) == 2 && m.getOrDefault("tarde", 0) == 2)
                .count();
        assertEquals(5, diasCom4, "Com capacidade de sobra, cada dia útil chega ao alvo 2/turno.");
    }

    @Test
    void alvoUmMantemApenasOMinimo() {
        // alvo=1 → sem reforço: cada tipo de turno fica exatamente com 1 por dia.
        List<Lojautilizador> colaboradores = criarColaboradores(6);
        PedidoGeracao pedido = pedido(colaboradores, Map.of(1, 1, 2, 1), 9_600L, 1);

        List<Horario> horarios = engine.gerar(pedido);

        Map<LocalDate, Map<String, Integer>> contagem = contarPorDiaTipo(horarios);
        assertEquals(5, contagem.size(), "Os 5 dias úteis estão cobertos.");
        for (Map<String, Integer> porTipo : contagem.values()) {
            assertEquals(1, porTipo.getOrDefault("manha", 0), "1 manhã/dia (só o mínimo).");
            assertEquals(1, porTipo.getOrDefault("tarde", 0), "1 tarde/dia (só o mínimo).");
        }
        assertEquals(10, horarios.size(), "5 dias × 2 turnos mínimos = 10, sem reforço.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Map<LocalDate, Map<String, Integer>> contarPorDiaTipo(List<Horario> horarios) {
        Map<LocalDate, Map<String, Integer>> contagem = new HashMap<>();
        for (Horario h : horarios) {
            contagem.computeIfAbsent(h.getDataTurno(), d -> new HashMap<>())
                    .merge(TurnoClassifier.tipoNormalizado(h.getIdTurno()), 1, Integer::sum);
        }
        return contagem;
    }

    private List<Lojautilizador> criarColaboradores(int n) {
        return java.util.stream.IntStream.rangeClosed(1, n)
                .mapToObj(i -> {
                    Utilizador u = new Utilizador();
                    u.setId(i);
                    u.setNome("Colab" + i);
                    Cargo c = new Cargo();
                    c.setNome("func");
                    c.setTipo("fulltime");
                    Lojautilizador lig = new Lojautilizador();
                    lig.setId(i);
                    lig.setIdUtilizador(u);
                    lig.setIdCargo(c);
                    return lig;
                }).toList();
    }

    private Turno turno(int id, String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(horaInicio);
        t.setHoraFim(horaFim);
        return t;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 Map<Integer, Integer> minimosPorTurno,
                                 long cargaMinutos,
                                 Integer alvoPorTurno) {
        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), cargaMinutos);
        }
        return new PedidoGeracao(
                colaboradores,
                List.of(manha, tarde),
                inicio,
                fim,
                minimosPorTurno,
                31,     // maxDiasConsecutivos
                11,     // descansoMinimoHoras
                2,      // descansoSemanalMinimoDias
                2,      // janelaRotacaoFimDeSemana
                false,  // exigirChefiaAoSabado
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
                42L,
                alvoPorTurno);
    }
}
