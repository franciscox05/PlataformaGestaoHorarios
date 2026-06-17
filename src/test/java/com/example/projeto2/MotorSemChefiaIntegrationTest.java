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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cenário sem chefia disponível. Verifica dois comportamentos críticos:
 * (1) Com exigirChefiaAoSabado=true e sem chefias → falha com mensagem diagnóstico.
 * (2) Com exigirChefiaAoSabado=false → gera normalmente para qualquer tipo de equipa.
 */
class MotorSemChefiaIntegrationTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Segunda a sexta — 5 dias úteis, sem FDS para não colidir com a regra de descanso semanal.
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(4); // segunda a sexta

    // Sábado isolado — para testar a regra de chefia ao sábado de forma controlada.
    private final LocalDate sabadoSeguinte = inicio.plusDays(5);

    private final Turno manha = turno(1, "manha", LocalTime.of(10, 0), LocalTime.of(14, 0));

    @Test
    void semChefiaComExigenciaAoSabadoFalhaComMensagemClara() {
        // Equipa só de fulltime + parttime — nenhum é chefia.
        // Período = apenas sábado, para isolar a verificação de chefia sem conflito de descanso semanal.
        Lojautilizador ft1 = colaborador(1, "FT1", "fulltime");
        Lojautilizador ft2 = colaborador(2, "FT2", "fulltime");
        Lojautilizador pt  = colaborador(3, "PT",  "parttime");

        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 5_760L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(ft1, ft2, pt),
                List.of(manha),
                sabadoSeguinte, sabadoSeguinte, // apenas 1 sábado
                Map.of(1, 1),
                6, 11, 2, 2,
                true,          // exige chefia ao sábado
                Set.of(),      // nenhuma chefia conhecida
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        FalhaGeracaoHorarioException ex = assertThrows(
                FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido),
                "Deve falhar quando se exige chefia ao sábado mas não há nenhuma disponível.");

        assertNotNull(ex.getMessage(), "A exceção deve ter mensagem.");
        assertFalse(ex.getMessage().isBlank(), "A mensagem de falha não deve estar vazia.");
    }

    @Test
    void semChefiaComExigenciaDesativadaGeraCorretamente() {
        Lojautilizador ft1 = colaborador(1, "FT1", "fulltime");
        Lojautilizador ft2 = colaborador(2, "FT2", "fulltime");
        Lojautilizador pt  = colaborador(3, "PT",  "parttime");

        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 5_760L);

        PedidoGeracao pedido = new PedidoGeracao(
                List.of(ft1, ft2, pt),
                List.of(manha),
                inicio, fim,
                Map.of(1, 1),
                6, 11, 2, 2,
                false,         // não exige chefia
                Set.of(),
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        assertFalse(horarios.isEmpty(),
                "Sem exigência de chefia, a geração deve funcionar com equipa só de FT/PT.");
        // Cada dia deve ter pelo menos 1 turno coberto.
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            final LocalDate d = dia;
            long cobertura = horarios.stream().filter(h -> d.equals(h.getDataTurno())).count();
            assertTrue(cobertura >= 1, "Dia " + d + " sem cobertura mesmo sem exigência de chefia.");
        }
    }

    @Test
    void chefiaUnicaAoSabadoEscaladaQuandoExigida() {
        // Período Mon-Sáb. Gerente com carga suficiente para toda a semana.
        Lojautilizador gerente = colaborador(1, "Gerente", "gerente");
        Lojautilizador ft1     = colaborador(2, "FT1",     "fulltime");
        Lojautilizador ft2     = colaborador(3, "FT2",     "fulltime");

        Map<Integer, Long> cargas = Map.of(1, 10_560L, 2, 10_560L, 3, 10_560L);

        // Período = 1 sábado apenas, para que não haja conflito semanal.
        PedidoGeracao pedido = new PedidoGeracao(
                List.of(gerente, ft1, ft2),
                List.of(manha),
                sabadoSeguinte, sabadoSeguinte,
                Map.of(1, 1),
                6, 11, 2, 2,
                true,
                Set.of(1),    // gerente é chefia
                cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(), Map.of(),
                42L, null);

        List<Horario> horarios = engine.gerar(pedido);

        boolean gerenteNoSabado = horarios.stream()
                .anyMatch(h -> sabadoSeguinte.equals(h.getDataTurno())
                        && h.getIdLojautilizador().getIdUtilizador().getId() == 1);
        assertTrue(gerenteNoSabado,
                "O gerente (única chefia) deve estar escalado ao sábado quando exigido.");
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador(); u.setId(id); u.setNome(nome);
        Cargo c = new Cargo(); c.setNome(nome); c.setTipo(tipoCargo);
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id); lig.setIdUtilizador(u); lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime ini, LocalTime fimT) {
        Turno t = new Turno(); t.setId(id); t.setTipo(tipo);
        t.setHoraInicio(ini); t.setHoraFim(fimT);
        return t;
    }
}
