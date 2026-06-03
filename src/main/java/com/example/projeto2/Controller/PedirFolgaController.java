package com.example.projeto2.Controller;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class PedirFolgaController {

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

    private final DayOffBLL dayOffBLL;

    private Utilizador utilizadorLogado;
    private Map<Integer, String> nomesPendentesPorUtilizador = Map.of();

    public PedirFolgaController(DayOffBLL dayOffBLL) {
        this.dayOffBLL = dayOffBLL;
    }

    @FXML
    public void initialize() {
        cbTipo.setItems(FXCollections.observableArrayList(
                "Férias",
                "Folgas",
                "Baixa"
        ));

        configurarTabelaHistorico();
        configurarTabelaAprovacao();

        tabelaPedidos.setPlaceholder(new Label("Ainda não tens pedidos de folga registados."));
        tabelaPedidosPendentes.setPlaceholder(new Label("Não existem pedidos pendentes para aprovar."));

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
        colDataPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDataAusencia())));

        colTipoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTipo(cellData.getValue().getTipo())));

        colMotivoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarMotivo(cellData.getValue().getMotivo())));

        colEstadoPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));
    }

    private void configurarTabelaAprovacao() {
        colColaboradorPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(nomesPendentesPorUtilizador.getOrDefault(
                        cellData.getValue().getIdUtilizador(),
                        "Utilizador #" + cellData.getValue().getIdUtilizador()
                )));

        colDataPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().getDataAusencia())));

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

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar pedido de folga" : "Rejeitar pedido de folga",
                    aprovar ? "Deseja aprovar este pedido?" : "Deseja rejeitar este pedido?",
                    aprovar
                            ? "A aprovação ficará registada no sistema."
                            : "A rejeição ficará registada no sistema."
            )) {
                return;
            }

            if (aprovar) {
                dayOffBLL.aprovarPedidoFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                mostrarInformacao("Sucesso", "Pedido aprovado com sucesso.");
            } else {
                dayOffBLL.rejeitarPedidoFolga(pedidoSelecionado.getIdDayoff(), utilizadorLogado.getId());
                mostrarInformacao("Sucesso", "Pedido rejeitado com sucesso.");
            }

            carregarPedidosPendentes();
            carregarHistoricoPedidos();
        } catch (IllegalArgumentException e) {
            mostrarErro("Erro", e.getMessage());
        } catch (Exception e) {
            mostrarErro("Erro", "Não foi possível atualizar o estado do pedido.");
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
