package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.Utilizador;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class EditarTelemovelController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditarTelemovelController.class);

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
            txtTelemovelAtual.setText("Não definido");
        }
    }

    @FXML
    public void onGuardarClick(ActionEvent event) {
        if (!DialogosHelper.confirmarAcao(
                ((Node) event.getSource()).getScene().getWindow(),
                "Confirmar alteração",
                "Deseja guardar a alteração do telemóvel?",
                "O teu contacto ficará atualizado para futuras comunicações internas.",
                "Guardar"
        )) {
            return;
        }

        try {
            utilizadorLogado = perfilBLL.atualizarTelemovel(utilizadorLogado.getId(), txtTelemovel.getText());
            fecharJanela(event);
        } catch (IllegalArgumentException e) {
            lblErro.setText(e.getMessage());
            lblErro.setVisible(true);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar o telemóvel do utilizador {}.", utilizadorLogado != null ? utilizadorLogado.getId() : null, e);
            lblErro.setText("Não foi possível atualizar o telemóvel. Tenta novamente.");
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
