package com.example.projeto2.Controller;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.Controller.support.CalendarioMensalHelper;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Enums.EstadoHorario;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
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
    private HBox boxEscalaDetalhadaLoja;

    @FXML
    private ComboBox<MesOption> cbMesHorarioMensal;

    @FXML
    private ComboBox<ColaboradorFiltroOption> cbColaboradorHorarioMensal;

    @FXML
    private Spinner<Integer> spAnoHorarioMensal;

    @FXML
    private Label lblResumoHorarioMensal;

    @FXML
    private GridPane gridHorarioMensalLoja;

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

    private final HorarioBLL horarioBll;
    private final SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;
    private final GestaoLojaBLL gestaoLojaBLL;
    private final DayOffBLL dayOffBLL;
    private final PermutaBLL permutaBLL;
    private Utilizador utilizadorLogado;
    private DashboardNavigator dashboardNavigation;
    private LocalDate semanaHorarioPublicadoInicio;
    private boolean aAtualizarSeletorSemana;
    private List<Horario> todosEquipaHoje = List.of();
    private String filtroTipoAtivo = null;

    public HomeController(HorarioBLL horarioBll,
                          SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL,
                          GestaoLojaBLL gestaoLojaBLL,
                          DayOffBLL dayOffBLL,
                          PermutaBLL permutaBLL) {
        this.horarioBll = horarioBll;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
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

        carregarHorarioPublicado();
        carregarEquipaHoje();
        configurarVisibilidadePainelOperacao();
        carregarMeusPedidos();
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
        cbMesHorarioMensal.setItems(FXCollections.observableArrayList(
                new MesOption(1, "Janeiro"),
                new MesOption(2, "Fevereiro"),
                new MesOption(3, "Março"),
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
        cbColaboradorHorarioMensal.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
        cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos());

        spAnoHorarioMensal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - 1,
                LocalDate.now().getYear() + 3,
                LocalDate.now().getYear()
        ));
        spAnoHorarioMensal.setEditable(true);

        renderizarCalendarioMensalLoja(YearMonth.now(), List.of());

        cbMesHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
            if (painelOperacaoLoja.isVisible()) {
                carregarHorarioMensalLoja();
            }
        });
        cbColaboradorHorarioMensal.valueProperty().addListener((obs, antigo, novo) -> {
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
            carregarColaboradoresOperacao();
            carregarColaboradoresHorarioMensal();
            carregarOperacaoLoja();
            carregarHorarioMensalLoja();
            atualizarBannerPendentes();
        } else {
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
            lblResumoHorarioMensal.setText("A vista mensal da loja está disponível apenas para perfis de gestão.");
            esconderBannerPendentes();
        }
    }

    private void atualizarBannerPendentes() {
        if (bannerPendentes == null || utilizadorLogado == null) return;
        try {
            int folgas = dayOffBLL.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId()).size();
            int permutas = permutaBLL.listarPedidosPendentesParaAprovacao(utilizadorLogado.getId()).size();
            int total = folgas + permutas;
            if (total > 0) {
                String msg = total == 1
                        ? "Tens 1 pedido pendente a aguardar a tua aprovação."
                        : "Tens " + total + " pedidos pendentes a aguardar a tua aprovação.";
                if (lblBannerPendentes != null) lblBannerPendentes.setText(msg);
                bannerPendentes.setVisible(true);
                bannerPendentes.setManaged(true);
            } else {
                esconderBannerPendentes();
            }
        } catch (Exception e) {
            esconderBannerPendentes();
        }
    }

    private void esconderBannerPendentes() {
        if (bannerPendentes == null) return;
        bannerPendentes.setVisible(false);
        bannerPendentes.setManaged(false);
    }

    private void carregarOperacaoLoja() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
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
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            lblResumoEscalaPublicada.setText("Escala publicada indisponível para o período atual.");
        } catch (Exception e) {
            mostrarFeedbackOperacao("Não foi possível carregar o painel diário da loja.");
            tabelaOperacaoLoja.setItems(FXCollections.observableArrayList());
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            lblResumoEscalaPublicada.setText("Escala publicada indisponível para o período atual.");
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

    private void carregarColaboradoresHorarioMensal() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }

        try {
            ColaboradorFiltroOption anterior = cbColaboradorHorarioMensal.getValue();
            List<ColaboradorFiltroOption> opcoes = horarioBll.listarColaboradoresAtivosDaLojaDoUtilizador(utilizadorLogado.getId()).stream()
                    .sorted(Comparator.comparing(HorarioBLL.ColaboradorLoja::nome, String.CASE_INSENSITIVE_ORDER))
                    .map(colaborador -> new ColaboradorFiltroOption(
                            colaborador.idUtilizador(),
                            colaborador.etiqueta()
                    ))
                    .toList();

            cbColaboradorHorarioMensal.setItems(FXCollections.observableArrayList());
            cbColaboradorHorarioMensal.getItems().add(ColaboradorFiltroOption.todos());
            cbColaboradorHorarioMensal.getItems().addAll(opcoes);

            if (anterior != null) {
                cbColaboradorHorarioMensal.getItems().stream()
                        .filter(item -> Objects.equals(item.idUtilizador(), anterior.idUtilizador()))
                        .findFirst()
                        .ifPresentOrElse(
                                cbColaboradorHorarioMensal::setValue,
                                () -> cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos())
                        );
            } else {
                cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos());
            }
        } catch (Exception e) {
            cbColaboradorHorarioMensal.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
            cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos());
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

            renderizarCalendarioEscalaLoja(dataInicio, horarios);

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
            renderizarCalendarioEscalaLoja(LocalDate.now(), List.of());
            lblResumoEscalaPublicada.setText("Não foi possível carregar a escala publicada da loja neste momento.");
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

    private String construirResumoOperacional(SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja snapshot) {
        String periodo = snapshot.intervalo().unicoDia()
                ? "para " + formatarData(snapshot.intervalo().dataInicio())
                : "de " + formatarData(snapshot.intervalo().dataInicio()) + " a " + formatarData(snapshot.intervalo().dataFim());

        String alertas = snapshot.resumo().totalPedidosPendentes() > 0
                ? snapshot.resumo().totalPedidosPendentes() + " pedido(s) pendente(s) precisam de acompanhamento."
                : "Sem pedidos pendentes no período selecionado.";

        return "Operação da loja " + snapshot.contexto().nomeLoja() + " " + periodo + ". " + alertas;
    }

    private String formatarTurnosOperacao(List<SnapshotOperacionalLojaBLL.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) {
            return "Sem turnos no período";
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

    // ── Os Meus Pedidos ─────────────────────────────────────────────────────

    private void carregarMeusPedidos() {
        if (painelMeusPedidos == null || listaMeusPedidos == null) {
            return;
        }

        if (utilizadorLogado == null) {
            painelMeusPedidos.setVisible(false);
            painelMeusPedidos.setManaged(false);
            return;
        }

        // Painel escondido para gestores (têm o painel de operação)
        boolean eGestor = gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());
        if (eGestor) {
            painelMeusPedidos.setVisible(false);
            painelMeusPedidos.setManaged(false);
            return;
        }

        try {
            // Combinar folgas e permutas numa lista unificada de eventos recentes
            List<PedidoResumo> pedidos = new ArrayList<>();

            dayOffBLL.listarPedidosPorUtilizador(utilizadorLogado.getId()).stream()
                    .limit(10)
                    .forEach(dayOff -> pedidos.add(new PedidoResumo(
                            "Folga / " + formatarTipoDayOff(dayOff.getTipo()),
                            dayOff.getDataAusencia() != null ? DATA_FORMATTER.format(dayOff.getDataAusencia()) : "-",
                            dayOff.getEstado() != null ? dayOff.getEstado() : "pendente",
                            dayOff.getDataAusencia() != null ? dayOff.getDataAusencia().atStartOfDay().toInstant(java.time.ZoneOffset.UTC) : Instant.MIN
                    )));

            permutaBLL.listarPedidosEnviados(utilizadorLogado.getId()).stream()
                    .limit(10)
                    .forEach(permuta -> {
                        String dataFormatada = permuta.getIdHorarioOrigem() != null
                                && permuta.getIdHorarioOrigem().getDataTurno() != null
                                ? DATA_FORMATTER.format(permuta.getIdHorarioOrigem().getDataTurno())
                                : "-";
                        String estado = permuta.getEstado() != null ? permuta.getEstado().name() : "pendente";
                        Instant dataOrdem = permuta.getDataPedido() != null ? permuta.getDataPedido() : Instant.MIN;
                        pedidos.add(new PedidoResumo("Troca de turno", dataFormatada, estado, dataOrdem));
                    });

            // Ordenar por data descendente (mais recentes primeiro), limitar a 5
            List<PedidoResumo> recentes = pedidos.stream()
                    .sorted(Comparator.comparing(PedidoResumo::dataOrdem, Comparator.reverseOrder()))
                    .limit(5)
                    .toList();

            listaMeusPedidos.getChildren().clear();

            if (recentes.isEmpty()) {
                Label lblVazio = new Label("Ainda não tens pedidos registados. Usa os atalhos acima para pedir folga ou trocar turno.");
                lblVazio.getStyleClass().add("home-card-subtitle");
                lblVazio.setWrapText(true);
                listaMeusPedidos.getChildren().add(lblVazio);
            } else {
                for (PedidoResumo pedido : recentes) {
                    listaMeusPedidos.getChildren().add(criarLinhaPedido(pedido));
                }
            }

            painelMeusPedidos.setVisible(true);
            painelMeusPedidos.setManaged(true);

        } catch (Exception e) {
            painelMeusPedidos.setVisible(false);
            painelMeusPedidos.setManaged(false);
        }
    }

    private HBox criarLinhaPedido(PedidoResumo pedido) {
        HBox linha = new HBox(12);
        linha.setAlignment(Pos.CENTER_LEFT);
        linha.getStyleClass().add("pedido-resumo-linha");
        linha.setPadding(new Insets(8, 12, 8, 12));

        Label lblTipo = new Label(pedido.tipo());
        lblTipo.getStyleClass().add("pedido-resumo-tipo");
        HBox.setHgrow(lblTipo, Priority.ALWAYS);
        lblTipo.setMaxWidth(Double.MAX_VALUE);

        Label lblData = new Label(pedido.data());
        lblData.getStyleClass().add("pedido-resumo-data");

        Label lblEstado = new Label(formatarEstadoPedido(pedido.estado()));
        lblEstado.getStyleClass().addAll("pedido-resumo-badge", resolverCssBadge(pedido.estado()));

        linha.getChildren().addAll(lblTipo, lblData, lblEstado);
        return linha;
    }

    private String formatarTipoDayOff(String tipo) {
        if (tipo == null) return "Ausência";
        return switch (tipo.toLowerCase(Locale.ROOT)) {
            case "ferias" -> "Férias";
            case "folgas" -> "Folgas";
            case "baixa" -> "Baixa";
            case "urgente" -> "Urgente";
            default -> capitalizar(tipo);
        };
    }

    private String formatarEstadoPedido(String estado) {
        if (estado == null) return "Pendente";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "pendente" -> "Pendente";
            case "aprovado" -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            case "recusado" -> "Recusado";
            default -> capitalizar(estado);
        };
    }

    private String resolverCssBadge(String estado) {
        if (estado == null) return "badge-pendente";
        return switch (estado.toLowerCase(Locale.ROOT)) {
            case "aprovado" -> "badge-aprovado";
            case "rejeitado", "recusado" -> "badge-rejeitado";
            default -> "badge-pendente";
        };
    }

    private record PedidoResumo(String tipo, String data, String estado, Instant dataOrdem) {}

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

        CalendarioMensalHelper.preencherCalendario(
                gridHorarioMensalLoja,
                periodo,
                eventosPorDia,
                "Sem turnos"
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

    private record SemanaOption(LocalDate inicio) {
        @Override
        public String toString() {
            return CalendarioSemanalHelper.formatarIntervaloSemana(inicio);
        }
    }
}
