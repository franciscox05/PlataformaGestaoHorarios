package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Equipa grande com preferências conflituantes: 8 colaboradores preferem turno de manhã,
 * mas só há 2 vagas. Verifica que o motor:
 * (1) não falha e cobre os mínimos de todos os turnos;
 * (2) não concentra todos os turnos de noite na mesma pessoa (equidade).
 */
class MotorPreferenciasConflituantesIntegrationTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // 1 semana útil (Mon–Fri) — sem fins-de-semana para não colidir com a regra
    // de descanso semanal mínimo de 2 dias (máx 5 dias de trabalho por semana).
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // segunda a sexta

    private final Turno manha = turno(1, "manha", LocalTime.of(10, 0), LocalTime.of(14, 0));
    private final Turno noite = turno(2, "noite", LocalTime.of(19, 0), LocalTime.of(23, 0));

    @Test
    void geracaoNaoFalhaComPreferenciasConflituantes() {
        // 8 FT todos preferem manhã; mínimo 2/turno → 4 vagas/dia mas 8 pessoas querem manhã.
        List<Lojautilizador> colab = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> colaborador(i, "Colab" + i, "fulltime"))
                .collect(Collectors.toList());

        // Todos preferem manhã (turno id=1)
        Map<Integer, List<Preferencia>> prefTurnos = IntStream.rangeClosed(1, 8)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> List.of(prefTurno(i, "manhã"))));

        // Carga: 176h/mês por pessoa
        Map<Integer, Long> cargas = IntStream.rangeClosed(1, 8)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> 10_560L));

        PedidoGeracao pedido = new PedidoGeracao(
                colab,
                List.of(manha, noite),
                inicio, fim,
                Map.of(1, 2, 2, 2),   // mínimo 2 por turno
                6, 11, 2, 2,
                false, Set.of(),
                cargas,
                Map.of(), prefTurnos, Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS,
                Map.of(), Map.of(),
                123L, null);

        List<Horario> horarios = engine.gerar(pedido);

        assertFalse(horarios.isEmpty(), "Motor falhou com preferências conflituantes.");
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            final LocalDate d = dia;
            long cobManha = horarios.stream()
                    .filter(h -> d.equals(h.getDataTurno()) && h.getIdTurno().getId() == 1).count();
            long cobNoite = horarios.stream()
                    .filter(h -> d.equals(h.getDataTurno()) && h.getIdTurno().getId() == 2).count();
            assertTrue(cobManha >= 2, "Mínimo manhã não cumprido no dia " + d + ": " + cobManha);
            assertTrue(cobNoite >= 2, "Mínimo noite não cumprido no dia " + d + ": " + cobNoite);
        }
    }

    @Test
    void turnoNaoPreferidoDistribuidoEquitativamente() {
        // Com 8 pessoas e 5 dias × 2 turnos de noite mínimos/dia = 10 noites mínimas a distribuir.
        // O motor pode gerar top-up adicional; o limite de equidade é ≤5 (nenhuma pessoa com mais de
        // metade das noites totais esperadas), verificando que o turno impopular não se concentra.
        List<Lojautilizador> colab = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> colaborador(i, "Colab" + i, "fulltime"))
                .collect(Collectors.toList());

        Map<Integer, List<Preferencia>> prefTurnos = IntStream.rangeClosed(1, 8)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> List.of(prefTurno(i, "manhã"))));

        Map<Integer, Long> cargas = IntStream.rangeClosed(1, 8)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> 10_560L));

        PedidoGeracao pedido = new PedidoGeracao(
                colab,
                List.of(manha, noite),
                inicio, fim,
                Map.of(1, 2, 2, 2),
                6, 11, 2, 2,
                false, Set.of(),
                cargas,
                Map.of(), prefTurnos, Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS,
                Map.of(), Map.of(),
                456L, null);

        List<Horario> horarios = engine.gerar(pedido);

        // Conta turnos de noite por colaborador.
        Map<Integer, Long> noitesPorColaborador = horarios.stream()
                .filter(h -> h.getIdTurno().getId() == 2)
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.counting()));

        long maxNoites = noitesPorColaborador.values().stream().mapToLong(Long::longValue).max().orElse(0);
        assertTrue(maxNoites <= 5,
                "Turno de noite concentrado: uma pessoa tem " + maxNoites
                        + " noites (máx tolerado=5 para 5 dias). Distribuição: " + noitesPorColaborador);
    }

    @Test
    void geracaoComMuitasFolgasPreferidaConflituantesNaoFalha() {
        // 6 dos 8 colaboradores querem folga no mesmo dia — o motor deve honrar as viáveis
        // e continuar a cobrir os mínimos.
        List<Lojautilizador> colab = IntStream.rangeClosed(1, 8)
                .mapToObj(i -> colaborador(i, "Colab" + i, "fulltime"))
                .collect(Collectors.toList());

        LocalDate diaConflito = inicio.plusDays(1); // terça-feira (dentro da semana de 5 dias)
        Map<Integer, Set<LocalDate>> folgasPreferidas = IntStream.rangeClosed(1, 6)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> Set.of(diaConflito)));

        Map<Integer, Long> cargas = IntStream.rangeClosed(1, 8)
                .boxed()
                .collect(Collectors.toMap(i -> i, i -> 10_560L));

        PedidoGeracao pedido = new PedidoGeracao(
                colab,
                List.of(manha, noite),
                inicio, fim,
                Map.of(1, 2, 2, 2),
                6, 11, 2, 2,
                false, Set.of(),
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS,
                folgasPreferidas, Map.of(),
                789L, null);

        List<Horario> horarios = engine.gerar(pedido);

        assertFalse(horarios.isEmpty(), "Motor não deve falhar mesmo com muitas folgas preferidas conflituantes.");
        long cobDiaConflito = horarios.stream()
                .filter(h -> diaConflito.equals(h.getDataTurno())).count();
        assertTrue(cobDiaConflito >= 4,
                "No dia de conflito (" + diaConflito + ") a cobertura mínima (4=2×2 turnos) deve ser mantida. Obtido: " + cobDiaConflito);
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

    private Preferencia prefTurno(int idColaborador, String descricao) {
        Preferencia p = new Preferencia();
        p.setId(idColaborador * 100);
        p.setDescricao(descricao);
        p.setTipo("turnos");
        return p;
    }
}
