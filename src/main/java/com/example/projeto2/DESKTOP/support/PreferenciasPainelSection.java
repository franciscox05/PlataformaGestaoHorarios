package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Services.PainelGerenteService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
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
    private final TableColumn<Preferencia, String> colDescricao;
    private final TextArea txtDecisao;
    private final Label feedback;
    private final Button btnAprovar;
    private final Button btnRejeitar;
    private final PainelGerenteService bll;
    private final PainelPedidosCoordinator coord;
    private final PaginadorTabela<Preferencia> paginador;

    public PreferenciasPainelSection(TableView<Preferencia> tabela,
                                     TableColumn<Preferencia, String> colColaborador,
                                     TableColumn<Preferencia, String> colTipo,
                                     TableColumn<Preferencia, String> colDescricao,
                                     TextArea txtDecisao,
                                     Label feedback,
                                     Button btnAprovar,
                                     Button btnRejeitar,
                                     PainelGerenteService bll,
                                     PainelPedidosCoordinator coord,
                                     Label lblPagina,
                                     Button btnPaginaAnterior,
                                     Button btnPaginaProxima) {
        this.tabela = tabela;
        this.colColaborador = colColaborador;
        this.colTipo = colTipo;
        this.colDescricao = colDescricao;
        this.txtDecisao = txtDecisao;
        this.feedback = feedback;
        this.btnAprovar = btnAprovar;
        this.btnRejeitar = btnRejeitar;
        this.bll = bll;
        this.coord = coord;
        this.paginador = new PaginadorTabela<>(tabela, lblPagina, btnPaginaAnterior, btnPaginaProxima);
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
        paginador.definirItens(preferencias);
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
            tabela.requestFocus();
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
                Label badge = new Label(tipo);
                badge.getStyleClass().addAll("badge-estado",
                        (tipo.toLowerCase(LOCALE_PT).contains("folga") || tipo.toLowerCase(LOCALE_PT).contains("fer"))
                                ? "badge-folga"
                                : "badge-enviado");
                setGraphic(badge);
                setText(null);
            }
        });

        colDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getDescricao())));
    }
}
