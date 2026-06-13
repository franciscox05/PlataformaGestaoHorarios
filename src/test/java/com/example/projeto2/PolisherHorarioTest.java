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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) da fase de polimento por pesquisa local
 * ({@link PolisherHorario}): trocas entre colaboradores que reduzem o custo global
 * (equilíbrio de carga e preferências de turno) sem nunca violar regras hard nem
 * alterar a cobertura.
 */
class PolisherHorarioTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final PolisherHorario polisher = new PolisherHorario(validator);

    // Segunda-feira de uma semana "limpa" no meio do mês seguinte
    private final LocalDate segunda = LocalDate.now()
            .plusMonths(1)
            .withDayOfMonth(10)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @Test
    void trocaPreservaSempreACoberturaPorDiaETurno() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno manha = turno(1, "manha", 9, 17);
        Turno tarde = turno(2, "tarde", 13, 21);

        // Dois dias, dois turnos por dia — Ana e Bruno cobrem tudo.
        List<Horario> plano = new ArrayList<>(List.of(
                horario(ana, manha, segunda),
                horario(bruno, tarde, segunda),
                horario(ana, manha, segunda.plusDays(1)),
                horario(bruno, tarde, segunda.plusDays(1))));

        List<String> coberturaAntes = cobertura(plano);

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(manha, tarde),
                Map.of(), Map.of());
        polisher.polir(pedido, plano);

        assertEquals(4, plano.size(), "O polimento nunca cria nem remove turnos.");
        assertEquals(coberturaAntes, cobertura(plano),
                "Cada par (dia, turno) continua coberto pelo mesmo número de pessoas.");
    }

    @Test
    void trocaHonraPreferenciaDeTurnoQuandoMelhoraOCusto() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno manha = turno(1, "manha", 9, 17);
        Turno tarde = turno(2, "tarde", 13, 21);
        LocalDate dia = segunda;

        // Ana ficou com a tarde e o Bruno com a manhã — exatamente o contrário das
        // suas preferências aprovadas. Uma troca no mesmo dia honra ambas sem mexer
        // na cobertura nem na carga (turnos de igual duração).
        List<Horario> plano = new ArrayList<>(List.of(
                horario(ana, tarde, dia),
                horario(bruno, manha, dia)));

        Map<Integer, List<Preferencia>> prefs = Map.of(
                1, List.of(preferencia("turno manha", dia)),
                2, List.of(preferencia("turno tarde", dia)));

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(manha, tarde),
                Map.of(), prefs);
        polisher.polir(pedido, plano);

        Horario turnoManha = plano.stream()
                .filter(h -> Objects.equals(h.getIdTurno().getId(), 1)).findFirst().orElseThrow();
        Horario turnoTarde = plano.stream()
                .filter(h -> Objects.equals(h.getIdTurno().getId(), 2)).findFirst().orElseThrow();

        assertEquals(1, turnoManha.getIdLojautilizador().getIdUtilizador().getId(),
                "A manhã devia ter passado para a Ana (preferência dela).");
        assertEquals(2, turnoTarde.getIdLojautilizador().getIdUtilizador().getId(),
                "A tarde devia ter passado para o Bruno (preferência dele).");
    }

    @Test
    void naoTrocaQuandoNaoHaMelhoriaPossivel() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno manha = turno(1, "manha", 9, 17);
        Turno tarde = turno(2, "tarde", 13, 21);
        LocalDate dia = segunda;

        // Preferências já honradas: qualquer troca pioraria o custo.
        List<Horario> plano = new ArrayList<>(List.of(
                horario(ana, manha, dia),
                horario(bruno, tarde, dia)));

        Map<Integer, List<Preferencia>> prefs = Map.of(
                1, List.of(preferencia("turno manha", dia)),
                2, List.of(preferencia("turno tarde", dia)));

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(manha, tarde),
                Map.of(), prefs);
        List<Horario> resultado = polisher.polir(pedido, plano);

        assertSame(plano, resultado, "Devolve a mesma lista.");
        assertEquals(1, plano.get(0).getIdLojautilizador().getIdUtilizador().getId(),
                "Ana mantém a manhã.");
        assertEquals(2, plano.get(1).getIdLojautilizador().getIdUtilizador().getId(),
                "Bruno mantém a tarde.");
    }

    @Test
    void respeitaFolgaPreferidaTrocandoParaDiaSemPreferencia() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno manha = turno(1, "manha", 9, 17);
        LocalDate diaA = segunda;            // Ana prefere folgar aqui
        LocalDate diaB = segunda.plusDays(1);

        // Ana trabalha no seu dia de folga preferida (diaA); Bruno trabalha noutro dia.
        // Trocar os donos honra a folga da Ana sem alterar a cobertura.
        List<Horario> plano = new ArrayList<>(List.of(
                horario(ana, manha, diaA),
                horario(bruno, manha, diaB)));

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(manha),
                Map.of(1, Set.of(diaA)), Map.of());
        polisher.polir(pedido, plano);

        boolean anaTrabalhaNoDiaDeFolga = plano.stream().anyMatch(h ->
                Objects.equals(h.getIdLojautilizador().getIdUtilizador().getId(), 1)
                        && diaA.equals(h.getDataTurno()));
        assertTrue(!anaTrabalhaNoDiaDeFolga,
                "A folga preferida da Ana devia ter sido honrada pela troca.");
        assertEquals(2, plano.size(), "O polimento nunca cria nem remove turnos.");
    }

    // ── Fixtures mínimas ────────────────────────────────────────────────────

    /** Assinatura legível da cobertura: lista ordenada de "dia|turno" (uma entrada por turno). */
    private List<String> cobertura(List<Horario> plano) {
        return plano.stream()
                .map(h -> h.getDataTurno() + "|" + h.getIdTurno().getId())
                .sorted()
                .toList();
    }

    private Lojautilizador colaborador(int id, String nome) {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(id);
        utilizador.setNome(nome);

        Cargo cargo = new Cargo();
        cargo.setNome("Assistente de Vendas FT");
        cargo.setTipo("fulltime");

        Lojautilizador ligacao = new Lojautilizador();
        ligacao.setId(id);
        ligacao.setIdUtilizador(utilizador);
        ligacao.setIdCargo(cargo);
        return ligacao;
    }

    private Turno turno(int id, String tipo, int horaInicio, int horaFim) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setTipo(tipo);
        turno.setHoraInicio(LocalTime.of(horaInicio, 0));
        turno.setHoraFim(LocalTime.of(horaFim, 0));
        return turno;
    }

    private Horario horario(Lojautilizador ligacao, Turno turno, LocalDate data) {
        Horario h = new Horario();
        h.setIdLojautilizador(ligacao);
        h.setIdTurno(turno);
        h.setDataTurno(data);
        return h;
    }

    private Preferencia preferencia(String descricao, LocalDate dia) {
        Preferencia p = new Preferencia();
        p.setDescricao(descricao);
        p.setDataInicio(dia);
        p.setDataFim(dia);
        return p;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 List<Turno> turnos,
                                 Map<Integer, Set<LocalDate>> folgasPreferidas,
                                 Map<Integer, List<Preferencia>> preferenciasTurnos) {
        return new PedidoGeracao(
                colaboradores,
                turnos,
                segunda,
                segunda.plusDays(6),
                Map.of(),
                6,      // maxDiasConsecutivos
                11,     // descansoMinimoHoras
                1,      // descansoSemanalMinimoDias
                1,      // janelaRotacaoFimDeSemana
                false,  // exigirChefiaAoSabado
                Set.of(),
                Map.of(1, 9_600L, 2, 9_600L), // carga máxima folgada (160h)
                Map.of(),
                preferenciasTurnos,
                Map.of(),
                List.of(),
                null,   // sem prazo
                PoliticaOtimizacao.PREFERENCIAS,
                folgasPreferidas,
                Map.of(),
                42L);
    }
}
