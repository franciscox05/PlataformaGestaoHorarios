package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.PermutaFolga;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.PermutaFolgaService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/**
 * Encapsula toda a lógica do painel "Permuta de Folga": configuração de combos
 * e tabelas, carregamento de dados, submissão, aprovação/rejeição e cancelamento.
 */
public final class PermutaFolgaHelper {

    private static final DateTimeFormatter DATA_FORMATTER      = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML nodes ────────────────────────────────────────────────────────────────
    private final ComboBox<Horario> cbMeuTurnoFolga;
    private final ComboBox<Horario> cbCompensacaoFolga;
    private final Button btnSubmeterPermutaFolga;
    private final Label lblMensagemFolga;
    private final TableView<PermutaFolga> tabelaPermutasFolga;
    private final TableColumn<PermutaFolga, String> colPfDataPedido;
    private final TableColumn<PermutaFolga, String> colPfDiaD;
    private final TableColumn<PermutaFolga, String> colPfDiaY;
    private final TableColumn<PermutaFolga, String> colPfEstado;
    private final VBox painelAprovacaoFolga;
    private final TableView<PermutaFolga> tabelaPendentesPermutaFolga;
    private final TableColumn<PermutaFolga, String> colPfPendSolicitante;
    private final TableColumn<PermutaFolga, String> colPfPendDiaD;
    private final TableColumn<PermutaFolga, String> colPfPendDiaY;
    private final Button btnAprovarPermutaFolga;
    private final Button btnRejeitarPermutaFolga;

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
            VBox painelAprovacaoFolga,
            TableView<PermutaFolga> tabelaPendentesPermutaFolga,
            TableColumn<PermutaFolga, String> colPfPendSolicitante,
            TableColumn<PermutaFolga, String> colPfPendDiaD,
            TableColumn<PermutaFolga, String> colPfPendDiaY,
            Button btnAprovarPermutaFolga,
            Button btnRejeitarPermutaFolga,
            PermutaFolgaService service,
            Supplier<Window> janelaSupplier) {
        this.cbMeuTurnoFolga            = cbMeuTurnoFolga;
        this.cbCompensacaoFolga         = cbCompensacaoFolga;
        this.btnSubmeterPermutaFolga    = btnSubmeterPermutaFolga;
        this.lblMensagemFolga           = lblMensagemFolga;
        this.tabelaPermutasFolga        = tabelaPermutasFolga;
        this.colPfDataPedido            = colPfDataPedido;
        this.colPfDiaD                  = colPfDiaD;
        this.colPfDiaY                  = colPfDiaY;
        this.colPfEstado                = colPfEstado;
        this.painelAprovacaoFolga       = painelAprovacaoFolga;
        this.tabelaPendentesPermutaFolga = tabelaPendentesPermutaFolga;
        this.colPfPendSolicitante       = colPfPendSolicitante;
        this.colPfPendDiaD              = colPfPendDiaD;
        this.colPfPendDiaY              = colPfPendDiaY;
        this.btnAprovarPermutaFolga     = btnAprovarPermutaFolga;
        this.btnRejeitarPermutaFolga    = btnRejeitarPermutaFolga;
        this.service                    = service;
        this.janelaSupplier             = janelaSupplier;
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

        colPfPendSolicitante.setCellValueFactory(c -> new SimpleStringProperty(obterNomeSolicitante(c.getValue())));
        colPfPendDiaD.setCellValueFactory(c -> new SimpleStringProperty(formatarTurnoFolga(c.getValue().getIdHorarioD())));
        colPfPendDiaY.setCellValueFactory(c -> new SimpleStringProperty(formatarTurnoCompensacao(c.getValue().getIdHorarioY())));

        btnSubmeterPermutaFolga.disableProperty().bind(
                cbMeuTurnoFolga.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbCompensacaoFolga.getSelectionModel().selectedItemProperty().isNull()));
        btnAprovarPermutaFolga.disableProperty().bind(
                Bindings.isNull(tabelaPendentesPermutaFolga.getSelectionModel().selectedItemProperty()));
        btnRejeitarPermutaFolga.disableProperty().bind(
                Bindings.isNull(tabelaPendentesPermutaFolga.getSelectionModel().selectedItemProperty()));

        tabelaPermutasFolga.setPlaceholder(new Label("Nenhuma permuta de folga encontrada."));
        tabelaPendentesPermutaFolga.setPlaceholder(new Label("Não há permutas de folga pendentes para aprovar."));

        if (lblMensagemFolga != null)     { lblMensagemFolga.setManaged(false);     lblMensagemFolga.setVisible(false); }
        if (painelAprovacaoFolga != null) { painelAprovacaoFolga.setManaged(false); painelAprovacaoFolga.setVisible(false); }
    }

    public void carregarDados(Utilizador utilizador) {
        this.utilizadorAtual = utilizador;
        carregarMeusTurnos();
        carregarHistorico();
        configurarPainelAprovacao();
    }

    // ── Public @FXML delegates ────────────────────────────────────────────────────

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
            carregarPendentes();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível submeter o pedido de permuta de folga.", false);
        }
    }

    public void onAprovar()  { tratarPedidoSelecionado(true); }
    public void onRejeitar() { tratarPedidoSelecionado(false); }

    // ── Data loading ──────────────────────────────────────────────────────────────

    private void carregarMeusTurnos() {
        if (utilizadorAtual == null) { cbMeuTurnoFolga.setItems(FXCollections.observableArrayList()); return; }
        List<Horario> turnos = service.listarTurnosParaCederFolga(utilizadorAtual.getId());
        cbMeuTurnoFolga.setItems(FXCollections.observableArrayList(turnos));
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

    private void configurarPainelAprovacao() {
        boolean podeAprovar = utilizadorAtual != null && service.podeAprovar(utilizadorAtual.getId());
        painelAprovacaoFolga.setManaged(podeAprovar);
        painelAprovacaoFolga.setVisible(podeAprovar);
        if (podeAprovar) carregarPendentes();
        else tabelaPendentesPermutaFolga.setItems(FXCollections.observableArrayList());
    }

    private void carregarPendentes() {
        if (utilizadorAtual == null || !service.podeAprovar(utilizadorAtual.getId())) {
            tabelaPendentesPermutaFolga.setItems(FXCollections.observableArrayList());
            return;
        }
        List<PermutaFolga> pendentes = service.listarPendentesParaAprovacao(utilizadorAtual.getId());
        tabelaPendentesPermutaFolga.setItems(FXCollections.observableArrayList(pendentes));
    }

    // ── Decision handlers ─────────────────────────────────────────────────────────

    private void tratarPedidoSelecionado(boolean aprovar) {
        try {
            if (utilizadorAtual == null) throw new IllegalArgumentException("Utilizador não identificado.");
            PermutaFolga pf = tabelaPendentesPermutaFolga.getSelectionModel().getSelectedItem();
            if (pf == null) throw new IllegalArgumentException("Seleciona um pedido pendente primeiro.");
            String detalhe = String.format(
                    "Solicitante: %s%nTurno cedido: %s%nCompensação:  %s",
                    obterNomeSolicitante(pf), formatarTurnoFolga(pf.getIdHorarioD()), formatarTurnoCompensacao(pf.getIdHorarioY()));
            if (!DialogosHelper.confirmarAcao(janelaSupplier.get(),
                    aprovar ? "Aprovar permuta de folga" : "Rejeitar permuta de folga",
                    aprovar ? "Confirmas a aprovação deste pedido?" : "Confirmas a rejeição deste pedido?",
                    detalhe)) return;
            if (aprovar) {
                service.aprovar(pf.getId(), utilizadorAtual.getId());
                mostrarMensagem("Permuta de folga aprovada. Os horários foram atualizados.", true);
            } else {
                service.rejeitar(pf.getId(), utilizadorAtual.getId());
                mostrarMensagem("Permuta de folga rejeitada.", true);
            }
            carregarPendentes();
            carregarHistorico();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível atualizar o pedido de permuta de folga.", false);
        }
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
                if ("pendente".equalsIgnoreCase(estado)) {
                    Button btnCancel = new Button("✕");
                    btnCancel.getStyleClass().add("botao-cancelar-pedido");
                    btnCancel.setTooltip(new Tooltip("Cancelar este pedido pendente"));
                    btnCancel.setOnAction(ev -> {
                        PermutaFolga pf = getTableView().getItems().get(getIndex());
                        cancelarPermutaFolga(pf);
                    });
                    HBox cell = new HBox(6, badge, btnCancel);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                } else {
                    setGraphic(badge);
                }
                setText(null);
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

    private String obterNomeSolicitante(PermutaFolga pf) {
        if (pf == null || pf.getIdHorarioD() == null
                || pf.getIdHorarioD().getIdLojautilizador() == null
                || pf.getIdHorarioD().getIdLojautilizador().getIdUtilizador() == null) return "-";
        return pf.getIdHorarioD().getIdLojautilizador().getIdUtilizador().getNome();
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
