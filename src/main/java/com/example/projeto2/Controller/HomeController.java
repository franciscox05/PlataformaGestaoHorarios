package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
public class HomeController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORARIO_COMPACTO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale LOCALE_PT = Locale.forLanguageTag("pt-PT");

    @FXML
    private Label lblBemVindo;

    @FXML
    private Button btnAtalhoFolga;

    @FXML
    private Button btnAtalhoPreferencias;

    @FXML
    private Button btnAtalhoPerfil;

    @FXML
    private HBox painelAcoesGestao;

    @FXML
    private Button btnAtalhoPainelGerente;

    @FXML
    private Button btnAtalhoHorarios;

    @FXML
    private Button btnAtalhoRelatorios;

    @FXML
    private Label lblSemanaHorarioPublicadoIntervalo;

    @FXML
    private Label lblResumoHorarioPublicado;

    @FXML
    private HBox boxSemanaHorarioPublicado;

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
    private javafx.scene.control.DatePicker dpDataOperacao;

    @FXML
    private ComboBox<String> cbIntervaloOperacao;

    @FXML
    private ComboBox<ColaboradorFiltroOption> cbColaboradorOperacao;

    @FXML
    private Label lblResumoOperacao;

    @FXML
    private Label lblResumoEscalaPublicada;

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

    @FXML
    private TableView<Horario> tabelaEscalaDetalhada;

    @FXML
    private TableColumn<Horario, String> colEscalaData;

    @FXML
    private TableColumn<Horario, String> colEscalaColaborador;

    @FXML
    private TableColumn<Horario, String> colEscalaCargo;

    @FXML
    private TableColumn<Horario, String> colEscalaHorario;

    @FXML
    private TableColumn<Horario, String> colEscalaEstado;

    @FXML
    private ComboBox<MesOption> cbMesHorarioMensal;

    @FXML
    private Spinner<Integer> spAnoHorarioMensal;

    @FXML
    private Label lblResumoHorarioMensal;

    @FXML
    private TableView<LinhaHorarioMensal> tabelaHorarioMensalLoja;

    private final HorarioBLL horarioBll;
    private final SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;
    private final GestaoLojaBLL gestaoLojaBLL;
    private Utilizador utilizadorLogado;
    private DashboardNavigator dashboardNavigation;
    private LocalDate semanaHorarioPublicadoInicio;

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
        configurarTabelaEscalaDetalhada();
        configurarPainelHorarioPublicado();
        configurarPainelOperacao();
        configurarPainelHorarioMensal();
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        if (utilizador == null) {
            return;
        }

        this.utilizadorLogado = utilizador;
        lblBemVindo.setText("Ola, " + utilizador.getNome() + "!");

        carregarHorarioPublicado();
        carregarEquipaHoje();
        configurarVisibilidadePainelOperacao();
    }

    public void setDashboardNavigation(DashboardNavigator dashboardNavigation) {
        this.dashboardNavigation = dashboardNavigation;
    }

    @FXML
    public void onAtalhoFolgaClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirFolgas();
        }
    }

    @FXML
    public void onAtalhoPreferenciasClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPreferencias();
        }
    }

    @FXML
    public void onAtalhoPerfilClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPerfil();
        }
    }

    @FXML
    public void onAtalhoPainelGerenteClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPainelGerente();
        }
    }

    @FXML
    public void onAtalhoHorariosClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirHorarios();
        }
    }

    @FXML
    public void onAtalhoRelatoriosClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirRelatorios();
        }
    }

    @FXML
    public void onSemanaHorarioAnteriorClick() {
        semanaHorarioPublicadoInicio = semanaHorarioPublicadoInicio.minusWeeks(1);
        carregarHorarioPublicado();
    }

    @FXML
    public void onSemanaHorarioSeguinteClick() {
        semanaHorarioPublicadoInicio = semanaHorarioPublicadoInicio.plusWeeks(1);
        carregarHorarioPublicado();
    }

    private void configurarTabelaTurnos() {
        colData.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarData(cellData.getValue().getDataTurno())));

        colHorario.setCellValueFactory(cellData -> {
            Horario horario = cellData.getValue();
            return new SimpleStringProperty(horario.getIdTurno().getHoraInicio() + " - " + horario.getIdTurno().getHoraFim());
        });

        colLoja.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getIdLojautilizador().getIdLoja().getNome()
                                + " | "
                                + capitalizar(textoOuTraco(cellData.getValue().getIdTurno().getTipo()))
                ));

        colEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        tabelaTurnos.setPlaceholder(new Label("Nao existe horario publicado no intervalo selecionado. Ajusta as datas para veres mais dias publicados."));
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

        tabelaEquipaHoje.setPlaceholder(new Label("Nao ha equipa escalada para hoje na tua loja. O painel operacional abaixo pode ajudar-te a validar outros dias."));
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

        tabelaOperacaoLoja.setPlaceholder(new Label("Nao existe equipa escalada para o periodo selecionado. Usa o atalho de horarios para rever o planeamento."));
        tabelaOperacaoLoja.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && cbColaboradorOperacao != null) {
                cbColaboradorOperacao.getItems().stream()
                        .filter(item -> Objects.equals(novo.idUtilizador(), item.idUtilizador()))
                        .findFirst()
                        .ifPresent(cbColaboradorOperacao::setValue);
            }
        });
    }

    private void configurarTabelaEscalaDetalhada() {
        colEscalaData.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarData(cellData.getValue().getDataTurno())));

        colEscalaColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdUtilizador().getNome()));

        colEscalaCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdCargo() != null
                        ? cellData.getValue().getIdLojautilizador().getIdCargo().getNome()
                        : "-"));

        colEscalaHorario.setCellValueFactory(cellData -> {
            Horario horario = cellData.getValue();
            return new SimpleStringProperty(horario.getIdTurno().getHoraInicio() + " - " + horario.getIdTurno().getHoraFim());
        });

        colEscalaEstado.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado())));

        tabelaEscalaDetalhada.setPlaceholder(new Label("Nao existem turnos publicados para este colaborador ou periodo."));
    }

    private void configurarPainelHorarioPublicado() {
        semanaHorarioPublicadoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        lblSemanaHorarioPublicadoIntervalo.setText(CalendarioSemanalHelper.formatarIntervaloSemana(semanaHorarioPublicadoInicio));
    }

    private void configurarPainelOperacao() {
        cbIntervaloOperacao.setItems(FXCollections.observableArrayList("Dia", "3 dias", "Semana"));
        cbIntervaloOperacao.setValue("Dia");
        dpDataOperacao.setValue(LocalDate.now());
        cbColaboradorOperacao.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
        cbColaboradorOperacao.setValue(ColaboradorFiltroOption.todos());

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

        cbColaboradorOperacao.valueProperty().addListener((obs, antigo, novo) -> {
            if (painelOperacaoLoja.isVisible()) {
                carregarEscalaDetalhadaLoja();
            }
        });
    }

    private void configurarPainelHorarioMensal() {
        cbMesHorarioMensal.setItems(FXCollections.observableArrayList(
                new MesOption(1, "Janeiro"),
                new MesOption(2, "Fevereiro"),
                new MesOption(3, "Marco"),
                new MesOption(4, "Abril"),
                new MesOption(5, "Maio"),
                new MesOption(6, "Junho"),
                new MesOption(7, "Julho"),
                new MesOption(8, "Agosto"),
                new MesOption(9, "Setembro"),
                new MesOption(10, "Outubro"),
                new MesOption(11, "Novembro"),
                new MesOption(12, "Dezembro")
        ));
        cbMesHorarioMensal.setValue(cbMesHorarioMensal.getItems().stream()
                .filter(item -> item.numero() == LocalDate.now().getMonthValue())
                .findFirst()
                .orElse(null));

        spAnoHorarioMensal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - 1,
                LocalDate.now().getYear() + 3,
                LocalDate.now().getYear()
        ));
        spAnoHorarioMensal.setEditable(true);

        tabelaHorarioMensalLoja.setPlaceholder(new Label("Nao existem horarios publicados para o mes selecionado."));

        cbMesHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (painelOperacaoLoja.isVisible()) {
                carregarHorarioMensalLoja();
            }
        });
        spAnoHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (painelOperacaoLoja.isVisible()) {
                carregarHorarioMensalLoja();
            }
        });
    }

    private void carregarHorarioPublicado() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            LocalDate dataInicio = semanaHorarioPublicadoInicio != null
                    ? semanaHorarioPublicadoInicio
                    : CalendarioSemanalHelper.inicioSemana(LocalDate.now());
            LocalDate dataFim = dataInicio.plusDays(6);
            lblSemanaHorarioPublicadoIntervalo.setText(CalendarioSemanalHelper.formatarIntervaloSemana(dataInicio));

            List<Horario> turnos = horarioBll.listarHorarioPublicadoDoUtilizador(utilizadorLogado.getId(), dataInicio, dataFim);
            tabelaTurnos.setItems(FXCollections.observableArrayList(turnos));
            renderizarCalendarioHorarioPublicado(dataInicio, turnos);

            if (turnos.isEmpty()) {
                lblResumoHorarioPublicado.setText(
                        "Nao existe nenhum turno publicado para ti entre "
                                + formatarData(dataInicio)
                                + " e "
                                + formatarData(dataFim)
                                + "."
                );
                return;
            }

            Horario proximoTurno = turnos.getFirst();
            lblResumoHorarioPublicado.setText(
                    turnos.size()
                            + " turnos publicados entre "
                            + formatarData(dataInicio)
                            + " e "
                            + formatarData(dataFim)
                            + ". Proximo turno: "
                            + formatarData(proximoTurno.getDataTurno())
                            + " | "
                            + proximoTurno.getIdTurno().getHoraInicio()
                            + " - "
                            + proximoTurno.getIdTurno().getHoraFim()
                            + "."
            );
        } catch (Exception e) {
            tabelaTurnos.setItems(FXCollections.observableArrayList());
            lblResumoHorarioPublicado.setText("Nao foi possivel carregar o horario publicado. Tenta novamente dentro de instantes.");
            renderizarCalendarioHorarioPublicado(
                    semanaHorarioPublicadoInicio != null ? semanaHorarioPublicadoInicio : CalendarioSemanalHelper.inicioSemana(LocalDate.now()),
                    List.of()
            );
        }
    }

    private void carregarEquipaHoje() {
        if (utilizadorLogado != null) {
            try {
                List<Horario> equipaHoje = horarioBll.listarEquipaDeHoje(utilizadorLogado.getId());
                tabelaEquipaHoje.setItems(FXCollections.observableArrayList(equipaHoje));
            } catch (Exception e) {
                tabelaEquipaHoje.setItems(FXCollections.observableArrayList());
            }
        }
    }

    private void configurarVisibilidadePainelOperacao() {
        boolean podeGerirLoja = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());

        painelOperacaoLoja.setManaged(podeGerirLoja);
        painelOperacaoLoja.setVisible(podeGerirLoja);
        painelAcoesGestao.setManaged(podeGerirLoja);
        painelAcoesGestao.setVisible(podeGerirLoja);

        if (podeGerirLoja) {
            carregarColaboradoresOperacao();
            carregarOperacaoLoja();
            carregarHorarioMensalLoja();
        } else {
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            tabelaEscalaDetalhada.setItems(FXCollections.observableArrayList());
            tabelaHorarioMensalLoja.setItems(FXCollections.observableArrayList());
            tabelaHorarioMensalLoja.getColumns().clear();
            lblResumoHorarioMensal.setText("A vista mensal da loja esta disponivel apenas para perfis de gestao.");
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
            carregarEscalaDetalhadaLoja();
            esconderFeedbackOperacao();
        } catch (IllegalArgumentException e) {
            mostrarFeedbackOperacao(e.getMessage());
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            tabelaEscalaDetalhada.setItems(FXCollections.observableArrayList());
            lblResumoEscalaPublicada.setText("Escala publicada indisponivel para o periodo atual.");
        } catch (Exception e) {
            mostrarFeedbackOperacao("Nao foi possivel carregar o painel diario da loja.");
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            tabelaEscalaDetalhada.setItems(FXCollections.observableArrayList());
            lblResumoEscalaPublicada.setText("Escala publicada indisponivel para o periodo atual.");
        }
    }

    private void carregarColaboradoresOperacao() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            ColaboradorFiltroOption anterior = cbColaboradorOperacao.getValue();
            List<ColaboradorFiltroOption> opcoes = horarioBll.listarColaboradoresAtivosDaLojaDoUtilizador(utilizadorLogado.getId()).stream()
                    .sorted(Comparator.comparing(HorarioBLL.ColaboradorLoja::nome, String.CASE_INSENSITIVE_ORDER))
                    .map(colaborador -> new ColaboradorFiltroOption(
                            colaborador.idUtilizador(),
                            colaborador.etiqueta()
                    ))
                    .toList();

            cbColaboradorOperacao.setItems(FXCollections.observableArrayList());
            cbColaboradorOperacao.getItems().add(ColaboradorFiltroOption.todos());
            cbColaboradorOperacao.getItems().addAll(opcoes);

            if (anterior != null) {
                cbColaboradorOperacao.getItems().stream()
                        .filter(item -> Objects.equals(item.idUtilizador(), anterior.idUtilizador()))
                        .findFirst()
                        .ifPresentOrElse(cbColaboradorOperacao::setValue, () -> cbColaboradorOperacao.setValue(ColaboradorFiltroOption.todos()));
            } else {
                cbColaboradorOperacao.setValue(ColaboradorFiltroOption.todos());
            }
        } catch (Exception e) {
            cbColaboradorOperacao.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
            cbColaboradorOperacao.setValue(ColaboradorFiltroOption.todos());
        }
    }

    private void carregarEscalaDetalhadaLoja() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            LocalDate dataInicio = dpDataOperacao.getValue() != null ? dpDataOperacao.getValue() : LocalDate.now();
            LocalDate dataFim = resolverDataFimOperacao(dataInicio);
            Integer idColaborador = cbColaboradorOperacao.getValue() != null ? cbColaboradorOperacao.getValue().idUtilizador() : null;

            List<Horario> horarios = horarioBll.listarHorarioPublicadoDaLojaDoUtilizador(
                    utilizadorLogado.getId(),
                    dataInicio,
                    dataFim,
                    idColaborador
            );

            tabelaEscalaDetalhada.setItems(FXCollections.observableArrayList(horarios));

            String etiquetaColaborador = cbColaboradorOperacao.getValue() != null
                    ? cbColaboradorOperacao.getValue().label()
                    : "toda a equipa";

            if (horarios.isEmpty()) {
                lblResumoEscalaPublicada.setText(
                        "Nao ha horarios publicados para "
                                + etiquetaColaborador.toLowerCase(Locale.ROOT)
                                + " no periodo selecionado."
                );
                return;
            }

            lblResumoEscalaPublicada.setText(
                    horarios.size()
                            + " turnos publicados para "
                            + etiquetaColaborador
                            + " entre "
                            + formatarData(dataInicio)
                            + " e "
                            + formatarData(dataFim)
                            + "."
            );
        } catch (Exception e) {
            tabelaEscalaDetalhada.setItems(FXCollections.observableArrayList());
            lblResumoEscalaPublicada.setText("Nao foi possivel carregar a escala publicada da loja neste momento.");
        }
    }

    private void carregarHorarioMensalLoja() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            MesOption mesSelecionado = cbMesHorarioMensal.getValue();
            Integer anoSelecionado = spAnoHorarioMensal.getValue();
            if (mesSelecionado == null || anoSelecionado == null) {
                tabelaHorarioMensalLoja.getColumns().clear();
                tabelaHorarioMensalLoja.setItems(FXCollections.observableArrayList());
                lblResumoHorarioMensal.setText("Seleciona um mes e um ano para veres o horario mensal da loja.");
                return;
            }

            List<Horario> horarios = horarioBll.listarHorarioPublicadoMensalDaLojaDoUtilizador(
                    utilizadorLogado.getId(),
                    anoSelecionado,
                    mesSelecionado.numero()
            );

            YearMonth periodo = YearMonth.of(anoSelecionado, mesSelecionado.numero());
            reconstruirTabelaHorarioMensal(periodo, horarios);

            if (horarios.isEmpty()) {
                lblResumoHorarioMensal.setText(
                        "Ainda nao existem horarios publicados para "
                                + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                                + " de "
                                + anoSelecionado
                                + "."
                );
                return;
            }

            lblResumoHorarioMensal.setText(
                    "Vista mensal publicada da loja para "
                            + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                            + " de "
                            + anoSelecionado
                            + ", com "
                            + tabelaHorarioMensalLoja.getItems().size()
                            + " colaborador(es) e "
                            + horarios.size()
                            + " turno(s)."
            );
        } catch (Exception e) {
            tabelaHorarioMensalLoja.getColumns().clear();
            tabelaHorarioMensalLoja.setItems(FXCollections.observableArrayList());
            lblResumoHorarioMensal.setText("Nao foi possivel carregar o horario mensal da loja neste momento.");
        }
    }

    private void reconstruirTabelaHorarioMensal(YearMonth periodo, List<Horario> horarios) {
        tabelaHorarioMensalLoja.getColumns().clear();

        TableColumn<LinhaHorarioMensal, String> colColaborador = new TableColumn<>("Colaborador");
        colColaborador.setPrefWidth(220.0);
        colColaborador.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nome()));

        TableColumn<LinhaHorarioMensal, String> colCargo = new TableColumn<>("Cargo");
        colCargo.setPrefWidth(150.0);
        colCargo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().cargo()));

        tabelaHorarioMensalLoja.getColumns().add(colColaborador);
        tabelaHorarioMensalLoja.getColumns().add(colCargo);

        for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
            final int diaAtual = dia;
            LocalDate data = periodo.atDay(diaAtual);
            String cabecalho = String.format("%02d %s", diaAtual, abreviarDia(data));

            TableColumn<LinhaHorarioMensal, String> colunaDia = new TableColumn<>(cabecalho);
            colunaDia.setPrefWidth(108.0);
            colunaDia.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().valorNoDia(diaAtual)));
            colunaDia.setCellFactory(coluna -> criarCelulaHorarioMensal());
            tabelaHorarioMensalLoja.getColumns().add(colunaDia);
        }

        Map<Integer, LinhaHorarioMensalBuilder> porColaborador = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null
                    || horario.getDataTurno() == null) {
                continue;
            }

            Integer idColaborador = horario.getIdLojautilizador().getIdUtilizador().getId();
            LinhaHorarioMensalBuilder builder = porColaborador.computeIfAbsent(
                    idColaborador,
                    chave -> new LinhaHorarioMensalBuilder(
                            horario.getIdLojautilizador().getIdUtilizador().getNome(),
                            horario.getIdLojautilizador().getIdCargo() != null
                                    ? horario.getIdLojautilizador().getIdCargo().getNome()
                                    : "-"
                    )
            );
            builder.adicionar(horario.getDataTurno().getDayOfMonth(), formatarTurnoMensal(horario));
        }

        List<LinhaHorarioMensal> linhas = porColaborador.values().stream()
                .map(LinhaHorarioMensalBuilder::build)
                .sorted(Comparator.comparing(LinhaHorarioMensal::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();

        tabelaHorarioMensalLoja.setItems(FXCollections.observableArrayList(linhas));
    }

    private TableCell<LinhaHorarioMensal, String> criarCelulaHorarioMensal() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                getStyleClass().removeAll("mensal-dia-com-turno", "mensal-dia-vazio");

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item);
                setWrapText(true);
                getStyleClass().add("-".equals(item) ? "mensal-dia-vazio" : "mensal-dia-com-turno");
            }
        };
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

    private String formatarTurnoMensal(Horario horario) {
        if (horario == null || horario.getIdTurno() == null) {
            return "-";
        }

        String inicio = horario.getIdTurno().getHoraInicio() != null
                ? horario.getIdTurno().getHoraInicio().format(HORARIO_COMPACTO)
                : "--:--";
        String fim = horario.getIdTurno().getHoraFim() != null
                ? horario.getIdTurno().getHoraFim().format(HORARIO_COMPACTO)
                : "--:--";

        return inicio + "-" + fim;
    }

    private String abreviarDia(LocalDate data) {
        return data.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, LOCALE_PT)
                .replace(".", "")
                .toUpperCase(LOCALE_PT);
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }

        String valor = texto.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private String textoOuTraco(Object valor) {
        return valor == null ? "-" : valor.toString();
    }

    private void renderizarCalendarioHorarioPublicado(LocalDate inicioSemana, List<Horario> horarios) {
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();

        for (Horario horario : horarios) {
            String evento = horario.getIdTurno().getHoraInicio()
                    + " - "
                    + horario.getIdTurno().getHoraFim()
                    + " | "
                    + horario.getIdLojautilizador().getIdLoja().getNome();
            eventosPorDia.computeIfAbsent(horario.getDataTurno(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxSemanaHorarioPublicado,
                inicioSemana,
                eventosPorDia,
                "Sem turno publicado"
        );
    }

    private record ColaboradorFiltroOption(Integer idUtilizador, String label) {
        private static ColaboradorFiltroOption todos() {
            return new ColaboradorFiltroOption(null, "Toda a equipa");
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record MesOption(int numero, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    private record LinhaHorarioMensal(String nome, String cargo, Map<Integer, String> turnosPorDia) {
        private String valorNoDia(int dia) {
            return turnosPorDia.getOrDefault(dia, "-");
        }
    }

    private static final class LinhaHorarioMensalBuilder {
        private final String nome;
        private final String cargo;
        private final Map<Integer, String> turnosPorDia = new LinkedHashMap<>();

        private LinhaHorarioMensalBuilder(String nome, String cargo) {
            this.nome = nome;
            this.cargo = cargo;
        }

        private void adicionar(int dia, String valor) {
            turnosPorDia.merge(dia, valor, (anterior, novo) -> anterior + " / " + novo);
        }

        private LinhaHorarioMensal build() {
            return new LinhaHorarioMensal(nome, cargo, turnosPorDia);
        }
    }
}
