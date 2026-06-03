package com.example.projeto2.Controller;

import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.stage.Window;
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

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private ComboBox<Horario> cbMeuTurno;

    @FXML
    private ComboBox<ColegaElegivel> cbColegaElegivel;

    @FXML
    private ComboBox<Horario> cbTurnoColega;

    @FXML
    private Label lblMensagem;

    @FXML
    private Label lblTurnosElegiveis;

    @FXML
    private Button btnSubmeterTroca;

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

    @FXML
    private VBox painelAprovacao;

    @FXML
    private TableView<Permuta> tabelaPedidosPendentes;

    @FXML
    private TableColumn<Permuta, String> colColaboradorPendente;

    @FXML
    private TableColumn<Permuta, String> colPedidoPendente;

    @FXML
    private TableColumn<Permuta, String> colOrigemPendente;

    @FXML
    private TableColumn<Permuta, String> colDestinoPendente;

    @FXML
    private Button btnAprovarPermuta;

    @FXML
    private Button btnRejeitarPermuta;

    private final HorarioBLL horarioBll;
    private final PermutaBLL permutaBll;
    private Utilizador utilizadorLogado;
    private List<Horario> turnosElegiveisAtuais = List.of();

    public PermutasController(HorarioBLL horarioBll, PermutaBLL permutaBll) {
        this.horarioBll = horarioBll;
        this.permutaBll = permutaBll;
    }

    @FXML
    public void initialize() {
        configurarCombos();
        configurarTabelaHistorico();
        configurarTabelaPendentes();

        lblMensagem.setManaged(false);
        lblMensagem.setVisible(false);
        atualizarResumoElegiveis(0);

        tabelaPedidosPermuta.setPlaceholder(new Label("Ainda não submeteste pedidos de permuta."));
        tabelaPedidosPendentes.setPlaceholder(new Label("Não existem pedidos pendentes para aprovar."));

        btnSubmeterTroca.disableProperty().bind(
                cbMeuTurno.getSelectionModel().selectedItemProperty().isNull()
                        .or(cbColegaElegivel.getSelectionModel().selectedItemProperty().isNull())
                        .or(cbTurnoColega.getSelectionModel().selectedItemProperty().isNull())
        );
        btnAprovarPermuta.disableProperty().bind(Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));
        btnRejeitarPermuta.disableProperty().bind(Bindings.isNull(tabelaPedidosPendentes.getSelectionModel().selectedItemProperty()));

        painelAprovacao.setManaged(false);
        painelAprovacao.setVisible(false);
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        this.utilizadorLogado = utilizador;
        carregarMeusTurnos();
        carregarHistorico();
        configurarPainelAprovacao();
    }

    @FXML
    public void onSubmeterTrocaClick() {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Submeter permuta",
                    "Deseja submeter este pedido de permuta?",
                    "O pedido ficará pendente para aprovação."
            )) {
                return;
            }

            permutaBll.registarPedidoTroca(
                    utilizadorLogado.getId(),
                    cbMeuTurno.getValue(),
                    cbTurnoColega.getValue()
            );

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

    @FXML
    public void onAprovarPermutaClick() {
        tratarPedidoSelecionado(true);
    }

    @FXML
    public void onRejeitarPermutaClick() {
        tratarPedidoSelecionado(false);
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

        cbColegaElegivel.setConverter(new StringConverter<>() {
            @Override
            public String toString(ColegaElegivel colega) {
                return colega == null ? "" : colega.nome();
            }

            @Override
            public ColegaElegivel fromString(String string) {
                return null;
            }
        });

        cbMeuTurno.setOnAction(event -> carregarColegasElegiveis());
        cbColegaElegivel.setOnAction(event -> filtrarTurnosDoColega());
    }

    private void configurarTabelaHistorico() {
        colDataPedido.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataPedido(cellData.getValue())));

        colTurnoOrigem.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoProprio(cellData.getValue().getIdHorarioOrigem())));

        colTurnoDestino.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoColega(cellData.getValue().getIdHorarioDestino())));

        colEstadoPermuta.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado() != null ? cellData.getValue().getEstado().name() : null)));
    }

    private void configurarTabelaPendentes() {
        colColaboradorPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(obterNomeSolicitante(cellData.getValue())));

        colPedidoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarDataPedido(cellData.getValue())));

        colOrigemPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoProprio(cellData.getValue().getIdHorarioOrigem())));

        colDestinoPendente.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarTurnoColega(cellData.getValue().getIdHorarioDestino())));
    }

    private void carregarMeusTurnos() {
        if (utilizadorLogado == null) {
            cbMeuTurno.setItems(FXCollections.observableArrayList());
            return;
        }

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

        turnosElegiveisAtuais = horarioBll.listarTurnosElegiveisParaPermuta(
                utilizadorLogado.getId(),
                meuTurno.getId()
        );

        Map<Integer, ColegaElegivel> colegasPorId = new LinkedHashMap<>();
        for (Horario horario : turnosElegiveisAtuais) {
            if (horario.getIdLojautilizador() == null || horario.getIdLojautilizador().getIdUtilizador() == null) {
                continue;
            }
            Integer idColega = horario.getIdLojautilizador().getIdUtilizador().getId();
            String nomeColega = horario.getIdLojautilizador().getIdUtilizador().getNome();
            colegasPorId.putIfAbsent(idColega, new ColegaElegivel(idColega, nomeColega));
        }

        cbColegaElegivel.setItems(FXCollections.observableArrayList(colegasPorId.values()));
        atualizarResumoElegiveis(turnosElegiveisAtuais.size());

        if (colegasPorId.size() == 1) {
            cbColegaElegivel.getSelectionModel().selectFirst();
        }
    }

    private void filtrarTurnosDoColega() {
        cbTurnoColega.setValue(null);

        ColegaElegivel colega = cbColegaElegivel.getValue();
        if (colega == null) {
            cbTurnoColega.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Horario> turnosDoColega = turnosElegiveisAtuais.stream()
                .filter(horario -> horario.getIdLojautilizador() != null)
                .filter(horario -> horario.getIdLojautilizador().getIdUtilizador() != null)
                .filter(horario -> Objects.equals(horario.getIdLojautilizador().getIdUtilizador().getId(), colega.idUtilizador()))
                .toList();
        cbTurnoColega.setItems(FXCollections.observableArrayList(turnosDoColega));

        if (turnosDoColega.size() == 1) {
            cbTurnoColega.getSelectionModel().selectFirst();
        }
    }

    private void atualizarResumoElegiveis(int totalTurnos) {
        if (lblTurnosElegiveis == null) {
            return;
        }

        if (totalTurnos == 0) {
            lblTurnosElegiveis.setText("Nao existem colegas elegiveis para o turno selecionado.");
        } else {
            lblTurnosElegiveis.setText(totalTurnos + " turno(s) elegiveis encontrados para este dia.");
        }
    }

    private void carregarHistorico() {
        if (utilizadorLogado == null) {
            tabelaPedidosPermuta.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Permuta> pedidos = permutaBll.listarPedidosEnviados(utilizadorLogado.getId());
        tabelaPedidosPermuta.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void configurarPainelAprovacao() {
        boolean podeAprovar = utilizadorLogado != null && permutaBll.utilizadorPodeAprovarPermutas(utilizadorLogado.getId());
        painelAprovacao.setManaged(podeAprovar);
        painelAprovacao.setVisible(podeAprovar);

        if (podeAprovar) {
            carregarPedidosPendentes();
        } else {
            tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
        }
    }

    private void carregarPedidosPendentes() {
        if (utilizadorLogado == null || !permutaBll.utilizadorPodeAprovarPermutas(utilizadorLogado.getId())) {
            tabelaPedidosPendentes.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Permuta> pedidos = permutaBll.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId());
        tabelaPedidosPendentes.setItems(FXCollections.observableArrayList(pedidos));
    }

    private void tratarPedidoSelecionado(boolean aprovar) {
        try {
            if (utilizadorLogado == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            Permuta pedidoSelecionado = tabelaPedidosPendentes.getSelectionModel().getSelectedItem();
            if (pedidoSelecionado == null) {
                throw new IllegalArgumentException("Seleciona um pedido pendente primeiro.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar permuta" : "Rejeitar permuta",
                    aprovar ? "Deseja aprovar esta permuta?" : "Deseja rejeitar esta permuta?",
                    aprovar
                            ? "A aprovação ficará registada no sistema."
                            : "A rejeição ficará registada no sistema."
            )) {
                return;
            }

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

    private void limparFormulario() {
        cbMeuTurno.setValue(null);
        cbColegaElegivel.setValue(null);
        cbColegaElegivel.setItems(FXCollections.observableArrayList());
        cbTurnoColega.setValue(null);
        cbTurnoColega.setItems(FXCollections.observableArrayList());
        turnosElegiveisAtuais = List.of();
        atualizarResumoElegiveis(0);
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

    private String obterNomeSolicitante(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null || permuta.getIdHorarioOrigem().getIdLojautilizador() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() == null) {
            return "-";
        }

        return permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome();
    }

    private Window obterJanela() {
        if (cbMeuTurno == null || cbMeuTurno.getScene() == null) {
            return null;
        }
        return cbMeuTurno.getScene().getWindow();
    }

    public record ColegaElegivel(Integer idUtilizador, String nome) {
    }
}
