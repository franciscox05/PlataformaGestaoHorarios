package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica que o turno impopular (ninguém o prefere) é distribuído de forma equitativa
 * entre colaboradores, em vez de se concentrar sempre na mesma pessoa pelo efeito do
 * bónus de consistência (componente 7 do avaliador).
 *
 * <p>Sem o componente 11 (equidade por tipo de turno), o greedy tendia a prender quem
 * calhou receber o primeiro turno impopular, tornando-o o "colaborador do turno mau"
 * permanente — mesmo sem qualquer preferência para tal.
 *
 * <p>Turnos: manhã 09:00-17:00 (popular) e tarde 13:00-21:00 (impopular).
 * Após tarde (21:00): próximo disponível = 08:00 (11h). Manhã começa às 09:00 → 12h gap ✓.
 * Fins de semana fechados para eliminar a restrição de chefia obrigatória.
 */
class EquidadeTurnoImpostoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // 4 semanas a começar na segunda-feira do mês seguinte
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1)
            .with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(27); // 4 semanas

    /** Turno popular — todos preferem. */
    private final Turno manha = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

    /**
     * Turno impopular — ninguém prefere.
     * 13:00-21:00: descanso até 09:00 do dia seguinte = 12h > 11h ✓.
     */
    private final Turno tarde = turno(2, "tarde", LocalTime.of(13, 0), LocalTime.of(21, 0));

    @Test
    void turnoImpopularDistribuiPorTodosQuandoNenhumPrefere() {
        // 6 colaboradores full-time, NENHUM prefere tarde — todos preferem manhã.
        List<Lojautilizador> equipa = List.of(
                colaborador(1, "Ana"),
                colaborador(2, "Bruno"),
                colaborador(3, "Carlos"),
                colaborador(4, "Diana"),
                colaborador(5, "Eva"),
                colaborador(6, "Filipe")
        );

        Map<Integer, Long> cargas = Map.of(
                1, 9_600L, 2, 9_600L, 3, 9_600L,
                4, 9_600L, 5, 9_600L, 6, 9_600L
        );

        // Todos preferem manhã — tarde é o turno "que sobra"
        Map<Integer, List<Preferencia>> prefsManha = equipa.stream().collect(Collectors.toMap(
                lig -> lig.getIdUtilizador().getId(),
                lig -> List.of(prefTurno("manha", inicio, fim))
        ));

        // Fechar fins de semana: elimina a restrição de chefia obrigatória ao FDS
        // (que exigiria um gerente na equipa, distorcendo a distribuição de tardes).
        Map<LocalDate, ConfiguracaoDia> fechamentos = new HashMap<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                fechamentos.put(d, new ConfiguracaoDia(true, List.of(), null, "Encerrado"));
            }
        }

        PedidoGeracao pedido = new PedidoGeracao(
                equipa,
                List.of(manha, tarde),
                inicio, fim,
                Map.of(1, 1, 2, 1),  // mínimo 1 manhã + 1 tarde por dia útil
                6, 11, 1, 3,         // maxConsecutivos, descanso, descSemanal, janelaFDS
                false,               // sem chefia obrigatória ao sábado
                Set.of(),
                cargas,
                Map.of(),            // bloqueiosPorColaborador
                prefsManha,          // preferenciasTurnos
                fechamentos,         // configuracoesPorData — fins de semana fechados
                List.of(),
                null,
                PoliticaOtimizacao.EQUILIBRIO,
                Map.of(),
                Map.of(),
                42L
        );

        List<Horario> plano = engine.gerar(pedido);

        // Conta tardes por colaborador
        Map<Integer, Long> tardesPorColab = plano.stream()
                .filter(h -> h.getIdTurno() != null
                        && "tarde".equalsIgnoreCase(h.getIdTurno().getTipo()))
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.counting()
                ));

        long minTardes = tardesPorColab.values().stream().mapToLong(Long::longValue).min().orElse(0);
        long maxTardes = tardesPorColab.values().stream().mapToLong(Long::longValue).max().orElse(0);

        // Com 20 dias úteis e 6 pessoas, cada uma devia ter ~3,3 tardes (minimo).
        // A diferença ≤ 2 garante que nenhum fica com >2× as tardes do colega com menos.
        // Sem o componente 11, a concentração típica era ≥ 5 (uma pessoa com ~8 tardes).
        assertTrue(maxTardes - minTardes <= 2,
                "Tardes muito concentradas — turno impopular não foi distribuído equitativamente."
                        + " max=" + maxTardes + " min=" + minTardes
                        + " distribuição: " + tardesPorColab);
    }

    @Test
    void turnoImpopularNaoPenalizaQuemPrefere() {
        // Ana prefere tarde → o componente 11 NÃO a deve penalizar.
        // Com 4 pessoas e 20 dias úteis, Ana devia ter >= tardes que a média dos outros.
        List<Lojautilizador> equipa = List.of(
                colaborador(1, "Ana"),
                colaborador(2, "Bruno"),
                colaborador(3, "Carlos"),
                colaborador(4, "Diana")
        );

        Map<LocalDate, ConfiguracaoDia> fechamentos = new HashMap<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                fechamentos.put(d, new ConfiguracaoDia(true, List.of(), null, "Encerrado"));
            }
        }

        PedidoGeracao pedido = new PedidoGeracao(
                equipa,
                List.of(manha, tarde),
                inicio, fim,
                Map.of(1, 1, 2, 1),
                6, 11, 1, 3,
                false, Set.of(),
                Map.of(1, 9_600L, 2, 9_600L, 3, 9_600L, 4, 9_600L),
                Map.of(),            // bloqueiosPorColaborador
                Map.of(              // preferenciasTurnos
                        1, List.of(prefTurno("tarde", inicio, fim)),
                        2, List.of(prefTurno("manha", inicio, fim)),
                        3, List.of(prefTurno("manha", inicio, fim)),
                        4, List.of(prefTurno("manha", inicio, fim))
                ),
                fechamentos,         // configuracoesPorData — fins de semana fechados
                List.of(), null,
                PoliticaOtimizacao.PREFERENCIAS,
                Map.of(), Map.of(), 42L
        );

        List<Horario> plano = engine.gerar(pedido);

        long tardesAna   = contarTipo(plano, 1, "tarde");
        long tardesBruno = contarTipo(plano, 2, "tarde");
        long tardesCarlos = contarTipo(plano, 3, "tarde");
        long tardesDiana = contarTipo(plano, 4, "tarde");

        double mediaOutros = (tardesBruno + tardesCarlos + tardesDiana) / 3.0;

        // Ana (prefere tarde) deve ter pelo menos tantas tardes como a média dos outros
        assertTrue(tardesAna >= mediaOutros - 1,
                "Ana (prefere tarde) devia ter >= tardes que a média dos outros (preferem manhã)."
                        + " Ana=" + tardesAna + " mediaOutros=" + mediaOutros);
    }

    // ── fixtures ─────────────────────────────────────────────────────────────

    private Lojautilizador colaborador(int id, String nome) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome("Assistente de Vendas FT");
        c.setTipo("fulltime");
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id);
        lig.setIdUtilizador(u);
        lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime hi, LocalTime hf) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(hi);
        t.setHoraFim(hf);
        return t;
    }

    private Preferencia prefTurno(String tipo, LocalDate de, LocalDate ate) {
        Preferencia p = new Preferencia();
        p.setTipo("turnos");
        p.setDescricao("Prefere turno de " + tipo + ".");
        p.setDataInicio(de);
        p.setDataFim(ate);
        return p;
    }

    private long contarTipo(List<Horario> plano, int idColab, String tipo) {
        return plano.stream()
                .filter(h -> {
                    if (h.getIdLojautilizador() == null
                            || h.getIdLojautilizador().getIdUtilizador() == null) return false;
                    Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
                    return idColab == (id != null ? id : -1);
                })
                .filter(h -> h.getIdTurno() != null
                        && tipo.equalsIgnoreCase(h.getIdTurno().getTipo()))
                .count();
    }
}
