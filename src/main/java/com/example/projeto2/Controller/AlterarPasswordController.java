package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AlterarPasswordController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlterarPasswordController.class);

    @FXML
    private PasswordField txtPasswordAtual;

    @FXML
    private TextField txtPasswordAtualVisivel;

    @FXML
    private PasswordField txtNovaPassword;

    @FXML
    private TextField txtNovaPasswordVisivel;

    @FXML
    private PasswordField txtConfirmarPassword;

    @FXML
    private TextField txtConfirmarPasswordVisivel;

    @FXML
    private ToggleButton btnVerPassAtual;

    @FXML
    private ImageView imgPassAtual;

    @FXML
    private ToggleButton btnVerNovaPass;

    @FXML
    private ImageView imgNovaPass;

    @FXML
    private ToggleButton btnVerConfirmarPass;

    @FXML
    private ImageView imgConfirmarPass;

    @FXML
    private CheckBox chkMostrarTodas;

    @FXML
    private Label lblErro;

    private final Image imgOlhoAberto = new Image(getClass().getResourceAsStream("/com/example/projeto2/imagens/login/olho-aberto.png"));
    private final Image imgOlhoFechado = new Image(getClass().getResourceAsStream("/com/example/projeto2/imagens/login/olho-fechado.png"));

    private final PerfilBLL perfilBLL;
    private Utilizador utilizadorLogado;

    public AlterarPasswordController(PerfilBLL perfilBLL) {
        this.perfilBLL = perfilBLL;
    }

    @FXML
    public void initialize() {
        txtPasswordAtualVisivel.textProperty().bindBidirectional(txtPasswordAtual.textProperty());
        txtNovaPasswordVisivel.textProperty().bindBidirectional(txtNovaPassword.textProperty());
        txtConfirmarPasswordVisivel.textProperty().bindBidirectional(txtConfirmarPassword.textProperty());

        txtPasswordAtual.textProperty().addListener((obs, antigo, novo) -> esconderErro());
        txtNovaPassword.textProperty().addListener((obs, antigo, novo) -> esconderErro());
        txtConfirmarPassword.textProperty().addListener((obs, antigo, novo) -> esconderErro());

        btnVerPassAtual.selectedProperty().addListener((obs, antigo, novoVisivel) -> {
            txtPasswordAtual.setVisible(!novoVisivel);
            txtPasswordAtualVisivel.setVisible(novoVisivel);
            imgPassAtual.setImage(novoVisivel ? imgOlhoFechado : imgOlhoAberto);
        });

        btnVerNovaPass.selectedProperty().addListener((obs, antigo, novoVisivel) -> {
            txtNovaPassword.setVisible(!novoVisivel);
            txtNovaPasswordVisivel.setVisible(novoVisivel);
            imgNovaPass.setImage(novoVisivel ? imgOlhoFechado : imgOlhoAberto);
        });

        btnVerConfirmarPass.selectedProperty().addListener((obs, antigo, novoVisivel) -> {
            txtConfirmarPassword.setVisible(!novoVisivel);
            txtConfirmarPasswordVisivel.setVisible(novoVisivel);
            imgConfirmarPass.setImage(novoVisivel ? imgOlhoFechado : imgOlhoAberto);
        });

        chkMostrarTodas.setOnAction(event -> {
            boolean estado = chkMostrarTodas.isSelected();
            btnVerPassAtual.setSelected(estado);
            btnVerNovaPass.setSelected(estado);
            btnVerConfirmarPass.setSelected(estado);
        });

        ChangeListener<Boolean> validadorOlhos = (obs, antigo, novo) -> {
            boolean todosAbertos = btnVerPassAtual.isSelected()
                    && btnVerNovaPass.isSelected()
                    && btnVerConfirmarPass.isSelected();

            chkMostrarTodas.setSelected(todosAbertos);
        };

        btnVerPassAtual.selectedProperty().addListener(validadorOlhos);
        btnVerNovaPass.selectedProperty().addListener(validadorOlhos);
        btnVerConfirmarPass.selectedProperty().addListener(validadorOlhos);
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
    }

    @FXML
    public void onGuardarClick(ActionEvent event) {
        if (!DialogosHelper.confirmarAcao(
                ((Node) event.getSource()).getScene().getWindow(),
                "Confirmar alteração",
                "Deseja guardar a nova palavra-passe?",
                "A palavra-passe atual será substituída e usada nos próximos acessos.",
                "Guardar"
        )) {
            return;
        }

        try {
            utilizadorLogado = perfilBLL.atualizarPassword(
                    utilizadorLogado.getId(),
                    txtPasswordAtual.getText(),
                    txtNovaPassword.getText(),
                    txtConfirmarPassword.getText()
            );
            fecharJanela(event);
        } catch (IllegalArgumentException e) {
            lblErro.setText(e.getMessage());
            lblErro.setVisible(true);
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao atualizar a palavra-passe.", e);
            lblErro.setText("Não foi possível atualizar a palavra-passe. Tenta novamente.");
            lblErro.setVisible(true);
        }
    }

    @FXML
    public void onCancelarClick(ActionEvent event) {
        fecharJanela(event);
    }

    private void esconderErro() {
        lblErro.setVisible(false);
        lblErro.setText("");
    }

    private void fecharJanela(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
