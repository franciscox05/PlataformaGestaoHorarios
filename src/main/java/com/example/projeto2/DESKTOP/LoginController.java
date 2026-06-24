package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.API.Services.SessaoService;
import com.example.projeto2.API.Services.UtilizadorService;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPasswordVisible;

    @FXML
    private Button btnMostrarSenha;

    @FXML
    private Label lblErro;

    private final UtilizadorService userBll;
    private final PerfilService perfilBLL;
    private final SessaoService sessaoBLL;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final ApplicationContext applicationContext;

    public LoginController(UtilizadorService userBll,
                           PerfilService perfilBLL,
                           SessaoService sessaoBLL,
                           LojautilizadorRepository lojautilizadorRepository,
                           ApplicationContext applicationContext) {
        this.userBll = userBll;
        this.perfilBLL = perfilBLL;
        this.sessaoBLL = sessaoBLL;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        if (txtPasswordVisible != null && txtPassword != null) {
            txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());
        }

        btnMostrarSenha.setText("");
        mudarIconeBotao("olho-aberto.png");
        esconderErro();

        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> esconderErro());
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> esconderErro());
        txtPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> esconderErro());

        txtEmail.setOnAction(event -> onLoginClick());
        txtPassword.setOnAction(event -> onLoginClick());
        txtPasswordVisible.setOnAction(event -> onLoginClick());
    }

    @FXML
    protected void onLoginClick() {
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        String password = txtPassword.getText() == null ? "" : txtPassword.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            mostrarErro("Preenche o email e a palavra-passe antes de continuares.");
            return;
        }

        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado == null) {
            mostrarErro("Email ou palavra-passe incorretos. Confirma os dados e tenta novamente.");
            return;
        }

        esconderErro();
        abrirDashboard(logado);
    }

    @FXML
    public void onMostrarSenhaClick() {
        if (txtPassword.isVisible()) {
            txtPassword.setVisible(false);
            txtPasswordVisible.setVisible(true);
            mudarIconeBotao("olho-fechado.png");
        } else {
            txtPassword.setVisible(true);
            txtPasswordVisible.setVisible(false);
            mudarIconeBotao("olho-aberto.png");
        }
    }

    @FXML
    public void onFecharAplicacaoClick() {
        Stage stage = (Stage) txtEmail.getScene().getWindow();
        if (stage != null) {
            stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private void mudarIconeBotao(String nomeImagem) {
        try (InputStream imageStream = getClass().getResourceAsStream("/com/example/projeto2/imagens/login/" + nomeImagem)) {
            if (imageStream == null) {
                LOGGER.warn("Imagem de autenticação não encontrada: {}", nomeImagem);
                return;
            }

            Image img = new Image(imageStream);
            ImageView view = new ImageView(img);
            view.setFitHeight(18);
            view.setFitWidth(18);
            btnMostrarSenha.setGraphic(view);
        } catch (IOException e) {
            LOGGER.warn("Não foi possível carregar o ícone de autenticação.", e);
        }
    }

    /**
     * Decide a rota pós-login espelhando {@code WebLoginController#autenticar}:
     * mais de 1 vínculo ativo → ecrã intermédio de seleção de loja; exatamente
     * 1 → tranca-a diretamente na sessão e segue para o dashboard. Ver
     * Revisao.md, ponto 17 (Fase 2 &amp; 3).
     */
    private void abrirDashboard(Utilizador logado) {
        List<Lojautilizador> ligacoesAtivas = lojautilizadorRepository.findLigacoesAtivasByIdUtilizador(logado.getId());

        if (ligacoesAtivas.size() > 1) {
            abrirEcraSelecaoLoja(logado, ligacoesAtivas);
            return;
        }

        if (ligacoesAtivas.size() == 1) {
            sessaoBLL.definirLojaAtiva(ligacoesAtivas.get(0).getIdLoja().getId());
        }

        abrirDashboardDireto(logado);
    }

    private void abrirEcraSelecaoLoja(Utilizador logado, List<Lojautilizador> ligacoesAtivas) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/login/selecionar-loja-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            SelecionarLojaController selecionarLojaController = loader.getController();
            selecionarLojaController.inicializarComLigacoes(logado, ligacoesAtivas, ContextoSelecao.LOGIN);

            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root, UIConstants.APP_WIDTH, UIConstants.APP_HEIGHT));
            stage.setTitle("Levi's Staff Portal - Selecionar Loja");
            stage.setMinWidth(UIConstants.APP_MIN_WIDTH);
            stage.setMinHeight(UIConstants.APP_MIN_HEIGHT);
            stage.setResizable(false);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o ecrã de seleção de loja.", e);
            mostrarErro("Não foi possível abrir a seleção de loja em segurança. Mantivemos-te no login para evitares entrar numa página vazia.");
        }
    }

    private void abrirDashboardDireto(Utilizador logado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/dashboard/dashboard-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            dashboardController.setUtilizadorLogado(logado);

            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root, UIConstants.APP_WIDTH, UIConstants.APP_HEIGHT));
            stage.setTitle("Levi's Staff Portal - Painel (UI V2)");
            stage.setMinWidth(UIConstants.APP_MIN_WIDTH);
            stage.setMinHeight(UIConstants.APP_MIN_HEIGHT);
            stage.setResizable(false);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            stage.setFullScreen(true);
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o dashboard.", e);
            mostrarErro("Não foi possível abrir o painel em segurança. Mantivemos-te no login para evitares entrar numa página vazia.");
        }
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
        lblErro.setManaged(true);
    }

    private void esconderErro() {
        lblErro.setText("");
        lblErro.setVisible(false);
        lblErro.setManaged(false);
    }
}
