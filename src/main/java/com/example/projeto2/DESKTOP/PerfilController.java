package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PerfilController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerfilController.class);

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

    @FXML
    private Label lblPerfilAvatar;

    private final PerfilService perfilBLL;
    private final ApplicationContext applicationContext;
    private Utilizador utilizadorLogado;

    public PerfilController(PerfilService perfilBLL, ApplicationContext applicationContext) {
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
            PerfilService.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizadorLogado);

            if (lblPerfilAvatar != null && resumo.nome() != null && !resumo.nome().isBlank()) {
                lblPerfilAvatar.setText(String.valueOf(resumo.nome().charAt(0)).toUpperCase());
            }
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
            lblEstadoPerfil.setText("Dados indisponíveis");
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
        abrirModalComBlur("/com/example/projeto2/dashboard/editar-telemovel-view.fxml", "Editar Telemóvel");
    }

    @FXML
    public void onAlterarPasswordClick() {
        abrirModalComBlur("/com/example/projeto2/dashboard/alterar-password-view.fxml", "Alterar Palavra-passe");
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
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            modalStage.setScene(scene);
            Window owner = obterJanelaAtual();
            if (owner != null) {
                modalStage.initOwner(owner);
                modalStage.initModality(Modality.WINDOW_MODAL);
            } else {
                modalStage.initModality(Modality.APPLICATION_MODAL);
            }
            modalStage.initStyle(StageStyle.TRANSPARENT);
            modalStage.setResizable(false);
            modalStage.showAndWait();

            if (this.utilizadorLogado != null && this.utilizadorLogado.getId() != null) {
                this.utilizadorLogado = perfilBLL.obterUtilizadorPorId(this.utilizadorLogado.getId());
            }
            setUtilizadorLogado(this.utilizadorLogado);
        } catch (Exception e) {
            LOGGER.error("Erro ao abrir o modal {}.", caminhoFxml, e);
            mostrarErro(
                    "Não foi possível abrir esta janela.",
                    "Tenta novamente. Se o problema persistir, volta a abrir o perfil."
            );
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

    private void mostrarErro(String titulo, String mensagem) {
        DialogosHelper.mostrarErro(obterJanelaAtual(), "Erro", titulo, mensagem);
    }

    private Window obterJanelaAtual() {
        if (raizPerfil == null || raizPerfil.getScene() == null) {
            return null;
        }
        return raizPerfil.getScene().getWindow();
    }
}
