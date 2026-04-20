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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

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
    private Button btnFolgas;

    @FXML
    private Button btnPermutas;

    @FXML
    private Button btnPerfil;

    @FXML
    private Button btnPreferencias;

    @FXML
    private Button btnRelatorios;

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
        configurarPermissoesMenu();
        configurarMonitorizacaoSessao();
        onDashboardHomeClick();
    }

    @FXML
    public void onDashboardHomeClick() {
        limparBotoesAtivos();
        btnDashboard.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/home-view.fxml");
    }

    @FXML
    public void onPedirFolgaClick() {
        limparBotoesAtivos();
        btnFolgas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/pedir-folga-view.fxml");
    }

    @FXML
    public void onGestaoLojaClick() {
        limparBotoesAtivos();
        btnGestaoLoja.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-loja-view.fxml");
    }

    @FXML
    public void onGestaoFuncionariosClick() {
        limparBotoesAtivos();
        btnGestaoFuncionarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/gestao-funcionarios-view.fxml");
    }

    @FXML
    public void onHorariosClick() {
        limparBotoesAtivos();
        btnHorarios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/geracao-horarios-view.fxml");
    }

    @FXML
    public void onTrocarTurnoClick() {
        limparBotoesAtivos();
        btnPermutas.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/permutas-view.fxml");
    }

    @FXML
    public void onPerfilClick() {
        limparBotoesAtivos();
        btnPerfil.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/perfil-view.fxml");
    }

    @FXML
    public void onPreferenciasClick() {
        limparBotoesAtivos();
        btnPreferencias.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/preferencias-view.fxml");
    }

    @FXML
    public void onRelatoriosHorasClick() {
        limparBotoesAtivos();
        btnRelatorios.getStyleClass().add("sidebar-btn-ativo");
        mudarEcraCentro("/com/example/projeto2/dashboard/relatorios-horas-view.fxml");
    }

    @FXML
    public void onLogoutClick() {
        sessaoBLL.terminarSessaoManual();
        encerrarMonitorizacaoSessao();
        utilizadorLogado = null;
        abrirLogin(false);
    }

    private void mudarEcraCentro(String caminhoFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent novoConteudo = loader.load();

            Object controller = loader.getController();

            if (controller instanceof HomeController homeController) {
                homeController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GestaoLojaController gestaoLojaController) {
                gestaoLojaController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GestaoFuncionariosController gestaoFuncionariosController) {
                gestaoFuncionariosController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GeracaoHorariosController geracaoHorariosController) {
                geracaoHorariosController.setUtilizadorLogado(utilizadorLogado);
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
        } catch (Exception e) {
            LOGGER.error("Erro ao carregar o ecra {}", caminhoFxml, e);
            mostrarAlertaErro(
                    "Nao foi possivel abrir este ecra.",
                    "Tenta novamente. Se o problema persistir, volta a iniciar sessao."
            );
        }
    }

    private void limparBotoesAtivos() {
        btnDashboard.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoLoja.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoFuncionarios.getStyleClass().remove("sidebar-btn-ativo");
        btnHorarios.getStyleClass().remove("sidebar-btn-ativo");
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
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Levi's Staff Portal - Autenticacao");

            if (sessaoExpirada) {
                Platform.runLater(() -> mostrarAlertaInformacao(
                        "Sessao terminada",
                        "A tua sessao terminou por inatividade. Inicia sessao novamente para continuar."
                ));
            }
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o login.", e);
            mostrarAlertaErro(
                    "Nao foi possivel regressar ao login.",
                    "Fecha e volta a abrir a aplicacao para continuares."
            );
        }
    }

    private void mostrarAlertaErro(String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }

    private void mostrarAlertaInformacao(String cabecalho, String conteudo) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informacao");
        alert.setHeaderText(cabecalho);
        alert.setContentText(conteudo);
        alert.showAndWait();
    }
}
