package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Services.ExportacaoPdfService;
import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.DESKTOP.support.BackgroundTaskRunner;
import com.example.projeto2.DESKTOP.support.CalendarioMensalHelper;
import com.example.projeto2.DESKTOP.support.CalendarioSemanalHelper;
import com.example.projeto2.DESKTOP.support.DetalheDiaDialog;
import com.example.projeto2.DESKTOP.support.DiagnosticoGeracaoPanel;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.DESKTOP.support.EdicaoTurnoDialog;
import com.example.projeto2.DESKTOP.support.ExportadorHorarioCsv;
import com.example.projeto2.DESKTOP.support.ExportadorHorarioPdf;
import com.example.projeto2.DESKTOP.support.GeracaoStepperPanel;
import com.example.projeto2.DESKTOP.support.GeracaoTabelaConfigurador;
import com.example.projeto2.DESKTOP.support.HorarioIndividualDialog;
import com.example.projeto2.DESKTOP.support.MensagemErroFormatter;
import com.example.projeto2.DESKTOP.support.MesOption;
import com.example.projeto2.DESKTOP.support.SelecaoColaboradoresPainel;
import com.example.projeto2.DESKTOP.support.VistaGrelhaHorarioRender;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.YearMonth;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class GeracaoHorariosController {

    private static final DateTimeFormatter FORMATO_DIA_DETALHE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", java.util.Locale.forLanguageTag("pt-PT"));

    @FXML private Label lblLoja;
    @FXML private Label lblLocalizacao;
    @FXML private ComboBox<MesOption> cbMes;
    @FXML private Spinner<Integer> spAno;
    @FXML private Button btnVerProposta;
    @FXML private Button btnGerarProposta;
    @FXML private Button btnGerarAlternativas;
    @FXML private Spinner<Integer> spQuantidadeAlternativas;
    @FXML private VBox painelSelecaoColaboradores;
    @FXML private VBox boxColaboradoresGeracao;
    @FXML private Label lblResumoColaboradoresGeracao;
    @FXML private Button btnSelecionarTodosColaboradores;
    @FXML private Button btnLimparColaboradores;
    @FXML private Label lblFeedback;
    @FXML private VBox painelDiagnosticoGeracao;
    @FXML private Label lblDiagnosticoTitulo;
    @FXML private Label lblDiagnosticoResumo;
    @FXML private Label lblDiagnosticoPerfilRecomendado;
    @FXML private VBox boxDiagnosticoMotivos;
    @FXML private VBox boxDiagnosticoSugestoes;
    @FXML private VBox painelValidacaoSupervisor;
    @FXML private TextArea txtObservacoesSupervisor;
    @FXML private Button btnAprovarProposta;
    @FXML private Button btnRejeitarProposta;
    @FXML private Label lblFeedbackValidacao;
    @FXML private Label lblEstadoProposta;
    @FXML private Label lblOrigemPlaneamento;
    @FXML private Label lblGeradoPor;
    @FXML private Label lblDataGeracao;
    @FXML private Label lblDecididoPor;
    @FXML private Label lblDataDecisao;
    @FXML private Label lblObservacoesSupervisor;
    @FXML private Label lblResumoGeracao;
    @FXML private Label lblPoliticaOtimizacao;
    @FXML private Label lblPontuacaoOtimizacao;
    @FXML private Label lblDesvioCarga;
    @FXML private Label lblAmplitudeCarga;
    @FXML private Label lblTotalColaboradores;
    @FXML private Label lblTotalTurnos;
    @FXML private Label lblTotalDiasCobertos;
    @FXML private TableView<PropostaResumo> tabelaPropostas;
    @FXML private TableColumn<PropostaResumo, String> colPropostaRotulo;
    @FXML private TableColumn<PropostaResumo, String> colPropostaEstado;
    @FXML private TableColumn<PropostaResumo, String> colPropostaData;
    @FXML private TableColumn<PropostaResumo, String> colPropostaScore;
    @FXML private TableColumn<PropostaResumo, String> colPropostaQualidade;
    @FXML private TableColumn<PropostaResumo, String> colPropostaTurnos;
    @FXML private ComboBox<PropostaResumo> cbComparacaoBase;
    @FXML private ComboBox<PropostaResumo> cbComparacaoAlvo;
    @FXML private Button btnCompararPropostas;
    @FXML private Button btnEnviarSupervisor;
    @FXML private Label lblResumoComparacao;
    @FXML private TableView<DiferencaColaborador> tabelaComparacao;
    @FXML private TableColumn<DiferencaColaborador, String> colComparacaoColaborador;
    @FXML private TableColumn<DiferencaColaborador, String> colComparacaoBase;
    @FXML private TableColumn<DiferencaColaborador, String> colComparacaoAlvo;
    @FXML private TableColumn<DiferencaColaborador, String> colComparacaoDiferenca;
    @FXML private ComboBox<FiltroColaboradorOption> cbFiltroColaborador;
    @FXML private Label lblSemanaPlaneamento;
    @FXML private HBox boxSemanaPlaneamento;
    @FXML private VBox emptyStatePropostas;
    @FXML private VBox emptyStateDistribuicao;
    @FXML private VBox emptyStateCalendario;
    @FXML private TabPane tabPaneHorarios;
    @FXML private Button btnMesAnterior;
    @FXML private Button btnMesSeguinte;
    @FXML private Button btnExportarCsvHorario;
    @FXML private Button btnExportarPdfHorario;
    @FXML private Label lblMesAtual;
    @FXML private VBox emptyStateCalendarioMensal;
    @FXML private GridPane calendarioMensalGrid;
    @FXML private TableView<ResumoColaborador> tabelaResumoColaboradores;
    @FXML private TableColumn<ResumoColaborador, String> colResumoColaborador;
    @FXML private TableColumn<ResumoColaborador, String> colResumoCargo;
    @FXML private TableColumn<ResumoColaborador, String> colResumoTurnos;
    @FXML private TableColumn<ResumoColaborador, String> colResumoHoras;
    @FXML private VBox stepperPasso1;
    @FXML private VBox stepperPasso2;
    @FXML private VBox stepperPasso3;
    @FXML private VBox stepperPasso4;
    @FXML private Label lblGuiaFluxo;
    @FXML private Label lblPeriodoPropostas;
    @FXML private Label lblIdentificacaoHorario;
    @FXML private ComboBox<PropostaResumo> cbSelecaoProposta;
    @FXML private Button btnVistaCalendario;
    @FXML private Button btnVistaGrelha;
    @FXML private VBox painelVistaCalendario;
    @FXML private VBox painelVistaGrelha;
    @FXML private Button btnGrelhaSemana;
    @FXML private Button btnGrelhaMes;
    @FXML private Button btnGrelhaAnterior;
    @FXML private Button btnGrelhaSeguinte;
    @FXML private Label lblGrelhaPeriodo;
    @FXML private VBox emptyStateGrelha;
    @FXML private ScrollPane grelhaScrollPane;
    @FXML private VBox grelhaContainer;
    @FXML private VBox painelEnvioGerente;
    @FXML private Button btnPasso1Continuar;
    @FXML private Button btnPasso2Continuar;
    @FXML private Button btnPasso3Continuar;
    @FXML private Label lblResumoEnvio;

    private final GeracaoHorariosService geracaoHorariosBLL;
    private final ExportacaoPdfService exportacaoPdfBLL;
    private final HorarioService horarioBLL;

    private Utilizador utilizadorLogado;
    private PropostaResultado propostaAtual;
    private boolean podeGerar;
    private boolean podeValidar;
    private LocalDate semanaPlaneamentoInicio;
    private BackgroundTaskRunner taskRunner;
    private boolean suprimirCarregamentoPorSelecao;
    private YearMonth periodoMensalAtual = YearMonth.now();
    private SelecaoColaboradoresPainel selecaoColaboradoresPainel;
    private boolean grelhaVistaSemanais = true;
    private LocalDate grelhaDataInicio = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
    private VistaGrelhaHorarioRender vistaGrelhaRender;
    private DiagnosticoGeracaoPanel diagnosticoGeracaoPanel;
    private GeracaoStepperPanel stepperPanel;

    public GeracaoHorariosController(GeracaoHorariosService geracaoHorariosBLL,
                                     ExportacaoPdfService exportacaoPdfBLL,
                                     HorarioService horarioBLL) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.exportacaoPdfBLL = exportacaoPdfBLL;
        this.horarioBLL = horarioBLL;
    }

    @FXML
    public void initialize() {
        vistaGrelhaRender = new VistaGrelhaHorarioRender(
                grelhaContainer, grelhaScrollPane, emptyStateGrelha, lblGrelhaPeriodo,
                this::abrirDetalheDia);
        diagnosticoGeracaoPanel = new DiagnosticoGeracaoPanel(
                painelDiagnosticoGeracao, lblDiagnosticoTitulo, lblDiagnosticoResumo,
                lblDiagnosticoPerfilRecomendado, boxDiagnosticoMotivos, boxDiagnosticoSugestoes,
                this::obterJanela);
        taskRunner = new BackgroundTaskRunner(
                this::mostrarInformacao,
                this::atualizarEstadoInterativo,
                "geracao-horarios-worker");
        selecaoColaboradoresPainel = new SelecaoColaboradoresPainel(
                boxColaboradoresGeracao,
                lblResumoColaboradoresGeracao,
                this::atualizarEstadoInterativo);
        stepperPanel = new GeracaoStepperPanel(
                stepperPasso1, stepperPasso2, stepperPasso3, stepperPasso4,
                tabPaneHorarios, lblGuiaFluxo,
                this::onEntrarPasso2);

        configurarFiltros();
        configurarTabelasUI();
        limparResultado();
        esconderFeedback();
        esconderFeedbackValidacao();
        painelValidacaoSupervisor.setManaged(false);
        painelValidacaoSupervisor.setVisible(false);

        tabelaResumoColaboradores.setPlaceholder(new Label("Ainda não existe proposta gerada para apresentar o resumo da equipa."));
        tabelaPropostas.setPlaceholder(new Label("Gera alternativas para as comparar antes da validação."));
        tabelaComparacao.setPlaceholder(new Label("Seleciona duas propostas para comparar a distribuição por colaborador."));

        btnGerarProposta.setTooltip(new Tooltip("Gera uma alternativa com as definições atuais"));
        btnGerarAlternativas.setTooltip(new Tooltip("Gera várias alternativas em lote para comparação"));
        btnVerProposta.setTooltip(new Tooltip("Carrega o planeamento do período selecionado"));
        btnEnviarSupervisor.setTooltip(new Tooltip("Envia as alternativas selecionadas para validação do supervisor"));
        btnAprovarProposta.setTooltip(new Tooltip("Aprova e publica esta proposta — as restantes pendentes são rejeitadas automaticamente"));
        btnRejeitarProposta.setTooltip(new Tooltip("Rejeita esta proposta de horário"));

        configurarAtalhosRapidos();
        stepperPanel.configurarNavegacao();

        javafx.application.Platform.runLater(() -> {
            if (emptyStateCalendarioMensal != null) {
                emptyStateCalendarioMensal.setVisible(true);
                emptyStateCalendarioMensal.setManaged(true);
            }
            atualizarCalendarioMensal();
            stepperPanel.atualizar(false, podeGerar, propostaAtual,
                    !tabelaPropostas.getItems().isEmpty());
            stepperPanel.marcarPassoAtual(0);
        });
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarContextoInicial();
    }

    // ── @FXML handlers ────────────────────────────────────────────────────────

    @FXML
    public void onVerPropostaClick() {
        PropostaResumo propostaSelecionada = tabelaPropostas.getSelectionModel().getSelectedItem();
        if (propostaSelecionada != null && propostaSelecionada.idProposta() != null) {
            carregarPropostaPorIdEmSegundoPlano(propostaSelecionada.idProposta(), false);
            return;
        }
        carregarPlaneamentoDoPeriodo();
    }

    @FXML public void onGerarPropostaClick()     { gerarAlternativasEmSegundoPlano(1); }
    @FXML public void onGerarAlternativasClick() { gerarAlternativasEmSegundoPlano(spQuantidadeAlternativas.getValue()); }
    @FXML public void onSelecionarTodosColaboradoresClick() { selecaoColaboradoresPainel.selecionarTodos(); }
    @FXML public void onLimparColaboradoresClick()          { selecaoColaboradoresPainel.limparSelecao(); }
    @FXML public void onEnviarSupervisorClick()             { enviarPropostasSelecionadasEmSegundoPlano(); }
    @FXML public void onAprovarPropostaClick()  { decidirProposta(true); }
    @FXML public void onRejeitarPropostaClick() { decidirProposta(false); }

    @FXML
    public void onCompararPropostasClick() {
        try {
            PropostaResumo base = cbComparacaoBase.getValue();
            PropostaResumo alvo = cbComparacaoAlvo.getValue();
            if (base == null || alvo == null) {
                throw new IllegalArgumentException("Seleciona duas alternativas para comparar.");
            }
            if (base.idProposta().equals(alvo.idProposta())) {
                throw new IllegalArgumentException("Seleciona alternativas diferentes para a comparação.");
            }
            ComparacaoPropostas comparacao = geracaoHorariosBLL.compararPropostas(
                    utilizadorLogado.getId(), base.idProposta(), alvo.idProposta());
            lblResumoComparacao.setText(comparacao.resumo());
            tabelaComparacao.setItems(FXCollections.observableArrayList(comparacao.diferencas()));
        } catch (IllegalArgumentException e) {
            lblResumoComparacao.setText(e.getMessage());
            tabelaComparacao.setItems(FXCollections.observableArrayList());
        } catch (Exception e) {
            lblResumoComparacao.setText("Não foi possível comparar as alternativas selecionadas.");
            tabelaComparacao.setItems(FXCollections.observableArrayList());
        }
    }

    @FXML
    public void onSemanaPlaneamentoAnteriorClick() {
        semanaPlaneamentoInicio = semanaPlaneamentoInicio.minusWeeks(1);
        atualizarCabecalhoSemanaPlaneamento();
        aplicarFiltroColaborador();
    }

    @FXML
    public void onSemanaPlaneamentoSeguinteClick() {
        semanaPlaneamentoInicio = semanaPlaneamentoInicio.plusWeeks(1);
        atualizarCabecalhoSemanaPlaneamento();
        aplicarFiltroColaborador();
    }

    // ── Navegação do assistente (wizard) ──────────────────────────────────────

    @FXML public void onIrConfigurar()   { stepperPanel.irParaPasso(0); }
    @FXML public void onIrAlternativas() { stepperPanel.irParaPasso(1); }
    @FXML public void onIrRever()        { stepperPanel.irParaPasso(2); }
    @FXML public void onIrEnviar()       { stepperPanel.irParaPasso(3); }

    private void onEntrarPasso2() {
        atualizarCalendarioMensal();
        if (painelVistaGrelha != null && painelVistaGrelha.isVisible()) {
            construirVistaGrelha();
        }
    }

    // ── Handlers da Vista em Grelha ───────────────────────────────────────────

    @FXML public void onVistaCalendarioClick() { mudarVistaCalendario(true); }
    @FXML public void onVistaGrelhaClick()     { mudarVistaCalendario(false); construirVistaGrelha(); }

    @FXML
    public void onGrelhaSemanaClick() {
        if (grelhaVistaSemanais) return;
        grelhaVistaSemanais = true;
        grelhaDataInicio = grelhaDataInicio
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        atualizarEstiloToggleGrelha();
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaMesClick() {
        if (!grelhaVistaSemanais) return;
        grelhaVistaSemanais = false;
        grelhaDataInicio = grelhaDataInicio.withDayOfMonth(1);
        atualizarEstiloToggleGrelha();
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaAnteriorClick() {
        grelhaDataInicio = grelhaVistaSemanais
                ? grelhaDataInicio.minusWeeks(1)
                : grelhaDataInicio.minusMonths(1).withDayOfMonth(1);
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaSeguinteClick() {
        grelhaDataInicio = grelhaVistaSemanais
                ? grelhaDataInicio.plusWeeks(1)
                : grelhaDataInicio.plusMonths(1).withDayOfMonth(1);
        construirVistaGrelha();
    }

    // ── Navegação Calendário Mensal ───────────────────────────────────────────

    @FXML public void onMesAnteriorClick() { periodoMensalAtual = periodoMensalAtual.minusMonths(1); atualizarCalendarioMensal(); }
    @FXML public void onMesSeguinteClick() { periodoMensalAtual = periodoMensalAtual.plusMonths(1);  atualizarCalendarioMensal(); }

    @FXML
    public void onExportarCsvHorarioClick() {
        if (propostaAtual == null || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty()) {
            mostrarErro("Carrega primeiro uma proposta de horário para exportar.");
            return;
        }
        MesOption mesSelecionado = cbMes.getValue();
        int mes = mesSelecionado != null ? mesSelecionado.numero() : periodoMensalAtual.getMonthValue();
        int ano = spAno.getValue() != null ? spAno.getValue() : periodoMensalAtual.getYear();
        Window janela = btnExportarCsvHorario.getScene() != null
                ? btnExportarCsvHorario.getScene().getWindow() : null;
        ExportadorHorarioCsv.exportar(propostaAtual, mes, ano, janela, this::mostrarSucesso, this::mostrarErro);
    }

    @FXML
    public void onExportarPdfHorarioClick() {
        if (propostaAtual == null || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty()) {
            mostrarErro("Carrega primeiro uma proposta de horário para exportar.");
            return;
        }
        MesOption mesSelecionado = cbMes.getValue();
        int mes = mesSelecionado != null ? mesSelecionado.numero() : periodoMensalAtual.getMonthValue();
        int ano = spAno.getValue() != null ? spAno.getValue() : periodoMensalAtual.getYear();
        Window janela = btnExportarPdfHorario.getScene() != null
                ? btnExportarPdfHorario.getScene().getWindow() : null;
        ExportadorHorarioPdf.exportar(propostaAtual, mes, ano, janela, exportacaoPdfBLL,
                this::mostrarSucesso, this::mostrarErro);
    }

    // ── Configuração ──────────────────────────────────────────────────────────

    private void configurarFiltros() {
        cbMes.setItems(FXCollections.observableArrayList(MesOption.todos()));
        spAno.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear(), LocalDate.now().getYear() + 5, LocalDate.now().getYear()));
        spAno.setEditable(true);
        spQuantidadeAlternativas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3));
        spQuantidadeAlternativas.setEditable(true);

        cbMes.valueProperty().addListener((obs, ant, novo) -> onPeriodoAlterado());
        spAno.valueProperty().addListener((obs, ant, novo) -> onPeriodoAlterado());

        cbFiltroColaborador.setItems(FXCollections.observableArrayList(FiltroColaboradorOption.todos()));
        cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        cbFiltroColaborador.valueProperty().addListener((obs, ant, novo) -> {
            aplicarFiltroColaborador();
            atualizarCalendarioMensal();
        });
        reposicionarSemanaPlaneamentoParaMesSelecionado();
    }

    private void onPeriodoAlterado() {
        invalidarPropostaAtual();
        carregarColaboradoresElegiveis();
        if (utilizadorLogado != null && utilizadorLogado.getId() != null && !taskRunner.isRunning()) {
            carregarPlaneamentoDoPeriodo();
        }
    }

    private void configurarAtalhosRapidos() {
        lblLoja.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) return;
            nova.setOnKeyPressed(evento -> {
                if (!evento.isControlDown()) return;
                if (evento.getCode() == KeyCode.G)     { onGerarPropostaClick();     evento.consume(); }
                else if (evento.getCode() == KeyCode.L) { onGerarAlternativasClick(); evento.consume(); }
                else if (evento.getCode() == KeyCode.ENTER) { onVerPropostaClick();  evento.consume(); }
            });
        });
    }

    private void configurarTabelasUI() {
        GeracaoTabelaConfigurador.configurarResumoColaboradores(
                tabelaResumoColaboradores,
                colResumoColaborador, colResumoCargo, colResumoTurnos, colResumoHoras,
                colaborador -> { if (propostaAtual != null) mostrarHorarioIndividual(colaborador); }
        );
        GeracaoTabelaConfigurador.configurarPropostas(
                tabelaPropostas,
                colPropostaRotulo, colPropostaEstado, colPropostaData,
                colPropostaScore, colPropostaQualidade, colPropostaTurnos,
                () -> suprimirCarregamentoPorSelecao,
                id -> carregarPropostaPorIdEmSegundoPlano(id, false),
                this::atualizarEstadoInterativo
        );
        GeracaoTabelaConfigurador.configurarComparacao(
                colComparacaoColaborador, colComparacaoBase, colComparacaoAlvo, colComparacaoDiferenca,
                cbComparacaoBase, cbComparacaoAlvo, cbSelecaoProposta,
                this::atualizarEstadoInterativo,
                id -> propostaAtual == null || !id.equals(propostaAtual.idProposta()),
                id -> carregarPropostaPorIdEmSegundoPlano(id, false)
        );
    }

    // ── Lógica interna da Vista em Grelha ─────────────────────────────────────

    private void mudarVistaCalendario(boolean mostrarCalendario) {
        if (painelVistaCalendario != null) {
            painelVistaCalendario.setVisible(mostrarCalendario);
            painelVistaCalendario.setManaged(mostrarCalendario);
        }
        if (painelVistaGrelha != null) {
            painelVistaGrelha.setVisible(!mostrarCalendario);
            painelVistaGrelha.setManaged(!mostrarCalendario);
        }
        if (btnVistaCalendario != null) {
            btnVistaCalendario.getStyleClass().removeAll("btn-vista-ativo", "btn-vista-inativo");
            btnVistaCalendario.getStyleClass().add(mostrarCalendario ? "btn-vista-ativo" : "btn-vista-inativo");
        }
        if (btnVistaGrelha != null) {
            btnVistaGrelha.getStyleClass().removeAll("btn-vista-ativo", "btn-vista-inativo");
            btnVistaGrelha.getStyleClass().add(mostrarCalendario ? "btn-vista-inativo" : "btn-vista-ativo");
        }
    }

    private void atualizarEstiloToggleGrelha() {
        if (btnGrelhaSemana != null) {
            btnGrelhaSemana.getStyleClass().removeAll("btn-grelha-sub-ativo", "btn-grelha-sub-inativo");
            btnGrelhaSemana.getStyleClass().add(grelhaVistaSemanais ? "btn-grelha-sub-ativo" : "btn-grelha-sub-inativo");
        }
        if (btnGrelhaMes != null) {
            btnGrelhaMes.getStyleClass().removeAll("btn-grelha-sub-ativo", "btn-grelha-sub-inativo");
            btnGrelhaMes.getStyleClass().add(grelhaVistaSemanais ? "btn-grelha-sub-inativo" : "btn-grelha-sub-ativo");
        }
    }

    private void construirVistaGrelha() {
        List<HorarioLinha> linhas = propostaAtual != null ? propostaAtual.linhas() : null;
        vistaGrelhaRender.renderizar(grelhaVistaSemanais, grelhaDataInicio, linhas);
    }

    // ── Calendário Mensal ─────────────────────────────────────────────────────

    private void atualizarCalendarioMensal() {
        if (calendarioMensalGrid == null) return;
        String[] MESES_PT = {
            "Janeiro","Fevereiro","Marco","Abril","Maio","Junho",
            "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"
        };
        if (lblMesAtual != null) {
            lblMesAtual.setText(MESES_PT[periodoMensalAtual.getMonthValue() - 1] + " " + periodoMensalAtual.getYear());
        }
        java.util.Map<java.time.LocalDate, java.util.List<String>> eventosMes = obterEventosParaMes(periodoMensalAtual);
        boolean temEventos = eventosMes != null && !eventosMes.isEmpty()
                && eventosMes.values().stream().anyMatch(l -> !l.isEmpty());
        if (emptyStateCalendarioMensal != null) {
            emptyStateCalendarioMensal.setVisible(!temEventos);
            emptyStateCalendarioMensal.setManaged(!temEventos);
        }
        CalendarioMensalHelper.preencherCalendario(
                calendarioMensalGrid, periodoMensalAtual, eventosMes, "Sem turnos atribuidos", this::abrirDetalheDia);
    }

    private java.util.Map<java.time.LocalDate, java.util.List<String>> obterEventosParaMes(YearMonth periodo) {
        if (propostaAtual == null || propostaAtual.linhas() == null) return java.util.Collections.emptyMap();
        FiltroColaboradorOption filtro = (cbFiltroColaborador != null) ? cbFiltroColaborador.getValue() : null;
        Integer idColaboradorFiltro = (filtro != null && !filtro.isTodos()) ? filtro.idColaborador() : null;

        java.util.Map<java.time.LocalDate, java.util.List<String>> mapa = new java.util.LinkedHashMap<>();
        for (HorarioLinha linha : propostaAtual.linhas()) {
            if (linha == null || linha.data() == null) continue;
            if (idColaboradorFiltro != null && !idColaboradorFiltro.equals(linha.idColaborador())) continue;
            java.time.LocalDate data = linha.data();
            if (!YearMonth.from(data).equals(periodo)) continue;
            String desc = idColaboradorFiltro != null
                    ? (linha.periodo() != null ? linha.periodo() : "?") + " (" + (linha.cargo() != null ? linha.cargo() : "-") + ")"
                    : (linha.periodo() != null ? linha.periodo() : "?") + " | " + (linha.colaborador() != null ? linha.colaborador() : "") + " (" + (linha.cargo() != null ? linha.cargo() : "-") + ")";
            mapa.computeIfAbsent(data, k -> new java.util.ArrayList<>()).add(desc);
        }
        return mapa;
    }

    private void abrirDetalheDia(LocalDate data) {
        DetalheDiaDialog.abrir(
                data,
                propostaAtual != null ? propostaAtual.linhas() : null,
                obterJanela(),
                podeGerar,
                (turno, janelaBotao) -> abrirEdicaoTurno(turno, janelaBotao != null ? janelaBotao : obterJanela())
        );
    }

    private void mostrarHorarioIndividual(ResumoColaborador colaborador) {
        HorarioIndividualDialog.abrir(colaborador, propostaAtual != null ? propostaAtual.linhas() : null, obterJanela());
    }

    // ── Carregamento de dados ─────────────────────────────────────────────────

    private void carregarContextoInicial() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            GeracaoContexto contexto = geracaoHorariosBLL.obterContexto(utilizadorLogado.getId());
            podeGerar = contexto.podeGerar();
            podeValidar = contexto.podeValidar();
            lblLoja.setText(contexto.nomeLoja());
            lblLocalizacao.setText(contexto.localizacao());
            spAno.getValueFactory().setValue(contexto.anoAtual());
            selecionarMes(contexto.mesAtual());
            configurarPermissoesEcra();
            carregarColaboradoresElegiveis();
            carregarPlaneamentoDoPeriodo();
        } catch (IllegalArgumentException e) {
            bloquearEcraSemPermissao(e.getMessage());
        } catch (Exception e) {
            bloquearEcraSemPermissao("Não foi possível carregar o contexto da geração de horários.");
        }
    }

    private void carregarColaboradoresElegiveis() {
        if (!podeGerar || utilizadorLogado == null || utilizadorLogado.getId() == null
                || cbMes.getValue() == null || spAno.getValue() == null) {
            selecaoColaboradoresPainel.mostrar(List.of());
            return;
        }
        try {
            List<ColaboradorElegivel> colaboradores = geracaoHorariosBLL.listarColaboradoresElegiveis(
                    utilizadorLogado.getId(), spAno.getValue(), cbMes.getValue().numero());
            selecaoColaboradoresPainel.mostrar(colaboradores);
        } catch (IllegalArgumentException e) {
            selecaoColaboradoresPainel.mostrar(List.of());
            lblResumoColaboradoresGeracao.setText(e.getMessage());
        } catch (Exception e) {
            selecaoColaboradoresPainel.mostrar(List.of());
            lblResumoColaboradoresGeracao.setText("Não foi possível carregar a equipa elegível para este período.");
        }
    }

    private List<Integer> obterIdsColaboradoresSelecionados() {
        return selecaoColaboradoresPainel.idsSelecionados();
    }

    private void carregarListaPropostas(Integer idSelecionar) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null
                    || cbMes.getValue() == null || spAno.getValue() == null) {
                aplicarListaPropostas(List.of(), null);
                return;
            }
            List<PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                    utilizadorLogado.getId(), spAno.getValue(), cbMes.getValue().numero());
            aplicarListaPropostas(propostas, idSelecionar);
        } catch (Exception e) {
            aplicarListaPropostas(List.of(), null);
        }
    }

    private void selecionarPropostaNaTabela(Integer idProposta) {
        if (idProposta == null || tabelaPropostas.getItems() == null) return;
        suprimirCarregamentoPorSelecao = true;
        try {
            tabelaPropostas.getItems().stream()
                    .filter(proposta -> idProposta.equals(proposta.idProposta()))
                    .findFirst()
                    .ifPresent(proposta -> tabelaPropostas.getSelectionModel().select(proposta));
        } finally {
            suprimirCarregamentoPorSelecao = false;
        }
    }

    private void decidirProposta(boolean aprovar) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            if (propostaAtual == null || propostaAtual.idProposta() == null) {
                throw new IllegalArgumentException("Seleciona primeiro uma proposta para validar.");
            }
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar proposta" : "Rejeitar proposta",
                    aprovar ? "Deseja aprovar e publicar esta proposta?" : "Deseja rejeitar esta proposta?",
                    aprovar ? "Os horários serão publicados para a loja." : "A proposta ficará rejeitada e não será publicada."
            )) return;

            PropostaResultado resultado = aprovar
                    ? geracaoHorariosBLL.aprovarProposta(utilizadorLogado.getId(), propostaAtual.idProposta(), txtObservacoesSupervisor.getText())
                    : geracaoHorariosBLL.rejeitarProposta(utilizadorLogado.getId(), propostaAtual.idProposta(), txtObservacoesSupervisor.getText());

            preencherResultado(resultado);
            carregarListaPropostas(resultado.idProposta());
            mostrarFeedbackValidacao(
                    aprovar ? "Proposta aprovada e horários publicados com sucesso." : "Proposta rejeitada com sucesso.", true);
        } catch (IllegalArgumentException e) {
            mostrarFeedbackValidacao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedbackValidacao("Não foi possível atualizar a decisão da proposta.", false);
        }
    }

    private MesOption obterMesSelecionado() {
        MesOption mesSelecionado = cbMes.getValue();
        if (mesSelecionado == null) {
            throw new IllegalArgumentException("Seleciona um mês para consultar ou gerar a proposta.");
        }
        return mesSelecionado;
    }

    private void selecionarMes(int numeroMes) {
        cbMes.getItems().stream()
                .filter(item -> item.numero() == numeroMes)
                .findFirst()
                .ifPresent(cbMes::setValue);
    }

    // ── Preenchimento / limpeza de resultado ─────────────────────────────────

    private void preencherResultado(PropostaResultado resultado) {
        propostaAtual = resultado;
        periodoMensalAtual = YearMonth.of(resultado.ano(), resultado.mes());
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        lblEstadoProposta.setText(resultado.estado());
        lblOrigemPlaneamento.setText(resultado.origemPlaneamento());
        lblGeradoPor.setText(resultado.geradoPor());
        lblDataGeracao.setText(resultado.dataGeracao());
        lblDecididoPor.setText(resultado.decididoPor());
        lblDataDecisao.setText(resultado.dataDecisao());
        lblObservacoesSupervisor.setText(resultado.observacoesSupervisor());
        lblResumoGeracao.setText(resultado.resumoGeracao() != null ? resultado.resumoGeracao() : "-");
        lblPoliticaOtimizacao.setText(resultado.metricas().politicaOtimizacao());
        lblPontuacaoOtimizacao.setText(String.valueOf(resultado.metricas().pontuacao()));
        lblDesvioCarga.setText(resultado.metricas().desvioMedioHoras());
        lblAmplitudeCarga.setText(resultado.metricas().amplitudeHoras());
        lblTotalColaboradores.setText(String.valueOf(resultado.resumo().colaboradores()));
        lblTotalTurnos.setText(String.valueOf(resultado.resumo().turnos()));
        lblTotalDiasCobertos.setText(String.valueOf(resultado.resumo().diasCobertos()));

        String periodoTexto = resultado.nomeMes() + " " + resultado.ano();
        if (lblPeriodoPropostas != null) {
            lblPeriodoPropostas.setText("Propostas de " + periodoTexto);
        }
        if (lblIdentificacaoHorario != null) {
            String idProposta = resultado.idProposta() != null
                    ? "Proposta #" + resultado.idProposta() + " · " + periodoTexto + " · " + resultado.estado()
                    : "Horário publicado · " + periodoTexto;
            lblIdentificacaoHorario.setText("A visualizar: " + idProposta);
        }
        if (cbSelecaoProposta != null && resultado.idProposta() != null) {
            cbSelecaoProposta.getItems().stream()
                    .filter(p -> resultado.idProposta().equals(p.idProposta()))
                    .findFirst()
                    .ifPresent(cbSelecaoProposta::setValue);
        }

        tabelaResumoColaboradores.setItems(FXCollections.observableArrayList(resultado.resumoColaboradores()));
        atualizarFiltroColaborador(resultado);
        aplicarFiltroColaborador();
        atualizarCalendarioMensal();
        atualizarPainelValidacao();
        atualizarEstadoInterativo();

        grelhaDataInicio = LocalDate.of(resultado.ano(), resultado.mes(), 1);
        if (grelhaVistaSemanais) {
            grelhaDataInicio = grelhaDataInicio.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        }
        if (painelVistaGrelha != null && painelVistaGrelha.isVisible()) {
            construirVistaGrelha();
        }
    }

    private void limparResultado() {
        propostaAtual = null;
        lblEstadoProposta.setText("-");
        lblOrigemPlaneamento.setText("-");
        lblGeradoPor.setText("-");
        lblDataGeracao.setText("-");
        lblDecididoPor.setText("-");
        lblDataDecisao.setText("-");
        lblObservacoesSupervisor.setText("-");
        lblResumoGeracao.setText("Ainda não existe uma proposta carregada.");
        lblPoliticaOtimizacao.setText("-");
        lblPontuacaoOtimizacao.setText("-");
        lblDesvioCarga.setText("-");
        lblAmplitudeCarga.setText("-");
        txtObservacoesSupervisor.clear();
        lblTotalColaboradores.setText("0");
        lblTotalTurnos.setText("0");
        lblTotalDiasCobertos.setText("0");
        tabelaResumoColaboradores.setItems(FXCollections.observableArrayList());
        tabelaComparacao.setItems(FXCollections.observableArrayList());
        lblResumoComparacao.setText("Seleciona duas alternativas para comparar distribuição, carga horária e impacto por colaborador.");
        cbFiltroColaborador.setItems(FXCollections.observableArrayList(FiltroColaboradorOption.todos()));
        cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        if (lblPeriodoPropostas != null)   lblPeriodoPropostas.setText("Seleciona um mês e gera uma proposta");
        if (lblIdentificacaoHorario != null) lblIdentificacaoHorario.setText("Sem proposta carregada");
        if (cbSelecaoProposta != null)     cbSelecaoProposta.setValue(null);
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        renderizarCalendarioPlaneamento(List.of());
        atualizarCalendarioMensal();
        atualizarPainelValidacao();
        atualizarEstadoInterativo();
    }

    private void invalidarPropostaAtual() {
        if (utilizadorLogado == null) return;
        limparResultado();
        aplicarListaPropostas(List.of(), null);
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        esconderFeedback();
    }

    // ── Permissões e estado do ecrã ───────────────────────────────────────────

    private void bloquearEcraSemPermissao(String mensagem) {
        lblLoja.setText("-");
        lblLocalizacao.setText("-");
        limparResultado();
        cbMes.setDisable(true);
        spAno.setDisable(true);
        btnVerProposta.setDisable(true);
        btnGerarProposta.setDisable(true);
        btnGerarAlternativas.setDisable(true);
        spQuantidadeAlternativas.setDisable(true);
        painelSelecaoColaboradores.setManaged(false);
        painelSelecaoColaboradores.setVisible(false);
        btnEnviarSupervisor.setDisable(true);
        painelValidacaoSupervisor.setManaged(false);
        painelValidacaoSupervisor.setVisible(false);
        tabelaPropostas.setDisable(true);
        cbComparacaoBase.setDisable(true);
        cbComparacaoAlvo.setDisable(true);
        if (btnCompararPropostas != null) btnCompararPropostas.setDisable(true);
        mostrarErro(mensagem);
    }

    private void configurarPermissoesEcra() {
        btnGerarProposta.setManaged(podeGerar);
        btnGerarProposta.setVisible(podeGerar);
        btnGerarAlternativas.setManaged(podeGerar);
        btnGerarAlternativas.setVisible(podeGerar);
        spQuantidadeAlternativas.setDisable(!podeGerar);
        painelSelecaoColaboradores.setManaged(podeGerar);
        painelSelecaoColaboradores.setVisible(podeGerar);
        btnEnviarSupervisor.setManaged(podeGerar);
        btnEnviarSupervisor.setVisible(podeGerar);
        if (painelEnvioGerente != null) {
            painelEnvioGerente.setManaged(podeGerar);
            painelEnvioGerente.setVisible(podeGerar);
        }
        painelValidacaoSupervisor.setManaged(podeValidar);
        painelValidacaoSupervisor.setVisible(podeValidar);
        atualizarPainelValidacao();
        atualizarEstadoInterativo();
    }

    private void atualizarPainelValidacao() {
        if (!podeValidar) { atualizarEstadoInterativo(); return; }
        boolean podeDecidirAgora = propostaAtual != null && propostaAtual.podeSerDecidida();
        if (podeDecidirAgora) esconderFeedbackValidacao();
        else if (propostaAtual == null) txtObservacoesSupervisor.clear();
        atualizarEstadoInterativo();
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    private void mostrarSucesso(String mensagem)    { mostrarFeedback(mensagem, "mensagem-sucesso"); }
    private void mostrarErro(String mensagem)       { mostrarFeedback(mensagem, "mensagem-erro"); }
    private void mostrarInformacao(String mensagem) { mostrarFeedback(mensagem, null); }

    private void mostrarFeedback(String mensagem, String estilo) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-feedback", "mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        if (estilo != null) lblFeedback.getStyleClass().add(estilo);
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void esconderFeedback() {
        lblFeedback.setVisible(false);
        lblFeedback.setManaged(false);
        lblFeedback.setText("");
    }

    private void mostrarDiagnosticoGeracao(Throwable erro) { diagnosticoGeracaoPanel.mostrar(erro); }
    private void esconderDiagnosticoGeracao()              { diagnosticoGeracaoPanel.esconder(); }

    private void mostrarFeedbackValidacao(String mensagem, boolean sucesso) {
        lblFeedbackValidacao.setText(mensagem);
        lblFeedbackValidacao.getStyleClass().removeAll("mensagem-feedback", "mensagem-sucesso", "mensagem-erro");
        lblFeedbackValidacao.getStyleClass().add("mensagem-feedback");
        lblFeedbackValidacao.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedbackValidacao.setVisible(true);
        lblFeedbackValidacao.setManaged(true);
    }

    private void esconderFeedbackValidacao() {
        lblFeedbackValidacao.setVisible(false);
        lblFeedbackValidacao.setManaged(false);
        lblFeedbackValidacao.setText("");
    }

    // ── Filtros e calendário semanal ──────────────────────────────────────────

    private boolean tentarCarregarPlaneamentoExistente() {
        try {
            validarUtilizadorAutenticado();
            PropostaResultado existente = geracaoHorariosBLL.obterPlaneamento(
                    utilizadorLogado.getId(), spAno.getValue(), obterMesSelecionado().numero());
            if (existente == null) return false;
            List<PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                    utilizadorLogado.getId(), spAno.getValue(), obterMesSelecionado().numero());
            aplicarListaPropostas(propostas, existente.idProposta());
            preencherResultado(existente);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void atualizarFiltroColaborador(PropostaResultado resultado) {
        FiltroColaboradorOption selecionadoAnteriormente = cbFiltroColaborador.getValue();
        Integer idAnterior = selecionadoAnteriormente != null ? selecionadoAnteriormente.idColaborador() : null;

        var opcoes = FXCollections.<FiltroColaboradorOption>observableArrayList();
        opcoes.add(FiltroColaboradorOption.todos());
        resultado.resumoColaboradores().forEach(c -> opcoes.add(new FiltroColaboradorOption(c.idColaborador(), c.nome())));

        cbFiltroColaborador.setItems(opcoes);
        if (idAnterior != null) {
            cbFiltroColaborador.getItems().stream()
                    .filter(opcao -> idAnterior.equals(opcao.idColaborador()))
                    .findFirst()
                    .ifPresentOrElse(cbFiltroColaborador::setValue,
                            () -> cbFiltroColaborador.setValue(FiltroColaboradorOption.todos()));
        } else {
            cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        }
    }

    private void aplicarFiltroColaborador() {
        if (propostaAtual == null) { renderizarCalendarioPlaneamento(List.of()); return; }

        FiltroColaboradorOption filtro = cbFiltroColaborador.getValue();
        List<HorarioLinha> linhas = propostaAtual.linhas();
        if (filtro != null && !filtro.isTodos()) {
            linhas = linhas.stream().filter(l -> filtro.idColaborador().equals(l.idColaborador())).toList();
        }

        LocalDate dataInicioSemana = semanaPlaneamentoInicio != null
                ? semanaPlaneamentoInicio
                : CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), obterMesSelecionado().numero(), 1));
        LocalDate dataFimSemana = dataInicioSemana.plusDays(6);

        List<HorarioLinha> linhasDaSemana = linhas.stream()
                .filter(l -> l.data() != null && !l.data().isBefore(dataInicioSemana) && !l.data().isAfter(dataFimSemana))
                .toList();
        renderizarCalendarioPlaneamento(linhasDaSemana);
    }

    private void reposicionarSemanaPlaneamentoParaMesSelecionado() {
        try {
            semanaPlaneamentoInicio = (cbMes.getValue() == null || spAno.getValue() == null)
                    ? CalendarioSemanalHelper.inicioSemana(LocalDate.now())
                    : CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), cbMes.getValue().numero(), 1));
        } catch (Exception e) {
            semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        }
        atualizarCabecalhoSemanaPlaneamento();
    }

    private void atualizarCabecalhoSemanaPlaneamento() {
        if (lblSemanaPlaneamento != null) {
            lblSemanaPlaneamento.setText(CalendarioSemanalHelper.formatarIntervaloSemana(semanaPlaneamentoInicio));
        }
    }

    private void renderizarCalendarioPlaneamento(List<HorarioLinha> linhas) {
        Map<LocalDate, List<String>> eventos = new LinkedHashMap<>();
        for (HorarioLinha linha : linhas) {
            eventos.computeIfAbsent(linha.data(), k -> new ArrayList<>())
                    .add(linha.periodo() + " | " + linha.colaborador() + " (" + linha.cargo() + ")");
        }
        CalendarioSemanalHelper.preencherCalendario(boxSemanaPlaneamento, semanaPlaneamentoInicio, eventos, "Sem turnos");
    }

    // ── Operações em segundo plano ────────────────────────────────────────────

    private void carregarPlaneamentoDoPeriodo() {
        executarOperacaoEmSegundoPlano(
                "A carregar o planeamento do período selecionado...",
                () -> {
                    validarUtilizadorAutenticado();
                    MesOption mes = obterMesSelecionado();
                    List<PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                            utilizadorLogado.getId(), spAno.getValue(), mes.numero());
                    PropostaResultado resultado = geracaoHorariosBLL.obterPlaneamento(
                            utilizadorLogado.getId(), spAno.getValue(), mes.numero());
                    return new PlaneamentoCarregadoDados(propostas, resultado);
                },
                dados -> {
                    aplicarListaPropostas(dados.propostas(),
                            dados.resultado() != null ? dados.resultado().idProposta() : null);
                    if (dados.resultado() == null) {
                        limparResultado();
                        mostrarInformacao("Ainda não existe proposta nem horários publicados para o período selecionado.");
                        return;
                    }
                    preencherResultado(dados.resultado());
                    selecionarPropostaNaTabela(dados.resultado().idProposta());
                    mostrarInformacao("Planeamento carregado com sucesso. As alternativas ficam guardadas na tabela acima, dentro do próprio sistema.");
                },
                erro -> {
                    limparResultado();
                    mostrarErro(resolverMensagemErro(erro, "Não foi possível carregar o planeamento selecionado."));
                }
        );
    }

    private void carregarPropostaPorIdEmSegundoPlano(Integer idProposta, boolean atualizarSelecaoTabela) {
        if (idProposta == null) return;
        executarOperacaoEmSegundoPlano(
                "A carregar a alternativa selecionada...",
                () -> {
                    validarUtilizadorAutenticado();
                    return geracaoHorariosBLL.obterPropostaPorId(utilizadorLogado.getId(), idProposta);
                },
                resultado -> {
                    preencherResultado(resultado);
                    if (atualizarSelecaoTabela) selecionarPropostaNaTabela(idProposta);
                    esconderFeedback();
                },
                erro -> mostrarErro(resolverMensagemErro(erro, "Não foi possível carregar a alternativa selecionada."))
        );
    }

    private void enviarPropostasSelecionadasEmSegundoPlano() {
        try {
            validarUtilizadorAutenticado();
            List<Integer> idsPropostas = tabelaPropostas.getSelectionModel().getSelectedItems().stream()
                    .filter(this::propostaEmRascunho)
                    .map(PropostaResumo::idProposta)
                    .toList();
            if (idsPropostas.isEmpty()) {
                throw new IllegalArgumentException("Seleciona uma ou mais alternativas em rascunho para enviar ao supervisor.");
            }
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Enviar ao supervisor",
                    idsPropostas.size() == 1
                            ? "Deseja enviar a alternativa selecionada ao supervisor?"
                            : "Deseja enviar " + idsPropostas.size() + " alternativas ao supervisor?",
                    "Só as alternativas enviadas ficam disponíveis para aprovação ou rejeição."
            )) return;

            executarOperacaoEmSegundoPlano(
                    idsPropostas.size() == 1 ? "A enviar alternativa ao supervisor..." : "A enviar alternativas ao supervisor...",
                    () -> {
                        geracaoHorariosBLL.enviarPropostasParaValidacao(utilizadorLogado.getId(), idsPropostas);
                        MesOption mes = obterMesSelecionado();
                        List<PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                                utilizadorLogado.getId(), spAno.getValue(), mes.numero());
                        PropostaResultado enviada = geracaoHorariosBLL.obterPropostaPorId(
                                utilizadorLogado.getId(), idsPropostas.getFirst());
                        return new EnvioSupervisorDados(propostas, enviada, idsPropostas.size());
                    },
                    dados -> {
                        aplicarListaPropostas(dados.propostas(), dados.propostaSelecionada().idProposta());
                        preencherResultado(dados.propostaSelecionada());
                        selecionarPropostaNaTabela(dados.propostaSelecionada().idProposta());
                        mostrarSucesso(dados.totalEnviadas() == 1
                                ? "Alternativa enviada ao supervisor."
                                : dados.totalEnviadas() + " alternativas enviadas ao supervisor.");
                    },
                    erro -> mostrarErro(resolverMensagemErro(erro, "Não foi possível enviar as alternativas ao supervisor."))
            );
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void gerarAlternativasEmSegundoPlano(int quantidade) {
        try {
            validarUtilizadorAutenticado();
            List<Integer> idsColaboradoresSelecionados = obterIdsColaboradoresSelecionados();
            if (idsColaboradoresSelecionados.isEmpty()) {
                throw new IllegalArgumentException("Seleciona pelo menos um colaborador para gerar o horário.");
            }
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    quantidade == 1 ? "Gerar alternativa" : "Gerar alternativas",
                    quantidade == 1 ? "Deseja gerar uma nova alternativa?" : "Deseja gerar " + quantidade + " alternativas?",
                    "A geração vai usar " + idsColaboradoresSelecionados.size()
                            + " colaboradores selecionados. Depois podes enviar ao supervisor apenas as alternativas escolhidas."
            )) return;

            final DialogosHelper.CarregamentoHandle overlayCarregamento = DialogosHelper.mostrarCarregamento(
                    obterJanela(),
                    quantidade == 1 ? "A gerar o horário para o período selecionado..." : "A gerar " + quantidade + " alternativas de horário...");

            executarOperacaoEmSegundoPlano(
                    quantidade == 1 ? "A gerar uma alternativa para o mês selecionado..." : "A gerar " + quantidade + " alternativas para o mês selecionado...",
                    () -> {
                        MesOption mes = obterMesSelecionado();
                        List<PropostaResultado> resultados = geracaoHorariosBLL.gerarPropostas(
                                utilizadorLogado.getId(), spAno.getValue(), mes.numero(),
                                quantidade, idsColaboradoresSelecionados,
                                overlayCarregamento::atualizarMensagem);
                        PropostaResultado melhorResultado = resultados.stream()
                                .min(Comparator.comparingInt(r -> r.metricas().pontuacao()))
                                .orElse(resultados.getFirst());
                        List<PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                                utilizadorLogado.getId(), spAno.getValue(), mes.numero());
                        return new GeracaoAlternativasDados(propostas, melhorResultado, resultados.size());
                    },
                    dados -> {
                        overlayCarregamento.fechar();
                        aplicarListaPropostas(dados.propostas(), dados.melhorResultado().idProposta());
                        preencherResultado(dados.melhorResultado());
                        selecionarPropostaNaTabela(dados.melhorResultado().idProposta());
                        esconderFeedbackValidacao();
                        esconderDiagnosticoGeracao();
                        stepperPanel.irParaPasso(1);
                        String tituloNotif = dados.totalGeradas() == 1 ? "Horário gerado!" : dados.totalGeradas() + " alternativas geradas!";
                        String mensagemNotif = dados.totalGeradas() == 1
                                ? "Estás agora no passo 2 (Alternativas). Analisa a proposta, e usa o botão 'Rever proposta' para ver o calendário antes de enviar."
                                : "As " + dados.totalGeradas() + " alternativas estão no passo 2. A melhor pontuação ficou selecionada. Avança para 'Rever' e depois 'Enviar'.";
                        DialogosHelper.mostrarNotificacaoGeracao(obterJanela(), true, tituloNotif, mensagemNotif);
                        mostrarSucesso(dados.totalGeradas() == 1
                                ? "Alternativa gerada como rascunho."
                                : dados.totalGeradas() + " alternativas geradas. A melhor pontuação ficou selecionada para análise.");
                    },
                    erro -> {
                        overlayCarregamento.fechar();
                        String mensagem = resolverMensagemErro(erro, "Não foi possível gerar alternativas para o período selecionado.");
                        String mensagemCurta = mensagem.length() > 220 ? mensagem.substring(0, 217) + "..." : mensagem;
                        DialogosHelper.mostrarNotificacaoGeracao(obterJanela(), false, "Não foi possível gerar o horário", mensagemCurta);
                        stepperPanel.irParaPasso(0);
                        if (tentarCarregarPlaneamentoExistente()) {
                            esconderFeedbackValidacao();
                            mostrarErro(mensagem + " O planeamento atual foi carregado no passo Rever para facilitar a análise.");
                            mostrarDiagnosticoGeracao(erro);
                            return;
                        }
                        mostrarErro(mensagem);
                        mostrarDiagnosticoGeracao(erro);
                    }
            );
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        }
    }

    private void aplicarListaPropostas(List<PropostaResumo> propostas, Integer idSelecionar) {
        List<PropostaResumo> propostasSeguras = propostas != null ? propostas : List.of();
        tabelaPropostas.setItems(FXCollections.observableArrayList(propostasSeguras));
        cbComparacaoBase.setItems(FXCollections.observableArrayList(propostasSeguras));
        cbComparacaoAlvo.setItems(FXCollections.observableArrayList(propostasSeguras));
        if (cbSelecaoProposta != null) cbSelecaoProposta.setItems(FXCollections.observableArrayList(propostasSeguras));

        if (propostasSeguras.size() >= 2) {
            cbComparacaoBase.setValue(propostasSeguras.get(0));
            cbComparacaoAlvo.setValue(propostasSeguras.get(1));
        } else if (propostasSeguras.size() == 1) {
            cbComparacaoBase.setValue(propostasSeguras.get(0));
            cbComparacaoAlvo.setValue(null);
        } else {
            cbComparacaoBase.setValue(null);
            cbComparacaoAlvo.setValue(null);
        }

        if (idSelecionar != null) selecionarPropostaNaTabela(idSelecionar);
        atualizarEstadoInterativo();
    }

    private boolean propostaEmRascunho(PropostaResumo proposta) {
        return proposta != null && "rascunho".equals(normalizarTexto(proposta.estado()));
    }

    private boolean existePropostaEmRascunhoSelecionada() {
        return tabelaPropostas.getSelectionModel().getSelectedItems().stream().anyMatch(this::propostaEmRascunho);
    }

    // ── Estado interativo ─────────────────────────────────────────────────────

    private void atualizarEstadoInterativo() {
        boolean emProcessamento = taskRunner != null && taskRunner.isRunning();
        boolean contextoCarregado = utilizadorLogado != null && utilizadorLogado.getId() != null;

        cbMes.setDisable(!contextoCarregado || emProcessamento);
        spAno.setDisable(!contextoCarregado || emProcessamento);
        btnVerProposta.setDisable(!contextoCarregado || emProcessamento);
        btnGerarProposta.setDisable(!podeGerar || emProcessamento);
        btnGerarAlternativas.setDisable(!podeGerar || emProcessamento);
        spQuantidadeAlternativas.setDisable(!podeGerar || emProcessamento);
        painelSelecaoColaboradores.setDisable(!podeGerar || emProcessamento);
        btnSelecionarTodosColaboradores.setDisable(!podeGerar || emProcessamento || selecaoColaboradoresPainel.isVazio());
        btnLimparColaboradores.setDisable(!podeGerar || emProcessamento || selecaoColaboradoresPainel.isVazio());
        tabelaPropostas.setDisable(emProcessamento);
        cbComparacaoBase.setDisable(emProcessamento);
        cbComparacaoAlvo.setDisable(emProcessamento);
        btnEnviarSupervisor.setDisable(!podeGerar || emProcessamento || !existePropostaEmRascunhoSelecionada());
        if (btnCompararPropostas != null) {
            btnCompararPropostas.setDisable(emProcessamento
                    || cbComparacaoBase.getValue() == null || cbComparacaoAlvo.getValue() == null);
        }

        boolean podeDecidirAgora = podeValidar && propostaAtual != null && propostaAtual.podeSerDecidida();
        btnAprovarProposta.setDisable(emProcessamento || !podeDecidirAgora);
        btnRejeitarProposta.setDisable(emProcessamento || !podeDecidirAgora);
        txtObservacoesSupervisor.setDisable(emProcessamento || !podeDecidirAgora);

        boolean semDados = emProcessamento || propostaAtual == null
                || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty();
        if (btnExportarCsvHorario != null) btnExportarCsvHorario.setDisable(semDados);
        if (btnExportarPdfHorario != null) btnExportarPdfHorario.setDisable(semDados);

        boolean temPropostasLista = tabelaPropostas.getItems() != null && !tabelaPropostas.getItems().isEmpty();
        boolean temPropostaSelecionada = propostaAtual != null;
        if (btnPasso1Continuar != null) btnPasso1Continuar.setDisable(emProcessamento || !temPropostasLista);
        if (btnPasso2Continuar != null) btnPasso2Continuar.setDisable(emProcessamento || !temPropostaSelecionada);
        if (btnPasso3Continuar != null) btnPasso3Continuar.setDisable(emProcessamento || !temPropostaSelecionada);

        if (lblResumoEnvio != null) {
            var selecionadas = tabelaPropostas.getSelectionModel().getSelectedItems();
            long emRascunho = selecionadas == null ? 0 : selecionadas.stream().filter(this::propostaEmRascunho).count();
            if (emRascunho == 0)      lblResumoEnvio.setText("Nenhum rascunho selecionado. Volta ao passo 2 (Alternativas) e seleciona a(s) alternativa(s) que queres enviar.");
            else if (emRascunho == 1) lblResumoEnvio.setText("Vais enviar 1 alternativa ao supervisor.");
            else                      lblResumoEnvio.setText("Vais enviar " + emRascunho + " alternativas ao supervisor.");
        }

        atualizarEmptyStates();
        stepperPanel.atualizar(emProcessamento, podeGerar, propostaAtual, temPropostasLista);
    }

    private void atualizarEmptyStates() {
        boolean temPropostas = tabelaPropostas.getItems() != null && !tabelaPropostas.getItems().isEmpty();
        alternarEmptyState(emptyStatePropostas, tabelaPropostas, temPropostas);

        boolean temDistribuicao = tabelaResumoColaboradores.getItems() != null && !tabelaResumoColaboradores.getItems().isEmpty();
        alternarEmptyState(emptyStateDistribuicao, tabelaResumoColaboradores, temDistribuicao);

        alternarEmptyState(emptyStateCalendario, boxSemanaPlaneamento, propostaAtual != null);
    }

    private void alternarEmptyState(javafx.scene.Node emptyState, javafx.scene.Node conteudo, boolean temConteudo) {
        if (emptyState != null) { emptyState.setVisible(!temConteudo); emptyState.setManaged(!temConteudo); }
        if (conteudo != null)   { conteudo.setVisible(temConteudo);    conteudo.setManaged(temConteudo); }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private <T> void executarOperacaoEmSegundoPlano(String mensagemProcessamento,
                                                    Callable<T> acao,
                                                    Consumer<T> onSuccess,
                                                    Consumer<Throwable> onFailure) {
        taskRunner.executar(mensagemProcessamento, acao, onSuccess, onFailure);
    }

    private void validarUtilizadorAutenticado() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    private String resolverMensagemErro(Throwable erro, String fallback) {
        return MensagemErroFormatter.resolver(erro, fallback);
    }

    private Window obterJanela() {
        if (lblLoja == null || lblLoja.getScene() == null) return null;
        return lblLoja.getScene().getWindow();
    }

    private void abrirEdicaoTurno(HorarioLinha linha, Window owner) {
        EdicaoTurnoDialog.abrir(
                linha, owner, horarioBLL,
                utilizadorLogado != null ? utilizadorLogado.getId() : null,
                this::mostrarSucesso, this::mostrarErro,
                () -> {
                    if (propostaAtual != null && propostaAtual.idProposta() != null) {
                        carregarPropostaPorIdEmSegundoPlano(propostaAtual.idProposta(), false);
                    }
                }
        );
    }

    // ── Records privados ──────────────────────────────────────────────────────

    private record PlaneamentoCarregadoDados(List<PropostaResumo> propostas, PropostaResultado resultado) {}
    private record GeracaoAlternativasDados(List<PropostaResumo> propostas, PropostaResultado melhorResultado, int totalGeradas) {}
    private record EnvioSupervisorDados(List<PropostaResumo> propostas, PropostaResultado propostaSelecionada, int totalEnviadas) {}

    private record FiltroColaboradorOption(Integer idColaborador, String nome) {
        private static FiltroColaboradorOption todos() { return new FiltroColaboradorOption(null, "Toda a equipa"); }
        private boolean isTodos() { return idColaborador == null; }
        @Override public String toString() { return nome; }
    }
}
