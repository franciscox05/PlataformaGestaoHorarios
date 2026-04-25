package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.SessaoBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DashboardController implements DashboardNavigator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);
    private static final double APP_WIDTH = 1480;
    private static final double APP_HEIGHT = 920;
    private static final double APP_MIN_WIDTH = 1280;
    private static final double APP_MIN_HEIGHT = 780;

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
    private Button btnAuditoria;

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
    private Label lblUtilizadorSidebar;

    @FXML
    private Label lblAvatarInicial;

    @FXML
    private Button btnTopoVisaoGeral;

    @FXML
    private Button btnTopoPedidos;

    @FXML
    private Button btnTopoPerfil;

    private final ApplicationContext applicationContext;
    private final GestaoLojaBLL gestaoLojaBLL;
    private final GeracaoHorariosBLL geracaoHorariosBLL;
    private final SessaoBLL sessaoBLL;
    private final EventHandler<MouseEvent> handlerMouse = event -> registarAtividadeSessao();
    private final EventHandler<KeyEvent> handlerTeclado = event -> registarAtividadeSessao();
    private final EventHandler<ScrollEvent> handlerScroll = event -> registarAtividadeSessao();

    private Utilizador utilizadorLogado;
    private PauseTransition temporizadorSessao;
    private Scene sceneMonitorizada;

    public DashboardController(ApplicationContext applicationContext,
                               GestaoLojaBLL gestaoLojaBLL,
                               GeracaoHorariosBLL geracaoHorariosBLL,
                               SessaoBLL sessaoBLL) {
        this.applicationContext = applicationContext;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.sessaoBLL = sessaoBLL;
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        sessaoBLL.iniciarSessao(utilizador);
        atualizarIdentidadeUtilizador();
        configurarPermissoesMenu();
        configurarMonitorizacaoSessao();

        if (!abrirDashboardHome()) {
            sessaoBLL.terminarSessaoManual();
            utilizadorLogado = null;
            throw new IllegalStateException("Nao foi possivel carregar a pagina inicial do painel.");
        }
    }

    @FXML
    public void onDashboardHomeClick() {
        abrirDashboardHome();
    }

    private boolean abrirDashboardHome() {
        atualizarTituloTopo("OS MEUS PROXIMOS TURNOS");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnDashboard.getStyleClass().add("sidebar-btn-ativo");
        return mudarEcraCentro("/com/example/projeto2/dashboard/home-view.fxml");
    }

    @FXML
    public void onPedirFolgaClick() {
        atualizarTituloTopo("PEDIR FOLGA");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnFolgas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/pedir-folga-view.fxml");
    }

    @FXML
    public void onGestaoLojaClick() {
        atualizarTituloTopo("LOJA E REGRAS");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnGestaoLoja.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-loja-view.fxml");
    }

    @FXML
    public void onGestaoFuncionariosClick() {
        atualizarTituloTopo("GESTAO DE FUNCIONARIOS");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnGestaoFuncionarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-funcionarios-view.fxml");
    }

    @FXML
    public void onHorariosClick() {
        atualizarTituloTopo("HORARIOS DA LOJA");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnHorarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/geracao-horarios-view.fxml");
    }

    @FXML
    public void onPainelGerentePedidosClick() {
        atualizarTituloTopo("PAINEL DO GERENTE");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPainelGerente.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/painel-gerente-pedidos-view.fxml");
    }

    @FXML
    public void onPainelAuditoriaClick() {
        atualizarTituloTopo("AUDITORIA");
        atualizarAtalhosTopo(btnTopoVisaoGeral);
        limparBotoesAtivos();
        btnAuditoria.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/painel-auditoria-view.fxml");
    }

    @FXML
    public void onTrocarTurnoClick() {
        atualizarTituloTopo("TROCAR TURNO");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPermutas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/permutas-view.fxml");
    }

    @FXML
    public void onPerfilClick() {
        atualizarTituloTopo("PERFIL");
        atualizarAtalhosTopo(btnTopoPerfil);
        limparBotoesAtivos();
        btnPerfil.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/perfil-view.fxml");
    }

    @FXML
    public void onPreferenciasClick() {
        atualizarTituloTopo("PREFERENCIAS");
        atualizarAtalhosTopo(btnTopoPedidos);
        limparBotoesAtivos();
        btnPreferencias.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/preferencias-view.fxml");
    }

    @FXML
    public void onRelatoriosHorasClick() {
        atualizarTituloTopo("RELATORIOS");
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
        if (btnPainelGerente != null && btnPainelGerente.isVisible()) {
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
        sessaoBLL.terminarSessaoManual();
        encerrarMonitorizacaoSessao();
        utilizadorLogado = null;
        abrirLogin(false);
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
            } else if (controller instanceof PainelAuditoriaController painelAuditoriaController) {
                painelAuditoriaController.setUtilizadorLogado(utilizadorLogado);
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

            mainContainer.setCenter(novoConteudo);
            registarAtividadeSessao();
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
        Label titulo = new Label("Nao foi possivel abrir este ecra");
        titulo.getStyleClass().add("titulo-dashboard");

        Label descricao = new Label("O painel encontrou um erro ao carregar esta pagina. Podes tentar novamente agora ou voltar a abrir o painel.");
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
        btnAuditoria.getStyleClass().remove("sidebar-btn-ativo");
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
        btnHorarios.setVisible(podeAcederHorarios);
        btnHorarios.setManaged(podeAcederHorarios);
        btnPainelGerente.setVisible(podeGerirLoja);
        btnPainelGerente.setManaged(podeGerirLoja);
        btnAuditoria.setVisible(podeGerirLoja);
        btnAuditoria.setManaged(podeGerirLoja);
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
            lblUtilizadorSidebar.setText("Ola, " + primeiroNome + "!");
        }

        if (lblAvatarInicial != null) {
            String inicial = primeiroNome.isBlank() ? "S" : primeiroNome.substring(0, 1).toUpperCase();
            lblAvatarInicial.setText(inicial);
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
            sceneMonitorizada.removeEventFilter(ScrollEvent.SCROLL, handlerScroll);
            sceneMonitorizada = null;
        }
    }

    private void abrirLogin(boolean sessaoExpirada) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/login/login-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(root, APP_WIDTH, APP_HEIGHT));
            stage.setTitle("Levi's Staff Portal - Autenticacao");
            stage.setMinWidth(APP_MIN_WIDTH);
            stage.setMinHeight(APP_MIN_HEIGHT);

            if (sessaoExpirada) {
                Platform.runLater(() -> mostrarInformacao(
                        "Sessao terminada",
                        "A tua sessao terminou por inatividade. Inicia sessao novamente para continuar."
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o login.", e);
            mostrarErro(
                    "Nao foi possivel regressar ao login.",
                    "Fecha e volta a abrir a aplicacao para continuares."
            );
        }
    }

    private void mostrarErro(String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    private void mostrarInformacao(String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
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
}
