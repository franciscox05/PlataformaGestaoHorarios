package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PermutaFolgaService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.DESKTOP.support.PermutaFolgaHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@Scope("prototype")
public class PermutasController {

    private static final DateTimeFormatter DATA_FORMATTER      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML fields — permuta de turno ────────────────────────────────────────────

    @FXML private ComboBox<Horario>      cbMeuTurno;
    @FXML private ComboBox<ColegaElegivel> cbColegaElegivel;
    @FXML private ComboBox<Horario>      cbTurnoColega;
    @FXML private Label                  lblMensagem;
    @FXML private Label                  lblTurnosElegiveis;
    @FXML private Button                 btnSubmeterTroca;
    @FXML private TableView<Permuta>     tabelaPedidosPermuta;
    @FXML private TableColumn<Permuta, String> colDataPedido;
    @FXML private TableColumn<Permuta, String> colTurnoOrigem;
    @FXML private TableColumn<Permuta, String> colTurnoDestino;
    @FXML private TableColumn<Permuta, String> colEstadoPermuta;
    @FXML private VBox                   painelAprovacao;
    @FXML private TableView<Permuta>     tabelaPedidosPendentes;
    @FXML private TableColumn<Permuta, String> colColaboradorPendente;
    @FXML private TableColumn<Permuta, String> colPedidoPendente;
    @FXML private TableColumn<Permuta, String> colOrigemPendente;
    @FXML private TableColumn<Permuta, String> colDestinoPendente;
    @FXML private Button                 btnAprovarPermuta;
    @FXML private Button                 btnRejeitarPermuta;

    // ── FXML fields — permuta de folga ────────────────────────────────────────────

    @FXML private ComboBox<Horario>       cbMeuTurnoFolga;
    @FXML private ComboBox<Horario>       cbCompensacaoFolga;
    @FXML private Button                  btnSubmeterPermutaFolga;
    @FXML private Label                   lblMensagemFolga;
    @FXML private TableView<PermutaFolga> tabelaPermutasFolga;
    @FXML private TableColumn<PermutaFolga, String> colPfDataPedido;
    @FXML private TableColumn<PermutaFolga, String> colPfDiaD;
    @FXML private TableColumn<PermutaFolga, String> colPfDiaY;
    @FXML private TableColumn<PermutaFolga, String> colPfEstado;
    @FXML private VBox                    painelAprovacaoFolga;
    @FXML private TableView<PermutaFolga> tabelaPendentesPermutaFolga;
    @FXML private TableColumn<PermutaFolga, String> colPfPendSolicitante;
    @FXML private TableColumn<PermutaFolga, String> colPfPendDiaD;
    @FXML private TableColumn<PermutaFolga, String> colPfPendDiaY;
    @FXML private Button                  btnAprovarPermutaFolga;
    @FXML private Button                  btnRejeitarPermutaFolga;

    // ── State ─────────────────────────────────────────────────────────────────────

    private final HorarioService      horarioBll;
    private final PermutaService      permutaBll;
    private final PermutaFolgaService permutaFolgaBll;
    private Utilizador utilizadorLogado;
    private List<Horario> turnosElegiveisAtuais = List.of();
    private PermutaFolgaHelper permutaFolgaHelper;

