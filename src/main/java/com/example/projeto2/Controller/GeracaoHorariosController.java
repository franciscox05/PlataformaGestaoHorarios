package com.example.projeto2.Controller;

import com.example.projeto2.BLL.ExportacaoPdfBLL;
import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.Controller.support.CalendarioMensalHelper;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.YearMonth;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@Component
@Scope("prototype")
public class    GeracaoHorariosController {

    private static final DateTimeFormatter FORMATO_DIA_DETALHE =
            DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", java.util.Locale.forLanguageTag("pt-PT"));

    @FXML
    private Label lblLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private ComboBox<MesOption> cbMes;

    @FXML
    private Spinner<Integer> spAno;

    @FXML
    private Button btnVerProposta;

    @FXML
    private Button btnGerarProposta;

    @FXML
    private Button btnGerarAlternativas;

    @FXML
    private Spinner<Integer> spQuantidadeAlternativas;

    @FXML
    private VBox painelSelecaoColaboradores;

    @FXML
    private VBox boxColaboradoresGeracao;

    @FXML
    private Label lblResumoColaboradoresGeracao;

    @FXML
    private Button btnSelecionarTodosColaboradores;

    @FXML
    private Button btnLimparColaboradores;

    @FXML
    private Label lblFeedback;

    @FXML
    private VBox painelDiagnosticoGeracao;

    @FXML
    private Label lblDiagnosticoTitulo;

    @FXML
    private Label lblDiagnosticoResumo;

    @FXML
    private Label lblDiagnosticoPerfilRecomendado;

    @FXML
    private VBox boxDiagnosticoMotivos;

    @FXML
    private VBox boxDiagnosticoSugestoes;

    @FXML
    private VBox painelValidacaoSupervisor;

    @FXML
    private TextArea txtObservacoesSupervisor;

    @FXML
    private Button btnAprovarProposta;

    @FXML
    private Button btnRejeitarProposta;

    @FXML
    private Label lblFeedbackValidacao;

    @FXML
    private Label lblEstadoProposta;

    @FXML
    private Label lblOrigemPlaneamento;

    @FXML
    private Label lblGeradoPor;

    @FXML
    private Label lblDataGeracao;

    @FXML
    private Label lblDecididoPor;

    @FXML
    private Label lblDataDecisao;

    @FXML
    private Label lblObservacoesSupervisor;

    @FXML
    private Label lblResumoGeracao;

    @FXML
    private Label lblPoliticaOtimizacao;

    @FXML
    private Label lblPontuacaoOtimizacao;

    @FXML
    private Label lblDesvioCarga;

    @FXML
    private Label lblAmplitudeCarga;

    @FXML
    private Label lblTotalColaboradores;

    @FXML
    private Label lblTotalTurnos;

    @FXML
    private Label lblTotalDiasCobertos;

    @FXML
    private TableView<GeracaoHorariosBLL.PropostaResumo> tabelaPropostas;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaRotulo;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaEstado;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaData;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaScore;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaQualidade;

    @FXML
    private TableColumn<GeracaoHorariosBLL.PropostaResumo, String> colPropostaTurnos;

    @FXML
    private ComboBox<GeracaoHorariosBLL.PropostaResumo> cbComparacaoBase;

    @FXML
    private ComboBox<GeracaoHorariosBLL.PropostaResumo> cbComparacaoAlvo;

    @FXML
    private Button btnCompararPropostas;

    @FXML
    private Button btnEnviarSupervisor;

    @FXML
    private Label lblResumoComparacao;

    @FXML
    private TableView<GeracaoHorariosBLL.DiferencaColaborador> tabelaComparacao;

    @FXML
    private TableColumn<GeracaoHorariosBLL.DiferencaColaborador, String> colComparacaoColaborador;

    @FXML
    private TableColumn<GeracaoHorariosBLL.DiferencaColaborador, String> colComparacaoBase;

    @FXML
    private TableColumn<GeracaoHorariosBLL.DiferencaColaborador, String> colComparacaoAlvo;

    @FXML
    private TableColumn<GeracaoHorariosBLL.DiferencaColaborador, String> colComparacaoDiferenca;

    @FXML
    private ComboBox<FiltroColaboradorOption> cbFiltroColaborador;

    @FXML
    private Label lblSemanaPlaneamento;

    @FXML
    private HBox boxSemanaPlaneamento;

    // ── Empty States (fx:id FXML declarados no FXML mas em falta no controller — fix crash) ──────
    @FXML
    private VBox emptyStatePropostas;

    @FXML
    private VBox emptyStateDistribuicao;

    @FXML
    private VBox emptyStateCalendario;

    // ── TabPane raiz (adicionado na reestruturação em abas) ──────────────────
    @FXML
    private TabPane tabPaneHorarios;

    // ── Calendário Mensal (adicionado na reestruturação visual) ──────────────
    @FXML
    private Button btnMesAnterior;

    @FXML
    private Button btnMesSeguinte;

    @FXML
    private Button btnExportarCsvHorario;

    @FXML
    private Button btnExportarPdfHorario;

    @FXML
    private Label lblMesAtual;

    @FXML
    private VBox emptyStateCalendarioMensal;

    @FXML
    private GridPane calendarioMensalGrid;

    @FXML
    private TableView<GeracaoHorariosBLL.ResumoColaborador> tabelaResumoColaboradores;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoColaborador;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoCargo;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoTurnos;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoHoras;

    // ── Stepper de fluxo (dinâmico) ──────────────────────────────────────────
    @FXML
    private VBox stepperPasso1;

    @FXML
    private VBox stepperPasso2;

    @FXML
    private VBox stepperPasso3;

    @FXML
    private VBox stepperPasso4;

    // ── Label de guia de fluxo (contextual) ──────────────────────────────────
    @FXML
    private Label lblGuiaFluxo;

    // ── Labels de identificação do contexto (período e proposta) ─────────────
    @FXML
    private Label lblPeriodoPropostas;

    @FXML
    private Label lblIdentificacaoHorario;

    // ── Seletor de proposta no calendário mensal ──────────────────────────────
    @FXML
    private ComboBox<GeracaoHorariosBLL.PropostaResumo> cbSelecaoProposta;

    // ── Vista em Grelha ───────────────────────────────────────────────────────
    @FXML
    private Button btnVistaCalendario;

    @FXML
    private Button btnVistaGrelha;

    @FXML
    private VBox painelVistaCalendario;

    @FXML
    private VBox painelVistaGrelha;

    @FXML
    private Button btnGrelhaSemana;

    @FXML
    private Button btnGrelhaMes;

    @FXML
    private Button btnGrelhaAnterior;

    @FXML
    private Button btnGrelhaSeguinte;

    @FXML
    private Label lblGrelhaPeriodo;

    @FXML
    private VBox emptyStateGrelha;

    @FXML
    private ScrollPane grelhaScrollPane;

    @FXML
    private VBox grelhaContainer;

    // ── Navegação do assistente (wizard) ──────────────────────────────────────
    @FXML
    private VBox painelEnvioGerente;

    @FXML
    private Button btnPasso1Continuar;

    @FXML
    private Button btnPasso2Continuar;

    @FXML
    private Button btnPasso3Continuar;

    @FXML
    private Label lblResumoEnvio;

    private final GeracaoHorariosBLL geracaoHorariosBLL;
    private final ExportacaoPdfBLL exportacaoPdfBLL;
    private final HorarioBLL horarioBLL;

    private Utilizador utilizadorLogado;
    private GeracaoHorariosBLL.PropostaResultado propostaAtual;
    private boolean podeGerar;
    private boolean podeValidar;
    private LocalDate semanaPlaneamentoInicio;
    private Task<?> operacaoEmCurso;
    private boolean suprimirCarregamentoPorSelecao;
    private YearMonth periodoMensalAtual = YearMonth.now();
    private final Map<Integer, CheckBox> selecaoColaboradoresGeracao = new LinkedHashMap<>();

