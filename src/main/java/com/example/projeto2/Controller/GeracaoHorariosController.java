package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
public class GeracaoHorariosController {

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

    // ── Calendário Mensal (adicionado na reestruturação visual) ──────────────
    @FXML
    private Button btnMesAnterior;

    @FXML
    private Button btnMesSeguinte;

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

    private final GeracaoHorariosBLL geracaoHorariosBLL;

    private Utilizador utilizadorLogado;
    private GeracaoHorariosBLL.PropostaResultado propostaAtual;
    private boolean podeGerar;
    private boolean podeValidar;
    private LocalDate semanaPlaneamentoInicio;
    private Task<?> operacaoEmCurso;
    private boolean suprimirCarregamentoPorSelecao;
    private YearMonth periodoMensalAtual = YearMonth.now();
    private final Map<Integer, CheckBox> selecaoColaboradoresGeracao = new LinkedHashMap<>();

    public GeracaoHorariosController(GeracaoHorariosBLL geracaoHorariosBLL) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
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
        // Inicializar o estado do calendário mensal (empty state visível, grid oculto até proposta)
        javafx.application.Platform.runLater(() -> {
            if (emptyStateCalendarioMensal != null) {
                emptyStateCalendarioMensal.setVisible(true);
                emptyStateCalendarioMensal.setManaged(true);
            }
            atualizarCalendarioMensal();
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
     *  Retorna mapa vazio se nao houver proposta carregada. */
    private java.util.Map<java.time.LocalDate, java.util.List<String>> obterEventosParaMes(YearMonth periodo) {
        if (propostaAtual == null || propostaAtual.linhas() == null) {
            return java.util.Collections.emptyMap();
        }
        java.util.Map<java.time.LocalDate, java.util.List<String>> mapa = new java.util.LinkedHashMap<>();
        for (GeracaoHorariosBLL.HorarioLinha linha : propostaAtual.linhas()) {
            if (linha == null || linha.data() == null) continue;
            java.time.LocalDate data = linha.data();
            if (!YearMonth.from(data).equals(periodo)) continue;
            String desc = (linha.periodo() != null ? linha.periodo() : "?")
                + " | " + (linha.colaborador() != null ? linha.colaborador() : "")
                + " (" + (linha.cargo() != null ? linha.cargo() : "-") + ")";
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
        return card;
    }

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
        cbFiltroColaborador.valueProperty().addListener((observavel, antigo, novo) -> aplicarFiltroColaborador());
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
                throw new IllegalArgumentException("Seleciona pelo menos um colaborador para gerar o horario.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    quantidade == 1 ? "Gerar alternativa" : "Gerar alternativas",
                    quantidade == 1 ? "Deseja gerar uma nova alternativa?" : "Deseja gerar " + quantidade + " alternativas?",
                    "A geracao vai usar " + idsColaboradoresSelecionados.size()
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
            lblResumoColaboradoresGeracao.setText("Nao foi possivel carregar a equipa elegivel para este periodo.");
        }
    }

    private void renderizarColaboradoresGeracao(List<GeracaoHorariosBLL.ColaboradorElegivel> colaboradores) {
        Set<Integer> selecionadosAnteriores = obterIdsColaboradoresSelecionadosComoSet();
        boolean devePreservarSelecao = !selecaoColaboradoresGeracao.isEmpty();

        selecaoColaboradoresGeracao.clear();
        boxColaboradoresGeracao.getChildren().clear();

        if (colaboradores == null || colaboradores.isEmpty()) {
            Label vazio = new Label("Sem colaboradores elegiveis para o periodo selecionado.");
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
            lblResumoColaboradoresGeracao.setText("Escolhe o periodo para carregar a equipa elegivel.");
        } else {
            lblResumoColaboradoresGeracao.setText(selecionados + " de " + total + " colaboradores selecionados para a geracao.");
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

        tabelaResumoColaboradores.setItems(FXCollections.observableArrayList(resultado.resumoColaboradores()));
        atualizarFiltroColaborador(resultado);
        aplicarFiltroColaborador();
        atualizarPainelValidacao();
        atualizarEstadoInterativo();
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
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        renderizarCalendarioPlaneamento(List.of());
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
                "Diagnostico da geracao",
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
                    "So as alternativas enviadas ficam disponiveis para aprovacao ou rejeicao."
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
                    erro -> mostrarErro(resolverMensagemErro(erro, "Nao foi possivel enviar as alternativas ao supervisor."))
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
                throw new IllegalArgumentException("Seleciona pelo menos um colaborador para gerar o horario.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    quantidade == 1 ? "Gerar alternativa" : "Gerar alternativas",
                    quantidade == 1 ? "Deseja gerar uma nova alternativa?" : "Deseja gerar " + quantidade + " alternativas?",
                    "A geracao vai usar " + idsColaboradoresSelecionados.size()
                            + " colaboradores selecionados. Depois podes enviar ao supervisor apenas as alternativas escolhidas."
            )) {
                return;
            }

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
                        aplicarListaPropostas(dados.propostas(), dados.melhorResultado().idProposta());
                        preencherResultado(dados.melhorResultado());
                        selecionarPropostaNaTabela(dados.melhorResultado().idProposta());
                        esconderFeedbackValidacao();
                        esconderDiagnosticoGeracao();
                        mostrarSucesso(dados.totalGeradas() == 1
                                ? "Alternativa gerada como rascunho. Seleciona-a na tabela e envia ao supervisor quando estiver pronta."
                                : dados.totalGeradas() + " alternativas geradas como rascunho. A melhor pontuacao ficou selecionada para analise.");
                    },
                    erro -> {
                        String mensagem = resolverMensagemErro(erro, "Não foi possível gerar alternativas para o período selecionado.");
                        if (tentarCarregarPlaneamentoExistente()) {
                            esconderFeedbackValidacao();
                            mostrarErro(mensagem + " O planeamento atual foi carregado abaixo para facilitar a análise.");
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
                + " Revê a equipa selecionada, folgas/preferencias aprovadas, descanso entre turnos, limite de horas e minimo exigido por turno.";
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
}
