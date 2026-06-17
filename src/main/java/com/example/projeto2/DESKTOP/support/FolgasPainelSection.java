package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Services.PainelGerenteService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

import static com.example.projeto2.DESKTOP.support.PedidosFormatters.DATA_FORMATTER;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarData;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTexto;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTipoFolga;

public final class FolgasPainelSection {

    private final TableView<DayOff> tabela;
    private final TableColumn<DayOff, String> colColaborador;
    private final TableColumn<DayOff, String> colData;
    private final TableColumn<DayOff, String> colTipo;
    private final TableColumn<DayOff, String> colMotivo;
    private final Label feedback;
    private final Button btnAprovar;
    private final Button btnRejeitar;
    private final PainelGerenteService bll;
    private final PainelPedidosCoordinator coord;

    private Map<Integer, String> nomesColaboradores = Map.of();

    public FolgasPainelSection(TableView<DayOff> tabela,
                               TableColumn<DayOff, String> colColaborador,
                               TableColumn<DayOff, String> colData,
                               TableColumn<DayOff, String> colTipo,
                               TableColumn<DayOff, String> colMotivo,
                               Label feedback,
                               Button btnAprovar,
                               Button btnRejeitar,
                               PainelGerenteService bll,
                               PainelPedidosCoordinator coord) {
        this.tabela = tabela;
        this.colColaborador = colColaborador;
        this.colData = colData;
        this.colTipo = colTipo;
        this.colMotivo = colMotivo;
        this.feedback = feedback;
        this.btnAprovar = btnAprovar;
        this.btnRejeitar = btnRejeitar;
        this.bll = bll;
        this.coord = coord;
    }

    public void configurar() {
        configurarColunas();
        tabela.setPlaceholder(new Label("Não existem pedidos de folga pendentes nesta loja."));
        FeedbackHelper.esconder(feedback);
        btnAprovar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnRejeitar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnAprovar.setTooltip(new Tooltip("Aprovar o pedido de folga selecionado"));
        btnRejeitar.setTooltip(new Tooltip("Rejeitar o pedido de folga selecionado"));
    }

    public TableView<DayOff> getTabela() {
        return tabela;
    }

    public void mostrarDados(List<DayOff> folgas, Map<Integer, String> nomes) {
        this.nomesColaboradores = nomes != null ? nomes : Map.of();
        tabela.setItems(FXCollections.observableArrayList(folgas));
        tabela.refresh();
    }

    public void mostrarErro(String mensagem) {
        FeedbackHelper.mostrar(feedback, mensagem, false);
    }

    public void tratar(boolean aprovar) {
        try {
            DayOff pedido = tabela.getSelectionModel().getSelectedItem();
            if (pedido == null) {
                throw new IllegalArgumentException("Seleciona um pedido de folga primeiro.");
            }

            String nomeColab = nomesColaboradores.getOrDefault(
                    pedido.getIdUtilizador() != null ? pedido.getIdUtilizador().getId() : null,
                    "Colaborador");
            String data = pedido.getDataAusencia() != null
                    ? pedido.getDataAusencia().format(DATA_FORMATTER) : "-";
            String tipo = pedido.getTipo() != null ? pedido.getTipo() : "-";
            String motivo = pedido.getMotivo() != null && !pedido.getMotivo().isBlank()
                    ? "\nMotivo: " + pedido.getMotivo() : "";
            String detalhes = String.format("Colaborador: %s%nData: %s%nTipo: %s%s",
                    nomeColab, data, tipo, motivo);

            if (aprovar) {
                if (!DialogosHelper.confirmarAcao(
                        coord.obterJanela(), "Aprovar folga", "Confirmas a aprovação?", detalhes)) {
                    return;
                }
                bll.aprovarFolga(pedido.getIdDayoff(), coord.obterIdUtilizadorLogado());
                FeedbackHelper.mostrar(feedback, "Pedido de folga aprovado com sucesso.", true);
            } else {
                String motivoRejeicao = pedirMotivoRejeicao(detalhes);
                if (motivoRejeicao == null) return;
                bll.rejeitarFolga(pedido.getIdDayoff(), coord.obterIdUtilizadorLogado(), motivoRejeicao);
                FeedbackHelper.mostrar(feedback, "Pedido de folga rejeitado.", true);
            }

            tabela.getSelectionModel().clearSelection();
            coord.aposAcaoBemSucedida();
        } catch (IllegalArgumentException e) {
            FeedbackHelper.mostrar(feedback, e.getMessage(), false);
        } catch (Exception e) {
            FeedbackHelper.mostrar(feedback, "Não foi possível atualizar o pedido de folga.", false);
        }
    }

    /** Returns the typed reason, or null if the manager cancelled / left it blank. */
    private String pedirMotivoRejeicao(String detalhesPedido) {
        TextArea area = new TextArea();
        area.setPromptText("Escreve o motivo da rejeição (obrigatório)...");
        area.setWrapText(true);
        area.setPrefRowCount(3);
        area.setMaxWidth(400);

        Label lblDetalhe = new Label(detalhesPedido);
        lblDetalhe.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px;");
        lblDetalhe.setWrapText(true);

        VBox corpo = new VBox(8, lblDetalhe, area);
        corpo.setPrefWidth(420);

        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Rejeitar folga");
        dlg.setHeaderText("Indica o motivo da rejeição");
        dlg.getDialogPane().setContent(corpo);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (coord.obterJanela() != null) {
            dlg.initOwner(coord.obterJanela());
        }

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                String texto = area.getText();
                return (texto != null && !texto.isBlank()) ? texto.trim() : null;
            }
            return null;
        });

        return dlg.showAndWait().orElse(null);
    }

    private void configurarColunas() {
        colColaborador.setCellValueFactory(cellData -> {
            var utilizador = cellData.getValue().getIdUtilizador();
            Integer id = (utilizador != null) ? utilizador.getId() : null;
            return new SimpleStringProperty(nomesColaboradores.getOrDefault(
                    id, id != null ? "Colaborador #" + id : "Colaborador"));
        });

        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarData(cellData.getValue().getDataAusencia())));

        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipoFolga(cellData.getValue().getTipo())));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(tipo);
                badge.getStyleClass().addAll("badge-estado",
                        tipo.toLowerCase().contains("folga") ? "badge-folga" : "badge-pendente");
                setGraphic(badge);
                setText(null);
            }
        });

        colMotivo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getMotivo())));
    }
}
