package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PermutaFolgaService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

public final class PermutaFolgaHelper {

    private static final DateTimeFormatter DATA_FORMATTER      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComboBox<Horario> cbMeuTurnoFolga;
    private final ComboBox<Horario> cbCompensacaoFolga;
    private final Button btnSubmeterPermutaFolga;
    private final Label lblMensagemFolga;
    private final TableView<PermutaFolga> tabelaPermutasFolga;
    private final TableColumn<PermutaFolga, String> colPfDataPedido;
    private final TableColumn<PermutaFolga, String> colPfDiaD;
    private final TableColumn<PermutaFolga, String> colPfDiaY;
    private final TableColumn<PermutaFolga, String> colPfEstado;
    private final TableColumn<PermutaFolga, PermutaFolga> colPfAcao;

    private final PermutaFolgaService service;
    private final Supplier<Window> janelaSupplier;

    private Utilizador utilizadorAtual;
    private List<Horario> compensacoesElegiveis = List.of();

    public PermutaFolgaHelper(
            ComboBox<Horario> cbMeuTurnoFolga,
            ComboBox<Horario> cbCompensacaoFolga,
            Button btnSubmeterPermutaFolga,
            Label lblMensagemFolga,
            TableView<PermutaFolga> tabelaPermutasFolga,
            TableColumn<PermutaFolga, String> colPfDataPedido,
            TableColumn<PermutaFolga, String> colPfDiaD,
            TableColumn<PermutaFolga, String> colPfDiaY,
            TableColumn<PermutaFolga, String> colPfEstado,
            TableColumn<PermutaFolga, PermutaFolga> colPfAcao,
            PermutaFolgaService service,
            Supplier<Window> janelaSupplier) {
        this.cbMeuTurnoFolga         = cbMeuTurnoFolga;
        this.cbCompensacaoFolga      = cbCompensacaoFolga;
        this.btnSubmeterPermutaFolga = btnSubmeterPermutaFolga;
        this.lblMensagemFolga        = lblMensagemFolga;
        this.tabelaPermutasFolga     = tabelaPermutasFolga;
        this.colPfDataPedido         = colPfDataPedido;
        this.colPfDiaD               = colPfDiaD;
        this.colPfDiaY               = colPfDiaY;
        this.colPfEstado             = colPfEstado;
        this.colPfAcao               = colPfAcao;
        this.service                 = service;
        this.janelaSupplier          = janelaSupplier;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────────

    public void configurar() {
        cbMeuTurnoFolga.setConverter(new StringConverter<>() {
            @Override public String toString(Horario h) { return formatarTurnoFolga(h); }
            @Override public Horario fromString(String s) { return null; }
        });
        cbCompensacaoFolga.setConverter(new StringConverter<>() {
            @Override public String toString(Horario h) { return formatarTurnoCompensacao(h); }
            @Override public Horario fromString(String s) { return null; }
        });
        cbMeuTurnoFolga.setOnAction(e -> carregarCompensacoes());

        colPfDataPedido.setCellValueFactory(c -> new SimpleStringProperty(formatarDataPedido(c.getValue())));
        colPfDiaD.setCellValueFactory(c -> new SimpleStringProperty(formatarTurnoFolga(c.getValue().getIdHorarioD())));
        colPfDiaY.setCellValueFactory(c -> new SimpleStringProperty(formatarTurnoCompensacao(c.getValue().getIdHorarioY())));
        colPfEstado.setCellValueFactory(c -> new SimpleStringProperty(formatarEstado(c.getValue().getEstado())));
        colPfEstado.setCellFactory(col -> criarCelulaEstado());
        colPfAcao.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue()));
        colPfAcao.setCellFactory(col -> criarCelulaAcao());

