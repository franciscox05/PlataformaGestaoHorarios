package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) do tie-break de preferência de turno no
 * {@link AvaliadorAtribuicao}: verifica que o bónus base independente da
 * política garante a preferência em empate técnico.
 */
class PreferenciaTurnoTieBreakTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(validator);

    // Semana de dias úteis: evita complicações de FDS para isolar o tie-break
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // Seg–Sex

    @Test
    void avaliadorPrefereTurnoComPreferenciaEmEmpate() {
        // A tem preferência de manhã; B não tem. Tudo o resto idêntico.
        // Com o tie-break, scoreA < scoreB ao avaliar o turno manhã.
        Lojautilizador ligA = colaborador(1, "A", "fulltime");
        Lojautilizador ligB = colaborador(2, "B", "fulltime");

        EstadoColaborador estadoA = new EstadoColaborador(ligA, 9_600L, Set.of(), validator);
        EstadoColaborador estadoB = new EstadoColaborador(ligB, 9_600L, Set.of(), validator);

        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        long minutos = validator.calcularDuracaoEmMinutos(manha);
        LocalDate data = inicio;

        Preferencia prefManha = preferencia("manha", inicio.minusMonths(1), fim.plusMonths(6));
        PedidoGeracao pedido = pedido(
                List.of(ligA, ligB),
                List.of(manha),
                Map.of(1, 1),
                Map.of(1, List.of(prefManha)),
                false);

        AvaliadorAtribuicao.ContextoAvaliacao ctx = avaliador.novoContexto(List.of());
        double scoreA = avaliador.pontuar(estadoA, manha, minutos, data, pedido, ctx);
        double scoreB = avaliador.pontuar(estadoB, manha, minutos, data, pedido, ctx);

        assertTrue(scoreA < scoreB,
                "O colaborador com preferência de manhã deve ter pontuação inferior (melhor).");
    }

    @Test
    void motorAtribuiTurnoPreferidoAoColaboradorCorreto() {
        // A prefere manhã, B prefere tarde. Uma semana de dias úteis com 1 slot de cada
        // tipo por dia. Todos os outros factores são idênticos — o tie-break deve garantir
        // que cada colaborador fica no turno preferido em todos os dias.
        HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

        Lojautilizador ligA = colaborador(1, "A", "fulltime");
        Lojautilizador ligB = colaborador(2, "B", "fulltime");

        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));
        Turno tarde  = turno(2, "tarde", LocalTime.of(14, 0), LocalTime.of(22, 0));

        Preferencia prefManha = preferencia("manha", inicio.minusMonths(1), fim.plusMonths(6));
        Preferencia prefTarde = preferencia("tarde",  inicio.minusMonths(1), fim.plusMonths(6));

        PedidoGeracao pedido = pedido(
                List.of(ligA, ligB),
                List.of(manha, tarde),
                Map.of(1, 1, 2, 1),
                Map.of(1, List.of(prefManha), 2, List.of(prefTarde)),
                false);

        List<Horario> horarios = engine.gerar(pedido);

        for (Horario h : horarios) {
            int id = h.getIdLojautilizador().getIdUtilizador().getId();
            String tipo = h.getIdTurno().getTipo();
            if (id == 1) {
                assertEquals("manha", tipo,
                        "A (prefere manhã) foi colocado no turno errado em " + h.getDataTurno());
            } else {
                assertEquals("tarde", tipo,
                        "B (prefere tarde) foi colocado no turno errado em " + h.getDataTurno());
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome, String tipo) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome(nome);
        c.setTipo(tipo);
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

    private Preferencia preferencia(String descricao, LocalDate dataInicio, LocalDate dataFim) {
        Preferencia p = new Preferencia();
        p.setId(1);
        p.setDescricao(descricao);
        p.setDataInicio(dataInicio);
        p.setDataFim(dataFim);
        p.setPrioridade(1);
        p.setEstado("aprovada");
        return p;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                  List<Turno> turnos,
                                  Map<Integer, Integer> minimosPorTurno,
                                  Map<Integer, List<Preferencia>> preferenciasTurnos,
                                  boolean exigirChefia) {
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
                11,
                2,
                2,
                exigirChefia,
                Set.of(),
                cargas,
                Map.of(),
                preferenciasTurnos,
                Map.of(),
                List.of(),
                null,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),
                Map.of(),
                42L);
    }
}
