package com.example.projeto2;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.ColaboradorCumprimento;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.Estado;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring nem JavaFX) de {@link AnaliseCumprimentoHorario}:
 * verifica o resumo por categoria (folga x/y, turno x/y, colega, carga, ausência,
 * fins de semana livres) a partir das linhas geradas + critérios.
 */
class AnaliseCumprimentoHorarioTest {

    private static final int ANO = 2026;
    private final LocalDate primeiroDiaMes = LocalDate.of(ANO, 8, 1);

    private HorarioLinha linha(int id, String nome, String cargo, LocalDate data, String turno, String periodo) {
        return new HorarioLinha(null, id, null, data, data.getDayOfWeek().toString(),
                turno, periodo, nome, cargo, "pendente");
    }

    private CriteriosGeracao criterios(List<String> detalheColaboradores,
                                       List<String> detalheAusencias,
                                       List<String> detalhePrefTurno,
                                       List<String> detalhePrefColegas,
                                       Map<Integer, Set<LocalDate>> folgasPreferidas) {
        return new CriteriosGeracao(
                11, 5, 1, 7, true,
                List.of(), List.of(), 0, 0,
                detalheColaboradores,
                detalheAusencias,
                List.of(),
                detalhePrefTurno,
                detalhePrefColegas,
                List.of(),
                folgasPreferidas);
    }

    @Test
    void folgaPreferidaResumeHonradasEVioladas() {
        LocalDate seg3 = LocalDate.of(ANO, 8, 3);
        LocalDate seg10 = LocalDate.of(ANO, 8, 10);

        // Ana trabalha no dia 10 (folga violada) mas não no dia 3 (honrada) → 1/2 parcial.
        List<HorarioLinha> linhas = List.of(
                linha(1, "Ana", "Assistente FT", LocalDate.of(ANO, 8, 4), "manha", "08:00 - 16:00"),
                linha(1, "Ana", "Assistente FT", seg10, "manha", "08:00 - 16:00"));

        var c = criterios(List.of("Ana — Assistente FT, 176h/mês"),
                List.of(), List.of(), List.of(), Map.of(1, Set.of(seg3, seg10)));

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento ana = porNome(resultado, "Ana");
        assertNotNull(ana);
        assertNotNull(ana.folga());
        assertEquals(Estado.PARCIAL, ana.folga().estado());
        assertTrue(ana.folga().texto().startsWith("1/2"), "deve resumir 1/2: " + ana.folga().texto());
    }

