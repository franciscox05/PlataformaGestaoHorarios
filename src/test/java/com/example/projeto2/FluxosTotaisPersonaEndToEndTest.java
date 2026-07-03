package com.example.projeto2;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Enums.EstadoPermuta;
import com.example.projeto2.API.Modules.Cargo;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PainelGerenteService;
import com.example.projeto2.WEB.WebSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Suite de aceitação ponta-a-ponta por persona/cargo, multi-interface (Web +
 * camada BLL partilhada com o Desktop) e multi-loja.
 *
 * <p><b>Mapeamento autónomo do sistema (confirmado por leitura exaustiva do
 * código antes de escrever qualquer teste — ver Revisao.md, secção "Mapeamento
 * Autónomo do Sistema" para o levantamento completo):</b>
 * <ul>
 *   <li><b>Cargos reais</b> (coluna {@code tipo} de {@code Cargo}, enum Postgres
 *       {@code tipo_cargo_enum}): {@code gerente}, {@code subgerente},
 *       {@code supervisor}, {@code fulltime}, {@code parttime},
 *       {@code reforco_parttime}. NÃO existe nenhum cargo "admin" no catálogo —
 *       o nível de privilégio mais alto é {@code gerente}/{@code subgerente}.</li>
 *   <li><b>Conjuntos de permissão</b> ({@code LojautilizadorHelper}):
 *       {@code APROVACAO} = gerente+subgerente+supervisor;
 *       {@code GESTAO} = gerente+subgerente (config. de loja, turnos, regras);
 *       {@code VALIDACAO} = supervisor (validação de propostas de horário).</li>
 *   <li><b>Controllers Web</b> (13 classes em {@code WEB/}): autenticação
 *       ({@code WebLoginController}), painel ({@code WebPainelController}),
 *       horários ({@code WebHorariosController} + API
 *       {@code WebHorariosApiController}), equipa/aprovações
 *       ({@code WebEquipaController}), complementares — folgas/preferências/
 *       permutas do próprio colaborador ({@code WebComplementaresController}),
 *       APIs de permuta ({@code WebPermutasApiController},
 *       {@code WebPermutaFolgaApiController}), módulos de gestão/relatórios
 *       ({@code WebModulosController}), perfil, notificações, exportação PDF.</li>
 *   <li><b>Camada BLL partilhada</b>: confirmado por leitura de
 *       {@code DESKTOP/GeracaoHorariosController},
 *       {@code DESKTOP/GestaoFuncionariosController},
 *       {@code DESKTOP/PainelGerentePedidosController},
 *       {@code DESKTOP/PermutasController}, {@code DESKTOP/PreferenciasController}
 *       — todos injetam exatamente os mesmos serviços {@code @Service} que os
 *       controllers Web ({@code geracaoHorariosBLL}, {@code gestaoLojaBLL},
 *       {@code dayOffBLL}, {@code preferenciaBLL}, {@code permutaBLL}). Os
 *       testes "inter-interface" abaixo simulam uma escrita via Web e validam
 *       via essa mesma camada BLL — exatamente o que o Desktop leria.</li>
 * </ul>
 *
 * <p><b>Regra de execução desta suite:</b> nenhum ficheiro em {@code src/main/}
 * foi alterado para a escrever. Onde um teste expôs um comportamento real
 * (bug, rota desprotegida, inconsistência) em vez de confirmar uma garantia
 * existente, o método fica anotado {@code @Disabled} com explicação — nunca
 * corrigido. Detalhe completo de cada descoberta em {@code Revisao.md}.
 *
 * <p><b>Decisão explícita sobre testes de UI Desktop (ver Revisao.md):</b> o
 * módulo {@code DESKTOP/} não tem nenhuma infraestrutura de teste de UI —
 * confirmei por leitura de {@code pom.xml} que não existe nenhuma dependência
 * TestFX/Monocle, e todos os controllers JavaFX (ex.: {@code GestaoLojaController},
 * {@code PainelGerentePedidosController}, {@code PermutasController}) têm
 * campos {@code @FXML} (TableView, ComboBox, Label, ...) que só existem depois
 * de um carregamento real de FXML — não são POJOs instanciáveis em teste sem
 * iniciar o toolkit JavaFX. Decidi <b>não adicionar TestFX/Monocle</b> ao
 * projeto sem pedido explícito — é uma alteração de dependências com risco
 * real de instabilidade de build a dias da defesa, fora do âmbito de "escrever
 * testes". Em vez disso, o Grupo E abaixo testa a lógica do Desktop pela via
 * que é realmente testável e fiel: invocação direta dos métodos de serviço
 * que cada handler {@code @FXML} chama, citados com ficheiro e linha exatos,
 * para que a rastreabilidade ao ecrã real seja verificável.
 */
@SpringBootTest(
        classes = Projeto2WebApplication.class,
        properties = "spring.main.web-application-type=servlet"
)
@ActiveProfiles("test")
@Transactional
@Rollback
class FluxosTotaisPersonaEndToEndTest extends FluxosCriticosTestSupport {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.example.projeto2.API.Repositories.PermutaRepository permutaRepositoryAutowired;

    @Autowired
    private PainelGerenteService painelGerenteBLL;

    @Autowired
    private com.example.projeto2.API.Services.SessaoService sessaoBLL;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepararMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // =========================================================================
    // GRUPO A — Isolamento de personas/cargo (RBAC) — sequencial
    // =========================================================================

    /**
     * Persona "Assistente" (cargo {@code fulltime}, fora de {@code APROVACAO}):
     * não pode aprovar a folga de um colega, mesmo sendo da mesma loja.
     */
    @Test
    void colaboradorFulltimeNaoTemPermissaoParaAprovarFolgaDeColega() {
        LojaFixture fixture = criarLojaComEquipaCompleta("persona-ft-folga");
        Utilizador colaboradorComum = fixture.colaboradores().get(0);
        Utilizador colega = fixture.colaboradores().get(1);

        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colega, LocalDate.now().plusDays(5), turno);
        flushAndClear();

        DayOff pedido = dayOffBLL.registarPedidoFolga(novoPedidoFolga(colega, dataFolgaMesSeguinte()));
        flushAndClear();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> dayOffBLL.aprovarPedidoFolga(pedido.getIdDayoff(), colaboradorComum.getId(), fixture.loja().getId()),
                "Um colaborador full-time (fora de APROVACAO) nao pode aprovar folgas, "
                        + "mesmo sendo colega de loja do solicitante.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissão"));
    }

    /**
     * Persona "Supervisor" (cargo em {@code VALIDACAO}, mas FORA de
     * {@code GESTAO}): pode aprovar folgas/permutas (está em APROVACAO), mas
     * NÃO pode gerir a configuração da loja (desativar turnos, alterar regras)
     * — isso é exclusivo de gerente/subgerente.
     */
    @Test
    void supervisorTemAprovacaoMasNaoTemGestaoDeLoja() {
        LojaFixture fixture = criarLojaComEquipaCompleta("persona-supervisor");
        Cargo cargoSupervisor = obterOuCriarCargo("supervisor", "Supervisor de Equipa");
        Utilizador supervisor = criarUtilizadorHashado("Supervisor Persona", "supervisor.persona." + novoUuidLocal(), "Pass123");
        criarLigacaoAtiva(supervisor, fixture.loja(), cargoSupervisor);

        Utilizador colaborador = fixture.colaboradores().get(0);
        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colaborador, LocalDate.now().plusDays(5), turno);
        flushAndClear();

        DayOff pedido = dayOffBLL.registarPedidoFolga(novoPedidoFolga(colaborador, dataFolgaMesSeguinte()));
        flushAndClear();

        // Pode aprovar — supervisor está em APROVACAO.
        DayOff aprovado = dayOffBLL.aprovarPedidoFolga(pedido.getIdDayoff(), supervisor.getId(), fixture.loja().getId());
        assertEquals("aprovado", aprovado.getEstado());

        // Mas não pode gerir a loja — supervisor NÃO está em GESTAO (gerente/subgerente).
        Turno turnoQualquer = salvarTurnoLocal("noite", LocalTime.of(15, 0), LocalTime.of(23, 0));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> gestaoLojaBLL.desativarTurno(supervisor.getId(), turnoQualquer.getId()),
                "Supervisor esta em APROVACAO mas NAO em GESTAO — nao pode desativar turnos da loja.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissao"));
    }

    /**
     * Barreira cross-store: um gerente de uma loja não pode aprovar/rejeitar um
     * pedido pendente que pertence a outra loja, mesmo tendo cargo de gestão
     * legítimo — a permissão é avaliada estritamente dentro do contexto da loja
     * ativa na sessão, não pelo cargo isolado.
     */
    @Test
    void gerenteDeLojaDiferenteNaoConsegueAprovarPedidoDeOutraLoja() {
        String uid = novoUuidLocal();
        LojaFixture fixtureA = criarLojaComEquipaCompleta("persona-cross-a-" + uid);
        Loja lojaB = criarLojaSimples("Persona Loja B " + uid);
        Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");
        Utilizador gerenteB = criarUtilizadorHashado("Gerente B Persona " + uid, "gerenteb.persona." + uid, "Pass123");
        criarLigacaoAtiva(gerenteB, lojaB, cargoGerente);

        Utilizador colaboradorA = fixtureA.colaboradores().get(0);
        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colaboradorA, LocalDate.now().plusDays(5), turno);
        flushAndClear();

        DayOff pedidoNaLojaA = dayOffBLL.registarPedidoFolga(
                novoPedidoFolga(colaboradorA, dataFolgaMesSeguinte()));
        flushAndClear();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> dayOffBLL.aprovarPedidoFolga(pedidoNaLojaA.getIdDayoff(), gerenteB.getId(), lojaB.getId()),
                "Gerente da Loja B nao pode gerir um pedido cujo solicitante nao tem "
                        + "vinculo ativo a Loja B.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissão"));
    }

    /**
     * Mesmo teste de barreira, mas para Permutas — confirma que o isolamento
     * cross-store de aprovação é consistente entre os dois fluxos de aprovação
     * (folgas e permutas), não apenas um deles.
     */
    @Test
    void gerenteDeLojaDiferenteNaoConsegueAprovarPermutaDeOutraLoja() {
        Cenario cenario = montarCenarioPermutaSimples("persona-permuta-cross");
        String uid = novoUuidLocal();
        Loja lojaB = criarLojaSimples("Persona Permuta Loja B " + uid);
        Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");
        Utilizador gerenteB = criarUtilizadorHashado("Gerente B Permuta " + uid, "gerenteb.permuta." + uid, "Pass123");
        criarLigacaoAtiva(gerenteB, lojaB, cargoGerente);
        flushAndClear();

        Permuta pendente = permutaRepositoryAutowired.findPedidosEnviadosPorUtilizador(cenario.solicitante().getId())
                .stream().findFirst().orElseThrow();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> permutaBLL.aprovarPedidoPermuta(pendente.getId(), gerenteB.getId(), lojaB.getId()),
                "Gerente da Loja B nao pode aprovar uma permuta entre colaboradores "
                        + "sem vinculo a essa loja.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissao"));
    }

    /**
     * Barreira ao nível do Portal Web (MockMvc, sessão real): um colaborador
     * comum (fora de GESTAO) é redirecionado com {@code acessoNegado=true} ao
     * tentar abrir os módulos de gestão de loja e relatórios — confirma que o
     * {@code WebGuardInterceptor} aplica exatamente a mesma regra GESTAO usada
     * na camada de serviço.
     */
    @Test
    void webBloqueiaColaboradorComumDosModulosDeGestaoERelatorios() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("web-persona-bloqueio");
        Utilizador colaborador = fixture.colaboradores().get(0);

        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(WebSession.UTILIZADOR_ID, colaborador.getId());
        sessao.setAttribute(WebSession.UTILIZADOR_NOME, colaborador.getNome());
        sessao.setAttribute(WebSession.UTILIZADOR_EMAIL, colaborador.getEmail());
        sessao.setAttribute(WebSession.LOJA_ID, fixture.loja().getId());

        mockMvc.perform(get("/web/gestao-loja").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel?acessoNegado=true"));

        mockMvc.perform(get("/web/relatorios").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel?acessoNegado=true"));
    }

    /**
     * Persona "Supervisor" no Portal Web: tem APROVACAO (acede a
     * /web/equipa e /web/complementares), mas o {@code WebGuardInterceptor}
     * só protege explicitamente /web/gestao-loja e /web/relatorios com
     * verificação de cargo — qualquer outra rota /web/** sem exclusão devolve
     * {@code true} em {@code podeAcederAoModulo}. Confirma que um supervisor
     * (sem GESTAO) ainda consegue abrir /web/gestao-loja se o
     * {@code WebModulosController} não repetir a verificação internamente.
     *
     * <p><b>DESCOBERTA (ver Revisao.md):</b> {@code WebModulosController
     * .moduloPlaceholder} (linhas 101-110) só verifica se existe sessão
     * autenticada — NÃO repete a verificação de cargo {@code podeGerirLoja()}
     * feita pelo interceptor. Isto está correto enquanto o
     * {@code WebGuardInterceptor} continuar a proteger esta rota — mas é uma
     * dependência implícita e não documentada: se algum dia este endpoint for
     * chamado fora do âmbito do interceptor (ex.: um teste de unidade direto
     * ao controller, ou uma futura rota alternativa), a verificação de cargo
     * desaparece silenciosamente. Este teste teria de FALHAR se a página
     * fosse acedida sem o interceptor — mas como o MockMvc aqui passa pela
     * cadeia de interceptors real, o bloqueio acontece e o teste confirma o
     * comportamento ATUAL (correto, mas estruturalmente frágil).
     */
    @Test
    void supervisorSemGestaoEhBloqueadoPorqueInterceptorEhAUnicaLinhaDeDefesa() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("web-persona-supervisor");
        Cargo cargoSupervisor = obterOuCriarCargo("supervisor", "Supervisor de Equipa");
        Utilizador supervisor = criarUtilizadorHashado("Supervisor Web Persona", "supervisor.web." + novoUuidLocal(), "Pass123");
        criarLigacaoAtiva(supervisor, fixture.loja(), cargoSupervisor);
        flushAndClear();

        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(WebSession.UTILIZADOR_ID, supervisor.getId());
        sessao.setAttribute(WebSession.UTILIZADOR_NOME, supervisor.getNome());
        sessao.setAttribute(WebSession.UTILIZADOR_EMAIL, supervisor.getEmail());
        sessao.setAttribute(WebSession.LOJA_ID, fixture.loja().getId());

        mockMvc.perform(get("/web/gestao-loja").session(sessao))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/web/painel?acessoNegado=true"));
    }

    // =========================================================================
    // GRUPO B — Fluxos inter-interface (escrita Web → leitura pela mesma BLL
    // que o Desktop consome)
    // =========================================================================

    /**
     * Submete uma preferência através do controller Web real
     * ({@code WebComplementaresController}, via MockMvc) e confirma que
     * {@code PreferenciaService} — a MESMA camada injetada pelo
     * {@code DESKTOP/PreferenciasController} — vê o pedido pendente
     * imediatamente. Não há nenhuma camada de cache/REST intermédia: é a
     * prova de que "submeter na Web, decidir no Desktop" funciona porque
     * ambos partilham o mesmo contexto Spring.
     */
    @Test
    void preferenciaSubmetidaNaWebEhVisivelImediatamenteNaCamadaPartilhadaComODesktop() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("inter-pref");
        Utilizador colaborador = fixture.colaboradores().get(0);

        MockHttpSession sessao = sessaoDoColaborador(colaborador, fixture.loja());

        mockMvc.perform(post("/web/complementares/preferencias")
                        .session(sessao)
                        .param("tipo", "folga_preferida")
                        .param("diaSemana", "SATURDAY")
                        .param("descricao", "Prefiro sabados de folga (teste inter-interface)."))
                .andExpect(status().is3xxRedirection());

        flushAndClear();
        List<Preferencia> pendentes = preferenciaBLL.listarPreferenciasPorUtilizador(colaborador.getId());
        assertEquals(1, pendentes.size(),
                "A preferencia submetida via Web tem de estar visivel via PreferenciaService "
                        + "— a mesma camada que o DESKTOP/PreferenciasController usaria.");
        assertEquals("folga_preferida", pendentes.get(0).getTipo());
    }

    /**
     * Submete um pedido de folga via Web e confirma que fica visível para
     * aprovação através de {@code DayOffService.listarPedidosPendentesParaAprovacao}
     * — o método exato que {@code DESKTOP/PainelGerentePedidosController} usaria
     * para popular o ecrã de aprovações do gerente.
     */
    @Test
    void folgaSubmetidaNaWebApareceNoPainelDeAprovacoesQueODesktopConsultaria() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("inter-folga");
        Utilizador colaborador = fixture.colaboradores().get(0);
        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colaborador, LocalDate.now().plusDays(3), turno);
        flushAndClear();

        MockHttpSession sessao = sessaoDoColaborador(colaborador, fixture.loja());
        LocalDate dataAusencia = dataFolgaMesSeguinte();

        mockMvc.perform(post("/web/complementares/folgas")
                        .session(sessao)
                        .param("dataAusencia", dataAusencia.toString())
                        .param("tipo", "folgas")
                        .param("motivo", "Teste inter-interface Web->Desktop"))
                .andExpect(status().is3xxRedirection());

        flushAndClear();
        List<DayOff> pendentesParaGerente = dayOffBLL.listarPedidosPendentesParaAprovacao(
                fixture.gerente().getId(), fixture.loja().getId());
        assertTrue(pendentesParaGerente.stream()
                        .anyMatch(d -> d.getIdUtilizador().getId().equals(colaborador.getId())
                                && d.getDataAusencia().equals(dataAusencia)),
                "O pedido de folga submetido na Web tem de aparecer no painel de aprovacoes "
                        + "que o DESKTOP/PainelGerentePedidosController consultaria via a mesma BLL.");
    }

    /**
     * Aprovação feita "no Desktop" (chamada direta à BLL, sem MockMvc — simula
     * o controller JavaFX) sobre um pedido criado "na Web" (MockMvc). Fecha o
     * ciclo completo de uma persona: Colaborador submete na Web → Gerente
     * aprova no Desktop → o estado fica consistente em ambas as interfaces.
     */
    @Test
    void cicloCompletoColaboradorSubmeteNaWebGerenteAprovaComoSeFosseNoDesktop() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("inter-ciclo");
        Utilizador colaborador = fixture.colaboradores().get(0);
        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colaborador, LocalDate.now().plusDays(3), turno);
        flushAndClear();

        MockHttpSession sessao = sessaoDoColaborador(colaborador, fixture.loja());
        LocalDate dataAusencia = dataFolgaMesSeguinte();

        mockMvc.perform(post("/web/complementares/folgas")
                        .session(sessao)
                        .param("dataAusencia", dataAusencia.toString())
                        .param("tipo", "folgas"))
                .andExpect(status().is3xxRedirection());
        flushAndClear();

        DayOff pendente = dayOffBLL.listarPedidosPendentesParaAprovacao(fixture.gerente().getId(), fixture.loja().getId())
                .stream()
                .filter(d -> d.getIdUtilizador().getId().equals(colaborador.getId()))
                .findFirst()
                .orElseThrow();

        // Esta chamada é EXATAMENTE a que o DESKTOP/PainelGerentePedidosController faria
        // ao clicar em "Aprovar" — não há MockMvc aqui, é a BLL pura, tal como o JavaFX a usa.
        DayOff aprovado = dayOffBLL.aprovarPedidoFolga(pendente.getIdDayoff(), fixture.gerente().getId(), fixture.loja().getId());
        assertEquals("aprovado", aprovado.getEstado());

        flushAndClear();
        DayOff estadoFinal = dayOffRepository.findById(pendente.getIdDayoff()).orElseThrow();
        assertEquals("aprovado", estadoFinal.getEstado(),
                "O ciclo Web (submissao) -> Desktop (aprovacao) tem de refletir o estado "
                        + "final corretamente em ambas as interfaces, porque partilham a mesma BD.");
    }

    // =========================================================================
    // GRUPO C — Concorrência multi-loja / múltiplos vínculos
    // =========================================================================

    /**
     * Dois aprovadores do MESMO cargo de gestão e da MESMA loja (gerente +
     * subgerente, ambos em GESTAO/APROVACAO) tentam aprovar a MESMA permuta
     * em simultâneo. Ao contrário do cenário cross-store já documentado
     * anteriormente, aqui o isolamento é dentro de uma única loja — testa se
     * {@code obterPedidoPendenteGerivel} (que verifica {@code estado ==
     * pendente} antes de agir) resiste a duas aprovações concorrentes do
     * mesmo pedido por dois aprovadores legítimos diferentes.
     *
     * <p><b>CORRIGIDO (ver Revisao.md, ponto 22):</b> foi adicionado {@code @Version}
     * (optimistic locking) a {@code Permuta}. Este teste passou de "documenta o bug"
     * para "guarda a regressão": prova que, das duas aprovações concorrentes do mesmo
     * pedido por dois aprovadores legítimos da mesma loja, EXATAMENTE UMA sobrevive — a
     * outra é bloqueada (pelo guard "já tratado" ou por OptimisticLockingFailureException
     * no commit). Nunca mais há dupla aprovação silenciosa.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void gerenteESubgerenteDaMesmaLojaTentamAprovarAMesmaPermutaConcorrentemente() throws Exception {
        String uid = novoUuidLocal();
        java.util.List<Object> paraLimpar = new java.util.ArrayList<>();
        try {
            Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");
            Cargo cargoSubgerente = obterOuCriarCargo("subgerente", "Sub-Gerente");
            Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente FT");

            Loja loja = criarLojaSimples("Concorrencia Mesma Loja " + uid);
            Utilizador gerente = criarUtilizadorHashado("Gerente Concorrencia " + uid, "gerente.conc." + uid, "Pass123");
            Utilizador subgerente = criarUtilizadorHashado("Subgerente Concorrencia " + uid, "subgerente.conc." + uid, "Pass123");
            Utilizador solicitante = criarUtilizadorHashado("Solicitante Concorrencia " + uid, "solic.conc." + uid, "Pass123");
            Utilizador colega = criarUtilizadorHashado("Colega Concorrencia " + uid, "colega.conc." + uid, "Pass123");

            criarLigacaoAtiva(gerente, loja, cargoGerente);
            criarLigacaoAtiva(subgerente, loja, cargoSubgerente);
            Lojautilizador ligSolicitante = criarLigacaoAtiva(solicitante, loja, cargoFullTime);
            Lojautilizador ligColega = criarLigacaoAtiva(colega, loja, cargoFullTime);

            LocalDate dia = LocalDate.now().plusDays(28);
            Turno turnoMeu = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
            Turno turnoColega = salvarTurnoLocal("noite", LocalTime.of(15, 0), LocalTime.of(23, 0));

            Horario meuHorario = criarHorarioBruto(ligSolicitante, turnoMeu, dia);
            Horario horarioColega = criarHorarioBruto(ligColega, turnoColega, dia);

            Permuta pedido = permutaBLL.registarPedidoTroca(solicitante.getId(),
                    horarioRepository.findById(meuHorario.getId()).orElseThrow(),
                    horarioRepository.findById(horarioColega.getId()).orElseThrow());

            paraLimpar.add(pedido);
            paraLimpar.add(meuHorario);
            paraLimpar.add(horarioColega);
            paraLimpar.add(gerente);
            paraLimpar.add(subgerente);
            paraLimpar.add(solicitante);
            paraLimpar.add(colega);
            paraLimpar.add(loja);

            CountDownLatch arranque = new CountDownLatch(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Boolean> resultadoGerente = executor.submit(() ->
                        tentarAprovar(arranque, pedido.getId(), gerente.getId(), loja.getId()));
                Future<Boolean> resultadoSubgerente = executor.submit(() ->
                        tentarAprovar(arranque, pedido.getId(), subgerente.getId(), loja.getId()));

                boolean sucessoGerente = resultadoGerente.get(10, TimeUnit.SECONDS);
                boolean sucessoSubgerente = resultadoSubgerente.get(10, TimeUnit.SECONDS);

                // Com @Version em Permuta, dentro da MESMA loja exatamente UM dos dois
                // aprovadores sobrevive — o outro é bloqueado (guard "já tratado" ou
                // OptimisticLockingFailureException). Nunca dupla aprovação silenciosa.
                assertTrue(sucessoGerente ^ sucessoSubgerente,
                        "Exatamente um dos dois aprovadores concorrentes deve conseguir aprovar "
                                + "a permuta (o outro bloqueado por @Version/guard). gerente="
                                + sucessoGerente + " subgerente=" + sucessoSubgerente);
            } finally {
                executor.shutdownNow();
            }
        } finally {
            limparEntidades(paraLimpar);
        }
    }

    // =========================================================================
    // GRUPO E — DESKTOP: invocação direta dos métodos vinculados aos handlers
    // @FXML (sem TestFX — ver nota de decisão no Javadoc da classe)
    // =========================================================================

    /**
     * Rastreia {@code DESKTOP/GestaoLojaController.onGuardarClick()}
     * (linha 121-152), que chama {@code gestaoLojaBLL.guardarConfiguracao(...)}
     * (linha 152). Simula exatamente essa chamada para um colaborador
     * "Assistente" (fulltime, fora de GESTAO) como se ele tivesse alcançado
     * este ecrã no Desktop — confirma que a tela de Gestão de Loja está
     * protegida na camada de serviço, independentemente de qualquer
     * visibilidade de botão na sidebar.
     */
    @Test
    void desktopGestaoLojaController_onGuardarClick_BloqueiaColaboradorComum() {
        LojaFixture fixture = criarLojaComEquipaCompleta("desktop-gestao-loja");
        Utilizador colaboradorComum = fixture.colaboradores().get(0);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> gestaoLojaBLL.guardarConfiguracao(colaboradorComum.getId(),
                        new com.example.projeto2.API.Services.GestaoLojaService.ConfiguracaoLojaRequest(
                                LocalTime.of(9, 0), LocalTime.of(22, 0), List.of())),
                "DESKTOP/GestaoLojaController.onGuardarClick() (linha 152) delega em "
                        + "gestaoLojaBLL.guardarConfiguracao() — tem de rejeitar um colaborador "
                        + "fora de GESTAO, exatamente como o ecrã equivalente na Web.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissao"));
    }

    /**
     * Rastreia {@code DESKTOP/GestaoLojaController}, linha 812
     * ({@code gestaoLojaBLL.desativarTurno(...)}, o handler do botão "Desativar"
     * na tabela de turnos). Persona "Supervisor": está em APROVACAO mas fora de
     * GESTAO — o mesmo resultado já confirmado para a Web tem de se replicar
     * exatamente para a chamada que o botão do Desktop dispara.
     */
    @Test
    void desktopGestaoLojaController_desativarTurno_BloqueiaSupervisor() {
        LojaFixture fixture = criarLojaComEquipaCompleta("desktop-desativar-turno");
        Cargo cargoSupervisor = obterOuCriarCargo("supervisor", "Supervisor de Equipa");
        Utilizador supervisor = criarUtilizadorHashado("Supervisor Desktop", "supervisor.desktop." + novoUuidLocal(), "Pass123");
        criarLigacaoAtiva(supervisor, fixture.loja(), cargoSupervisor);
        Turno turno = salvarTurnoLocal("noite", LocalTime.of(15, 0), LocalTime.of(23, 0));
        flushAndClear();

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> gestaoLojaBLL.desativarTurno(supervisor.getId(), turno.getId()),
                "DESKTOP/GestaoLojaController linha 812 (botao 'Desativar' de um turno) chama "
                        + "gestaoLojaBLL.desativarTurno() — tem de rejeitar um supervisor (APROVACAO "
                        + "mas nao GESTAO), tal como na Web.");
        assertTrue(erro.getMessage().toLowerCase().contains("permissao"));
    }

    /**
     * Rastreia {@code DESKTOP/PainelGerentePedidosController} → o botão
     * "Aprovar" da tabela de folgas delega em
     * {@code PainelGerenteService.aprovarFolga(Integer, Integer)}
     * (linhas 98-101), que por sua vez chama
     * {@code dayOffBLL.aprovarPedidoFolga(idPedido, idUtilizadorGestor)} — o
     * overload de <b>2 argumentos, SEM {@code idLoja}</b>. Confirma o caminho
     * feliz para um gerente de UMA SÓ loja (caso comum).
     */
    @Test
    void desktopPainelGerentePedidos_AprovarFolga_FuncionaParaGerenteDeUmaSoLoja() {
        LojaFixture fixture = criarLojaComEquipaCompleta("desktop-painel-aprova");
        Utilizador colaborador = fixture.colaboradores().get(0);
        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioPublicadoSemProposta(colaborador, LocalDate.now().plusDays(3), turno);
        flushAndClear();

        DayOff pedido = dayOffBLL.registarPedidoFolga(novoPedidoFolga(colaborador, dataFolgaMesSeguinte()));
        flushAndClear();

        // Chamada EXATA que o botao "Aprovar" do PainelGerentePedidosController dispara
        // via PainelGerenteService — sem MockMvc, é a BLL pura tal como o JavaFX a usa.
        painelGerenteBLL.aprovarFolga(pedido.getIdDayoff(), fixture.gerente().getId());

        flushAndClear();
        DayOff estadoFinal = dayOffRepository.findById(pedido.getIdDayoff()).orElseThrow();
        assertEquals("aprovado", estadoFinal.getEstado());
    }

    /**
     * <b>CORRIGIDO (bug 14.2 — ver Revisao.md, pontos 17/18 e 22):</b>
     * {@code PainelGerenteService} passou a injectar {@code SessaoService} e a resolver
     * a loja activa da sessão Desktop ({@code obterLojaAtivaSegura()}), propagando-a aos
     * overloads store-scoped de {@code dayOffBLL.aprovarPedidoFolga}. Este teste passou de
     * "documenta o bug" para "guarda a regressão": um gerente multi-loja que escolheu a
     * Loja B no login (loja trancada na sessão, exactamente como o ecrã de seleção de loja
     * do Desktop faz) consegue aprovar um pedido pendente da Loja B — antes era
     * arbitrariamente rejeitado por o serviço resolver a primeira loja alfabética (Loja A).
     */
    @Test
    void desktopPainelGerentePedidos_AprovarFolga_AprovaGerenteMultiLojaNaLojaActivaSecundaria() {
        String uid = novoUuidLocal();
        Loja lojaA = criarLojaSimples("AAA Loja Primaria " + uid); // nome cedo no alfabeto, de propósito
        Loja lojaB = criarLojaSimples("ZZZ Loja Secundaria " + uid);
        Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");
        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente FT");

        Utilizador gerenteMultiLoja = criarUtilizadorHashado("Gerente Multi Desktop " + uid, "gerente.multi.desktop." + uid, "Pass123");
        criarLigacaoAtiva(gerenteMultiLoja, lojaA, cargoGerente);
        criarLigacaoAtiva(gerenteMultiLoja, lojaB, cargoGerente);

        Utilizador colaboradorLojaB = criarUtilizadorHashado("Colaborador Loja B Desktop " + uid, "colab.lojab.desktop." + uid, "Pass123");
        Lojautilizador ligColaboradorB = criarLigacaoAtiva(colaboradorLojaB, lojaB, cargoFullTime);

        Turno turno = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarHorarioBruto(ligColaboradorB, turno, LocalDate.now().plusDays(3));
        flushAndClear();

        DayOff pedidoNaLojaB = dayOffBLL.registarPedidoFolga(
                novoPedidoFolga(colaboradorLojaB, dataFolgaMesSeguinte()));
        flushAndClear();

        try {
            // O gerente escolheu a Loja B no login — o ecrã de seleção tranca-a na sessão.
            // É exactamente isto que o botão "Aprovar" do Desktop tem por trás agora.
            sessaoBLL.definirLojaAtiva(lojaB.getId());
            painelGerenteBLL.aprovarFolga(pedidoNaLojaB.getIdDayoff(), gerenteMultiLoja.getId());
        } finally {
            sessaoBLL.definirLojaAtiva(null); // não vazar a loja activa para outros testes
        }

        flushAndClear();
        DayOff estadoFinal = dayOffRepository.findById(pedidoNaLojaB.getIdDayoff()).orElseThrow();
        assertEquals("aprovado", estadoFinal.getEstado(),
                "Um gerente legitimo de Loja B, com a Loja B trancada na sessao, deve conseguir "
                        + "aprovar este pedido pelo Desktop, independentemente de ter tambem ligacao a Loja A.");
    }

    // =========================================================================
    // GRUPO F — Concorrência cruzada Web + "Desktop" (BLL direta), lojas
    // distintas, em simultâneo
    // =========================================================================

    /**
     * Um colaborador submete uma preferência via Web (MockMvc, HTTP real,
     * Loja A) ao MESMO TEMPO que um "gerente no Desktop" (chamada BLL direta,
     * sem MockMvc) gera horários para a Loja B. Não há sobreposição de dados
     * entre as duas lojas — o objetivo é confirmar que a stack Web (servlet +
     * sessão HTTP) e a chamada direta de serviço (padrão Desktop) não colidem,
     * bloqueiam, nem lançam excePções inesperadas quando correm em paralelo
     * contra a mesma base de dados.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void webESimulacaoDesktopOperamSimultaneamenteEmLojasDistintasSemInterferencia() throws Exception {
        String uid = novoUuidLocal();
        List<Object> paraLimpar = new java.util.ArrayList<>();
        try {
            org.springframework.transaction.support.TransactionTemplate transacaoDeSetup =
                    new org.springframework.transaction.support.TransactionTemplate(transactionManager);

            GeracaoFixture fixtureDesktop = transacaoDeSetup.execute(status -> criarContextoGeracao("grupo-f-desktop-" + uid));
            LojaFixture fixtureWeb = transacaoDeSetup.execute(status -> criarLojaComEquipaCompleta("grupo-f-web-" + uid));
            paraLimpar.add(fixtureDesktop.lojaFixture().loja());
            paraLimpar.add(fixtureWeb.loja());

            Utilizador colaboradorWeb = fixtureWeb.colaboradores().get(0);
            MockHttpSession sessaoWeb = sessaoDoColaborador(colaboradorWeb, fixtureWeb.loja());

            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<Exception> tarefaWeb = executor.submit(() -> {
                    try {
                        mockMvc.perform(post("/web/complementares/preferencias")
                                        .session(sessaoWeb)
                                        .param("tipo", "folga_preferida")
                                        .param("diaSemana", "SUNDAY")
                                        .param("descricao", "Preferencia concorrente Grupo F (Web)."))
                                .andExpect(status().is3xxRedirection());
                        return null;
                    } catch (Exception ex) {
                        return ex;
                    }
                });

                Future<Exception> tarefaDesktop = executor.submit(() -> {
                    try {
                        // Chamada direta de servico — exatamente o padrao do
                        // DESKTOP/GeracaoHorariosController ao clicar "Gerar Proposta".
                        geracaoHorariosBLL.gerarPropostas(
                                fixtureDesktop.lojaFixture().gerente().getId(),
                                fixtureDesktop.referencia().getYear(),
                                fixtureDesktop.referencia().getMonthValue(),
                                1);
                        return null;
                    } catch (com.example.projeto2.API.Services.geracao.FalhaGeracaoHorarioException falhaEsperavel) {
                        return null;
                    } catch (Exception ex) {
                        return ex;
                    }
                });

                Exception erroWeb = tarefaWeb.get(20, TimeUnit.SECONDS);
                Exception erroDesktop = tarefaDesktop.get(20, TimeUnit.SECONDS);

                assertEquals(null, erroWeb,
                        "A submissao Web nao pode falhar so porque correu em paralelo com a "
                                + "geracao 'Desktop' de outra loja: " + erroWeb);
                assertEquals(null, erroDesktop,
                        "A geracao 'Desktop' nao pode falhar so porque correu em paralelo com a "
                                + "submissao Web de outra loja: " + erroDesktop);
            } finally {
                executor.shutdownNow();
            }

            // Sem flushAndClear() aqui: este metodo esta com a transacao de teste
            // suspensa (NOT_SUPPORTED) — EntityManager.flush() exige uma transacao
            // ativa. A leitura abaixo e uma query fresca, ja ve os dados comitados
            // pelas chamadas REQUIRED proprias do MockMvc/servico.
            assertEquals(1, preferenciaBLL.listarPreferenciasPorUtilizador(colaboradorWeb.getId()).size(),
                    "A preferencia submetida na Web tem de ter sido persistida corretamente, "
                            + "sem interferencia da geracao concorrente noutra loja.");
        } finally {
            limparEntidades(paraLimpar);
        }
    }

    // =========================================================================
    // GRUPO D — Código morto / rotas sem proteção (descoberta, não correção)
    // =========================================================================

    /**
     * <b>DESCOBERTA (ver Revisao.md):</b> {@code WebAuthApiController} expõe
     * {@code POST /api/auth/login} e {@code POST /api/auth/logout} — uma
     * segunda via de autenticação completa, paralela a
     * {@code WebLoginController#autenticar} (POST /web/login). Não encontrei
     * nenhuma referência a {@code /api/auth} em nenhum template Thymeleaf nem
     * recurso estático (grep exaustivo em {@code src/main/resources}) — é
     * código alcançável, funcional, mas não consumido por nenhuma página
     * atual. Por estar mapeado em {@code /api/**}, fica também fora do âmbito
     * do {@code WebGuardInterceptor} (que só cobre {@code /web/**}) — o que é
     * irrelevante para um endpoint de login (tem de ser público), mas
     * confirma que esta rota nunca passa por nenhuma camada de proteção
     * adicional. Este teste não verifica um bug — verifica que a rota está,
     * de facto, viva e acessível, suportando a recomendação no Revisao.md de
     * decidirem se a querem manter, documentar como API pública, ou remover.
     */
    @Test
    void apiAuthLoginEhCodigoAlcancavelMasNaoReferenciadoPorNenhumaViewAtual() throws Exception {
        LojaFixture fixture = criarLojaComEquipaCompleta("dead-code-auth");
        Utilizador colaborador = fixture.colaboradores().get(0);

        String corpo = "{\"email\":\"" + colaborador.getEmail() + "\",\"password\":\"Colaborador123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(corpo))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private MockHttpSession sessaoDoColaborador(Utilizador colaborador, Loja loja) {
        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(WebSession.UTILIZADOR_ID, colaborador.getId());
        sessao.setAttribute(WebSession.UTILIZADOR_NOME, colaborador.getNome());
        sessao.setAttribute(WebSession.UTILIZADOR_EMAIL, colaborador.getEmail());
        sessao.setAttribute(WebSession.LOJA_ID, loja.getId());
        return sessao;
    }

    private boolean tentarAprovar(CountDownLatch latch, Integer idPermuta, Integer idAprovador, Integer idLoja) {
        latch.countDown();
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            permutaBLL.aprovarPedidoPermuta(idPermuta, idAprovador, idLoja);
            return true;
        } catch (RuntimeException bloqueado) {
            // Bloqueado por guard "já tratado" (IllegalArgumentException) OU por optimistic
            // locking (@Version em Permuta → OptimisticLockingFailureException no commit).
            return false;
        }
    }

    private DayOff novoPedidoFolga(Utilizador utilizador, LocalDate data) {
        DayOff pedido = new DayOff();
        Utilizador proxy = new Utilizador();
        proxy.setId(utilizador.getId());
        pedido.setIdUtilizador(proxy);
        pedido.setDataAusencia(data);
        pedido.setTipo("folgas");
        pedido.setMotivo("Pedido de teste — fluxos totais por persona");
        return pedido;
    }

    private Loja criarLojaSimples(String nome) {
        Loja loja = new Loja();
        loja.setNome(nome);
        loja.setLocalizacao("Ambiente de testes");
        loja.setHoraAbertura(LocalTime.of(9, 0));
        loja.setHoraFecho(LocalTime.of(23, 59));
        return lojaRepository.save(loja);
    }

    /** IDs dos turnos criados localmente neste teste — purgados em {@link #limparEntidades(List)}. */
    private final java.util.List<Integer> idsTurnosLocais = new java.util.ArrayList<>();

    private Turno salvarTurnoLocal(String tipo, LocalTime inicio, LocalTime fim) {
        Turno t = new Turno();
        t.setTipo(tipo);
        t.setHoraInicio(inicio);
        t.setHoraFim(fim);
        Turno salvo = turnoRepository.save(t);
        idsTurnosLocais.add(salvo.getId());
        return salvo;
    }

    private Horario criarHorarioBruto(Lojautilizador ligacao, Turno turno, LocalDate dia) {
        Horario h = new Horario();
        h.setIdLojautilizador(ligacao);
        h.setIdTurno(turno);
        h.setDataTurno(dia);
        h.setEstado(EstadoHorario.aprovado);
        return horarioRepository.save(h);
    }

    private Cenario montarCenarioPermutaSimples(String prefixo) {
        LojaFixture fixture = criarLojaComEquipaCompleta(prefixo);
        Utilizador solicitante = fixture.colaboradores().get(0);
        Utilizador colega = fixture.colaboradores().get(1);
        LocalDate dia = LocalDate.now().plusDays(22);

        Turno turnoMeu = salvarTurnoLocal("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        Turno turnoColegaT = salvarTurnoLocal("noite", LocalTime.of(15, 0), LocalTime.of(23, 0));

        Lojautilizador ligSolicitante = lojautilizadorRepository
                .findLigacaoAtivaByIdUtilizador(solicitante.getId()).orElseThrow();
        Lojautilizador ligColega = lojautilizadorRepository
                .findLigacaoAtivaByIdUtilizador(colega.getId()).orElseThrow();

        Horario meuHorario = criarHorarioBruto(ligSolicitante, turnoMeu, dia);
        Horario horarioColega = criarHorarioBruto(ligColega, turnoColegaT, dia);
        flushAndClear();

        permutaBLL.registarPedidoTroca(solicitante.getId(),
                horarioRepository.findById(meuHorario.getId()).orElseThrow(),
                horarioRepository.findById(horarioColega.getId()).orElseThrow());
        flushAndClear();

        return new Cenario(solicitante);
    }

    private String novoUuidLocal() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Limpeza manual para os testes do Grupo C — a transação de teste foi
     * suspensa de propósito (mesma técnica e mesma ressalva documentada em
     * {@code SistemaMultiLojaStressEndToEndTest}: best-effort, ordem de FK
     * pode falhar parcialmente; falhas são logadas, nunca relançadas).
     */
    private void limparEntidades(List<Object> entidades) {
        for (Object entidade : entidades) {
            try {
                if (entidade instanceof Permuta p) {
                    permutaRepositoryAutowired.deleteById(p.getId());
                } else if (entidade instanceof Horario h) {
                    // Apaga primeiro qualquer Permuta dinamica (ex.: criada pelas threads
                    // concorrentes deste teste) que ainda referencie este horario — sem
                    // isto o DELETE falha por FK e e silenciosamente engolido, deixando o
                    // Horario e, em cascata, o Turno global orfaos na BD partilhada.
                    permutaRepositoryAutowired.deleteByHorarioId(h.getId());
                    horarioRepository.deleteById(h.getId());
                } else if (entidade instanceof Loja l) {
                    lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(l.getId())
                            .forEach(lu -> lojautilizadorRepository.deleteById(lu.getId()));
                    lojaRepository.deleteById(l.getId());
                } else if (entidade instanceof Utilizador u) {
                    utilizadorRepository.deleteById(u.getId());
                }
            } catch (Exception ignorada) {
                System.err.println("[limparEntidades] Falha ao limpar " + entidade.getClass().getSimpleName()
                        + ": " + ignorada.getMessage());
            }
        }

        // Purga os turnos criados por salvarTurnoLocal (orphan-safe): só apaga os que
        // já não têm horários a apontar para eles — os horários do teste foram removidos
        // no loop acima. Elimina de vez o leak da tabela 'turnos' (ver Revisao.md 21.8.4).
        for (Integer idTurno : idsTurnosLocais) {
            try {
                if (turnoRepository.existsById(idTurno) && !turnoRepository.existeEmHorarios(idTurno)) {
                    turnoRepository.deleteById(idTurno);
                }
            } catch (Exception ignorada) {
                System.err.println("[limparEntidades] Falha ao limpar turno " + idTurno
                        + ": " + ignorada.getMessage());
            }
        }
        idsTurnosLocais.clear();
    }

    private record Cenario(Utilizador solicitante) {
    }
}
