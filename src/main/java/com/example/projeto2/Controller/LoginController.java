package com.example.projeto2.Controller;

import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
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
import javafx.stage.Stage;
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

    private final UtilizadorBLL userBll;
    private final ApplicationContext applicationContext;

    public LoginController(UtilizadorBLL userBll, ApplicationContext applicationContext) {
        this.userBll = userBll;
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

        txtEmail.setOnAction(event -> onLoginClick());
        txtPassword.setOnAction(event -> onLoginClick());
        txtPasswordVisible.setOnAction(event -> onLoginClick());
    }

    @FXML
    protected void onLoginClick() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            mostrarErro("Preenche o email e a password para continuar.");
            return;
        }

        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado == null) {
            mostrarErro("Nao foi possivel iniciar sessao. Confirma as credenciais e o estado da conta.");
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

    private void mudarIconeBotao(String nomeImagem) {
        try (InputStream imageStream = getClass().getResourceAsStream("/com/example/projeto2/imagens/login/" + nomeImagem)) {
            if (imageStream == null) {
                LOGGER.warn("Imagem de autenticacao nao encontrada: {}", nomeImagem);
                return;
            }

            Image img = new Image(imageStream);
            ImageView view = new ImageView(img);
            view.setFitHeight(18);
            view.setFitWidth(18);
            btnMostrarSenha.setGraphic(view);
        } catch (IOException e) {
            LOGGER.warn("Nao foi possivel carregar o icone de autenticacao.", e);
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
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Levi's Staff Portal - Dashboard");
        } catch (IOException e) {
            LOGGER.error("Erro ao abrir o dashboard.", e);
            mostrarErro("A autenticacao foi concluida, mas nao foi possivel abrir o painel principal.");
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
