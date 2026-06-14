package com.example.projeto2;

import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioGeneratorEngine;
import com.example.projeto2.API.Services.HorarioValidatorService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressão do bug reportado: "Não foi possível garantir presença de gerente/subgerente
 * no sábado 29/08" — o 5.º sábado do mês falhava com 2 chefias e rotação de 7 semanas.
 *
 * <p>Três causas corrigidas em conjunto:
 * <ol>
 *   <li>O pré-check de chefia não relaxava o descanso semanal (a tentativa 4 do motor
 *       relaxa) — desistia de sábados ainda cobríveis.</li>
 *   <li>O planeador de FDS deixava fins de semana sem chefia designada quando a rotação
 *       não permitia — sem designação, o avaliador não protegia ninguém.</li>
 *   <li>O greedy podia esgotar os dias úteis e a carga das chefias antes do sábado —
 *       novo componente de proteção no avaliador.</li>
 * </ol>
 */
class ChefiaSabadoRotacaoLongaTest {

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    // 5 semanas completas (35 dias) a partir de uma segunda — garante 5 fins de semana,
    // como agosto de 2026 (o mês do bug reportado).
    private final LocalDate inicio = LocalDate.now()
            .plusMonths(1).with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
    private final LocalDate fim = inicio.plusDays(34);

    @Test
    void planeadorDesignaChefiaEmTodosOsFinsDeSemanaMesmoComRotacaoLonga() {
        PlaneadorFinsDeSemana planeador = new PlaneadorFinsDeSemana(validator);
        PedidoGeracao pedido = pedidoComDuasChefias(7);

        PlanoFinsDeSemana plano = planeador.planear(pedido);
        assertTrue(plano.ativo(), "O plano de fins de semana deve estar ativo.");

        // Enumerar os sábados do período e verificar que todos têm chefia designada
        List<LocalDate> sabados = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY) sabados.add(d);
        }
        assertTrue(sabados.size() == 5, "O período de teste deve ter 5 sábados; tem " + sabados.size());

        for (LocalDate sabado : sabados) {
            boolean temChefia = plano.chefiaEm(1).contains(sabado) || plano.chefiaEm(2).contains(sabado);
            assertTrue(temChefia,
                    "O sábado " + sabado + " ficou sem chefia designada — o fallback do planeador falhou.");
        }
    }

    @Test
    void motorCobreTodosOsSabadosComChefiaMesmoComRotacaoDe7Semanas() {
        // Cenário do bug: 2 chefias, rotação 7 semanas, 5 sábados, chefia obrigatória.
        PedidoGeracao pedido = pedidoComDuasChefias(7);

        List<Horario> horarios = engine.gerar(pedido);
        assertFalse(horarios.isEmpty(), "A geração deve concluir sem falhar.");

        // Todos os sábados têm de ter pelo menos uma chefia escalada.
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY) continue;
            LocalDate sabado = d;
            boolean temChefia = horarios.stream()
                    .filter(h -> sabado.equals(h.getDataTurno()))
                    .anyMatch(h -> {
                        Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
                        return id == 1 || id == 2;
                    });
            assertTrue(temChefia, "O sábado " + sabado + " ficou sem gerente/subgerente escalado.");
        }
    }

    @Test
    void motorCobreTodosOsSabadosComRotacaoCurtaTambem() {
        // Regressão: o fix não pode partir o caso que já funcionava (rotação 2).
        PedidoGeracao pedido = pedidoComDuasChefias(2);

        List<Horario> horarios = engine.gerar(pedido);
        assertFalse(horarios.isEmpty());

        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY) continue;
            LocalDate sabado = d;
            boolean temChefia = horarios.stream()
                    .filter(h -> sabado.equals(h.getDataTurno()))
                    .anyMatch(h -> {
                        Integer id = h.getIdLojautilizador().getIdUtilizador().getId();
                        return id == 1 || id == 2;
                    });
            assertTrue(temChefia, "O sábado " + sabado + " ficou sem chefia (rotação curta).");
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Equipa de 10: gerente (1), subgerente (2) e 8 fulltime. Chefia obrigatória ao
     * sábado, mínimo 2 colaboradores por dia, turnos de 8h.
     */
    private PedidoGeracao pedidoComDuasChefias(int janelaRotacao) {
        List<Lojautilizador> colaboradores = new ArrayList<>();
        colaboradores.add(colaborador(1, "Gerente", "gerente"));
        colaboradores.add(colaborador(2, "Subgerente", "subgerente"));
        for (int i = 3; i <= 10; i++) {
            colaboradores.add(colaborador(i, "Colab" + i, "fulltime"));
        }

        Turno turno = turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0));

        Map<Integer, Long> cargas = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargas.put(lig.getIdUtilizador().getId(), 176 * 60L);
        }

        return new PedidoGeracao(
                colaboradores,
                List.of(turno),
                inicio,
                fim,
                Map.of(1, 2),
                31,
                11,
                2,
                janelaRotacao,
                true,
                Set.of(1, 2),
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

    private Lojautilizador colaborador(int id, String nome, String tipoCargo) {
        Utilizador u = new Utilizador();
        u.setId(id);
        u.setNome(nome);
        Cargo c = new Cargo();
        c.setNome(nome);
        c.setTipo(tipoCargo);
        Lojautilizador lig = new Lojautilizador();
        lig.setId(id);
        lig.setIdUtilizador(u);
        lig.setIdCargo(c);
        return lig;
    }

    private Turno turno(int id, String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno t = new Turno();
        t.setId(id);
        t.setTipo(tipo);
        t.setHoraInicio(horaInicio);
        t.setHoraFim(horaFim);
        return t;
    }
}
