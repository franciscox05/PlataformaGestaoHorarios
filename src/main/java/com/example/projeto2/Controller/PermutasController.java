package com.example.projeto2.Controller;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
public class PermutasController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ComboBox<Horario> cbMeuTurno;

    @FXML
    private ComboBox<Horario> cbTurnoColega;

    @FXML
    private Label lblMensagem;

    @FXML
    private TableView<Permuta> tabelaPedidosPermuta;

    @FXML
    private TableColumn<Permuta, String> colDataPedido;

    @FXML
    private TableColumn<Permuta, String> colTurnoOrigem;

    @FXML
    private TableColumn<Permuta, String> colTurnoDestino;

    @FXML
    private TableColumn<Permuta, String> colEstadoPermuta;

    private final HorarioBLL horarioBll;
    private final PermutaBLL permutaBll;
    private Utilizador utilizadorLogado;

    public PermutasController(HorarioBLL horarioBll, PermutaBLL permutaBll) {
        this.horarioBll = horarioBll;
        this.permutaBll = permutaBll;
    }

    @FXML
    public void initialize() {
        configurarCombos();
        configurarTabelaHistorico();

        lblMensagem.setManaged(false);
        lblMensagem.setVisible(false);
        tabelaPedidosPermuta.setPlaceholder(new Label("Ainda nao submeteste pedidos de permuta."));
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        carregarMeusTurnos();
        carregarHistorico();
    }

    @FXML
    public void onSubmeterTrocaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            permutaBll.registarPedidoTroca(
                    utilizadorLogado.getId(),
                    cbMeuTurno.getValue(),
                    cbTurnoColega.getValue()
            );

            mostrarMensagem("Pedido de permuta submetido com sucesso.", true);
            limparFormulario();
            carregarHistorico();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Nao foi possivel submeter o pedido de permuta.", false);
        }
    }

    private void configurarCombos() {
        cbMeuTurno.setConverter(new StringConverter<>() {
            @Override
            public String toString(Horario horario) {
                return formatarTurnoProprio(horario);
            }

            @Override
            public Horario fromString(String string) {
                return null;
            }
        });

        cbTurnoColega.setConverter(new StringConverter<>() {
            @Override
            public String toString(Horario horario) {
                return formatarTurnoColega(horario);
            }

            @Override
            public Horario fromString(String string) {
                return null;
            }
        });

        cbMeuTurno.setOnAction(event -> carregarTurnosElegiveis());
    }

    private void configurarTabelaHistorico() {
        colDataPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataPedido(cellData.getValue())));

        colTurnoOrigem.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoProprio(cellData.getValue().getIdHorarioOrigem())));

        colTurnoDestino.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoColega(cellData.getValue().getIdHorarioDestino())));

        colEstadoPermuta.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));
    }

    private void carregarMeusTurnos() {
        if (utilizadorLogado == null) {
            cbMeuTurno.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Horario> meusTurnos = horarioBll.listarMeusTurnosDisponiveisParaPermuta(utilizadorLogado.getId());
        cbMeuTurno.setItems(FXCollections.observableArrayList(meusTurnos));
    }

    private void carregarTurnosElegiveis() {
        cbTurnoColega.setValue(null);

        Horario meuTurno = cbMeuTurno.getValue();
        if (utilizadorLogado == null || meuTurno == null || meuTurno.getId() == null) {
            cbTurnoColega.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Horario> turnosElegiveis = horarioBll.listarTurnosElegiveisParaPermuta(
                utilizadorLogado.getId(),
                meuTurno.getId()
        );
        cbTurnoColega.setItems(FXCollections.observableArrayList(turnosElegiveis));
    }

    private void carregarHistorico() {
        if (utilizadorLogado == null) {
            tabelaPedidosPermuta.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Permuta> pedidos = permutaBll.listarPedidosEnviados(utilizadorLogado.getId());
        tabelaPedidosPermuta.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void limparFormulario() {
        cbMeuTurno.setValue(null);
        cbTurnoColega.setValue(null);
        cbTurnoColega.setItems(FXCollections.observableArrayList());
    }

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        lblMensagem.setText(mensagem);
        lblMensagem.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagem.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagem.setManaged(true);
        lblMensagem.setVisible(true);
    }

    private String formatarTurnoProprio(Horario horario) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null) {
            return "";
        }

        return horario.getDataTurno().format(DATA_FORMATTER)
                + " | "
                + horario.getIdTurno().getHoraInicio()
                + " - "
                + horario.getIdTurno().getHoraFim();
    }

    private String formatarTurnoColega(Horario horario) {
        if (horario == null || horario.getIdLojautilizador() == null || horario.getIdLojautilizador().getIdUtilizador() == null) {
            return "";
        }

        return horario.getIdLojautilizador().getIdUtilizador().getNome()
                + " | "
                + formatarTurnoProprio(horario);
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }

        return Character.toUpperCase(estado.charAt(0)) + estado.substring(1).toLowerCase();
    }

    private String formatarDataPedido(Permuta permuta) {
        if (permuta == null || permuta.getDataPedido() == null) {
            return "-";
        }

        return DATA_HORA_FORMATTER.format(permuta.getDataPedido().atZone(ZoneId.systemDefault()));
    }
}
