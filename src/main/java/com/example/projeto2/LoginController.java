package com.example.projeto2;

import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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

    private final UtilizadorBLL userBll;
    private final ApplicationContext applicationContext; // <--- NOVA FERRAMENTA DO SPRING

    // O Spring Boot injeta a BLL e o Contexto automaticamente!
    public LoginController(UtilizadorBLL userBll, ApplicationContext applicationContext) {
        this.userBll = userBll;
        this.applicationContext = applicationContext;
    }

    @FXML
    protected void onLoginClick() {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        Utilizador logado = userBll.efetuarLogin(email, password);

        if (logado != null) {
            System.out.println("✅ SUCESSO! A abrir o Dashboard...");
            abrirDashboard(logado); // <--- CHAMA O MÉTODO DE MUDAR DE ECRÃ
        } else {
            System.out.println("❌ ERRO! Email ou Password incorretos.");
        }
    }

    // Método que faz a transição visual e abre o Dashboard
    private void abrirDashboard(Utilizador logado) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("dashboard-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            DashboardController dashboardController = loader.getController();
            dashboardController.setUtilizadorLogado(logado);
            // ---------------------------------------------------

            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Levi's Staff Portal - Dashboard");

        } catch (IOException e) {
            System.out.println("Erro ao abrir o Dashboard: " + e.getMessage());
        }
    }
}