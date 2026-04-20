package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GestaoLojaBLL;
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
    private Button btnGestaoLoja;

    @FXML
    private Button btnGestaoFuncionarios;

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
    private Utilizador utilizadorLogado;

    public DashboardController(ApplicationContext applicationContext, GestaoLojaBLL gestaoLojaBLL) {
        this.applicationContext = applicationContext;
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        configurarPermissoesMenu();
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
            } else if (controller instanceof GestaoLojaController gestaoLojaController) {
                gestaoLojaController.setUtilizadorLogado(utilizadorLogado);
            } else if (controller instanceof GestaoFuncionariosController gestaoFuncionariosController) {
                gestaoFuncionariosController.setUtilizadorLogado(utilizadorLogado);
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
        } catch (Exception e) {
            System.out.println("Erro ao carregar o ecra: " + e.getMessage());
        }
    }

    private void limparBotoesAtivos() {
        btnDashboard.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoLoja.getStyleClass().remove("sidebar-btn-ativo");
        btnGestaoFuncionarios.getStyleClass().remove("sidebar-btn-ativo");
        btnFolgas.getStyleClass().remove("sidebar-btn-ativo");
        btnPermutas.getStyleClass().remove("sidebar-btn-ativo");
        btnPerfil.getStyleClass().remove("sidebar-btn-ativo");
        btnPreferencias.getStyleClass().remove("sidebar-btn-ativo");
        btnRelatorios.getStyleClass().remove("sidebar-btn-ativo");
    }

    private void configurarPermissoesMenu() {
        boolean podeGerirLoja = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());
        btnGestaoLoja.setVisible(podeGerirLoja);
        btnGestaoLoja.setManaged(podeGerirLoja);
        btnRelatorios.setVisible(podeGerirLoja);
        btnRelatorios.setManaged(podeGerirLoja);
        btnGestaoFuncionarios.setVisible(podeGerirLoja);
        btnGestaoFuncionarios.setManaged(podeGerirLoja);
    }
}