        btnSubmeterPermutaFolga.disableProperty().bind(
                cbMeuTurnoFolga.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbCompensacaoFolga.getSelectionModel().selectedItemProperty().isNull()));

        tabelaPermutasFolga.setPlaceholder(new Label("Nenhuma permuta de folga encontrada."));

        if (lblMensagemFolga != null) { lblMensagemFolga.setManaged(false); lblMensagemFolga.setVisible(false); }
    }

    public void carregarDados(Utilizador utilizador) {
        this.utilizadorAtual = utilizador;
        carregarMeusTurnos();
        try { carregarHistorico(); } catch (Exception e) { tabelaPermutasFolga.setItems(FXCollections.observableArrayList()); }
    }

    // ── Public delegate ───────────────────────────────────────────────────────────

    public void onSubmeter() {
        try {
            if (utilizadorAtual == null) throw new IllegalArgumentException("Utilizador não identificado.");
            Horario horarioD = cbMeuTurnoFolga.getValue();
            Horario horarioY = cbCompensacaoFolga.getValue();
            if (horarioD == null || horarioY == null) {
                throw new IllegalArgumentException("Seleciona o teu turno e o turno de compensação.");
            }
            String colega = horarioY.getIdLojautilizador() != null
                    && horarioY.getIdLojautilizador().getIdUtilizador() != null
                    ? horarioY.getIdLojautilizador().getIdUtilizador().getNome() : "colega";
            String detalhe = String.format(
                    "O teu turno (%s) será atribuído a %s.%n"
                    + "O turno de %s (%s) será atribuído a ti como compensação.%n%n"
                    + "O pedido fica pendente para aprovação do supervisor.",
                    formatarTurnoFolga(horarioD), colega, colega, formatarTurnoFolga(horarioY));

            if (!DialogosHelper.confirmarAcao(janelaSupplier.get(),
                    "Confirmar permuta de folga", "Confirmas este pedido de permuta de folga?", detalhe)) return;

            service.registarPedido(utilizadorAtual.getId(), horarioD, horarioY);
            mostrarMensagem("Pedido de permuta de folga submetido. Aguarda aprovação do supervisor.", true);
            cbMeuTurnoFolga.setValue(null);
            cbCompensacaoFolga.setValue(null);
            cbCompensacaoFolga.setItems(FXCollections.observableArrayList());
            compensacoesElegiveis = List.of();
            carregarMeusTurnos();
            carregarHistorico();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível submeter o pedido de permuta de folga.", false);
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────────

    private void carregarMeusTurnos() {
        if (utilizadorAtual == null) { cbMeuTurnoFolga.setItems(FXCollections.observableArrayList()); return; }
        try {
            List<Horario> turnos = service.listarTurnosParaCederFolga(utilizadorAtual.getId());
            cbMeuTurnoFolga.setItems(FXCollections.observableArrayList(turnos));
        } catch (Exception e) {
            cbMeuTurnoFolga.setItems(FXCollections.observableArrayList());
            mostrarMensagem("Não foi possível carregar os turnos disponíveis para permuta.", false);
        }
    }

    private void carregarCompensacoes() {
        cbCompensacaoFolga.setValue(null);
        cbCompensacaoFolga.setItems(FXCollections.observableArrayList());
        compensacoesElegiveis = List.of();
        Horario turnoD = cbMeuTurnoFolga.getValue();
        if (utilizadorAtual == null || turnoD == null || turnoD.getId() == null) return;
        compensacoesElegiveis = service.listarTurnosElegiveisCompensacao(utilizadorAtual.getId(), turnoD.getId());
        cbCompensacaoFolga.setItems(FXCollections.observableArrayList(compensacoesElegiveis));
        if (compensacoesElegiveis.size() == 1) cbCompensacaoFolga.getSelectionModel().selectFirst();
    }

    private void carregarHistorico() {
        if (utilizadorAtual == null) { tabelaPermutasFolga.setItems(FXCollections.observableArrayList()); return; }
        List<PermutaFolga> pedidos = service.listarPedidosPorUtilizador(utilizadorAtual.getId());
        tabelaPermutasFolga.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void cancelarPermutaFolga(PermutaFolga pf) {
        try {
            if (utilizadorAtual == null) throw new IllegalArgumentException("Utilizador não identificado.");
            if (pf == null) throw new IllegalArgumentException("Pedido inválido.");
            if (!DialogosHelper.confirmarAcao(janelaSupplier.get(),
                    "Cancelar permuta de folga", "Confirmas o cancelamento deste pedido?",
                    "Turno: " + formatarTurnoFolga(pf.getIdHorarioD()))) return;
            service.cancelar(pf.getId(), utilizadorAtual.getId());
            mostrarMensagem("Pedido de permuta de folga cancelado.", true);
            carregarHistorico();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível cancelar o pedido.", false);
        }
    }

    // ── Cell factory ──────────────────────────────────────────────────────────────

    private TableCell<PermutaFolga, String> criarCelulaEstado() {
        return new TableCell<>() {
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
                setGraphic(badge);
                setText(null);
            }
        };
    }

    private TableCell<PermutaFolga, PermutaFolga> criarCelulaAcao() {
        return new TableCell<>() {
            @Override
            protected void updateItem(PermutaFolga pf, boolean empty) {
                super.updateItem(pf, empty);
                if (empty || pf == null || !"pendente".equalsIgnoreCase(pf.getEstado())) {
                    setGraphic(null);
                    return;
                }
                Button btnCancel = new Button("Cancelar");
                btnCancel.getStyleClass().add("botao-cancelar-pedido-texto");
                btnCancel.setTooltip(new Tooltip("Cancelar este pedido pendente"));
                btnCancel.setOnAction(ev -> cancelarPermutaFolga(getTableView().getItems().get(getIndex())));
                setGraphic(btnCancel);
            }
        };
    }

    // ── Formatters ────────────────────────────────────────────────────────────────

    private String formatarTurnoFolga(Horario h) {
        if (h == null || h.getDataTurno() == null || h.getIdTurno() == null) return "-";
        String ini = h.getIdTurno().getHoraInicio() != null ? h.getIdTurno().getHoraInicio().toString() : "--:--";
        String fim = h.getIdTurno().getHoraFim()    != null ? h.getIdTurno().getHoraFim().toString()    : "--:--";
        return h.getDataTurno().format(DATA_FORMATTER) + " · " + ini + " – " + fim;
    }

    private String formatarTurnoCompensacao(Horario h) {
        if (h == null) return "-";
        String nome = h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdUtilizador() != null
                ? h.getIdLojautilizador().getIdUtilizador().getNome() : "?";
        return nome + " | " + formatarTurnoFolga(h);
    }

    private String formatarDataPedido(PermutaFolga pf) {
        if (pf == null || pf.getDataPedido() == null) return "-";
        return DATA_HORA_FORMATTER.format(pf.getDataPedido().atZone(ZoneId.systemDefault()));
    }

    private String formatarEstado(String estado) {
        if (estado == null) return "-";
        return switch (estado.toLowerCase()) {
            case "pendente"  -> "Pendente";
            case "aprovado"  -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            case "cancelado" -> "Cancelado";
            default -> Character.toUpperCase(estado.charAt(0)) + estado.substring(1).toLowerCase();
        };
    }

    // ── Feedback ──────────────────────────────────────────────────────────────────

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        if (lblMensagemFolga == null) return;
        lblMensagemFolga.setText(mensagem);
        lblMensagemFolga.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagemFolga.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagemFolga.setManaged(true);
        lblMensagemFolga.setVisible(true);
        if (sucesso) {
            javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
            p.setOnFinished(e -> { lblMensagemFolga.setManaged(false); lblMensagemFolga.setVisible(false); });
            p.play();
        }
    }
}
