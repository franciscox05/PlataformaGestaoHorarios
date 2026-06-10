package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Services.PainelGerenteService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;

import java.util.List;

import static com.example.projeto2.DESKTOP.support.PedidosFormatters.DATA_FORMATTER;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarDataHora;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTurno;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.obterNomePermuta;

public final class PermutasPainelSection {

    private final TableView<Permuta> tabela;
    private final TableColumn<Permuta, String> colColaborador;
    private final TableColumn<Permuta, String> colPedido;
    private final TableColumn<Permuta, String> colOrigem;
    private final TableColumn<Permuta, String> colDestino;
    private final Label feedback;
    private final Button btnAprovar;
    private final Button btnRejeitar;
    private final PainelGerenteService bll;
    private final PainelPedidosCoordinator coord;

    public PermutasPainelSection(TableView<Permuta> tabela,
                                 TableColumn<Permuta, String> colColaborador,
                                 TableColumn<Permuta, String> colPedido,
                                 TableColumn<Permuta, String> colOrigem,
                                 TableColumn<Permuta, String> colDestino,
                                 Label feedback,
                                 Button btnAprovar,
                                 Button btnRejeitar,
                                 PainelGerenteService bll,
                                 PainelPedidosCoordinator coord) {
        this.tabela = tabela;
        this.colColaborador = colColaborador;
        this.colPedido = colPedido;
        this.colOrigem = colOrigem;
        this.colDestino = colDestino;
        this.feedback = feedback;
        this.btnAprovar = btnAprovar;
        this.btnRejeitar = btnRejeitar;
        this.bll = bll;
        this.coord = coord;
    }

    public void configurar() {
        configurarColunas();
        tabela.setPlaceholder(new Label("Não existem pedidos de permuta pendentes nesta loja."));
        FeedbackHelper.esconder(feedback);
        btnAprovar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnRejeitar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnAprovar.setTooltip(new Tooltip("Aprovar a permuta de turno selecionada"));
        btnRejeitar.setTooltip(new Tooltip("Rejeitar a permuta de turno selecionada"));
    }

    public TableView<Permuta> getTabela() {
        return tabela;
    }

    public void mostrarDados(List<Permuta> permutas) {
        tabela.setItems(FXCollections.observableArrayList(permutas));
        tabela.refresh();
    }

    public void tratar(boolean aprovar) {
        try {
            Permuta pedido = tabela.getSelectionModel().getSelectedItem();
            if (pedido == null) {
                throw new IllegalArgumentException("Seleciona um pedido de permuta primeiro.");
            }

            String nomeOrigem = pedido.getIdHorarioOrigem() != null
                    && pedido.getIdHorarioOrigem().getIdLojautilizador() != null
                    && pedido.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() != null
                    ? pedido.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome()
                    : "Colaborador A";
            String nomeDestino = pedido.getIdHorarioDestino() != null
                    && pedido.getIdHorarioDestino().getIdLojautilizador() != null
                    && pedido.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador() != null
                    ? pedido.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador().getNome()
                    : "Colaborador B";
            String dataOrigem = pedido.getIdHorarioOrigem() != null
                    && pedido.getIdHorarioOrigem().getDataTurno() != null
                    ? pedido.getIdHorarioOrigem().getDataTurno().format(DATA_FORMATTER) : "-";
            String dataDestino = pedido.getIdHorarioDestino() != null
                    && pedido.getIdHorarioDestino().getDataTurno() != null
                    ? pedido.getIdHorarioDestino().getDataTurno().format(DATA_FORMATTER) : "-";
            String detalhes = String.format("%s (%s)  ↔  %s (%s)", nomeOrigem, dataOrigem, nomeDestino, dataDestino);

            if (!DialogosHelper.confirmarAcao(
                    coord.obterJanela(),
                    aprovar ? "Aprovar permuta" : "Rejeitar permuta",
                    aprovar ? "Confirmas a aprovação desta troca de turno?" : "Confirmas a rejeição desta troca de turno?",
                    detalhes)) {
                return;
            }

            if (aprovar) {
                bll.aprovarPermuta(pedido.getId(), coord.obterIdUtilizadorLogado());
                FeedbackHelper.mostrar(feedback, "Pedido de permuta aprovado com sucesso.", true);
            } else {
                bll.rejeitarPermuta(pedido.getId(), coord.obterIdUtilizadorLogado());
                FeedbackHelper.mostrar(feedback, "Pedido de permuta rejeitado com sucesso.", true);
            }

            tabela.getSelectionModel().clearSelection();
            coord.aposAcaoBemSucedida();
        } catch (IllegalArgumentException e) {
            FeedbackHelper.mostrar(feedback, e.getMessage(), false);
        } catch (Exception e) {
            FeedbackHelper.mostrar(feedback, "Não foi possível atualizar o pedido de permuta.", false);
        }
    }

    private void configurarColunas() {
        colColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomePermuta(cellData.getValue())));

        colPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataHora(cellData.getValue().getDataPedido())));

        colOrigem.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurno(cellData.getValue().getIdHorarioOrigem(), false)));

        colDestino.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurno(cellData.getValue().getIdHorarioDestino(), true)));
    }
}
