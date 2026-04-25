package com.example.projeto2.Controller;

import com.example.projeto2.BLL.AuditoriaBLL;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class PainelAuditoriaController {

    @FXML
    private Label lblLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private Label lblCargo;

    @FXML
    private ComboBox<AuditoriaBLL.FiltroTipoEvento> cbTipoEvento;

    @FXML
    private ComboBox<AuditoriaBLL.FiltroUtilizador> cbColaborador;

    @FXML
    private DatePicker dpDataInicio;

    @FXML
    private DatePicker dpDataFim;

    @FXML
    private Button btnAplicarFiltros;

    @FXML
    private Button btnLimparFiltros;

    @FXML
    private Label lblFeedback;

    @FXML
    private Label lblTotalEventos;

    @FXML
    private Label lblTotalFalhas;

    @FXML
    private Label lblTotalAutenticacoes;

    @FXML
    private Label lblTotalAlteracoes;

    @FXML
    private TableView<AuditoriaBLL.EventoLinha> tabelaEventos;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colDataHora;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colTipo;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colResultado;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colColaborador;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colOrigem;

    @FXML
    private TableColumn<AuditoriaBLL.EventoLinha, String> colDetalhes;

    @FXML
    private Label lblEventoSelecionado;

    @FXML
    private Label lblEmailSelecionado;

    @FXML
    private Label lblSessaoSelecionada;

    @FXML
    private Label lblMomentoSelecionado;

    @FXML
    private TextArea txtDetalhesEvento;

    private final AuditoriaBLL auditoriaBLL;

    private Utilizador utilizadorLogado;

    public PainelAuditoriaController(AuditoriaBLL auditoriaBLL) {
        this.auditoriaBLL = auditoriaBLL;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        tabelaEventos.setPlaceholder(new Label("Ainda nao existem eventos de auditoria para os filtros selecionados."));
        tabelaEventos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, valor) -> preencherDetalheEvento(valor));
        txtDetalhesEvento.setEditable(false);
        txtDetalhesEvento.setFocusTraversable(false);
        limparDetalheEvento();
        esconderFeedback();
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarPainel(false);
    }

    @FXML
    public void onAplicarFiltrosClick() {
        carregarPainel(true);
    }

    @FXML
    public void onLimparFiltrosClick() {
        cbTipoEvento.getSelectionModel().selectFirst();
        cbColaborador.getSelectionModel().selectFirst();
        dpDataInicio.setValue(null);
        dpDataFim.setValue(null);
        carregarPainel(true);
    }

    private void configurarTabela() {
        colDataHora.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().dataHora()));

        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().tipoEvento()));

        colResultado.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().resultado()));

        colColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().colaborador()));

        colOrigem.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().origem()));

        colDetalhes.setCellValueFactory(cellData ->
                new SimpleStringProperty(resumirDetalhes(cellData.getValue().detalhes())));
    }

    private void carregarPainel(boolean mostrarMensagemSucesso) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            String tipoSelecionado = obterTipoSelecionado();
            Integer idColaboradorSelecionado = obterColaboradorSelecionado();

            AuditoriaBLL.PainelAuditoriaSnapshot snapshot = auditoriaBLL.carregarPainel(
                    utilizadorLogado.getId(),
                    new AuditoriaBLL.FiltroAuditoria(
                            tipoSelecionado,
                            idColaboradorSelecionado,
                            dpDataInicio.getValue(),
                            dpDataFim.getValue()
                    )
            );

            preencherContexto(snapshot.contexto(), tipoSelecionado, idColaboradorSelecionado);
            preencherResumo(snapshot.resumo());
            tabelaEventos.setItems(FXCollections.observableArrayList(snapshot.eventos()));

            if (snapshot.eventos().isEmpty()) {
                tabelaEventos.getSelectionModel().clearSelection();
                limparDetalheEvento();
                if (mostrarMensagemSucesso) {
                    mostrarFeedback("Filtros aplicados. Não foram encontrados eventos para esse critério.", true);
                } else {
                    esconderFeedback();
                }
            } else {
                tabelaEventos.getSelectionModel().selectFirst();
                preencherDetalheEvento(tabelaEventos.getSelectionModel().getSelectedItem());
                if (mostrarMensagemSucesso) {
                    mostrarFeedback("Historico de auditoria atualizado com sucesso.", true);
                } else {
                    esconderFeedback();
                }
            }

            btnAplicarFiltros.setDisable(false);
            btnLimparFiltros.setDisable(false);
            cbTipoEvento.setDisable(false);
            cbColaborador.setDisable(false);
            dpDataInicio.setDisable(false);
            dpDataFim.setDisable(false);
        } catch (IllegalArgumentException e) {
            tabelaEventos.setItems(FXCollections.observableArrayList());
            limparResumo();
            limparDetalheEvento();
            mostrarFeedback(e.getMessage(), false);
            aplicarEstadoRestrito();
        } catch (Exception e) {
            tabelaEventos.setItems(FXCollections.observableArrayList());
            limparResumo();
            limparDetalheEvento();
            mostrarFeedback("Não foi possível carregar o histórico de auditoria.", false);
        }
    }

    private void preencherContexto(AuditoriaBLL.AuditoriaContexto contexto,
                                   String tipoSelecionado,
                                   Integer idColaboradorSelecionado) {
        lblLoja.setText(contexto.nomeLoja());
        lblLocalizacao.setText(contexto.localizacao());
        lblCargo.setText(contexto.cargoGestao());

        List<AuditoriaBLL.FiltroTipoEvento> tiposEvento = new ArrayList<>();
        tiposEvento.add(new AuditoriaBLL.FiltroTipoEvento(null, "Todos os tipos"));
        tiposEvento.addAll(contexto.tiposEvento());
        cbTipoEvento.setItems(FXCollections.observableArrayList(tiposEvento));
        selecionarTipo(tipoSelecionado);

        List<AuditoriaBLL.FiltroUtilizador> colaboradores = new ArrayList<>();
        colaboradores.add(new AuditoriaBLL.FiltroUtilizador(null, "Todos os colaboradores", null));
        colaboradores.addAll(contexto.utilizadores());
        cbColaborador.setItems(FXCollections.observableArrayList(colaboradores));
        selecionarColaborador(idColaboradorSelecionado);
    }

    private void preencherResumo(AuditoriaBLL.AuditoriaResumo resumo) {
        lblTotalEventos.setText(String.valueOf(resumo.totalEventos()));
        lblTotalFalhas.setText(String.valueOf(resumo.totalFalhas()));
        lblTotalAutenticacoes.setText(String.valueOf(resumo.totalAutenticacoes()));
        lblTotalAlteracoes.setText(String.valueOf(resumo.totalAlteracoesSensiveis()));
    }

    private void limparResumo() {
        lblTotalEventos.setText("0");
        lblTotalFalhas.setText("0");
        lblTotalAutenticacoes.setText("0");
        lblTotalAlteracoes.setText("0");
    }

    private void preencherDetalheEvento(AuditoriaBLL.EventoLinha evento) {
        if (evento == null) {
            limparDetalheEvento();
            return;
        }

        lblEventoSelecionado.setText(evento.colaborador());
        lblEmailSelecionado.setText(evento.email());
        lblSessaoSelecionada.setText(evento.identificadorSessao());
        lblMomentoSelecionado.setText(evento.dataHora());
        txtDetalhesEvento.setText(evento.detalhes());
        txtDetalhesEvento.positionCaret(0);
    }

    private void limparDetalheEvento() {
        lblEventoSelecionado.setText("-");
        lblEmailSelecionado.setText("-");
        lblSessaoSelecionada.setText("-");
        lblMomentoSelecionado.setText("-");
        txtDetalhesEvento.setText("Seleciona um evento na tabela para veres o detalhe completo.");
        txtDetalhesEvento.positionCaret(0);
    }

    private void mostrarFeedback(String mensagem, boolean sucesso) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!lblFeedback.getStyleClass().contains("mensagem-feedback")) {
            lblFeedback.getStyleClass().add("mensagem-feedback");
        }
        lblFeedback.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedback.setManaged(true);
        lblFeedback.setVisible(true);
    }

    private void esconderFeedback() {
        lblFeedback.setText("");
        lblFeedback.setManaged(false);
        lblFeedback.setVisible(false);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!lblFeedback.getStyleClass().contains("mensagem-feedback")) {
            lblFeedback.getStyleClass().add("mensagem-feedback");
        }
    }

    private void aplicarEstadoRestrito() {
        btnAplicarFiltros.setDisable(true);
        btnLimparFiltros.setDisable(true);
        cbTipoEvento.setDisable(true);
        cbColaborador.setDisable(true);
        dpDataInicio.setDisable(true);
        dpDataFim.setDisable(true);
    }

    private void selecionarTipo(String codigoSelecionado) {
        cbTipoEvento.getItems().stream()
                .filter(item -> codigoSelecionado == null ? item.codigo() == null : codigoSelecionado.equals(item.codigo()))
                .findFirst()
                .ifPresentOrElse(
                        cbTipoEvento::setValue,
                        () -> cbTipoEvento.getSelectionModel().selectFirst()
                );
    }

    private void selecionarColaborador(Integer idSelecionado) {
        cbColaborador.getItems().stream()
                .filter(item -> idSelecionado == null ? item.id() == null : idSelecionado.equals(item.id()))
                .findFirst()
                .ifPresentOrElse(
                        cbColaborador::setValue,
                        () -> cbColaborador.getSelectionModel().selectFirst()
                );
    }

    private String obterTipoSelecionado() {
        AuditoriaBLL.FiltroTipoEvento tipoSelecionado = cbTipoEvento.getValue();
        return tipoSelecionado != null ? tipoSelecionado.codigo() : null;
    }

    private Integer obterColaboradorSelecionado() {
        AuditoriaBLL.FiltroUtilizador colaboradorSelecionado = cbColaborador.getValue();
        return colaboradorSelecionado != null ? colaboradorSelecionado.id() : null;
    }

    private String resumirDetalhes(String detalhes) {
        if (detalhes == null || detalhes.isBlank()) {
            return "-";
        }

        if (detalhes.length() <= 70) {
            return detalhes;
        }

        return detalhes.substring(0, 67) + "...";
    }
}
