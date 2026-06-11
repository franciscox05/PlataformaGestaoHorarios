package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.DESKTOP.support.PreferenciaDescricaoBuilder;
import com.example.projeto2.DESKTOP.support.PreferenciaFormatters;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class PreferenciasController {

    // ── FXML fields ─────────────────────────────────────────────────────────────

    @FXML private VBox painelFormulario;
    @FXML private Label lblTituloFormulario;
    @FXML private ComboBox<String> cbTipo;
    @FXML private VBox painelColega;
    @FXML private ComboBox<String> cbColegaPreferido;
    @FXML private VBox painelTurnos;
    @FXML private CheckBox chkTurnoManha;
    @FXML private CheckBox chkTurnoIntermedio;
    @FXML private CheckBox chkTurnoNoite;
    @FXML private ComboBox<String> cbDuracaoPreferida;
    @FXML private DatePicker dpDataInicio;
    @FXML private DatePicker dpDataFim;
    @FXML private CheckBox chkSemDataFim;
    @FXML private TextArea txtDescricao;
    @FXML private Button btnGuardarPreferencia;
    @FXML private Button btnCancelarEdicao;
    @FXML private Button btnRemoverPreferencia;
    @FXML private Label lblFeedback;

    @FXML private TableView<Preferencia> tabelaPreferencias;
    @FXML private TableColumn<Preferencia, String> colTipo;
    @FXML private TableColumn<Preferencia, String> colPeriodo;
    @FXML private TableColumn<Preferencia, String> colPrioridade;
    @FXML private TableColumn<Preferencia, String> colEstado;
    @FXML private TableColumn<Preferencia, String> colDescricao;

    @FXML private VBox painelAprovacao;
    @FXML private TableView<Preferencia> tabelaPreferenciasPendentes;
    @FXML private TableColumn<Preferencia, String> colColaboradorPendente;
    @FXML private TableColumn<Preferencia, String> colTipoPendente;
    @FXML private TableColumn<Preferencia, String> colPeriodoPendente;
    @FXML private TableColumn<Preferencia, String> colPrioridadePendente;
    @FXML private TableColumn<Preferencia, String> colDescricaoPendente;
    @FXML private TextArea txtDecisaoGestor;
    @FXML private Label lblFeedbackGestao;
    @FXML private Button btnAprovarPreferencia;
    @FXML private Button btnRejeitarPreferencia;

    @FXML private TableView<Preferencia> tabelaHistoricoDecisoes;
    @FXML private TableColumn<Preferencia, String> colColaboradorHistorico;
    @FXML private TableColumn<Preferencia, String> colEstadoHistorico;
    @FXML private TableColumn<Preferencia, String> colDecisaoHistorico;
    @FXML private TableColumn<Preferencia, String> colDecisorHistorico;
    @FXML private TableColumn<Preferencia, String> colDataDecisaoHistorico;

    // ── State ────────────────────────────────────────────────────────────────────

    private final PreferenciaService preferenciaBLL;
    private PreferenciaDescricaoBuilder descricaoBuilder;

    private Utilizador utilizadorLogado;
    private Preferencia preferenciaEmEdicao;
    private List<String> colegasDaLoja = List.of();

    public PreferenciasController(PreferenciaService preferenciaBLL) {
        this.preferenciaBLL = preferenciaBLL;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("Folgas", "Férias", "Folga preferida", "Colegas", "Turnos"));
        cbDuracaoPreferida.setItems(FXCollections.observableArrayList("Indiferente", "Mais curto", "Mais longo"));
        cbDuracaoPreferida.setValue("Indiferente");

        configurarTabelaHistoricoProprio();
        configurarTabelaPendentes();
        configurarTabelaHistoricoDecisoes();
        configurarAcoes();
        limparFormulario();
        esconderFeedback();
        esconderFeedbackGestao();

        cbTipo.setTooltip(new Tooltip("Tipo de preferência: folgas, férias, folga preferida (recorrente soft), colegas ou turnos"));
        dpDataInicio.setTooltip(new Tooltip("Data a partir da qual a preferência é válida"));
        dpDataFim.setTooltip(new Tooltip("Data limite de validade (deixa vazio para permanente)"));
        txtDescricao.setTooltip(new Tooltip("Notas adicionais para o gestor (opcional)"));

        tabelaPreferencias.setPlaceholder(new Label("Ainda não tens preferências registadas."));
        tabelaPreferenciasPendentes.setPlaceholder(new Label("Não existem preferências pendentes para decidir nesta loja."));
        tabelaHistoricoDecisoes.setPlaceholder(new Label("Ainda não existem decisões registadas nesta loja."));

        btnGuardarPreferencia.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    Preferencia sel = tabelaPreferencias.getSelectionModel().getSelectedItem();
                    return sel != null && !preferenciaPodeSerEditada(sel);
                },
                tabelaPreferencias.getSelectionModel().selectedItemProperty()
        ));
        btnRemoverPreferencia.disableProperty().bind(Bindings.createBooleanBinding(
                () -> {
                    Preferencia sel = tabelaPreferencias.getSelectionModel().getSelectedItem();
                    return sel == null || !preferenciaPodeSerEditada(sel);
                },
                tabelaPreferencias.getSelectionModel().selectedItemProperty()
        ));
        btnAprovarPreferencia.disableProperty().bind(
                Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPreferencia.disableProperty().bind(
                Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));

        painelAprovacao.setManaged(false);
        painelAprovacao.setVisible(false);
        painelTurnos.setManaged(false);
        painelTurnos.setVisible(false);

        descricaoBuilder = new PreferenciaDescricaoBuilder(
                cbColegaPreferido, chkTurnoManha, chkTurnoIntermedio, chkTurnoNoite, cbDuracaoPreferida);
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarColegasDaLoja();
        carregarPreferencias();
        configurarPainelAprovacao();
    }

    // ── FXML event handlers ──────────────────────────────────────────────────────

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
                    novaPreferencia ? "A preferência ficará registada para análise." : "A preferência atual será atualizada."
            )) {
                return;
            }

            Preferencia preferencia = preferenciaEmEdicao != null ? preferenciaEmEdicao : new Preferencia();
            String tipoNormalizado = mapearTipoParaBaseDados(cbTipo.getValue());
            preferencia.setTipo(tipoNormalizado);
            preferencia.setDataInicio(resolverDataInicio(tipoNormalizado));
            preferencia.setDataFim(resolverDataFim(tipoNormalizado));
            preferencia.setPrioridade(null);
            String textoLivre = PreferenciaFormatters.limparTexto(txtDescricao.getText());
            preferencia.setDescricao(descricaoBuilder.construirDescricaoFinal(tipoNormalizado, textoLivre));

            preferenciaBLL.guardarPreferencia(utilizadorLogado.getId(), preferencia);
            mostrarFeedback(
                    preferenciaEmEdicao == null ? "Preferência registada com sucesso." : "Preferência atualizada com sucesso.",
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

    // ── Table configuration ──────────────────────────────────────────────────────

    private void configurarTabelaHistoricoProprio() {
        colTipo.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarTipo(cd.getValue().getTipo())));
        colPeriodo.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarPeriodo(cd.getValue())));
        colPrioridade.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarVigencia(cd.getValue())));
        colEstado.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarEstado(cd.getValue().getEstado())));
        colEstado.setCellFactory(col -> PreferenciaFormatters.criarCelulaBadgeEstado());
        colDescricao.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getDescricao()));
    }

    private void configurarTabelaPendentes() {
        colColaboradorPendente.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.obterNomeUtilizador(cd.getValue().getIdUtilizador())));
        colTipoPendente.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarTipo(cd.getValue().getTipo())));
        colPeriodoPendente.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarPeriodo(cd.getValue())));
        colPrioridadePendente.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarVigencia(cd.getValue())));
        colDescricaoPendente.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarDescricao(cd.getValue().getDescricao())));
    }

    private void configurarTabelaHistoricoDecisoes() {
        colColaboradorHistorico.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.obterNomeUtilizador(cd.getValue().getIdUtilizador())));
        colEstadoHistorico.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarEstado(cd.getValue().getEstado())));
        colEstadoHistorico.setCellFactory(col -> PreferenciaFormatters.criarCelulaBadgeEstado());
        colDecisaoHistorico.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarDecisao(cd.getValue().getDecisao())));
        colDecisorHistorico.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.obterNomeUtilizador(cd.getValue().getIdDecisor())));
        colDataDecisaoHistorico.setCellValueFactory(cd ->
                new SimpleStringProperty(PreferenciaFormatters.formatarDataDecisao(cd.getValue().getDataDecisao())));
    }

    // ── Actions and listeners ────────────────────────────────────────────────────

    private void configurarAcoes() {
        tabelaPreferencias.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) return;

            preferenciaEmEdicao = nova;
            lblTituloFormulario.setText("Editar Preferência");
            btnGuardarPreferencia.setText("Atualizar Preferência");
            btnCancelarEdicao.setDisable(false);

            cbTipo.setValue(PreferenciaFormatters.formatarTipo(nova.getTipo()));
            configurarTipoSelecionado();
            dpDataInicio.setValue(nova.getDataInicio());
            chkSemDataFim.setSelected(nova.getDataFim() == null && permitePreferenciaSemDataFim(nova.getTipo()));
            dpDataFim.setValue(nova.getDataFim());
            descricaoBuilder.preencherFormularioColegas(nova, colegasDaLoja);
            descricaoBuilder.preencherFormularioTurnos(nova);
            txtDescricao.setText(descricaoBuilder.obterNotaLivre(nova));
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

        cbTipo.valueProperty().addListener((obs, a, n) -> esconderFeedback());
        cbTipo.valueProperty().addListener((obs, a, n) -> configurarTipoSelecionado());
        dpDataInicio.valueProperty().addListener((obs, a, n) -> esconderFeedback());
        dpDataFim.valueProperty().addListener((obs, a, n) -> esconderFeedback());
        cbColegaPreferido.valueProperty().addListener((obs, a, n) -> esconderFeedback());
        cbDuracaoPreferida.valueProperty().addListener((obs, a, n) -> esconderFeedback());
        chkTurnoManha.selectedProperty().addListener((obs, a, n) -> esconderFeedback());
        chkTurnoIntermedio.selectedProperty().addListener((obs, a, n) -> esconderFeedback());
        chkTurnoNoite.selectedProperty().addListener((obs, a, n) -> esconderFeedback());
        chkSemDataFim.selectedProperty().addListener((obs, a, n) -> { atualizarEstadoDatas(); esconderFeedback(); });
        txtDescricao.textProperty().addListener((obs, a, n) -> esconderFeedback());

        tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()
                .addListener((obs, a, n) -> { txtDecisaoGestor.clear(); esconderFeedbackGestao(); });
        txtDecisaoGestor.textProperty().addListener((obs, a, n) -> esconderFeedbackGestao());
    }

    // ── Data loading ─────────────────────────────────────────────────────────────

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
        List<Preferencia> pendentes = preferenciaBLL.listarPreferenciasPendentesParaAprovacao(utilizadorLogado.getId());
        tabelaPreferenciasPendentes.setItems(FXCollections.observableArrayList(pendentes));
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

    // ── Form management ──────────────────────────────────────────────────────────

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

    // ── Decision handling ────────────────────────────────────────────────────────

    private void tratarDecisaoPreferencia(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            Preferencia selecionada = tabelaPreferenciasPendentes.getSelectionModel().getSelectedItem();
            if (selecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferência pendente primeiro.");
            }
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar preferência" : "Rejeitar preferência",
                    aprovar ? "Deseja aprovar esta preferência?" : "Deseja rejeitar esta preferência?",
                    aprovar ? "A decisão ficará registada para a loja." : "A rejeição ficará registada para a loja."
            )) {
                return;
            }

            if (aprovar) {
                preferenciaBLL.aprovarPreferencia(selecionada.getId(), utilizadorLogado.getId(), txtDecisaoGestor.getText());
                mostrarFeedbackGestao("Preferência aprovada com sucesso.", true);
            } else {
                preferenciaBLL.rejeitarPreferencia(selecionada.getId(), utilizadorLogado.getId(), txtDecisaoGestor.getText());
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

    // ── Feedback display ─────────────────────────────────────────────────────────

    private void mostrarFeedback(String mensagem, boolean sucesso) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().addAll("mensagem-feedback", sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
        if (sucesso) {
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            p.setOnFinished(e -> esconderFeedback());
            p.play();
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
        lblFeedbackGestao.getStyleClass().addAll("mensagem-feedback", sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedbackGestao.setVisible(true);
        lblFeedbackGestao.setManaged(true);
        if (sucesso) {
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            p.setOnFinished(e -> esconderFeedbackGestao());
            p.play();
        }
    }

    private void esconderFeedbackGestao() {
        lblFeedbackGestao.setVisible(false);
        lblFeedbackGestao.setManaged(false);
        lblFeedbackGestao.setText("");
    }

    // ── Business logic helpers ───────────────────────────────────────────────────

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferência.");
        }
        return switch (tipoSelecionado) {
            case "Folgas"         -> "folgas";
            case "Férias"         -> "ferias";
            case "Folga preferida"-> "folga_preferida";
            case "Colegas"        -> "colegas";
            case "Turnos"         -> "turnos";
            default -> throw new IllegalArgumentException("O tipo de preferência selecionado é inválido.");
        };
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
        cbColegaPreferido.setPromptText(colegasDaLoja.isEmpty() ? "Sem colegas disponíveis" : "Seleciona um colega");
    }

    private void configurarTipoSelecionado() {
        String tipoSelecionado = cbTipo.getValue();
        boolean tipoColegas  = "Colegas".equals(tipoSelecionado);
        boolean tipoTurnos   = "Turnos".equals(tipoSelecionado);
        boolean permiteSemFim = permitePreferenciaSemDataFim(tipoSelecionado);

        painelColega.setManaged(tipoColegas);
        painelColega.setVisible(tipoColegas);
        painelTurnos.setManaged(tipoTurnos);
        painelTurnos.setVisible(tipoTurnos);

        if (!tipoColegas) cbColegaPreferido.setValue(null);
        if (!tipoTurnos) {
            chkTurnoManha.setSelected(false);
            chkTurnoIntermedio.setSelected(false);
            chkTurnoNoite.setSelected(false);
            cbDuracaoPreferida.setValue("Indiferente");
        }

        chkSemDataFim.setDisable(!permiteSemFim);
        if (!permiteSemFim) chkSemDataFim.setSelected(false);

        if (tipoColegas) {
            txtDescricao.setPromptText("Se quiseres, acrescenta contexto adicional para esta preferência.");
        } else if ("Turnos".equals(tipoSelecionado)) {
            txtDescricao.setPromptText("Acrescenta contexto opcional, por exemplo: estudo de manha, prefiro fechos curtos ou quero evitar aberturas consecutivas.");
        } else if ("Folga preferida".equals(tipoSelecionado)) {
            txtDescricao.setPromptText("Folga recorrente semanal: escolhe a data inicial no dia da semana que preferes folgar. "
                    + "O algoritmo tenta muito respeitá-la (1/semana), mas pode escalar-te se for preciso para a cobertura.");
        } else if ("Folgas".equals(tipoSelecionado) || "Férias".equals(tipoSelecionado)) {
            txtDescricao.setPromptText("Explica a tua preferência com o contexto necessário para análise.");
        } else {
            txtDescricao.setPromptText("Explica a tua preferência com detalhe suficiente para ser analisada.");
        }

        atualizarEstadoDatas();
    }

    private boolean permitePreferenciaSemDataFim(String tipoSelecionado) {
        String n = PreferenciaFormatters.normalizarTipo(tipoSelecionado);
        return "folgas".equals(n) || "ferias".equals(n) || "folga preferida".equals(n)
                || "colegas".equals(n) || "turnos".equals(n);
    }

    private void atualizarEstadoDatas() {
        boolean semDataFim = chkSemDataFim.isSelected() && !chkSemDataFim.isDisable();
        dpDataFim.setDisable(semDataFim);
        if (semDataFim) {
            dpDataFim.setValue(null);
            dpDataFim.setPromptText("Sem data fim");
            if (dpDataInicio.getValue() == null) dpDataInicio.setValue(LocalDate.now());
        } else {
            dpDataFim.setPromptText("Opcional");
        }
    }

    private LocalDate resolverDataInicio(String tipoNormalizado) {
        LocalDate dataInicio = dpDataInicio.getValue();
        if (dataInicio != null) return dataInicio;
        if ("colegas".equals(tipoNormalizado) || "turnos".equals(tipoNormalizado)) return LocalDate.now();
        return null;
    }

    private LocalDate resolverDataFim(String tipoNormalizado) {
        if (chkSemDataFim.isSelected() && permitePreferenciaSemDataFim(tipoNormalizado)) return null;
        return dpDataFim.getValue();
    }

    private Window obterJanela() {
        if (painelFormulario == null || painelFormulario.getScene() == null) return null;
        return painelFormulario.getScene().getWindow();
    }
}
