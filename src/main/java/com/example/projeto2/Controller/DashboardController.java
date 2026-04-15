package com.example.projeto2.Controller;

import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button; // <-- IMPORT CORRETO DO JAVAFX
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    @FXML private BorderPane mainContainer;
    @FXML private Button btnDashboard;
    @FXML private Button btnFolgas;
    @FXML private Button btnPermutas;

    private final ApplicationContext applicationContext;
    private Utilizador utilizadorLogado;

    public DashboardController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;

        // Arranca logo a página inicial com a tabela para não ficar o centro vazio!
        onDashboardHomeClick();
    }

    // ==========================================
    // BOTÕES DO MENU LATERAL
    // ==========================================

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

        // mudarEcraCentro("/com/example/projeto2/dashboard/pedir-folga-view.fxml");
    }

    @FXML
    public void onTrocarTurnoClick() {
        limparBotoesAtivos();
        btnPermutas.getStyleClass().add("sidebar-btn-ativo");

        // mudarEcraCentro("/com/example/projeto2/dashboard/permutas-view.fxml");
    }

    @FXML
    public void onLogoutClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/login/login-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            // Usa o mainContainer para descobrir qual é a janela atual
            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Levi's Staff Portal - Autenticação");
        } catch (Exception e) {
            System.out.println("❌ Erro no logout: " + e.getMessage());
        }
    }

    // ==========================================
    // MOTOR DE NAVEGAÇÃO
    // ==========================================

    private void mudarEcraCentro(String caminhoFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent novoConteudo = loader.load();

            Object controller = loader.getController();
            if (controller instanceof HomeController) {
                ((HomeController) controller).setUtilizadorLogado(utilizadorLogado);
            }

            mainContainer.setCenter(novoConteudo);

        } catch (Exception e) {
            System.out.println("❌ Erro ao carregar o ecrã " + caminhoFxml + ": " + e.getMessage());
        }
    }

    private void limparBotoesAtivos() {
        btnDashboard.getStyleClass().remove("sidebar-btn-ativo");
        btnFolgas.getStyleClass().remove("sidebar-btn-ativo");
        btnPermutas.getStyleClass().remove("sidebar-btn-ativo");
    }
}