    public PermutasController(HorarioService horarioBll, PermutaService permutaBll,
                              PermutaFolgaService permutaFolgaBll) {
        this.horarioBll      = horarioBll;
        this.permutaBll      = permutaBll;
        this.permutaFolgaBll = permutaFolgaBll;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        configurarCombos();
        configurarTabelaHistorico();
        configurarTabelaPendentes();

        lblMensagem.setManaged(false);
        lblMensagem.setVisible(false);
        atualizarResumoElegiveis(0);

        cbMeuTurno.setTooltip(new Tooltip("Seleciona o teu turno que queres trocar"));
        cbColegaElegivel.setTooltip(new Tooltip("Escolhe o colega com quem queres trocar"));
        cbTurnoColega.setTooltip(new Tooltip("Seleciona o turno do colega que recebes em troca"));

        javafx.scene.layout.VBox emptyPermutas = new javafx.scene.layout.VBox(12);
        emptyPermutas.setAlignment(javafx.geometry.Pos.CENTER);
        emptyPermutas.setPadding(new javafx.geometry.Insets(40, 24, 40, 24));
        Label titulo = new Label("Nenhuma troca ainda");
        titulo.getStyleClass().add("empty-state-titulo");
        Label subtitulo = new Label("As tuas propostas de troca de turno aparecem aqui depois de as submeteres.");
        subtitulo.getStyleClass().add("empty-state-subtitulo");
        subtitulo.setWrapText(true);
        subtitulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button btnEmpty = new Button("Propor uma troca agora");
        btnEmpty.getStyleClass().add("botao-acao");
        btnEmpty.setOnAction(e -> cbMeuTurno.requestFocus());
        emptyPermutas.getChildren().addAll(titulo, subtitulo, btnEmpty);
        tabelaPedidosPermuta.setPlaceholder(emptyPermutas);

        tabelaPedidosPendentes.setPlaceholder(new Label("Não existem pedidos pendentes para aprovar."));

        btnSubmeterTroca.disableProperty().bind(
                cbMeuTurno.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbColegaElegivel.getSelectionModel().selectedItemProperty().isNull())
                        .or(cbTurnoColega.getSelectionModel().selectedItemProperty().isNull())
        );
        btnAprovarPermuta.disableProperty().bind(
                Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPermuta.disableProperty().bind(
                Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));

        painelAprovacao.setManaged(false);
        painelAprovacao.setVisible(false);

