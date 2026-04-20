package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class AlterarPasswordController {

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
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Alteracao");
        confirmacao.setHeaderText(null);
        confirmacao.setGraphic(null);
        confirmacao.setContentText("Tens a certeza que queres alterar a tua password?");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacao.getButtonTypes().setAll(btnGuardar, btnCancelar);

        try {
            DialogPane dialogPane = confirmacao.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
            dialogPane.getStyleClass().add("alerta-personalizado");

            Button nodeGuardar = (Button) dialogPane.lookupButton(btnGuardar);
            nodeGuardar.getStyleClass().add("botao-acao");

            Button nodeCancelar = (Button) dialogPane.lookupButton(btnCancelar);
            nodeCancelar.getStyleClass().add("botao-secundario");
        } catch (Exception ignored) {
        }

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isEmpty() || resultado.get() != btnGuardar) {
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
            lblErro.setText("Erro ao guardar na base de dados.");
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
