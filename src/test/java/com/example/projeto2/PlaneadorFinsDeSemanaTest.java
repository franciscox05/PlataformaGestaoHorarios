package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.AvaliadorAtribuicao;
import com.example.projeto2.API.Services.geracao.EstadoColaborador;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PlaneadorFinsDeSemana;
import com.example.projeto2.API.Services.geracao.PlanoFinsDeSemana;
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
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) do lookahead de fins de semana
 * ({@link PlaneadorFinsDeSemana}): designação global respeitando rotação, e
 * verificação de que o {@link AvaliadorAtribuicao} usa essa designação como steering.
 */
class PlaneadorFinsDeSemanaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final PlaneadorFinsDeSemana planeador = new PlaneadorFinsDeSemana(validator);
    private final AvaliadorAtribuicao avaliador = new AvaliadorAtribuicao(validator);

    // Primeiro dia de um mês futuro — garante um período com 4-5 fins de semana.
    private final LocalDate inicio = LocalDate.now().plusMonths(1).withDayOfMonth(1);
    private final LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

    @Test
    void designaUmaChefiaPorFimDeSemanaRespeitandoRotacao() {
        // 2 chefias, janela 2 → devem alternar e cobrir todos os fins de semana.
        List<Lojautilizador> equipa = new ArrayList<>();
        equipa.add(colaborador(1, "Chefe A", "gerente"));
        equipa.add(colaborador(2, "Chefe B", "subgerente"));
        equipa.add(colaborador(3, "Regular C", "fulltime"));
        equipa.add(colaborador(4, "Regular D", "fulltime"));

        PlanoFinsDeSemana plano = planeador.planear(
                pedido(equipa, Set.of(1, 2), 2, true));

        List<LocalDate> fins = finsDeSemana();
        for (LocalDate fds : fins) {
            long chefiasDesignadas = Set.of(1, 2).stream()
                    .filter(id -> plano.chefiaEm(id).contains(fds))
                    .count();
            assertTrue(chefiasDesignadas >= 1,
                    "Cada fim de semana deve ter pelo menos uma chefia designada: " + fds);
        }
        assertTrue(rotacaoRespeitada(plano.chefiaEm(1), 2),
                "A chefia A não pode ser designada para fins de semana a menos de 2 semanas.");
        assertTrue(rotacaoRespeitada(plano.chefiaEm(2), 2),
                "A chefia B não pode ser designada para fins de semana a menos de 2 semanas.");
    }

    @Test
    void reforcoEhDesignadoParaTodosOsFinsDeSemana() {
        List<Lojautilizador> equipa = new ArrayList<>();
        equipa.add(colaborador(1, "Chefe A", "gerente"));
        equipa.add(colaborador(2, "Regular B", "fulltime"));
        equipa.add(colaborador(3, "Reforco C", "reforco_parttime"));

        PlanoFinsDeSemana plano = planeador.planear(
                pedido(equipa, Set.of(1), 2, true));

        assertTrue(plano.designados(3).containsAll(finsDeSemana()),
                "O reforço de fim de semana (isento de rotação) deve ser designado para todos os FDS.");
    }

    @Test
    void colaboradoresRegularesRespeitamOEspacamentoDeRotacao() {
        List<Lojautilizador> equipa = new ArrayList<>();
        equipa.add(colaborador(1, "Chefe A", "gerente"));
        for (int id = 2; id <= 8; id++) {
            equipa.add(colaborador(id, "Regular " + id, "fulltime"));
        }

        PlanoFinsDeSemana plano = planeador.planear(
                pedido(equipa, Set.of(1), 2, true));

        for (int id = 2; id <= 8; id++) {
            assertTrue(rotacaoRespeitada(plano.designados(id), 2),
                    "O regular " + id + " não pode ser designado para FDS a menos de 2 semanas.");
        }
    }

    @Test
    void semChefiasProduzPlanoAtivoSemDesignacaoDeChefia() {
        List<Lojautilizador> equipa = new ArrayList<>();
        equipa.add(colaborador(1, "Regular A", "fulltime"));
        equipa.add(colaborador(2, "Regular B", "fulltime"));

        PlanoFinsDeSemana plano = planeador.planear(
                pedido(equipa, Set.of(), 2, true));

        assertTrue(plano.ativo(), "Com fins de semana no período, o plano deve estar ativo.");
        assertTrue(plano.finsDeSemanaComoChefia().isEmpty(),
                "Sem chefias, não há designação de chefia — mas o planeamento não falha.");
        assertFalse(plano.finsDeSemanaPorColaborador().isEmpty(),
                "Os regulares continuam a ser designados para cobertura dos fins de semana.");
    }

    @Test
    void avaliadorPrefereAChefiaDesignadaDoFimDeSemana() {
        LocalDate sabado = inicio.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        Lojautilizador chefeA = colaborador(1, "Chefe A", "gerente");
        Lojautilizador chefeB = colaborador(2, "Chefe B", "subgerente");

        // Ambos designados a trabalhar este FDS; só A é a chefia designada.
        EstadoColaborador estadoA = estado(chefeA, Set.of(1, 2));
        estadoA.designarFinsDeSemana(Set.of(sabado), Set.of(sabado));
        EstadoColaborador estadoB = estado(chefeB, Set.of(1, 2));
        estadoB.designarFinsDeSemana(Set.of(sabado), Set.of());

        PedidoGeracao pedido = pedido(List.of(chefeA, chefeB), Set.of(1, 2), 2, true);
        Turno turno = turnoDia();
        long minutos = validator.calcularDuracaoEmMinutos(turno);
        AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContexto(List.of());

        double scoreA = avaliador.pontuar(estadoA, turno, minutos, sabado, pedido, contexto);
        double scoreB = avaliador.pontuar(estadoB, turno, minutos, sabado, pedido, contexto);

        assertTrue(scoreA < scoreB,
                "A chefia designada do fim de semana deve ser preferida (menor pontuação) à não-designada.");
    }

    @Test
    void avaliadorPrefereOColaboradorDesignadoParaOFimDeSemana() {
        LocalDate sabado = inicio.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        Lojautilizador regularA = colaborador(1, "Regular A", "fulltime");
        Lojautilizador regularB = colaborador(2, "Regular B", "fulltime");

        EstadoColaborador estadoA = estado(regularA, Set.of());
        estadoA.designarFinsDeSemana(Set.of(sabado), Set.of()); // designado este FDS
        EstadoColaborador estadoB = estado(regularB, Set.of());
        estadoB.designarFinsDeSemana(Set.of(), Set.of());        // plano ativo, não designado

        PedidoGeracao pedido = pedido(List.of(regularA, regularB), Set.of(), 2, false);
        Turno turno = turnoDia();
        long minutos = validator.calcularDuracaoEmMinutos(turno);
        AvaliadorAtribuicao.ContextoAvaliacao contexto = avaliador.novoContexto(List.of());

        double scoreA = avaliador.pontuar(estadoA, turno, minutos, sabado, pedido, contexto);
        double scoreB = avaliador.pontuar(estadoB, turno, minutos, sabado, pedido, contexto);

        assertTrue(scoreA < scoreB,
                "O colaborador designado para o fim de semana deve ser preferido ao não-designado.");
    }

    @Test
    void motorReforcaCoberturaAoFimDeSemanaParaColaboradoresDesignados() {
        // Com o plano de FDS ativo, o top-up ao fim de semana fica desbloqueado (restrito
        // aos designados): algum dia de FDS deve passar a ter mais do que o mínimo (1).
        // O motor exige sempre 1 chefia por fim de semana, por isso a equipa inclui duas
        // chefias (alternam por rotação) além dos regulares.
        HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);
        List<Lojautilizador> equipa = new ArrayList<>();
        equipa.add(colaborador(1, "Chefe A", "gerente"));
        equipa.add(colaborador(2, "Chefe B", "subgerente"));
        for (int id = 3; id <= 6; id++) {
            equipa.add(colaborador(id, "Regular " + id, "fulltime"));
        }
        PedidoGeracao pedido = pedido(equipa, Set.of(1, 2), 2, true);

        List<Horario> plano = engine.gerar(pedido);

        Map<LocalDate, Long> porDiaFimDeSemana = plano.stream()
                .filter(h -> h.getDataTurno() != null && validator.ehFimDeSemana(h.getDataTurno()))
                .collect(Collectors.groupingBy(Horario::getDataTurno, Collectors.counting()));

        long maxNoFimDeSemana = porDiaFimDeSemana.values().stream()
                .mapToLong(Long::longValue).max().orElse(0);
        assertTrue(maxNoFimDeSemana > 1,
                "Com plano de FDS ativo, o top-up deve reforçar algum dia de fim de semana acima do mínimo.");

        // A rotação tem de continuar intacta: nenhum regular trabalha dois fins de semana
        // a menos de 2 semanas (top-up no mesmo FDS — sábado+domingo — não conta).
        Map<Integer, Set<LocalDate>> fdsPorColaborador = plano.stream()
                .filter(h -> h.getDataTurno() != null && validator.ehFimDeSemana(h.getDataTurno()))
                .collect(Collectors.groupingBy(
                        h -> h.getIdLojautilizador().getIdUtilizador().getId(),
                        Collectors.mapping(h -> h.getDataTurno().with(DayOfWeek.SATURDAY),
                                Collectors.toSet())));
        for (Map.Entry<Integer, Set<LocalDate>> entry : fdsPorColaborador.entrySet()) {
            assertTrue(rotacaoRespeitada(entry.getValue(), 2),
                    "O top-up ao FDS não pode violar a rotação do colaborador " + entry.getKey() + ".");
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Sábados distintos no período (mesmo critério do planeador). */
    private List<LocalDate> finsDeSemana() {
        Set<LocalDate> sabados = new TreeSet<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                sabados.add(d.with(DayOfWeek.SATURDAY));
            }
        }
        return new ArrayList<>(sabados);
    }

    /** Verifica que nenhum par de sábados designados está a menos de {@code janela} semanas. */
    private boolean rotacaoRespeitada(Set<LocalDate> sabados, int janela) {
        List<LocalDate> ordenados = new ArrayList<>(new TreeSet<>(sabados));
        for (int i = 1; i < ordenados.size(); i++) {
            long semanas = ordenados.get(i).toEpochDay() / 7 - ordenados.get(i - 1).toEpochDay() / 7;
            if (semanas < janela) {
                return false;
            }
        }
        return true;
    }

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(id);
        utilizador.setNome(nome);

        Cargo cargo = new Cargo();
        cargo.setNome(nome);
        cargo.setTipo(tipoCargo);

        Lojautilizador ligacao = new Lojautilizador();
        ligacao.setId(id);
        ligacao.setIdUtilizador(utilizador);
        ligacao.setIdCargo(cargo);
        return ligacao;
    }

    private EstadoColaborador estado(Lojautilizador ligacao, Set<Integer> chefiasIds) {
        return new EstadoColaborador(ligacao, 9_600L, chefiasIds, validator);
    }

    private Turno turnoDia() {
        Turno turno = new Turno();
        turno.setId(1);
        turno.setTipo("manha");
        turno.setHoraInicio(LocalTime.of(9, 0));
        turno.setHoraFim(LocalTime.of(17, 0));
        return turno;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 Set<Integer> chefiasSabadoIds,
                                 int janelaRotacao,
                                 boolean exigirChefiaAoSabado) {
        Turno turno = turnoDia();
        java.util.Map<Integer, Long> cargas = new java.util.HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), 9_600L);
        }
        return new PedidoGeracao(
                colaboradores,
                List.of(turno),
                inicio,
                fim,
                Map.of(1, 1),
                31,                     // maxDiasConsecutivos
                11,                     // descansoMinimoHoras
                2,                      // descansoSemanalMinimoDias
                janelaRotacao,
                exigirChefiaAoSabado,
                chefiasSabadoIds,
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
