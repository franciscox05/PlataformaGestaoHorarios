package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PainelGerenteService;
import com.example.projeto2.API.Services.SnapshotOperacionalLojaService;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PainelGerentePedidosController {

    private static final DateTimeFormatter DATA_FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int ITENS_POR_PAGINA = 25;

    @FXML private Label lblLoja;
    @FXML private Label lblLocalizacao;
    @FXML private Label lblCargo;
    @FXML private Label lblTotalPendentes;
    @FXML private Label lblTotalFolgas;
    @FXML private Label lblTotalPermutas;
    @FXML private Label lblTotalPreferencias;
    @FXML private Label lblFeedbackFolgas;
    @FXML private Label lblFeedbackPermutas;
    @FXML private Label lblFeedbackPreferencias;
    @FXML private VBox  painelContextoOperacional;
    @FXML private Label lblContextoPedidoSelecionado;
    @FXML private Label lblContextoPeriodo;
    @FXML private Label lblContextoResumo;
    @FXML private Label lblContextoMotivoCompleto;
    @FXML private Label lblContextoColaboradoresEscalados;
    @FXML private Label lblContextoTurnosPlaneados;
    @FXML private Label lblContextoAusencias;
    @FXML private Label lblContextoPedidosPendentes;
    @FXML private TableView<SnapshotOperacionalLojaService.ColaboradorContexto> tabelaColaboradoresEnvolvidos;
    @FXML private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoColaborador;
    @FXML private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoCargo;
    @FXML private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoTurnos;
    @FXML private TableColumn<SnapshotOperacionalLojaService.ColaboradorContexto, String> colContextoAusencias;
    @FXML private VBox  gradeHorario;
    @FXML private TableView<DayOff>    tabelaFolgasPendentes;
    @FXML private TableColumn<DayOff, String> colFolgaColaborador;
    @FXML private TableColumn<DayOff, String> colFolgaData;
    @FXML private TableColumn<DayOff, String> colFolgaTipo;
    @FXML private TableColumn<DayOff, String> colFolgaMotivo;
    @FXML private Button btnAprovarFolga;
    @FXML private Button btnRejeitarFolga;
    @FXML private Button btnHistoricoFolgas;
    @FXML private TableView<Permuta>   tabelaPermutasPendentes;
    @FXML private TableColumn<Permuta, String> colPermutaColaborador;
    @FXML private TableColumn<Permuta, String> colPermutaPedido;
    @FXML private TableColumn<Permuta, String> colPermutaOrigem;
    @FXML private TableColumn<Permuta, String> colPermutaDestino;
    @FXML private Button btnAprovarPermuta;
    @FXML private Button btnRejeitarPermuta;
    @FXML private Button btnHistoricoPermutas;
    @FXML private TableView<Preferencia> tabelaPreferenciasPendentes;
    @FXML private TableColumn<Preferencia, String> colPreferenciaColaborador;
    @FXML private TableColumn<Preferencia, String> colPreferenciaTipo;
    @FXML private TableColumn<Preferencia, String> colPreferenciaDescricao;
    @FXML private TextArea txtDecisaoPreferencia;
    @FXML private Button btnAprovarPreferencia;
    @FXML private Button btnRejeitarPreferencia;
    @FXML private Button btnHistoricoPreferencias;
    @FXML private Button btnAtalhoFolgas;
    @FXML private Button btnAtalhoPermutas;
    @FXML private Button btnAtalhoPreferencias;
    @FXML private Button btnAtalhoHorarios;

    private final PainelGerenteService painelGerenteBLL;
    private final SnapshotOperacionalLojaService snapshotOperacionalLojaBLL;
    private Utilizador utilizadorLogado;
    private boolean aSincronizarSelecao;
    private DashboardNavigator dashboardNavigation;

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
                colPreferenciaColaborador, colPreferenciaTipo, colPreferenciaDescricao,
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

    @FXML public void onAprovarFolgaClick()       { folgasSection.tratar(true); }
    @FXML public void onRejeitarFolgaClick()      { folgasSection.tratar(false); }
    @FXML public void onAprovarPermutaClick()     { permutasSection.tratar(true); }
    @FXML public void onRejeitarPermutaClick()    { permutasSection.tratar(false); }
    @FXML public void onAprovarPreferenciaClick() { preferenciasSection.tratar(true); }
    @FXML public void onRejeitarPreferenciaClick(){ preferenciasSection.tratar(false); }

    @FXML
    public void onHistoricoFolgasClick() {
        if (utilizadorLogado == null) return;
        try {
            List<DayOff> historico = painelGerenteBLL.listarHistoricoFolgas(utilizadorLogado.getId());
            abrirDialogoHistoricoFolgas(historico);
        } catch (Exception e) {
            folgasSection.mostrarErro("Não foi possível carregar o histórico de folgas.");
        }
    }

    @FXML
    public void onHistoricoPermutasClick() {
        if (utilizadorLogado == null) return;
        try {
            List<Permuta> historico = painelGerenteBLL.listarHistoricoPermutas(utilizadorLogado.getId());
            abrirDialogoHistoricoPermutas(historico);
        } catch (Exception e) {
            folgasSection.mostrarErro("Não foi possível carregar o histórico de permutas.");
        }
    }

    @FXML
    public void onHistoricoPreferenciasClick() {
        if (utilizadorLogado == null) return;
        try {
            List<Preferencia> historico = painelGerenteBLL.listarHistoricoPreferencias(utilizadorLogado.getId());
            abrirDialogoHistoricoPreferencias(historico);
        } catch (Exception e) {
            folgasSection.mostrarErro("Não foi possível carregar o histórico de preferências.");
        }
    }

    @FXML public void onAtalhoFolgasClick()       { if (dashboardNavigation != null) dashboardNavigation.abrirFolgas(); }
    @FXML public void onAtalhoPermutasClick()     { if (dashboardNavigation != null) dashboardNavigation.abrirPermutas(); }
    @FXML public void onAtalhoPreferenciasClick() { if (dashboardNavigation != null) dashboardNavigation.abrirPreferencias(); }
    @FXML public void onAtalhoHorariosClick()     { if (dashboardNavigation != null) dashboardNavigation.abrirHorarios(); }

    // ── History dialogs ───────────────────────────────────────────────────────────

    private void abrirDialogoHistoricoFolgas(List<DayOff> historico) {
        TableView<DayOff> tabela = new TableView<>();
        tabela.getStyleClass().add("tabela-premium");
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<DayOff, String> cColaborador = new TableColumn<>("Colaborador");
        cColaborador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIdUtilizador() != null ? c.getValue().getIdUtilizador().getNome() : "-"));
        TableColumn<DayOff, String> cData = new TableColumn<>("Data Ausência");
        cData.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataAusencia() != null ? c.getValue().getDataAusencia().format(DATA_FMT) : "-"));
        TableColumn<DayOff, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c -> new SimpleStringProperty(formatarTipoFolga(c.getValue().getTipo())));
        TableColumn<DayOff, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c -> new SimpleStringProperty(capitalizar(c.getValue().getEstado())));
        TableColumn<DayOff, String> cMotivo = new TableColumn<>("Motivo");
        cMotivo.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMotivo() != null && !c.getValue().getMotivo().isBlank() ? c.getValue().getMotivo() : "-"));

        tabela.getColumns().addAll(cColaborador, cData, cTipo, cEstado, cMotivo);
        tabela.setMinHeight(360);

        abrirDialogoPaginado("Histórico de Decisões — Folgas", tabela, historico);
    }

    private void abrirDialogoHistoricoPermutas(List<Permuta> historico) {
        TableView<Permuta> tabela = new TableView<>();
        tabela.getStyleClass().add("tabela-premium");
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Permuta, String> cSolicitante = new TableColumn<>("Solicitante");
        cSolicitante.setCellValueFactory(c -> {
            Permuta p = c.getValue();
            String nome = (p.getIdHorarioOrigem() != null && p.getIdHorarioOrigem().getIdLojautilizador() != null
                    && p.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() != null)
                    ? p.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome() : "-";
            return new SimpleStringProperty(nome);
        });
        TableColumn<Permuta, String> cColega = new TableColumn<>("Colega");
        cColega.setCellValueFactory(c -> {
            Permuta p = c.getValue();
            String nome = (p.getIdHorarioDestino() != null && p.getIdHorarioDestino().getIdLojautilizador() != null
                    && p.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador() != null)
                    ? p.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador().getNome() : "-";
            return new SimpleStringProperty(nome);
        });
        TableColumn<Permuta, String> cData = new TableColumn<>("Data Turno");
        cData.setCellValueFactory(c -> new SimpleStringProperty(
                (c.getValue().getIdHorarioOrigem() != null && c.getValue().getIdHorarioOrigem().getDataTurno() != null)
                        ? c.getValue().getIdHorarioOrigem().getDataTurno().format(DATA_FMT) : "-"));
        TableColumn<Permuta, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEstado() != null ? capitalizar(c.getValue().getEstado().name()) : "-"));
        TableColumn<Permuta, String> cPedido = new TableColumn<>("Data Pedido");
        cPedido.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDataPedido() != null
                        ? DATA_HORA_FMT.format(c.getValue().getDataPedido().atZone(ZoneId.systemDefault())) : "-"));

        tabela.getColumns().addAll(cSolicitante, cColega, cData, cEstado, cPedido);
        tabela.setMinHeight(360);

        abrirDialogoPaginado("Histórico de Decisões — Permutas", tabela, historico);
    }

    private void abrirDialogoHistoricoPreferencias(List<Preferencia> historicoInicial) {
        List<Preferencia> dados = new ArrayList<>(historicoInicial);

        TableView<Preferencia> tabela = new TableView<>();
        tabela.getStyleClass().add("tabela-premium");
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Preferencia, String> cColaborador = new TableColumn<>("Colaborador");
        cColaborador.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getIdUtilizador() != null ? c.getValue().getIdUtilizador().getNome() : "-"));
        TableColumn<Preferencia, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(c -> new SimpleStringProperty(capitalizar(c.getValue().getTipo())));
        TableColumn<Preferencia, String> cPeriodo = new TableColumn<>("Período");
        cPeriodo.setCellValueFactory(c -> {
            Preferencia p = c.getValue();
            String di = p.getDataInicio() != null ? p.getDataInicio().format(DATA_FMT) : "∞";
            String df = p.getDataFim()    != null ? p.getDataFim().format(DATA_FMT)    : "∞";
            return new SimpleStringProperty(di + " – " + df);
        });
        TableColumn<Preferencia, String> cEstado = new TableColumn<>("Estado");
        cEstado.setCellValueFactory(c -> new SimpleStringProperty(capitalizar(c.getValue().getEstado())));
        TableColumn<Preferencia, String> cDescricao = new TableColumn<>("Descrição");
        cDescricao.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescricao() != null && !c.getValue().getDescricao().isBlank()
                        ? c.getValue().getDescricao() : "-"));

        tabela.getColumns().addAll(cColaborador, cTipo, cPeriodo, cEstado, cDescricao);
        tabela.setMinHeight(300);

        int[] paginaAtual = {0};
        int totalPaginas[] = {Math.max(1, (int) Math.ceil((double) dados.size() / ITENS_POR_PAGINA))};

        Label lblPagina = new Label();
        Button btnAnterior = new Button("◀ Anterior");
        btnAnterior.getStyleClass().add("botao-secundario");
        Button btnProxima = new Button("Próxima ▶");
        btnProxima.getStyleClass().add("botao-secundario");

        Runnable atualizar = () -> {
            int total = dados.size();
            totalPaginas[0] = Math.max(1, (int) Math.ceil((double) total / ITENS_POR_PAGINA));
            if (paginaAtual[0] >= totalPaginas[0]) paginaAtual[0] = Math.max(0, totalPaginas[0] - 1);
            int inicio = paginaAtual[0] * ITENS_POR_PAGINA;
            int fim = Math.min(inicio + ITENS_POR_PAGINA, total);
            tabela.setItems(FXCollections.observableArrayList(total > 0 ? dados.subList(inicio, fim) : List.of()));
            lblPagina.setText("Pág. " + (paginaAtual[0] + 1) + " / " + totalPaginas[0]
                    + "  (" + total + " registos)");
            btnAnterior.setDisable(paginaAtual[0] == 0);
            btnProxima.setDisable(paginaAtual[0] >= totalPaginas[0] - 1);
        };

        btnAnterior.setOnAction(e -> { paginaAtual[0]--; atualizar.run(); });
        btnProxima.setOnAction(e -> { paginaAtual[0]++; atualizar.run(); });
        atualizar.run();

        Label lblRevogarInfo = new Label("Seleciona uma preferência aprovada para a revogar.");
        lblRevogarInfo.getStyleClass().add("texto-ajuda");

        Button btnRevogar = new Button("Revogar preferência");
        btnRevogar.getStyleClass().add("botao-perigo");
        btnRevogar.setDisable(true);

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) ->
                btnRevogar.setDisable(nova == null || !"aprovado".equalsIgnoreCase(nova.getEstado())));

        btnRevogar.setOnAction(e -> {
            Preferencia sel = tabela.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                painelGerenteBLL.rejeitarPreferencia(sel.getId(), utilizadorLogado.getId(), "Revogado pelo gestor");
                List<Preferencia> atualizado = painelGerenteBLL.listarHistoricoPreferencias(utilizadorLogado.getId());
                dados.clear();
                dados.addAll(atualizado);
                tabela.getSelectionModel().clearSelection();
                atualizar.run();
                carregarPainel();
            } catch (Exception ex) {
                folgasSection.mostrarErro("Não foi possível revogar a preferência.");
            }
        });

        HBox controlosPaginacao = new HBox(10, btnAnterior, lblPagina, btnProxima);
        controlosPaginacao.setAlignment(Pos.CENTER);
        controlosPaginacao.setPadding(new Insets(8, 0, 0, 0));

        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);
        HBox botoesAcao = new HBox(10, lblRevogarInfo, espacador, btnRevogar);
        botoesAcao.setAlignment(Pos.CENTER_LEFT);
        botoesAcao.setPadding(new Insets(8, 0, 0, 0));

        VBox conteudo = new VBox(10, tabela, controlosPaginacao, botoesAcao);
        conteudo.setPadding(new Insets(16));
        conteudo.setPrefWidth(860);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Histórico de Decisões — Preferências");
        dialog.setHeaderText(null);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(obterJanela());
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node btnFechar = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnFechar instanceof Button b) b.setText("Fechar");
        dialog.showAndWait();
    }

    private <T> void abrirDialogoPaginado(String titulo, TableView<T> tabela, List<T> dados) {
        int[] paginaAtual = {0};
        int total = dados.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / ITENS_POR_PAGINA));

        Label lblPagina = new Label();
        Button btnAnterior = new Button("◀ Anterior");
        btnAnterior.getStyleClass().add("botao-secundario");
        Button btnProxima  = new Button("Próxima ▶");
        btnProxima.getStyleClass().add("botao-secundario");

        Runnable atualizar = () -> {
            int inicio = paginaAtual[0] * ITENS_POR_PAGINA;
            int fim    = Math.min(inicio + ITENS_POR_PAGINA, total);
            tabela.setItems(FXCollections.observableArrayList(dados.subList(inicio, fim)));
            lblPagina.setText("Pág. " + (paginaAtual[0] + 1) + " / " + totalPaginas
                    + "  (" + total + " registos)");
            btnAnterior.setDisable(paginaAtual[0] == 0);
            btnProxima.setDisable(paginaAtual[0] >= totalPaginas - 1);
        };

        btnAnterior.setOnAction(e -> { paginaAtual[0]--; atualizar.run(); });
        btnProxima.setOnAction(e  -> { paginaAtual[0]++; atualizar.run(); });
        atualizar.run();

        HBox controlosPaginacao = new HBox(10, btnAnterior, lblPagina, btnProxima);
        controlosPaginacao.setAlignment(Pos.CENTER);
        controlosPaginacao.setPadding(new Insets(8, 0, 0, 0));

        VBox conteudo = new VBox(10, tabela, controlosPaginacao);
        conteudo.setPadding(new Insets(16));
        conteudo.setPrefWidth(860);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(obterJanela());
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node btnFechar = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnFechar instanceof Button b) b.setText("Fechar");
        dialog.showAndWait();
    }

    // ── Painel loading ────────────────────────────────────────────────────────────

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
                    utilizadorLogado.getId(), tipoPedido, idPedido);

            lblContextoPedidoSelecionado.setText(descreverPedido(contexto));
            boolean preferenciaPermamente = contexto.pedido().tipo() == SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA
                    && contexto.pedido().dataFim() == null;
            lblContextoPeriodo.setText(preferenciaPermamente
                    ? "Preferência sem data de fim (permanente até o colaborador cancelar) · escala de contexto: próximos 30 dias"
                    : descreverPeriodoContexto(contexto.snapshotRelacionada().intervalo()));
            lblContextoResumo.setText(contexto.pedido().resumo());
            lblContextoMotivoCompleto.setText(contexto.pedido().motivoCompleto());

            lblContextoColaboradoresEscalados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().colaboradoresEscalados()));
            lblContextoTurnosPlaneados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().turnosPlaneados()));
            lblContextoAusencias.setText(String.valueOf(contexto.snapshotRelacionada().resumo().ausenciasAprovadas()));
            lblContextoPedidosPendentes.setText(String.valueOf(contexto.snapshotRelacionada().resumo().totalPedidosPendentes()));

            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList(contexto.colaboradoresEnvolvidos()));
            renderizarGradeHorario(contexto.snapshotRelacionada());

        } catch (IllegalArgumentException e) {
            lblContextoPedidoSelecionado.setText("Não foi possível carregar o contexto operacional.");
            lblContextoPeriodo.setText("-");
            lblContextoResumo.setText(e.getMessage());
            lblContextoMotivoCompleto.setText("-");
            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
            gradeHorario.getChildren().clear();
        }
    }

    private void limparContextoOperacional() {
        lblContextoPedidoSelecionado.setText("Seleciona um pedido pendente para veres o contexto operacional.");
        lblContextoPeriodo.setText("-");
        lblContextoResumo.setText("Aqui vão aparecer os colaboradores envolvidos e a escala da loja no período relevante.");
        lblContextoMotivoCompleto.setText("-");
        lblContextoColaboradoresEscalados.setText("0");
        lblContextoTurnosPlaneados.setText("0");
        lblContextoAusencias.setText("0");
        lblContextoPedidosPendentes.setText("0");
        tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
        gradeHorario.getChildren().clear();
    }

    // ── Schedule grid ─────────────────────────────────────────────────────────────

    private void renderizarGradeHorario(SnapshotOperacionalLojaService.SnapshotOperacionalLoja snapshot) {
        gradeHorario.getChildren().clear();

        if (snapshot == null || snapshot.equipaEscalada().isEmpty()) {
            Label vazio = new Label("Sem turnos publicados no período.");
            vazio.getStyleClass().add("texto-ajuda");
            gradeHorario.getChildren().add(vazio);
            return;
        }

        List<LocalDate> datas = snapshot.equipaEscalada().stream()
                .flatMap(c -> c.turnos().stream())
                .map(SnapshotOperacionalLojaService.TurnoPlaneado::data)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (datas.isEmpty()) {
            Label vazio = new Label("Sem turnos publicados no período.");
            vazio.getStyleClass().add("texto-ajuda");
            gradeHorario.getChildren().add(vazio);
            return;
        }

        GridPane grade = new GridPane();
        grade.setHgap(4);
        grade.setVgap(3);
        grade.setPadding(new Insets(4));

        ColumnConstraints colNome = new ColumnConstraints(110, 130, 160);
        grade.getColumnConstraints().add(colNome);
        for (int i = 0; i < datas.size(); i++) {
            ColumnConstraints col = new ColumnConstraints(60, 72, Double.MAX_VALUE);
            col.setHgrow(Priority.SOMETIMES);
            grade.getColumnConstraints().add(col);
        }

        Label cabNome = new Label("Colaborador");
        cabNome.getStyleClass().addAll("campo-titulo");
        cabNome.setMaxWidth(Double.MAX_VALUE);
        grade.add(cabNome, 0, 0);

        DateTimeFormatter fmtCab = DateTimeFormatter.ofPattern("dd/MM\nEEE", Locale.forLanguageTag("pt-PT"));
        for (int c = 0; c < datas.size(); c++) {
            Label lblDia = new Label(datas.get(c).format(fmtCab));
            lblDia.getStyleClass().add("campo-titulo");
            lblDia.setAlignment(Pos.CENTER);
            lblDia.setMaxWidth(Double.MAX_VALUE);
            grade.add(lblDia, c + 1, 0);
        }

        int row = 1;
        for (SnapshotOperacionalLojaService.ColaboradorEscala colaborador : snapshot.equipaEscalada()) {
            Label lblNome = new Label(colaborador.nome());
            lblNome.getStyleClass().add("texto-ajuda");
            lblNome.setWrapText(true);
            lblNome.setMaxWidth(Double.MAX_VALUE);
            grade.add(lblNome, 0, row);

            Map<LocalDate, List<SnapshotOperacionalLojaService.TurnoPlaneado>> porDia = new LinkedHashMap<>();
            for (SnapshotOperacionalLojaService.TurnoPlaneado t : colaborador.turnos()) {
                porDia.computeIfAbsent(t.data(), k -> new ArrayList<>()).add(t);
            }

            for (int c = 0; c < datas.size(); c++) {
                List<SnapshotOperacionalLojaService.TurnoPlaneado> turnos = porDia.getOrDefault(datas.get(c), List.of());
                String texto = turnos.isEmpty() ? "-"
                        : turnos.stream().map(SnapshotOperacionalLojaService.TurnoPlaneado::periodo).collect(Collectors.joining("\n"));
                Label lblCel = new Label(texto);
                lblCel.getStyleClass().add("texto-ajuda");
                lblCel.setAlignment(Pos.CENTER);
                lblCel.setWrapText(true);
                lblCel.setMaxWidth(Double.MAX_VALUE);
                grade.add(lblCel, c + 1, row);
            }
            row++;
        }

        gradeHorario.getChildren().add(grade);
    }

    // ── Selection coordination ────────────────────────────────────────────────────

    private void configurarSelecaoContextual() {
        folgasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) return;
            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA, nova.getIdDayoff());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        permutasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) return;
            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        preferenciasSection.getTabela().getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) return;
            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA);
                carregarContextoOperacional(SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });
    }

    private void limparSelecoesExcepto(SnapshotOperacionalLojaService.TipoPedidoOperacional tipoMantido) {
        aSincronizarSelecao = true;
        try {
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.FOLGA)
                folgasSection.getTabela().getSelectionModel().clearSelection();
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.PERMUTA)
                permutasSection.getTabela().getSelectionModel().clearSelection();
            if (tipoMantido != SnapshotOperacionalLojaService.TipoPedidoOperacional.PREFERENCIA)
                preferenciasSection.getTabela().getSelectionModel().clearSelection();
        } finally {
            aSincronizarSelecao = false;
        }
    }

    private boolean haPedidoSelecionado() {
        return folgasSection.getTabela().getSelectionModel().getSelectedItem() != null
                || permutasSection.getTabela().getSelectionModel().getSelectedItem() != null
                || preferenciasSection.getTabela().getSelectionModel().getSelectedItem() != null;
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

    private void configurarAtalhosRapidos() {
        lblLoja.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) return;
            nova.setOnKeyPressed(evento -> {
                if (!evento.isControlDown()) return;
                if (evento.getCode() == KeyCode.DIGIT1) { onAtalhoFolgasClick();       evento.consume(); }
                else if (evento.getCode() == KeyCode.DIGIT2) { onAtalhoPermutasClick(); evento.consume(); }
                else if (evento.getCode() == KeyCode.DIGIT3) { onAtalhoPreferenciasClick(); evento.consume(); }
                else if (evento.getCode() == KeyCode.DIGIT4) { onAtalhoHorariosClick(); evento.consume(); }
            });
        });
    }

    // ── Formatters ────────────────────────────────────────────────────────────────

    private String formatarTipoFolga(String tipo) {
        if (tipo == null || tipo.isBlank()) return "-";
        return switch (tipo.toLowerCase()) {
            case "ferias"   -> "Férias";
            case "folgas"   -> "Folgas";
            case "baixa"    -> "Baixa";
            case "urgente"  -> "⚡ Urgente";
            default         -> capitalizar(tipo);
        };
    }

    private String capitalizar(String valor) {
        if (valor == null || valor.isBlank()) return "-";
        String s = valor.trim().toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private Window obterJanela() {
        if (lblLoja == null || lblLoja.getScene() == null) return null;
        return lblLoja.getScene().getWindow();
    }
}
