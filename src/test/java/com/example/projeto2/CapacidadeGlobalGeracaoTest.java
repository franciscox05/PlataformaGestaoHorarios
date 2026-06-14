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
import com.example.projeto2.API.Services.geracao.RegraAplicada;
import com.example.projeto2.API.Services.geracao.RegraGeracaoResolver;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do guard de capacidade global do motor (bug reportado: a geração consumia
 * a carga da equipa no início do mês e falhava só no dia 30 com "carga esgotada"):
 *
 * <ol>
 *   <li>Pré-flight: capacidade insuficiente falha imediatamente com mensagem clara
 *       e sugestões, em vez de falhar a meio do mês.</li>
 *   <li>Orçamento de top-up: os turnos extra do início do mês não podem comprometer
 *       a cobertura mínima dos últimos dias.</li>
 *   <li>Matcher de regras: "Rotação de fins-de-semana" (plural/hífenes) é reconhecida
 *       — antes caía silenciosamente no default de 2 semanas.</li>
 * </ol>
 */
class CapacidadeGlobalGeracaoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));

    // ── Pré-flight de capacidade ─────────────────────────────────────────────

    @Test
    void capacidadeInsuficienteFalhaImediatamenteComMensagemClara() {
        // 2 colaboradores × 40h = 80h de capacidade; mínimo 2/dia × 8h × 20 dias = 320h.
        List<Lojautilizador> colaboradores = List.of(colaborador(1, "A"), colaborador(2, "B"));
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        LocalDate fim = inicio.plusDays(19);
        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 2),
                2_400L, inicio, fim); // 40h por colaborador

        FalhaGeracaoHorarioException falha = assertThrows(FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido));

        assertTrue(falha.getMessage().contains("capacidade") || falha.getMessage().contains("carga contratual"),
                "A mensagem deve explicar que é um problema de capacidade: " + falha.getMessage());
        assertFalse(falha.sugestoes().isEmpty(),
                "A falha de capacidade deve trazer sugestões acionáveis.");
        assertTrue(falha.sugestoes().stream().anyMatch(s -> s.texto().contains("mínimo")),
                "Deve sugerir reduzir os mínimos por turno.");
    }

    @Test
    void capacidadeJustaNaoDisparaOPreFlight() {
        // 5 colaboradores × 48h = 240h; mínimo 1/dia × 8h × 5 dias = 40h — sobra muita capacidade.
        List<Lojautilizador> colaboradores = List.of(
                colaborador(1, "A"), colaborador(2, "B"), colaborador(3, "C"),
                colaborador(4, "D"), colaborador(5, "E"));
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 1),
                2_880L, inicio, inicio.plusDays(4));

        List<Horario> horarios = engine.gerar(pedido);
        assertFalse(horarios.isEmpty(), "Com capacidade suficiente, a geração deve concluir.");
    }

    // ── Orçamento do top-up: fim do mês continua coberto ─────────────────────

    @Test
    void topUpNaoEsgotaCargaAntesDoFimDoPeriodo() {
        // Capacidade apertada: 3 colaboradores × 2 turnos (16h) = 6 turnos no total.
        // Mínimo 1/dia × 5 dias = 5 turnos; margem para top-up: 1 turno.
        // Sem o orçamento de capacidade, o top-up gastava a margem nos primeiros dias
        // (1 mínimo + 1 extra por dia) e o dia 4 ficava sem candidatos.
        List<Lojautilizador> colaboradores = List.of(
                colaborador(1, "A"), colaborador(2, "B"), colaborador(3, "C"));
        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        LocalDate fim = inicio.plusDays(4); // Seg–Sex
        PedidoGeracao pedido = pedido(colaboradores, List.of(turno), Map.of(1, 1),
                2L * 480, inicio, fim); // 16h = 2 turnos de 8h por colaborador

        List<Horario> horarios = engine.gerar(pedido);

        // Todos os dias do período têm de estar cobertos — incluindo os últimos.
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            LocalDate dia = data;
            long cobertura = horarios.stream().filter(h -> dia.equals(h.getDataTurno())).count();
            assertTrue(cobertura >= 1,
                    "O dia " + dia + " ficou sem cobertura — o top-up esgotou a carga antes do fim do período.");
        }
    }

    // ── Matcher de regras de rotação de FDS ──────────────────────────────────

    @Test
    void regraRotacaoComPluralEHifenesEReconhecida() {
        RegraGeracaoResolver resolver = new RegraGeracaoResolver(null, null, validator);
        Turno turno = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));

        var parametros = resolver.resolverParametrosGeracao(
                List.of(
                        regra("Minimo de colaboradores por turno", 1),
                        regra("Rotacao de fins-de-semana (semanas)", 7)),
                List.of(turno));

        assertEquals(7, parametros.janelaRotacaoFinsDeSemanaSemanas(),
                "A regra 'Rotacao de fins-de-semana' (plural, hífenes) deve ser reconhecida — não cair no default 2.");
    }

    @Test
    void regraRotacaoComSingularContinuaReconhecida() {
        RegraGeracaoResolver resolver = new RegraGeracaoResolver(null, null, validator);
        Turno turno = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(16, 0));

        var parametros = resolver.resolverParametrosGeracao(
                List.of(
                        regra("Minimo de colaboradores por turno", 1),
                        regra("Janela de rotacao de fim de semana", 4)),
                List.of(turno));

        assertEquals(4, parametros.janelaRotacaoFinsDeSemanaSemanas());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private RegraAplicada regra(String descricao, Integer valor) {
        return new RegraAplicada(descricao, "operacional", valor);
    }

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

    private Turno turno(int id, String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(horaInicio);
        t.setHoraFim(horaFim);
        return t;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores, List<Turno> turnos,
                                  Map<Integer, Integer> minimosPorTurno, long cargaMinutos,
                                  LocalDate dataInicio, LocalDate dataFim) {
        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), cargaMinutos);
        }
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
