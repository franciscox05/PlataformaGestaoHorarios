package com.example.projeto2;

import com.example.projeto2.WEB.WebSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = Projeto2WebApplication.class,
        properties = "spring.main.web-application-type=servlet"
)
@ActiveProfiles("test")
@Transactional
@Rollback
class WebAcessoPerfisIntegrationTest extends FluxosCriticosTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void prepararMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void rotasProtegidasRedirecionamQuandoNaoExisteSessao() throws Exception {
        mockMvc.perform(get("/web/painel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/login"));

        mockMvc.perform(get("/web/horarios"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/login"));
    }

    @Test
    void colaboradorSemPermissaoNaoAbreModulosDeGestao() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("web-acessos");
        var colaborador = fixture.colaboradores().get(0);

        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(WebSession.UTILIZADOR_ID, colaborador.getId());
        sessao.setAttribute(WebSession.UTILIZADOR_NOME, colaborador.getNome());
        sessao.setAttribute(WebSession.UTILIZADOR_EMAIL, colaborador.getEmail());

        mockMvc.perform(get("/web/gestao-loja").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel?acessoNegado=true"));

        mockMvc.perform(get("/web/relatorios").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel?acessoNegado=true"));
    }

    @Test
    void perfilDeGestaoComSessaoValidaAbreModulosPermitidos() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("web-acessos-gestao");
        var gerente = fixture.gerente();

        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(WebSession.UTILIZADOR_ID, gerente.getId());
        sessao.setAttribute(WebSession.UTILIZADOR_NOME, gerente.getNome());
        sessao.setAttribute(WebSession.UTILIZADOR_EMAIL, gerente.getEmail());

        mockMvc.perform(get("/web/gestao-loja").session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(get("/web/relatorios").session(sessao))
                .andExpect(status().isOk());

        mockMvc.perform(get("/web/complementares").session(sessao))
                .andExpect(status().isOk());
    }
}
