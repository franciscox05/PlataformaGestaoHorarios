package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class EditarTelemovelController {

    @FXML
    private TextField txtTelemovelAtual;

    @FXML
    private TextField txtTelemovel;

    @FXML
    private Label lblErro;

    private final PerfilBLL perfilBLL;
    private Utilizador utilizadorLogado;

    public EditarTelemovelController(PerfilBLL perfilBLL) {
        this.perfilBLL = perfilBLL;
    }

    @FXML
    public void initialize() {
        txtTelemovel.textProperty().addListener((observavel, valorAntigo, valorNovo) -> {
            esconderErro();

            if (!valorNovo.matches("\\d*")) {
                txtTelemovel.setText(valorNovo.replaceAll("[^\\d]", ""));
            }

            if (txtTelemovel.getText().length() > 9) {
                txtTelemovel.setText(txtTelemovel.getText().substring(0, 9));
            }
        });
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;

        if (utilizadorLogado != null && utilizadorLogado.getTelemovel() != null && !utilizadorLogado.getTelemovel().isBlank()) {
            txtTelemovelAtual.setText(utilizadorLogado.getTelemovel());
        } else {
            txtTelemovelAtual.setText("Nao definido");
        }
    }

    @FXML
    public void onGuardarClick(ActionEvent event) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Alteracao");
        confirmacao.setHeaderText(null);
        confirmacao.setGraphic(null);
        confirmacao.setContentText("Tens a certeza que queres atualizar o teu telemovel?");

        ButtonType btnGuardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmacao.getButtonTypes().setAll(btnGuardar, btnCancelar);

        try {
            DialogPane dialogPane = confirmacao.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
            dialogPane.getStyleClass().add("alerta-personalizado");

            javafx.scene.control.Button nodeGuardar = (javafx.scene.control.Button) dialogPane.lookupButton(btnGuardar);
            nodeGuardar.getStyleClass().add("botao-acao");

            javafx.scene.control.Button nodeCancelar = (javafx.scene.control.Button) dialogPane.lookupButton(btnCancelar);
            nodeCancelar.getStyleClass().add("botao-secundario");
        } catch (Exception e) {
            System.out.println("Aviso: Nao foi possivel aplicar CSS ao Alerta.");
        }

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isEmpty() || resultado.get() != btnGuardar) {
            return;
        }

        try {
            utilizadorLogado = perfilBLL.atualizarTelemovel(utilizadorLogado.getId(), txtTelemovel.getText());
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
