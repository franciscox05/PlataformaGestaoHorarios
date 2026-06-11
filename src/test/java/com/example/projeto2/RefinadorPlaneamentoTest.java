package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Services.geracao.RefinadorPlaneamento;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) da fase de refinamento por pesquisa local
 * ({@link RefinadorPlaneamento}): recuperação de folgas preferidas e
 * equilíbrio de carga, sempre sem violar regras hard.
 */
class RefinadorPlaneamentoTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final RefinadorPlaneamento refinador = new RefinadorPlaneamento(validator);

    // Segunda-feira de uma semana "limpa" no meio do mês seguinte
    private final LocalDate segunda = LocalDate.now()
            .plusMonths(1)
            .withDayOfMonth(10)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @Test
    void recuperaFolgaPreferidaQuandoOutroColaboradorPodeAssumirOTurno() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno turno = turnoDia();
        LocalDate diaPreferido = segunda.plusDays(2); // quarta-feira

        // Ana foi escalada precisamente no seu dia de folga preferida; Bruno está livre.
        List<Horario> plano = new ArrayList<>(List.of(horario(ana, turno, diaPreferido)));

        PedidoGeracao pedido = pedido(
                List.of(ana, bruno),
                List.of(turno),
                Map.of(1, Set.of(diaPreferido)));

        List<Horario> refinado = refinador.refinar(pedido, plano);

        assertEquals(1, refinado.size(), "O refinamento nunca cria nem remove turnos.");
        Horario movido = refinado.getFirst();
        assertEquals(diaPreferido, movido.getDataTurno(), "O dia do turno mantém-se.");
        assertEquals(turno, movido.getIdTurno(), "O turno mantém-se.");
        assertEquals(2, movido.getIdLojautilizador().getIdUtilizador().getId(),
                "O turno devia ter sido reatribuído ao Bruno para honrar a folga preferida da Ana.");
    }

    @Test
    void naoMoveTurnoQuandoOUnicoCandidatoTambemPrefereFolgarNesseDia() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno turno = turnoDia();
        LocalDate diaPreferido = segunda.plusDays(2);

        List<Horario> plano = new ArrayList<>(List.of(horario(ana, turno, diaPreferido)));

        // Ambos preferem folgar à quarta — mover só trocaria a violação de sítio.
        PedidoGeracao pedido = pedido(
                List.of(ana, bruno),
                List.of(turno),
                Map.of(1, Set.of(diaPreferido), 2, Set.of(diaPreferido)));

        List<Horario> refinado = refinador.refinar(pedido, plano);

        assertEquals(1, refinado.getFirst().getIdLojautilizador().getIdUtilizador().getId(),
                "Sem candidato melhor, o turno fica onde estava.");
    }

    @Test
    void equilibraCargaMovendoTurnosDoMaisCarregadoParaOMenosCarregado() {
        Lojautilizador ana = colaborador(1, "Ana");
        Lojautilizador bruno = colaborador(2, "Bruno");
        Turno turno = turnoDia();

        // Ana com 3 turnos seguidos; Bruno sem nenhum.
        List<Horario> plano = new ArrayList<>(List.of(
                horario(ana, turno, segunda),
                horario(ana, turno, segunda.plusDays(1)),
                horario(ana, turno, segunda.plusDays(2))));

        PedidoGeracao pedido = pedido(List.of(ana, bruno), List.of(turno), Map.of());

        List<Horario> refinado = refinador.refinar(pedido, plano);

        long turnosBruno = refinado.stream()
                .filter(h -> h.getIdLojautilizador().getIdUtilizador().getId() == 2)
                .count();
        assertTrue(turnosBruno >= 1,
                "O equilíbrio de carga devia ter movido pelo menos um turno para o Bruno.");
        assertEquals(3, refinado.size(), "O refinamento nunca cria nem remove turnos.");

        // Nenhum dia ficou com dois turnos para o mesmo colaborador
        long diasDistintos = refinado.stream().map(Horario::getDataTurno).distinct().count();
        assertEquals(3, diasDistintos, "Cada dia mantém exatamente um turno.");
    }

    // ── Fixtures mínimas ────────────────────────────────────────────────────

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

    /** Turno de 8h (mínimo exigido a colaboradores fulltime). */
    private Turno turnoDia() {
        Turno turno = new Turno();
        turno.setId(1);
        turno.setTipo("manha");
        turno.setHoraInicio(LocalTime.of(9, 0));
        turno.setHoraFim(LocalTime.of(17, 0));
        return turno;
    }

    private Horario horario(Lojautilizador ligacao, Turno turno, LocalDate data) {
        Horario h = new Horario();
        h.setIdLojautilizador(ligacao);
        h.setIdTurno(turno);
        h.setDataTurno(data);
        return h;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 List<Turno> turnos,
                                 Map<Integer, Set<LocalDate>> folgasPreferidas) {
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
                Map.of(),
                Map.of(),
                List.of(),
                null,   // sem prazo
                PoliticaOtimizacao.porIndice(0),
                folgasPreferidas,
                Map.of(),
                42L);
    }
}
