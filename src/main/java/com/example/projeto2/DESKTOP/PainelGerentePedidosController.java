package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PainelGerenteService;
import com.example.projeto2.API.Services.SnapshotOperacionalLojaService;
import com.example.projeto2.DESKTOP.support.CalendarioSemanalHelper;
import com.example.projeto2.DESKTOP.support.FolgasPainelSection;
import com.example.projeto2.DESKTOP.support.PainelPedidosCoordinator;
import com.example.projeto2.DESKTOP.support.PermutasPainelSection;
import com.example.projeto2.DESKTOP.support.PreferenciasPainelSection;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.descreverPedido;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.descreverPeriodoContexto;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarAusenciasColaborador;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTexto;
import static com.example.projeto2.DESKTOP.support.PedidosFormatters.formatarTurnosColaborador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class PainelGerentePedidosController {

    @FXML
    private Label lblLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private Label lblCargo;

    @FXML
    private Label lblTotalPendentes;

    @FXML
    private Label lblTotalFolgas;

    @FXML
    private Label lblTotalPermutas;

    @FXML
    private Label lblTotalPreferencias;

    @FXML
    private Label lblFeedbackFolgas;

    @FXML
    private Label lblFeedbackPermutas;

    @FXML
    private Label lblFeedbackPreferencias;

    @FXML
    private VBox painelContextoOperacional;

    @FXML
    private Label lblContextoPedidoSelecionado;

    @FXML
    private Label lblContextoPeriodo;

    @FXML
    private Label lblContextoResumo;

    @FXML
    private Label lblContextoColaboradoresEscalados;

    @FXML
    private Label lblContextoTurnosPlaneados;

    @FXML
    private Label lblContextoAusencias;

    @FXML
    private Label lblContextoPedidosPendentes;

    @FXML
    private TableView<SnapshotOperacionalLojaService.ColaboradorContexto> tabelaColaboradoresEnvolvidos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoColaborador;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoCargo;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoTurnos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoAusencias;

    @FXML
    private Label lblContextoColaboradorSelecionado;

    @FXML
    private HBox boxHorarioColaborador;

    @FXML
    private HBox boxEscalaLoja;

    @FXML
    private TableView<DayOff> tabelaFolgasPendentes;

    @FXML
    private TableColumn<DayOff, String> colFolgaColaborador;

    @FXML
    private TableColumn<DayOff, String> colFolgaData;

    @FXML
    private TableColumn<DayOff, String> colFolgaTipo;

    @FXML
    private TableColumn<DayOff, String> colFolgaMotivo;

    @FXML
    private Button btnAprovarFolga;

    @FXML
    private Button btnRejeitarFolga;

    @FXML
    private TableView<Permuta> tabelaPermutasPendentes;

    @FXML
    private TableColumn<Permuta, String> colPermutaColaborador;

    @FXML
    private TableColumn<Permuta, String> colPermutaPedido;

    @FXML
    private TableColumn<Permuta, String> colPermutaOrigem;

    @FXML
    private TableColumn<Permuta, String> colPermutaDestino;

    @FXML
    private Button btnAprovarPermuta;

    @FXML
    private Button btnRejeitarPermuta;

    @FXML
    private TableView<Preferencia> tabelaPreferenciasPendentes;

    @FXML
    private TableColumn<Preferencia, String> colPreferenciaColaborador;

    @FXML
    private TableColumn<Preferencia, String> colPreferenciaTipo;

    @FXML
    private TableColumn<Preferencia, String> colPreferenciaPeriodo;

    @FXML
    private TableColumn<Preferencia, String> colPreferenciaPrioridade;

    @FXML
    private TableColumn<Preferencia, String> colPreferenciaDescricao;

    @FXML
    private TextArea txtDecisaoPreferencia;

    @FXML
    private Button btnAprovarPreferencia;

    @FXML
    private Button btnRejeitarPreferencia;

    @FXML
    private Button btnAtalhoFolgas;

    @FXML
    private Button btnAtalhoPermutas;

    @FXML
    private Button btnAtalhoPreferencias;

    @FXML
    private Button btnAtalhoHorarios;

    private final PainelGerenteService painelGerenteBLL;
    private final SnapshotOperacionalLojaService snapshotOperacionalLojaBLL;
    private Utilizador utilizadorLogado;
    private boolean aSincronizarSelecao;
    private DashboardNavigator dashboardNavigation;
    private LocalDate inicioSemanaContextoAtual = CalendarioSemanalHelper.inicioSemana(LocalDate.now());

    private FolgasPainelSection folgasSection;
    private PermutasPainelSection permutasSection;
    private PreferenciasPainelSection preferenciasSection;

    public PainelGerentePedidosController(PainelGerenteService painelGerenteBLL,
                                          SnapshotOperacionalLojaService snapshotOperacionalLojaBLL) {
        this.painelGerenteBLL = painelGerenteBLL;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
    }

    @FXML
    public void initialize() {
        PainelPedidosCoordinator coord = new PainelPedidosCoordinator() {
            @Override
            public Integer obterIdUtilizadorLogado() {
                return utilizadorLogado != null ? utilizadorLogado.getId() : null;
            }

            @Override
            public Window obterJanela() {
                return PainelGerentePedidosController.this.obterJanela();
            }

            @Override
            public void aposAcaoBemSucedida() {
                limparContextoOperacional();
                carregarPainel();
                if (dashboardNavigation != null) dashboardNavigation.atualizarBadges();
            }
        };

        folgasSection = new FolgasPainelSection(
                tabelaFolgasPendentes,
                colFolgaColaborador, colFolgaData, colFolgaTipo, colFolgaMotivo,
                lblFeedbackFolgas, btnAprovarFolga, btnRejeitarFolga,
                painelGerenteBLL, coord);

        permutasSection = new PermutasPainelSection(
                tabelaPermutasPendentes,
                colPermutaColaborador, colPermutaPedido, colPermutaOrigem, colPermutaDestino,
                lblFeedbackPermutas, btnAprovarPermuta, btnRejeitarPermuta,
                painelGerenteBLL, coord);

        preferenciasSection = new PreferenciasPainelSection(
                tabelaPreferenciasPendentes,
                colPreferenciaColaborador, colPreferenciaTipo, colPreferenciaPeriodo,
                colPreferenciaPrioridade, colPreferenciaDescricao,
                txtDecisaoPreferencia, lblFeedbackPreferencias,
                btnAprovarPreferencia, btnRejeitarPreferencia,
                painelGerenteBLL, coord);

        folgasSection.configurar();
        permutasSection.configurar();
        preferenciasSection.configurar();

        configurarTabelaContexto();
        tabelaColaboradoresEnvolvidos.setPlaceholder(new Label("Seleciona um pedido para veres os colaboradores envolvidos."));

        btnAtalhoFolgas.setTooltip(new Tooltip("Abrir módulo de folgas (Ctrl+1)"));
        btnAtalhoPermutas.setTooltip(new Tooltip("Abrir módulo de permutas (Ctrl+2)"));
        btnAtalhoPreferencias.setTooltip(new Tooltip("Abrir módulo de preferências (Ctrl+3)"));
        btnAtalhoHorarios.setTooltip(new Tooltip("Abrir módulo de horários (Ctrl+4)"));

        configurarSelecaoContextual();
        configurarAtalhosRapidos();
        limparContextoOperacional();
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarPainel();
    }

    public void setDashboardNavigation(DashboardNavigator dashboardNavigation) {
        this.dashboardNavigation = dashboardNavigation;
    }

    @FXML
    public void onAprovarFolgaClick() {
        folgasSection.tratar(true);
    }

    @FXML
    public void onRejeitarFolgaClick() {
        folgasSection.tratar(false);
    }

    @FXML
    public void onAprovarPermutaClick() {
        permutasSection.tratar(true);
    }

    @FXML
    public void onRejeitarPermutaClick() {
        permutasSection.tratar(false);
    }

    @FXML
    public void onAprovarPreferenciaClick() {
        preferenciasSection.tratar(true);
    }

    @FXML
    public void onRejeitarPreferenciaClick() {
        preferenciasSection.tratar(false);
    }

    @FXML
    public void onAtalhoFolgasClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirFolgas();
        }
    }

    @FXML
    public void onAtalhoPermutasClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPermutas();
        }
    }

    @FXML
    public void onAtalhoPreferenciasClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPreferencias();
        }
    }

    @FXML
    public void onAtalhoHorariosClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirHorarios();
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
                if (evento.getCode() == KeyCode.DIGIT1) {
                    onAtalhoFolgasClick();
                    evento.consume();
                } else if (evento.getCode() == KeyCode.DIGIT2) {
                    onAtalhoPermutasClick();
                    evento.consume();
                } else if (evento.getCode() == KeyCode.DIGIT3) {
                    onAtalhoPreferenciasClick();
                    evento.consume();
                } else if (evento.getCode() == KeyCode.DIGIT4) {
                    onAtalhoHorariosClick();
                    evento.consume();
                }
            });
        });
    }

    private void configurarTabelaContexto() {
        colContextoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().nome())));
        colContextoCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().cargo())));
        colContextoTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnosColaborador(cellData.getValue().turnosNoPeriodo())));
        colContextoAusencias.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarAusenciasColaborador(cellData.getValue().ausenciasNoPeriodo())));
    }

    private void configurarSelecaoContextual() {
        folgasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA, nova.getIdDayoff());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        permutasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        preferenciasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        tabelaColaboradoresEnvolvidos.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) ->
                atualizarDetalheColaborador(nova));
    }

    private void carregarPainel() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            PainelGerenteService.PainelGerenteSnapshot snapshot = painelGerenteBLL.carregarPainel(utilizadorLogado.getId());

            lblLoja.setText(snapshot.contexto().nomeLoja());
            lblLocalizacao.setText(snapshot.contexto().localizacao());
            lblCargo.setText(snapshot.contexto().cargoGestao());

            lblTotalPendentes.setText(String.valueOf(snapshot.resumo().totalPendentes()));
            lblTotalFolgas.setText(String.valueOf(snapshot.resumo().folgasPendentes()));
            lblTotalPermutas.setText(String.valueOf(snapshot.resumo().permutasPendentes()));
            lblTotalPreferencias.setText(String.valueOf(snapshot.resumo().preferenciasPendentes()));

            folgasSection.mostrarDados(snapshot.folgasPendentes(), snapshot.nomesFolgasPendentes());
            permutasSection.mostrarDados(snapshot.permutasPendentes());
            preferenciasSection.mostrarDados(snapshot.preferenciasPendentes());

            if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        } catch (IllegalArgumentException e) {
            folgasSection.mostrarErro(e.getMessage());
        }
    }

    private void carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional tipoPedido, Integer idPedido) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            SnapshotOperacionalLojaService.ContextoPedidoOperacional contexto = snapshotOperacionalLojaBLL.carregarContextoPedido(
                    utilizadorLogado.getId(),
                    tipoPedido,
                    idPedido
            );

            inicioSemanaContextoAtual = CalendarioSemanalHelper.inicioSemana(
                    contexto.snapshotRelacionada().intervalo() != null
                            ? contexto.snapshotRelacionada().intervalo().dataInicio()
                            : LocalDate.now()
            );

            lblContextoPedidoSelecionado.setText(descreverPedido(contexto));
            lblContextoPeriodo.setText(descreverPeriodoContexto(contexto.snapshotRelacionada().intervalo()));
            lblContextoResumo.setText(contexto.pedido().resumo());

            lblContextoColaboradoresEscalados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().colaboradoresEscalados()));
            lblContextoTurnosPlaneados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().turnosPlaneados()));
            lblContextoAusencias.setText(String.valueOf(contexto.snapshotRelacionada().resumo().ausenciasAprovadas()));
            lblContextoPedidosPendentes.setText(String.valueOf(contexto.snapshotRelacionada().resumo().totalPedidosPendentes()));

            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList(contexto.colaboradoresEnvolvidos()));
            renderizarCalendarioEscalaLoja(construirEscalaDetalhada(contexto.snapshotRelacionada()), inicioSemanaContextoAtual);

            if (!contexto.colaboradoresEnvolvidos().isEmpty()) {
                tabelaColaboradoresEnvolvidos.getSelectionModel().selectFirst();
            } else {
                atualizarDetalheColaborador(null);
            }
        } catch (IllegalArgumentException e) {
            lblContextoPedidoSelecionado.setText("Não foi possível carregar o contexto operacional.");
            lblContextoPeriodo.setText("-");
            lblContextoResumo.setText(e.getMessage());
            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
            atualizarDetalheColaborador(null);
            renderizarCalendarioEscalaLoja(List.of(), CalendarioSemanalHelper.inicioSemana(LocalDate.now()));
        }
    }

    private void limparContextoOperacional() {
        inicioSemanaContextoAtual = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        lblContextoPedidoSelecionado.setText("Seleciona um pedido pendente para veres o contexto operacional.");
        lblContextoPeriodo.setText("-");
        lblContextoResumo.setText("Aqui vão aparecer os colaboradores envolvidos e a escala da loja no período relevante.");
        lblContextoColaboradoresEscalados.setText("0");
        lblContextoTurnosPlaneados.setText("0");
        lblContextoAusencias.setText("0");
        lblContextoPedidosPendentes.setText("0");
        tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
        renderizarCalendarioColaborador(List.of(), inicioSemanaContextoAtual);
        renderizarCalendarioEscalaLoja(List.of(), inicioSemanaContextoAtual);
        lblContextoColaboradorSelecionado.setText("Seleciona um colaborador envolvido para veres o horário em formato de calendário.");
    }

    private void limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional tipoMantido) {
        aSincronizarSelecao = true;
        try {
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA) {
                folgasSection.getTabela().getSelectionModel().clearSelection();
            }
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA) {
                permutasSection.getTabela().getSelectionModel().clearSelection();
            }
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA) {
                preferenciasSection.getTabela().getSelectionModel().clearSelection();
            }
        } finally {
            aSincronizarSelecao = false;
        }
    }

    private boolean haPedidoSelecionado() {
        return folgasSection.getTabela().getSelectionModel().getSelectedItem() != null
                || permutasSection.getTabela().getSelectionModel().getSelectedItem() != null
                || preferenciasSection.getTabela().getSelectionModel().getSelectedItem() != null;
    }

    private void atualizarDetalheColaborador(SnapshotOperacionalLojaService.ColaboradorContexto colaborador) {
        if (colaborador == null) {
            lblContextoColaboradorSelecionado.setText("Seleciona um colaborador envolvido para veres o horário em formato de calendário.");
            renderizarCalendarioColaborador(List.of(), inicioSemanaContextoAtual);
            return;
        }

        lblContextoColaboradorSelecionado.setText(formatarTexto(colaborador.nome()) + " | " + formatarTexto(colaborador.cargo()));
        renderizarCalendarioColaborador(colaborador.turnosNoPeriodo(), inicioSemanaContextoAtual);
    }

    private void renderizarCalendarioColaborador(List<SnapshotOperacionalLojaService.TurnoPlaneado> turnos, LocalDate dataBase) {
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();

        for (SnapshotOperacionalLojaService.TurnoPlaneado turno : turnos) {
            String evento = formatarTexto(turno.periodo())
                    + " | "
                    + formatarTexto(turno.turno())
                    + " | "
                    + formatarTexto(turno.estado());
            eventosPorDia.computeIfAbsent(turno.data(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxHorarioColaborador,
                CalendarioSemanalHelper.inicioSemana(dataBase != null ? dataBase : LocalDate.now()),
                eventosPorDia,
                "Sem horário"
        );
    }

    private void renderizarCalendarioEscalaLoja(List<LinhaEscalaDetalhe> linhas, LocalDate dataBase) {
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();

        for (LinhaEscalaDetalhe linha : linhas) {
            String evento = formatarTexto(linha.periodo())
                    + " | "
                    + formatarTexto(linha.colaborador())
                    + " ("
                    + formatarTexto(linha.cargo())
                    + ")";
            eventosPorDia.computeIfAbsent(linha.data(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxEscalaLoja,
                CalendarioSemanalHelper.inicioSemana(dataBase != null ? dataBase : LocalDate.now()),
                eventosPorDia,
                "Sem turnos"
        );
    }

    private List<LinhaEscalaDetalhe> construirEscalaDetalhada(SnapshotOperacionalLojaService.SnapshotOperacionalLoja snapshot) {
        List<LinhaEscalaDetalhe> linhas = new java.util.ArrayList<>();
        for (SnapshotOperacionalLojaService.ColaboradorEscala colaborador : snapshot.equipaEscalada()) {
            for (SnapshotOperacionalLojaService.TurnoPlaneado turno : colaborador.turnos()) {
                linhas.add(new LinhaEscalaDetalhe(
                        colaborador.idUtilizador(),
                        turno.data(),
                        colaborador.nome(),
                        colaborador.cargo(),
                        turno.turno(),
                        turno.periodo(),
                        turno.estado()
                ));
            }
        }

        linhas.sort(Comparator
                .comparing(LinhaEscalaDetalhe::data, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LinhaEscalaDetalhe::colaborador, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(LinhaEscalaDetalhe::periodo, String.CASE_INSENSITIVE_ORDER));
        return linhas;
    }

    private record LinhaEscalaDetalhe(
            Integer idUtilizador,
            LocalDate data,
            String colaborador,
            String cargo,
            String turno,
            String periodo,
            String estado
    ) {
    }

    private Window obterJanela() {
        if (lblLoja == null || lblLoja.getScene() == null) {
            return null;
        }
        return lblLoja.getScene().getWindow();
    }
}
