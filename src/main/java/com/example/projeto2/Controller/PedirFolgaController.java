package com.example.projeto2.Controller;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class PedirFolgaController {

    @FXML
    private DatePicker dpData;

    @FXML
    private ComboBox<String> cbTipo;

    @FXML
    private TextArea txtMotivo;

    @FXML
    private TableView<DayOff> tabelaPedidos;

    @FXML
    private TableColumn<DayOff, String> colDataPedido;

    @FXML
    private TableColumn<DayOff, String> colTipoPedido;

    @FXML
    private TableColumn<DayOff, String> colMotivoPedido;

    @FXML
    private TableColumn<DayOff, String> colEstadoPedido;

    private final DayOffBLL dayOffBLL;
    private Utilizador utilizadorLogado;

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

        configurarTabela();
        tabelaPedidos.setPlaceholder(new Label("Ainda nao tens pedidos de folga registados."));
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarHistoricoPedidos();
    }

    @FXML
    public void onSubmitClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            String tipoSelecionado = mapearTipoParaBaseDados(cbTipo.getValue());

            DayOff pedido = new DayOff();
            pedido.setIdUtilizador(utilizadorLogado.getId());
            pedido.setDataAusencia(dpData.getValue());
            pedido.setTipo(tipoSelecionado);
            pedido.setMotivo(txtMotivo.getText());

            dayOffBLL.registarPedidoFolga(pedido);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Sucesso", "Pedido de folga registado com sucesso.");
            limparFormulario();
            carregarHistoricoPedidos();
        } catch (IllegalArgumentException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", e.getMessage());
        } catch (Exception e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Erro", "Nao foi possivel guardar o pedido de folga.");
        }
    }

    private void configurarTabela() {
        colDataPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDataAusencia())));

        colTipoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colMotivoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMotivo(cellData.getValue().getMotivo())));

        colEstadoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));
    }

    private void carregarHistoricoPedidos() {
        if (utilizadorLogado == null) {
            tabelaPedidos.setItems(FXCollections.observableArrayList());
            return;
        }

        List<DayOff> pedidos = dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId());
        tabelaPedidos.setItems(FXCollections.observableArrayList(pedidos));
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

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }

        return switch (tipo.toLowerCase()) {
            case "ferias" -> "F\u00e9rias";
            case "folgas" -> "Folgas";
            case "baixa" -> "Baixa";
            default -> tipo;
        };
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }

        return switch (estado.toLowerCase()) {
            case "pendente" -> "Pendente";
            case "aprovado" -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            default -> estado;
        };
    }

    private String formatarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "-";
        }
        return motivo;
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
