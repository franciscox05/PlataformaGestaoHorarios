package com.example.projeto2;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.WEB.WebSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de segurança IDOR sobre {@code WebPermutasApiController} (rota
 * {@code /api/permutas/submeter}).
 *
 * <p><b>Correção de premissa importante (ver Revisao.md, secção 3 — LER ANTES DE
 * INTEGRAR):</b> o pedido original assumia que o {@code WebGuardInterceptor}
 * intercetaria este ataque e devolveria HTTP 403. Isso não corresponde ao código
 * atual, por dois motivos verificados em {@code WebMvcConfig.java}:
 * <ol>
 *   <li>O interceptor só está registado para {@code addPathPatterns("/web/**")} —
 *       {@code /api/permutas/**} está fora do seu âmbito. A proteção deste endpoint
 *       não passa pelo interceptor.</li>
 *   <li>O endpoint não recebe nenhum {@code idLoja} no corpo do pedido — apenas
 *       {@code idHorarioOrigem}/{@code idHorarioDestino}. O vetor de IDOR real não é
 *       "forjar um idLoja", é forjar um {@code idHorarioDestino} pertencente a um
 *       colaborador de outra loja.</li>
 * </ol>
 * A defesa real está na camada de repositório: {@code HorarioRepository
 * .findTurnosElegiveisParaPermuta} filtra por
 * {@code l.id = (SELECT origem.idLojautilizador.idLoja.id ...)} — um
 * {@code idHorarioDestino} de outra loja nunca aparece na lista de elegíveis, e o
 * controller faz {@code orElseThrow(...)} sobre essa lista, devolvendo
 * <b>HTTP 422</b> (não 403) através do bloco {@code catch (IllegalArgumentException)}.
 * Este teste valida o comportamento real: o ataque é bloqueado, mas com 422, na
 * camada de serviço/repositório — não com 403 no interceptor.
 */
@SpringBootTest(
        classes = Projeto2WebApplication.class,
        properties = "spring.main.web-application-type=servlet"
)
@ActiveProfiles("test")
@Transactional
@Rollback
class PermutaIdorCrossStoreSecurityTest extends FluxosCriticosTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.example.projeto2.API.Repositories.PermutaRepository permutaRepository;

    @BeforeEach
    void prepararMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void submeterPermutaComTurnoDestinoDeOutraLojaEhRejeitadoComUnprocessableEntity() throws Exception {
        String uid = java.util.UUID.randomUUID().toString().substring(0, 8);
        LocalDate dia = LocalDate.now().plusDays(10);

        // ── Loja A: o atacante (sessão autenticada legítima na Loja A) ──
        LojaFixture fixtureA = criarLojaComEquipaCompleta("idor-loja-a-" + uid);
        Utilizador atacante = fixtureA.colaboradores().get(0);

        Turno turnoOrigemA = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        Lojautilizador ligacaoAtacante = lojautilizadorRepository
                .findLigacaoAtivaByIdUtilizador(atacante.getId()).orElseThrow();

        Horario turnoOrigem = new Horario();
        turnoOrigem.setIdLojautilizador(ligacaoAtacante);
        turnoOrigem.setIdTurno(turnoOrigemA);
        turnoOrigem.setDataTurno(dia);
        turnoOrigem.setEstado(EstadoHorario.aprovado);
        turnoOrigem = horarioRepository.save(turnoOrigem);

        // ── Loja B: a vítima — turno que o atacante vai tentar referenciar por ID ──
        Loja lojaB = criarLojaComNome("Loja B Vitima " + uid);
        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente FT");
        Utilizador vitima = criarUtilizadorHashado("Vitima Loja B " + uid, "vitima.lojab." + uid, "Pass123");
        Lojautilizador ligacaoVitima = criarLigacaoAtiva(vitima, lojaB, cargoFullTime);

        Turno turnoDestinoB = salvarTurnoLocal("intermedio", LocalTime.of(14, 0), LocalTime.of(23, 0));
        Horario turnoDestino = new Horario();
        turnoDestino.setIdLojautilizador(ligacaoVitima);
        turnoDestino.setIdTurno(turnoDestinoB);
        turnoDestino.setDataTurno(dia);
        turnoDestino.setEstado(EstadoHorario.aprovado);
        turnoDestino = horarioRepository.save(turnoDestino);
        flushAndClear();

        // ── Sessão do atacante, autenticado e com contexto da Loja A ──
        MockHttpSession sessaoAtacante = new MockHttpSession();
        sessaoAtacante.setAttribute(WebSession.UTILIZADOR_ID, atacante.getId());
        sessaoAtacante.setAttribute(WebSession.UTILIZADOR_NOME, atacante.getNome());
        sessaoAtacante.setAttribute(WebSession.UTILIZADOR_EMAIL, atacante.getEmail());
        sessaoAtacante.setAttribute(WebSession.LOJA_ID, fixtureA.loja().getId());

        // ── Act: o atacante tenta forjar idHorarioDestino para um turno da Loja B ──
        mockMvc.perform(post("/api/permutas/submeter")
                        .session(sessaoAtacante)
                        .param("idHorarioOrigem", String.valueOf(turnoOrigem.getId()))
                        .param("idHorarioDestino", String.valueOf(turnoDestino.getId())))
                // Comportamento REAL e correto do sistema: 422, vindo da validação de
                // negócio (lista de elegíveis já filtrada por loja), não 403 do interceptor.
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").exists());

        // ── Assert: nenhuma Permuta foi criada para este par de turnos ──
        flushAndClear();
        boolean existePermutaCriada = permutaRepository.existsPedidoPendentePorOrigemEDestino(
                turnoOrigem.getId(), turnoDestino.getId());
        org.junit.jupiter.api.Assertions.assertFalse(existePermutaCriada,
                "O pedido de permuta cross-store forjado nao pode ter sido persistido.");
    }

    // ── helpers locais ───────────────────────────────────────────────────────

    private Loja criarLojaComNome(String nome) {
        Loja loja = new Loja();
        loja.setNome(nome);
        loja.setLocalizacao("Ambiente de testes");
        loja.setHoraAbertura(LocalTime.of(9, 0));
        loja.setHoraFecho(LocalTime.of(23, 59));
        return lojaRepository.save(loja);
    }

    private Turno salvarTurnoLocal(String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        return turnoRepository.save(t);
    }
}
