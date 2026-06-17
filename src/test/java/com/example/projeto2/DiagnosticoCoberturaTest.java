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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Semântica do alvo de pessoas por turno (decisão do Francisco, 2026-06): "N por turno" é uma
 * GARANTIA RÍGIDA, não um melhor-esforço. Ao escolher um número, a geração assegura esse número
 * em todos os dias abertos — ou falha com um diagnóstico do que ajustar se a capacidade não
 * chegar — em vez de degradar silenciosamente para o mínimo nalguns dias.
 *
 * <p>O alvo é elevado a mínimo rígido na cobertura obrigatória (FASE 1), pelo que o pré-flight
 * de capacidade ({@code validarCapacidadeGlobal}) deteta logo um alvo inalcançável.
 */
class DiagnosticoCoberturaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    private final LocalDate inicio = LocalDate.of(2026, 8, 1);
    private final LocalDate fim = LocalDate.of(2026, 8, 31);

    @Test
    void alvoAcimaDaCapacidadeFalhaComDiagnosticoEmVezDeDegradar() {
        // 9 colaboradores, mês inteiro. Para 3/turno (3 tipos → 9 pessoas/dia × ~9h) a equipa
        // não tem carga contratual suficiente: antes degradava para cobertura parcial; agora o
        // alvo é rígido, logo a geração tem de falhar com a causa real (capacidade).
        List<Lojautilizador> colaboradores = new ArrayList<>();
        colaboradores.add(colaborador(1, "Gerente", "gerente"));
        colaboradores.add(colaborador(2, "Subgerente", "subgerente"));
        for (int i = 3; i <= 7; i++) colaboradores.add(colaborador(i, "FT" + i, "fulltime"));
        colaboradores.add(colaborador(8, "PT8", "parttime"));
        colaboradores.add(colaborador(9, "Reforco9", "reforco_parttime"));

        Turno manha = turno(1, "manha", LocalTime.of(8, 0), LocalTime.of(17, 0));
        Turno inter = turno(2, "intermedio", LocalTime.of(10, 0), LocalTime.of(19, 0));
        Turno tarde = turno(3, "tarde", LocalTime.of(12, 0), LocalTime.of(21, 0));

        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            long horas = switch (lig.getIdCargo().getTipo()) {
                case "parttime" -> 96;
                case "reforco_parttime" -> 64;
                default -> 176;
            };
            cargas.put(lig.getIdUtilizador().getId(), horas * 60L);
        }

        PedidoGeracao pedido = new PedidoGeracao(
                colaboradores, List.of(manha, inter, tarde), inicio, fim,
                Map.of(1, 1, 2, 1, 3, 1), // mínimo das regras = 1 por turno
                5, 11, 2, 2, true, Set.of(1, 2), cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS, Map.of(), Map.of(), 42L, 3); // alvo=3 (acima da capacidade)

        FalhaGeracaoHorarioException falha = assertThrows(FalhaGeracaoHorarioException.class,
                () -> engine.gerar(pedido),
                "Um alvo de 3/turno acima da capacidade da equipa deve falhar, não degradar.");
        assertTrue(falha.getMessage().toLowerCase().contains("capacidade")
                        || falha.getMessage().toLowerCase().contains("carga"),
                "A falha deve explicar que o problema é de capacidade, e não de um dia específico: "
                        + falha.getMessage());
    }

    @Test
    void alvoAlcancavelGaranteExatamenteOAlvoEmTodosOsDiasUteis() {
        // 8 colaboradores full-time, semana útil (seg–sex), 2 turnos. Capacidade folgada para
        // garantir 2/turno todos os dias úteis → cada dia útil tem exatamente 2+2 = 4.
        LocalDate seg = LocalDate.now().plusMonths(1)
                .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        LocalDate sex = seg.plusDays(4);

        List<Lojautilizador> colaboradores = new ArrayList<>();
        for (int i = 1; i <= 8; i++) colaboradores.add(colaborador(i, "FT" + i, "fulltime"));

        Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));
        Turno tarde = turno(2, "tarde", LocalTime.of(13, 0), LocalTime.of(21, 0));

        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) cargas.put(lig.getIdUtilizador().getId(), 9_600L);

        PedidoGeracao pedido = new PedidoGeracao(
                colaboradores, List.of(manha, tarde), seg, sex,
                Map.of(1, 1, 2, 1), // mínimo das regras = 1; alvo eleva-o a 2
                5, 11, 2, 2, false, Set.of(), cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.EQUILIBRIO, Map.of(), Map.of(), 42L, 2); // alvo=2 (alcançável)

        List<Horario> horarios = engine.gerar(pedido);

        Map<LocalDate, Long> porDia = new TreeMap<>();
        for (Horario h : horarios) porDia.merge(h.getDataTurno(), 1L, Long::sum);

        assertEquals(5, porDia.size(), "Os 5 dias úteis estão cobertos.");
        for (Map.Entry<LocalDate, Long> entry : porDia.entrySet()) {
            assertEquals(4L, entry.getValue(),
                    "Cada dia útil deve ter exatamente 2/turno (2 manhã + 2 tarde) em " + entry.getKey());
        }
    }

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador(); u.setId(id); u.setNome(nome);
        Cargo c = new Cargo(); c.setNome(nome); c.setTipo(tipoCargo);
        Lojautilizador lig = new Lojautilizador(); lig.setId(id); lig.setIdUtilizador(u); lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime hi, LocalTime hf) {
        Turno t = new Turno(); t.setId(id); t.setTipo(tipo); t.setHoraInicio(hi); t.setHoraFim(hf);
        return t;
    }
}
