package com.example.projeto2.Controller;

import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.UtilizadorRepository;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class AlterarPasswordController {

    // Campos de Texto
    @FXML private PasswordField txtPasswordAtual;
    @FXML private TextField txtPasswordAtualVisivel;
    @FXML private PasswordField txtNovaPassword;
    @FXML private TextField txtNovaPasswordVisivel;
    @FXML private PasswordField txtConfirmarPassword;
    @FXML private TextField txtConfirmarPasswordVisivel;

    // Botões, Imagens e Checkbox
    @FXML private ToggleButton btnVerPassAtual;
    @FXML private ImageView imgPassAtual;
    @FXML private ToggleButton btnVerNovaPass;
    @FXML private ImageView imgNovaPass;
    @FXML private ToggleButton btnVerConfirmarPass;
    @FXML private ImageView imgConfirmarPass;
    @FXML private CheckBox chkMostrarTodas;

    @FXML private Label lblErro;

    private final Image imgOlhoAberto = new Image(getClass().getResourceAsStream("/com/example/projeto2/imagens/login/olho-aberto.png"));
    private final Image imgOlhoFechado = new Image(getClass().getResourceAsStream("/com/example/projeto2/imagens/login/olho-fechado.png"));

    private final UtilizadorRepository utilizadorRepository;
    private Utilizador utilizadorLogado;

    public AlterarPasswordController(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @FXML
    public void initialize() {
        txtPasswordAtualVisivel.textProperty().bindBidirectional(txtPasswordAtual.textProperty());
        txtNovaPasswordVisivel.textProperty().bindBidirectional(txtNovaPassword.textProperty());
        txtConfirmarPasswordVisivel.textProperty().bindBidirectional(txtConfirmarPassword.textProperty());

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

        // --- A SOLUÇÃO DEFINITIVA ---

        // 1. Ação Humana: Quando o utilizador clica fisicamente na CheckBox
        chkMostrarTodas.setOnAction(event -> {
            boolean estado = chkMostrarTodas.isSelected();
            btnVerPassAtual.setSelected(estado);
            btnVerNovaPass.setSelected(estado);
            btnVerConfirmarPass.setSelected(estado);
        });

        // 2. Ação Automática: Quando o utilizador clica nos botões individuais
        ChangeListener<Boolean> validadorOlhos = (obs, antigo, novo) -> {
            boolean todosAbertos = btnVerPassAtual.isSelected() &&
                    btnVerNovaPass.isSelected() &&
                    btnVerConfirmarPass.isSelected();

            // Isto atualiza a CheckBox visualmente, mas NÃO dispara o "setOnAction" acima. Zero loops!
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
        String passAtual = txtPasswordAtual.getText();
        String novaPass = txtNovaPassword.getText();
        String confirmarPass = txtConfirmarPassword.getText();

        if (passAtual.isEmpty() || novaPass.isEmpty() || confirmarPass.isEmpty()) {
            mostrarErro("Por favor, preenche todos os campos.");
            return;
        }

        if (novaPass.length() < 6) {
            mostrarErro("A nova password deve ter pelo menos 6 caracteres.");
            return;
        }

        if (!novaPass.equals(confirmarPass)) {
            mostrarErro("As novas passwords não coincidem.");
            return;
        }

        if (!passAtual.equals(utilizadorLogado.getPasswordHash())) {
            mostrarErro("A password atual está incorreta.");
            return;
        }

        if (novaPass.equals(passAtual)) {
            mostrarErro("A nova password tem de ser diferente da atual.");
            return;
        }

        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Alteração");
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
        } catch (Exception e) {
            System.out.println("Aviso: CSS do Alerta não carregado.");
        }

        Optional<ButtonType> resultado = confirmacao.showAndWait();

        if (resultado.isPresent() && resultado.get() == btnGuardar) {
            try {
                utilizadorLogado.setPasswordHash(novaPass);
                utilizadorRepository.save(utilizadorLogado);
                fecharJanela(event);
            } catch (Exception e) {
                mostrarErro("Erro ao guardar na base de dados.");
            }
        }
    }

    private void mostrarErro(String mensagem) {
        lblErro.setText(mensagem);
        lblErro.setVisible(true);
    }

    @FXML
    public void onCancelarClick(ActionEvent event) {
        fecharJanela(event);
    }

    private void fecharJanela(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}