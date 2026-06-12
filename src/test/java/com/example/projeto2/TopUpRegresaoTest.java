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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de regressão para o fix de limiteTopUp=0 no motor de geração.
 *
 * <p>Antes do fix: {@code numTipos / minimoMaxGlobal} podia dar 0 quando
 * {@code numTipos < minimoMaxGlobal} (ex: 2 tipos / min=3 = 0). Com limiteTopUp=0
 * a fase de top-up nunca gerava turnos extra, limitando a ocupação ao mínimo diário.
 *
 * <p>Após o fix: {@code Math.max(1L, numTipos / Math.max(1, minimoMaxGlobal))} garante
 * pelo menos 1 candidato extra por dia, evitando que colaboradores fiquem com carga
 * muito abaixo da contratual.
 */
class TopUpRegresaoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Semana de segunda a sexta (5 dias úteis sem FDS) para isolar a lógica de top-up
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // Seg–Sex (5 dias)

    /**
     * Cenário-chave do bug: 2 tipos de turno, mínimo 3 por tipo (minimoMaxGlobal=3).
     * <p>Antes do fix: limiteTopUp = 2/3 = 0 → sem top-up → total = 6 turnos/dia
     * para 8 colaboradores → ~15 turnos em 20 dias → carga fortemente subaproveitada.
     * <p>Após o fix: limiteTopUp = max(1, 2/3) = 1 → pelo menos 1 top-up por dia
     * → total >= 7 turnos/dia → ~17-18 turnos/colaborador em 20 dias.
     */
    @Test
    void topUpGeraAoMenosUmTurnoExtraPorDiaComDoisTiposEMinimoAlto() {
        List<Lojautilizador> colaboradores = criarColaboradores(8);
        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        Turno tarde  = turno(2, "tarde",  LocalTime.of(14, 0), LocalTime.of(22, 0));

        // minimosPorTurno = {1: 3, 2: 3} → minimoMaxGlobal=3, numTipos=2
        // Sem fix: limiteTopUp = 2/3 = 0; Com fix: max(1, 2/3) = 1
        // Com 8 colaboradores, 6 mínimo por dia, 5 dias: sem top-up = 30; com top-up > 30
        PedidoGeracao pedido = pedido(
                colaboradores,
                List.of(manha, tarde),
                Map.of(1, 3, 2, 3), // 3 de cada → 6 mínimo por dia
                9_600L);

        List<Horario> horarios = engine.gerar(pedido);

        // 5 dias × 6 mínimo/dia = 30; com o fix limiteTopUp=1 → pelo menos 1 extra/dia
        long minimoSemTopUp = 5L * 6;
        assertTrue(horarios.size() > minimoSemTopUp,
                "Com o fix de limiteTopUp, o total de turnos deve exceder o mínimo de "
                        + minimoSemTopUp + "; gerado=" + horarios.size());
    }

    @Test
    void topUpNaoUltrapassaCargaContratualDosColaboradores() {
        // Garante que o top-up extra não sobrecarrega os colaboradores.
        // 5 colaboradores, 1 turno de 8h, mínimo 2/dia, 5 dias.
        // Carga = 5 × 480 min = 2400 min → max 5 turnos por colaborador (exatamente 5 dias)
        List<Lojautilizador> colaboradores = criarColaboradores(5);
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        long cargaMinutos = 5L * 480; // exactamente 5 turnos de 8h
        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 2), cargaMinutos);

        List<Horario> horarios = engine.gerar(pedido);

        long maxTurnosPorColaborador = cargaMinutos / 480L; // 5

        Map<Integer, Long> turnosPorColaborador = horarios.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.counting()));

        for (Map.Entry<Integer, Long> entry : turnosPorColaborador.entrySet()) {
            assertTrue(entry.getValue() <= maxTurnosPorColaborador,
                    "Colaborador " + entry.getKey() + " recebeu " + entry.getValue()
                            + " turnos, excedendo a carga máxima de " + maxTurnosPorColaborador);
        }
    }

    @Test
    void topUpEquilibraCargaEntreColaboradores() {
        // Com top-up activo e 4 colaboradores com 1 slot/dia (mínimo 1), cada deve
        // receber turnos equitativos — amplitude máxima de 1 em 5 dias.
        List<Lojautilizador> colaboradores = criarColaboradores(4);
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 1), 9_600L);

        List<Horario> horarios = engine.gerar(pedido);
        assertFalse(horarios.isEmpty(), "Devem existir horários gerados.");

        Map<Integer, Long> turnosPorColaborador = horarios.stream()
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.counting()));

        long max = turnosPorColaborador.values().stream().mapToLong(l -> l).max().orElse(0);
        long min = turnosPorColaborador.values().stream().mapToLong(l -> l).min().orElse(0);

        // Em 5 dias com 4 colaboradores: 1+top-up = 5+extra turnos distribuídos por 4.
        // Amplitude aceitável: 2 dias (distribuição justa por carga)
        assertTrue(max - min <= 2,
                "A amplitude de carga entre colaboradores é demasiado alta (max=" + max
                        + ", min=" + min + "). O top-up deve equilibrar a distribuição.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores, List<Turno> turnos,
                                  Map<Integer, Integer> minimosPorTurno, long cargaMinutos) {
        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), cargaMinutos);
        }
        return new PedidoGeracao(
                colaboradores,
                turnos,
                inicio,
                fim,
                minimosPorTurno,
                31,
                11,
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
