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
import com.example.projeto2.DESKTOP.support.TabelaHelper;
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
import javafx.scene.control.TabPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
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
    @FXML private TabPane tabsPedidos;
    @FXML private TableView<DayOff>    tabelaFolgasPendentes;
    @FXML private TableColumn<DayOff, String> colFolgaColaborador;
    @FXML private TableColumn<DayOff, String> colFolgaData;
    @FXML private TableColumn<DayOff, String> colFolgaTipo;
    @FXML private TableColumn<DayOff, String> colFolgaMotivo;
    @FXML private Button btnAprovarFolga;
    @FXML private Button btnRejeitarFolga;
    @FXML private Button btnHistoricoFolgas;
    @FXML private Label lblPaginaFolgas;
    @FXML private Button btnPaginaAnteriorFolgas;
    @FXML private Button btnPaginaProximaFolgas;
    @FXML private TableView<Permuta>   tabelaPermutasPendentes;
    @FXML private TableColumn<Permuta, String> colPermutaColaborador;
    @FXML private TableColumn<Permuta, String> colPermutaPedido;
    @FXML private TableColumn<Permuta, String> colPermutaOrigem;
    @FXML private TableColumn<Permuta, String> colPermutaDestino;
    @FXML private Button btnAprovarPermuta;
    @FXML private Button btnRejeitarPermuta;
    @FXML private Button btnHistoricoPermutas;
    @FXML private Label lblPaginaPermutas;
    @FXML private Button btnPaginaAnteriorPermutas;
    @FXML private Button btnPaginaProximaPermutas;
    @FXML private TableView<Preferencia> tabelaPreferenciasPendentes;
    @FXML private TableColumn<Preferencia, String> colPreferenciaColaborador;
    @FXML private TableColumn<Preferencia, String> colPreferenciaTipo;
    @FXML private TableColumn<Preferencia, String> colPreferenciaDescricao;
    @FXML private TextArea txtDecisaoPreferencia;
    @FXML private Button btnAprovarPreferencia;
    @FXML private Button btnRejeitarPreferencia;
    @FXML private Button btnHistoricoPreferencias;
    @FXML private Label lblPaginaPreferencias;
    @FXML private Button btnPaginaAnteriorPreferencias;
    @FXML private Button btnPaginaProximaPreferencias;

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
                painelGerenteBLL, coord,
                lblPaginaFolgas, btnPaginaAnteriorFolgas, btnPaginaProximaFolgas);

        permutasSection = new PermutasPainelSection(
                tabelaPermutasPendentes,
                colPermutaColaborador, colPermutaPedido, colPermutaOrigem, colPermutaDestino,
                lblFeedbackPermutas, btnAprovarPermuta, btnRejeitarPermuta,
                painelGerenteBLL, coord,
                lblPaginaPermutas, btnPaginaAnteriorPermutas, btnPaginaProximaPermutas);

        preferenciasSection = new PreferenciasPainelSection(
                tabelaPreferenciasPendentes,
                colPreferenciaColaborador, colPreferenciaTipo, colPreferenciaDescricao,
                txtDecisaoPreferencia, lblFeedbackPreferencias,
                btnAprovarPreferencia, btnRejeitarPreferencia,
                painelGerenteBLL, coord,
                lblPaginaPreferencias, btnPaginaAnteriorPreferencias, btnPaginaProximaPreferencias);

        folgasSection.configurar();
        permutasSection.configurar();
        preferenciasSection.configurar();

        configurarTabelaContexto();
        tabelaColaboradoresEnvolvidos.setPlaceholder(new Label("Seleciona um pedido para veres os colaboradores envolvidos."));

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

    /**
     * Seleciona a aba inicial (Folgas/Permutas/Preferências) ao abrir o painel a partir de
     * outro ecrã (ex.: botão "Gerir em Pedidos" do perfil do colaborador). {@code tipo} aceita
     * variações como "folga", "permuta", "preferencia"/"ferias"/"folga_preferida".
     */
    public void selecionarAbaInicial(String tipo) {
        if (tabsPedidos == null || tipo == null) return;
        String t = java.text.Normalizer.normalize(tipo.trim().toLowerCase(java.util.Locale.ROOT),
                        java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        int indice;
        if (t.startsWith("permuta")) {
            indice = 1;
        } else if (t.startsWith("folga") && !t.contains("preferid")) {
            indice = 0; // pedido de ausência/folga
        } else {
            indice = 2; // preferências (inclui férias, folga_preferida, turnos, colegas…)
        }
        if (indice < tabsPedidos.getTabs().size()) {
            tabsPedidos.getSelectionModel().select(indice);
        }
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
        cMotivo.setCellFactory(col -> criarCelulaTextoLongo());

        tabela.getColumns().addAll(cColaborador, cData, cTipo, cEstado, cMotivo);
        tabela.setMinHeight(360);
        tabela.setRowFactory(tv -> {
            TableRow<DayOff> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    DayOff item = row.getItem();
                    String nome = item.getIdUtilizador() != null ? item.getIdUtilizador().getNome() : "-";
                    String motivo = item.getMotivo() != null && !item.getMotivo().isBlank() ? item.getMotivo() : "-";

                    LinkedHashMap<String, String> campos = new LinkedHashMap<>();
                    campos.put("Data de Ausência", item.getDataAusencia() != null ? item.getDataAusencia().format(DATA_FMT) : "-");
                    campos.put("Tipo", formatarTipoFolga(item.getTipo()));

                    mostrarDetalhePedido(row.getScene().getWindow(), "Pedido de Folga", nome, item.getEstado(),
                            campos, "Motivo", motivo);
                }
            });
            return row;
        });

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
        cDescricao.setCellFactory(col -> criarCelulaTextoLongo());

        tabela.setRowFactory(tv -> {
            TableRow<Preferencia> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Preferencia item = row.getItem();
                    String nome = item.getIdUtilizador() != null ? item.getIdUtilizador().getNome() : "-";
                    String descricao = item.getDescricao() != null && !item.getDescricao().isBlank()
                            ? item.getDescricao() : "-";

                    LinkedHashMap<String, String> campos = new LinkedHashMap<>();
                    campos.put("Período", (item.getDataInicio() != null ? item.getDataInicio().format(DATA_FMT) : "∞")
                            + " – " + (item.getDataFim() != null ? item.getDataFim().format(DATA_FMT) : "∞"));
                    campos.put("Tipo", capitalizar(item.getTipo()));

                    mostrarDetalhePedido(row.getScene().getWindow(), "Preferência", nome, item.getEstado(),
                            campos, "Descrição", descricao);
                }
            });
            return row;
        });

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
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node btnFechar = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnFechar instanceof Button b) {
            b.setText("Fechar");
            b.getStyleClass().add("botao-secundario");
        }
        dialog.showAndWait();
    }

    private void mostrarDetalhePedido(Window owner, String tipoPedido, String colaborador, String estado,
                                       LinkedHashMap<String, String> campos, String motivoTitulo, String motivoTexto) {
        Label lblTipo = new Label(tipoPedido.toUpperCase(Locale.ROOT));
        lblTipo.getStyleClass().add("detalhe-pedido-subtitulo");

        Label lblNome = new Label(colaborador);
        lblNome.getStyleClass().add("detalhe-pedido-nome");

        Label lblBadge = new Label(capitalizar(estado));
        lblBadge.getStyleClass().addAll("pedido-resumo-badge", classeBadgeEstado(estado));

        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);
        HBox linhaTopo = new HBox(10, lblNome, espacador, lblBadge);
        linhaTopo.setAlignment(Pos.CENTER_LEFT);

        VBox cabecalho = new VBox(2, lblTipo, linhaTopo);

        GridPane grade = new GridPane();
        grade.getStyleClass().add("detalhe-pedido-grid");
        grade.setHgap(28);
        grade.setVgap(12);
        int linha = 0;
        int coluna = 0;
        for (Map.Entry<String, String> campo : campos.entrySet()) {
            Label lblLabel = new Label(campo.getKey().toUpperCase(Locale.ROOT));
            lblLabel.getStyleClass().add("detalhe-pedido-campo-label");
            Label lblValor = new Label(campo.getValue());
            lblValor.getStyleClass().add("detalhe-pedido-campo-valor");
            lblValor.setWrapText(true);
            VBox bloco = new VBox(3, lblLabel, lblValor);
            grade.add(bloco, coluna, linha);
            coluna++;
            if (coluna == 2) {
                coluna = 0;
                linha++;
            }
        }

        Label lblMotivoTitulo = new Label(motivoTitulo);
        lblMotivoTitulo.getStyleClass().add("detalhe-pedido-motivo-titulo");

        TextArea area = new TextArea(motivoTexto);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(8);
        area.getStyleClass().add("detalhe-pedido-motivo-area");

        VBox conteudo = new VBox(14, cabecalho, grade, lblMotivoTitulo, area);
        conteudo.getStyleClass().add("detalhe-pedido-conteudo");
        conteudo.setPadding(new Insets(20));
        conteudo.setPrefWidth(480);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detalhe do Pedido");
        dialog.setHeaderText(null);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.getDialogPane().getStyleClass().add("detalhe-pedido-dialog");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node btnFechar = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnFechar instanceof Button b) {
            b.setText("Fechar");
            b.getStyleClass().add("botao-secundario");
        }
        dialog.showAndWait();
    }

    private String classeBadgeEstado(String estado) {
        if (estado == null) return "badge-rascunho";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "aprovado", "aprovada" -> "badge-aprovado";
            case "rejeitado", "rejeitada", "recusado", "recusada" -> "badge-rejeitado";
            case "pendente" -> "badge-pendente";
            default -> "badge-rascunho";
        };
    }

    private <T> TableCell<T, String> criarCelulaTextoLongo() {
        return new TableCell<>() {
            private final Label label = new Label();
            private final Tooltip tooltip = new Tooltip();
            {
                label.getStyleClass().add("texto-celula-longa");
                label.setWrapText(true);
                label.prefWidthProperty().bind(widthProperty().subtract(20));
            }

            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                if (empty || texto == null) {
                    setGraphic(null);
                    setText(null);
                    setTooltip(null);
                    return;
                }
                label.setText(texto);
                setGraphic(label);
                setText(null);
                tooltip.setText(texto + "\n\n(duplo clique no registo para ver na totalidade)");
                setTooltip(tooltip);
            }
        };
    }

    private <T> void abrirDialogoPaginado(String titulo, TableView<T> tabela, List<T> dados) {
        TabelaHelper.prepararTabela(tabela);
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
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/com/example/projeto2/dashboard/dashboard.css").toExternalForm());
        dialog.getDialogPane().setContent(conteudo);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        Node btnFechar = dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (btnFechar instanceof Button b) {
            b.setText("Fechar");
            b.getStyleClass().add("botao-secundario");
        }
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
                    ? "Preferência sem data de fim (permanente até o colaborador cancelar) · janela considerada: próximos 30 dias"
                    : descreverPeriodoContexto(contexto.snapshotRelacionada().intervalo()));
            lblContextoResumo.setText(contexto.pedido().resumo());
            lblContextoMotivoCompleto.setText(contexto.pedido().motivoCompleto());

            lblContextoColaboradoresEscalados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().colaboradoresEscalados()));
            lblContextoTurnosPlaneados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().turnosPlaneados()));
            lblContextoAusencias.setText(String.valueOf(contexto.snapshotRelacionada().resumo().ausenciasAprovadas()));
            lblContextoPedidosPendentes.setText(String.valueOf(contexto.snapshotRelacionada().resumo().totalPedidosPendentes()));

            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList(contexto.colaboradoresEnvolvidos()));

        } catch (IllegalArgumentException e) {
            lblContextoPedidoSelecionado.setText("Não foi possível carregar o contexto operacional.");
            lblContextoPeriodo.setText("-");
            lblContextoResumo.setText(e.getMessage());
            lblContextoMotivoCompleto.setText("-");
            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
        }
    }

    private void limparContextoOperacional() {
        lblContextoPedidoSelecionado.setText("Seleciona um pedido pendente para veres o contexto operacional.");
        lblContextoPeriodo.setText("-");
        lblContextoResumo.setText("Aqui vão aparecer os colaboradores envolvidos no período relevante.");
        lblContextoMotivoCompleto.setText("-");
        lblContextoColaboradoresEscalados.setText("0");
        lblContextoTurnosPlaneados.setText("0");
        lblContextoAusencias.setText("0");
        lblContextoPedidosPendentes.setText("0");
        tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
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
