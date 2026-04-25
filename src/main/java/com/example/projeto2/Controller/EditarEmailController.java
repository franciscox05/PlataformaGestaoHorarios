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
public class EditarEmailController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditarEmailController.class);

    @FXML
    private TextField txtEmailAtual;

    @FXML
    private TextField txtEmailNovo;

    @FXML
    private Label lblErro;

    private final PerfilBLL perfilBLL;
    private Utilizador utilizadorLogado;

    public EditarEmailController(PerfilBLL perfilBLL) {
        this.perfilBLL = perfilBLL;
    }

    @FXML
    public void initialize() {
        txtEmailNovo.textProperty().addListener((observavel, valorAntigo, valorNovo) -> esconderErro());
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;

        if (utilizadorLogado != null && utilizadorLogado.getEmail() != null && !utilizadorLogado.getEmail().isBlank()) {
            txtEmailAtual.setText(utilizadorLogado.getEmail());
        } else {
            txtEmailAtual.setText("Não definido");
        }
    }

    @FXML
    public void onGuardarClick(ActionEvent event) {
        if (!DialogosHelper.confirmarAcao(
                ((Node) event.getSource()).getScene().getWindow(),
                "Confirmar alteração",
                "Deseja guardar a alteração do email?",
                "O email da tua conta será atualizado e usado nos próximos acessos.",
                "Guardar"
        )) {
            return;
        }

        try {
            utilizadorLogado = perfilBLL.atualizarEmail(utilizadorLogado.getId(), txtEmailNovo.getText());
            fecharJanela(event);
        } catch (IllegalArgumentException e) {
            lblErro.setText(e.getMessage());
            lblErro.setVisible(true);
        } catch (Exception e) {
            LOGGER.error("Erro ao atualizar o email do utilizador {}.", utilizadorLogado != null ? utilizadorLogado.getId() : null, e);
            lblErro.setText("Não foi possível atualizar o email. Tenta novamente.");
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
