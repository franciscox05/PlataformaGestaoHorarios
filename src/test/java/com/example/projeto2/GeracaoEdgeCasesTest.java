package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.FalhaGeracaoHorarioException;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Edge cases degenerados do motor de geração: equipa vazia, loja sem turnos base
 * e cargas contratuais a zero. Sem os guards de entrada, estes casos caíam em
 * mensagens enganadoras ("a equipa só tem 0h", "turnos compatíveis com a exceção"
 * num dia sem exceção) em vez de apontar a causa real.
 */
class GeracaoEdgeCasesTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

    @Test
    void equipaVaziaFalhaComMensagemClara() {
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));
        PedidoGeracao pedido = pedido(List.of(), List.of(turno), Map.of(1, 1),
                Map.of(), inicio, inicio.plusDays(4));

        FalhaGeracaoHorarioException falha = assertThrows(FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido));

        assertTrue(falha.getMessage().contains("colaborador"),
                "A mensagem deve dizer que faltam colaboradores, não falar em capacidade: " + falha.getMessage());
        assertFalse(falha.sugestoes().isEmpty(),
                "A falha de equipa vazia deve trazer uma sugestão acionável.");
    }

    @Test
    void semTurnosBaseFalhaComMensagemClara() {
        List<Lojautilizador> colaboradores = List.of(colaborador(1, "A", "fulltime"));
        PedidoGeracao pedido = pedido(colaboradores, List.of(), Map.of(),
                Map.of(1, 9_600L), inicio, inicio.plusDays(4));

        FalhaGeracaoHorarioException falha = assertThrows(FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido));

        assertTrue(falha.getMessage().contains("turnos base"),
                "A mensagem deve apontar a falta de turnos base: " + falha.getMessage());
        assertFalse(falha.getMessage().contains("exce"),
                "Sem exceções configuradas, a mensagem não deve falar em exceções: " + falha.getMessage());
        assertFalse(falha.sugestoes().isEmpty(),
                "A falha por falta de turnos deve trazer uma sugestão acionável.");
    }

    @Test
    void cargaContratualZeroEmTodaAEquipaFalhaPorCapacidade() {
        List<Lojautilizador> colaboradores = List.of(
                colaborador(1, "A", "fulltime"), colaborador(2, "B", "fulltime"));
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));
        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 1),
                Map.of(1, 0L, 2, 0L), inicio, inicio.plusDays(4));

        FalhaGeracaoHorarioException falha = assertThrows(FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido));

        assertTrue(falha.getMessage().contains("capacidade") || falha.getMessage().contains("carga contratual"),
                "Com toda a equipa a 0h, a falha deve ser de capacidade: " + falha.getMessage());
        assertFalse(falha.sugestoes().isEmpty(),
                "A falha de capacidade deve trazer sugestões acionáveis.");
    }

    @Test
    void colaboradorComCargaZeroNaoRecebeTurnosMasGeracaoConclui() {
        // C tem carga 0 (p.ex. contrato suspenso); A e B chegam para os mínimos.
        List<Lojautilizador> colaboradores = List.of(
                colaborador(1, "A", "fulltime"),
                colaborador(2, "B", "fulltime"),
                colaborador(3, "C", "fulltime"));
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));
        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 1),
                Map.of(1, 4_800L, 2, 4_800L, 3, 0L), inicio, inicio.plusDays(4));

        List<Horario> horarios = engine.gerar(pedido);

        assertFalse(horarios.isEmpty(), "A geração deve concluir com a capacidade de A e B.");
        assertTrue(horarios.stream().noneMatch(h ->
                        h.getIdLojautilizador().getIdUtilizador().getId() == 3),
                "Um colaborador com carga contratual 0 nunca pode receber turnos.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores, List<Turno> turnos,
                                 Map<Integer, Integer> minimosPorTurno, Map<Integer, Long> cargas,
                                 LocalDate dataInicio, LocalDate dataFim) {
        return new PedidoGeracao(
                colaboradores,
                turnos,
                dataInicio,
                dataFim,
                minimosPorTurno,
                31,
                11,
                2,
                2,
                false,
                Set.of(),
                new HashMap<>(cargas),
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
