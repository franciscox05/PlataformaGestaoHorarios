package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PainelGerenteService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.SnapshotOperacionalLojaService;
import com.example.projeto2.DESKTOP.support.CalendarioMensalHelper;
import com.example.projeto2.DESKTOP.support.CalendarioSemanalHelper;
import com.example.projeto2.DESKTOP.support.DetalheDiaDialog;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.DESKTOP.support.GrelhaHorarioHelper;
import com.example.projeto2.DESKTOP.support.HomePedidosHelper;
import com.example.projeto2.DESKTOP.support.MesOption;
import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
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
    private Label lblDataHoje;

    @FXML
    private Label lblChipEscala;

    @FXML
    private Button btnAprovarHorarioHome;

    @FXML
    private Label lblProximoTurnoInfo;

    @FXML
    private Button btnAtalhoFolga;

    @FXML
    private Button btnAtalhoPreferencias;

    @FXML
    private Button btnAtalhoPerfil;

    @FXML
    private HBox painelAcoesGestao;

    @FXML
    private VBox painelMetricasLoja;

    @FXML
    private Button btnAtalhoPainelGerente;

    @FXML
    private Button btnAtalhoHorarios;

    @FXML
    private Button btnAtalhoRelatorios;

    @FXML
    private Label lblSemanaHorarioPublicadoIntervalo;

    @FXML
    private ComboBox<SemanaOption> cbSemanaHorarioPublicado;

    @FXML
    private Label lblResumoHorarioPublicado;

    @FXML
    private HBox boxSemanaHorarioPublicado;

    @FXML
    private Button pillTodos;
    @FXML
    private Button pillManha;
    @FXML
    private Button pillTarde;
    @FXML
    private Button pillNoite;

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
    private TableView<SnapshotOperacionalLojaService.ColaboradorEscala> tabelaOperacaoLoja;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorEscala, String> colOperacaoColaborador;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorEscala, String> colOperacaoCargo;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorEscala, String> colOperacaoTurnos;

    @FXML
    private TableColumn<SnapshotOperacionalLojaService.ColaboradorEscala, String> colOperacaoEstado;

    @FXML
    private HBox boxEscalaDetalhadaLoja;

    @FXML
    private Button btnVistaStripEscala;

    @FXML
    private Button btnVistaGrelhaEscala;

    @FXML
    private ScrollPane scrollGrelhaEscalaLoja;

    @FXML
    private VBox boxGrelhaEscalaLoja;

    @FXML
    private ComboBox<MesOption> cbMesHorarioMensal;

    @FXML
    private ComboBox<ColaboradorFiltroOption> cbColaboradorHorarioMensal;

    @FXML
    private Spinner<Integer> spAnoHorarioMensal;

    @FXML
    private Label lblResumoHorarioMensal;

    @FXML
    private VBox secaoGrelhaEquipaMensal;

    @FXML
    private VBox boxGrelhaEquipaMensal;

    @FXML
    private Button btnVistaCalendarioHome;

    @FXML
    private Button btnVistaGrelhaHome;

    @FXML
    private VBox painelVistaCalendarioHome;

    @FXML
    private ScrollPane scrollGrelhaEquipaMensal;

    @FXML
    private GridPane calendarioMensalHome;

    private boolean vistaGrelhaAtiva = false;
    private boolean vistaGrelhaEscalaAtiva = false;

    @FXML
    private VBox painelPedidosPendentes;

    @FXML
    private Label lblPedidosPendentesSub;

    @FXML
    private VBox listaPedidosPendentes;

    @FXML
    private VBox painelMeusPedidos;

    @FXML
    private VBox listaMeusPedidos;

    @FXML
    private HBox bannerPendentes;

    @FXML
    private Label lblBannerPendentes;

    @FXML
    private Button btnBannerVerPedidos;

    private final HorarioService horarioBll;
    private final SnapshotOperacionalLojaService snapshotOperacionalLojaBLL;
    private final GestaoLojaService gestaoLojaBLL;
    private final DayOffService dayOffBLL;
    private final PermutaService permutaBLL;
    private final PainelGerenteService painelGerenteBLL;
    private Utilizador utilizadorLogado;
    private DashboardNavigator dashboardNavigation;
    private HomePedidosHelper pedidosHelper;
    private LocalDate semanaHorarioPublicadoInicio;
    private boolean aAtualizarSeletorSemana;
    private List<Horario> todosEquipaHoje = List.of();
    private List<Horario> horariosMensaisAtuais = List.of();
    private String filtroTipoAtivo = null;

    public HomeController(HorarioService horarioBll,
                          SnapshotOperacionalLojaService snapshotOperacionalLojaBLL,
                          GestaoLojaService gestaoLojaBLL,
                          DayOffService dayOffBLL,
                          PermutaService permutaBLL,
                          PainelGerenteService painelGerenteBLL) {
        this.horarioBll = horarioBll;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.painelGerenteBLL = painelGerenteBLL;
    }

    @FXML
    public void initialize() {
        configurarTabelaEquipaHoje();
        configurarTabelaOperacaoLoja();
        configurarPainelHorarioPublicado();
        configurarPainelOperacao();
        configurarPainelHorarioMensal();
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        if (utilizador == null) {
            return;
        }

        this.utilizadorLogado = utilizador;
        lblBemVindo.setText(construirSaudacao(utilizador.getNome()));
        atualizarDataHoje();

        pedidosHelper = new HomePedidosHelper(
                bannerPendentes, lblBannerPendentes,
                painelMeusPedidos, listaMeusPedidos,
                painelPedidosPendentes, listaPedidosPendentes, lblPedidosPendentesSub,
                dayOffBLL, permutaBLL, painelGerenteBLL, gestaoLojaBLL,
                this::obterJanela);

        carregarHorarioPublicado();
        carregarEquipaHoje();
        configurarVisibilidadePainelOperacao();
        pedidosHelper.carregarMeusPedidos(utilizadorLogado);
    }

    public void setDashboardNavigation(DashboardNavigator dashboardNavigation) {
        this.dashboardNavigation = dashboardNavigation;
    }

    @FXML
    public void onPillTodosClick() {
        filtroTipoAtivo = null;
        ativarPill(pillTodos);
        aplicarFiltroPill();
    }

    @FXML
    public void onPillManhaClick() {
        filtroTipoAtivo = "manha";
        ativarPill(pillManha);
        aplicarFiltroPill();
    }

    @FXML
    public void onPillTardeClick() {
        filtroTipoAtivo = "tarde";
        ativarPill(pillTarde);
        aplicarFiltroPill();
    }

    @FXML
    public void onPillNoiteClick() {
        filtroTipoAtivo = "noite";
        ativarPill(pillNoite);
        aplicarFiltroPill();
    }

    private void ativarPill(Button pillAtiva) {
        for (Button pill : new Button[]{pillTodos, pillManha, pillTarde, pillNoite}) {
            if (pill == null) continue;
            pill.getStyleClass().remove("home-filter-pill-active");
            if (!pill.getStyleClass().contains("home-filter-pill")) {
                pill.getStyleClass().add("home-filter-pill");
            }
        }
        if (pillAtiva != null) {
            pillAtiva.getStyleClass().remove("home-filter-pill");
            if (!pillAtiva.getStyleClass().contains("home-filter-pill-active")) {
                pillAtiva.getStyleClass().add("home-filter-pill-active");
            }
        }
    }

    private void aplicarFiltroPill() {
        List<Horario> filtrados = filtroTipoAtivo == null
                ? todosEquipaHoje
                : todosEquipaHoje.stream()
                        .filter(h -> h.getIdTurno() != null
                                && h.getIdTurno().getTipo() != null
                                && h.getIdTurno().getTipo().toLowerCase(Locale.ROOT).contains(filtroTipoAtivo))
                        .toList();
        tabelaEquipaHoje.setItems(FXCollections.observableArrayList(filtrados));
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
    public void onBannerVerPedidosClick() {
        if (dashboardNavigation != null) {
            dashboardNavigation.abrirPainelGerente();
        }
    }

    @FXML
    public void onVistaCalendarioHomeClick() {
        if (!vistaGrelhaAtiva) return;
        vistaGrelhaAtiva = false;
        atualizarToggleVistaHome();
        carregarHorarioMensalLoja();
    }

    @FXML
    public void onVistaGrelhaHomeClick() {
        if (vistaGrelhaAtiva) return;
        vistaGrelhaAtiva = true;
        atualizarToggleVistaHome();
        carregarHorarioMensalLoja();
    }

    @FXML
    public void onVistaStripEscalaClick() {
        if (!vistaGrelhaEscalaAtiva) return;
        vistaGrelhaEscalaAtiva = false;
        atualizarToggleVistaEscala();
        carregarEscalaDetalhadaLoja();
    }

    @FXML
    public void onVistaGrelhaEscalaClick() {
        if (vistaGrelhaEscalaAtiva) return;
        vistaGrelhaEscalaAtiva = true;
        atualizarToggleVistaEscala();
        carregarEscalaDetalhadaLoja();
    }

    private void atualizarToggleVistaEscala() {
        if (btnVistaStripEscala != null) {
            btnVistaStripEscala.getStyleClass().removeAll("home-vista-btn", "home-vista-btn-active");
            btnVistaStripEscala.getStyleClass().add(vistaGrelhaEscalaAtiva ? "home-vista-btn" : "home-vista-btn-active");
        }
        if (btnVistaGrelhaEscala != null) {
            btnVistaGrelhaEscala.getStyleClass().removeAll("home-vista-btn", "home-vista-btn-active");
            btnVistaGrelhaEscala.getStyleClass().add(vistaGrelhaEscalaAtiva ? "home-vista-btn-active" : "home-vista-btn");
        }
        if (boxEscalaDetalhadaLoja != null) {
            boxEscalaDetalhadaLoja.setVisible(!vistaGrelhaEscalaAtiva);
            boxEscalaDetalhadaLoja.setManaged(!vistaGrelhaEscalaAtiva);
        }
        if (scrollGrelhaEscalaLoja != null) {
            scrollGrelhaEscalaLoja.setVisible(vistaGrelhaEscalaAtiva);
            scrollGrelhaEscalaLoja.setManaged(vistaGrelhaEscalaAtiva);
        }
    }

    private void atualizarToggleVistaHome() {
        if (btnVistaCalendarioHome != null) {
            btnVistaCalendarioHome.getStyleClass().removeAll("home-vista-btn", "home-vista-btn-active");
            btnVistaCalendarioHome.getStyleClass().add(vistaGrelhaAtiva ? "home-vista-btn" : "home-vista-btn-active");
        }
        if (btnVistaGrelhaHome != null) {
            btnVistaGrelhaHome.getStyleClass().removeAll("home-vista-btn", "home-vista-btn-active");
            btnVistaGrelhaHome.getStyleClass().add(vistaGrelhaAtiva ? "home-vista-btn-active" : "home-vista-btn");
        }
        if (painelVistaCalendarioHome != null) {
            painelVistaCalendarioHome.setVisible(!vistaGrelhaAtiva);
            painelVistaCalendarioHome.setManaged(!vistaGrelhaAtiva);
        }
        if (scrollGrelhaEquipaMensal != null) {
            scrollGrelhaEquipaMensal.setVisible(vistaGrelhaAtiva);
            scrollGrelhaEquipaMensal.setManaged(vistaGrelhaAtiva);
        }
    }

    // ── Aprovar Horário ──────────────────────────────────────────────────────
    @FXML
    public void onAprovarHorarioClick() {
        // 1. Obter o horário selecionado na TableView
        Horario horarioSelecionado = tabelaEquipaHoje.getSelectionModel().getSelectedItem();

        if (horarioSelecionado == null) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Nenhum horário selecionado",
                    "Seleciona um horário na tabela antes de aprovar."
            );
            return;
        }

        // 2. Validar se o estado atual é PENDENTE
        if (horarioSelecionado.getEstado() != EstadoHorario.pendente) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Operação inválida",
                    "Só é possível aprovar horários no estado PENDENTE. "
                            + "Estado atual: " + horarioSelecionado.getEstado().name().toUpperCase()
            );
            return;
        }

        // Confirmação antes de persistir
        boolean confirmado = DialogosHelper.confirmarAcao(
                obterJanela(),
                "Aprovar horário",
                "Confirmas a aprovação deste horário?",
                "O estado será alterado para APROVADO e guardado na base de dados."
        );
        if (!confirmado) {
            return;
        }

        // 3 + 4. Atualizar estado para APROVADO e chamar a camada BLL para guardar no PostgreSQL
        try {
            horarioBll.aprovarHorario(horarioSelecionado.getId(), utilizadorLogado.getId());

            // 5. Alerta de sucesso
            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Horário aprovado",
                    "O horário foi aprovado com sucesso."
            );
            carregarEquipaHoje(); // refrescar a TableView
        } catch (IllegalStateException e) {
            // 5. Alerta de erro de negócio (estado inválido detectado na BLL)
            mostrarAlerta(Alert.AlertType.WARNING, "Operação inválida", e.getMessage());
        } catch (Exception e) {
            // 5. Alerta de erro genérico
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Erro ao aprovar",
                    "Não foi possível aprovar o horário. Tenta novamente."
            );
        }
    }

    // ── Auxiliares de UI ─────────────────────────────────────────────────────
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensagem) {
        javafx.application.Platform.runLater(() -> {
            Alert alerta = new Alert(tipo);
            alerta.setTitle(titulo);
            alerta.setHeaderText(null);
            alerta.setContentText(mensagem);
            alerta.initOwner(obterJanela());
            alerta.showAndWait();
        });
    }

    private javafx.stage.Window obterJanela() {
        if (lblBemVindo == null || lblBemVindo.getScene() == null) {
            return null;
        }
        return lblBemVindo.getScene().getWindow();
    }
    // ────────────────────────────────────────────────────────────────────────

    @FXML
    public void onSemanaHorarioAnteriorClick() {
        semanaHorarioPublicadoInicio = semanaHorarioPublicadoInicio.minusWeeks(1);
        atualizarCabecalhoSemanaHorarioPublicado();
        carregarHorarioPublicado();
    }

    @FXML
    public void onSemanaHorarioSeguinteClick() {
        semanaHorarioPublicadoInicio = semanaHorarioPublicadoInicio.plusWeeks(1);
        atualizarCabecalhoSemanaHorarioPublicado();
        carregarHorarioPublicado();
    }

    private void configurarTabelaEquipaHoje() {
        colColaboradorHoje.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getIdLojautilizador().getIdUtilizador().getNome()));

        colHorarioHoje.setCellValueFactory(cellData -> {
            Horario horario = cellData.getValue();
            return new SimpleStringProperty(formatarPeriodo(horario));
        });

        colEstadoHoje.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatarEstado(cellData.getValue().getEstado() != null ? cellData.getValue().getEstado().name() : null)));

        tabelaEquipaHoje.setPlaceholder(new Label("Não há equipa escalada para hoje na tua loja."));
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

        tabelaOperacaoLoja.setPlaceholder(new Label("Não existe equipa escalada para o período selecionado."));
        tabelaOperacaoLoja.getSelectionModel().selectedItemProperty().addListener((obs, antigo, novo) -> {
            if (novo != null && cbColaboradorOperacao != null) {
                cbColaboradorOperacao.getItems().stream()
                        .filter(item -> Objects.equals(novo.idUtilizador(), item.idUtilizador()))
                        .findFirst()
                        .ifPresent(cbColaboradorOperacao::setValue);
            }
        });
    }

    private void configurarPainelHorarioPublicado() {
        semanaHorarioPublicadoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        atualizarCabecalhoSemanaHorarioPublicado();
        renderizarCalendarioHorarioPublicado(semanaHorarioPublicadoInicio, List.of());

        if (cbSemanaHorarioPublicado != null) {
            atualizarOpcoesSemanaReferencia();
            cbSemanaHorarioPublicado.valueProperty().addListener((obs, antiga, nova) -> {
                if (aAtualizarSeletorSemana || nova == null || Objects.equals(semanaHorarioPublicadoInicio, nova.inicio())) {
                    return;
                }

                semanaHorarioPublicadoInicio = nova.inicio();
                atualizarCabecalhoSemanaHorarioPublicado();
                carregarHorarioPublicado();
            });
        }
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
        renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());

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
        cbMesHorarioMensal.setItems(FXCollections.observableArrayList(MesOption.todos()));
        cbMesHorarioMensal.setValue(cbMesHorarioMensal.getItems().stream()
                .filter(item -> item.numero() == LocalDate.now().getMonthValue())
                .findFirst()
                .orElse(null));
        cbColaboradorHorarioMensal.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
        cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos());

        spAnoHorarioMensal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - 1,
                LocalDate.now().getYear() + 3,
                LocalDate.now().getYear()
        ));
        spAnoHorarioMensal.setEditable(true);

        atualizarToggleVistaHome();
        renderizarCalendarioMensalLoja(YearMonth.now(), List.of());

        cbMesHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) {
                carregarHorarioMensalLoja();
            }
        });
        cbColaboradorHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) {
                carregarHorarioMensalLoja();
            }
        });
        spAnoHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) {
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
            atualizarCabecalhoSemanaHorarioPublicado();

            List<Horario> turnos = horarioBll.listarHorarioPublicadoDoUtilizador(utilizadorLogado.getId(), dataInicio, dataFim);
            renderizarCalendarioHorarioPublicado(dataInicio, turnos);

            if (turnos.isEmpty()) {
                lblResumoHorarioPublicado.setText(
                        "Não existe nenhum turno publicado para ti entre "
                                + formatarData(dataInicio)
                                + " e "
                                + formatarData(dataFim)
                                + "."
                );
                atualizarProximoTurno(List.of());
                return;
            }

            Horario proximoTurno = turnos.getFirst();
            lblResumoHorarioPublicado.setText(
                    turnos.size()
                            + " turno(s) publicados entre "
                            + formatarData(dataInicio)
                            + " e "
                            + formatarData(dataFim)
                            + ". Próximo turno: "
                            + formatarData(proximoTurno.getDataTurno())
                            + " | "
                            + formatarPeriodo(proximoTurno)
                            + "."
            );
            atualizarProximoTurno(turnos);
        } catch (Exception e) {
            lblResumoHorarioPublicado.setText("Não foi possível carregar o horário publicado. Tenta novamente dentro de instantes.");
            renderizarCalendarioHorarioPublicado(
                    semanaHorarioPublicadoInicio != null ? semanaHorarioPublicadoInicio : CalendarioSemanalHelper.inicioSemana(LocalDate.now()),
                    List.of()
            );
            atualizarProximoTurno(List.of());
        }
    }

    private void atualizarProximoTurno(List<Horario> turnos) {
        if (lblProximoTurnoInfo == null) return;

        // Só relevante para colaboradores (gestores vêem o painel de operação)
        boolean eGestor = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());
        if (eGestor) {
            lblProximoTurnoInfo.setText("Como gestor, tens acesso ao painel de operação da loja com vista completa da equipa.");
            return;
        }

        // Filtrar turnos a partir de hoje
        LocalDate hoje = LocalDate.now();
        List<Horario> futuros = turnos.stream()
                .filter(h -> h.getDataTurno() != null && !h.getDataTurno().isBefore(hoje))
                .limit(3)
                .toList();

        if (futuros.isEmpty()) {
            lblProximoTurnoInfo.setText("Não tens turnos publicados nesta semana. Verifica semanas futuras no calendário.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Horario turno : futuros) {
            LocalDate data = turno.getDataTurno();
            String diaLabel;
            if (data.equals(hoje)) {
                diaLabel = "Hoje";
            } else if (data.equals(hoje.plusDays(1))) {
                diaLabel = "Amanhã";
            } else {
                diaLabel = data.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_PT);
                diaLabel = diaLabel.substring(0, 1).toUpperCase() + diaLabel.substring(1).toLowerCase(LOCALE_PT);
                diaLabel += " (" + data.format(DateTimeFormatter.ofPattern("dd/MM")) + ")";
            }
            sb.append("• ").append(diaLabel).append("  ").append(formatarPeriodo(turno)).append("\n");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // remover último \n
        }

        lblProximoTurnoInfo.setText(sb.toString());
    }

    private void carregarEquipaHoje() {
        if (utilizadorLogado == null) {
            return;
        }

        try {
            todosEquipaHoje = horarioBll.listarEquipaDeHoje(utilizadorLogado.getId());
        } catch (Exception e) {
            todosEquipaHoje = List.of();
        }
        aplicarFiltroPill();
        atualizarChipEscala();
    }

    private void atualizarChipEscala() {
        if (lblChipEscala == null) return;
        if (todosEquipaHoje.isEmpty()) {
            lblChipEscala.setText("Sem escala hoje");
            lblChipEscala.getStyleClass().removeAll("home-chip-ok", "home-chip-warn");
            if (!lblChipEscala.getStyleClass().contains("home-chip-warn")) {
                lblChipEscala.getStyleClass().add("home-chip-warn");
            }
        } else {
            lblChipEscala.setText(todosEquipaHoje.size() + " escalado(s)");
            lblChipEscala.getStyleClass().removeAll("home-chip-ok", "home-chip-warn");
            if (!lblChipEscala.getStyleClass().contains("home-chip-ok")) {
                lblChipEscala.getStyleClass().add("home-chip-ok");
            }
        }
    }

    private void configurarVisibilidadePainelOperacao() {
        boolean podeGerirLoja = utilizadorLogado != null && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());

        painelOperacaoLoja.setManaged(podeGerirLoja);
        painelOperacaoLoja.setVisible(podeGerirLoja);
        secaoGrelhaEquipaMensal.setManaged(podeGerirLoja);
        secaoGrelhaEquipaMensal.setVisible(podeGerirLoja);
        painelPedidosPendentes.setManaged(podeGerirLoja);
        painelPedidosPendentes.setVisible(podeGerirLoja);
        painelAcoesGestao.setManaged(podeGerirLoja);
        painelAcoesGestao.setVisible(podeGerirLoja);
        painelMetricasLoja.setManaged(podeGerirLoja);
        painelMetricasLoja.setVisible(podeGerirLoja);

        // Botão "Aprovar Horário" só para gestores
        if (btnAprovarHorarioHome != null) {
            btnAprovarHorarioHome.setVisible(podeGerirLoja);
            btnAprovarHorarioHome.setManaged(podeGerirLoja);
        }

        if (podeGerirLoja) {
            carregarColaboradoresParaComboBox(cbColaboradorOperacao);
            carregarColaboradoresParaComboBox(cbColaboradorHorarioMensal);
            carregarOperacaoLoja();
            carregarHorarioMensalLoja();
            pedidosHelper.carregarPedidosPendentes(utilizadorLogado);
            pedidosHelper.atualizarBannerPendentes(utilizadorLogado);
        } else {
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
            lblResumoHorarioMensal.setText("A vista mensal da loja está disponível apenas para perfis de gestão.");
            esconderBannerPendentes();
        }
    }

    private void atualizarBannerPendentes() {
        if (pedidosHelper != null) pedidosHelper.atualizarBannerPendentes(utilizadorLogado);
    }

    private void esconderBannerPendentes() {
        if (pedidosHelper != null) pedidosHelper.esconderBannerPendentes();
    }

    private void carregarOperacaoLoja() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            LocalDate dataInicio = dpDataOperacao.getValue() != null ? dpDataOperacao.getValue() : LocalDate.now();
            LocalDate dataFim = resolverDataFimOperacao(dataInicio);

            SnapshotOperacionalLojaService.SnapshotOperacionalLoja snapshot = snapshotOperacionalLojaBLL.carregarSnapshot(
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
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            lblResumoEscalaPublicada.setText("Escala publicada indisponível para o período atual.");
        } catch (Exception e) {
            mostrarFeedbackOperacao("Não foi possível carregar o painel diário da loja.");
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            lblResumoEscalaPublicada.setText("Escala publicada indisponível para o período atual.");
        }
    }

    private void carregarColaboradoresParaComboBox(ComboBox<ColaboradorFiltroOption> comboBox) {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null || comboBox == null) return;
        try {
            ColaboradorFiltroOption anterior = comboBox.getValue();
            List<ColaboradorFiltroOption> opcoes = horarioBll
                    .listarColaboradoresAtivosDaLojaDoUtilizador(utilizadorLogado.getId()).stream()
                    .sorted(Comparator.comparing(HorarioService.ColaboradorLoja::nome,
                            String.CASE_INSENSITIVE_ORDER))
                    .map(c -> new ColaboradorFiltroOption(c.idUtilizador(), c.etiqueta()))
                    .toList();

            comboBox.setItems(FXCollections.observableArrayList());
            comboBox.getItems().add(ColaboradorFiltroOption.todos());
            comboBox.getItems().addAll(opcoes);

            if (anterior != null) {
                comboBox.getItems().stream()
                        .filter(item -> Objects.equals(item.idUtilizador(), anterior.idUtilizador()))
                        .findFirst()
                        .ifPresentOrElse(comboBox::setValue,
                                () -> comboBox.setValue(ColaboradorFiltroOption.todos()));
            } else {
                comboBox.setValue(ColaboradorFiltroOption.todos());
            }
        } catch (Exception e) {
            comboBox.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
            comboBox.setValue(ColaboradorFiltroOption.todos());
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

            if (vistaGrelhaEscalaAtiva) {
                renderizarGrelhaEscalaLoja(dataInicio, dataFim, horarios);
            } else {
                renderizarCalendarioEscalaLoja(dataInicio, horarios);
            }

            String etiquetaColaborador = cbColaboradorOperacao.getValue() != null
                    ? cbColaboradorOperacao.getValue().label()
                    : "toda a equipa";

            if (horarios.isEmpty()) {
                lblResumoEscalaPublicada.setText(
                        "Não há horários publicados para "
                                + etiquetaColaborador.toLowerCase(Locale.ROOT)
                                + " no período selecionado."
                );
                return;
            }

            lblResumoEscalaPublicada.setText(
                    horarios.size()
                            + " turno(s) publicados para "
                            + etiquetaColaborador
                            + " entre "
                            + formatarData(dataInicio)
                            + " e "
                            + formatarData(dataFim)
                            + "."
            );
        } catch (Exception e) {
            if (vistaGrelhaEscalaAtiva) {
                renderizarGrelhaEscalaLoja(LocalDate.now(), LocalDate.now().plusDays(6), List.of());
            } else {
                renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            }
            lblResumoEscalaPublicada.setText("Não foi possível carregar a escala publicada da loja neste momento.");
        }
    }

    private void renderizarGrelhaEscalaLoja(LocalDate dataInicio, LocalDate dataFim, List<Horario> horarios) {
        if (boxGrelhaEscalaLoja == null) return;
        GrelhaHorarioHelper.preencherSemanal(
                boxGrelhaEscalaLoja, dataInicio, dataFim,
                horarios, LocalDate.now(),
                data -> DetalheDiaDialog.abrirHorariosPublicados(data, horarios, obterJanela()));
    }

    private void carregarHorarioMensalLoja() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            MesOption mesSelecionado = cbMesHorarioMensal.getValue();
            Integer anoSelecionado = spAnoHorarioMensal.getValue();
            if (mesSelecionado == null || anoSelecionado == null) {
                lblResumoHorarioMensal.setText("Seleciona um mês e um ano para veres o horário mensal da loja.");
                renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
                return;
            }

            YearMonth periodo = YearMonth.of(anoSelecionado, mesSelecionado.numero());
            Integer idColaborador = cbColaboradorHorarioMensal.getValue() != null
                    ? cbColaboradorHorarioMensal.getValue().idUtilizador()
                    : null;
            String etiquetaColaborador = cbColaboradorHorarioMensal.getValue() != null
                    ? cbColaboradorHorarioMensal.getValue().label()
                    : "Toda a equipa";

            List<Horario> horarios = horarioBll.listarHorarioPublicadoDaLojaDoUtilizador(
                    utilizadorLogado.getId(),
                    periodo.atDay(1),
                    periodo.atEndOfMonth(),
                    idColaborador
            );

            renderizarCalendarioMensalLoja(periodo, horarios);

            if (horarios.isEmpty()) {
                lblResumoHorarioMensal.setText(
                        "Ainda não existem horários publicados para "
                                + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                                + " de "
                                + anoSelecionado
                                + "."
                );
                return;
            }

            if (idColaborador != null) {
                lblResumoHorarioMensal.setText(
                        "Vista mensal filtrada para "
                                + etiquetaColaborador
                                + " em "
                                + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                                + " de "
                                + anoSelecionado
                                + ", com "
                                + horarios.size()
                                + " turno(s) publicados."
                );
                return;
            }

            long totalColaboradores = horarios.stream()
                    .map(Horario::getIdLojautilizador)
                    .filter(Objects::nonNull)
                    .map(relacao -> relacao.getIdUtilizador())
                    .filter(Objects::nonNull)
                    .map(Utilizador::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            lblResumoHorarioMensal.setText(
                    "Calendário mensal publicado da loja para "
                            + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                            + " de "
                            + anoSelecionado
                            + ", com "
                            + totalColaboradores
                            + " colaborador(es) e "
                            + horarios.size()
                            + " turno(s). Usa o filtro de colaborador para isolar uma escala individual."
            );
        } catch (Exception e) {
            renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
            lblResumoHorarioMensal.setText("Não foi possível carregar o horário mensal da loja neste momento.");
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

    private String construirResumoOperacional(SnapshotOperacionalLojaService.SnapshotOperacionalLoja snapshot) {
        String periodo = snapshot.intervalo().unicoDia()
                ? "para " + formatarData(snapshot.intervalo().dataInicio())
                : "de " + formatarData(snapshot.intervalo().dataInicio()) + " a " + formatarData(snapshot.intervalo().dataFim());

        String alertas = snapshot.resumo().totalPedidosPendentes() > 0
                ? snapshot.resumo().totalPedidosPendentes() + " pedido(s) pendente(s) precisam de acompanhamento."
                : "Sem pedidos pendentes no período selecionado.";

        return "Operação da loja " + snapshot.contexto().nomeLoja() + " " + periodo + ". " + alertas;
    }

    private String formatarTurnosOperacao(List<SnapshotOperacionalLojaService.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "Sem turnos no período";
        }

        return turnos.stream()
                .map(turno -> formatarData(turno.data()) + " | " + turno.periodo())
                .collect(Collectors.joining("; "));
    }

    private String formatarEstadosOperacao(List<SnapshotOperacionalLojaService.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "-";
        }

        return turnos.stream()
                .map(SnapshotOperacionalLojaService.TurnoPlaneado::estado)
                .filter(estado -> estado != null && !estado.isBlank())
                .map(this::formatarEstado)
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

    // ── Saudação e data ─────────────────────────────────────────────────────

    private String construirSaudacao(String nome) {
        String primeiroNome = nome != null && !nome.isBlank()
                ? nome.trim().split("\\s+")[0]
                : "Equipa";

        int hora = java.time.LocalTime.now().getHour();
        String cumprimento;
        if (hora >= 6 && hora < 13) {
            cumprimento = "Bom dia";
        } else if (hora >= 13 && hora < 20) {
            cumprimento = "Boa tarde";
        } else {
            cumprimento = "Boa noite";
        }
        return cumprimento + ", " + primeiroNome + ".";
    }

    private void atualizarDataHoje() {
        if (lblDataHoje == null) return;
        LocalDate hoje = LocalDate.now();
        String diaSemana = hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_PT);
        String dataFormatada = hoje.getDayOfMonth()
                + " " + hoje.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_PT).toLowerCase(LOCALE_PT);
        // Capitalizar o dia da semana
        String diaCapitalizado = diaSemana.substring(0, 1).toUpperCase() + diaSemana.substring(1).toLowerCase(LOCALE_PT);
        lblDataHoje.setText(diaCapitalizado + ", " + dataFormatada);
    }

    // ────────────────────────────────────────────────────────────────────────

    // ────────────────────────────────────────────────────────────────────────

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

    private String formatarPeriodo(Horario horario) {
        if (horario == null || horario.getIdTurno() == null) {
            return "-";
        }

        String inicio = horario.getIdTurno().getHoraInicio() != null
                ? horario.getIdTurno().getHoraInicio().format(HORARIO_COMPACTO)
                : "--:--";
        String fim = horario.getIdTurno().getHoraFim() != null
                ? horario.getIdTurno().getHoraFim().format(HORARIO_COMPACTO)
                : "--:--";

        return inicio + " - " + fim;
    }

    private void atualizarCabecalhoSemanaHorarioPublicado() {
        if (lblSemanaHorarioPublicadoIntervalo != null && semanaHorarioPublicadoInicio != null) {
            lblSemanaHorarioPublicadoIntervalo.setText(CalendarioSemanalHelper.formatarIntervaloSemana(semanaHorarioPublicadoInicio));
        }

        atualizarOpcoesSemanaReferencia();
    }

    private void atualizarOpcoesSemanaReferencia() {
        if (cbSemanaHorarioPublicado == null || semanaHorarioPublicadoInicio == null) {
            return;
        }

        aAtualizarSeletorSemana = true;
        try {
            List<SemanaOption> semanas = java.util.stream.IntStream.rangeClosed(-12, 24)
                    .mapToObj(indice -> new SemanaOption(semanaHorarioPublicadoInicio.plusWeeks(indice)))
                    .toList();

            cbSemanaHorarioPublicado.setItems(FXCollections.observableArrayList(semanas));
            cbSemanaHorarioPublicado.getSelectionModel().select(
                    semanas.stream()
                            .filter(semana -> Objects.equals(semana.inicio(), semanaHorarioPublicadoInicio))
                            .findFirst()
                            .orElseGet(() -> new SemanaOption(semanaHorarioPublicadoInicio))
            );
        } finally {
            aAtualizarSeletorSemana = false;
        }
    }

    private String abreviarDia(LocalDate data) {
        return data.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, LOCALE_PT)
                .replace(".", "")
                .toUpperCase(LOCALE_PT);
    }

    private void renderizarCalendarioHorarioPublicado(LocalDate inicioSemana, List<Horario> horarios) {
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();

        for (Horario horario : horarios) {
            String evento = formatarPeriodo(horario)
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

    private void renderizarCalendarioEscalaLoja(LocalDate dataBase, List<Horario> horarios) {
        LocalDate inicioSemana = CalendarioSemanalHelper.inicioSemana(dataBase != null ? dataBase : LocalDate.now());
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();

        for (Horario horario : horarios) {
            String cargo = horario.getIdLojautilizador().getIdCargo() != null
                    ? horario.getIdLojautilizador().getIdCargo().getNome()
                    : "-";

            String evento = formatarPeriodo(horario)
                    + " | "
                    + horario.getIdLojautilizador().getIdUtilizador().getNome()
                    + " (" + cargo + ")";

            eventosPorDia.computeIfAbsent(horario.getDataTurno(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxEscalaDetalhadaLoja,
                inicioSemana,
                eventosPorDia,
                "Sem turnos publicados"
        );
    }

    private void renderizarCalendarioMensalLoja(YearMonth periodo, List<Horario> horarios) {
        horariosMensaisAtuais = horarios != null ? horarios : List.of();
        if (vistaGrelhaAtiva) {
            GrelhaHorarioHelper.preencher(boxGrelhaEquipaMensal, periodo, horarios, LocalDate.now(),
                    this::abrirDetalheDiaMensal);
        } else {
            Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();
            if (horarios != null) {
                for (Horario h : horarios) {
                    if (h == null || h.getDataTurno() == null) continue;
                    String nome = h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdUtilizador() != null
                            ? h.getIdLojautilizador().getIdUtilizador().getNome() : "?";
                    String cargo = h.getIdLojautilizador() != null && h.getIdLojautilizador().getIdCargo() != null
                            && h.getIdLojautilizador().getIdCargo().getNome() != null
                            ? h.getIdLojautilizador().getIdCargo().getNome() : "-";
                    eventosPorDia.computeIfAbsent(h.getDataTurno(), k -> new java.util.ArrayList<>())
                            .add(formatarPeriodo(h) + " | " + nome + " (" + cargo + ")");
                }
            }
            CalendarioMensalHelper.preencherCalendario(calendarioMensalHome, periodo, eventosPorDia,
                    "Sem horários publicados para o período selecionado.",
                    this::abrirDetalheDiaMensal);
        }
    }

    /** Abre o diálogo "Detalhe do dia" com os turnos publicados da loja nesse dia. */
    private void abrirDetalheDiaMensal(LocalDate data) {
        DetalheDiaDialog.abrirHorariosPublicados(data, horariosMensaisAtuais, obterJanela());
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

    private record SemanaOption(LocalDate inicio) {
        @Override
        public String toString() {
            return CalendarioSemanalHelper.formatarIntervaloSemana(inicio);
        }
    }
}
