package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PedirFolgaController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private DatePicker dpData;

    @FXML
    private ComboBox<String> cbTipo;

    @FXML
    private TextArea txtMotivo;

    @FXML
    private TableView<DayOff> tabelaPedidos;

    @FXML
    private TableColumn<DayOff, String> colDataPedido;

    @FXML
    private TableColumn<DayOff, String> colTipoPedido;

    @FXML
    private TableColumn<DayOff, String> colMotivoPedido;

    @FXML
    private TableColumn<DayOff, String> colEstadoPedido;

    @FXML
    private VBox painelAprovacao;

    @FXML
    private TableView<DayOff> tabelaPedidosPendentes;

    @FXML
    private TableColumn<DayOff, String> colColaboradorPendente;

    @FXML
    private TableColumn<DayOff, String> colDataPendente;

    @FXML
    private TableColumn<DayOff, String> colTipoPendente;

    @FXML
    private TableColumn<DayOff, String> colMotivoPendente;

    @FXML
    private Button btnAprovarPedido;

    @FXML
    private Button btnRejeitarPedido;

    private final DayOffService dayOffBLL;

    private Utilizador utilizadorLogado;
    private Map<Integer, String> nomesPendentesPorUtilizador = Map.of();

    public PedirFolgaController(DayOffService dayOffBLL) {
        this.dayOffBLL = dayOffBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList(
                "Férias",
                "Folgas",
                "Baixa",
                "Urgente / Emergência"
        ));

        configurarTabelaHistorico();
        configurarTabelaAprovacao();

        // Empty state com CTA para a tabela histórico
        javafx.scene.layout.VBox emptyFolgas = new javafx.scene.layout.VBox(12);
        emptyFolgas.setAlignment(javafx.geometry.Pos.CENTER);
        emptyFolgas.setPadding(new javafx.geometry.Insets(40, 24, 40, 24));
        Label emptyFolgasTitulo = new Label("Nenhum pedido ainda");
        emptyFolgasTitulo.getStyleClass().add("empty-state-titulo");
        Label emptyFolgasSubtitulo = new Label("Os teus pedidos de ausência aparecem aqui depois de os submeteres.");
        emptyFolgasSubtitulo.getStyleClass().add("empty-state-subtitulo");
        emptyFolgasSubtitulo.setWrapText(true);
        emptyFolgasSubtitulo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        Button btnEmptyFolga = new Button("Fazer o meu primeiro pedido");
        btnEmptyFolga.getStyleClass().add("botao-acao");
        btnEmptyFolga.setOnAction(e -> dpData.requestFocus());
        emptyFolgas.getChildren().addAll(emptyFolgasTitulo, emptyFolgasSubtitulo, btnEmptyFolga);
        tabelaPedidos.setPlaceholder(emptyFolgas);

        tabelaPedidosPendentes.setPlaceholder(new Label("Não existem pedidos pendentes para aprovar."));

        // Tooltips nos campos do formulário
        dpData.setTooltip(new Tooltip("Seleciona a data em que pretendes ausentar-te"));
        cbTipo.setTooltip(new Tooltip("Seleciona o tipo de ausência: férias, folga, baixa ou emergência urgente"));
        txtMotivo.setTooltip(new Tooltip("Opcional — descreve brevemente o motivo da ausência"));

        btnAprovarPedido.disableProperty().bind(Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPedido.disableProperty().bind(Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));

        painelAprovacao.setManaged(false);
        painelAprovacao.setVisible(false);
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarHistoricoPedidos();
        configurarPainelAprovacao();
    }

    @FXML
    public void onSubmitClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            // Validar campos obrigatórios antes de qualquer diálogo
            if (dpData.getValue() == null) {
                throw new IllegalArgumentException("Seleciona uma data para o pedido de folga.");
            }
            if (cbTipo.getValue() == null) {
                throw new IllegalArgumentException("Seleciona o tipo de ausência.");
            }

            // Verificar pedido duplicado (pendente ou aprovado para a mesma data)
            java.time.LocalDate dataAusencia = dpData.getValue();
            List<DayOff> pedidosExistentes = dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId());
            boolean jaTem = pedidosExistentes.stream().anyMatch(d ->
                    dataAusencia.equals(d.getDataAusencia())
                    && ("pendente".equalsIgnoreCase(d.getEstado()) || "aprovado".equalsIgnoreCase(d.getEstado()))
            );
            if (jaTem) {
                mostrarErro("Pedido duplicado",
                        "Já tens uma folga pendente ou aprovada para "
                        + dataAusencia.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        + ". Cancela o pedido anterior antes de submeter um novo.");
                return;
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Registar pedido de folga",
                    "Deseja submeter este pedido?",
                    "O pedido ficará pendente para análise."
            )) {
                return;
            }

            String tipoSelecionado = mapearTipoParaBaseDados(cbTipo.getValue());

            DayOff pedido = new DayOff();
            pedido.setIdUtilizador(utilizadorLogado);
            pedido.setDataAusencia(dpData.getValue());
            pedido.setTipo(tipoSelecionado);
            pedido.setMotivo(txtMotivo.getText());

            dayOffBLL.registarPedidoFolga(pedido);

            mostrarInformacao("Sucesso", "Pedido de folga registado com sucesso.");
            limparFormulario();
            carregarHistoricoPedidos();
            carregarPedidosPendentes();
        } catch (IllegalArgumentException e) {
            mostrarErro("Erro", e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro", "Não foi possível guardar o pedido de folga.");
        }
    }

    @FXML
    public void onAprovarPedidoClick() {
        tratarPedidoSelecionado(true);
    }

    @FXML
    public void onRejeitarPedidoClick() {
        tratarPedidoSelecionado(false);
    }

    private void configurarTabelaHistorico() {
        colDataPedido.setCellValueFactory(cellData -> {
            java.time.LocalDate data = cellData.getValue().getDataAusencia();
            return new SimpleStringProperty(data != null ? data.format(DATA_FORMATTER) : "-");
        });

        colTipoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colMotivoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMotivo(cellData.getValue().getMotivo())));

        colEstadoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        // Badge colorido + botão cancelar para pedidos pendentes
        colEstadoPedido.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(estado);
                badge.getStyleClass().add("badge-estado");
                switch (estado.toLowerCase()) {
                    case "pendente" -> badge.getStyleClass().add("badge-pendente");
                    case "aprovado" -> badge.getStyleClass().add("badge-aprovado");
                    case "rejeitado" -> badge.getStyleClass().add("badge-rejeitado");
                    default -> badge.getStyleClass().add("badge-rascunho");
                }

                if ("pendente".equalsIgnoreCase(estado)) {
                    Button btnCancelar = new Button("✕");
                    btnCancelar.getStyleClass().add("botao-cancelar-pedido");
                    btnCancelar.setTooltip(new Tooltip("Cancelar este pedido pendente"));
                    btnCancelar.setOnAction(ev -> {
                        DayOff pedido = getTableView().getItems().get(getIndex());
                        cancelarPedidoProprio(pedido);
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

    private void configurarTabelaAprovacao() {
        colColaboradorPendente.setCellValueFactory(cellData -> {
            var utilizadorPendente = cellData.getValue().getIdUtilizador();
            Integer idColaborador = (utilizadorPendente != null) ? utilizadorPendente.getId() : null;
            return new SimpleStringProperty(nomesPendentesPorUtilizador.getOrDefault(
                    idColaborador,
                    idColaborador != null ? "Colaborador #" + idColaborador : "Colaborador"));
        });

        colDataPendente.setCellValueFactory(cellData -> {
            java.time.LocalDate data = cellData.getValue().getDataAusencia();
            return new SimpleStringProperty(data != null ? data.format(DATA_FORMATTER) : "-");
        });

        colTipoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colMotivoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMotivo(cellData.getValue().getMotivo())));
    }

    private void carregarHistoricoPedidos() {
        if (utilizadorLogado == null) {
            tabelaPedidos.setItems(FXCollections.observableArrayList());
            return;
        }

        List<DayOff> pedidos = dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId());
        tabelaPedidos.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void configurarPainelAprovacao() {
        boolean podeAprovar = utilizadorLogado != null && dayOffBLL.utilizadorPodeAprovarFolgas(utilizadorLogado.getId());
        painelAprovacao.setManaged(podeAprovar);
        painelAprovacao.setVisible(podeAprovar);

        if (podeAprovar) {
            carregarPedidosPendentes();
        } else {
            tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
        }
    }

    private void carregarPedidosPendentes() {
        if (utilizadorLogado == null || !dayOffBLL.utilizadorPodeAprovarFolgas(utilizadorLogado.getId())) {
            tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
            nomesPendentesPorUtilizador = Map.of();
            return;
        }

        List<DayOff> pedidosPendentes = dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId());
        nomesPendentesPorUtilizador = dayOffBLL.listarNomesUtilizadores(
                pedidosPendentes.stream().map(d -> d.getIdUtilizador().getId()).collect(Collectors.toSet())
        );
        tabelaPedidosPendentes.setItems(FXCollections.observableArrayList(pedidosPendentes));
        tabelaPedidosPendentes.refresh();
    }

    private void tratarPedidoSelecionado(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            DayOff pedidoSelecionado = tabelaPedidosPendentes.getSelectionModel().getSelectedItem();
            if (pedidoSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um pedido pendente primeiro.");
            }

            String nomeColaboradorFolga = nomesPendentesPorUtilizador.getOrDefault(
                    pedidoSelecionado.getIdUtilizador() != null ? pedidoSelecionado.getIdUtilizador().getId() : null,
                    "Colaborador");
            String dataFolga = pedidoSelecionado.getDataAusencia() != null
                    ? pedidoSelecionado.getDataAusencia().format(DATA_FORMATTER) : "-";
            String motivoFolga = pedidoSelecionado.getMotivo() != null && !pedidoSelecionado.getMotivo().isBlank()
                    ? "\nMotivo: " + pedidoSelecionado.getMotivo() : "";
            String detalhesFolga = "Colaborador: " + nomeColaboradorFolga + "\nData: " + dataFolga + motivoFolga;

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar pedido de folga" : "Rejeitar pedido de folga",
                    aprovar ? "Confirmas a aprovação desta ausência?" : "Confirmas a rejeição desta ausência?",
                    detalhesFolga
            )) {
                return;
            }

            if (aprovar) {
                boolean urgente = "urgente".equalsIgnoreCase(pedidoSelecionado.getTipo());
                if (urgente) {
                    com.example.projeto2.API.Services.DayOffService.ResultadoAprovacaoFolga resultado =
                            dayOffBLL.aprovarPedidoFolgaComCobertura(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                    mostrarInformacao("Ausência aprovada", resultado.avisoCobertura());
                } else {
                    dayOffBLL.aprovarPedidoFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                    mostrarInformacao("Sucesso", "Pedido de folga aprovado.");
                }
            } else {
                dayOffBLL.rejeitarPedidoFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                mostrarInformacao("Sucesso", "Pedido de folga rejeitado.");
            }

            carregarPedidosPendentes();
            carregarHistoricoPedidos();
        } catch (IllegalArgumentException e) {
            mostrarErro("Erro", e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro", "Não foi possível atualizar o estado do pedido.");
        }
    }

    private void cancelarPedidoProprio(DayOff pedido) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            if (pedido == null) {
                throw new IllegalArgumentException("Pedido inválido.");
            }

            String dataFolga = pedido.getDataAusencia() != null ? pedido.getDataAusencia().format(DATA_FORMATTER) : "-";
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Cancelar pedido de folga",
                    "Confirmas o cancelamento deste pedido?",
                    "Data: " + dataFolga + "\nTipo: " + formatarTipo(pedido.getTipo())
            )) {
                return;
            }

            dayOffBLL.cancelarPedidoProprio(pedido.getIdDayoff(), utilizadorLogado.getId());
            mostrarInformacao("Pedido cancelado", "O pedido de folga foi cancelado.");
            carregarHistoricoPedidos();
        } catch (IllegalArgumentException e) {
            mostrarErro("Erro", e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro", "Não foi possível cancelar o pedido de folga.");
        }
    }

    private String mapearTipoParaBaseDados(String tipoSelecionado) {
        if (tipoSelecionado == null || tipoSelecionado.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de ausência.");
        }

        return switch (tipoSelecionado) {
            case "Férias" -> "ferias";
            case "Folgas" -> "folgas";
            case "Baixa" -> "baixa";
            case "Urgente / Emergência" -> "urgente";
            default -> throw new IllegalArgumentException("Tipo de ausência inválido.");
        };
    }

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "-";
        }

        return switch (tipo.toLowerCase()) {
            case "ferias" -> "Férias";
            case "folgas" -> "Folgas";
            case "baixa" -> "Baixa";
            case "urgente" -> "⚡ Urgente";
            default -> tipo;
        };
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }

        return switch (estado.toLowerCase()) {
            case "pendente" -> "Pendente";
            case "aprovado" -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            case "cancelado" -> "Cancelado";
            default -> estado;
        };
    }

    private String formatarMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            return "-";
        }
        return motivo;
    }

    private void limparFormulario() {
        dpData.setValue(null);
        cbTipo.setValue(null);
        txtMotivo.clear();
    }

    private void mostrarInformacao(String cabecalho, String mensagem) {
        DialogosHelper.mostrarInformacao(obterJanela(), "Informação", cabecalho, mensagem);
    }

    private void mostrarErro(String cabecalho, String mensagem) {
        DialogosHelper.mostrarErro(obterJanela(), "Erro", cabecalho, mensagem);
    }

    private Window obterJanela() {
        if (dpData == null || dpData.getScene() == null) {
            return null;
        }
        return dpData.getScene().getWindow();
    }
}