        permutaFolgaHelper = new PermutaFolgaHelper(
                cbMeuTurnoFolga, cbCompensacaoFolga, btnSubmeterPermutaFolga, lblMensagemFolga,
                tabelaPermutasFolga, colPfDataPedido, colPfDiaD, colPfDiaY, colPfEstado,
                painelAprovacaoFolga, tabelaPendentesPermutaFolga, colPfPendSolicitante, colPfPendDiaD, colPfPendDiaY,
                btnAprovarPermutaFolga, btnRejeitarPermutaFolga,
                permutaFolgaBll, this::obterJanela);
        permutaFolgaHelper.configurar();
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        carregarMeusTurnos();
        carregarHistorico();
        configurarPainelAprovacao();
        permutaFolgaHelper.carregarDados(utilizador);
    }

    // ── FXML event handlers — permuta de turno ────────────────────────────────────

    @FXML
    public void onSubmeterTrocaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            Horario meuTurno = cbMeuTurno.getValue();
            Horario turnoColega = cbTurnoColega.getValue();
            String nomeColega = cbColegaElegivel.getValue() != null ? cbColegaElegivel.getValue().nome() : "colega";
            String detalheTroca = String.format(
                    "O teu turno  →  %s%n"
                    + "será trocado com o turno de %s  →  %s%n%n"
                    + "O pedido ficará pendente para aprovação do gerente.",
                    formatarTurnoProprio(meuTurno), nomeColega, formatarTurnoProprio(turnoColega));

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(), "Confirmar troca de turno", "Confirmas este pedido de permuta?", detalheTroca)) {
                return;
            }
            permutaBll.registarPedidoTroca(utilizadorLogado.getId(), cbMeuTurno.getValue(), cbTurnoColega.getValue());
            mostrarMensagem("Pedido de permuta submetido com sucesso.", true);
            limparFormulario();
            carregarMeusTurnos();
            carregarHistorico();
            carregarPedidosPendentes();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível submeter o pedido de permuta.", false);
        }
    }

    @FXML public void onAprovarPermutaClick()  { tratarPedidoSelecionado(true); }
    @FXML public void onRejeitarPermutaClick() { tratarPedidoSelecionado(false); }

    // ── FXML event handlers — permuta de folga ────────────────────────────────────

    @FXML public void onSubmeterPermutaFolgaClick()  { permutaFolgaHelper.onSubmeter(); }
    @FXML public void onAprovarPermutaFolgaClick()   { permutaFolgaHelper.onAprovar(); }
    @FXML public void onRejeitarPermutaFolgaClick()  { permutaFolgaHelper.onRejeitar(); }

    // ── Combo configuration ───────────────────────────────────────────────────────

    private void configurarCombos() {
        cbMeuTurno.setConverter(new StringConverter<>() {
            @Override public String toString(Horario h) { return formatarTurnoProprio(h); }
            @Override public Horario fromString(String s) { return null; }
        });
        cbTurnoColega.setConverter(new StringConverter<>() {
            @Override public String toString(Horario h) { return formatarTurnoColega(h); }
            @Override public Horario fromString(String s) { return null; }
        });
        cbColegaElegivel.setConverter(new StringConverter<>() {
            @Override public String toString(ColegaElegivel c) { return c == null ? "" : c.nome(); }
            @Override public ColegaElegivel fromString(String s) { return null; }
        });
        cbMeuTurno.setOnAction(event -> carregarColegasElegiveis());
        cbColegaElegivel.setOnAction(event -> filtrarTurnosDoColega());
    }

    // ── Table configuration ───────────────────────────────────────────────────────

    private void configurarTabelaHistorico() {
        colDataPedido.setCellValueFactory(cd -> new SimpleStringProperty(formatarDataPedido(cd.getValue())));
        colTurnoOrigem.setCellValueFactory(cd -> new SimpleStringProperty(formatarTurnoProprio(cd.getValue().getIdHorarioOrigem())));
        colTurnoDestino.setCellValueFactory(cd -> new SimpleStringProperty(formatarTurnoColega(cd.getValue().getIdHorarioDestino())));
        colEstadoPermuta.setCellValueFactory(cd ->
                new SimpleStringProperty(formatarEstado(cd.getValue().getEstado() != null ? cd.getValue().getEstado().name() : null)));
        colEstadoPermuta.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) { setGraphic(null); setText(null); return; }
                Label badge = new Label(estado);
                badge.getStyleClass().add("badge-estado");
                switch (estado.toLowerCase()) {
                    case "pendente"  -> badge.getStyleClass().add("badge-pendente");
                    case "aprovado"  -> badge.getStyleClass().add("badge-aprovado");
                    case "rejeitado" -> badge.getStyleClass().add("badge-rejeitado");
                    default          -> badge.getStyleClass().add("badge-rascunho");
                }
                if ("pendente".equalsIgnoreCase(estado)) {
                    Button btnCancelar = new Button("✕");
                    btnCancelar.getStyleClass().add("botao-cancelar-pedido");
                    btnCancelar.setTooltip(new Tooltip("Cancelar este pedido pendente"));
                    btnCancelar.setOnAction(ev -> {
                        Permuta permuta = getTableView().getItems().get(getIndex());
                        cancelarPermutaPropria(permuta);
                    });
                    HBox cell = new HBox(6, badge, btnCancelar);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                } else {
                    setGraphic(badge);
                }
                setText(null);
            }
        });
    }

    private void configurarTabelaPendentes() {
        colColaboradorPendente.setCellValueFactory(cd -> new SimpleStringProperty(obterNomeSolicitante(cd.getValue())));
        colPedidoPendente.setCellValueFactory(cd -> new SimpleStringProperty(formatarDataPedido(cd.getValue())));
        colOrigemPendente.setCellValueFactory(cd -> new SimpleStringProperty(formatarTurnoProprio(cd.getValue().getIdHorarioOrigem())));
        colDestinoPendente.setCellValueFactory(cd -> new SimpleStringProperty(formatarTurnoColega(cd.getValue().getIdHorarioDestino())));
    }

    // ── Data loading ──────────────────────────────────────────────────────────────

    private void carregarMeusTurnos() {
        if (utilizadorLogado == null) { cbMeuTurno.setItems(FXCollections.observableArrayList()); return; }
        List<Horario> meusTurnos = horarioBll.listarMeusTurnosDisponiveisParaPermuta(utilizadorLogado.getId());
        cbMeuTurno.setItems(FXCollections.observableArrayList(meusTurnos));
    }

    private void carregarColegasElegiveis() {
        cbColegaElegivel.setValue(null);
        cbColegaElegivel.setItems(FXCollections.observableArrayList());
        cbTurnoColega.setValue(null);
        cbTurnoColega.setItems(FXCollections.observableArrayList());
        turnosElegiveisAtuais = List.of();

        Horario meuTurno = cbMeuTurno.getValue();
        if (utilizadorLogado == null || meuTurno == null || meuTurno.getId() == null) {
            atualizarResumoElegiveis(0);
            return;
        }
        turnosElegiveisAtuais = horarioBll.listarTurnosElegiveisParaPermuta(utilizadorLogado.getId(), meuTurno.getId());

        Map<Integer, ColegaElegivel> colegasPorId = new LinkedHashMap<>();
        for (Horario horario : turnosElegiveisAtuais) {
            if (horario.getIdLojautilizador() == null || horario.getIdLojautilizador().getIdUtilizador() == null) continue;
            Integer idColega  = horario.getIdLojautilizador().getIdUtilizador().getId();
            String nomeColega = horario.getIdLojautilizador().getIdUtilizador().getNome();
            colegasPorId.putIfAbsent(idColega, new ColegaElegivel(idColega, nomeColega));
        }
        cbColegaElegivel.setItems(FXCollections.observableArrayList(colegasPorId.values()));
        atualizarResumoElegiveis(turnosElegiveisAtuais.size());
        if (colegasPorId.size() == 1) cbColegaElegivel.getSelectionModel().selectFirst();
    }

    private void filtrarTurnosDoColega() {
        cbTurnoColega.setValue(null);
        ColegaElegivel colega = cbColegaElegivel.getValue();
        if (colega == null) { cbTurnoColega.setItems(FXCollections.observableArrayList()); return; }
        List<Horario> turnosDoColega = turnosElegiveisAtuais.stream()
                .filter(h -> h.getIdLojautilizador() != null)
                .filter(h -> h.getIdLojautilizador().getIdUtilizador() != null)
                .filter(h -> Objects.equals(h.getIdLojautilizador().getIdUtilizador().getId(), colega.idUtilizador()))
                .toList();
        cbTurnoColega.setItems(FXCollections.observableArrayList(turnosDoColega));
        if (turnosDoColega.size() == 1) cbTurnoColega.getSelectionModel().selectFirst();
    }

    private void atualizarResumoElegiveis(int totalTurnos) {
        if (lblTurnosElegiveis == null) return;
        if (totalTurnos == 0) {
            lblTurnosElegiveis.setText(cbMeuTurno != null && cbMeuTurno.getValue() != null
                    ? "Não existem turnos de colegas elegíveis para trocar com o turno selecionado."
                    : "Seleciona um dos teus turnos para ver as opções de troca disponíveis.");
        } else {
            lblTurnosElegiveis.setText(totalTurnos + " turno(s) de colegas disponíveis para troca neste dia.");
        }
    }

    private void carregarHistorico() {
        if (utilizadorLogado == null) { tabelaPedidosPermuta.setItems(FXCollections.observableArrayList()); return; }
        List<Permuta> pedidos = permutaBll.listarPedidosEnviados(utilizadorLogado.getId());
        tabelaPedidosPermuta.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void configurarPainelAprovacao() {
        boolean podeAprovar = utilizadorLogado != null && permutaBll.utilizadorPodeAprovarPermutas(utilizadorLogado.getId());
        painelAprovacao.setManaged(podeAprovar);
        painelAprovacao.setVisible(podeAprovar);
        if (podeAprovar) carregarPedidosPendentes();
        else tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
    }

    private void carregarPedidosPendentes() {
        if (utilizadorLogado == null || !permutaBll.utilizadorPodeAprovarPermutas(utilizadorLogado.getId())) {
            tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
            return;
        }
        List<Permuta> pedidos = permutaBll.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId());
        tabelaPedidosPendentes.setItems(FXCollections.observableArrayList(pedidos));
    }

    // ── Decision handlers ─────────────────────────────────────────────────────────

    private void tratarPedidoSelecionado(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            Permuta pedidoSelecionado = tabelaPedidosPendentes.getSelectionModel().getSelectedItem();
            if (pedidoSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um pedido pendente primeiro.");
            }
            String nomeOrigem  = obterNomeSolicitante(pedidoSelecionado);
            String turnoOrigem = formatarTurnoProprio(pedidoSelecionado.getIdHorarioOrigem());
            String nomeDestino = pedidoSelecionado.getIdHorarioDestino() != null
                    && pedidoSelecionado.getIdHorarioDestino().getIdLojautilizador() != null
                    && pedidoSelecionado.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador() != null
                    ? pedidoSelecionado.getIdHorarioDestino().getIdLojautilizador().getIdUtilizador().getNome()
                    : "Colega";
            String turnoDestino = formatarTurnoProprio(pedidoSelecionado.getIdHorarioDestino());
            String detalhes = String.format("%s  (%s)%n    ↔%n%s  (%s)", nomeOrigem, turnoOrigem, nomeDestino, turnoDestino);

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar permuta" : "Rejeitar permuta",
                    aprovar ? "Confirmas a aprovação desta troca de turno?" : "Confirmas a rejeição desta troca de turno?",
                    detalhes)) return;

            if (aprovar) {
                permutaBll.aprovarPedidoPermuta(pedidoSelecionado.getId(), utilizadorLogado.getId());
                mostrarMensagem("Pedido de permuta aprovado com sucesso.", true);
            } else {
                permutaBll.rejeitarPedidoPermuta(pedidoSelecionado.getId(), utilizadorLogado.getId());
                mostrarMensagem("Pedido de permuta rejeitado com sucesso.", true);
            }
            limparFormulario();
            carregarMeusTurnos();
            carregarHistorico();
            carregarPedidosPendentes();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível atualizar o pedido de permuta.", false);
        }
    }

    private void cancelarPermutaPropria(Permuta permuta) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            if (permuta == null) throw new IllegalArgumentException("Permuta inválida.");
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(), "Cancelar pedido de permuta", "Confirmas o cancelamento deste pedido?",
                    "Turno: " + formatarTurnoProprio(permuta.getIdHorarioOrigem()))) return;
            permutaBll.cancelarPedidoProprio(permuta.getId(), utilizadorLogado.getId());
            mostrarMensagem("Pedido de permuta cancelado.", true);
            carregarHistorico();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível cancelar o pedido de permuta.", false);
        }
    }

    // ── Form helpers ──────────────────────────────────────────────────────────────

    private void limparFormulario() {
        cbMeuTurno.setValue(null);
        cbColegaElegivel.setValue(null);
        cbColegaElegivel.setItems(FXCollections.observableArrayList());
        cbTurnoColega.setValue(null);
        cbTurnoColega.setItems(FXCollections.observableArrayList());
        turnosElegiveisAtuais = List.of();
        atualizarResumoElegiveis(0);
    }

    // ── Feedback ──────────────────────────────────────────────────────────────────

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        lblMensagem.setText(mensagem);
        lblMensagem.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagem.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagem.setManaged(true);
        lblMensagem.setVisible(true);
        if (sucesso) {
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            p.setOnFinished(e -> { lblMensagem.setManaged(false); lblMensagem.setVisible(false); });
            p.play();
        }
    }

    // ── Formatters ────────────────────────────────────────────────────────────────

    private String formatarTurnoProprio(Horario horario) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null) return "";
        return horario.getDataTurno().format(DATA_FORMATTER)
                + " | " + horario.getIdTurno().getHoraInicio()
                + " - " + horario.getIdTurno().getHoraFim();
    }

    private String formatarTurnoColega(Horario horario) {
        if (horario == null || horario.getIdLojautilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador() == null) return "";
        return horario.getIdLojautilizador().getIdUtilizador().getNome() + " | " + formatarTurnoProprio(horario);
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) return "-";
        return switch (estado.toLowerCase()) {
            case "pendente"  -> "Pendente";
            case "aprovado"  -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            case "cancelado" -> "Cancelado";
            default -> Character.toUpperCase(estado.charAt(0)) + estado.substring(1).toLowerCase();
        };
    }

    private String formatarDataPedido(Permuta permuta) {
        if (permuta == null || permuta.getDataPedido() == null) return "-";
        return DATA_HORA_FORMATTER.format(permuta.getDataPedido().atZone(ZoneId.systemDefault()));
    }

    private String obterNomeSolicitante(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() == null) return "-";
        return permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome();
    }

    private Window obterJanela() {
        if (cbMeuTurno == null || cbMeuTurno.getScene() == null) return null;
        return cbMeuTurno.getScene().getWindow();
    }

    public record ColegaElegivel(Integer idUtilizador, String nome) {}
}
