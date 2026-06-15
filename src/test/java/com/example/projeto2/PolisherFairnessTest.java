package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Services.geracao.PolisherHorario;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Quando um turno é impopular (toda a gente prefere manhã) e <em>alguém tem de o fazer</em>,
 * o polisher deve repartir esse turno pelos colaboradores em vez de o despejar sempre no
 * mesmo — graças à penalização quadrática por colaborador.
 */
class PolisherFairnessTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final PolisherHorario polisher = new PolisherHorario(validator);

    private final LocalDate segunda = LocalDate.now().plusMonths(1).withDayOfMonth(8)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @Test
    void repartirTurnoImpopularEntreColaboradores() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(14, 0));
        Turno tarde = turno(2, "tarde", LocalTime.of(16, 0), LocalTime.of(21, 0));

        // Ambos preferem manhã. Plano inicial injusto: a Ana apanha a tarde (impopular)
        // todos os 4 dias; o Bruno fica sempre com a manhã.
        List<Horario> plano = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            plano.add(horario(ana, tarde, segunda.plusDays(i)));
            plano.add(horario(bruno, manha, segunda.plusDays(i)));
        }

        Map<Integer, List<Preferencia>> prefs = Map.of(
                1, List.of(preferencia("turno manha")),
                2, List.of(preferencia("turno manha")));

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(manha, tarde), prefs);
        polisher.polir(pedido, plano);

        long tardeAna = plano.stream().filter(h -> idDe(h) == 1 && h.getIdTurno().getId() == 2).count();
        long tardeBruno = plano.stream().filter(h -> idDe(h) == 2 && h.getIdTurno().getId() == 2).count();

        assertEquals(4, tardeAna + tardeBruno, "A cobertura da tarde mantém-se (4 turnos).");
        assertTrue(Math.abs(tardeAna - tardeBruno) <= 1,
                "A tarde impopular devia ter sido repartida (Ana=" + tardeAna + ", Bruno=" + tardeBruno + ").");
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private int idDe(Horario h) { return h.getIdLojautilizador().getIdUtilizador().getId(); }

    private Lojautilizador colaborador(int id, String nome) {
        Utilizador u = new Utilizador(); u.setId(id); u.setNome(nome);
        Cargo c = new Cargo(); c.setNome("Assistente PT"); c.setTipo("parttime");
        Lojautilizador lig = new Lojautilizador(); lig.setId(id); lig.setIdUtilizador(u); lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime hi, LocalTime hf) {
        Turno t = new Turno(); t.setId(id); t.setTipo(tipo); t.setHoraInicio(hi); t.setHoraFim(hf);
        return t;
    }

    private Horario horario(Lojautilizador lig, Turno t, LocalDate d) {
        Horario h = new Horario(); h.setIdLojautilizador(lig); h.setIdTurno(t); h.setDataTurno(d);
        return h;
    }

    private Preferencia preferencia(String descricao) {
        Preferencia p = new Preferencia();
        p.setDescricao(descricao);
        p.setDataInicio(segunda);
        p.setDataFim(segunda.plusDays(6));
        return p;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores, List<Turno> turnos,
                                 Map<Integer, List<Preferencia>> preferenciasTurnos) {
        return new PedidoGeracao(
                colaboradores, turnos, segunda, segunda.plusDays(6),
                Map.of(), 6, 11, 1, 1, false, Set.of(),
                Map.of(1, 9_600L, 2, 9_600L),
                Map.of(), preferenciasTurnos, Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS, Map.of(), Map.of(), 42L);
    }
}
