package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.API.Services.UtilizadorService;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.UIConstants;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
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
    private final ApplicationContext applicationContext;

    public LoginController(UtilizadorService userBll,
                           PerfilService perfilBLL,
                           ApplicationContext applicationContext) {
        this.userBll = userBll;
        this.perfilBLL = perfilBLL;
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
    public void onEsqueciPasswordClick() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Recuperar palavra-passe");
        dialog.setHeaderText("Indica o teu email e define uma nova palavra-passe.");

        ButtonType btnConfirmar = new ButtonType("Confirmar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

        TextField tfEmail = new TextField();
        tfEmail.setPromptText("O teu email de acesso");
        PasswordField pfNova = new PasswordField();
        pfNova.setPromptText("Nova palavra-passe (mín. 6 caracteres)");
        PasswordField pfConfirma = new PasswordField();
        pfConfirma.setPromptText("Confirmar nova palavra-passe");
        Label lblDialogErro = new Label();
        lblDialogErro.setStyle("-fx-text-fill: #c9141e; -fx-font-size: 11px;");
        lblDialogErro.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Email:"), 0, 0);
        grid.add(tfEmail, 1, 0);
        grid.add(new Label("Nova password:"), 0, 1);
        grid.add(pfNova, 1, 1);
        grid.add(new Label("Confirmar:"), 0, 2);
        grid.add(pfConfirma, 1, 2);

        VBox conteudo = new VBox(10, grid, lblDialogErro);
        conteudo.setPrefWidth(360);
        dialog.getDialogPane().setContent(conteudo);

        javafx.scene.Node botaoConfirmar = dialog.getDialogPane().lookupButton(btnConfirmar);
        botaoConfirmar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblDialogErro.setText("");
            try {
                perfilBLL.redefinirPassword(
                        tfEmail.getText(),
                        pfNova.getText(),
                        pfConfirma.getText());
            } catch (IllegalArgumentException e) {
                lblDialogErro.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.initOwner(txtEmail.getScene() != null ? txtEmail.getScene().getWindow() : null);
        dialog.showAndWait().ifPresent(resultado -> {
            if (resultado == btnConfirmar) {
                mostrarErro("Palavra-passe redefinida com sucesso. Inicia sessão com a nova password.");
            }
        });
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

    private void abrirDashboard(Utilizador logado) {
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
