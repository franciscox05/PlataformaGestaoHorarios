package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.ConfiguracaoDia;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão para a distribuição temporal do top-up (fase 2 do motor).
 *
 * <p>Bug reportado pelo Francisco: ao gerar um mês, o início ficava com 1 pessoa por
 * turno e só os últimos dias tinham 2. Causa: o orçamento de reforço era recalculado a
 * cada dia como {@code capacidadeRestante / diasRestantes}; quando essa folga diária era
 * menor que a duração de um turno (equipas pequenas, folga modesta), nunca chegava para
 * um turno extra cedo no mês — só quando faltavam poucos dias, empurrando todos os turnos
 * extra para o fim do período.
 *
 * <p>Fix: o orçamento passa a ser acumulado a uma taxa diária constante, pelo que os
 * turnos extra surgem a intervalos regulares desde o início do mês.
 */
class TopUpDistribuicaoUniformeTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Três semanas a começar numa segunda-feira; fins de semana encerrados para isolar
    // os dias úteis (uma equipa sem chefia não cobre fins de semana).
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(20); // 3 semanas (15 dias úteis)

    @Test
    void topUpDistribuiTurnosExtraDesdeOInicioDoMes() {
        List<Lojautilizador> colaboradores = criarColaboradores(4);
        Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        // Folga modesta: 4 × 5 turnos = 20 de capacidade; 15 mínimos (1/dia útil) →
        // sobram exatamente 5 turnos extra para repartir pelos 15 dias úteis.
        long cargaMinutos = 5L * 480; // 5 turnos de 8h por colaborador
        PedidoGeracao pedido = pedido(colaboradores, List.of(manha), Map.of(1, 1), cargaMinutos);

        List<Horario> horarios = engine.gerar(pedido);

        // Turnos por dia útil aberto, em ordem cronológica.
        Map<LocalDate, Long> turnosPorDia = new TreeMap<>();
        for (Horario h : horarios) {
            turnosPorDia.merge(h.getDataTurno(), 1L, Long::sum);
        }

        List<LocalDate> diasAbertos = new ArrayList<>(turnosPorDia.keySet());
        List<Integer> indicesComExtra = new ArrayList<>();
        for (int i = 0; i < diasAbertos.size(); i++) {
            if (turnosPorDia.get(diasAbertos.get(i)) > 1) {
                indicesComExtra.add(i);
            }
        }

        assertTrue(indicesComExtra.size() >= 4,
                "Esperava ~5 dias com turno extra; obtidos " + indicesComExtra.size()
                        + " (turnos/dia=" + turnosPorDia.values() + ").");

        int primeiroExtra = indicesComExtra.get(0);
        assertTrue(primeiroExtra <= 5,
                "O primeiro turno extra devia surgir cedo no mês (índice <= 5), mas surgiu "
                        + "no dia útil #" + primeiroExtra + ". Antes do fix, o reforço só "
                        + "aparecia no fim do mês.");

        // Os extra não devem estar todos concentrados na 2ª metade: pelo menos um na
        // primeira metade dos dias úteis abertos.
        int metade = diasAbertos.size() / 2;
        long extrasPrimeiraMetade = indicesComExtra.stream().filter(i -> i < metade).count();
        assertTrue(extrasPrimeiraMetade >= 1,
                "Os turnos extra deviam estar repartidos pelo mês, mas nenhum caiu na "
                        + "primeira metade (índices=" + indicesComExtra + ").");
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

    /** Fins de semana encerrados; dias úteis abertos sem configuração especial. */
    private Map<LocalDate, ConfiguracaoDia> finsDeSemanaEncerrados() {
        Map<LocalDate, ConfiguracaoDia> configs = new LinkedHashMap<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                configs.put(d, new ConfiguracaoDia(true, List.of(), null, "encerrado"));
            }
        }
        return configs;
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
                31,     // maxDiasConsecutivos (sem limite prático)
                11,     // descansoMinimoHoras
                2,      // descansoSemanalMinimoDias
                2,      // janelaRotacaoFimDeSemana
                false,  // exigirChefiaAoSabado
                Set.of(),
                cargas,
                Map.of(),
                Map.of(),
                finsDeSemanaEncerrados(),
                List.of(),
                null,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),
                Map.of(),
                42L);
    }
}
