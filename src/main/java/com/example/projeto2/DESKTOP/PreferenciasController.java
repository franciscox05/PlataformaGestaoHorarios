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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    @FXML private VBox painelFolgaPreferida;
    @FXML private ComboBox<String> cbDiaSemana;
    @FXML private HBox painelDatas;
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

    private static final Map<String, DayOfWeek> DIAS_SEMANA = Map.of(
            "Segunda-feira", DayOfWeek.MONDAY,
            "Terça-feira",   DayOfWeek.TUESDAY,
            "Quarta-feira",  DayOfWeek.WEDNESDAY,
            "Quinta-feira",  DayOfWeek.THURSDAY,
            "Sexta-feira",   DayOfWeek.FRIDAY,
            "Sábado",        DayOfWeek.SATURDAY,
            "Domingo",       DayOfWeek.SUNDAY
    );

    private static final Map<DayOfWeek, String> DIA_PARA_LABEL = Map.of(
            DayOfWeek.MONDAY,    "Segunda-feira",
            DayOfWeek.TUESDAY,   "Terça-feira",
            DayOfWeek.WEDNESDAY, "Quarta-feira",
            DayOfWeek.THURSDAY,  "Quinta-feira",
            DayOfWeek.FRIDAY,    "Sexta-feira",
            DayOfWeek.SATURDAY,  "Sábado",
            DayOfWeek.SUNDAY,    "Domingo"
    );

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
        cbTipo.setItems(FXCollections.observableArrayList("Folga preferida", "Colegas", "Turnos"));
        cbDiaSemana.setItems(FXCollections.observableArrayList(
                "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
                "Sexta-feira", "Sábado", "Domingo"));
        cbDuracaoPreferida.setItems(FXCollections.observableArrayList("Indiferente", "Mais curto", "Mais longo"));
        cbDuracaoPreferida.setValue("Indiferente");

        configurarTabelaHistoricoProprio();
        configurarAcoes();
        limparFormulario();
        esconderFeedback();

        cbTipo.setTooltip(new Tooltip("Tipo de preferência: folga preferida (dia de descanso semanal), colegas ou turnos"));
        dpDataInicio.setTooltip(new Tooltip("Data a partir da qual a preferência é válida"));
        dpDataFim.setTooltip(new Tooltip("Data limite de validade (deixa vazio para permanente)"));
        txtDescricao.setTooltip(new Tooltip("Notas adicionais para o gestor (opcional)"));

        tabelaPreferencias.setPlaceholder(new Label("Ainda não tens preferências registadas."));

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

        painelTurnos.setManaged(false);
        painelTurnos.setVisible(false);

        painelFolgaPreferida.setManaged(false);
        painelFolgaPreferida.setVisible(false);

        descricaoBuilder = new PreferenciaDescricaoBuilder(
                cbColegaPreferido, chkTurnoManha, chkTurnoIntermedio, chkTurnoNoite, cbDuracaoPreferida);
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarColegasDaLoja();
        carregarPreferencias();
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
            if ("folga_preferida".equalsIgnoreCase(nova.getTipo()) && nova.getDataInicio() != null) {
                cbDiaSemana.setValue(DIA_PARA_LABEL.get(nova.getDataInicio().getDayOfWeek()));
            } else {
                dpDataInicio.setValue(nova.getDataInicio());
                chkSemDataFim.setSelected(nova.getDataFim() == null && permitePreferenciaSemDataFim(nova.getTipo()));
                dpDataFim.setValue(nova.getDataFim());
            }
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

    // ── Form management ──────────────────────────────────────────────────────────

    private void limparFormulario() {
        preferenciaEmEdicao = null;
        lblTituloFormulario.setText("Nova Preferência");
        btnGuardarPreferencia.setText("Guardar Preferência");
        btnCancelarEdicao.setDisable(true);

        cbTipo.setValue(null);
        cbColegaPreferido.setValue(null);
        cbDiaSemana.setValue(null);
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
        painelFolgaPreferida.setManaged(false);
        painelFolgaPreferida.setVisible(false);
        painelDatas.setManaged(true);
        painelDatas.setVisible(true);
        dpDataFim.setDisable(false);
        dpDataFim.setPromptText("Opcional");
        txtDescricao.setPromptText("Explica a tua preferência com detalhe suficiente para ser analisada.");
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

    // ── Business logic helpers ───────────────────────────────────────────────────

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferência.");
        }
        return switch (tipoSelecionado) {
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
        boolean tipoColegas       = "Colegas".equals(tipoSelecionado);
        boolean tipoTurnos        = "Turnos".equals(tipoSelecionado);
        boolean tipoFolgaPreferida = "Folga preferida".equals(tipoSelecionado);
        boolean permiteSemFim = permitePreferenciaSemDataFim(tipoSelecionado);

        painelColega.setManaged(tipoColegas);
        painelColega.setVisible(tipoColegas);
        painelTurnos.setManaged(tipoTurnos);
        painelTurnos.setVisible(tipoTurnos);
        painelFolgaPreferida.setManaged(tipoFolgaPreferida);
        painelFolgaPreferida.setVisible(tipoFolgaPreferida);
        painelDatas.setManaged(!tipoFolgaPreferida);
        painelDatas.setVisible(!tipoFolgaPreferida);

        if (!tipoColegas) cbColegaPreferido.setValue(null);
        if (!tipoTurnos) {
            chkTurnoManha.setSelected(false);
            chkTurnoIntermedio.setSelected(false);
            chkTurnoNoite.setSelected(false);
            cbDuracaoPreferida.setValue("Indiferente");
        }
        if (!tipoFolgaPreferida) cbDiaSemana.setValue(null);

        chkSemDataFim.setDisable(!permiteSemFim || tipoFolgaPreferida);
        if (!permiteSemFim || tipoFolgaPreferida) chkSemDataFim.setSelected(false);

        if (tipoColegas) {
            txtDescricao.setPromptText("Se quiseres, acrescenta contexto adicional para esta preferência.");
        } else if (tipoTurnos) {
            txtDescricao.setPromptText("Acrescenta contexto opcional, por exemplo: estudo de manha, prefiro fechos curtos ou quero evitar aberturas consecutivas.");
        } else if (tipoFolgaPreferida) {
            txtDescricao.setPromptText("Notas adicionais (opcional).");
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
        if ("folga_preferida".equals(tipoNormalizado)) {
            String diaSelecionado = cbDiaSemana.getValue();
            if (diaSelecionado == null || diaSelecionado.isBlank()) {
                throw new IllegalArgumentException("Seleciona o dia da semana preferido para a folga.");
            }
            DayOfWeek dia = DIAS_SEMANA.get(diaSelecionado);
            if (dia == null) throw new IllegalArgumentException("Dia da semana inválido.");
            return LocalDate.now().with(TemporalAdjusters.nextOrSame(dia));
        }
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
