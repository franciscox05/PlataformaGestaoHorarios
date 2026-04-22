package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PainelGerenteBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class PainelGerentePedidosController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
    private TableView<SnapshotOperacionalLojaBLL.ColaboradorContexto> tabelaColaboradoresEnvolvidos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorContexto, String> colContextoColaborador;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorContexto, String> colContextoCargo;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorContexto, String> colContextoTurnos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorContexto, String> colContextoAusencias;

    @FXML
    private TableView<SnapshotOperacionalLojaBLL.ColaboradorEscala> tabelaEscalaLoja;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colEscalaColaborador;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colEscalaCargo;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colEscalaTurnos;

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

    private final PainelGerenteBLL painelGerenteBLL;
    private final SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;
    private Utilizador utilizadorLogado;
    private Map<Integer, String> nomesFolgasPendentes = Map.of();
    private boolean aSincronizarSelecao;
    private DashboardNavigator dashboardNavigation;

    public PainelGerentePedidosController(PainelGerenteBLL painelGerenteBLL,
                                          SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL) {
        this.painelGerenteBLL = painelGerenteBLL;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
    }

    @FXML
    public void initialize() {
        configurarTabelaFolgas();
        configurarTabelaPermutas();
        configurarTabelaPreferencias();
        configurarTabelasContexto();

        esconderFeedback(lblFeedbackFolgas);
        esconderFeedback(lblFeedbackPermutas);
        esconderFeedback(lblFeedbackPreferencias);
        limparContextoOperacional();

        tabelaFolgasPendentes.setPlaceholder(new Label("Nao existem pedidos de folga pendentes nesta loja."));
        tabelaPermutasPendentes.setPlaceholder(new Label("Nao existem pedidos de permuta pendentes nesta loja."));
        tabelaPreferenciasPendentes.setPlaceholder(new Label("Nao existem preferencias pendentes nesta loja."));
        tabelaColaboradoresEnvolvidos.setPlaceholder(new Label("Seleciona um pedido para ver os colaboradores envolvidos."));
        tabelaEscalaLoja.setPlaceholder(new Label("Seleciona um pedido para ver a escala da loja no periodo relevante."));

        btnAprovarFolga.disableProperty().bind(Bindings.isNull(tabelaFolgasPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarFolga.disableProperty().bind(Bindings.isNull(tabelaFolgasPendentes.getSelectionModel().selectedItemProperty()));
        btnAprovarPermuta.disableProperty().bind(Bindings.isNull(tabelaPermutasPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPermuta.disableProperty().bind(Bindings.isNull(tabelaPermutasPendentes.getSelectionModel().selectedItemProperty()));
        btnAprovarPreferencia.disableProperty().bind(Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPreferencia.disableProperty().bind(Bindings.isNull(tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty()));

        txtDecisaoPreferencia.textProperty().addListener((obs, oldValue, newValue) -> esconderFeedback(lblFeedbackPreferencias));
        tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            txtDecisaoPreferencia.clear();
            esconderFeedback(lblFeedbackPreferencias);
        });

        configurarSelecaoContextual();
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
        tratarFolga(true);
    }

    @FXML
    public void onRejeitarFolgaClick() {
        tratarFolga(false);
    }

    @FXML
    public void onAprovarPermutaClick() {
        tratarPermuta(true);
    }

    @FXML
    public void onRejeitarPermutaClick() {
        tratarPermuta(false);
    }

    @FXML
    public void onAprovarPreferenciaClick() {
        tratarPreferencia(true);
    }

    @FXML
    public void onRejeitarPreferenciaClick() {
        tratarPreferencia(false);
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

    private void configurarTabelaFolgas() {
        colFolgaColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(nomesFolgasPendentes.getOrDefault(
                        cellData.getValue().getIdUtilizador(),
                        "Utilizador #" + cellData.getValue().getIdUtilizador()
                )));

        colFolgaData.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarData(cellData.getValue().getDataAusencia())));

        colFolgaTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipoFolga(cellData.getValue().getTipo())));

        colFolgaMotivo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getMotivo())));
    }

    private void configurarTabelaPermutas() {
        colPermutaColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomePermuta(cellData.getValue())));

        colPermutaPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataHora(cellData.getValue().getDataPedido())));

        colPermutaOrigem.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurno(cellData.getValue().getIdHorarioOrigem(), false)));

        colPermutaDestino.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurno(cellData.getValue().getIdHorarioDestino(), true)));
    }

    private void configurarTabelaPreferencias() {
        colPreferenciaColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomePreferencia(cellData.getValue())));

        colPreferenciaTipo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipoPreferencia(cellData.getValue().getTipo())));

        colPreferenciaPeriodo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarPeriodo(cellData.getValue().getDataInicio(), cellData.getValue().getDataFim())));

        colPreferenciaPrioridade.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarVigencia(cellData.getValue())));

        colPreferenciaDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getDescricao())));
    }

    private void configurarTabelasContexto() {
        colContextoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().nome())));
        colContextoCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().cargo())));
        colContextoTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnosColaborador(cellData.getValue().turnosNoPeriodo())));
        colContextoAusencias.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarAusenciasColaborador(cellData.getValue().ausenciasNoPeriodo())));

        colEscalaColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().nome())));
        colEscalaCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().cargo())));
        colEscalaTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnosEscala(cellData.getValue().turnos())));
    }

    private void configurarSelecaoContextual() {
        tabelaFolgasPendentes.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.FOLGA);
                carregarContextoOperacional(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.FOLGA, nova.getIdDayoff());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        tabelaPermutasPendentes.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA);
                carregarContextoOperacional(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });

        tabelaPreferenciasPendentes.getSelectionModel().selectedItemProperty().addListener((obs, antiga, nova) -> {
            if (aSincronizarSelecao) {
                return;
            }

            if (nova != null) {
                limparSelecoesExcepto(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PREFERENCIA);
                carregarContextoOperacional(SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PREFERENCIA, nova.getId());
            } else if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        });
    }

    private void carregarPainel() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            PainelGerenteBLL.PainelGerenteSnapshot snapshot = painelGerenteBLL.carregarPainel(utilizadorLogado.getId());

            lblLoja.setText(snapshot.contexto().nomeLoja());
            lblLocalizacao.setText(snapshot.contexto().localizacao());
            lblCargo.setText(snapshot.contexto().cargoGestao());

            lblTotalPendentes.setText(String.valueOf(snapshot.resumo().totalPendentes()));
            lblTotalFolgas.setText(String.valueOf(snapshot.resumo().folgasPendentes()));
            lblTotalPermutas.setText(String.valueOf(snapshot.resumo().permutasPendentes()));
            lblTotalPreferencias.setText(String.valueOf(snapshot.resumo().preferenciasPendentes()));

            nomesFolgasPendentes = snapshot.nomesFolgasPendentes();
            tabelaFolgasPendentes.setItems(FXCollections.observableArrayList(snapshot.folgasPendentes()));
            tabelaFolgasPendentes.refresh();

            tabelaPermutasPendentes.setItems(FXCollections.observableArrayList(snapshot.permutasPendentes()));
            tabelaPermutasPendentes.refresh();

            tabelaPreferenciasPendentes.setItems(FXCollections.observableArrayList(snapshot.preferenciasPendentes()));
            tabelaPreferenciasPendentes.refresh();
            if (!haPedidoSelecionado()) {
                limparContextoOperacional();
            }
        } catch (IllegalArgumentException e) {
            mostrarFeedback(lblFeedbackFolgas, e.getMessage(), false);
        }
    }

    private void tratarFolga(boolean aprovar) {
        try {
            DayOff pedidoSelecionado = tabelaFolgasPendentes.getSelectionModel().getSelectedItem();
            if (pedidoSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um pedido de folga primeiro.");
            }

            if (aprovar) {
                painelGerenteBLL.aprovarFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                mostrarFeedback(lblFeedbackFolgas, "Pedido de folga aprovado com sucesso.", true);
            } else {
                painelGerenteBLL.rejeitarFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                mostrarFeedback(lblFeedbackFolgas, "Pedido de folga rejeitado com sucesso.", true);
            }

            tabelaFolgasPendentes.getSelectionModel().clearSelection();
            limparContextoOperacional();
            carregarPainel();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(lblFeedbackFolgas, e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback(lblFeedbackFolgas, "Nao foi possivel atualizar o pedido de folga.", false);
        }
    }

    private void tratarPermuta(boolean aprovar) {
        try {
            Permuta pedidoSelecionado = tabelaPermutasPendentes.getSelectionModel().getSelectedItem();
            if (pedidoSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um pedido de permuta primeiro.");
            }

            if (aprovar) {
                painelGerenteBLL.aprovarPermuta(pedidoSelecionado.getId(), utilizadorLogado.getId());
                mostrarFeedback(lblFeedbackPermutas, "Pedido de permuta aprovado com sucesso.", true);
            } else {
                painelGerenteBLL.rejeitarPermuta(pedidoSelecionado.getId(), utilizadorLogado.getId());
                mostrarFeedback(lblFeedbackPermutas, "Pedido de permuta rejeitado com sucesso.", true);
            }

            tabelaPermutasPendentes.getSelectionModel().clearSelection();
            limparContextoOperacional();
            carregarPainel();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(lblFeedbackPermutas, e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback(lblFeedbackPermutas, "Nao foi possivel atualizar o pedido de permuta.", false);
        }
    }

    private void tratarPreferencia(boolean aprovar) {
        try {
            Preferencia preferenciaSelecionada = tabelaPreferenciasPendentes.getSelectionModel().getSelectedItem();
            if (preferenciaSelecionada == null) {
                throw new IllegalArgumentException("Seleciona uma preferencia primeiro.");
            }

            if (aprovar) {
                painelGerenteBLL.aprovarPreferencia(preferenciaSelecionada.getId(), utilizadorLogado.getId(), txtDecisaoPreferencia.getText());
                mostrarFeedback(lblFeedbackPreferencias, "Preferencia aprovada com sucesso.", true);
            } else {
                painelGerenteBLL.rejeitarPreferencia(preferenciaSelecionada.getId(), utilizadorLogado.getId(), txtDecisaoPreferencia.getText());
                mostrarFeedback(lblFeedbackPreferencias, "Preferencia rejeitada com sucesso.", true);
            }

            txtDecisaoPreferencia.clear();
            tabelaPreferenciasPendentes.getSelectionModel().clearSelection();
            limparContextoOperacional();
            carregarPainel();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(lblFeedbackPreferencias, e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback(lblFeedbackPreferencias, "Nao foi possivel atualizar a preferencia.", false);
        }
    }

    private void carregarContextoOperacional(SnapshotOperacionalLojaBLL.TipoPedidoOperacional tipoPedido, Integer idPedido) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            SnapshotOperacionalLojaBLL.ContextoPedidoOperacional contexto = snapshotOperacionalLojaBLL.carregarContextoPedido(
                    utilizadorLogado.getId(),
                    tipoPedido,
                    idPedido
            );

            lblContextoPedidoSelecionado.setText(descreverPedido(contexto));
            lblContextoPeriodo.setText(descreverPeriodoContexto(contexto.snapshotRelacionada().intervalo()));
            lblContextoResumo.setText(contexto.pedido().resumo());

            lblContextoColaboradoresEscalados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().colaboradoresEscalados()));
            lblContextoTurnosPlaneados.setText(String.valueOf(contexto.snapshotRelacionada().resumo().turnosPlaneados()));
            lblContextoAusencias.setText(String.valueOf(contexto.snapshotRelacionada().resumo().ausenciasAprovadas()));
            lblContextoPedidosPendentes.setText(String.valueOf(contexto.snapshotRelacionada().resumo().totalPedidosPendentes()));

            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList(contexto.colaboradoresEnvolvidos()));
            tabelaEscalaLoja.setItems(FXCollections.observableArrayList(contexto.snapshotRelacionada().equipaEscalada()));
        } catch (IllegalArgumentException e) {
            lblContextoPedidoSelecionado.setText("Nao foi possivel carregar o contexto operacional.");
            lblContextoPeriodo.setText("-");
            lblContextoResumo.setText(e.getMessage());
            tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
            tabelaEscalaLoja.setItems(FXCollections.observableArrayList());
        }
    }

    private void limparContextoOperacional() {
        lblContextoPedidoSelecionado.setText("Seleciona um pedido pendente para veres o contexto operacional.");
        lblContextoPeriodo.setText("-");
        lblContextoResumo.setText("Aqui vao aparecer os colaboradores envolvidos e a escala da loja no periodo relevante.");
        lblContextoColaboradoresEscalados.setText("0");
        lblContextoTurnosPlaneados.setText("0");
        lblContextoAusencias.setText("0");
        lblContextoPedidosPendentes.setText("0");
        tabelaColaboradoresEnvolvidos.setItems(FXCollections.observableArrayList());
        tabelaEscalaLoja.setItems(FXCollections.observableArrayList());
    }

    private void limparSelecoesExcepto(SnapshotOperacionalLojaBLL.TipoPedidoOperacional tipoMantido) {
        aSincronizarSelecao = true;
        try {
            if (tipoMantido != SnapshotOperacionalLojaBLL.TipoPedidoOperacional.FOLGA) {
                tabelaFolgasPendentes.getSelectionModel().clearSelection();
            }
            if (tipoMantido != SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PERMUTA) {
                tabelaPermutasPendentes.getSelectionModel().clearSelection();
            }
            if (tipoMantido != SnapshotOperacionalLojaBLL.TipoPedidoOperacional.PREFERENCIA) {
                tabelaPreferenciasPendentes.getSelectionModel().clearSelection();
            }
        } finally {
            aSincronizarSelecao = false;
        }
    }

    private boolean haPedidoSelecionado() {
        return tabelaFolgasPendentes.getSelectionModel().getSelectedItem() != null
                || tabelaPermutasPendentes.getSelectionModel().getSelectedItem() != null
                || tabelaPreferenciasPendentes.getSelectionModel().getSelectedItem() != null;
    }

    private void mostrarFeedback(Label label, String mensagem, boolean sucesso) {
        label.setText(mensagem);
        label.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!label.getStyleClass().contains("mensagem-feedback")) {
            label.getStyleClass().add("mensagem-feedback");
        }
        label.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        label.setManaged(true);
        label.setVisible(true);
    }

    private void esconderFeedback(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
        label.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!label.getStyleClass().contains("mensagem-feedback")) {
            label.getStyleClass().add("mensagem-feedback");
        }
    }

    private String formatarData(LocalDate data) {
        if (data == null) {
            return "-";
        }
        return DATA_FORMATTER.format(data);
    }

    private String formatarDataHora(java.time.Instant dataPedido) {
        if (dataPedido == null) {
            return "-";
        }
        return DATA_HORA_FORMATTER.format(dataPedido.atZone(ZoneId.systemDefault()));
    }

    private String formatarTipoFolga(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }

        return switch (tipo.toLowerCase()) {
            case "ferias" -> "Ferias";
            case "folgas" -> "Folgas";
            case "baixa" -> "Baixa";
            default -> tipo;
        };
    }

    private String formatarTipoPreferencia(String tipo) {
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

    private String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) {
            return "Sem periodo";
        }

        if (dataInicio != null && dataFim != null) {
            return DATA_FORMATTER.format(dataInicio) + " a " + DATA_FORMATTER.format(dataFim);
        }

        return dataInicio != null ? formatarData(dataInicio) : formatarData(dataFim);
    }

    private String formatarVigencia(Preferencia preferencia) {
        if (preferencia == null) {
            return "-";
        }

        if (preferencia.getDataFim() == null && preferencia.getDataInicio() != null) {
            return ("colegas".equalsIgnoreCase(preferencia.getTipo()) || "turnos".equalsIgnoreCase(preferencia.getTipo()))
                    ? "Permanente"
                    : "Data unica";
        }

        if (preferencia.getDataInicio() != null || preferencia.getDataFim() != null) {
            return "Temporaria";
        }

        return "Sem periodo";
    }

    private String formatarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }
        return texto;
    }

    private String obterNomePermuta(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null || permuta.getIdHorarioOrigem().getIdLojautilizador() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() == null) {
            return "-";
        }

        return formatarTexto(permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome());
    }

    private String obterNomePreferencia(Preferencia preferencia) {
        if (preferencia == null || preferencia.getIdUtilizador() == null) {
            return "-";
        }

        return formatarTexto(preferencia.getIdUtilizador().getNome());
    }

    private String formatarTurno(com.example.projeto2.Modules.Horario horario, boolean incluirNome) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null) {
            return "-";
        }

        String base = DATA_FORMATTER.format(horario.getDataTurno())
                + " | "
                + horario.getIdTurno().getHoraInicio()
                + " - "
                + horario.getIdTurno().getHoraFim();

        if (!incluirNome
                || horario.getIdLojautilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador().getNome() == null) {
            return base;
        }

        return horario.getIdLojautilizador().getIdUtilizador().getNome() + " | " + base;
    }

    private String formatarTurnosColaborador(List<SnapshotOperacionalLojaBLL.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "Sem turnos planeados no periodo";
        }

        return turnos.stream()
                .map(turno -> formatarData(turno.data()) + " | " + turno.periodo())
                .reduce((primeiro, segundo) -> primeiro + "; " + segundo)
                .orElse("-");
    }

    private String formatarAusenciasColaborador(List<SnapshotOperacionalLojaBLL.AusenciaOperacional> ausencias) {
        if (ausencias == null || ausencias.isEmpty()) {
            return "Sem ausencias aprovadas no periodo";
        }

        return ausencias.stream()
                .map(ausencia -> formatarData(ausencia.data()) + " | " + ausencia.tipo())
                .reduce((primeiro, segundo) -> primeiro + "; " + segundo)
                .orElse("-");
    }

    private String formatarTurnosEscala(List<SnapshotOperacionalLojaBLL.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "Sem turnos planeados";
        }

        return turnos.stream()
                .map(turno -> formatarData(turno.data()) + " | " + turno.periodo())
                .reduce((primeiro, segundo) -> primeiro + "; " + segundo)
                .orElse("-");
    }

    private String descreverPedido(SnapshotOperacionalLojaBLL.ContextoPedidoOperacional contexto) {
        if (contexto == null || contexto.pedido() == null) {
            return "Contexto operacional indisponivel";
        }

        return switch (contexto.pedido().tipo()) {
            case FOLGA -> "Pedido de folga de " + contexto.pedido().colaboradorPrincipal();
            case PERMUTA -> "Pedido de permuta de " + contexto.pedido().colaboradorPrincipal();
            case PREFERENCIA -> "Preferencia de " + contexto.pedido().colaboradorPrincipal();
        };
    }

    private String descreverPeriodoContexto(SnapshotOperacionalLojaBLL.IntervaloOperacional intervalo) {
        if (intervalo == null) {
            return "-";
        }

        if (intervalo.unicoDia()) {
            return "Contexto de loja para " + formatarData(intervalo.dataInicio());
        }

        return "Contexto de loja de "
                + formatarData(intervalo.dataInicio())
                + " a "
                + formatarData(intervalo.dataFim());
    }
}
