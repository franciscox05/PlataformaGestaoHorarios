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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Efeito Natal" — stress do motor de backtracking com 25 colaboradores (escala
 * de época alta) e múltiplas restrições cruzadas (mínimos por turno, carga
 * contratual, rotação de fins de semana, chefia ao sábado).
 *
 * <p>Teste unitário puro, sem Spring: instancia {@code HorarioGeneratorEngine}
 * diretamente, no mesmo padrão usado por {@code CapacidadeGlobalGeracaoTest}.
 *
 * <p>O motor já tem dois mecanismos de corte independentes (ver Revisao.md,
 * secção 5, e a auditoria anterior): orçamento de nós de pesquisa
 * ({@code LIMITE_NOS_PESQUISA_BASE/ALARGADO/EXCECAO} = 12k/24k/40k) e um
 * {@code prazoLimite} (deadline de parede) verificado dentro da própria
 * recursão. Este teste define explicitamente um {@code prazoLimite} de 8s — para
 * que o próprio motor pare de forma controlada antes do timeout externo de 10s —
 * e usa {@code assertTimeoutPreemptively} como rede de segurança adicional contra
 * qualquer regressão futura que reintroduza um loop sem corte.
 */
class StressNatalGeracaoPerformanceTest {

    private static final int NUM_COLABORADORES = 25;

    private final HorarioValidatorService validator = new HorarioValidatorService();
    private final HorarioGeneratorEngine engine = new HorarioGeneratorEngine(validator);

    @Test
    void geracaoComVinteCincoColaboradoresConcluiOuFalhaGraciosamenteEmMenosDeDezSegundos() {
        LocalDate inicio = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        LocalDate fim = inicio.plusDays(inicio.lengthOfMonth() - 1);

        List<Lojautilizador> colaboradores = criarColaboradoresEpocaAlta(NUM_COLABORADORES);
        List<Turno> turnos = List.of(
                turno(1, "manha", LocalTime.of(9, 0), LocalTime.of(17, 0)),
                turno(2, "intermedio", LocalTime.of(13, 0), LocalTime.of(21, 0)),
                turno(3, "noite", LocalTime.of(15, 0), LocalTime.of(23, 0))
        );

        // Mínimo reforçado de Natal: 4 colaboradores por turno (vs. 1-2 num mês normal).
        Map<Integer, Integer> minimosPorTurno = Map.of(1, 4, 2, 4, 3, 4);

        // Carga contratual: 25 colaboradores full-time a 176h/mês (44.000 min) —
        // suficiente para cobrir os mínimos reforçados sem esgotar capacidade.
        Map<Integer, Long> cargaMaximaPorColaborador = new HashMap<>();
        for (Lojautilizador lig : colaboradores) {
            cargaMaximaPorColaborador.put(lig.getIdUtilizador().getId(), 176L * 60);
        }

        // Deadline interno de 8s — o próprio motor deve respeitar isto e devolver
        // uma solução parcial/melhor-esforço ou lançar FalhaGeracaoHorarioException
        // de forma controlada, nunca ultrapassar isto sem resposta.
        Instant prazoLimite = Instant.now().plusSeconds(8);

        PedidoGeracao pedido = new PedidoGeracao(
                colaboradores,
                turnos,
                inicio,
                fim,
                minimosPorTurno,
                31,                         // maxDiasConsecutivos
                11,                         // descansoMinimoHoras
                2,                          // descansoSemanalMinimoDias
                2,                          // janelaRotacaoFimDeSemana
                true,                       // exigirChefiaAoSabado
                Set.of(colaboradores.get(0).getIdUtilizador().getId()), // chefiasSabadoIds
                cargaMaximaPorColaborador,
                Map.of(),                   // bloqueiosPorColaborador
                Map.of(),                   // preferenciasTurnos
                Map.of(),                   // configuracoesPorData
                List.of(),                  // historicoHorarios
                prazoLimite,
                PoliticaOtimizacao.porIndice(0),
                Map.of(),                   // folgasPreferidasPorColaborador
                Map.of(),                   // paresPreferisPorColaborador
                42L                         // semente — determinístico para o baseline
        );

        // ── Rede de segurança externa: nada pode demorar mais de 10s ──
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            try {
                List<Horario> horarios = engine.gerar(pedido);
                assertFalse(horarios.isEmpty(),
                        "Com 25 colaboradores e mínimos reforçados de Natal, o motor deveria "
                                + "encontrar pelo menos uma escala viável dentro do orçamento.");
            } catch (FalhaGeracaoHorarioException falhaEsperavel) {
                // Falha graciosa e diagnosticada é um resultado ACEITÁVEL para este teste:
                // o que NÃO é aceitável é nunca devolver controlo (loop infinito) ou
                // StackOverflowError. A asserção abaixo prova que a falha é diagnosticada,
                // não um crash silencioso.
                assertTrue(falhaEsperavel.getMessage() != null && !falhaEsperavel.getMessage().isBlank(),
                        "Mesmo em falha, o motor deve devolver um diagnóstico explicável ao gestor.");
            }
        }, "A geracao com 25 colaboradores (escala de epoca de Natal) nao pode ultrapassar "
                + "10 segundos nem entrar em loop infinito / StackOverflowError.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Lojautilizador> criarColaboradoresEpocaAlta(int quantidade) {
        List<Lojautilizador> colaboradores = new ArrayList<>();
        for (int i = 1; i <= quantidade; i++) {
            Utilizador u = new Utilizador();
            u.setId(i);
            u.setNome("Colaborador Natal " + i);

            Cargo c = new Cargo();
            c.setNome("Assistente FT");
            c.setTipo("fulltime");

            Lojautilizador lig = new Lojautilizador();
            lig.setId(i);
            lig.setIdUtilizador(u);
            lig.setIdCargo(c);
            colaboradores.add(lig);
        }
        return colaboradores;
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
