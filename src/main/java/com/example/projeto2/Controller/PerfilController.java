package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PerfilController {

    @FXML
    private VBox raizPerfil;

    @FXML
    private Label lblNomePerfil;

    @FXML
    private Label lblEmailPerfil;

    @FXML
    private Label lblTelemovelPerfil;

    @FXML
    private Label lblEstadoPerfil;

    @FXML
    private Label lblLojaAtual;

    @FXML
    private Label lblCargoAtual;

    @FXML
    private Label lblDataEntrada;

    @FXML
    private Label lblProximoTurno;

    @FXML
    private Label lblHorasMes;

    @FXML
    private Label lblFolgasPendentes;

    @FXML
    private Label lblFolgasAprovadas;

    @FXML
    private Label lblTurnosFuturos;

    private final PerfilBLL perfilBLL;
    private final ApplicationContext applicationContext;
    private Utilizador utilizadorLogado;

    public PerfilController(PerfilBLL perfilBLL, ApplicationContext applicationContext) {
        this.perfilBLL = perfilBLL;
        this.applicationContext = applicationContext;
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;

        if (utilizadorLogado == null) {
            preencherValoresEmFalta();
            return;
        }

        try {
            PerfilBLL.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizadorLogado);

            lblNomePerfil.setText(resumo.nome());
            lblEmailPerfil.setText(resumo.email());
            lblTelemovelPerfil.setText(resumo.telemovel());
            lblEstadoPerfil.setText(resumo.estado());
            lblLojaAtual.setText(resumo.lojaAtual());
            lblCargoAtual.setText(resumo.cargoAtual());
            lblDataEntrada.setText(resumo.dataEntrada());
            lblProximoTurno.setText(resumo.proximoTurno());
            lblHorasMes.setText(resumo.horasEsteMes());
            lblFolgasPendentes.setText(String.valueOf(resumo.pedidosPendentes()));
            lblFolgasAprovadas.setText(String.valueOf(resumo.pedidosAprovados()));
            lblTurnosFuturos.setText(String.valueOf(resumo.turnosFuturos()));
        } catch (IllegalArgumentException e) {
            preencherValoresEmFalta();
            lblNomePerfil.setText(utilizadorLogado.getNome());
            lblEstadoPerfil.setText("Dados indisponiveis");
            lblProximoTurno.setText(e.getMessage());
        }
    }

    @FXML
    public void onEditarEmailClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-email-view.fxml", "Editar Email");
    }

    @FXML
    public void onEditarNomeClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-nome-view.fxml", "Editar Nome");
    }

    @FXML
    public void onEditarTelemovelClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-telemovel-view.fxml", "Editar Telemovel");
    }

    @FXML
    public void onAlterarPasswordClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/alterar-password-view.fxml", "Alterar Password");
    }

    private void abrirModalComBlur(String caminhoFxml, String titulo) {
        try {
            raizPerfil.setEffect(new GaussianBlur(10));

            FXMLLoader loader = new FXMLLoader(getClass().getResource(caminhoFxml));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EditarNomeController nomeController) {
                nomeController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof EditarEmailController emailController) {
                emailController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof EditarTelemovelController telemovelController) {
                telemovelController.setUtilizadorLogado(this.utilizadorLogado);
            } else if (controller instanceof AlterarPasswordController passwordController) {
                passwordController.setUtilizadorLogado(this.utilizadorLogado);
            }

            Stage modalStage = new Stage();
            modalStage.setTitle(titulo);
            modalStage.setScene(new Scene(root));
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setResizable(false);
            modalStage.showAndWait();

            if (this.utilizadorLogado != null && this.utilizadorLogado.getId() != null) {
                this.utilizadorLogado = perfilBLL.obterUtilizadorPorId(this.utilizadorLogado.getId());
            }
            setUtilizadorLogado(this.utilizadorLogado);
        } catch (Exception e) {
            System.out.println("Erro ao abrir modal: " + e.getMessage());
        } finally {
            raizPerfil.setEffect(null);
        }
    }

    private void preencherValoresEmFalta() {
        lblNomePerfil.setText("-");
        lblEmailPerfil.setText("-");
        lblTelemovelPerfil.setText("-");
        lblEstadoPerfil.setText("-");
        lblLojaAtual.setText("-");
        lblCargoAtual.setText("-");
        lblDataEntrada.setText("-");
        lblProximoTurno.setText("-");
        lblHorasMes.setText("0h 0m");
        lblFolgasPendentes.setText("0");
        lblFolgasAprovadas.setText("0");
        lblTurnosFuturos.setText("0");
    }
}
