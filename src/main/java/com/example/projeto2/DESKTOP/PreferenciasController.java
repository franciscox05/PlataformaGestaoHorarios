package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Scope("prototype")
public class PreferenciasController {

    private static final DateTimeFormatter DATA_DECISAO_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private VBox painelFormulario;

    @FXML
    private Label lblTituloFormulario;

    @FXML
    private ComboBox<String> cbTipo;

    @FXML
    private VBox painelColega;

    @FXML
    private ComboBox<String> cbColegaPreferido;

    @FXML
    private VBox painelTurnos;

    @FXML
    private CheckBox chkTurnoManha;

    @FXML
    private CheckBox chkTurnoIntermedio;

    @FXML
    private CheckBox chkTurnoNoite;

    @FXML
    private ComboBox<String> cbDuracaoPreferida;

    @FXML
    private DatePicker dpDataInicio;

    @FXML
    private DatePicker dpDataFim;

    @FXML
    private CheckBox chkSemDataFim;

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

    private final PreferenciaService preferenciaBLL;

    private Utilizador utilizadorLogado;
    private Preferencia preferenciaEmEdicao;
    private List<String> colegasDaLoja = List.of();

    public PreferenciasController(PreferenciaService preferenciaBLL) {
        this.preferenciaBLL = preferenciaBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("Folgas", "Férias", "Colegas", "Turnos"));
        cbDuracaoPreferida.setItems(FXCollections.observableArrayList("Indiferente", "Mais curto", "Mais longo"));
        cbDuracaoPreferida.setValue("Indiferente");

        configurarTabelaHistoricoProprio();
        configurarTabelaPendentes();
        configurarTabelaHistoricoDecisoes();
        configurarAcoes();
        limparFormulario();
        esconderFeedback();
        esconderFeedbackGestao();

        // Tooltips nos campos do formulário
        cbTipo.setTooltip(new Tooltip("Tipo de preferência: folgas, férias, colegas ou turnos"));
        dpDataInicio.setTooltip(new Tooltip("Data a partir da qual a preferência é válida"));
        dpDataFim.setTooltip(new Tooltip("Data limite de validade (deixa vazio para permanente)"));
        txtDescricao.setTooltip(new Tooltip("Notas adicionais para o gestor (opcional)"));

