package com.example.projeto2.Controller;

import com.example.projeto2.BLL.PainelGerenteBLL;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    private final PainelGerenteBLL painelGerenteBLL;
    private Utilizador utilizadorLogado;
    private Map<Integer, String> nomesFolgasPendentes = Map.of();

    public PainelGerentePedidosController(PainelGerenteBLL painelGerenteBLL) {
        this.painelGerenteBLL = painelGerenteBLL;
    }

    @FXML
    public void initialize() {
        configurarTabelaFolgas();
        configurarTabelaPermutas();
        configurarTabelaPreferencias();

        esconderFeedback(lblFeedbackFolgas);
        esconderFeedback(lblFeedbackPermutas);
        esconderFeedback(lblFeedbackPreferencias);

        tabelaFolgasPendentes.setPlaceholder(new Label("Nao existem pedidos de folga pendentes nesta loja."));
        tabelaPermutasPendentes.setPlaceholder(new Label("Nao existem pedidos de permuta pendentes nesta loja."));
        tabelaPreferenciasPendentes.setPlaceholder(new Label("Nao existem preferencias pendentes nesta loja."));

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
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarPainel();
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
                new SimpleStringProperty(String.valueOf(cellData.getValue().getPrioridade())));

        colPreferenciaDescricao.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTexto(cellData.getValue().getDescricao())));
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
            carregarPainel();
        } catch (IllegalArgumentException e) {
            mostrarFeedback(lblFeedbackPreferencias, e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedback(lblFeedbackPreferencias, "Nao foi possivel atualizar a preferencia.", false);
        }
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

        return formatarData(dataInicio);
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
}
