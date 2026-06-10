package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Services.PainelGerenteService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

import java.util.List;

import static com.example.projeto2.DESKTOP.support.PedidosFormatters.DATA_FORMATTER;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.LOCALE_PT;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarPeriodo;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTexto;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTipoPreferencia;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarVigencia;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.obterNomePreferencia;

public final class PreferenciasPainelSection {

    private final TableView<Preferencia> tabela;
    private final TableColumn<Preferencia, String> colColaborador;
    private final TableColumn<Preferencia, String> colTipo;
    private final TableColumn<Preferencia, String> colPeriodo;
    private final TableColumn<Preferencia, String> colPrioridade;
    private final TableColumn<Preferencia, String> colDescricao;
    private final TextArea txtDecisao;
    private final Label feedback;
    private final Button btnAprovar;
    private final Button btnRejeitar;
    private final PainelGerenteService bll;
    private final PainelPedidosCoordinator coord;

    public PreferenciasPainelSection(TableView<Preferencia> tabela,
                                     TableColumn<Preferencia, String> colColaborador,
                                     TableColumn<Preferencia, String> colTipo,
                                     TableColumn<Preferencia, String> colPeriodo,
                                     TableColumn<Preferencia, String> colPrioridade,
                                     TableColumn<Preferencia, String> colDescricao,
                                     TextArea txtDecisao,
                                     Label feedback,
                                     Button btnAprovar,
                                     Button btnRejeitar,
                                     PainelGerenteService bll,
                                     PainelPedidosCoordinator coord) {
        this.tabela = tabela;
        this.colColaborador = colColaborador;
        this.colTipo = colTipo;
        this.colPeriodo = colPeriodo;
        this.colPrioridade = colPrioridade;
        this.colDescricao = colDescricao;
        this.txtDecisao = txtDecisao;
        this.feedback = feedback;
        this.btnAprovar = btnAprovar;
        this.btnRejeitar = btnRejeitar;
        this.bll = bll;
        this.coord = coord;
    }

    public void configurar() {
        configurarColunas();
        tabela.setPlaceholder(new Label("Não existem preferências pendentes nesta loja."));
        FeedbackHelper.esconder(feedback);
        btnAprovar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnRejeitar.disableProperty().bind(Bindings.isNull(tabela.getSelectionModel().selectedItemProperty()));
        btnAprovar.setTooltip(new Tooltip("Aprovar a preferência selecionada"));
        btnRejeitar.setTooltip(new Tooltip("Rejeitar a preferência selecionada"));

        txtDecisao.textProperty().addListener((obs, oldValue, newValue) -> FeedbackHelper.esconder(feedback));
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            txtDecisao.clear();
            FeedbackHelper.esconder(feedback);
        });
    }

    public TableView<Preferencia> getTabela() {
        return tabela;
    }

    public void mostrarDados(List<Preferencia> preferencias) {
        tabela.setItems(FXCollections.observableArrayList(preferencias));
        tabela.refresh();
    }

    public void tratar(boolean aprovar) {
        try {
            Preferencia preferencia = tabela.getSelectionModel().getSelectedItem();
            if (preferencia == null) {
                throw new IllegalArgumentException("Seleciona uma preferência primeiro.");
            }

            String nomeColab = preferencia.getIdUtilizador() != null
                    && preferencia.getIdUtilizador().getNome() != null
                    ? preferencia.getIdUtilizador().getNome() : "Colaborador";
            String tipo = preferencia.getTipo() != null ? preferencia.getTipo() : "-";
            String dataInicio = preferencia.getDataInicio() != null
                    ? preferencia.getDataInicio().format(DATA_FORMATTER) : "-";
            String dataFim = preferencia.getDataFim() != null
                    ? preferencia.getDataFim().format(DATA_FORMATTER) : "-";
            String descricao = preferencia.getDescricao() != null && !preferencia.getDescricao().isBlank()
                    ? "\n\"" + preferencia.getDescricao() + "\"" : "";
            String detalhes = String.format("Colaborador: %s%nTipo: %s%nPeríodo: %s a %s%s",
                    nomeColab, tipo, dataInicio, dataFim, descricao);

            if (!DialogosHelper.confirmarAcao(
                    coord.obterJanela(),
                    aprovar ? "Aprovar preferência" : "Rejeitar preferência",
                    aprovar ? "Confirmas a aprovação desta preferência?" : "Confirmas a rejeição desta preferência?",
                    detalhes)) {
                return;
            }

            if (aprovar) {
                bll.aprovarPreferencia(preferencia.getId(), coord.obterIdUtilizadorLogado(), txtDecisao.getText());
                FeedbackHelper.mostrar(feedback, "Preferência aprovada com sucesso.", true);
            } else {
                bll.rejeitarPreferencia(preferencia.getId(), coord.obterIdUtilizadorLogado(), txtDecisao.getText());
                FeedbackHelper.mostrar(feedback, "Preferência rejeitada com sucesso.", true);
            }

            txtDecisao.clear();
            tabela.getSelectionModel().clearSelection();
            coord.aposAcaoBemSucedida();
        } catch (IllegalArgumentException e) {
            FeedbackHelper.mostrar(feedback, e.getMessage(), false);
        } catch (Exception e) {
            FeedbackHelper.mostrar(feedback, "Não foi possível atualizar a preferência.", false);
        }
    }

    private void configurarColunas() {
        colColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomePreferencia(cellData.getValue())));

        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipoPreferencia(cellData.getValue().getTipo())));
        colTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null || tipo.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(tipo.toUpperCase(LOCALE_PT));
                badge.getStyleClass().addAll("badge-estado",
                        (tipo.toLowerCase(LOCALE_PT).contains("folga") || tipo.toLowerCase(LOCALE_PT).contains("fer"))
                                ? "badge-folga"
                                : "badge-enviado");
                setGraphic(badge);
                setText(null);
            }
        });

        colPeriodo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarPeriodo(cellData.getValue().getDataInicio(), cellData.getValue().getDataFim())));

        colPrioridade.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarVigencia(cellData.getValue())));
        colPrioridade.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String vigencia, boolean empty) {
                super.updateItem(vigencia, empty);
                if (empty || vigencia == null || vigencia.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(vigencia.toUpperCase(LOCALE_PT));
                badge.getStyleClass().add("badge-estado");
                String normalizado = vigencia.toLowerCase(LOCALE_PT);
                if (normalizado.contains("permanente")) {
                    badge.getStyleClass().add("badge-aprovado");
                } else if (normalizado.contains("tempor")) {
                    badge.getStyleClass().add("badge-pendente");
                } else {
                    badge.getStyleClass().add("badge-rascunho");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getDescricao())));
    }
}
