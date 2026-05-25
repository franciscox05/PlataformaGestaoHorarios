package com.example.projeto2;

import com.example.projeto2.WEB.WebSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = Projeto2WebApplication.class,
        properties = "spring.main.web-application-type=servlet"
)
@ActiveProfiles("test")
@Transactional
@Rollback
class WebFluxosCriticosE2ETest extends FluxosCriticosTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepararMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void loginPainelEHorariosFuncionamEmSequencia() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("web-e2e-horarios");
        var gerente = fixture.lojaFixture().gerente();

        MvcResult login = mockMvc.perform(post("/web/login")
                        .param("email", gerente.getEmail())
                        .param("password", "Gestor123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel"))
                .andReturn();

        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(sessao);
        assertNotNull(sessao.getAttribute(WebSession.UTILIZADOR_ID));

        mockMvc.perform(get("/web/painel").session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(post("/web/horarios/gerar")
                        .session(sessao)
                        .param("ano", String.valueOf(fixture.referencia().getYear()))
                        .param("mes", String.valueOf(fixture.referencia().getMonthValue())))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/web/horarios")
                        .session(sessao)
                        .param("ano", String.valueOf(fixture.referencia().getYear()))
                        .param("mes", String.valueOf(fixture.referencia().getMonthValue())))
                .andExpect(status().isOk());

        assertFalse(geracaoHorariosBLL.listarPropostas(
                gerente.getId(),
                fixture.referencia().getYear(),
                fixture.referencia().getMonthValue()
        ).isEmpty());
    }

    @Test
    void complementaresCobremFolgasPreferenciasEPermutas() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("web-e2e-comp");
        var colaborador = fixture.lojaFixture().colaboradores().get(0);
        var colega = fixture.lojaFixture().colaboradores().get(1);
        var gerente = fixture.lojaFixture().gerente();
        LocalDate diaPermuta = fixture.referencia().plusDays(12);

        var turnoOrigem = criarHorarioPublicadoSemProposta(colaborador, diaPermuta, fixture.turnos().get(0));
        var turnoDestino = criarHorarioPublicadoSemProposta(colega, diaPermuta, fixture.turnos().get(1));

        MvcResult login = mockMvc.perform(post("/web/login")
                        .param("email", colaborador.getEmail())
                        .param("password", "Colaborador123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessaoColaborador = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(sessaoColaborador);

        mockMvc.perform(post("/web/complementares/folgas")
                        .session(sessaoColaborador)
                        .param("dataAusencia", fixture.referencia().plusDays(8).toString())
                        .param("tipo", "folgas")
                        .param("motivo", "Pedido E2E de folga"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/web/complementares/preferencias")
                        .session(sessaoColaborador)
                        .param("tipo", "folgas")
                        .param("dataInicio", fixture.referencia().plusDays(14).toString())
                        .param("dataFim", fixture.referencia().plusDays(15).toString())
                        .param("prioridade", "5")
                        .param("descricao", "Preferencia E2E para validacao"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/web/complementares/permutas")
                        .session(sessaoColaborador)
                        .param("idHorarioOrigem", String.valueOf(turnoOrigem.getId()))
                        .param("idHorarioDestino", String.valueOf(turnoDestino.getId())))
                .andExpect(status().is3xxRedirection());

        assertFalse(dayOffBLL.listarPedidosPorUtilizador(colaborador.getId()).isEmpty());
        assertFalse(preferenciaBLL.listarPreferenciasPorUtilizador(colaborador.getId()).isEmpty());
        assertFalse(permutaBLL.listarPedidosEnviados(colaborador.getId()).isEmpty());

        MvcResult loginGestao = mockMvc.perform(post("/web/login")
                        .param("email", gerente.getEmail())
                        .param("password", "Gestor123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessaoGestao = (MockHttpSession) loginGestao.getRequest().getSession(false);
        assertNotNull(sessaoGestao);

        mockMvc.perform(get("/web/complementares").session(sessaoGestao))
                .andExpect(status().isOk());

        assertFalse(dayOffBLL.listarPedidosPendentesParaAprovacao(gerente.getId()).isEmpty());
        assertFalse(preferenciaBLL.listarPreferenciasPendentesParaAprovacao(gerente.getId()).isEmpty());
        assertFalse(permutaBLL.listarPedidosPendentesParaAprovacao(gerente.getId()).isEmpty());
    }

    @Test
    void permutaAprovadaNaWebTrocaTurnosEntreColaboradores() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("web-e2e-aprovacao-permuta");
        var colaborador = fixture.lojaFixture().colaboradores().get(0);
        var colega = fixture.lojaFixture().colaboradores().get(1);
        var gerente = fixture.lojaFixture().gerente();
        LocalDate diaPermuta = fixture.referencia().plusDays(16);

        var turnoOrigem = criarHorarioPublicadoSemProposta(colaborador, diaPermuta, fixture.turnos().get(0));
        var turnoDestino = criarHorarioPublicadoSemProposta(colega, diaPermuta, fixture.turnos().get(1));

        Integer turnoOrigemInicial = turnoOrigem.getIdTurno().getId();
        Integer turnoDestinoInicial = turnoDestino.getIdTurno().getId();

        MvcResult loginColaborador = mockMvc.perform(post("/web/login")
                        .param("email", colaborador.getEmail())
                        .param("password", "Colaborador123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessaoColaborador = (MockHttpSession) loginColaborador.getRequest().getSession(false);
        assertNotNull(sessaoColaborador);

        mockMvc.perform(post("/web/complementares/permutas")
                        .session(sessaoColaborador)
                        .param("idHorarioOrigem", String.valueOf(turnoOrigem.getId()))
                        .param("idHorarioDestino", String.valueOf(turnoDestino.getId())))
                .andExpect(status().is3xxRedirection());

        var pendentes = permutaBLL.listarPedidosPendentesParaAprovacao(gerente.getId());
        assertEquals(1, pendentes.size());
        Integer idPermuta = pendentes.get(0).getId();

        MvcResult loginGerente = mockMvc.perform(post("/web/login")
                        .param("email", gerente.getEmail())
                        .param("password", "Gestor123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessaoGerente = (MockHttpSession) loginGerente.getRequest().getSession(false);
        assertNotNull(sessaoGerente);

        mockMvc.perform(post("/web/complementares/permutas/{idPermuta}/aprovar", idPermuta)
                        .session(sessaoGerente))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/complementares"));

        flushAndClear();

        var origemRecarregada = horarioRepository.findById(turnoOrigem.getId()).orElseThrow();
        var destinoRecarregado = horarioRepository.findById(turnoDestino.getId()).orElseThrow();

        assertEquals(turnoDestinoInicial, origemRecarregada.getIdTurno().getId());
        assertEquals(turnoOrigemInicial, destinoRecarregado.getIdTurno().getId());
        assertTrue(permutaBLL.listarPedidosPendentesParaAprovacao(gerente.getId()).isEmpty());
    }

    @Test
    void relatorioWebPermiteExportarCsv() throws Exception {
        GeracaoFixture fixture = criarContextoGeracao("web-e2e-csv");
        var gerente = fixture.lojaFixture().gerente();
        var colaborador = fixture.lojaFixture().colaboradores().get(0);
        LocalDate dia = fixture.referencia().plusDays(9);
        criarHorarioPublicadoSemProposta(colaborador, dia, fixture.turnos().get(0));

        MvcResult login = mockMvc.perform(post("/web/login")
                        .param("email", gerente.getEmail())
                        .param("password", "Gestor123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession sessao = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(sessao);

        MvcResult resultado = mockMvc.perform(get("/web/relatorios/exportar.csv")
                        .session(sessao)
                        .param("ano", String.valueOf(fixture.referencia().getYear()))
                        .param("mes", String.valueOf(fixture.referencia().getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn();

        String corpo = resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(corpo.contains("Loja;Mes;Ano;Colaborador;Cargo;Turnos;FolgasAprovadas;Horas"));
        assertTrue(corpo.contains(colaborador.getNome()));
    }
}
