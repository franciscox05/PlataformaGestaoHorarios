package com.example.projeto2.Controller;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.Modules.DayOff;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PedirFolgaController {

    @FXML
    private DatePicker dpData;

    @FXML
    private ComboBox<String> cbTipo;

    @FXML
    private TextArea txtMotivo;

    private final DayOffBLL dayOffBLL;

    public PedirFolgaController(DayOffBLL dayOffBLL) {
        this.dayOffBLL = dayOffBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList(
                "F\u00e9rias",
                "Folgas",
                "Baixa"
        ));
    }

    @FXML
    public void onSubmitClick() {
        try {
            String tipoSelecionado = mapearTipoParaBaseDados(cbTipo.getValue());

            DayOff pedido = new DayOff();
            pedido.setIdUtilizador(1);
            pedido.setDataAusencia(dpData.getValue());
            pedido.setTipo(tipoSelecionado);
            pedido.setMotivo(txtMotivo.getText());

            dayOffBLL.registarPedidoFolga(pedido);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Pedido de folga registado com sucesso.");
            limparFormulario();
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Nao foi possivel guardar o pedido de folga.");
        }
    }

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Selecione um tipo de ausencia.");
        }

        return switch (tipoSelecionado) {
            case "F\u00e9rias" -> "ferias";
            case "Folgas" -> "folgas";
            case "Baixa" -> "baixa";
            default -> throw new IllegalArgumentException("Tipo de ausencia invalido.");
        };
    }

    private void limparFormulario() {
        dpData.setValue(null);
        cbTipo.setValue(null);
        txtMotivo.clear();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
