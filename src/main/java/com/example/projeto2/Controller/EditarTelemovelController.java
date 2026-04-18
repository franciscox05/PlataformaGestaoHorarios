package com.example.projeto2.Controller;

import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.UtilizadorRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Scope("prototype")
public class EditarTelemovelController {

    @FXML private TextField txtTelemovelAtual; // O novo campo bloqueado
    @FXML private TextField txtTelemovel;
    @FXML private Label lblErro;

    private final UtilizadorRepository utilizadorRepository;
    private Utilizador utilizadorLogado;

    public EditarTelemovelController(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @FXML
    public void initialize() {
        // Validação em Tempo Real: Bloquear letras e limitar a 9 números!
        txtTelemovel.textProperty().addListener((observavel, valorAntigo, valorNovo) -> {
            if (!valorNovo.matches("\\d*")) {
                // Se o utilizador escrever algo que não seja um número (digito), removemos:
                txtTelemovel.setText(valorNovo.replaceAll("[^\\d]", ""));
            }
            // Depois de garantir que só tem números, verificamos o limite de tamanho:
            if (txtTelemovel.getText().length() > 9) {
                txtTelemovel.setText(txtTelemovel.getText().substring(0, 9));
            }
        });
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;

        // Preenche a caixa bloqueada com o número atual
        if (utilizador.getTelemovel() != null && !utilizador.getTelemovel().isEmpty()) {
            txtTelemovelAtual.setText(utilizador.getTelemovel());
        } else {
            txtTelemovelAtual.setText("Não definido");
        }
    }

    @FXML
    public void onGuardarClick(ActionEvent event) {
        String novoTelemovel = txtTelemovel.getText();

        // 1. Validação Tamanho
        if (novoTelemovel.length() != 9) {
            lblErro.setText("O número tem de ter exatamente 9 dígitos.");
            lblErro.setVisible(true);
            return;
        }

        // 2. Validação Número Igual ao Atual
        if (novoTelemovel.equals(txtTelemovelAtual.getText())) {
            lblErro.setText("O novo número não pode ser igual ao atual.");
            lblErro.setVisible(true);
            return;
        }

        // 3. Validação Número Já Existente
        if (utilizadorRepository.existsByTelemovel(novoTelemovel)) {
            lblErro.setText("Este número já está registado noutra conta.");
            lblErro.setVisible(true);
            return;
        }

        // 4. Pergunta de Confirmação Nativa do JavaFX
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmar Alteração");
        confirmacao.setHeaderText(null); // Remove o cabeçalho cinzento!
        confirmacao.setGraphic(null);    // Remove o ícone azul do Windows!
        confirmacao.setContentText("Tens a certeza que queres substituir o número "
                + txtTelemovelAtual.getText() + " por " + novoTelemovel + "?");

        // Criar botões traduzidos personalizados
        javafx.scene.control.ButtonType btnGuardar = new javafx.scene.control.ButtonType("Guardar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        javafx.scene.control.ButtonType btnCancelar = new javafx.scene.control.ButtonType("Cancelar", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);

        // Substituir os botões padrão pelos nossos
        confirmacao.getButtonTypes().setAll(btnGuardar, btnCancelar);

        // Aplicar estilos
        try {
            javafx.scene.control.DialogPane dialogPane = confirmacao.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
            dialogPane.getStyleClass().add("alerta-personalizado");

            // Extrair os botões e aplicar as tuas classes CSS
            javafx.scene.control.Button nodeGuardar = (javafx.scene.control.Button) dialogPane.lookupButton(btnGuardar);
            nodeGuardar.getStyleClass().add("botao-acao"); // Fica Vermelho

            javafx.scene.control.Button nodeCancelar = (javafx.scene.control.Button) dialogPane.lookupButton(btnCancelar);
            nodeCancelar.getStyleClass().add("botao-secundario"); // Fica Cinzento

        } catch (Exception e) {
            System.out.println("Aviso: Não foi possível aplicar CSS ao Alerta.");
        }

        // Fica à espera que o utilizador clique
        Optional<javafx.scene.control.ButtonType> resultado = confirmacao.showAndWait();

        // Verifica se clicou no botão "Guardar"
        if (resultado.isPresent() && resultado.get() == btnGuardar) {
            try {
                // 5. O utilizador clicar em Guardar, guardar na BD!
                utilizadorLogado.setTelemovel(novoTelemovel);
                utilizadorRepository.save(utilizadorLogado);

                fecharJanela(event);

            } catch (Exception e) {
                lblErro.setText("Erro ao guardar na base de dados.");
                lblErro.setVisible(true);
            }
        }
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