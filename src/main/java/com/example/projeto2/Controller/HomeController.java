package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class HomeController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    private Label lblBemVindo;

    @FXML
    private TableView<Horario> tabelaTurnos;

    @FXML
    private TableColumn<Horario, String> colData;

    @FXML
    private TableColumn<Horario, String> colHorario;

    @FXML
    private TableColumn<Horario, String> colLoja;

    @FXML
    private TableColumn<Horario, String> colEstado;

    @FXML
    private TableView<Horario> tabelaEquipaHoje;

    @FXML
    private TableColumn<Horario, String> colColaboradorHoje;

    @FXML
    private TableColumn<Horario, String> colHorarioHoje;

    @FXML
    private TableColumn<Horario, String> colEstadoHoje;

    @FXML
    private VBox painelOperacaoLoja;

    @FXML
    private DatePicker dpDataOperacao;

    @FXML
    private ComboBox<String> cbIntervaloOperacao;

    @FXML
    private Label lblResumoOperacao;

    @FXML
    private Label lblOperacaoColaboradores;

    @FXML
    private Label lblOperacaoTurnos;

    @FXML
    private Label lblOperacaoAusencias;

    @FXML
    private Label lblOperacaoPedidos;

    @FXML
    private Label lblFeedbackOperacao;

    @FXML
    private TableView<SnapshotOperacionalLojaBLL.ColaboradorEscala> tabelaOperacaoLoja;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colOperacaoColaborador;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colOperacaoCargo;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colOperacaoTurnos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaBLL.ColaboradorEscala, String> colOperacaoEstado;

    private final HorarioBLL horarioBll;
    private final SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;
    private final GestaoLojaBLL gestaoLojaBLL;
    private Utilizador utilizadorLogado;

    public HomeController(HorarioBLL horarioBll,
                          SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL,
                          GestaoLojaBLL gestaoLojaBLL) {
        this.horarioBll = horarioBll;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    @FXML
    public void initialize() {
        configurarTabelaTurnos();
        configurarTabelaEquipaHoje();
        configurarTabelaOperacaoLoja();
        configurarPainelOperacao();
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        lblBemVindo.setText("Bem-vindo(a), " + utilizador.getNome() + "!");

        carregarTurnos();
        carregarEquipaHoje();
        configurarVisibilidadePainelOperacao();
    }

    private void configurarTabelaTurnos() {
        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarData(cellData.getValue().getDataTurno())));

        colHorario.setCellValueFactory(cellData -> {
            Horario horario = cellData.getValue();
            return new SimpleStringProperty(horario.getIdTurno().getHoraInicio() + " - " + horario.getIdTurno().getHoraFim());
        });

        colLoja.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdLoja().getNome()));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        tabelaTurnos.setPlaceholder(new Label("Ainda nao tens turnos futuros atribuidos."));
    }

    private void configurarTabelaEquipaHoje() {
        colColaboradorHoje.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdUtilizador().getNome()));

        colHorarioHoje.setCellValueFactory(cellData -> {
            Horario horario = cellData.getValue();
            return new SimpleStringProperty(horario.getIdTurno().getHoraInicio() + " - " + horario.getIdTurno().getHoraFim());
        });

        colEstadoHoje.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        tabelaEquipaHoje.setPlaceholder(new Label("Nao ha equipa escalada para hoje na tua loja."));
    }

    private void configurarTabelaOperacaoLoja() {
        colOperacaoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().nome()));

        colOperacaoCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().cargo()));

        colOperacaoTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnosOperacao(cellData.getValue().turnos())));

        colOperacaoEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstadosOperacao(cellData.getValue().turnos())));

        tabelaOperacaoLoja.setPlaceholder(new Label("Nao existe equipa escalada para o periodo selecionado."));
    }

    private void configurarPainelOperacao() {
        cbIntervaloOperacao.setItems(FXCollections.observableArrayList("Dia", "3 dias", "Semana"));
        cbIntervaloOperacao.setValue("Dia");
        dpDataOperacao.setValue(LocalDate.now());

        painelOperacaoLoja.setManaged(false);
        painelOperacaoLoja.setVisible(false);
        esconderFeedbackOperacao();

        dpDataOperacao.valueProperty().addListener((obs, antiga, nova) -> {
            esconderFeedbackOperacao();
            if (painelOperacaoLoja.isVisible()) {
                carregarOperacaoLoja();
            }
        });

        cbIntervaloOperacao.valueProperty().addListener((obs, antigo, novo) -> {
            esconderFeedbackOperacao();
            if (painelOperacaoLoja.isVisible()) {
                carregarOperacaoLoja();
            }
        });
    }

    private void carregarTurnos() {
        if (utilizadorLogado != null) {
            List<Horario> turnos = horarioBll.listarProximosTurnos(utilizadorLogado.getId());
            tabelaTurnos.setItems(FXCollections.observableArrayList(turnos));
        }
    }

    private void carregarEquipaHoje() {
        if (utilizadorLogado != null) {
            List<Horario> equipaHoje = horarioBll.listarEquipaDeHoje(utilizadorLogado.getId());
            tabelaEquipaHoje.setItems(FXCollections.observableArrayList(equipaHoje));
        }
    }

    private void configurarVisibilidadePainelOperacao() {
        boolean podeGerirLoja = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());

        painelOperacaoLoja.setManaged(podeGerirLoja);
        painelOperacaoLoja.setVisible(podeGerirLoja);

        if (podeGerirLoja) {
            carregarOperacaoLoja();
        } else {
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
        }
    }

    private void carregarOperacaoLoja() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            LocalDate dataInicio = dpDataOperacao.getValue() != null ? dpDataOperacao.getValue() : LocalDate.now();
            LocalDate dataFim = resolverDataFimOperacao(dataInicio);

            SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot = snapshotOperacionalLojaBLL.carregarSnapshot(
                    utilizadorLogado.getId(),
                    dataInicio,
                    dataFim
            );

            lblOperacaoColaboradores.setText(String.valueOf(snapshot.resumo().colaboradoresEscalados()));
            lblOperacaoTurnos.setText(String.valueOf(snapshot.resumo().turnosPlaneados()));
            lblOperacaoAusencias.setText(String.valueOf(snapshot.resumo().ausenciasAprovadas()));
            lblOperacaoPedidos.setText(String.valueOf(snapshot.resumo().totalPedidosPendentes()));
            lblResumoOperacao.setText(construirResumoOperacional(snapshot));

            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList(snapshot.equipaEscalada()));
            esconderFeedbackOperacao();
        } catch (IllegalArgumentException e) {
            mostrarFeedbackOperacao(e.getMessage());
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
        } catch (Exception e) {
            mostrarFeedbackOperacao("Nao foi possivel carregar o painel diario da loja.");
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
        }
    }

    private LocalDate resolverDataFimOperacao(LocalDate dataInicio) {
        String intervalo = cbIntervaloOperacao.getValue() != null ? cbIntervaloOperacao.getValue() : "Dia";

        return switch (intervalo) {
            case "3 dias" -> dataInicio.plusDays(2);
            case "Semana" -> dataInicio.plusDays(6);
            default -> dataInicio;
        };
    }

    private String construirResumoOperacional(SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot) {
        String periodo = snapshot.intervalo().unicoDia()
                ? "para " + formatarData(snapshot.intervalo().dataInicio())
                : "de " + formatarData(snapshot.intervalo().dataInicio()) + " a " + formatarData(snapshot.intervalo().dataFim());

        String alertas = snapshot.resumo().totalPedidosPendentes() > 0
                ? snapshot.resumo().totalPedidosPendentes() + " pedidos pendentes precisam de acompanhamento."
                : "Sem pedidos pendentes no periodo selecionado.";

        return "Operacao da loja " + snapshot.contexto().nomeLoja() + " " + periodo + ". " + alertas;
    }

    private String formatarTurnosOperacao(List<SnapshotOperacionalLojaBLL.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "Sem turnos no periodo";
        }

        return turnos.stream()
                .map(turno -> formatarData(turno.data()) + " | " + turno.periodo())
                .collect(Collectors.joining("; "));
    }

    private String formatarEstadosOperacao(List<SnapshotOperacionalLojaBLL.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "-";
        }

        return turnos.stream()
                .map(SnapshotOperacionalLojaBLL.TurnoPlaneado::estado)
                .filter(estado -> estado != null && !estado.isBlank())
                .map(estado -> Character.toUpperCase(estado.toLowerCase(Locale.ROOT).charAt(0))
                        + estado.toLowerCase(Locale.ROOT).substring(1))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private void mostrarFeedbackOperacao(String mensagem) {
        lblFeedbackOperacao.setText(mensagem);
        lblFeedbackOperacao.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!lblFeedbackOperacao.getStyleClass().contains("mensagem-feedback")) {
            lblFeedbackOperacao.getStyleClass().add("mensagem-feedback");
        }
        lblFeedbackOperacao.getStyleClass().add("mensagem-erro");
        lblFeedbackOperacao.setVisible(true);
        lblFeedbackOperacao.setManaged(true);
    }

    private void esconderFeedbackOperacao() {
        lblFeedbackOperacao.setText("");
        lblFeedbackOperacao.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        if (!lblFeedbackOperacao.getStyleClass().contains("mensagem-feedback")) {
            lblFeedbackOperacao.getStyleClass().add("mensagem-feedback");
        }
        lblFeedbackOperacao.setVisible(false);
        lblFeedbackOperacao.setManaged(false);
    }

    private String formatarData(LocalDate data) {
        return data == null ? "-" : DATA_FORMATTER.format(data);
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }

        String valor = estado.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }
}
