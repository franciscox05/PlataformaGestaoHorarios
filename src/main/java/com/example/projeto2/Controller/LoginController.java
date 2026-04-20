package com.example.projeto2.Controller;

import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class LoginController {

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
            mostrarErro("Preenche o email e a password antes de continuares.");
            return;
        }

        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado != null) {
            esconderErro();
            abrirDashboard(logado);
        } else {
            mostrarErro("Email ou password incorretos. Confirma os dados e tenta novamente.");
        }
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
                return;
            }

            Image img = new Image(imageStream);
            ImageView view = new ImageView(img);
            view.setFitHeight(18);
            view.setFitWidth(18);
            btnMostrarSenha.setGraphic(view);
        } catch (IOException ignored) {
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
            mostrarErro("Nao foi possivel abrir o dashboard. Tenta novamente dentro de instantes.");
            mostrarAlertaErro("Erro ao abrir dashboard", "Nao foi possivel carregar o dashboard com sucesso.");
        }
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
    }

    private void esconderErro() {
        lblErro.setText("");
        lblErro.setVisible(false);
    }

    private void mostrarAlertaErro(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
