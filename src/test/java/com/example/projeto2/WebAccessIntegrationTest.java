package com.example.projeto2;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        classes = Projeto2WebApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class WebAccessIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private UtilizadorBLL utilizadorBLL;

    @MockitoBean
    private GeracaoHorariosBLL geracaoHorariosBLL;

    @MockitoBean
    private PreferenciaBLL preferenciaBLL;

    @MockitoBean
    private DayOffBLL dayOffBLL;

    @MockitoBean
    private PermutaBLL permutaBLL;

    @MockitoBean
    private HorarioBLL horarioBLL;

    @Test
    void loginPageSemSessaoDeveResponderOk() throws Exception {
        HttpResponse<String> response = clientSemSessao()
                .send(getRequest("/web/login"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void raizWebSemSessaoDeveResponderOk() throws Exception {
        HttpResponse<String> response = clientSemSessao()
                .send(getRequest("/web"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void horariosSemSessaoDeveRedirecionarParaLogin() throws Exception {
        HttpResponse<String> response = clientSemSessao()
                .send(getRequest("/web/horarios"), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/login"));
    }

    @Test
    void painelSemSessaoDeveRedirecionarParaLogin() throws Exception {
        HttpResponse<String> response = clientSemSessao()
                .send(getRequest("/web/painel"), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/login"));
    }

    @Test
    void complementaresSemSessaoDeveRedirecionarParaLogin() throws Exception {
        HttpResponse<String> response = clientSemSessao()
                .send(getRequest("/web/complementares"), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/login"));
    }

    @Test
    void loginValidoDeveRedirecionarParaPainel() throws Exception {
        Utilizador utilizador = new Utilizador();
        utilizador.setId(10);
        utilizador.setNome("Teste");
        when(utilizadorBLL.efetuarLogin("teste@ipvc.pt", "123456")).thenReturn(utilizador);

        HttpResponse<String> response = clientSemSessao()
                .send(postForm("/web/login", "email=teste%40ipvc.pt&password=123456"), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/painel"));
    }

    @Test
    void loginInvalidoDeveRedirecionarParaLogin() throws Exception {
        when(utilizadorBLL.efetuarLogin("invalido@ipvc.pt", "errada")).thenReturn(null);

        HttpResponse<String> response = clientSemSessao()
                .send(postForm("/web/login", "email=invalido%40ipvc.pt&password=errada"), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/login"));
    }

    @Test
    void complementaresComSessaoDeveResponderOk() throws Exception {
        when(preferenciaBLL.listarPreferenciasPorUtilizador(anyInt())).thenReturn(List.of());
        when(dayOffBLL.listarPedidosPorUtilizador(anyInt())).thenReturn(List.of());
        when(permutaBLL.listarPedidosEnviados(anyInt())).thenReturn(List.of());
        when(horarioBLL.listarMeusTurnosDisponiveisParaPermuta(anyInt())).thenReturn(List.of());

        Utilizador utilizador = new Utilizador();
        utilizador.setId(10);
        utilizador.setNome("Teste");
        when(utilizadorBLL.efetuarLogin("teste@ipvc.pt", "123456")).thenReturn(utilizador);

        HttpClient client = clientComCookies();
        client.send(postForm("/web/login", "email=teste%40ipvc.pt&password=123456"), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = client.send(getRequest("/web/complementares"), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
    }

    @Test
    void horariosComPerfilSemPermissaoGeracaoNaoDeveMostrarBotaoGerar() throws Exception {
        when(preferenciaBLL.listarPreferenciasPorUtilizador(anyInt())).thenReturn(List.of());
        when(dayOffBLL.listarPedidosPorUtilizador(anyInt())).thenReturn(List.of());
        when(permutaBLL.listarPedidosEnviados(anyInt())).thenReturn(List.of());
        when(horarioBLL.listarMeusTurnosDisponiveisParaPermuta(anyInt())).thenReturn(List.of());
        when(geracaoHorariosBLL.obterContexto(anyInt())).thenReturn(new GeracaoHorariosBLL.GeracaoContexto(
                1, "Loja Teste", "Viana", 2026, 5, false, false, false
        ));
        when(geracaoHorariosBLL.obterPlaneamento(anyInt(), anyInt(), anyInt())).thenReturn(null);
        when(geracaoHorariosBLL.listarPropostas(anyInt(), anyInt(), anyInt())).thenReturn(List.of());

        Utilizador utilizador = new Utilizador();
        utilizador.setId(10);
        utilizador.setNome("Teste");
        when(utilizadorBLL.efetuarLogin("teste@ipvc.pt", "123456")).thenReturn(utilizador);

        HttpClient client = clientComCookies();
        client.send(postForm("/web/login", "email=teste%40ipvc.pt&password=123456"), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = client.send(getRequest("/web/horarios"), HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Horários da loja") || response.body().contains("HorÃ¡rios da loja"));
        assertTrue(!response.body().contains("Gerar proposta"));
    }

    @Test
    void logoutDeveRemoverSessaoERedirecionarPaginasProtegidas() throws Exception {
        when(preferenciaBLL.listarPreferenciasPorUtilizador(anyInt())).thenReturn(List.of());
        when(dayOffBLL.listarPedidosPorUtilizador(anyInt())).thenReturn(List.of());
        when(permutaBLL.listarPedidosEnviados(anyInt())).thenReturn(List.of());
        when(horarioBLL.listarMeusTurnosDisponiveisParaPermuta(anyInt())).thenReturn(List.of());

        Utilizador utilizador = new Utilizador();
        utilizador.setId(11);
        utilizador.setNome("Teste2");
        when(utilizadorBLL.efetuarLogin(eq("teste2@ipvc.pt"), eq("123456"))).thenReturn(utilizador);

        HttpClient client = clientComCookies();
        client.send(postForm("/web/login", "email=teste2%40ipvc.pt&password=123456"), HttpResponse.BodyHandlers.ofString());
        client.send(postForm("/web/logout", ""), HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = client.send(getRequest("/web/complementares"), HttpResponse.BodyHandlers.ofString());

        assertEquals(302, response.statusCode());
        assertTrue(response.headers().firstValue("location").orElse("").contains("/web/login"));
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpClient clientSemSessao() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private HttpClient clientComCookies() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private HttpRequest getRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl(path)))
                .GET()
                .build();
    }

    private HttpRequest postForm(String path, String body) {
        return HttpRequest.newBuilder(URI.create(baseUrl(path)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}