        tabelaPreferencias.setPlaceholder(new Label("Ainda não tens preferências registadas."));
        tabelaPreferenciasPendentes.setPlaceholder(new Label("Não existem preferências pendentes para decidir nesta loja."));
        tabelaHistoricoDecisoes.setPlaceholder(new Label("Ainda não existem decisões registadas nesta loja."));

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
        painelTurnos.setManaged(false);
        painelTurnos.setVisible(false);
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarColegasDaLoja();
        carregarPreferencias();
        configurarPainelAprovacao();
    }

    @FXML
    public void onGuardarPreferenciaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            boolean novaPreferencia = preferenciaEmEdicao == null;
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    novaPreferencia ? "Guardar preferência" : "Atualizar preferência",
                    novaPreferencia ? "Deseja guardar esta preferência?" : "Deseja guardar as alterações desta preferência?",
                    novaPreferencia
                            ? "A preferência ficará registada para análise."
                            : "A preferência atual será atualizada."
            )) {
                return;
            }

            Preferencia preferencia = preferenciaEmEdicao != null ? preferenciaEmEdicao : new Preferencia();
            String tipoNormalizado = mapearTipoParaBaseDados(cbTipo.getValue());
            preferencia.setTipo(tipoNormalizado);
            preferencia.setDataInicio(resolverDataInicio(tipoNormalizado));
            preferencia.setDataFim(resolverDataFim(tipoNormalizado));
            preferencia.setPrioridade(null);
            preferencia.setDescricao(construirDescricaoFinal(tipoNormalizado));

            preferenciaBLL.guardarPreferencia(utilizadorLogado.getId(), preferencia);
            mostrarFeedback(
                    preferenciaEmEdicao == null
                            ? "Preferência registada com sucesso."
                            : "Preferência atualizada com sucesso.",
                    true
            );

            carregarPreferencias();
            tabelaPreferencias.getSelectionModel().clearSelection();
            limparFormulario();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback("Não foi possível guardar a preferência.", false);
        }
    }

    @FXML
    public void onCancelarEdicaoClick() {
        tabelaPreferencias.getSelectionModel().clearSelection();
        limparFormulario();
        mostrarFeedback("Edição cancelada.", true);
    }

    @FXML
    public void onRemoverPreferenciaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            Preferencia selecionada = tabelaPreferencias.getSelectionModel().getSelectedItem();
            if (selecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferência para remover.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Remover preferência",
                    "Deseja remover esta preferência?",
                    "A preferência selecionada será removida do teu registo."
            )) {
                return;
            }

            preferenciaBLL.removerPreferencia(utilizadorLogado.getId(), selecionada.getId());
            carregarPreferencias();
            tabelaPreferencias.getSelectionModel().clearSelection();
            limparFormulario();
            mostrarFeedback("Preferência removida com sucesso.", true);
        } catch (IllegalArgumentException e) {
            mostrarFeedback(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback("Não foi possível remover a preferência.", false);
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
                new SimpleStringProperty(formatarPeriodo(cellData.getValue())));

        colPrioridade.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarVigencia(cellData.getValue())));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(estado);
                badge.getStyleClass().add("badge-estado");
                switch (estado.toLowerCase()) {
                    case "pendente" -> badge.getStyleClass().add("badge-pendente");
                    case "aprovado" -> badge.getStyleClass().add("badge-aprovado");
                    case "rejeitado" -> badge.getStyleClass().add("badge-rejeitado");
                    default -> badge.getStyleClass().add("badge-rascunho");
                }
                setGraphic(badge);
                setText(null);
            }
        });

        colDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescricao()));
    }

    private void configurarTabelaPendentes() {
        colColaboradorPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeUtilizador(cellData.getValue().getIdUtilizador())));

        colTipoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colPeriodoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarPeriodo(cellData.getValue())));

        colPrioridadePendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarVigencia(cellData.getValue())));

        colDescricaoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDescricao(cellData.getValue().getDescricao())));
    }

    private void configurarTabelaHistoricoDecisoes() {
        colColaboradorHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeUtilizador(cellData.getValue().getIdUtilizador())));

        colEstadoHistorico.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));
        colEstadoHistorico.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(estado);
                badge.getStyleClass().add("badge-estado");
                switch (estado.toLowerCase()) {
                    case "pendente" -> badge.getStyleClass().add("badge-pendente");
                    case "aprovado" -> badge.getStyleClass().add("badge-aprovado");
                    case "rejeitado" -> badge.getStyleClass().add("badge-rejeitado");
                    default -> badge.getStyleClass().add("badge-rascunho");
                }
                setGraphic(badge);
                setText(null);
            }
        });

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
            lblTituloFormulario.setText("Editar Preferência");
            btnGuardarPreferencia.setText("Atualizar Preferência");
            btnCancelarEdicao.setDisable(false);

            cbTipo.setValue(formatarTipo(nova.getTipo()));
            configurarTipoSelecionado();
            dpDataInicio.setValue(nova.getDataInicio());
            chkSemDataFim.setSelected(nova.getDataFim() == null && permitePreferenciaSemDataFim(nova.getTipo()));
            dpDataFim.setValue(nova.getDataFim());
            preencherFormularioColegas(nova);
            preencherFormularioTurnos(nova);
            txtDescricao.setText(obterNotaLivre(nova));
            atualizarEstadoDatas();

            if (!preferenciaPodeSerEditada(nova)) {
                mostrarFeedback(
                        "Esta preferência já foi decidida. Regista uma nova preferência se precisares de alterar o pedido.",
                        false
                );
            } else {
                esconderFeedback();
            }
        });

        cbTipo.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        cbTipo.valueProperty().addListener((obs, antigo, novo) -> configurarTipoSelecionado());
        dpDataInicio.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        dpDataFim.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        cbColegaPreferido.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        cbDuracaoPreferida.valueProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        chkTurnoManha.selectedProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        chkTurnoIntermedio.selectedProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        chkTurnoNoite.selectedProperty().addListener((obs, antigo, novo) -> esconderFeedback());
        chkSemDataFim.selectedProperty().addListener((obs, antigo, novo) -> {
            atualizarEstadoDatas();
            esconderFeedback();
        });
        txtDescricao.textProperty().addListener((obs, antigo, novo) -> esconderFeedback());

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
        lblTituloFormulario.setText("Nova Preferência");
        btnGuardarPreferencia.setText("Guardar Preferência");
        btnCancelarEdicao.setDisable(true);

        cbTipo.setValue(null);
        cbColegaPreferido.setValue(null);
        chkTurnoManha.setSelected(false);
        chkTurnoIntermedio.setSelected(false);
        chkTurnoNoite.setSelected(false);
        cbDuracaoPreferida.setValue("Indiferente");
        dpDataInicio.setValue(null);
        dpDataFim.setValue(null);
        chkSemDataFim.setSelected(false);
        txtDescricao.clear();
        painelColega.setManaged(false);
        painelColega.setVisible(false);
        painelTurnos.setManaged(false);
        painelTurnos.setVisible(false);
        dpDataFim.setDisable(false);
        dpDataFim.setPromptText("Opcional");
        txtDescricao.setPromptText("Explica a tua preferência com detalhe suficiente para ser analisada.");
    }

    private void tratarDecisaoPreferencia(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            Preferencia preferenciaSelecionada = tabelaPreferenciasPendentes.getSelectionModel().getSelectedItem();
            if (preferenciaSelecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferência pendente primeiro.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar preferência" : "Rejeitar preferência",
                    aprovar ? "Deseja aprovar esta preferência?" : "Deseja rejeitar esta preferência?",
                    aprovar
                            ? "A decisão ficará registada para a loja."
                            : "A rejeição ficará registada para a loja."
            )) {
                return;
            }

            if (aprovar) {
                preferenciaBLL.aprovarPreferencia(
                        preferenciaSelecionada.getId(),
                        utilizadorLogado.getId(),
                        txtDecisaoGestor.getText()
                );
                mostrarFeedbackGestao("Preferência aprovada com sucesso.", true);
            } else {
                preferenciaBLL.rejeitarPreferencia(
                        preferenciaSelecionada.getId(),
                        utilizadorLogado.getId(),
                        txtDecisaoGestor.getText()
                );
                mostrarFeedbackGestao("Preferência rejeitada com sucesso.", true);
            }

            txtDecisaoGestor.clear();
            tabelaPreferenciasPendentes.getSelectionModel().clearSelection();
            carregarPreferenciasPendentes();
            carregarHistoricoDecisoes();
        } catch (IllegalArgumentException e) {
            mostrarFeedbackGestao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedbackGestao("Não foi possível atualizar a decisão da preferência.", false);
        }
    }

    private void mostrarFeedback(String mensagem, boolean sucesso) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        lblFeedback.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);

        if (sucesso) {
            javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            pausa.setOnFinished(e -> esconderFeedback());
            pausa.play();
        }
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

        if (sucesso) {
            javafx.animation.PauseTransition pausa = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            pausa.setOnFinished(e -> esconderFeedbackGestao());
            pausa.play();
        }
    }

    private void esconderFeedbackGestao() {
        lblFeedbackGestao.setVisible(false);
        lblFeedbackGestao.setManaged(false);
        lblFeedbackGestao.setText("");
    }

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferência.");
        }

        return switch (tipoSelecionado) {
            case "Folgas" -> "folgas";
            case "Férias" -> "ferias";
            case "Colegas" -> "colegas";
            case "Turnos" -> "turnos";
            default -> throw new IllegalArgumentException("O tipo de preferência selecionado é inválido.");
        };
    }

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }

        return switch (tipo.toLowerCase()) {
            case "folgas" -> "Folgas";
            case "ferias" -> "Férias";
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

    private String formatarPeriodo(Preferencia preferencia) {
        if (preferencia == null) {
            return "Sem período definido";
        }

        return formatarPeriodo(preferencia.getDataInicio(), preferencia.getDataFim(), preferencia.getTipo());
    }

    private String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim, String tipo) {
        if (dataInicio == null && dataFim == null) {
            return "Sem período definido";
        }

        if (dataInicio != null && dataFim == null) {
            return "Desde " + DATA_FORMATTER.format(dataInicio);
        }

        if (dataInicio != null && dataFim != null) {
            return DATA_FORMATTER.format(dataInicio) + " a " + DATA_FORMATTER.format(dataFim);
        }

        return DATA_FORMATTER.format(dataInicio);
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

    private String formatarVigencia(Preferencia preferencia) {
        if (preferencia == null) {
            return "-";
        }

        if (preferencia.getDataFim() == null && preferencia.getDataInicio() != null) {
            return "Permanente";
        }

        if (preferencia.getDataInicio() != null || preferencia.getDataFim() != null) {
            return "Temporária";
        }

        return "Sem período";
    }

    private boolean preferenciaPodeSerEditada(Preferencia preferencia) {
        return preferencia != null
                && (preferencia.getEstado() == null
                || preferencia.getEstado().isBlank()
                || "pendente".equalsIgnoreCase(preferencia.getEstado()));
    }

    private void carregarColegasDaLoja() {
        if (utilizadorLogado == null) {
            colegasDaLoja = List.of();
            cbColegaPreferido.setItems(FXCollections.observableArrayList());
            cbColegaPreferido.setPromptText("Sem colegas disponíveis");
            return;
        }

        colegasDaLoja = new ArrayList<>(preferenciaBLL.listarColegasDaLoja(utilizadorLogado.getId()));
        cbColegaPreferido.setItems(FXCollections.observableArrayList(colegasDaLoja));
        cbColegaPreferido.setPromptText(colegasDaLoja.isEmpty()
                ? "Sem colegas disponíveis"
                : "Seleciona um colega");
    }

    private void configurarTipoSelecionado() {
        String tipoSelecionado = cbTipo.getValue();
        boolean tipoColegas = "Colegas".equals(tipoSelecionado);
        boolean tipoTurnos = "Turnos".equals(tipoSelecionado);
        boolean permiteSemFim = permitePreferenciaSemDataFim(tipoSelecionado);

        painelColega.setManaged(tipoColegas);
        painelColega.setVisible(tipoColegas);
        painelTurnos.setManaged(tipoTurnos);
        painelTurnos.setVisible(tipoTurnos);

        if (!tipoColegas) {
            cbColegaPreferido.setValue(null);
        }
        if (!tipoTurnos) {
            chkTurnoManha.setSelected(false);
            chkTurnoIntermedio.setSelected(false);
            chkTurnoNoite.setSelected(false);
            cbDuracaoPreferida.setValue("Indiferente");
        }

        chkSemDataFim.setDisable(!permiteSemFim);
        if (!permiteSemFim) {
            chkSemDataFim.setSelected(false);
        }

        if (tipoColegas) {
            txtDescricao.setPromptText("Se quiseres, acrescenta contexto adicional para esta preferência.");
        } else if ("Turnos".equals(tipoSelecionado)) {
            txtDescricao.setPromptText("Acrescenta contexto opcional, por exemplo: estudo de manha, prefiro fechos curtos ou quero evitar aberturas consecutivas.");
        } else if ("Folgas".equals(tipoSelecionado) || "Férias".equals(tipoSelecionado)) {
            txtDescricao.setPromptText("Explica a tua preferência com o contexto necessário para análise.");
        } else {
            txtDescricao.setPromptText("Explica a tua preferência com detalhe suficiente para ser analisada.");
        }

        atualizarEstadoDatas();
    }

    private boolean permitePreferenciaSemDataFim(String tipoSelecionado) {
        String tipoNormalizado = normalizarTipoInterface(tipoSelecionado);
        return "folgas".equals(tipoNormalizado)
                || "ferias".equals(tipoNormalizado)
                || "colegas".equals(tipoNormalizado)
                || "turnos".equals(tipoNormalizado);
    }

    private void atualizarEstadoDatas() {
        boolean semDataFim = chkSemDataFim.isSelected() && !chkSemDataFim.isDisable();
        dpDataFim.setDisable(semDataFim);
        if (semDataFim) {
            dpDataFim.setValue(null);
            dpDataFim.setPromptText("Sem data fim");
            if (dpDataInicio.getValue() == null) {
                dpDataInicio.setValue(LocalDate.now());
            }
        } else {
            dpDataFim.setPromptText("Opcional");
        }
    }

    private LocalDate resolverDataInicio(String tipoNormalizado) {
        LocalDate dataInicioSelecionada = dpDataInicio.getValue();
        if (dataInicioSelecionada != null) {
            return dataInicioSelecionada;
        }

        if ("colegas".equals(tipoNormalizado) || "turnos".equals(tipoNormalizado)) {
            return LocalDate.now();
        }

        return null;
    }

    private LocalDate resolverDataFim(String tipoNormalizado) {
        if (chkSemDataFim.isSelected() && preferenciaPermiteSemDataFim(tipoNormalizado)) {
            return null;
        }
        return dpDataFim.getValue();
    }

    private String construirDescricaoFinal(String tipoNormalizado) {
        String textoLivre = limparTexto(txtDescricao.getText());

        if ("colegas".equals(tipoNormalizado)) {
            String colega = cbColegaPreferido.getValue();
            if (colega == null || colega.isBlank()) {
                throw new IllegalArgumentException("Seleciona o colega com quem queres trabalhar.");
            }

            if (textoLivre == null) {
                return "Colega preferido: " + colega + ".";
            }
            return "Colega preferido: " + colega + ". Nota adicional: " + textoLivre;
        }

        if ("turnos".equals(tipoNormalizado)) {
            return construirDescricaoEstruturadaTurnos(textoLivre);
        }

        if (textoLivre == null) {
            throw new IllegalArgumentException("Indica uma descrição para a preferência.");
        }
        return textoLivre;
    }

    private String construirDescricaoEstruturadaTurnos(String textoLivre) {
        Set<String> turnosPreferidos = new LinkedHashSet<>();
        if (chkTurnoManha.isSelected()) {
            turnosPreferidos.add("manha/abertura");
        }
        if (chkTurnoIntermedio.isSelected()) {
            turnosPreferidos.add("intermedio/tarde");
        }
        if (chkTurnoNoite.isSelected()) {
            turnosPreferidos.add("noite/fecho");
        }

        if (turnosPreferidos.isEmpty()) {
            turnosPreferidos.addAll(inferirTurnosAPartirDoContexto(textoLivre));
        }

        if (turnosPreferidos.isEmpty()) {
            throw new IllegalArgumentException("Seleciona pelo menos um bloco de turnos preferido.");
        }

        StringBuilder descricao = new StringBuilder("Turnos preferidos: ");
        descricao.append(String.join(", ", turnosPreferidos)).append(".");

        String duracao = resolverDuracaoPreferidaEstruturada();
        if (duracao == null) {
            duracao = inferirDuracaoAPartirDoContexto(textoLivre);
        }
        if (duracao != null) {
            descricao.append(" Duração preferida: ").append(duracao).append(".");
        }

        if (textoLivre != null) {
            descricao.append(" Nota adicional: ").append(textoLivre);
        }

        return descricao.toString();
    }

    private Set<String> inferirTurnosAPartirDoContexto(String textoLivre) {
        Set<String> turnosInferidos = new LinkedHashSet<>();
        String textoNormalizado = normalizarTextoPesquisa(textoLivre);
        if (textoNormalizado.isBlank()) {
            return turnosInferidos;
        }

        if (textoNormalizado.contains("manha")
                || textoNormalizado.contains("abertura")
                || textoNormalizado.contains("cedo")) {
            turnosInferidos.add("manha/abertura");
        }

        if (textoNormalizado.contains("intermedio")
                || textoNormalizado.contains("tarde")
                || textoNormalizado.contains("meio dia")
                || textoNormalizado.contains("meio-dia")) {
            turnosInferidos.add("intermedio/tarde");
        }

        if (textoNormalizado.contains("noite")
                || textoNormalizado.contains("fecho")
                || textoNormalizado.contains("encerramento")) {
            turnosInferidos.add("noite/fecho");
        }

        return turnosInferidos;
    }

    private String resolverDuracaoPreferidaEstruturada() {
        String selecao = cbDuracaoPreferida.getValue();
        if (selecao == null || selecao.isBlank() || "Indiferente".equalsIgnoreCase(selecao)) {
            return null;
        }

        return switch (selecao) {
            case "Mais curto" -> "curto";
            case "Mais longo" -> "longo";
            default -> null;
        };
    }

    private String inferirDuracaoAPartirDoContexto(String textoLivre) {
        if (textoLivre == null || textoLivre.isBlank()) {
            return null;
        }

        String textoNormalizado = normalizarTextoPesquisa(textoLivre);
        if (textoNormalizado.contains("curto")
                || textoNormalizado.contains("curtos")
                || textoNormalizado.contains("reduzido")
                || textoNormalizado.contains("mais curto")) {
            return "curto";
        }

        if (textoNormalizado.contains("longo")
                || textoNormalizado.contains("longos")
                || textoNormalizado.contains("mais longo")
                || textoNormalizado.contains("completo")) {
            return "longo";
        }

        return null;
    }

    private String normalizarTextoPesquisa(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    private void preencherFormularioColegas(Preferencia preferencia) {
        if (!"colegas".equalsIgnoreCase(preferencia.getTipo())) {
            cbColegaPreferido.setValue(null);
            return;
        }

        String descricao = preferencia.getDescricao();
        if (descricao == null || descricao.isBlank()) {
            cbColegaPreferido.setValue(null);
            return;
        }

        String descricaoNormalizada = descricao.toLowerCase(Locale.ROOT);
        for (String colega : colegasDaLoja) {
            if (descricaoNormalizada.contains(colega.toLowerCase(Locale.ROOT))) {
                cbColegaPreferido.setValue(colega);
                return;
            }
        }
        cbColegaPreferido.setValue(null);
    }

    private void preencherFormularioTurnos(Preferencia preferencia) {
        if (preferencia == null || !"turnos".equalsIgnoreCase(preferencia.getTipo())) {
            chkTurnoManha.setSelected(false);
            chkTurnoIntermedio.setSelected(false);
            chkTurnoNoite.setSelected(false);
            cbDuracaoPreferida.setValue("Indiferente");
            return;
        }

        String descricao = limparTexto(preferencia.getDescricao());
        if (descricao == null) {
            chkTurnoManha.setSelected(false);
            chkTurnoIntermedio.setSelected(false);
            chkTurnoNoite.setSelected(false);
            cbDuracaoPreferida.setValue("Indiferente");
            return;
        }

        String descricaoNormalizada = descricao.toLowerCase(Locale.ROOT);
        chkTurnoManha.setSelected(descricaoNormalizada.contains("manha") || descricaoNormalizada.contains("abertura"));
        chkTurnoIntermedio.setSelected(descricaoNormalizada.contains("intermedio") || descricaoNormalizada.contains("tarde"));
        chkTurnoNoite.setSelected(descricaoNormalizada.contains("noite") || descricaoNormalizada.contains("fecho"));

        if (descricaoNormalizada.contains("duracao preferida: curto") || descricaoNormalizada.contains(" turnos curtos")) {
            cbDuracaoPreferida.setValue("Mais curto");
        } else if (descricaoNormalizada.contains("duracao preferida: longo") || descricaoNormalizada.contains(" turnos longos")) {
            cbDuracaoPreferida.setValue("Mais longo");
        } else {
            cbDuracaoPreferida.setValue("Indiferente");
        }
    }

    private String obterNotaLivre(Preferencia preferencia) {
        if (preferencia == null || preferencia.getDescricao() == null) {
            return "";
        }

        String descricao = preferencia.getDescricao().trim();
        if (!"colegas".equalsIgnoreCase(preferencia.getTipo())
                && !"turnos".equalsIgnoreCase(preferencia.getTipo())) {
            return descricao;
        }

        int indiceNota = descricao.indexOf("Nota adicional:");
        if (indiceNota >= 0) {
            return descricao.substring(indiceNota + "Nota adicional:".length()).trim();
        }
        return "";
    }

    private String limparTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String textoLimpo = texto.trim();
        return textoLimpo.isEmpty() ? null : textoLimpo;
    }

    private String normalizarTipoInterface(String tipoSelecionado) {
        return tipoSelecionado == null ? "" : tipoSelecionado.trim().toLowerCase(Locale.ROOT);
    }

    private boolean preferenciaPermiteSemDataFim(String tipo) {
        return permitePreferenciaSemDataFim(tipo);
    }

    private Window obterJanela() {
        if (painelFormulario == null || painelFormulario.getScene() == null) {
            return null;
        }
        return painelFormulario.getScene().getWindow();
    }
}