    // ── Estado da vista em grelha ─────────────────────────────────────────────
    private boolean grelhaVistaSemanais = true;          // true=semana, false=mês
    private LocalDate grelhaDataInicio = LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY);           // início da semana atual

    public GeracaoHorariosController(GeracaoHorariosBLL geracaoHorariosBLL,
                                     ExportacaoPdfBLL exportacaoPdfBLL,
                                     HorarioBLL horarioBLL) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.exportacaoPdfBLL = exportacaoPdfBLL;
        this.horarioBLL = horarioBLL;
    }

    @FXML
    public void initialize() {
        configurarFiltros();
        configurarTabelas();
        configurarTabelaPropostas();
        configurarComparacao();
        limparResultado();
        esconderFeedback();
        esconderFeedbackValidacao();
        painelValidacaoSupervisor.setManaged(false);
        painelValidacaoSupervisor.setVisible(false);

        tabelaResumoColaboradores.setPlaceholder(new Label("Ainda não existe proposta gerada para apresentar o resumo da equipa."));
        tabelaPropostas.setPlaceholder(new Label("Gera alternativas para as comparar antes da validação."));
        tabelaComparacao.setPlaceholder(new Label("Seleciona duas propostas para comparar a distribuição por colaborador."));

        // Tooltips nos botões de ação
        btnGerarProposta.setTooltip(new Tooltip("Gera uma alternativa com as definições atuais"));
        btnGerarAlternativas.setTooltip(new Tooltip("Gera várias alternativas em lote para comparação"));
        btnVerProposta.setTooltip(new Tooltip("Carrega o planeamento do período selecionado"));
        btnEnviarSupervisor.setTooltip(new Tooltip("Envia as alternativas selecionadas para validação do supervisor"));
        btnAprovarProposta.setTooltip(new Tooltip("Aprova e publica esta proposta — as restantes pendentes são rejeitadas automaticamente"));
        btnRejeitarProposta.setTooltip(new Tooltip("Rejeita esta proposta de horário"));
        configurarAtalhosRapidos();
        configurarNavegacaoWizard();
        // Inicializar o estado do calendário mensal (empty state visível, grid oculto até proposta)
        javafx.application.Platform.runLater(() -> {
            if (emptyStateCalendarioMensal != null) {
                emptyStateCalendarioMensal.setVisible(true);
                emptyStateCalendarioMensal.setManaged(true);
            }
            atualizarCalendarioMensal();
            atualizarStepper(false);
            marcarPassoAtual(0);
        });
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarContextoInicial();
    }

    @FXML
    public void onVerPropostaClick() {
        GeracaoHorariosBLL.PropostaResumo propostaSelecionada = tabelaPropostas.getSelectionModel().getSelectedItem();
        if (propostaSelecionada != null && propostaSelecionada.idProposta() != null) {
            carregarPropostaPorIdEmSegundoPlano(propostaSelecionada.idProposta(), false);
            return;
        }
        carregarPlaneamentoDoPeriodo();
    }

    @FXML
    public void onGerarPropostaClick() {
        gerarAlternativasEmSegundoPlano(1);
    }

    @FXML
    public void onGerarAlternativasClick() {
        gerarAlternativasEmSegundoPlano(spQuantidadeAlternativas.getValue());
    }

    @FXML
    public void onSelecionarTodosColaboradoresClick() {
        selecaoColaboradoresGeracao.values().forEach(checkBox -> checkBox.setSelected(true));
        atualizarResumoSelecaoColaboradores();
    }

    @FXML
    public void onLimparColaboradoresClick() {
        selecaoColaboradoresGeracao.values().forEach(checkBox -> checkBox.setSelected(false));
        atualizarResumoSelecaoColaboradores();
    }

    @FXML
    public void onEnviarSupervisorClick() {
        enviarPropostasSelecionadasEmSegundoPlano();
    }

    @FXML
    public void onCompararPropostasClick() {
        try {
            GeracaoHorariosBLL.PropostaResumo base = cbComparacaoBase.getValue();
            GeracaoHorariosBLL.PropostaResumo alvo = cbComparacaoAlvo.getValue();
            if (base == null || alvo == null) {
                throw new IllegalArgumentException("Seleciona duas alternativas para comparar.");
            }
            if (base.idProposta().equals(alvo.idProposta())) {
                throw new IllegalArgumentException("Seleciona alternativas diferentes para a comparação.");
            }

            GeracaoHorariosBLL.ComparacaoPropostas comparacao = geracaoHorariosBLL.compararPropostas(
                    utilizadorLogado.getId(),
                    base.idProposta(),
                    alvo.idProposta()
            );
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
    public void onAprovarPropostaClick() {
        decidirProposta(true);
    }

    @FXML
    public void onRejeitarPropostaClick() {
        decidirProposta(false);
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

    // ── Navegação do assistente (wizard) ────────────────────────────────────────

    @FXML
    public void onIrConfigurar()   { irParaPasso(0); }
    @FXML
    public void onIrAlternativas() { irParaPasso(1); }
    @FXML
    public void onIrRever()        { irParaPasso(2); }
    @FXML
    public void onIrEnviar()       { irParaPasso(3); }

    /** Navega para um passo do assistente (0..3) selecionando o separador correspondente. */
    private void irParaPasso(int indice) {
        if (tabPaneHorarios == null) return;
        if (indice < 0 || indice >= tabPaneHorarios.getTabs().size()) return;
        tabPaneHorarios.getSelectionModel().select(indice);
        // o listener de selectedIndex trata do destaque e dos refresh por passo
    }

    /** Destaca no stepper o passo atualmente visível. */
    private void marcarPassoAtual(int indice) {
        VBox[] passos = { stepperPasso1, stepperPasso2, stepperPasso3, stepperPasso4 };
        for (int i = 0; i < passos.length; i++) {
            if (passos[i] == null) continue;
            passos[i].getStyleClass().remove("stepper-passo-atual");
            if (i == indice) {
                passos[i].getStyleClass().add("stepper-passo-atual");
            }
        }
    }

    /** Liga os cliques do stepper à navegação e arranca o destaque do passo atual. */
    private void configurarNavegacaoWizard() {
        VBox[] passos = { stepperPasso1, stepperPasso2, stepperPasso3, stepperPasso4 };
        for (int i = 0; i < passos.length; i++) {
            if (passos[i] == null) continue;
            final int idx = i;
            passos[i].setOnMouseClicked(e -> irParaPasso(idx));
        }
        if (tabPaneHorarios != null) {
            tabPaneHorarios.getSelectionModel().selectedIndexProperty().addListener(
                    (obs, antigo, novo) -> {
                        int idx = novo.intValue();
                        marcarPassoAtual(idx);
                        // Ao entrar no passo Rever, garantir que o calendário/grelha estão atualizados
                        if (idx == 2) {
                            atualizarCalendarioMensal();
                            if (painelVistaGrelha != null && painelVistaGrelha.isVisible()) {
                                construirVistaGrelha();
                            }
                        }
                    });
            marcarPassoAtual(tabPaneHorarios.getSelectionModel().getSelectedIndex());
        }
    }

    // ── Handlers da Vista em Grelha ─────────────────────────────────────────────

    @FXML
    public void onVistaCalendarioClick() {
        mudarVistaCalendario(true);
    }

    @FXML
    public void onVistaGrelhaClick() {
        mudarVistaCalendario(false);
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaSemanaClick() {
        if (grelhaVistaSemanais) return;
        grelhaVistaSemanais = true;
        // Reposicionar para a semana que contém o primeiro dia do período mensal
        grelhaDataInicio = grelhaDataInicio
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        atualizarEstiloToggleGrelha();
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaMesClick() {
        if (!grelhaVistaSemanais) return;
        grelhaVistaSemanais = false;
        // Reposicionar para o início do mês da semana atual
        grelhaDataInicio = grelhaDataInicio.withDayOfMonth(1);
        atualizarEstiloToggleGrelha();
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaAnteriorClick() {
        if (grelhaVistaSemanais) {
            grelhaDataInicio = grelhaDataInicio.minusWeeks(1);
        } else {
            grelhaDataInicio = grelhaDataInicio.minusMonths(1).withDayOfMonth(1);
        }
        construirVistaGrelha();
    }

    @FXML
    public void onGrelhaSeguinteClick() {
        if (grelhaVistaSemanais) {
            grelhaDataInicio = grelhaDataInicio.plusWeeks(1);
        } else {
            grelhaDataInicio = grelhaDataInicio.plusMonths(1).withDayOfMonth(1);
        }
        construirVistaGrelha();
    }

    // ── Lógica interna da Vista em Grelha ──────────────────────────────────────

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

    // Cores para os avatares dos colaboradores (ciclam pela lista)
    private static final String[] AVATAR_CORES = {
        "#dc2626", "#2563eb", "#7c3aed", "#059669",
        "#d97706", "#db2777", "#0891b2", "#65a30d",
        "#0f172a", "#9333ea", "#ea580c", "#0284c7"
    };
    private final Map<Integer, String> grelhaAvatarCores = new LinkedHashMap<>();

    private void construirVistaGrelha() {
        if (grelhaContainer == null) return;
        grelhaContainer.getChildren().clear();

        // Determinar intervalo de datas
        LocalDate inicio;
        LocalDate fim;
        if (grelhaVistaSemanais) {
            inicio = grelhaDataInicio.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            fim = inicio.plusDays(6);
        } else {
            YearMonth ym = YearMonth.of(grelhaDataInicio.getYear(), grelhaDataInicio.getMonth());
            inicio = ym.atDay(1);
            fim = ym.atEndOfMonth();
        }

        // Atualizar label do período
        java.time.format.DateTimeFormatter fmtDia = java.time.format.DateTimeFormatter.ofPattern(
                "d MMM", java.util.Locale.forLanguageTag("pt-PT"));
        java.time.format.DateTimeFormatter fmtMes = java.time.format.DateTimeFormatter.ofPattern(
                "MMMM yyyy", java.util.Locale.forLanguageTag("pt-PT"));
        if (lblGrelhaPeriodo != null) {
            String periodoTexto = grelhaVistaSemanais
                    ? fmtDia.format(inicio) + " – " + fmtDia.format(fim)
                    : capitalizar(YearMonth.from(inicio).format(fmtMes));
            // conta colaboradores visíveis
            long nPessoas = propostaAtual != null && propostaAtual.linhas() != null
                    ? propostaAtual.linhas().stream()
                        .filter(l -> l != null && l.data() != null
                            && !l.data().isBefore(inicio) && !l.data().isAfter(fim))
                        .map(GeracaoHorariosBLL.HorarioLinha::idColaborador)
                        .distinct().count()
                    : 0;
            lblGrelhaPeriodo.setText(periodoTexto + (nPessoas > 0 ? "   · " + nPessoas + " pessoas" : ""));
        }

        // Verificar se há dados
        boolean temDados = propostaAtual != null
                && propostaAtual.linhas() != null
                && !propostaAtual.linhas().isEmpty();

        if (emptyStateGrelha != null) {
            emptyStateGrelha.setVisible(!temDados);
            emptyStateGrelha.setManaged(!temDados);
        }
        if (grelhaScrollPane != null) {
            grelhaScrollPane.setVisible(temDados);
            grelhaScrollPane.setManaged(temDados);
        }
        if (!temDados) return;

        // Lista de dias no intervalo
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) dias.add(d);

        // Agrupar linhas por colaborador (turno() = tipo, periodo() = horário HH:MM-HH:MM)
        Map<Integer, String> nomesColab  = new LinkedHashMap<>();
        Map<Integer, String> cargosColab = new LinkedHashMap<>();
        // mapa: idColab → { data → HorarioLinha }
        Map<Integer, Map<LocalDate, GeracaoHorariosBLL.HorarioLinha>> porColaborador = new LinkedHashMap<>();

        for (GeracaoHorariosBLL.HorarioLinha linha : propostaAtual.linhas()) {
            if (linha == null || linha.data() == null) continue;
            Integer id = linha.idColaborador();
            nomesColab.put(id, linha.colaborador() != null ? linha.colaborador() : "?");
            cargosColab.put(id, linha.cargo() != null ? linha.cargo() : "");
            porColaborador.computeIfAbsent(id, k -> new LinkedHashMap<>()).put(linha.data(), linha);
        }

        if (porColaborador.isEmpty()) {
            if (emptyStateGrelha != null) { emptyStateGrelha.setVisible(true); emptyStateGrelha.setManaged(true); }
            if (grelhaScrollPane != null) { grelhaScrollPane.setVisible(false); grelhaScrollPane.setManaged(false); }
            return;
        }

        // Atribuir cores de avatar estáveis
        int corIdx = 0;
        for (Integer id : porColaborador.keySet()) {
            grelhaAvatarCores.putIfAbsent(id, AVATAR_CORES[corIdx % AVATAR_CORES.length]);
            corIdx++;
        }

        LocalDate hoje = LocalDate.now();

        // ── LINHA DE CABEÇALHO ──
        HBox headerRow = new HBox();
        headerRow.getStyleClass().add("grelha-header-row");

        javafx.scene.control.Label headerColab = new javafx.scene.control.Label("COLABORADOR");
        headerColab.getStyleClass().add("grelha-header-colab");
        headerRow.getChildren().add(headerColab);

        for (LocalDate dia : dias) {
            VBox hDia = new VBox();
            hDia.getStyleClass().add("grelha-header-dia");
            hDia.setAlignment(Pos.CENTER);
            hDia.setSpacing(2);
            boolean fds = dia.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                    || dia.getDayOfWeek() == java.time.DayOfWeek.SUNDAY;
            boolean eHoje = dia.equals(hoje);
            if (fds) hDia.getStyleClass().add("grelha-header-dia-fim-semana");

            javafx.scene.control.Label lblSem = new javafx.scene.control.Label(
                    diaSemanaAbrev(dia.getDayOfWeek()).toUpperCase(java.util.Locale.ROOT));
            lblSem.getStyleClass().add("grelha-header-dia-sem");

            // Número do dia — hoje fica dentro de um círculo vermelho
            if (eHoje) {
                javafx.scene.layout.StackPane circulo = new javafx.scene.layout.StackPane();
                circulo.setMinSize(34, 34); circulo.setPrefSize(34, 34); circulo.setMaxSize(34, 34);
                circulo.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 50%;");
                javafx.scene.control.Label lblNumHoje = new javafx.scene.control.Label(
                        String.valueOf(dia.getDayOfMonth()));
                lblNumHoje.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: white;");
                circulo.getChildren().add(lblNumHoje);
                hDia.getChildren().addAll(lblSem, circulo);
            } else {
                javafx.scene.control.Label lblNum = new javafx.scene.control.Label(
                        String.valueOf(dia.getDayOfMonth()));
                lblNum.getStyleClass().add("grelha-header-dia-num");
                hDia.getChildren().addAll(lblSem, lblNum);
            }
            headerRow.getChildren().add(hDia);
        }
        grelhaContainer.getChildren().add(headerRow);

        // ── LINHAS POR COLABORADOR ──
        boolean alternado = false;
        for (Map.Entry<Integer, Map<LocalDate, GeracaoHorariosBLL.HorarioLinha>> entry
                : porColaborador.entrySet()) {
            Integer idColab = entry.getKey();
            Map<LocalDate, GeracaoHorariosBLL.HorarioLinha> diaParaLinha = entry.getValue();

            HBox empRow = new HBox();
            empRow.getStyleClass().add("grelha-employee-row");
            if (alternado) empRow.getStyleClass().add("grelha-employee-row-alt");
            alternado = !alternado;

            String corAvatar = grelhaAvatarCores.getOrDefault(idColab, "#6b7280");
            HBox infoCell = construirCelulaColaborador(
                    nomesColab.get(idColab), cargosColab.get(idColab), corAvatar);
            empRow.getChildren().add(infoCell);

            for (LocalDate dia : dias) {
                GeracaoHorariosBLL.HorarioLinha linha = diaParaLinha.get(dia);
                // turno() = nome tipo (Manhã/Tarde/Noite/Folga)
                // periodo() = horário ("09:00 - 15:00") — só existe para turnos, não folgas
                String tipoTurno = (linha != null && linha.turno() != null) ? linha.turno() : null;
                String horasTurno = (linha != null && linha.periodo() != null
                        && !"-".equals(linha.periodo())) ? linha.periodo() : null;
                javafx.scene.layout.StackPane diaCell = construirCelulaDia(
                        tipoTurno, horasTurno, dia.getDayOfWeek(), dia.equals(hoje));
                empRow.getChildren().add(diaCell);
            }
            grelhaContainer.getChildren().add(empRow);
        }
    }

    private HBox construirCelulaColaborador(String nome, String cargo, String corAvatar) {
        HBox cell = new HBox(10);
        cell.getStyleClass().add("grelha-employee-info");
        cell.setAlignment(Pos.CENTER_LEFT);

        // Avatar colorido com iniciais
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.getStyleClass().add("grelha-avatar");
        avatar.setStyle("-fx-background-color: " + corAvatar + ";");
        javafx.scene.control.Label lblIniciais = new javafx.scene.control.Label(gerarIniciais(nome));
        lblIniciais.getStyleClass().add("grelha-avatar-iniciais");
        avatar.getChildren().add(lblIniciais);

        // Nome + cargo
        VBox nomeBox = new VBox(2);
        nomeBox.setAlignment(Pos.CENTER_LEFT);
        javafx.scene.control.Label lblNome = new javafx.scene.control.Label(
                nome != null ? nome : "?");
        lblNome.getStyleClass().add("grelha-employee-nome");
        lblNome.setMaxWidth(128);
        javafx.scene.control.Label lblCargo = new javafx.scene.control.Label(
                cargo != null ? cargo : "");
        lblCargo.getStyleClass().add("grelha-employee-cargo");
        nomeBox.getChildren().addAll(lblNome, lblCargo);

        cell.getChildren().addAll(avatar, nomeBox);
        return cell;
    }

    /** Constrói a célula de um dia: card colorido com nome do turno + horas abaixo.
     *  tipoTurno = "Manhã" / "Tarde" / "Noite" / "Folga" (ou null = sem dados)
     *  horasTurno = "09:00 - 15:00" (ou null para folgas/sem dados)
     */
    private javafx.scene.layout.StackPane construirCelulaDia(
            String tipoTurno,
            String horasTurno,
            java.time.DayOfWeek diaSemana,
            boolean eHoje) {

        javafx.scene.layout.StackPane cell = new javafx.scene.layout.StackPane();
        cell.getStyleClass().add("grelha-dia-cell");

        boolean fds = diaSemana == java.time.DayOfWeek.SATURDAY
                || diaSemana == java.time.DayOfWeek.SUNDAY;
        if (fds) cell.getStyleClass().add("grelha-dia-cell-fim-semana");
        if (eHoje) cell.setStyle("-fx-background-color: #fff5f5;");

        // Sem dados para este dia → mostrar Folga
        if (tipoTurno == null || tipoTurno.isBlank() || "-".equals(tipoTurno)) {
            tipoTurno = "Folga";
            horasTurno = null;
        }

        String chave = turnoChave(tipoTurno);

        // Card interior
        VBox card = new VBox(3);
        card.getStyleClass().addAll("grelha-turno-card", "grelha-turno-card-" + chave);
        card.setAlignment(Pos.CENTER);

        javafx.scene.control.Label lblNome = new javafx.scene.control.Label(
                turnoNomeDisplay(tipoTurno));
        lblNome.getStyleClass().addAll("grelha-turno-nome", "grelha-turno-nome-" + chave);

        card.getChildren().add(lblNome);

        // Horas formatadas (ex: "09:00 - 15:00" → "09-15")
        if (horasTurno != null && !horasTurno.isBlank() && !"folga".equals(chave)) {
            String horasFormatadas = formatarHorasGrelha(horasTurno);
            javafx.scene.control.Label lblHora = new javafx.scene.control.Label(horasFormatadas);
            lblHora.getStyleClass().addAll("grelha-turno-hora", "grelha-turno-hora-" + chave);
            card.getChildren().add(lblHora);
        }

        cell.getChildren().add(card);
        return cell;
    }

    /** Extrai a chave CSS do nome do turno: "manha", "tarde", "noite", "folga", "intermedio", "outro" */
    private String turnoChave(String tipo) {
        if (tipo == null) return "outro";
        String p = Normalizer.normalize(tipo.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return switch (p) {
            case "manha"      -> "manha";
            case "tarde"      -> "tarde";
            case "noite"      -> "noite";
            case "folga"      -> "folga";
            case "intermedio" -> "intermedio";
            default           -> "outro";
        };
    }

    /** Nome do turno para mostrar no card (com acentos corretos) */
    private String turnoNomeDisplay(String tipo) {
        if (tipo == null) return "–";
        String chave = turnoChave(tipo);
        return switch (chave) {
            case "manha"      -> "Manhã";
            case "tarde"      -> "Tarde";
            case "noite"      -> "Noite";
            case "folga"      -> "Folga";
            case "intermedio" -> "Interm.";
            default           -> tipo.length() > 8 ? tipo.substring(0, 7) + "." : tipo;
        };
    }

    /** Formata "09:00 - 15:00" → "09-15", "09:30 - 15:30" → "09:30-15:30" */
    private String formatarHorasGrelha(String horas) {
        if (horas == null) return "";
        // Remove espaços e normaliza separador
        String s = horas.trim().replace(" ", "").replace("–", "-");
        // Remove :00 de cada parte se ambas terminarem em :00
        String[] partes = s.split("-", 2);
        if (partes.length == 2) {
            String p1 = partes[0].endsWith(":00") ? partes[0].replace(":00", "") : partes[0];
            String p2 = partes[1].endsWith(":00") ? partes[1].replace(":00", "") : partes[1];
            return p1 + "-" + p2;
        }
        return s;
    }

    private String diaSemanaAbrev(java.time.DayOfWeek dow) {
        return switch (dow) {
            case MONDAY    -> "Seg";
            case TUESDAY   -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY  -> "Qui";
            case FRIDAY    -> "Sex";
            case SATURDAY  -> "Sáb";
            case SUNDAY    -> "Dom";
        };
    }

    private String gerarIniciais(String nome) {
        if (nome == null || nome.isBlank()) return "?";
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1)
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase(java.util.Locale.ROOT);
        return (String.valueOf(partes[0].charAt(0))
                + partes[partes.length - 1].charAt(0)).toUpperCase(java.util.Locale.ROOT);
    }

    // ── Navegação Calendário Mensal ─────────────────────────────────────────────
    @FXML
    public void onMesAnteriorClick() {
        periodoMensalAtual = periodoMensalAtual.minusMonths(1);
        atualizarCalendarioMensal();
    }

    @FXML
    public void onMesSeguinteClick() {
        periodoMensalAtual = periodoMensalAtual.plusMonths(1);
        atualizarCalendarioMensal();
    }

    @FXML
    public void onExportarCsvHorarioClick() {
        if (propostaAtual == null || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty()) {
            mostrarErro("Carrega primeiro uma proposta de horário para exportar.");
            return;
        }

        String[] MESES_PT = {
            "janeiro","fevereiro","marco","abril","maio","junho",
            "julho","agosto","setembro","outubro","novembro","dezembro"
        };
        MesOption mesSelecionado = cbMes.getValue();
        int mes = mesSelecionado != null ? mesSelecionado.numero() : periodoMensalAtual.getMonthValue();
        int ano = spAno.getValue() != null ? spAno.getValue() : periodoMensalAtual.getYear();
        String nomeMes = MESES_PT[mes - 1];

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar horário mensal");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        fileChooser.setInitialFileName("horario-" + ano + "-" + nomeMes + ".csv");

        Window janela = btnExportarCsvHorario.getScene() != null
                ? btnExportarCsvHorario.getScene().getWindow() : null;
        java.io.File ficheiro = fileChooser.showSaveDialog(janela);
        if (ficheiro == null) return;

        try (BufferedWriter writer = Files.newBufferedWriter(ficheiro.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Loja;" + propostaAtual.origemPlaneamento()
                    + ";Mês;" + nomeMes + ";Ano;" + ano);
            writer.newLine();
            writer.write("Proposta;" + sanitizarCsv(propostaAtual.geradoPor())
                    + ";Estado;" + sanitizarCsv(propostaAtual.estado()));
            writer.newLine();
            writer.newLine();
            writer.write("Colaborador;Cargo;Data;Dia Semana;Período;Turno;Estado");
            writer.newLine();

            List<GeracaoHorariosBLL.HorarioLinha> linhasOrdenadas = propostaAtual.linhas().stream()
                    .sorted(Comparator.comparing(GeracaoHorariosBLL.HorarioLinha::data,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(GeracaoHorariosBLL.HorarioLinha::colaborador,
                            Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            for (GeracaoHorariosBLL.HorarioLinha linha : linhasOrdenadas) {
                writer.write(sanitizarCsv(linha.colaborador()) + ";"
                        + sanitizarCsv(linha.cargo()) + ";"
                        + (linha.data() != null ? linha.data().toString() : "") + ";"
                        + sanitizarCsv(linha.diaSemana()) + ";"
                        + sanitizarCsv(linha.periodo()) + ";"
                        + sanitizarCsv(linha.turno()) + ";"
                        + sanitizarCsv(linha.estado()));
                writer.newLine();
            }

            mostrarSucesso("Horário exportado com sucesso.");
        } catch (IOException e) {
            mostrarErro("Não foi possível exportar o ficheiro CSV.");
        }
    }

    private String sanitizarCsv(String valor) {
        if (valor == null) return "";
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }

    @FXML
    public void onExportarPdfHorarioClick() {
        if (propostaAtual == null || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty()) {
            mostrarErro("Carrega primeiro uma proposta de horário para exportar.");
            return;
        }

        String[] MESES_PT = {
            "janeiro","fevereiro","marco","abril","maio","junho",
            "julho","agosto","setembro","outubro","novembro","dezembro"
        };
        MesOption mesSelecionado = cbMes.getValue();
        int mes = mesSelecionado != null ? mesSelecionado.numero() : periodoMensalAtual.getMonthValue();
        int ano = spAno.getValue() != null ? spAno.getValue() : periodoMensalAtual.getYear();
        String nomeMes = MESES_PT[mes - 1];
        String nomeMesCapitalizado = Character.toUpperCase(nomeMes.charAt(0)) + nomeMes.substring(1);

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar horário mensal para PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fileChooser.setInitialFileName("horario-" + ano + "-" + nomeMes + ".pdf");

        Window janela = btnExportarPdfHorario.getScene() != null
                ? btnExportarPdfHorario.getScene().getWindow() : null;
        java.io.File ficheiro = fileChooser.showSaveDialog(janela);
        if (ficheiro == null) return;

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(ficheiro)) {
            exportacaoPdfBLL.exportarHorarioPdf(
                    fos,
                    propostaAtual.origemPlaneamento(),
                    nomeMesCapitalizado + " " + ano,
                    ano,
                    propostaAtual.estado(),
                    propostaAtual.geradoPor(),
                    propostaAtual.linhas()
            );
            mostrarSucesso("PDF exportado com sucesso.");
        } catch (IOException e) {
            mostrarErro("Não foi possível exportar o ficheiro PDF.");
        }
    }

    private void atualizarCalendarioMensal() {
        if (calendarioMensalGrid == null) return;
        String[] MESES_PT = {
            "Janeiro","Fevereiro","Marco","Abril","Maio","Junho",
            "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"
        };
        if (lblMesAtual != null) {
            lblMesAtual.setText(MESES_PT[periodoMensalAtual.getMonthValue() - 1]
                + " " + periodoMensalAtual.getYear());
        }
        java.util.Map<java.time.LocalDate, java.util.List<String>> eventosMes =
            obterEventosParaMes(periodoMensalAtual);
        boolean temEventos = eventosMes != null && !eventosMes.isEmpty()
            && eventosMes.values().stream().anyMatch(l -> !l.isEmpty());
        if (emptyStateCalendarioMensal != null) {
            emptyStateCalendarioMensal.setVisible(!temEventos);
            emptyStateCalendarioMensal.setManaged(!temEventos);
        }
        CalendarioMensalHelper.preencherCalendario(
            calendarioMensalGrid,
            periodoMensalAtual,
            eventosMes,
            "Sem turnos atribuidos",
            this::abrirDetalheDia
        );
    }

    /** Devolve os eventos (turnos) do mes para o helper do calendario.
     *  Retorna mapa vazio se nao houver proposta carregada.
     *  Aplica o filtro de colaborador selecionado em cbFiltroColaborador. */
    private java.util.Map<java.time.LocalDate, java.util.List<String>> obterEventosParaMes(YearMonth periodo) {
        if (propostaAtual == null || propostaAtual.linhas() == null) {
            return java.util.Collections.emptyMap();
        }
        // Determinar se há filtro de colaborador ativo
        FiltroColaboradorOption filtro = (cbFiltroColaborador != null) ? cbFiltroColaborador.getValue() : null;
        Integer idColaboradorFiltro = (filtro != null && !filtro.isTodos()) ? filtro.idColaborador() : null;

        java.util.Map<java.time.LocalDate, java.util.List<String>> mapa = new java.util.LinkedHashMap<>();
        for (GeracaoHorariosBLL.HorarioLinha linha : propostaAtual.linhas()) {
            if (linha == null || linha.data() == null) continue;
            // Aplicar filtro de colaborador
            if (idColaboradorFiltro != null && !idColaboradorFiltro.equals(linha.idColaborador())) continue;
            java.time.LocalDate data = linha.data();
            if (!YearMonth.from(data).equals(periodo)) continue;
            String desc;
            if (idColaboradorFiltro != null) {
                // Vista individual: mostrar apenas período/turno (nome não é necessário)
                desc = (linha.periodo() != null ? linha.periodo() : "?")
                    + " (" + (linha.cargo() != null ? linha.cargo() : "-") + ")";
            } else {
                desc = (linha.periodo() != null ? linha.periodo() : "?")
                    + " | " + (linha.colaborador() != null ? linha.colaborador() : "")
                    + " (" + (linha.cargo() != null ? linha.cargo() : "-") + ")";
            }
            mapa.computeIfAbsent(data, k -> new java.util.ArrayList<>()).add(desc);
        }
        return mapa;
    }

    private void abrirDetalheDia(LocalDate data) {
        if (data == null || propostaAtual == null || propostaAtual.linhas() == null) {
            return;
        }

        List<GeracaoHorariosBLL.HorarioLinha> turnosDia = propostaAtual.linhas().stream()
                .filter(linha -> linha != null && data.equals(linha.data()))
                .sorted(Comparator
                        .comparing(GeracaoHorariosBLL.HorarioLinha::periodo, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(GeracaoHorariosBLL.HorarioLinha::colaborador, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        if (turnosDia.isEmpty()) {
            return;
        }

        VBox listaTurnos = new VBox(10.0);
        listaTurnos.getStyleClass().add("detalhe-dia-lista");
        for (GeracaoHorariosBLL.HorarioLinha turno : turnosDia) {
            listaTurnos.getChildren().add(criarCardDetalheTurno(turno));
        }

        DialogosHelper.mostrarConteudo(
                obterJanela(),
                "DETALHE DO DIA",
                capitalizar(data.format(FORMATO_DIA_DETALHE)),
                turnosDia.size() + " turno(s) planeado(s). Revê a cobertura antes de enviar ao supervisor.",
                listaTurnos
        );
    }

    private VBox criarCardDetalheTurno(GeracaoHorariosBLL.HorarioLinha turno) {
        VBox card = new VBox(5.0);
        card.getStyleClass().add("detalhe-dia-turno-card");

        Label periodo = new Label(valorOuTraco(turno.periodo()));
        periodo.getStyleClass().add("detalhe-dia-periodo");

        Label colaborador = new Label(valorOuTraco(turno.colaborador()));
        colaborador.getStyleClass().add("detalhe-dia-colaborador");

        Label cargo = new Label(valorOuTraco(turno.cargo()) + " · " + valorOuTraco(turno.estado()));
        cargo.getStyleClass().add("detalhe-dia-cargo");
        cargo.setWrapText(true);

        card.getChildren().addAll(periodo, colaborador, cargo);

        // Botão de edição — só visível para gestores com proposta aprovada/publicada
        if (podeGerar && turno.idHorario() != null) {
            Button btnEditar = new Button("Editar turno");
            btnEditar.getStyleClass().add("botao-editar-turno");
            btnEditar.setMaxWidth(Double.MAX_VALUE);
            btnEditar.setOnAction(e -> {
                // Obtém a janela do diálogo de detalhe (pai do botão) para o ChoiceDialog aparecer por cima
                javafx.stage.Window owner = btnEditar.getScene() != null
                        ? btnEditar.getScene().getWindow()
                        : obterJanela();
                abrirEdicaoTurno(turno, owner);
            });
            card.getChildren().add(btnEditar);
        }

        return card;
    }

    // ── Horário individual do colaborador ────────────────────────────────────

    private void mostrarHorarioIndividual(GeracaoHorariosBLL.ResumoColaborador colaborador) {
        if (propostaAtual == null || propostaAtual.linhas() == null) return;

        List<GeracaoHorariosBLL.HorarioLinha> turnos = propostaAtual.linhas().stream()
                .filter(l -> colaborador.idColaborador() != null
                        && colaborador.idColaborador().equals(l.idColaborador()))
                .sorted(Comparator.comparing(GeracaoHorariosBLL.HorarioLinha::data,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        VBox conteudo = new VBox(16.0);
        conteudo.getStyleClass().add("horario-individual-conteudo");

        // Cabeçalho de estatísticas
        HBox statBar = new HBox(24.0);
        statBar.getStyleClass().add("horario-individual-stat-bar");
        statBar.setAlignment(Pos.CENTER_LEFT);
        statBar.getChildren().addAll(
                criarStatMini("Turnos no mês", String.valueOf(colaborador.turnos())),
                criarStatMini("Horas contratuais", colaborador.horasFormatadas())
        );
        conteudo.getChildren().add(statBar);

        if (turnos.isEmpty()) {
            Label vazio = new Label("Este colaborador não tem turnos atribuídos nesta proposta.");
            vazio.getStyleClass().add("horario-individual-vazio");
            vazio.setWrapText(true);
            conteudo.getChildren().add(vazio);
        } else {
            // Agrupar por semana
            Map<LocalDate, List<GeracaoHorariosBLL.HorarioLinha>> porSemana = new java.util.LinkedHashMap<>();
            for (GeracaoHorariosBLL.HorarioLinha linha : turnos) {
                LocalDate inicioSem = CalendarioSemanalHelper.inicioSemana(linha.data());
                porSemana.computeIfAbsent(inicioSem, k -> new java.util.ArrayList<>()).add(linha);
            }
            for (Map.Entry<LocalDate, List<GeracaoHorariosBLL.HorarioLinha>> entrada : porSemana.entrySet()) {
                conteudo.getChildren().add(criarBlocoSemanaIndividual(entrada.getKey(), entrada.getValue()));
            }
        }

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("horario-individual-scroll");
        scroll.setMaxHeight(500.0);
        scroll.setMinWidth(480.0);

        DialogosHelper.mostrarConteudo(
                obterJanela(),
                "HORÁRIO INDIVIDUAL",
                colaborador.nome() + "  ·  " + colaborador.cargo(),
                colaborador.turnos() + " turno(s) atribuído(s)  ·  " + colaborador.horasFormatadas(),
                scroll
        );
    }

    private VBox criarStatMini(String etiqueta, String valor) {
        VBox box = new VBox(3.0);
        box.getStyleClass().add("horario-individual-stat");
        Label lEtiqueta = new Label(etiqueta.toUpperCase(java.util.Locale.ROOT));
        lEtiqueta.getStyleClass().add("horario-individual-stat-etiqueta");
        Label lValor = new Label(valor);
        lValor.getStyleClass().add("horario-individual-stat-valor");
        box.getChildren().addAll(lEtiqueta, lValor);
        return box;
    }

    private VBox criarBlocoSemanaIndividual(LocalDate inicioSemana,
                                             List<GeracaoHorariosBLL.HorarioLinha> linhas) {
        VBox bloco = new VBox(6.0);
        bloco.getStyleClass().add("horario-individual-semana");

        Label lblSemana = new Label(CalendarioSemanalHelper.formatarIntervaloSemana(inicioSemana));
        lblSemana.getStyleClass().add("horario-individual-semana-titulo");
        bloco.getChildren().add(lblSemana);

        for (GeracaoHorariosBLL.HorarioLinha linha : linhas) {
            bloco.getChildren().add(criarLinhaTurnoIndividual(linha));
        }
        return bloco;
    }

    private HBox criarLinhaTurnoIndividual(GeracaoHorariosBLL.HorarioLinha linha) {
        HBox hbox = new HBox(12.0);
        hbox.getStyleClass().add("horario-individual-turno");
        hbox.setAlignment(Pos.CENTER_LEFT);

        String diaCurto = linha.diaSemana() != null && linha.diaSemana().length() >= 3
                ? linha.diaSemana().substring(0, 3).toUpperCase(java.util.Locale.ROOT)
                : "---";
        Label lblDia = new Label(diaCurto);
        lblDia.getStyleClass().add("horario-individual-dia");
        lblDia.setMinWidth(36.0);

        String dataStr = linha.data() != null
                ? linha.data().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
                : "--/--";
        Label lblData = new Label(dataStr);
        lblData.getStyleClass().add("horario-individual-data");
        lblData.setMinWidth(44.0);

        Label lblPeriodo = new Label(valorOuTraco(linha.periodo()));
        lblPeriodo.getStyleClass().add("horario-individual-periodo");
        HBox.setHgrow(lblPeriodo, Priority.ALWAYS);

        String estadoStr = valorOuTraco(linha.estado());
        Label lblEstado = new Label(estadoStr);
        lblEstado.getStyleClass().add("horario-individual-estado");

        hbox.getChildren().addAll(lblDia, lblData, lblPeriodo, lblEstado);
        return hbox;
    }

    // ────────────────────────────────────────────────────────────────────────

    private String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase(java.util.Locale.forLanguageTag("pt-PT")) + texto.substring(1);
    }

    private void configurarFiltros() {
        cbMes.setItems(FXCollections.observableArrayList(
                new MesOption(1, "Janeiro"),
                new MesOption(2, "Fevereiro"),
                new MesOption(3, "Março"),
                new MesOption(4, "Abril"),
                new MesOption(5, "Maio"),
                new MesOption(6, "Junho"),
                new MesOption(7, "Julho"),
                new MesOption(8, "Agosto"),
                new MesOption(9, "Setembro"),
                new MesOption(10, "Outubro"),
                new MesOption(11, "Novembro"),
                new MesOption(12, "Dezembro")
        ));

        spAno.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear(),
                LocalDate.now().getYear() + 5,
                LocalDate.now().getYear()
        ));
        spAno.setEditable(true);

        spQuantidadeAlternativas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 3));
        spQuantidadeAlternativas.setEditable(true);

        cbMes.valueProperty().addListener((observavel, antigo, novo) -> {
            onPeriodoAlterado();
        });
        spAno.valueProperty().addListener((observavel, antigo, novo) -> {
            onPeriodoAlterado();
        });

        cbFiltroColaborador.setItems(FXCollections.observableArrayList(FiltroColaboradorOption.todos()));
        cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        cbFiltroColaborador.valueProperty().addListener((observavel, antigo, novo) -> {
            aplicarFiltroColaborador();
            atualizarCalendarioMensal();
        });
        reposicionarSemanaPlaneamentoParaMesSelecionado();
    }

    private void onPeriodoAlterado() {
        invalidarPropostaAtual();
        carregarColaboradoresElegiveis();
        if (utilizadorLogado != null && utilizadorLogado.getId() != null && (operacaoEmCurso == null || !operacaoEmCurso.isRunning())) {
            carregarPlaneamentoDoPeriodo();
        }
    }

    private void configurarAtalhosRapidos() {
        lblLoja.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) {
                return;
            }
            nova.setOnKeyPressed(evento -> {
                if (!evento.isControlDown()) {
                    return;
                }
                if (evento.getCode() == KeyCode.G) {
                    onGerarPropostaClick();
                    evento.consume();
                    return;
                }
                if (evento.getCode() == KeyCode.L) {
                    onGerarAlternativasClick();
                    evento.consume();
                    return;
                }
                if (evento.getCode() == KeyCode.ENTER) {
                    onVerPropostaClick();
                    evento.consume();
                }
            });
        });
    }

    private void configurarTabelas() {
        colResumoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().nome()));

        colResumoCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().cargo()));

        colResumoTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().turnos())));

        colResumoHoras.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().horasFormatadas()));

        // Duplo-clique numa linha → ver horário completo do colaborador
        tabelaResumoColaboradores.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && propostaAtual != null) {
                GeracaoHorariosBLL.ResumoColaborador selecionado =
                        tabelaResumoColaboradores.getSelectionModel().getSelectedItem();
                if (selecionado != null) {
                    mostrarHorarioIndividual(selecionado);
                }
            }
        });
    }

    private void configurarTabelaPropostas() {
        tabelaPropostas.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        colPropostaRotulo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().rotulo()));
        colPropostaEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().estado()));
        colPropostaEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(estado.toUpperCase());
                badge.getStyleClass().add("badge-estado");
                String normalizado = Normalizer.normalize(estado.toLowerCase(), Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                if (normalizado.contains("aprovad") || normalizado.contains("publicad")) badge.getStyleClass().add("badge-aprovado");
                else if (normalizado.contains("rejeitad") || normalizado.contains("cancelad")) badge.getStyleClass().add("badge-rejeitado");
                else if (normalizado.contains("pendente") || normalizado.contains("enviado")) badge.getStyleClass().add("badge-enviado");
                else badge.getStyleClass().add("badge-rascunho");
                setGraphic(badge);
                setText(null);
            }
        });
        colPropostaData.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().dataGeracao()));
        colPropostaScore.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().pontuacao())));
        colPropostaQualidade.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().qualidade()));
        colPropostaQualidade.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String qualidade, boolean empty) {
                super.updateItem(qualidade, empty);
                if (empty || qualidade == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(qualidade);
                String q = qualidade.toLowerCase();
                if (q.contains("alta") || q.contains("excelente") || q.contains("ótima")) chip.getStyleClass().add("chip-qualidade-alta");
                else if (q.contains("média") || q.contains("media") || q.contains("razoável")) chip.getStyleClass().add("chip-qualidade-media");
                else chip.getStyleClass().add("chip-qualidade-baixa");
                setGraphic(chip);
                setText(null);
            }
        });
        colPropostaTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().turnos())));

        tabelaPropostas.getSelectionModel().selectedItemProperty().addListener((observavel, anterior, selecionada) -> {
            if (selecionada != null && !suprimirCarregamentoPorSelecao) {
                carregarPropostaPorIdEmSegundoPlano(selecionada.idProposta(), false);
            }
            atualizarEstadoInterativo();
        });
    }

    private void configurarComparacao() {
        colComparacaoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().colaborador()));
        colComparacaoBase.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().turnosBase() + " turnos - " + cellData.getValue().horasBase()));
        colComparacaoAlvo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().turnosComparada() + " turnos - " + cellData.getValue().horasComparada()));
        colComparacaoDiferenca.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        (cellData.getValue().diferencaTurnos() > 0 ? "+" : "")
                                + cellData.getValue().diferencaTurnos()
                                + " turnos - "
                                + cellData.getValue().diferencaHoras()
                ));

        cbComparacaoBase.valueProperty().addListener((observavel, antigo, novo) -> atualizarEstadoInterativo());
        cbComparacaoAlvo.valueProperty().addListener((observavel, antigo, novo) -> atualizarEstadoInterativo());

        // Seletor de proposta no calendário mensal: carregar proposta ao mudar seleção
        if (cbSelecaoProposta != null) {
            cbSelecaoProposta.valueProperty().addListener((observavel, antigo, novo) -> {
                if (novo != null && (propostaAtual == null || !novo.idProposta().equals(propostaAtual.idProposta()))) {
                    carregarPropostaPorIdEmSegundoPlano(novo.idProposta(), false);
                }
            });
        }
    }

    private void carregarContextoInicial() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            GeracaoHorariosBLL.GeracaoContexto contexto = geracaoHorariosBLL.obterContexto(utilizadorLogado.getId());
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

    private void carregarPropostaSelecionada() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            carregarListaPropostas(null);
            MesOption mesSelecionado = obterMesSelecionado();
            GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.obterPlaneamento(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    mesSelecionado.numero()
            );

            if (resultado == null) {
                limparResultado();
                mostrarInformacao("Ainda não existe proposta nem horários publicados para o período selecionado.");
                return;
            }

            preencherResultado(resultado);
            selecionarPropostaNaTabela(resultado.idProposta());
            mostrarInformacao("Planeamento do período carregado com sucesso.");
        } catch (IllegalArgumentException e) {
            limparResultado();
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            limparResultado();
            mostrarErro("Não foi possível carregar a proposta selecionada.");
        }
    }

    private void carregarPropostaPorId(Integer idProposta, boolean atualizarSelecaoTabela) {
        try {
            if (idProposta == null) {
                return;
            }
            GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.obterPropostaPorId(
                    utilizadorLogado.getId(),
                    idProposta
            );
            preencherResultado(resultado);
            esconderFeedback();
            if (atualizarSelecaoTabela) {
                selecionarPropostaNaTabela(idProposta);
            }
        } catch (IllegalArgumentException e) {
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            mostrarErro("Não foi possível carregar a alternativa selecionada.");
        }
    }

    private void gerarAlternativas(int quantidade) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
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
            )) {
                return;
            }

            MesOption mesSelecionado = obterMesSelecionado();
            List<GeracaoHorariosBLL.PropostaResultado> resultados = geracaoHorariosBLL.gerarPropostas(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    mesSelecionado.numero(),
                    quantidade,
                    idsColaboradoresSelecionados
            );

            GeracaoHorariosBLL.PropostaResultado melhorResultado = resultados.stream()
                    .min(java.util.Comparator.comparingInt(resultado -> resultado.metricas().pontuacao()))
                    .orElse(resultados.getFirst());
            carregarListaPropostas(melhorResultado.idProposta());
            preencherResultado(melhorResultado);
            selecionarPropostaNaTabela(melhorResultado.idProposta());
            esconderFeedbackValidacao();
            mostrarSucesso(resultados.size() == 1
                    ? "Alternativa gerada com sucesso."
                    : resultados.size() + " alternativas geradas. A melhor pontuação ficou selecionada.");
        } catch (IllegalArgumentException e) {
            if (tentarCarregarPlaneamentoExistente()) {
                esconderFeedbackValidacao();
                mostrarErro(e.getMessage() + " O planeamento atual foi carregado abaixo para te ajudar a analisar a situação.");
            } else {
                mostrarErro(e.getMessage());
            }
        } catch (Exception e) {
            mostrarErro("Não foi possível gerar alternativas para o período selecionado.");
        }
    }

    private void carregarColaboradoresElegiveis() {
        if (!podeGerar
                || utilizadorLogado == null
                || utilizadorLogado.getId() == null
                || cbMes.getValue() == null
                || spAno.getValue() == null) {
            renderizarColaboradoresGeracao(List.of());
            return;
        }

        try {
            List<GeracaoHorariosBLL.ColaboradorElegivel> colaboradores = geracaoHorariosBLL.listarColaboradoresElegiveis(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    cbMes.getValue().numero()
            );
            renderizarColaboradoresGeracao(colaboradores);
        } catch (IllegalArgumentException e) {
            renderizarColaboradoresGeracao(List.of());
            lblResumoColaboradoresGeracao.setText(e.getMessage());
        } catch (Exception e) {
            renderizarColaboradoresGeracao(List.of());
            lblResumoColaboradoresGeracao.setText("Não foi possível carregar a equipa elegível para este período.");
        }
    }

    private void renderizarColaboradoresGeracao(List<GeracaoHorariosBLL.ColaboradorElegivel> colaboradores) {
        Set<Integer> selecionadosAnteriores = obterIdsColaboradoresSelecionadosComoSet();
        boolean devePreservarSelecao = !selecaoColaboradoresGeracao.isEmpty();

        selecaoColaboradoresGeracao.clear();
        boxColaboradoresGeracao.getChildren().clear();

        if (colaboradores == null || colaboradores.isEmpty()) {
            Label vazio = new Label("Sem colaboradores elegíveis para o período selecionado.");
            vazio.getStyleClass().add("texto-ajuda");
            boxColaboradoresGeracao.getChildren().add(vazio);
            atualizarResumoSelecaoColaboradores();
            return;
        }

        Map<String, List<GeracaoHorariosBLL.ColaboradorElegivel>> colaboradoresPorCargo = agruparColaboradoresPorCargo(colaboradores);
        for (Map.Entry<String, List<GeracaoHorariosBLL.ColaboradorElegivel>> grupo : colaboradoresPorCargo.entrySet()) {
            VBox boxGrupo = new VBox(6);
            boxGrupo.getStyleClass().add("grupo-colaboradores");

            CheckBox checkGrupo = new CheckBox(grupo.getKey() + " (" + grupo.getValue().size() + ")");
            checkGrupo.getStyleClass().add("grupo-colaboradores-titulo");
            boxGrupo.getChildren().add(checkGrupo);

            VBox boxItensGrupo = new VBox(5);
            boxItensGrupo.getStyleClass().add("grupo-colaboradores-itens");
            List<CheckBox> checksGrupo = new ArrayList<>();
            for (GeracaoHorariosBLL.ColaboradorElegivel colaborador : grupo.getValue()) {
                CheckBox checkBox = criarCheckBoxColaborador(colaborador, devePreservarSelecao, selecionadosAnteriores);
                checksGrupo.add(checkBox);
                boxItensGrupo.getChildren().add(checkBox);
                selecaoColaboradoresGeracao.put(colaborador.idColaborador(), checkBox);
            }

            checkGrupo.setOnAction(event -> checksGrupo.forEach(checkBox -> checkBox.setSelected(checkGrupo.isSelected())));
            checksGrupo.forEach(checkBox -> checkBox.selectedProperty().addListener(
                    (observavel, anterior, selecionado) -> atualizarEstadoCheckGrupo(checkGrupo, checksGrupo)
            ));
            atualizarEstadoCheckGrupo(checkGrupo, checksGrupo);

            boxGrupo.getChildren().add(boxItensGrupo);
            boxColaboradoresGeracao.getChildren().add(boxGrupo);
        }

        atualizarResumoSelecaoColaboradores();
    }

    private Map<String, List<GeracaoHorariosBLL.ColaboradorElegivel>> agruparColaboradoresPorCargo(
            List<GeracaoHorariosBLL.ColaboradorElegivel> colaboradores) {
        Map<String, List<GeracaoHorariosBLL.ColaboradorElegivel>> grupos = new LinkedHashMap<>();
        colaboradores.stream()
                .sorted(Comparator
                        .comparing(GeracaoHorariosBLL.ColaboradorElegivel::cargo, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(GeracaoHorariosBLL.ColaboradorElegivel::nome, String.CASE_INSENSITIVE_ORDER))
                .forEach(colaborador -> grupos
                        .computeIfAbsent(colaborador.cargo(), ignorado -> new ArrayList<>())
                        .add(colaborador));
        return grupos;
    }

    private CheckBox criarCheckBoxColaborador(GeracaoHorariosBLL.ColaboradorElegivel colaborador,
                                              boolean devePreservarSelecao,
                                              Set<Integer> selecionadosAnteriores) {
            CheckBox checkBox = new CheckBox(colaborador.nome()
                    + " | "
                    + colaborador.perfilContratual());
            checkBox.getStyleClass().add("colaborador-check");
            checkBox.setWrapText(true);
            checkBox.setSelected(devePreservarSelecao
                    ? selecionadosAnteriores.contains(colaborador.idColaborador())
                    : colaborador.selecionadoPorDefeito());
            checkBox.selectedProperty().addListener((observavel, anterior, selecionado) -> atualizarResumoSelecaoColaboradores());
            return checkBox;
    }

    private void atualizarEstadoCheckGrupo(CheckBox checkGrupo, List<CheckBox> checksGrupo) {
        long selecionados = checksGrupo.stream().filter(CheckBox::isSelected).count();
        checkGrupo.setIndeterminate(selecionados > 0 && selecionados < checksGrupo.size());
        checkGrupo.setSelected(selecionados == checksGrupo.size() && !checksGrupo.isEmpty());
    }

    private List<Integer> obterIdsColaboradoresSelecionados() {
        return obterIdsColaboradoresSelecionadosComoSet().stream().toList();
    }

    private Set<Integer> obterIdsColaboradoresSelecionadosComoSet() {
        Set<Integer> selecionados = new LinkedHashSet<>();
        for (Map.Entry<Integer, CheckBox> entrada : selecaoColaboradoresGeracao.entrySet()) {
            if (entrada.getValue().isSelected()) {
                selecionados.add(entrada.getKey());
            }
        }
        return selecionados;
    }

    private void atualizarResumoSelecaoColaboradores() {
        int total = selecaoColaboradoresGeracao.size();
        int selecionados = obterIdsColaboradoresSelecionadosComoSet().size();
        if (total == 0) {
            lblResumoColaboradoresGeracao.setText("Escolhe o período para carregar a equipa elegível.");
        } else {
            lblResumoColaboradoresGeracao.setText(selecionados + " de " + total + " colaboradores selecionados para a geração.");
        }
        atualizarEstadoInterativo();
    }

    private void carregarListaPropostas(Integer idSelecionar) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null || cbMes.getValue() == null || spAno.getValue() == null) {
                aplicarListaPropostas(List.of(), null);
                return;
            }

            List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    cbMes.getValue().numero()
            );
            aplicarListaPropostas(propostas, idSelecionar);
        } catch (Exception e) {
            aplicarListaPropostas(List.of(), null);
        }
    }

    private void selecionarPropostaNaTabela(Integer idProposta) {
        if (idProposta == null || tabelaPropostas.getItems() == null) {
            return;
        }
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
                    aprovar
                            ? "Os horários serão publicados para a loja."
                            : "A proposta ficará rejeitada e não será publicada."
            )) {
                return;
            }

            GeracaoHorariosBLL.PropostaResultado resultado = aprovar
                    ? geracaoHorariosBLL.aprovarProposta(
                    utilizadorLogado.getId(),
                    propostaAtual.idProposta(),
                    txtObservacoesSupervisor.getText()
            )
                    : geracaoHorariosBLL.rejeitarProposta(
                    utilizadorLogado.getId(),
                    propostaAtual.idProposta(),
                    txtObservacoesSupervisor.getText()
            );

            preencherResultado(resultado);
            carregarListaPropostas(resultado.idProposta());
            mostrarFeedbackValidacao(
                    aprovar
                            ? "Proposta aprovada e horários publicados com sucesso."
                            : "Proposta rejeitada com sucesso.",
                    true
            );
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

    private void preencherResultado(GeracaoHorariosBLL.PropostaResultado resultado) {
        propostaAtual = resultado;
        // Sincronizar o período mensal com o mês/ano da proposta carregada
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

        // Atualizar labels de identificação do contexto
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
        // Sincronizar seletor de proposta no calendário (o listener tem guard: não dispara em ciclo)
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
        // Sincronizar grelha com o mês/ano da proposta carregada
        grelhaDataInicio = LocalDate.of(resultado.ano(), resultado.mes(), 1);
        if (grelhaVistaSemanais) {
            grelhaDataInicio = grelhaDataInicio.with(
                    java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        }
        // Rebuilds only if the grelha panel is currently visible
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
        if (lblPeriodoPropostas != null) {
            lblPeriodoPropostas.setText("Seleciona um mês e gera uma proposta");
        }
        if (lblIdentificacaoHorario != null) {
            lblIdentificacaoHorario.setText("Sem proposta carregada");
        }
        if (cbSelecaoProposta != null) {
            cbSelecaoProposta.setValue(null);
        }
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        renderizarCalendarioPlaneamento(List.of());
        atualizarCalendarioMensal();
        atualizarPainelValidacao();
        atualizarEstadoInterativo();
    }

    private void invalidarPropostaAtual() {
        if (utilizadorLogado == null) {
            return;
        }

        limparResultado();
        aplicarListaPropostas(List.of(), null);
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        esconderFeedback();
    }

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
        if (btnCompararPropostas != null) {
            btnCompararPropostas.setDisable(true);
        }
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
        if (!podeValidar) {
            atualizarEstadoInterativo();
            return;
        }

        boolean podeDecidirAgora = propostaAtual != null && propostaAtual.podeSerDecidida();
        if (podeDecidirAgora) {
            esconderFeedbackValidacao();
        } else if (propostaAtual == null) {
            txtObservacoesSupervisor.clear();
        }
        atualizarEstadoInterativo();
    }

    private void mostrarSucesso(String mensagem) {
        mostrarFeedback(mensagem, "mensagem-sucesso");
    }

    private void mostrarErro(String mensagem) {
        mostrarFeedback(mensagem, "mensagem-erro");
    }

    private void mostrarInformacao(String mensagem) {
        mostrarFeedback(mensagem, null);
    }

    private void mostrarFeedback(String mensagem, String estilo) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-feedback", "mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        if (estilo != null) {
            lblFeedback.getStyleClass().add(estilo);
        }
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void esconderFeedback() {
        lblFeedback.setVisible(false);
        lblFeedback.setManaged(false);
        lblFeedback.setText("");
    }

    private void mostrarDiagnosticoGeracao(Throwable erro) {
        Throwable causaRaiz = encontrarCausaRaiz(erro);
        if (!(causaRaiz instanceof GeracaoHorariosBLL.FalhaGeracaoHorarioException falha)) {
            esconderDiagnosticoGeracao();
            return;
        }

        lblDiagnosticoTitulo.setText("Turno sem cobertura: " + falha.turno() + " em " + falha.data());
        lblDiagnosticoResumo.setText(
                "Foram considerados "
                        + falha.colaboradoresConsiderados()
                        + " colaboradores. Principal bloqueio: "
                        + falha.motivoPrincipal()
        );
        String perfilRecomendado = perfilRecomendado(falha);
        lblDiagnosticoPerfilRecomendado.setText(perfilRecomendado.isBlank()
                ? ""
                : "Reforco recomendado: " + perfilRecomendado
                        + ". Este e o perfil que mais ajuda a aliviar este gargalo sem mexer nas restantes regras.");
        lblDiagnosticoPerfilRecomendado.setVisible(!perfilRecomendado.isBlank());
        lblDiagnosticoPerfilRecomendado.setManaged(!perfilRecomendado.isBlank());
        boxDiagnosticoMotivos.getChildren().clear();
        boxDiagnosticoSugestoes.getChildren().clear();

        for (GeracaoHorariosBLL.MotivoFalhaGeracao motivo : falha.motivos()) {
            VBox blocoMotivo = new VBox(6);
            blocoMotivo.getStyleClass().add("diagnostico-bloco-motivo");

            HBox linha = new HBox(10);
            linha.getStyleClass().add("diagnostico-linha");

            Button total = new Button(motivo.total() == 1 ? "1 pessoa" : motivo.total() + " pessoas");
            total.getStyleClass().add("diagnostico-contador");
            total.setOnAction(event -> mostrarDetalheMotivo(motivo));

            Label descricao = new Label(motivo.descricao());
            descricao.getStyleClass().add("diagnostico-descricao");
            descricao.setWrapText(true);

            linha.getChildren().addAll(total, descricao);
            blocoMotivo.getChildren().add(linha);

            if (motivo.nomes() != null && !motivo.nomes().isEmpty()) {
                HBox nomes = new HBox(6);
                nomes.getStyleClass().add("diagnostico-nomes");
                List<String> nomesVisiveis = motivo.nomes().stream().limit(4).toList();
                for (String nome : nomesVisiveis) {
                    Label etiqueta = new Label(nome);
                    etiqueta.getStyleClass().add("diagnostico-nome");
                    nomes.getChildren().add(etiqueta);
                }
                if (motivo.nomes().size() > nomesVisiveis.size()) {
                    Label restantes = new Label("+" + (motivo.nomes().size() - nomesVisiveis.size()) + " ver no contador");
                    restantes.getStyleClass().add("diagnostico-nome-mais");
                    nomes.getChildren().add(restantes);
                }
                blocoMotivo.getChildren().add(nomes);
            }

            boxDiagnosticoMotivos.getChildren().add(blocoMotivo);
        }

        int indice = 0;
        for (GeracaoHorariosBLL.SugestaoFalhaGeracao sugestao : falha.sugestoes()) {
            HBox linha = new HBox(10);
            linha.getStyleClass().add("diagnostico-linha");

            Label ordem = new Label(rotuloSugestao(indice++));
            ordem.getStyleClass().add("diagnostico-etapa");

            Label descricao = new Label(sugestao.texto());
            descricao.getStyleClass().add("diagnostico-sugestao");
            descricao.setWrapText(true);

            VBox textoSugestao = new VBox(5);
            textoSugestao.getChildren().add(descricao);

            linha.getChildren().addAll(ordem, textoSugestao);
            boxDiagnosticoSugestoes.getChildren().add(linha);
        }

        painelDiagnosticoGeracao.setVisible(true);
        painelDiagnosticoGeracao.setManaged(true);
    }

    private String perfilRecomendado(GeracaoHorariosBLL.FalhaGeracaoHorarioException falha) {
        return falha.sugestoes().stream()
                .map(GeracaoHorariosBLL.SugestaoFalhaGeracao::perfilRecomendado)
                .filter(perfil -> perfil != null && !perfil.isBlank())
                .findFirst()
                .orElse("");
    }

    private void mostrarDetalheMotivo(GeracaoHorariosBLL.MotivoFalhaGeracao motivo) {
        FlowPane nomes = new FlowPane(8, 8);
        nomes.getStyleClass().add("diagnostico-modal-nomes");
        for (String nome : motivo.nomes()) {
            Label etiqueta = new Label(nome);
            etiqueta.getStyleClass().add("diagnostico-nome");
            nomes.getChildren().add(etiqueta);
        }

        DialogosHelper.mostrarConteudo(
                obterJanela(),
                "Diagnóstico da geração",
                motivo.total() == 1 ? "1 pessoa afetada" : motivo.total() + " pessoas afetadas",
                motivo.descricao(),
                nomes
        );
    }

    private String rotuloSugestao(int indice) {
        return switch (indice) {
            case 0 -> "Fazer primeiro";
            case 1 -> "Depois";
            case 2 -> "Verificar";
            default -> "Opcional";
        };
    }

    private void esconderDiagnosticoGeracao() {
        if (painelDiagnosticoGeracao == null) {
            return;
        }
        painelDiagnosticoGeracao.setVisible(false);
        painelDiagnosticoGeracao.setManaged(false);
        if (lblDiagnosticoPerfilRecomendado != null) {
            lblDiagnosticoPerfilRecomendado.setVisible(false);
            lblDiagnosticoPerfilRecomendado.setManaged(false);
            lblDiagnosticoPerfilRecomendado.setText("");
        }
        if (boxDiagnosticoMotivos != null) {
            boxDiagnosticoMotivos.getChildren().clear();
        }
        if (boxDiagnosticoSugestoes != null) {
            boxDiagnosticoSugestoes.getChildren().clear();
        }
    }

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

    private boolean tentarCarregarPlaneamentoExistente() {
        try {
            validarUtilizadorAutenticado();

            GeracaoHorariosBLL.PropostaResultado existente = geracaoHorariosBLL.obterPlaneamento(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    obterMesSelecionado().numero()
            );
            if (existente == null) {
                return false;
            }

            List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    obterMesSelecionado().numero()
            );
            aplicarListaPropostas(propostas, existente.idProposta());
            preencherResultado(existente);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void atualizarFiltroColaborador(GeracaoHorariosBLL.PropostaResultado resultado) {
        FiltroColaboradorOption selecionadoAnteriormente = cbFiltroColaborador.getValue();
        Integer idAnterior = selecionadoAnteriormente != null ? selecionadoAnteriormente.idColaborador() : null;

        var opcoes = FXCollections.<FiltroColaboradorOption>observableArrayList();
        opcoes.add(FiltroColaboradorOption.todos());
        resultado.resumoColaboradores().forEach(colaborador -> opcoes.add(
                new FiltroColaboradorOption(colaborador.idColaborador(), colaborador.nome())
        ));

        cbFiltroColaborador.setItems(opcoes);
        if (idAnterior != null) {
            cbFiltroColaborador.getItems().stream()
                    .filter(opcao -> idAnterior.equals(opcao.idColaborador()))
                    .findFirst()
                    .ifPresentOrElse(cbFiltroColaborador::setValue, () -> cbFiltroColaborador.setValue(FiltroColaboradorOption.todos()));
        } else {
            cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        }
    }

    private void aplicarFiltroColaborador() {
        if (propostaAtual == null) {
            renderizarCalendarioPlaneamento(List.of());
            return;
        }

        FiltroColaboradorOption filtro = cbFiltroColaborador.getValue();
        List<GeracaoHorariosBLL.HorarioLinha> linhas = propostaAtual.linhas();
        if (filtro != null && !filtro.isTodos()) {
            linhas = linhas.stream()
                    .filter(linha -> filtro.idColaborador().equals(linha.idColaborador()))
                    .toList();
        }

        LocalDate dataInicioSemana = semanaPlaneamentoInicio != null
                ? semanaPlaneamentoInicio
                : CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), obterMesSelecionado().numero(), 1));
        LocalDate dataFimSemana = dataInicioSemana.plusDays(6);

        List<GeracaoHorariosBLL.HorarioLinha> linhasDaSemana = linhas.stream()
                .filter(linha -> linha.data() != null
                        && !linha.data().isBefore(dataInicioSemana)
                        && !linha.data().isAfter(dataFimSemana))
                .toList();

        renderizarCalendarioPlaneamento(linhasDaSemana);
    }

    private void reposicionarSemanaPlaneamentoParaMesSelecionado() {
        try {
            if (cbMes.getValue() == null || spAno.getValue() == null) {
                semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
            } else {
                semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), cbMes.getValue().numero(), 1));
            }
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

    private void renderizarCalendarioPlaneamento(List<GeracaoHorariosBLL.HorarioLinha> linhas) {
        Map<LocalDate, List<String>> eventos = new LinkedHashMap<>();
        for (GeracaoHorariosBLL.HorarioLinha linha : linhas) {
            String evento = linha.periodo() + " | " + linha.colaborador() + " (" + linha.cargo() + ")";
            eventos.computeIfAbsent(linha.data(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxSemanaPlaneamento,
                semanaPlaneamentoInicio,
                eventos,
                "Sem turnos"
        );
    }

    private void carregarPlaneamentoDoPeriodo() {
        executarOperacaoEmSegundoPlano(
                "A carregar o planeamento do período selecionado...",
                () -> {
                    validarUtilizadorAutenticado();
                    MesOption mesSelecionado = obterMesSelecionado();
                    List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                            utilizadorLogado.getId(),
                            spAno.getValue(),
                            mesSelecionado.numero()
                    );
                    GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.obterPlaneamento(
                            utilizadorLogado.getId(),
                            spAno.getValue(),
                            mesSelecionado.numero()
                    );
                    return new PlaneamentoCarregadoDados(propostas, resultado);
                },
                dados -> {
                    aplicarListaPropostas(
                            dados.propostas(),
                            dados.resultado() != null ? dados.resultado().idProposta() : null
                    );
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
        if (idProposta == null) {
            return;
        }

        executarOperacaoEmSegundoPlano(
                "A carregar a alternativa selecionada...",
                () -> {
                    validarUtilizadorAutenticado();
                    return geracaoHorariosBLL.obterPropostaPorId(utilizadorLogado.getId(), idProposta);
                },
                resultado -> {
                    preencherResultado(resultado);
                    if (atualizarSelecaoTabela) {
                        selecionarPropostaNaTabela(idProposta);
                    }
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
                    .map(GeracaoHorariosBLL.PropostaResumo::idProposta)
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
            )) {
                return;
            }

            executarOperacaoEmSegundoPlano(
                    idsPropostas.size() == 1
                            ? "A enviar alternativa ao supervisor..."
                            : "A enviar alternativas ao supervisor...",
                    () -> {
                        geracaoHorariosBLL.enviarPropostasParaValidacao(utilizadorLogado.getId(), idsPropostas);
                        MesOption mesSelecionado = obterMesSelecionado();
                        List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                                utilizadorLogado.getId(),
                                spAno.getValue(),
                                mesSelecionado.numero()
                        );
                        GeracaoHorariosBLL.PropostaResultado enviada = geracaoHorariosBLL.obterPropostaPorId(
                                utilizadorLogado.getId(),
                                idsPropostas.getFirst()
                        );
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
            )) {
                return;
            }

            // Mostrar overlay de carregamento (não-bloqueante, cobre toda a janela)
            final Stage overlayCarregamento = DialogosHelper.mostrarCarregamento(
                    obterJanela(),
                    quantidade == 1
                            ? "A gerar o horário para o período selecionado..."
                            : "A gerar " + quantidade + " alternativas de horário..."
            );

            executarOperacaoEmSegundoPlano(
                    quantidade == 1
                            ? "A gerar uma alternativa para o mês selecionado..."
                            : "A gerar " + quantidade + " alternativas para o mês selecionado...",
                    () -> {
                        MesOption mesSelecionado = obterMesSelecionado();
                        List<GeracaoHorariosBLL.PropostaResultado> resultados = geracaoHorariosBLL.gerarPropostas(
                                utilizadorLogado.getId(),
                                spAno.getValue(),
                                mesSelecionado.numero(),
                                quantidade,
                                idsColaboradoresSelecionados
                        );

                        GeracaoHorariosBLL.PropostaResultado melhorResultado = resultados.stream()
                                .min(java.util.Comparator.comparingInt(resultado -> resultado.metricas().pontuacao()))
                                .orElse(resultados.getFirst());
                        List<GeracaoHorariosBLL.PropostaResumo> propostas = geracaoHorariosBLL.listarPropostas(
                                utilizadorLogado.getId(),
                                spAno.getValue(),
                                mesSelecionado.numero()
                        );
                        return new GeracaoAlternativasDados(propostas, melhorResultado, resultados.size());
                    },
                    dados -> {
                        // Fechar overlay de carregamento
                        overlayCarregamento.close();

                        aplicarListaPropostas(dados.propostas(), dados.melhorResultado().idProposta());
                        preencherResultado(dados.melhorResultado());
                        selecionarPropostaNaTabela(dados.melhorResultado().idProposta());
                        esconderFeedbackValidacao();
                        esconderDiagnosticoGeracao();

                        // Avançar para o passo "Alternativas" (índice 1) para ver o resultado
                        if (tabPaneHorarios != null) {
                            tabPaneHorarios.getSelectionModel().select(1);
                        }

                        // Notificação grande de sucesso centrada na janela
                        String tituloNotif = dados.totalGeradas() == 1
                                ? "Horário gerado!"
                                : dados.totalGeradas() + " alternativas geradas!";
                        String mensagemNotif = dados.totalGeradas() == 1
                                ? "Estás agora no passo 2 (Alternativas). Analisa a proposta, e usa o botão 'Rever proposta' para ver o calendário antes de enviar."
                                : "As " + dados.totalGeradas() + " alternativas estão no passo 2. A melhor pontuação ficou selecionada. Avança para 'Rever' e depois 'Enviar'.";
                        DialogosHelper.mostrarNotificacaoGeracao(obterJanela(), true, tituloNotif, mensagemNotif);

                        mostrarSucesso(dados.totalGeradas() == 1
                                ? "Alternativa gerada como rascunho."
                                : dados.totalGeradas() + " alternativas geradas. A melhor pontuação ficou selecionada para análise.");
                    },
                    erro -> {
                        // Fechar overlay de carregamento
                        overlayCarregamento.close();

                        String mensagem = resolverMensagemErro(erro, "Não foi possível gerar alternativas para o período selecionado.");
                        String mensagemCurta = mensagem.length() > 220 ? mensagem.substring(0, 217) + "..." : mensagem;

                        // Notificação grande de erro — utilizador vê antes de ver o diagnóstico
                        DialogosHelper.mostrarNotificacaoGeracao(
                                obterJanela(), false,
                                "Não foi possível gerar o horário",
                                mensagemCurta
                        );

                        // Voltar para a tab "Gerar" (índice 0) para mostrar diagnóstico
                        if (tabPaneHorarios != null) {
                            tabPaneHorarios.getSelectionModel().select(0);
                        }

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

    private void aplicarListaPropostas(List<GeracaoHorariosBLL.PropostaResumo> propostas, Integer idSelecionar) {
        List<GeracaoHorariosBLL.PropostaResumo> propostasSeguras = propostas != null ? propostas : List.of();
        tabelaPropostas.setItems(FXCollections.observableArrayList(propostasSeguras));
        cbComparacaoBase.setItems(FXCollections.observableArrayList(propostasSeguras));
        cbComparacaoAlvo.setItems(FXCollections.observableArrayList(propostasSeguras));
        if (cbSelecaoProposta != null) {
            cbSelecaoProposta.setItems(FXCollections.observableArrayList(propostasSeguras));
        }

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

        if (idSelecionar != null) {
            selecionarPropostaNaTabela(idSelecionar);
        }

        atualizarEstadoInterativo();
    }

    private boolean propostaEmRascunho(GeracaoHorariosBLL.PropostaResumo proposta) {
        return proposta != null && "rascunho".equals(normalizarTexto(proposta.estado()));
    }

    private boolean existePropostaEmRascunhoSelecionada() {
        return tabelaPropostas.getSelectionModel().getSelectedItems().stream()
                .anyMatch(this::propostaEmRascunho);
    }

    private void atualizarEstadoInterativo() {
        boolean emProcessamento = operacaoEmCurso != null && operacaoEmCurso.isRunning();
        boolean contextoCarregado = utilizadorLogado != null && utilizadorLogado.getId() != null;

        cbMes.setDisable(!contextoCarregado || emProcessamento);
        spAno.setDisable(!contextoCarregado || emProcessamento);
        btnVerProposta.setDisable(!contextoCarregado || emProcessamento);
        btnGerarProposta.setDisable(!podeGerar || emProcessamento);
        btnGerarAlternativas.setDisable(!podeGerar || emProcessamento);
        spQuantidadeAlternativas.setDisable(!podeGerar || emProcessamento);
        painelSelecaoColaboradores.setDisable(!podeGerar || emProcessamento);
        btnSelecionarTodosColaboradores.setDisable(!podeGerar || emProcessamento || selecaoColaboradoresGeracao.isEmpty());
        btnLimparColaboradores.setDisable(!podeGerar || emProcessamento || selecaoColaboradoresGeracao.isEmpty());

        tabelaPropostas.setDisable(emProcessamento);
        cbComparacaoBase.setDisable(emProcessamento);
        cbComparacaoAlvo.setDisable(emProcessamento);
        btnEnviarSupervisor.setDisable(!podeGerar || emProcessamento || !existePropostaEmRascunhoSelecionada());
        if (btnCompararPropostas != null) {
            btnCompararPropostas.setDisable(
                    emProcessamento
                            || cbComparacaoBase.getValue() == null
                            || cbComparacaoAlvo.getValue() == null
            );
        }

        boolean podeDecidirAgora = podeValidar && propostaAtual != null && propostaAtual.podeSerDecidida();
        btnAprovarProposta.setDisable(emProcessamento || !podeDecidirAgora);
        btnRejeitarProposta.setDisable(emProcessamento || !podeDecidirAgora);
        txtObservacoesSupervisor.setDisable(emProcessamento || !podeDecidirAgora);

        boolean semDados = emProcessamento || propostaAtual == null
                || propostaAtual.linhas() == null || propostaAtual.linhas().isEmpty();
        if (btnExportarCsvHorario != null) btnExportarCsvHorario.setDisable(semDados);
        if (btnExportarPdfHorario != null) btnExportarPdfHorario.setDisable(semDados);

        // ── Navegação do assistente: gating dos botões "Continuar" ──
        boolean temPropostasLista = tabelaPropostas.getItems() != null
                && !tabelaPropostas.getItems().isEmpty();
        boolean temPropostaSelecionada = propostaAtual != null;
        if (btnPasso1Continuar != null) {
            btnPasso1Continuar.setDisable(emProcessamento || !temPropostasLista);
        }
        if (btnPasso2Continuar != null) {
            btnPasso2Continuar.setDisable(emProcessamento || !temPropostaSelecionada);
        }
        if (btnPasso3Continuar != null) {
            btnPasso3Continuar.setDisable(emProcessamento || !temPropostaSelecionada);
        }

        // ── Resumo do que vai ser enviado (passo 4 — gerente) ──
        if (lblResumoEnvio != null) {
            var selecionadas = tabelaPropostas.getSelectionModel().getSelectedItems();
            long emRascunho = selecionadas == null ? 0 :
                    selecionadas.stream().filter(this::propostaEmRascunho).count();
            if (emRascunho == 0) {
                lblResumoEnvio.setText("Nenhum rascunho selecionado. Volta ao passo 2 (Alternativas) "
                        + "e seleciona a(s) alternativa(s) que queres enviar.");
            } else if (emRascunho == 1) {
                lblResumoEnvio.setText("Vais enviar 1 alternativa ao supervisor.");
            } else {
                lblResumoEnvio.setText("Vais enviar " + emRascunho + " alternativas ao supervisor.");
            }
        }

        atualizarEmptyStates();
        atualizarStepper(emProcessamento);
    }

    /**
     * Alterna os "empty states" das três zonas de resultado (alternativas geradas,
     * distribuição por colaborador e calendário semanal) consoante existam ou não dados.
     * Sem isto, o cartão "Sem ..." ficava permanentemente visível por cima da tabela/calendário
     * já preenchido, dando a sensação de ecrã partido.
     */
    private void atualizarEmptyStates() {
        boolean temPropostas = tabelaPropostas.getItems() != null
                && !tabelaPropostas.getItems().isEmpty();
        alternarEmptyState(emptyStatePropostas, tabelaPropostas, temPropostas);

        boolean temDistribuicao = tabelaResumoColaboradores.getItems() != null
                && !tabelaResumoColaboradores.getItems().isEmpty();
        alternarEmptyState(emptyStateDistribuicao, tabelaResumoColaboradores, temDistribuicao);

        boolean temPlaneamento = propostaAtual != null;
        alternarEmptyState(emptyStateCalendario, boxSemanaPlaneamento, temPlaneamento);
    }

    /**
     * Quando há conteúdo: esconde o empty-state e mostra o conteúdo.
     * Quando não há: mostra o empty-state e esconde o conteúdo (evita o duplo "vazio").
     */
    private void alternarEmptyState(javafx.scene.Node emptyState, javafx.scene.Node conteudo, boolean temConteudo) {
        if (emptyState != null) {
            emptyState.setVisible(!temConteudo);
            emptyState.setManaged(!temConteudo);
        }
        if (conteudo != null) {
            conteudo.setVisible(temConteudo);
            conteudo.setManaged(temConteudo);
        }
    }

    private void atualizarStepper(boolean emProcessamento) {
        if (stepperPasso1 == null) return;

        boolean temProposta = propostaAtual != null;
        boolean temRascunho = !tabelaPropostas.getItems().isEmpty();
        boolean enviada = temProposta && propostaAtual.estado() != null
                && normalizarTexto(propostaAtual.estado()).contains("enviado");
        boolean aprovada = temProposta && propostaAtual.estado() != null
                && normalizarTexto(propostaAtual.estado()).contains("aprovad");
        boolean decidida = enviada || aprovada
                || (temProposta && propostaAtual.estado() != null
                        && normalizarTexto(propostaAtual.estado()).contains("rejeitad"));

        // Passo 1 — Configurar: sempre ativo/concluído
        aplicarEstadoStepper(stepperPasso1, true, temRascunho || temProposta);

        // Passo 2 — Gerar: concluído se existem propostas
        aplicarEstadoStepper(stepperPasso2, temRascunho || temProposta || emProcessamento, temRascunho || temProposta);

        // Passo 3 — Analisar: ativo quando há proposta mas ainda não enviada
        aplicarEstadoStepper(stepperPasso3, temProposta, decidida || aprovada);

        // Passo 4 — Enviar: ativo quando já enviado ou aprovado
        aplicarEstadoStepper(stepperPasso4, decidida, aprovada);

        // Guia de fluxo contextual
        atualizarGuiaFluxo(temRascunho, temProposta, enviada, aprovada, emProcessamento);
    }

    private void aplicarEstadoStepper(VBox passo, boolean ativo, boolean concluido) {
        if (passo == null) return;
        passo.getStyleClass().removeAll("stepper-passo-ativo", "stepper-passo-concluido", "stepper-passo-inativo");
        if (concluido) {
            passo.getStyleClass().add("stepper-passo-concluido");
        } else if (ativo) {
            passo.getStyleClass().add("stepper-passo-ativo");
        } else {
            passo.getStyleClass().add("stepper-passo-inativo");
        }
    }

    private void atualizarGuiaFluxo(boolean temRascunho, boolean temProposta,
                                     boolean enviada, boolean aprovada, boolean emProcessamento) {
        if (lblGuiaFluxo == null) return;
        String guia;
        if (emProcessamento) {
            guia = "A processar... aguarda.";
        } else if (aprovada) {
            guia = "✔ Proposta aprovada e publicada. O calendário fica disponível no passo 3 (Rever).";
        } else if (enviada) {
            guia = "Proposta enviada ao supervisor. Aguarda a decisão ou gera mais alternativas.";
        } else if (temProposta) {
            guia = "Já tens uma proposta. Usa 'Ver alternativas' para comparar, ou 'Rever proposta' para ver o calendário e enviar.";
        } else if (temRascunho) {
            guia = "Alternativas geradas. Avança para o passo 2 (Alternativas) para analisar e comparar.";
        } else if (podeGerar) {
            guia = "Configura o período, seleciona a equipa e clica em 'Gerar horário'.";
        } else {
            guia = "Seleciona o período para consultar o planeamento.";
        }
        lblGuiaFluxo.setText(guia);
        lblGuiaFluxo.setVisible(true);
        lblGuiaFluxo.setManaged(true);
    }

    private <T> void executarOperacaoEmSegundoPlano(String mensagemProcessamento,
                                                    Callable<T> acao,
                                                    Consumer<T> onSuccess,
                                                    Consumer<Throwable> onFailure) {
        if (operacaoEmCurso != null && operacaoEmCurso.isRunning()) {
            mostrarInformacao("Existe uma operação em curso. Aguarda pela conclusão antes de iniciar outra.");
            return;
        }

        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return acao.call();
            }
        };

        operacaoEmCurso = task;
        atualizarEstadoInterativo();
        mostrarInformacao(mensagemProcessamento);

        task.setOnSucceeded(evento -> {
            operacaoEmCurso = null;
            atualizarEstadoInterativo();
            onSuccess.accept(task.getValue());
        });

        task.setOnFailed(evento -> {
            operacaoEmCurso = null;
            atualizarEstadoInterativo();
            Throwable erro = task.getException() != null
                    ? task.getException()
                    : new IllegalStateException("Ocorreu um erro inesperado durante a operação.");
            onFailure.accept(erro);
        });

        task.setOnCancelled(evento -> {
            operacaoEmCurso = null;
            atualizarEstadoInterativo();
            mostrarInformacao("A operação foi cancelada.");
        });

        Thread thread = new Thread(task, "geracao-horarios-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void validarUtilizadorAutenticado() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .trim();
    }

    private String resolverMensagemErro(Throwable erro, String fallback) {
        Throwable causaRaiz = encontrarCausaRaiz(erro);
        if (causaRaiz instanceof IllegalArgumentException
                && causaRaiz.getMessage() != null
                && !causaRaiz.getMessage().isBlank()) {
            return tornarMensagemErroApresentavel(causaRaiz.getMessage(), fallback);
        }
        return fallback;
    }

    private String tornarMensagemErroApresentavel(String mensagem, String fallback) {
        String mensagemNormalizada = mensagem != null ? mensagem.trim() : "";
        if (mensagemNormalizada.isBlank()) {
            return fallback;
        }

        boolean pareceDiagnosticoTecnico = mensagemNormalizada.length() > 420
                || mensagemNormalizada.contains("={")
                || mensagemNormalizada.contains("=[")
                || mensagemNormalizada.contains("@10:")
                || mensagemNormalizada.contains("@14:")
                || mensagemNormalizada.contains("@18:");

        if (!pareceDiagnosticoTecnico) {
            return mensagemNormalizada;
        }

        String mensagemCurta = mensagemNormalizada
                .replaceAll("\\s*\\[[\\s\\S]*", "")
                .replaceAll("\\s*\\{[\\s\\S]*", "")
                .trim();

        if (mensagemCurta.isBlank() || mensagemCurta.length() > 260) {
            mensagemCurta = fallback;
        }

        return mensagemCurta
                + " Revê a equipa selecionada, folgas/preferências aprovadas, descanso entre turnos, limite de horas e mínimo exigido por turno.";
    }

    private Throwable encontrarCausaRaiz(Throwable erro) {
        Throwable atual = erro;
        while (atual != null && atual.getCause() != null && atual.getCause() != atual) {
            atual = atual.getCause();
        }
        return atual != null ? atual : erro;
    }

    private Window obterJanela() {
        if (lblLoja == null || lblLoja.getScene() == null) {
            return null;
        }
        return lblLoja.getScene().getWindow();
    }

    private record PlaneamentoCarregadoDados(
            List<GeracaoHorariosBLL.PropostaResumo> propostas,
            GeracaoHorariosBLL.PropostaResultado resultado
    ) {
    }

    private record GeracaoAlternativasDados(
            List<GeracaoHorariosBLL.PropostaResumo> propostas,
            GeracaoHorariosBLL.PropostaResultado melhorResultado,
            int totalGeradas
    ) {
    }

    private record EnvioSupervisorDados(
            List<GeracaoHorariosBLL.PropostaResumo> propostas,
            GeracaoHorariosBLL.PropostaResultado propostaSelecionada,
            int totalEnviadas
    ) {
    }

    private record MesOption(int numero, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    private record FiltroColaboradorOption(Integer idColaborador, String nome) {

        private static FiltroColaboradorOption todos() {
            return new FiltroColaboradorOption(null, "Toda a equipa");
        }

        private boolean isTodos() {
            return idColaborador == null;
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    /** Abre diálogo para o gestor alterar o turno de um horário publicado. */
    private void abrirEdicaoTurno(GeracaoHorariosBLL.HorarioLinha linha, javafx.stage.Window owner) {
        try {
            List<com.example.projeto2.Modules.Turno> turnos = horarioBLL.listarTodosOsTurnos();
            if (turnos.isEmpty()) {
                mostrarErro("Sem turnos disponíveis.");
                return;
            }

            javafx.scene.control.ChoiceDialog<com.example.projeto2.Modules.Turno> dialogo =
                    new javafx.scene.control.ChoiceDialog<>(null, turnos);
            dialogo.setTitle("Editar turno");
            dialogo.setHeaderText("Colaborador: " + (linha.colaborador() != null ? linha.colaborador() : "-")
                    + "\nDia: " + (linha.data() != null ? linha.data() : "-")
                    + "\nTurno atual: " + (linha.turno() != null ? linha.turno() : "-"));
            dialogo.setContentText("Novo turno:");

            // Formatar turnos no dialog
            javafx.util.StringConverter<com.example.projeto2.Modules.Turno> conversor = new javafx.util.StringConverter<>() {
                @Override
                public String toString(com.example.projeto2.Modules.Turno t) {
                    if (t == null) return "-";
                    return (t.getTipo() != null ? t.getTipo() + " " : "")
                        + t.getHoraInicio() + " — " + t.getHoraFim();
                }
                @Override
                public com.example.projeto2.Modules.Turno fromString(String s) { return null; }
            };

            // Aplicar o conversor ao combo interno do ChoiceDialog
            if (dialogo.getDialogPane().lookupAll(".combo-box").stream()
                    .findFirst().orElse(null) instanceof javafx.scene.control.ComboBox<?> combo) {
                @SuppressWarnings("unchecked")
                javafx.scene.control.ComboBox<com.example.projeto2.Modules.Turno> turnoCombo =
                        (javafx.scene.control.ComboBox<com.example.projeto2.Modules.Turno>) combo;
                turnoCombo.setConverter(conversor);
                turnoCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(com.example.projeto2.Modules.Turno t, boolean empty) {
                        super.updateItem(t, empty);
                        setText(empty || t == null ? "-" : conversor.toString(t));
                    }
                });
                turnoCombo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                    @Override
                    protected void updateItem(com.example.projeto2.Modules.Turno t, boolean empty) {
                        super.updateItem(t, empty);
                        setText(empty || t == null ? "-" : conversor.toString(t));
                    }
                });
            }

            // Garantir que o ChoiceDialog aparece por cima do diálogo pai (detalhe do dia)
            if (owner != null) {
                dialogo.initOwner(owner);
            }
            java.util.Optional<com.example.projeto2.Modules.Turno> resultado = dialogo.showAndWait();
            resultado.ifPresent(novoTurno -> {
                try {
                    horarioBLL.editarTurnoPublicado(
                            linha.idHorario(),
                            novoTurno.getId(),
                            utilizadorLogado != null ? utilizadorLogado.getId() : null,
                            null
                    );
                    // Fechar o diálogo de detalhe do dia (owner) para evitar dados desatualizados
                    if (owner instanceof javafx.stage.Stage ownerStage) {
                        ownerStage.close();
                    }
                    mostrarSucesso("Turno alterado para " + conversor.toString(novoTurno) + " com sucesso.");
                    // Recarregar a proposta para refletir a alteração
                    if (propostaAtual != null && propostaAtual.idProposta() != null) {
                        carregarPropostaPorIdEmSegundoPlano(propostaAtual.idProposta(), false);
                    }
                } catch (IllegalArgumentException ex) {
                    mostrarErro(ex.getMessage());
                } catch (Exception ex) {
                    mostrarErro("Não foi possível alterar o turno.");
                }
            });
        } catch (Exception e) {
            mostrarErro("Não foi possível abrir o editor de turno.");
        }
    }
}
