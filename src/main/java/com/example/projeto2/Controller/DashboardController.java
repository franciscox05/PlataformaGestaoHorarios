package com.example.projeto2.Controller;

import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    @FXML
    private BorderPane mainContainer;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnFolgas;

    @FXML
    private Button btnPermutas;

    @FXML
    private Button btnPerfil;

    @FXML
    private Button btnPreferencias;

    private final ApplicationContext applicationContext;
    private Utilizador utilizadorLogado;

    public DashboardController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
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
    public void onLogoutClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/projeto2/login/login-view.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Stage stage = (Stage) mainContainer.getScene().getWindow();
            stage.setScene(new Scene(root, 800, 600));
            stage.setTitle("Levi's Staff Portal - Autenticacao");
        } catch (Exception e) {
            System.out.println("Erro no logout: " + e.getMessage());
        }
    }

    private void mudarEcraCentro(String caminhoFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent novoConteudo = loader.load();

            Object controller = loader.getController();

            if (controller instanceof HomeController homeController) {
                homeController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PermutasController permutasController) {
                permutasController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PedirFolgaController pedirFolgaController) {
                pedirFolgaController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PerfilController perfilController) {
                perfilController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof PreferenciasController preferenciasController) {
                preferenciasController.setUtilizadorLogado(utilizadorLogado);
            }

            mainContainer.setCenter(novoConteudo);
        } catch (Exception e) {
            System.out.println("Erro ao carregar o ecra: " + e.getMessage());
            e.printStackTrace(); // <--- ADICIONA ESTA LINHA!
        }
    }

    private void limparBotoesAtivos() {
        btnDashboard.getStyleClass().remove("sidebar-btn-ativo");
        btnFolgas.getStyleClass().remove("sidebar-btn-ativo");
        btnPermutas.getStyleClass().remove("sidebar-btn-ativo");
        btnPerfil.getStyleClass().remove("sidebar-btn-ativo");
        btnPreferencias.getStyleClass().remove("sidebar-btn-ativo");
    }
}
