package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class PreferenciasController {

    private static final DateTimeFormatter DATA_DECISAO_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private VBox painelFormulario;

    @FXML
    private Label lblTituloFormulario;

    @FXML
    private ComboBox<String> cbTipo;

    @FXML
    private DatePicker dpDataInicio;

    @FXML
    private DatePicker dpDataFim;

    @FXML
    private Spinner<Integer> spPrioridade;

    @FXML
    private TextArea txtDescricao;

    @FXML
    private Button btnGuardarPreferencia;

    @FXML
    private Button btnCancelarEdicao;

    @FXML
    private Button btnRemoverPreferencia;

    @FXML
    private Label lblFeedback;

    @FXML
    private TableView<Preferencia> tabelaPreferencias;

    @FXML
    private TableColumn<Preferencia, String> colTipo;

    @FXML
    private TableColumn<Preferencia, String> colPeriodo;

    @FXML
    private TableColumn<Preferencia, String> colPrioridade;

    @FXML
    private TableColumn<Preferencia, String> colEstado;

    @FXML
    private TableColumn<Preferencia, String> colDescricao;

    @FXML
    private VBox painelAprovacao;

    @FXML
    private TableView<Preferencia> tabelaPreferenciasPendentes;

    @FXML
    private TableColumn<Preferencia, String> colColaboradorPendente;

    @FXML
    private TableColumn<Preferencia, String> colTipoPendente;

    @FXML
    private TableColumn<Preferencia, String> colPeriodoPendente;

    @FXML
    private TableColumn<Preferencia, String> colPrioridadePendente;

    @FXML
    private TableColumn<Preferencia, String> colDescricaoPendente;

    @FXML
    private TextArea txtDecisaoGestor;

    @FXML
    private Label lblFeedbackGestao;

    @FXML
    private Button btnAprovarPreferencia;

    @FXML
    private Button btnRejeitarPreferencia;

    @FXML
    private TableView<Preferencia> tabelaHistoricoDecisoes;

    @FXML
    private TableColumn<Preferencia, String> colColaboradorHistorico;

    @FXML
    private TableColumn<Preferencia, String> colEstadoHistorico;

    @FXML
    private TableColumn<Preferencia, String> colDecisaoHistorico;

    @FXML
    private TableColumn<Preferencia, String> colDecisorHistorico;

    @FXML
    private TableColumn<Preferencia, String> colDataDecisaoHistorico;

    private final PreferenciaBLL preferenciaBLL;

    private Utilizador utilizadorLogado;
    private Preferencia preferenciaEmEdicao;

    public PreferenciasController(PreferenciaBLL preferenciaBLL) {
        this.preferenciaBLL = preferenciaBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("Folgas", "Ferias", "Colegas", "Turnos"));
        spPrioridade.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3));
        spPrioridade.setEditable(false);

        configurarTabelaHistoricoProprio();
        configurarTabelaPendentes();
        configurarTabelaHistoricoDecisoes();
        configurarAcoes();
        limparFormulario();
        esconderFeedback();
        esconderFeedbackGestao();

        tabelaPreferencias.setPlaceholder(new Label("Ainda nao tens preferencias registadas."));
        tabelaPreferenciasPendentes.setPlaceholder(new Label("Nao existem preferencias pendentes para decidir nesta loja."));
        tabelaHistoricoDecisoes.setPlaceholder(new Label("Ainda nao existem decisoes registadas nesta loja."));

        btnGuardarPreferencia.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    Preferencia selecionada = tabelaPreferencias.getSelectionModel().getSelectedItem();
                    return selecionada != null && !preferenciaPodeSerEditada(selecionada);
                },
                tabelaPreferencias.getSelectionModel().selectedItemProperty()
        ));

        btnRemoverPreferencia.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    Preferencia selecionada = tabelaPreferencias.getSelectionModel().getSelectedItem();
                    return selecionada == null || !preferenciaPodeSerEditada(selecionada);
                },
                tabelaPreferencias.getSelectionModel().selectedItemProperty()
        ));

        btnAprovarPreferencia.disableProperty().bind(Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPreferencia.disableProperty().bind(Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));

        painelAprovacao.setManaged(false);
        painelAprovacao.setVisible(false);
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarPreferencias();
        configurarPainelAprovacao();
    }

    @FXML
    public void onGuardarPreferenciaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            Preferencia preferencia = preferenciaEmEdicao != null ? preferenciaEmEdicao : new Preferencia();
            preferencia.setTipo(mapearTipoParaBaseDados(cbTipo.getValue()));
            preferencia.setDataInicio(dpDataInicio.getValue());
            preferencia.setDataFim(dpDataFim.getValue());
            preferencia.setPrioridade(spPrioridade.getValue());
            preferencia.setDescricao(txtDescricao.getText());

            Preferencia guardada = preferenciaBLL.guardarPreferencia(utilizadorLogado.getId(), preferencia);
            mostrarFeedback(
                    preferenciaEmEdicao == null
                            ? "Preferencia registada com sucesso."
                            : "Preferencia atualizada com sucesso.",
                    true
            );

            carregarPreferencias();
            tabelaPreferencias.getSelectionModel().clearSelection();
            limparFormulario();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback("Nao foi possivel guardar a preferencia.", false);
        }
    }

    @FXML
    public void onCancelarEdicaoClick() {
        tabelaPreferencias.getSelectionModel().clearSelection();
        limparFormulario();
        mostrarFeedback("Edicao cancelada.", true);
    }

    @FXML
    public void onRemoverPreferenciaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            Preferencia selecionada = tabelaPreferencias.getSelectionModel().getSelectedItem();
            if (selecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferencia para remover.");
            }

            preferenciaBLL.removerPreferencia(utilizadorLogado.getId(), selecionada.getId());
            carregarPreferencias();
            tabelaPreferencias.getSelectionModel().clearSelection();
            limparFormulario();
            mostrarFeedback("Preferencia removida com sucesso.", true);
        } catch (IllegalArgumentException e) {
            mostrarFeedback(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback("Nao foi possivel remover a preferencia.", false);
        }
    }

    @FXML
    public void onAprovarPreferenciaClick() {
        tratarDecisaoPreferencia(true);
    }

    @FXML
    public void onRejeitarPreferenciaClick() {
        tratarDecisaoPreferencia(false);
    }

    private void configurarTabelaHistoricoProprio() {
        colTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colPeriodo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarPeriodo(cellData.getValue().getDataInicio(), cellData.getValue().getDataFim())));

        colPrioridade.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getPrioridade())));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        colDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescricao()));
    }

    private void configurarTabelaPendentes() {
        colColaboradorPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeUtilizador(cellData.getValue().getIdUtilizador())));

        colTipoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colPeriodoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarPeriodo(cellData.getValue().getDataInicio(), cellData.getValue().getDataFim())));

        colPrioridadePendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getPrioridade())));

        colDescricaoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDescricao(cellData.getValue().getDescricao())));
    }

    private void configurarTabelaHistoricoDecisoes() {
        colColaboradorHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeUtilizador(cellData.getValue().getIdUtilizador())));

        colEstadoHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        colDecisaoHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDecisao(cellData.getValue().getDecisao())));

        colDecisorHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeUtilizador(cellData.getValue().getIdDecisor())));

        colDataDecisaoHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataDecisao(cellData.getValue().getDataDecisao())));
    }

    private void configurarAcoes() {
        tabelaPreferencias.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) {
                return;
            }

            preferenciaEmEdicao = nova;
            lblTituloFormulario.setText("Editar Preferencia");
            btnGuardarPreferencia.setText("Atualizar Preferencia");
            btnCancelarEdicao.setDisable(false);

            cbTipo.setValue(formatarTipo(nova.getTipo()));
            dpDataInicio.setValue(nova.getDataInicio());
            dpDataFim.setValue(nova.getDataFim());
            spPrioridade.getValueFactory().setValue(nova.getPrioridade() != null ? nova.getPrioridade() : 3);
            txtDescricao.setText(nova.getDescricao());

            if (!preferenciaPodeSerEditada(nova)) {
                mostrarFeedback(
                        "Esta preferencia ja foi decidida. Regista uma nova preferencia se precisares de alterar o pedido.",
                        false
                );
            } else {
                esconderFeedback();
            }
        });

        cbTipo.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        dpDataInicio.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        dpDataFim.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        txtDescricao.textProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        spPrioridade.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());

        tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            txtDecisaoGestor.clear();
            esconderFeedbackGestao();
        });
        txtDecisaoGestor.textProperty().addListener((obs, antigo, novo) -> esconderFeedbackGestao());
    }

    private void carregarPreferencias() {
        if (utilizadorLogado == null) {
            tabelaPreferencias.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Preferencia> preferencias = preferenciaBLL.listarPreferenciasPorUtilizador(utilizadorLogado.getId());
        tabelaPreferencias.setItems(FXCollections.observableArrayList(preferencias));
        tabelaPreferencias.refresh();
    }

    private void configurarPainelAprovacao() {
        boolean podeAprovar = utilizadorLogado != null
                && preferenciaBLL.utilizadorPodeAprovarPreferencias(utilizadorLogado.getId());

        painelAprovacao.setManaged(podeAprovar);
        painelAprovacao.setVisible(podeAprovar);

        if (podeAprovar) {
            carregarPreferenciasPendentes();
            carregarHistoricoDecisoes();
        } else {
            tabelaPreferenciasPendentes.setItems(FXCollections.observableArrayList());
            tabelaHistoricoDecisoes.setItems(FXCollections.observableArrayList());
        }
    }

    private void carregarPreferenciasPendentes() {
        if (utilizadorLogado == null
                || !preferenciaBLL.utilizadorPodeAprovarPreferencias(utilizadorLogado.getId())) {
            tabelaPreferenciasPendentes.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Preferencia> preferencias = preferenciaBLL.listarPreferenciasPendentesParaAprovacao(utilizadorLogado.getId());
        tabelaPreferenciasPendentes.setItems(FXCollections.observableArrayList(preferencias));
        tabelaPreferenciasPendentes.refresh();
    }

    private void carregarHistoricoDecisoes() {
        if (utilizadorLogado == null
                || !preferenciaBLL.utilizadorPodeAprovarPreferencias(utilizadorLogado.getId())) {
            tabelaHistoricoDecisoes.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Preferencia> historico = preferenciaBLL.listarHistoricoDecisoesDaLoja(utilizadorLogado.getId());
        tabelaHistoricoDecisoes.setItems(FXCollections.observableArrayList(historico));
        tabelaHistoricoDecisoes.refresh();
    }

    private void limparFormulario() {
        preferenciaEmEdicao = null;
        lblTituloFormulario.setText("Nova Preferencia");
        btnGuardarPreferencia.setText("Guardar Preferencia");
        btnCancelarEdicao.setDisable(true);

        cbTipo.setValue(null);
        dpDataInicio.setValue(null);
        dpDataFim.setValue(null);
        spPrioridade.getValueFactory().setValue(3);
        txtDescricao.clear();
    }

    private void tratarDecisaoPreferencia(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            Preferencia preferenciaSelecionada = tabelaPreferenciasPendentes.getSelectionModel().getSelectedItem();
            if (preferenciaSelecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferencia pendente primeiro.");
            }

            if (aprovar) {
                preferenciaBLL.aprovarPreferencia(
                        preferenciaSelecionada.getId(),
                        utilizadorLogado.getId(),
                        txtDecisaoGestor.getText()
                );
                mostrarFeedbackGestao("Preferencia aprovada com sucesso.", true);
            } else {
                preferenciaBLL.rejeitarPreferencia(
                        preferenciaSelecionada.getId(),
                        utilizadorLogado.getId(),
                        txtDecisaoGestor.getText()
                );
                mostrarFeedbackGestao("Preferencia rejeitada com sucesso.", true);
            }

            txtDecisaoGestor.clear();
            tabelaPreferenciasPendentes.getSelectionModel().clearSelection();
            carregarPreferenciasPendentes();
            carregarHistoricoDecisoes();
        } catch (IllegalArgumentException e) {
            mostrarFeedbackGestao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedbackGestao("Nao foi possivel atualizar a decisao da preferencia.", false);
        }
    }

    private void mostrarFeedback(String mensagem, boolean sucesso) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        lblFeedback.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void esconderFeedback() {
        lblFeedback.setVisible(false);
        lblFeedback.setManaged(false);
        lblFeedback.setText("");
    }

    private void mostrarFeedbackGestao(String mensagem, boolean sucesso) {
        lblFeedbackGestao.setText(mensagem);
        lblFeedbackGestao.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblFeedbackGestao.getStyleClass().add("mensagem-feedback");
        lblFeedbackGestao.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedbackGestao.setVisible(true);
        lblFeedbackGestao.setManaged(true);
    }

    private void esconderFeedbackGestao() {
        lblFeedbackGestao.setVisible(false);
        lblFeedbackGestao.setManaged(false);
        lblFeedbackGestao.setText("");
    }

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferencia.");
        }

        return switch (tipoSelecionado) {
            case "Folgas" -> "folgas";
            case "Ferias" -> "ferias";
            case "Colegas" -> "colegas";
            case "Turnos" -> "turnos";
            default -> throw new IllegalArgumentException("O tipo de preferencia selecionado e invalido.");
        };
    }

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }

        return switch (tipo.toLowerCase()) {
            case "folgas" -> "Folgas";
            case "ferias" -> "Ferias";
            case "colegas" -> "Colegas";
            case "turnos" -> "Turnos";
            default -> tipo;
        };
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "Pendente";
        }

        return switch (estado.toLowerCase()) {
            case "pendente" -> "Pendente";
            case "aprovado" -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            default -> estado;
        };
    }

    private String formatarDecisao(String decisao) {
        if (decisao == null || decisao.isBlank()) {
            return "-";
        }
        return decisao;
    }

    private String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) {
            return "Sem periodo definido";
        }

        if (dataInicio != null && dataFim != null) {
            return dataInicio + " a " + dataFim;
        }

        return String.valueOf(dataInicio);
    }

    private String formatarDataDecisao(LocalDateTime dataDecisao) {
        if (dataDecisao == null) {
            return "-";
        }
        return DATA_DECISAO_FORMATTER.format(dataDecisao);
    }

    private String obterNomeUtilizador(Utilizador utilizador) {
        if (utilizador == null || utilizador.getNome() == null || utilizador.getNome().isBlank()) {
            return "-";
        }
        return utilizador.getNome();
    }

    private String formatarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return "-";
        }
        return descricao;
    }

    private boolean preferenciaPodeSerEditada(Preferencia preferencia) {
        return preferencia != null
                && (preferencia.getEstado() == null
                || preferencia.getEstado().isBlank()
                || "pendente".equalsIgnoreCase(preferencia.getEstado()));
    }
}
