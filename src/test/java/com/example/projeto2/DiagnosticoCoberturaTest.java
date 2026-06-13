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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do reporte do Francisco: com "3 por turno" e 9 colaboradores, o horário tinha
 * dias úteis sistematicamente com menos pessoas (sempre o mesmo dia da semana). A causa era
 * o reforço gastar a folga de capacidade em ordem cronológica, amontoando a cobertura em
 * certos dias e deixando outros no mínimo.
 *
 * <p>Com o preenchimento "levanta o piso" (todos os dias sobem a 2/turno antes de qualquer
 * um ir a 3), a cobertura dos dias ÚTEIS passa a ser uniforme — mesmo quando a capacidade da
 * equipa não chega para o alvo em todos os dias.
 */
class DiagnosticoCoberturaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    private final LocalDate inicio = LocalDate.of(2026, 8, 1);
    private final LocalDate fim = LocalDate.of(2026, 8, 31);

    @Test
    void coberturaDosDiasUteisEhUniformeComAlvoAcimaDaCapacidade() {
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
                Map.of(1, 1, 2, 1, 3, 1), // mínimo 1 por turno → 3/dia
                5, 11, 2, 2, true, Set.of(1, 2), cargas,
                Map.of(), Map.of(), Map.of(), List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS, Map.of(), Map.of(), 42L, 3); // alvo=3

        List<Horario> horarios = engine.gerar(pedido);

        Map<LocalDate, Long> porDia = new TreeMap<>();
        for (Horario h : horarios) porDia.merge(h.getDataTurno(), 1L, Long::sum);

        // Cobertura por dia ÚTIL (segunda a sexta). Os fins de semana têm rotação própria
        // (menos pessoas) e são avaliados à parte.
        List<Long> diasUteis = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                diasUteis.add(porDia.getOrDefault(d, 0L));
            }
        }

        long maxUtil = diasUteis.stream().mapToLong(Long::longValue).max().orElse(0);
        long minUtil = diasUteis.stream().mapToLong(Long::longValue).min().orElse(0);

        // Nenhum dia útil fica no mínimo (3) enquanto outros disparam: amplitude pequena.
        assertTrue(maxUtil - minUtil <= 2,
                "A cobertura dos dias úteis devia ser uniforme (amplitude <= 2), mas foi de "
                        + minUtil + " a " + maxUtil + " (" + diasUteis + ").");
        // E o reforço ocorreu: todos os dias úteis estão acima do mínimo de 3.
        assertTrue(minUtil >= 4,
                "Com capacidade de sobra face ao mínimo, todos os dias úteis deviam subir "
                        + "acima de 3; o menor foi " + minUtil + ".");
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
