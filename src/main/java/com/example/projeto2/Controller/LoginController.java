package com.example.projeto2.Controller;

import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

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

        // ====== Esconder o erro APENAS se o texto mudar ======
        txtEmail.textProperty().addListener((observable, oldValue, newValue) -> lblErro.setVisible(false));
        txtPassword.textProperty().addListener((observable, oldValue, newValue) -> lblErro.setVisible(false));

        // Ligar o "ENTER" ao Login
        txtEmail.setOnAction(event -> onLoginClick());
        txtPassword.setOnAction(event -> onLoginClick());
        txtPasswordVisible.setOnAction(event -> onLoginClick());
    }

    // Method de Login!
    @FXML
    protected void onLoginClick() {
        String email = txtEmail.getText();
        String password = txtPassword.getText(); // Sempre atualizado graças ao bindBidirectional!

        // 1. VALIDAÇÃO: Verificar se os campos estão vazios ANTES de ir à Base de Dados
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            lblErro.setText("Por favor, preencha o Email e/ou a Password.");
            lblErro.setVisible(true);
            return; // Aborta o login aqui, poupando a base de dados
        }

        // 2. BLL: Tentar autenticar o utilizador
        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado != null) {
            lblErro.setVisible(false); // Limpa qualquer erro antigo
            System.out.println("✅ SUCESSO! A abrir o Dashboard...");
            abrirDashboard(logado);
        } else {
            // 3. ERRO: Atualiza o texto para erro de credenciais erradas
            lblErro.setText("❌ Email ou Password incorretos.");
            lblErro.setVisible(true);
            System.out.println("❌ ERRO! Email ou Password incorretos.");
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
        try {
            // 1. Tenta carregar o ficheiro primeiro
            java.io.InputStream imageStream = getClass().getResourceAsStream("/com/example/projeto2/imagens/login/" + nomeImagem);

            // 2. O nosso guarda-costas para deixar o IntelliJ feliz
            if (imageStream == null) {
                System.out.println("⚠️ Imagem não encontrada no caminho especificado: " + nomeImagem);
                return; // Sai do method para evitar o erro fatal
            }

            // 3. Se chegou aqui, é porque a imagem existe. Já podemos usá-la!
            Image img = new Image(imageStream);
            ImageView view = new ImageView(img);

            view.setFitHeight(18);
            view.setFitWidth(18);

            btnMostrarSenha.setGraphic(view);

        } catch (Exception e) {
            System.out.println("Erro ao carregar a imagem: " + e.getMessage());
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
            System.out.println("Erro ao abrir o Dashboard: " + e.getMessage());
        }
    }
}