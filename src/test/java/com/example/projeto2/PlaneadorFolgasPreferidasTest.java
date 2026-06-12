package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
import com.example.projeto2.API.Services.geracao.PedidoGeracao;
import com.example.projeto2.API.Services.geracao.PlaneadorFolgasPreferidas;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários (sem Spring) da fase de pré-planeamento de folgas preferidas
 * ({@link PlaneadorFolgasPreferidas}): reserva proativa das folgas que cabem sem
 * comprometer a cobertura, e verificação de que o motor honra de facto as reservas.
 */
class PlaneadorFolgasPreferidasTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final PlaneadorFolgasPreferidas planeador = new PlaneadorFolgasPreferidas(validator);
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // Segunda-feira de uma semana "limpa" no meio do mês seguinte
    private final LocalDate segunda = LocalDate.now()
            .plusMonths(1)
            .withDayOfMonth(10)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

    @Test
    void reservaFolgaEmDiaUtilQuandoSobraCapacidade() {
        // 4 procuráveis, cobertura 1, margem 2 → capacidade livre 1 → folga cabe.
        List<Lojautilizador> equipa = equipaFullTime(4);
        LocalDate quarta = segunda.plusDays(2);

        Map<Integer, Set<LocalDate>> reservadas = planeador.reservar(
                pedido(equipa, Map.of(1, Set.of(quarta))));

        assertEquals(Set.of(quarta), reservadas.getOrDefault(1, Set.of()),
                "Com margem de sobra, a folga preferida de um dia útil deve ser reservada.");
    }

    @Test
    void naoReservaQuandoNaoHaCapacidadeAcimaDaCoberturaMaisMargem() {
        // 3 procuráveis, cobertura 1, margem 2 → capacidade livre 0 → nada reservado.
        List<Lojautilizador> equipa = equipaFullTime(3);
        LocalDate quarta = segunda.plusDays(2);

        Map<Integer, Set<LocalDate>> reservadas = planeador.reservar(
                pedido(equipa, Map.of(1, Set.of(quarta))));

        assertTrue(reservadas.isEmpty(),
                "Sem margem acima da cobertura mínima, nenhuma folga deve ser reservada.");
    }

    @Test
    void naoReservaFolgaAoFimDeSemana() {
        List<Lojautilizador> equipa = equipaFullTime(6);
        LocalDate sabado = segunda.plusDays(5);

        Map<Integer, Set<LocalDate>> reservadas = planeador.reservar(
                pedido(equipa, Map.of(1, Set.of(sabado))));

        assertTrue(reservadas.isEmpty(),
                "Folgas preferidas ao fim de semana ficam soft (lookahead de FDS trata-as).");
    }

    @Test
    void distribuiFolgasComJustabilidadeQuandoACapacidadeEEscassa() {
        // Capacidade de 1 folga/dia. Segunda: só A pede. Terça: A e B pedem.
        // A já gastou a sua folga na segunda → na terça a prioridade vai para B.
        List<Lojautilizador> equipa = equipaFullTime(4);
        LocalDate terca = segunda.plusDays(1);

        Map<Integer, Set<LocalDate>> reservadas = planeador.reservar(
                pedido(equipa, Map.of(
                        1, Set.of(segunda, terca),
                        2, Set.of(terca))));

        assertEquals(Set.of(segunda), reservadas.getOrDefault(1, Set.of()),
                "A recebe a folga de segunda; a de terça cede a quem ainda não tem folga.");
        assertEquals(Set.of(terca), reservadas.getOrDefault(2, Set.of()),
                "B, sem folgas ainda, tem prioridade na terça escassa.");
    }

    @Test
    void motorHonraAFolgaReservadaNoPlanoGerado() {
        // Semana útil (seg-sex), 1 turno/dia, 4 FT com carga folgada: a folga de quarta
        // do colaborador 1 é reservada e o motor nunca o escala nesse dia.
        List<Lojautilizador> equipa = equipaFullTime(4);
        LocalDate quarta = segunda.plusDays(2);

        PedidoGeracao pedido = pedido(equipa, segunda, segunda.plusDays(4),
                Map.of(1, Set.of(quarta)));

        List<Horario> plano = engine.gerar(pedido);

        boolean colaborador1NaQuarta = plano.stream()
                .anyMatch(h -> quarta.equals(h.getDataTurno())
                        && h.getIdLojautilizador().getIdUtilizador().getId() == 1);
        assertFalse(colaborador1NaQuarta,
                "O colaborador 1 não deve ser escalado na sua folga preferida reservada.");

        boolean quartaTemCobertura = plano.stream().anyMatch(h -> quarta.equals(h.getDataTurno()));
        assertTrue(quartaTemCobertura,
                "A reserva da folga não pode deixar a quarta-feira sem cobertura.");
    }

    // ── Fixtures mínimas ────────────────────────────────────────────────────

    private List<Lojautilizador> equipaFullTime(int quantos) {
        return java.util.stream.IntStream.rangeClosed(1, quantos)
                .mapToObj(id -> colaborador(id, "Colaborador " + id, "fulltime"))
                .toList();
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

    /** Turno de 8h (mínimo exigido a colaboradores fulltime). */
    private Turno turnoDia() {
        Turno turno = new Turno();
        turno.setId(1);
        turno.setTipo("manha");
        turno.setHoraInicio(LocalTime.of(9, 0));
        turno.setHoraFim(LocalTime.of(17, 0));
        return turno;
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 Map<Integer, Set<LocalDate>> folgasPreferidas) {
        return pedido(colaboradores, segunda, segunda.plusDays(6), folgasPreferidas);
    }

    private PedidoGeracao pedido(List<Lojautilizador> colaboradores,
                                 LocalDate dataInicio,
                                 LocalDate dataFim,
                                 Map<Integer, Set<LocalDate>> folgasPreferidas) {
        Turno turno = turnoDia();
        java.util.Map<Integer, Long> cargas = new java.util.HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), 9_600L); // 160h — folgada
        }
        return new PedidoGeracao(
                colaboradores,
                List.of(turno),
                dataInicio,
                dataFim,
                Map.of(1, 1),   // minimosPorTurno: turno 1 → mínimo 1
                31,             // maxDiasConsecutivos
                11,             // descansoMinimoHoras
                2,              // descansoSemanalMinimoDias
                2,              // janelaRotacaoFimDeSemana
                false,          // exigirChefiaAoSabado
                Set.of(),
                cargas,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                null,           // sem prazo
                PoliticaOtimizacao.porIndice(0),
                folgasPreferidas,
                Map.of(),
                42L);
    }
}
