package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Utilizador;
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
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class PedirFolgaController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private DatePicker dpData;
    @FXML private DatePicker dpDataFim;
    @FXML private javafx.scene.layout.VBox painelDataFim;
    @FXML private Label lblData;
    @FXML private Label lblMotivo;
    @FXML private ComboBox<String> cbTipo;
    @FXML private TextArea txtMotivo;

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

    private final DayOffService dayOffBLL;

    private Utilizador utilizadorLogado;

    public PedirFolgaController(DayOffService dayOffBLL) {
        this.dayOffBLL = dayOffBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList("Férias", "Folgas", "Baixa"));

        cbTipo.valueProperty().addListener((obs, old, novo) -> configurarCamposParaTipo(novo));

        configurarTabelaHistorico();

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

        dpData.setTooltip(new Tooltip("Seleciona a data em que pretendes ausentar-te"));
        cbTipo.setTooltip(new Tooltip("Férias: intervalo de datas | Folgas: dia isolado | Baixa: só hoje ou amanhã, motivo obrigatório"));
        txtMotivo.setTooltip(new Tooltip("Obrigatório para Baixa Médica; opcional para os restantes tipos"));
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarHistoricoPedidos();
    }

    @FXML
    public void onSubmitClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            if (cbTipo.getValue() == null) {
                throw new IllegalArgumentException("Seleciona o tipo de ausência.");
            }
            if (dpData.getValue() == null) {
                throw new IllegalArgumentException("Seleciona uma data para o pedido.");
            }

            String tipoSelecionado = mapearTipoParaBaseDados(cbTipo.getValue());
            String motivo = txtMotivo.getText();

            if ("ferias".equals(tipoSelecionado)) {
                submeterFerias(tipoSelecionado, motivo);
            } else {
                submeterFolgaSimples(tipoSelecionado, motivo);
            }
        } catch (IllegalArgumentException e) {
            mostrarErro("Erro", e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro", "Não foi possível guardar o pedido.");
        }
    }

    private void submeterFerias(String tipo, String motivo) {
        java.time.LocalDate inicio = dpData.getValue();
        java.time.LocalDate fim = dpDataFim.getValue();
        if (fim == null) {
            mostrarErro("Erro", "Seleciona a data de fim das férias.");
            return;
        }
        if (fim.isBefore(inicio)) {
            mostrarErro("Erro", "A data de fim não pode ser anterior à data de início.");
            return;
        }
        if (!DialogosHelper.confirmarAcao(obterJanela(),
                "Registar pedido de férias",
                "Submeter férias de " + inicio.format(DATA_FORMATTER) + " a " + fim.format(DATA_FORMATTER) + "?",
                "O pedido ficará pendente para aprovação do gestor.")) {
            return;
        }
        dayOffBLL.registarPedidoFeriasIntervalo(utilizadorLogado, inicio, fim, motivo);
        mostrarInformacao("Sucesso", "Pedido de férias registado com sucesso.");
        limparFormulario();
        carregarHistoricoPedidos();
    }

    private void submeterFolgaSimples(String tipo, String motivo) {
        java.time.LocalDate data = dpData.getValue();
        List<DayOff> pedidosExistentes = dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId());
        boolean jaTem = pedidosExistentes.stream().anyMatch(d ->
                data.equals(d.getDataAusencia())
                && ("pendente".equalsIgnoreCase(d.getEstado()) || "aprovado".equalsIgnoreCase(d.getEstado())));
        if (jaTem) {
            mostrarErro("Pedido duplicado",
                    "Já tens uma folga pendente ou aprovada para "
                    + data.format(DATA_FORMATTER) + ". Cancela o pedido anterior antes de submeter um novo.");
            return;
        }
        if (!DialogosHelper.confirmarAcao(obterJanela(),
                "Registar pedido de folga",
                "Deseja submeter este pedido?",
                "O pedido ficará pendente para análise.")) {
            return;
        }
        DayOff pedido = new DayOff();
        pedido.setIdUtilizador(utilizadorLogado);
        pedido.setDataAusencia(data);
        pedido.setTipo(tipo);
        pedido.setMotivo(motivo);
        dayOffBLL.registarPedidoFolga(pedido);
        mostrarInformacao("Sucesso", "Pedido de folga registado com sucesso.");
        limparFormulario();
        carregarHistoricoPedidos();
    }

    private void configurarCamposParaTipo(String tipo) {
        boolean ferias = "Férias".equals(tipo);
        boolean baixa  = "Baixa".equals(tipo);
        painelDataFim.setManaged(ferias);
        painelDataFim.setVisible(ferias);
        if (lblData != null) {
            lblData.setText(ferias ? "Data de início das férias" : "Data da ausência");
        }
        if (lblMotivo != null) {
            lblMotivo.setText(baixa ? "Justificação (obrigatória para baixa médica)" : "Justificação (opcional)");
        }
        if (!ferias && dpDataFim != null) dpDataFim.setValue(null);
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

    private void carregarHistoricoPedidos() {
        if (utilizadorLogado == null) {
            tabelaPedidos.setItems(FXCollections.observableArrayList());
            return;
        }

        List<DayOff> pedidos = dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId());
        tabelaPedidos.setItems(FXCollections.observableArrayList(pedidos));
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
        if (dpDataFim != null) dpDataFim.setValue(null);
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