    @Test
    void cargaHorariaExcedidaEhNaoCumprida() {
        List<HorarioLinha> linhas = new ArrayList<>();
        for (int i = 0; i < 25; i++) { // 25 × 8h = 200h > 176h
            linhas.add(linha(1, "Bruno", "Assistente FT", primeiroDiaMes.plusDays(i), "manha", "08:00 - 16:00"));
        }
        var c = criterios(List.of("Bruno — Assistente FT, 176h/mês"),
                List.of(), List.of(), List.of(), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento bruno = porNome(resultado, "Bruno");
        assertNotNull(bruno);
        assertEquals(200, bruno.horasTrabalhadas());
        assertEquals(176, bruno.horasPrevistas());
        assertEquals(Estado.NAO_CUMPRIDO, bruno.estadoCarga());
    }

    @Test
    void turnoPreferidoCumpridoPorMaioria() {
        List<HorarioLinha> linhas = List.of(
                linha(1, "Carla", "Assistente FT", primeiroDiaMes,            "manha", "08:00 - 16:00"),
                linha(1, "Carla", "Assistente FT", primeiroDiaMes.plusDays(1), "manha", "08:00 - 16:00"),
                linha(1, "Carla", "Assistente FT", primeiroDiaMes.plusDays(2), "manha", "08:00 - 16:00"),
                linha(1, "Carla", "Assistente FT", primeiroDiaMes.plusDays(3), "manha", "08:00 - 16:00"),
                linha(1, "Carla", "Assistente FT", primeiroDiaMes.plusDays(4), "noite", "16:00 - 00:00"));

        var c = criterios(List.of("Carla — Assistente FT, 176h/mês"),
                List.of(), List.of("Carla — Prefiro turnos de manhã"), List.of(), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento carla = porNome(resultado, "Carla");
        assertNotNull(carla);
        assertNotNull(carla.turno());
        assertEquals(Estado.CUMPRIDO, carla.turno().estado());
        assertTrue(carla.turno().texto().contains("4/5"), "deve contar 4/5: " + carla.turno().texto());
    }

    @Test
    void colegaPreferidoContaTurnosJuntos() {
        LocalDate dia = primeiroDiaMes;
        List<HorarioLinha> linhas = List.of(
                linha(1, "David", "Assistente FT", dia, "manha", "08:00 - 16:00"),
                linha(2, "Eva",   "Assistente FT", dia, "manha", "08:00 - 16:00"));

        var c = criterios(
                List.of("David — Assistente FT, 176h/mês", "Eva — Assistente FT, 176h/mês"),
                List.of(), List.of(), List.of("David — Eva"), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento david = porNome(resultado, "David");
        assertNotNull(david);
        assertEquals(1, david.colegas().size());
        assertEquals(Estado.CUMPRIDO, david.colegas().get(0).estado());
        assertTrue(david.colegas().get(0).texto().contains("1 turno"), david.colegas().get(0).texto());
    }

    @Test
    void ausenciaVioladaContaNoResumo() {
        LocalDate diaAusencia = LocalDate.of(ANO, 8, 5);
        List<HorarioLinha> linhas = List.of(
                linha(1, "Filipe", "Assistente FT", diaAusencia, "manha", "08:00 - 16:00"));

        var c = criterios(List.of("Filipe — Assistente FT, 176h/mês"),
                List.of("Filipe — 05/08 (ferias)"), List.of(), List.of(), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        assertEquals(1, resultado.resumo().ausenciasVioladas());
        ColaboradorCumprimento filipe = porNome(resultado, "Filipe");
        assertNotNull(filipe);
        assertNotNull(filipe.ausencias());
        assertEquals(Estado.NAO_CUMPRIDO, filipe.ausencias().estado());
    }

    @Test
    void semFimDeSemanaLivreComJanelaAlargadaNaoEhViolacao() {
        // Trabalha todos os 5 sábados de agosto 2026 (janela=7 sem.) → 5 < 7 → não viola.
        List<HorarioLinha> linhas = new ArrayList<>();
        LocalDate sabado = primeiroDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        while (sabado.getMonthValue() == 8) {
            linhas.add(linha(1, "Gabriel", "Assistente FT", sabado, "manha", "08:00 - 16:00"));
            sabado = sabado.plusWeeks(1);
        }
        // Helper usa janelaRotacaoFimDeSemana=7; 5 FDS < 7 → período não obriga a folga.
        var c = criterios(List.of("Gabriel — Assistente FT, 176h/mês"),
                List.of(), List.of(), List.of(), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento gabriel = porNome(resultado, "Gabriel");
        assertNotNull(gabriel);
        assertNotNull(gabriel.finsDeSemana());
        assertEquals(Estado.INFORMATIVO, gabriel.finsDeSemana().estado());
        // Conta como "ok" no resumo porque a regra de rotação não é violada neste mês.
        assertEquals(1, resultado.resumo().fdsComFolga());
    }

    @Test
    void semFimDeSemanaLivreComJanelaAplicavelEhNaoCumprido() {
        // Trabalha todos os 5 sábados de agosto 2026 com janela=4 → 5 >= 4 → violação.
        List<HorarioLinha> linhas = new ArrayList<>();
        LocalDate sabado = primeiroDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        while (sabado.getMonthValue() == 8) {
            linhas.add(linha(1, "Gabriel", "Assistente FT", sabado, "manha", "08:00 - 16:00"));
            sabado = sabado.plusWeeks(1);
        }
        var c = new com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao(
                11, 5, 1, 4, true,
                List.of(), List.of(), 0, 0,
                List.of("Gabriel — Assistente FT, 176h/mês"),
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());

        var resultado = AnaliseCumprimentoHorario.analisar(linhas, c, ANO);
        ColaboradorCumprimento gabriel = porNome(resultado, "Gabriel");
        assertNotNull(gabriel);
        assertNotNull(gabriel.finsDeSemana());
        assertEquals(Estado.NAO_CUMPRIDO, gabriel.finsDeSemana().estado());
        assertEquals(0, resultado.resumo().fdsComFolga());
    }

    private ColaboradorCumprimento porNome(AnaliseCumprimentoHorario.Resultado r, String nome) {
        return r.colaboradores().stream()
                .filter(c -> c.nome().equals(nome))
                .findFirst().orElse(null);
    }
}
