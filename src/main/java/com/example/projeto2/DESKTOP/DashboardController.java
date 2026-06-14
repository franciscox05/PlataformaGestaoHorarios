package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.API.Services.SessaoService;
import com.example.projeto2.DESKTOP.support.DashboardPesquisaHelper;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.DESKTOP.support.TabelaHelper;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.UIConstants;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class DashboardController implements DashboardNavigator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private BorderPane mainContainer;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnGestaoLoja;

    @FXML
    private Button btnGestaoFuncionarios;

    @FXML
    private Button btnHorarios;

    @FXML
    private Button btnPainelGerente;


    @FXML
    private Button btnFolgas;

    @FXML
    private Button btnPermutas;

    @FXML
    private Button btnPerfil;

    @FXML
    private Button btnPreferencias;

    @FXML
    private Button btnRelatorios;

    @FXML
    private Label lblTopPageTitle;

    @FXML
    private Label lblTopUserName;

    @FXML
    private Label lblTopUserRole;

    @FXML
    private Label lblUtilizadorSidebar;

    @FXML
    private Label lblSecaoGestao;

    @FXML
    private Label lblAvatarInicial;

    @FXML
    private Label lblCargoSidebar;

    @FXML
    private Button btnTopoVisaoGeral;

    @FXML
    private Button btnTopoPedidos;

    @FXML
    private Button btnTopoPerfil;

    @FXML
    private TextField txtPesquisa;

    @FXML
    private Button btnFecharAplicacao;

    @FXML
    private HBox boxPesquisa;

    @FXML
    private StackPane stackHorarios;

    @FXML
    private StackPane stackPainelGerente;

    @FXML
    private Label badgeFolgas;

    @FXML
    private Label badgePermutas;

    @FXML
    private Label badgePreferencias;

    @FXML
    private Label badgeHorarios;

    @FXML
    private Label badgePainelGerente;

    private final ApplicationContext applicationContext;
    private final GestaoLojaService gestaoLojaBLL;
    private final GeracaoHorariosService geracaoHorariosBLL;
    private final SessaoService sessaoBLL;
    private final DayOffService dayOffBLL;
    private final PermutaService permutaBLL;
    private final PreferenciaService preferenciaBLL;
    private final EventHandler<MouseEvent> handlerMouse = event -> registarAtividadeSessao();
    private final EventHandler<KeyEvent> handlerTeclado = event -> registarAtividadeSessao();
    private final EventHandler<ScrollEvent> handlerScroll = event -> registarAtividadeSessao();
    private DashboardPesquisaHelper pesquisaHelper;

    private Utilizador utilizadorLogado;
    private PauseTransition temporizadorSessao;
    private Scene sceneMonitorizada;
    private Timeline timelineRefreshBadges;

    public DashboardController(ApplicationContext applicationContext,
                               GestaoLojaService gestaoLojaBLL,
                               GeracaoHorariosService geracaoHorariosBLL,
                               SessaoService sessaoBLL,
                               DayOffService dayOffBLL,
                               PermutaService permutaBLL,
                               PreferenciaService preferenciaBLL) {
        this.applicationContext = applicationContext;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.sessaoBLL = sessaoBLL;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.preferenciaBLL = preferenciaBLL;
    }

    @FXML
    public void initialize() {
        aplicarTemaDashboard();
        pesquisaHelper = new DashboardPesquisaHelper(txtPesquisa, boxPesquisa, this::construirEntradasPesquisa);
        pesquisaHelper.configurar();
        configurarTooltipsSidebar();
    }

    private void configurarTooltipsSidebar() {
        btnDashboard.setTooltip(new Tooltip("Painel inicial  (Alt+1)"));
        btnFolgas.setTooltip(new Tooltip("Folgas e ausências  (Alt+2)"));
        btnPermutas.setTooltip(new Tooltip("Trocar turnos  (Alt+3)"));
        btnPreferencias.setTooltip(new Tooltip("Preferências de horário  (Alt+4)"));
        btnPerfil.setTooltip(new Tooltip("O teu perfil  (Alt+5)"));
        if (btnHorarios != null) {
            btnHorarios.setTooltip(new Tooltip("Horários da loja  (Alt+H)"));
        }
        if (btnPainelGerente != null) {
            btnPainelGerente.setTooltip(new Tooltip("Pedidos pendentes da loja  (Alt+G / Alt+P)"));
        }
        if (btnGestaoLoja != null) {
            btnGestaoLoja.setTooltip(new Tooltip("Configurações da loja  (Alt+L)"));
        }
        if (btnGestaoFuncionarios != null) {
            btnGestaoFuncionarios.setTooltip(new Tooltip("Gerir funcionários  (Alt+F)"));
        }
        if (btnRelatorios != null) {
            btnRelatorios.setTooltip(new Tooltip("Relatórios de horas"));
        }
    }

    private void aplicarTemaDashboard() {
        if (mainContainer == null) {
            return;
        }

        String css = "/com/example/projeto2/dashboard/dashboard.css";
        var recursoCss = getClass().getResource(css);
        if (recursoCss != null) {
            String urlCss = recursoCss.toExternalForm();
            if (mainContainer.getStylesheets().stream().noneMatch(urlCss::equals)) {
                mainContainer.getStylesheets().add(urlCss);
            }
        }

        if (!mainContainer.getStyleClass().contains("page-shell")) {
            mainContainer.getStyleClass().add("page-shell");
        }
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        sessaoBLL.iniciarSessao(utilizador);
        atualizarIdentidadeUtilizador();
        configurarPermissoesMenu();
        configurarMonitorizacaoSessao();
        atualizarBadgesSidebar();

        iniciarAutoRefreshBadges();

        if (!abrirDashboardHome()) {
            sessaoBLL.terminarSessaoManual();
            utilizadorLogado = null;
            throw new IllegalStateException("Não foi possível carregar a página inicial do painel.");
        }
    }

    private void iniciarAutoRefreshBadges() {
        pararAutoRefreshBadges();
        timelineRefreshBadges = new Timeline(
                new KeyFrame(Duration.minutes(5), evt -> atualizarBadgesSidebar())
        );
        timelineRefreshBadges.setCycleCount(Timeline.INDEFINITE);
        timelineRefreshBadges.play();
    }

    private void pararAutoRefreshBadges() {
        if (timelineRefreshBadges != null) {
            timelineRefreshBadges.stop();
            timelineRefreshBadges = null;
        }
    }

    @FXML
    public void onDashboardHomeClick() {
        abrirDashboardHome();
    }

    private boolean abrirDashboardHome() {
        atualizarTituloTopo("Painel de Controlo");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnDashboard.getStyleClass().add("sidebar-btn-ativo");
        return mudarEcraCentro("/com/example/projeto2/dashboard/home-view.fxml");
    }

    @FXML
    public void onPedirFolgaClick() {
        atualizarTituloTopo("Pedir folga");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnFolgas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/pedir-folga-view.fxml");
        atualizarBadgesSidebar();
    }

    @FXML
    public void onGestaoLojaClick() {
        atualizarTituloTopo("Loja e regras");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnGestaoLoja.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-loja-view.fxml");
    }

    @FXML
    public void onGestaoFuncionariosClick() {
        atualizarTituloTopo("Gest\u00E3o de funcion\u00E1rios");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnGestaoFuncionarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-funcionarios-view.fxml");
    }

    @FXML
    public void onHorariosClick() {
        atualizarTituloTopo("Hor\u00E1rios da loja");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnHorarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/geracao-horarios-view.fxml");
    }

    @FXML
    public void onPainelGerentePedidosClick() {
        atualizarTituloTopo("Painel do gerente");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPainelGerente.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/painel-gerente-pedidos-view.fxml");
    }


    @FXML
    public void onTrocarTurnoClick() {
        atualizarTituloTopo("Trocar turno");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPermutas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/permutas-view.fxml");
        atualizarBadgesSidebar();
    }

    @FXML
    public void onPerfilClick() {
        atualizarTituloTopo("Perfil");
        atualizarAtalhosTopo(btnTopoPerfil);
        limparBotoesAtivos();
        btnPerfil.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/perfil-view.fxml");
    }

    @FXML
    public void onPreferenciasClick() {
        atualizarTituloTopo("Prefer\u00EAncias");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPreferencias.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/preferencias-view.fxml");
    }

    @FXML
    public void onRelatoriosHorasClick() {
        atualizarTituloTopo("Relat\u00F3rios");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnRelatorios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/relatorios-horas-view.fxml");
    }

    @FXML
    public void onTopVisaoGeralClick() {
        abrirDashboardHome();
    }

    @FXML
    public void onTopPedidosClick() {
        if (stackPainelGerente != null && stackPainelGerente.isVisible()) {
            onPainelGerentePedidosClick();
            return;
        }

        onPedirFolgaClick();
    }

    @FXML
    public void onTopPerfilClick() {
        onPerfilClick();
    }

    @FXML
    public void onLogoutClick() {
        if (!DialogosHelper.confirmarAcao(
                obterJanelaAtual(),
                "Terminar sessão",
                "Deseja terminar sessão?",
                "Vais sair do painel atual e regressar ao ecrã de autenticação."
        )) {
            return;
        }

        sessaoBLL.terminarSessaoManual();
        encerrarMonitorizacaoSessao();
        pararAutoRefreshBadges();
        utilizadorLogado = null;
        abrirLogin(false);
    }

    @FXML
    public void onFecharAplicacaoClick() {
        Stage stage = obterStageAtual();
        if (stage != null) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private boolean mudarEcraCentro(String caminhoFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent novoConteudo = loader.load();

            Object controller = loader.getController();

            if (controller instanceof HomeController homeController) {
                homeController.setDashboardNavigation(this);
                homeController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GestaoLojaController gestaoLojaController) {
                gestaoLojaController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GestaoFuncionariosController gestaoFuncionariosController) {
                gestaoFuncionariosController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GeracaoHorariosController geracaoHorariosController) {
                geracaoHorariosController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PainelGerentePedidosController painelGerentePedidosController) {
                painelGerentePedidosController.setDashboardNavigation(this);
                painelGerentePedidosController.setUtilizadorLogado(utilizadorLogado);

            } else if (controller instanceof PermutasController permutasController) {
                permutasController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PedirFolgaController pedirFolgaController) {
                pedirFolgaController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PerfilController perfilController) {
                perfilController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PreferenciasController preferenciasController) {
                preferenciasController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof RelatoriosHorasController relatoriosHorasController) {
                relatoriosHorasController.setUtilizadorLogado(utilizadorLogado);
            }

            novoConteudo.setOpacity(0.0);
            mainContainer.setCenter(novoConteudo);
            TabelaHelper.prepararArvore(novoConteudo);
            registarAtividadeSessao();

            // Fade-in suave ao mudar de módulo
            FadeTransition ft = new FadeTransition(Duration.millis(180), novoConteudo);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            return true;
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar o ecra {}", caminhoFxml, e);
            if (mainContainer != null) {
                mainContainer.setCenter(criarConteudoErroCentro(caminhoFxml));
            }
            return false;
        }
    }

    private Parent criarConteudoErroCentro(String caminhoFxml) {
        Label titulo = new Label("Não foi possível abrir este ecrã");
        titulo.getStyleClass().add("titulo-dashboard");

        Label descricao = new Label("O painel encontrou um erro ao carregar esta página. Podes tentar novamente agora ou voltar a abrir o painel.");
        descricao.getStyleClass().add("texto-ajuda");
        descricao.setWrapText(true);

        Button btnTentarNovamente = new Button("Tentar novamente");
        btnTentarNovamente.getStyleClass().add("botao-acao");
        btnTentarNovamente.setOnAction(event -> mudarEcraCentro(caminhoFxml));

        Button btnVoltarDashboard = new Button("Voltar ao painel");
        btnVoltarDashboard.getStyleClass().add("botao-secundario");
        btnVoltarDashboard.setOnAction(event -> abrirDashboardHome());

        VBox conteudo = new VBox(16.0, titulo, descricao, btnTentarNovamente, btnVoltarDashboard);
        conteudo.getStyleClass().add("info-card");
        conteudo.setFillWidth(false);
        conteudo.setMaxWidth(560.0);
        conteudo.setTranslateY(20.0);
        return conteudo;
    }

    private void limparBotoesAtivos() {
        btnDashboard.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoLoja.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoFuncionarios.getStyleClass().remove("sidebar-btn-ativo");
        btnHorarios.getStyleClass().remove("sidebar-btn-ativo");
        btnPainelGerente.getStyleClass().remove("sidebar-btn-ativo");
        btnFolgas.getStyleClass().remove("sidebar-btn-ativo");
        btnPermutas.getStyleClass().remove("sidebar-btn-ativo");
        btnPerfil.getStyleClass().remove("sidebar-btn-ativo");
        btnPreferencias.getStyleClass().remove("sidebar-btn-ativo");
        btnRelatorios.getStyleClass().remove("sidebar-btn-ativo");
    }

    private void configurarPermissoesMenu() {
        boolean podeGerirLoja = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());
        boolean podeAcederHorarios = utilizadorLogado != null
                && (podeGerirLoja || geracaoHorariosBLL.utilizadorPodeValidarHorarios(utilizadorLogado.getId()));
        btnGestaoLoja.setVisible(podeGerirLoja);
        btnGestaoLoja.setManaged(podeGerirLoja);
        btnRelatorios.setVisible(podeGerirLoja);
        btnRelatorios.setManaged(podeGerirLoja);
        btnGestaoFuncionarios.setVisible(podeGerirLoja);
        btnGestaoFuncionarios.setManaged(podeGerirLoja);
        if (stackHorarios != null) {
            stackHorarios.setVisible(podeAcederHorarios);
            stackHorarios.setManaged(podeAcederHorarios);
        }
        if (stackPainelGerente != null) {
            stackPainelGerente.setVisible(podeGerirLoja);
            stackPainelGerente.setManaged(podeGerirLoja);
        }
        if (lblSecaoGestao != null) {
            lblSecaoGestao.setVisible(podeAcederHorarios);
            lblSecaoGestao.setManaged(podeAcederHorarios);
        }
    }

    private void atualizarBadgesSidebar() {
        if (utilizadorLogado == null) return;
        int idUtilizador = utilizadorLogado.getId();
        Platform.runLater(() -> {
            try {
                atualizarBadge(badgeFolgas, dayOffBLL.contarPendentesParaAprovacao(idUtilizador));
                atualizarBadge(badgePermutas, permutaBLL.contarPendentesParaAprovacao(idUtilizador));
                atualizarBadge(badgePreferencias, preferenciaBLL.contarPendentesParaAprovacao(idUtilizador));
                atualizarBadge(badgeHorarios, geracaoHorariosBLL.contarHorariosPendentesValidacao(idUtilizador));
                int totalGerente = dayOffBLL.contarPendentesParaAprovacao(idUtilizador)
                        + permutaBLL.contarPendentesParaAprovacao(idUtilizador)
                        + preferenciaBLL.contarPendentesParaAprovacao(idUtilizador);
                atualizarBadge(badgePainelGerente, totalGerente);
            } catch (Exception e) {
                LOGGER.debug("Nao foi possivel atualizar badges do sidebar: {}", e.getMessage());
            }
        });
    }

    private void atualizarBadge(Label badge, int contagem) {
        if (badge == null) return;
        if (contagem <= 0) {
            badge.setVisible(false);
        } else {
            badge.setText(contagem > 99 ? "99+" : String.valueOf(contagem));
            badge.setVisible(true);
        }
    }

    private void atualizarTituloTopo(String titulo) {
        if (lblTopPageTitle != null) {
            lblTopPageTitle.setText(titulo);
        }
    }

    private void atualizarIdentidadeUtilizador() {
        if (utilizadorLogado == null) {
            return;
        }

        String nome = utilizadorLogado.getNome() != null ? utilizadorLogado.getNome().trim() : "";
        String primeiroNome = nome.isBlank() ? "Staff" : nome.split("\\s+")[0];

        if (lblUtilizadorSidebar != null) {
            lblUtilizadorSidebar.setText("Olá, " + primeiroNome + "!");
        }

        if (lblAvatarInicial != null) {
            String inicial = primeiroNome.isBlank() ? "S" : primeiroNome.substring(0, 1).toUpperCase();
            lblAvatarInicial.setText(inicial);
        }

        if (lblTopUserName != null) {
            lblTopUserName.setText(nome.isBlank() ? "Staff" : nome);
        }

        if (lblTopUserRole != null) {
            String email = utilizadorLogado.getEmail() != null ? utilizadorLogado.getEmail().trim() : "";
            lblTopUserRole.setText(email.isBlank() ? "Portal interno" : email);
        }

        // Cargo na sidebar
        if (lblCargoSidebar != null) {
            try {
                String cargo = gestaoLojaBLL.obterNomeCargo(utilizadorLogado.getId());
                if (cargo != null && !cargo.isBlank()) {
                    lblCargoSidebar.setText(cargo.toUpperCase());
                } else {
                    lblCargoSidebar.setText("VER PERFIL");
                }
            } catch (Exception e) {
                lblCargoSidebar.setText("VER PERFIL");
            }
        }
    }

    private void configurarMonitorizacaoSessao() {
        Platform.runLater(() -> instalarMonitorizacaoSessao(mainContainer != null ? mainContainer.getScene() : null));
    }

    private void instalarMonitorizacaoSessao(Scene scene) {
        if (scene == null || scene == sceneMonitorizada) {
            return;
        }

        encerrarMonitorizacaoSessao();
        sceneMonitorizada = scene;
        sceneMonitorizada.addEventFilter(MouseEvent.MOUSE_PRESSED, handlerMouse);
        sceneMonitorizada.addEventFilter(MouseEvent.MOUSE_MOVED, handlerMouse);
        sceneMonitorizada.addEventFilter(KeyEvent.KEY_PRESSED, handlerTeclado);
        sceneMonitorizada.addEventFilter(KeyEvent.KEY_PRESSED, this::processarShortcutGlobal);
        sceneMonitorizada.addEventFilter(ScrollEvent.SCROLL, handlerScroll);
        iniciarTemporizadorSessao();
    }

    private void iniciarTemporizadorSessao() {
        if (temporizadorSessao == null) {
            temporizadorSessao = new PauseTransition();
            temporizadorSessao.setOnFinished(event -> tratarSessaoExpirada());
        }

        temporizadorSessao.setDuration(Duration.millis(sessaoBLL.obterTempoMaximoInatividade().toMillis()));
        registarAtividadeSessao();
    }

    private void registarAtividadeSessao() {
        if (!sessaoBLL.temSessaoAtiva()) {
            return;
        }

        sessaoBLL.registarAtividade();

        if (temporizadorSessao != null) {
            temporizadorSessao.playFromStart();
        }
    }

    private void tratarSessaoExpirada() {
        if (!sessaoBLL.temSessaoAtiva()) {
            return;
        }

        sessaoBLL.expirarSessao();
        encerrarMonitorizacaoSessao();
        pararAutoRefreshBadges();
        utilizadorLogado = null;
        abrirLogin(true);
    }

    private void encerrarMonitorizacaoSessao() {
        if (temporizadorSessao != null) {
            temporizadorSessao.stop();
        }

        if (sceneMonitorizada != null) {
            sceneMonitorizada.removeEventFilter(MouseEvent.MOUSE_PRESSED, handlerMouse);
            sceneMonitorizada.removeEventFilter(MouseEvent.MOUSE_MOVED, handlerMouse);
            sceneMonitorizada.removeEventFilter(KeyEvent.KEY_PRESSED, handlerTeclado);
            sceneMonitorizada.removeEventFilter(KeyEvent.KEY_PRESSED, this::processarShortcutGlobal);
            sceneMonitorizada.removeEventFilter(ScrollEvent.SCROLL, handlerScroll);
            sceneMonitorizada = null;
        }
    }

    private void processarShortcutGlobal(KeyEvent evento) {
        // Não disparar shortcuts se o utilizador está a escrever num campo de texto
        if (evento.getTarget() instanceof javafx.scene.control.TextInputControl) {
            return;
        }
        if (!evento.isAltDown()) {
            return;
        }

        switch (evento.getCode()) {
            case DIGIT1 -> { onDashboardHomeClick(); evento.consume(); }
            case DIGIT2 -> { onPedirFolgaClick(); evento.consume(); }
            case DIGIT3 -> { onTrocarTurnoClick(); evento.consume(); }
            case DIGIT4 -> { onPreferenciasClick(); evento.consume(); }
            case DIGIT5 -> { onPerfilClick(); evento.consume(); }
            case H -> {
                if (stackHorarios != null && stackHorarios.isVisible()) {
                    onHorariosClick();
                    evento.consume();
                }
            }
            case G, P -> {
                if (stackPainelGerente != null && stackPainelGerente.isVisible()) {
                    onPainelGerentePedidosClick();
                    evento.consume();
                }
            }
            case L -> {
                if (btnGestaoLoja != null && btnGestaoLoja.isVisible()) {
                    onGestaoLojaClick();
                    evento.consume();
                }
            }
            case F -> {
                if (btnGestaoFuncionarios != null && btnGestaoFuncionarios.isVisible()) {
                    onGestaoFuncionariosClick();
                    evento.consume();
                }
            }
            default -> { /* Ignorar */ }
        }
    }

    private void abrirLogin(boolean sessaoExpirada) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/login/login-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(root, UIConstants.APP_WIDTH, UIConstants.APP_HEIGHT));
            stage.setTitle("Levi's Staff Portal - Autenticação");
            stage.setMinWidth(UIConstants.APP_MIN_WIDTH);
            stage.setMinHeight(UIConstants.APP_MIN_HEIGHT);
            stage.setResizable(false);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);

            if (sessaoExpirada) {
                Platform.runLater(() -> mostrarInformacao(
                        "Sessão terminada",
                        "A tua sessão terminou por inatividade. Inicia sessão novamente para continuar."
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o login.", e);
            mostrarErro(
                    "Não foi possível regressar ao login.",
                    "Fecha e volta a abrir a aplicação para continuares."
            );
        }
    }

    private void mostrarErro(String cabecalho, String conteudo) {
        DialogosHelper.mostrarErro(obterJanelaAtual(), "Erro", cabecalho, conteudo);
    }

    private void mostrarInformacao(String cabecalho, String conteudo) {
        DialogosHelper.mostrarInformacao(obterJanelaAtual(), "Informação", cabecalho, conteudo);
    }

    private void atualizarAtalhosTopo(Button botaoAtivo) {
        if (btnTopoVisaoGeral == null || btnTopoPedidos == null || btnTopoPerfil == null) {
            return;
        }

        btnTopoVisaoGeral.getStyleClass().remove("top-nav-button-ativo");
        btnTopoPedidos.getStyleClass().remove("top-nav-button-ativo");
        btnTopoPerfil.getStyleClass().remove("top-nav-button-ativo");

        if (botaoAtivo != null && !botaoAtivo.getStyleClass().contains("top-nav-button-ativo")) {
            botaoAtivo.getStyleClass().add("top-nav-button-ativo");
        }
    }

    private Window obterJanelaAtual() {
        return mainContainer != null && mainContainer.getScene() != null ? mainContainer.getScene().getWindow() : null;
    }

    private Stage obterStageAtual() {
        Window janela = obterJanelaAtual();
        return janela instanceof Stage stage ? stage : null;
    }

    private List<DashboardPesquisaHelper.EntradaPesquisa> construirEntradasPesquisa() {
        List<DashboardPesquisaHelper.EntradaPesquisa> entradas = new ArrayList<>();
        entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                "Painel",
                "Consultar os próximos turnos, a semana publicada e os atalhos principais.",
                this::abrirDashboardHome,
                "painel", "dashboard", "visao geral", "visão geral", "inicio", "início", "home", "turnos semana"
        ));
        entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                "Pedir folga",
                "Submeter férias, folgas e ausências e acompanhar o histórico dos pedidos.",
                this::onPedirFolgaClick,
                "folga", "folgas", "ferias", "férias", "ausencia", "ausência", "pedido", "pedir férias", "marcar folga"
        ));
        entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                "Trocar turno",
                "Criar e acompanhar pedidos de troca de turno com outros colegas.",
                this::onTrocarTurnoClick,
                "permuta", "permutas", "troca", "trocar turno", "turno", "troca de horario", "troca de horário"
        ));
        entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                "Perfil",
                "Editar nome, email, telemóvel e palavra-passe da conta.",
                this::onPerfilClick,
                "perfil", "conta", "dados pessoais", "editar email", "alterar password", "mudar telemovel", "seguranca", "segurança"
        ));
        entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                "Preferências",
                "Gerir disponibilidade, preferências permanentes e pedidos para aprovação.",
                this::onPreferenciasClick,
                "preferencias", "preferências", "disponibilidade", "preferencia permanente", "dias preferidos"
        ));
        if (btnGestaoLoja != null && btnGestaoLoja.isVisible()) {
            entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                    "Loja e regras",
                    "Atualizar horário de funcionamento, regras base e horários especiais da loja.",
                    this::onGestaoLojaClick,
                    "loja", "regras", "horario loja", "horário loja", "horarios especiais", "horários especiais", "abrir loja", "fechar loja"
            ));
        }
        if (btnGestaoFuncionarios != null && btnGestaoFuncionarios.isVisible()) {
            entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                    "Funcionários",
                    "Criar, editar, associar e desativar colaboradores da loja.",
                    this::onGestaoFuncionariosClick,
                    "funcionarios", "funcionários", "equipa", "colaboradores", "novo colaborador", "desativar colaborador"
            ));
        }
        if (stackHorarios != null && stackHorarios.isVisible()) {
            entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                    "Horários",
                    "Gerar propostas, validar e publicar horários mensais da loja.",
                    this::onHorariosClick,
                    "horarios", "horários", "planeamento", "escala", "escala da loja", "publicar horario", "gerar horario", "validar horario"
            ));
        }
        if (stackPainelGerente != null && stackPainelGerente.isVisible()) {
            entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                    "Painel do gerente",
                    "Aprovar folgas, permutas e preferências pendentes da equipa.",
                    this::onPainelGerentePedidosClick,
                    "gerente", "pedidos", "aprovacoes", "aprovações", "aprovar folga", "aprovar permuta", "aprovar preferencia"
            ));
        }
        if (btnRelatorios != null && btnRelatorios.isVisible()) {
            entradas.add(new DashboardPesquisaHelper.EntradaPesquisa(
                    "Relatórios",
                    "Gerar relatórios mensais de horas e exportar resultados.",
                    this::onRelatoriosHorasClick,
                    "relatorios", "relatórios", "horas", "relatorio", "relatório", "exportar csv", "mapa de horas"
            ));
        }
        return entradas;
    }

    @Override
    public void abrirDashboard() {
        onDashboardHomeClick();
    }

    @Override
    public void abrirFolgas() {
        onPedirFolgaClick();
    }

    @Override
    public void abrirPermutas() {
        onTrocarTurnoClick();
    }

    @Override
    public void abrirPerfil() {
        onPerfilClick();
    }

    @Override
    public void abrirPreferencias() {
        onPreferenciasClick();
    }

    @Override
    public void abrirPainelGerente() {
        onPainelGerentePedidosClick();
    }

    @Override
    public void abrirHorarios() {
        onHorariosClick();
    }

    @Override
    public void abrirRelatorios() {
        onRelatoriosHorasClick();
    }

    @Override
    public void atualizarBadges() {
        atualizarBadgesSidebar();
    }

}
