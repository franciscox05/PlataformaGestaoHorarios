package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.SessaoService;
import com.example.projeto2.DESKTOP.support.CalendarioMensalHelper;
import com.example.projeto2.DESKTOP.support.CalendarioSemanalHelper;
import com.example.projeto2.DESKTOP.support.DetalheDiaDialog;
import com.example.projeto2.DESKTOP.support.GrelhaHorarioHelper;
import com.example.projeto2.DESKTOP.support.HomePedidosHelper;
import com.example.projeto2.DESKTOP.support.MesOption;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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

@Component
@Scope("prototype")
public class HomeController {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORARIO_COMPACTO = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale LOCALE_PT = Locale.forLanguageTag("pt-PT");

    @FXML private Label lblBemVindo;
    @FXML private Label lblDataHoje;
    @FXML private Label lblResumoHorarioPublicado;

    @FXML private HBox bannerPendentes;
    @FXML private Label lblBannerPendentes;

    @FXML private Label lblSemanaHorarioPublicadoIntervalo;
    @FXML private ComboBox<SemanaOption> cbSemanaHorarioPublicado;
    @FXML private HBox boxSemanaHorarioPublicado;

    @FXML private VBox secaoGrelhaEquipaMensal;
    @FXML private Button btnVistaCalendarioHome;
    @FXML private Button btnVistaGrelhaHome;
    @FXML private ComboBox<MesOption> cbMesHorarioMensal;
    @FXML private Spinner<Integer> spAnoHorarioMensal;
    @FXML private ComboBox<ColaboradorFiltroOption> cbColaboradorHorarioMensal;
    @FXML private Label lblResumoHorarioMensal;
    @FXML private VBox painelVistaCalendarioHome;
    @FXML private GridPane calendarioMensalHome;
    @FXML private ScrollPane scrollGrelhaEquipaMensal;
    @FXML private VBox boxGrelhaEquipaMensal;

    @FXML private VBox painelMeusPedidos;
    @FXML private VBox listaMeusPedidos;

    private boolean vistaGrelhaAtiva = false;

    private final HorarioService horarioBll;
    private final GestaoLojaService gestaoLojaBLL;
    private final DayOffService dayOffBLL;
    private final PermutaService permutaBLL;
    private final SessaoService sessaoBLL;

    private Utilizador utilizadorLogado;
    private DashboardNavigator dashboardNavigation;
    private HomePedidosHelper pedidosHelper;
    private LocalDate semanaHorarioPublicadoInicio;
    private boolean aAtualizarSeletorSemana;
    private List<Horario> horariosMensaisAtuais = List.of();

    public HomeController(HorarioService horarioBll,
                          GestaoLojaService gestaoLojaBLL,
                          DayOffService dayOffBLL,
                          PermutaService permutaBLL,
                          SessaoService sessaoBLL) {
        this.horarioBll = horarioBll;
        this.gestaoLojaBLL = gestaoLojaBLL;
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.sessaoBLL = sessaoBLL;
    }

    /**
     * Resolve a loja activa da sessão aplicando a guarda de segurança partilhada
     * (ver Revisao.md, pontos 17–18): {@code idLoja <= 0} ou {@code null} nunca é
     * uma loja válida (serial do Postgres começa em 1; o Mockito devolve 0 para
     * Integer não esboçado). Devolve {@code null} quando não há loja activa válida,
     * sinalizando às vistas store-scoped que devem mostrar-se vazias em vez de
     * vazarem dados de outra loja.
     */
    private Integer obterLojaAtivaSegura() {
        Integer idLojaAtiva = sessaoBLL.obterLojaAtiva();
        if (idLojaAtiva == null || idLojaAtiva <= 0) {
            return null;
        }
        return idLojaAtiva;
    }

    @FXML
    public void initialize() {
        configurarPainelHorarioPublicado();
        configurarPainelHorarioMensal();
    }

    public void setUtilizadorLogado(Utilizador utilizador) {
        if (utilizador == null) return;

        this.utilizadorLogado = utilizador;
        lblBemVindo.setText(construirSaudacao(utilizador.getNome()));
        atualizarDataHoje();

        pedidosHelper = new HomePedidosHelper(
                bannerPendentes, lblBannerPendentes,
                painelMeusPedidos, listaMeusPedidos,
                dayOffBLL, permutaBLL, gestaoLojaBLL,
                this::obterJanela);

        carregarHorarioPublicado();
        configurarVisibilidadePainelMensal();
        pedidosHelper.carregarMeusPedidos(utilizadorLogado);
    }

    public void setDashboardNavigation(DashboardNavigator dashboardNavigation) {
        this.dashboardNavigation = dashboardNavigation;
    }

    @FXML
    public void onAtalhoFolgaClick() {
        if (dashboardNavigation != null) dashboardNavigation.abrirFolgas();
    }

    @FXML
    public void onBannerVerPedidosClick() {
        if (dashboardNavigation != null) dashboardNavigation.abrirPainelGerente();
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

    // ── Configuração ─────────────────────────────────────────────────────────

    private void configurarPainelHorarioPublicado() {
        semanaHorarioPublicadoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        atualizarCabecalhoSemanaHorarioPublicado();
        renderizarCalendarioHorarioPublicado(semanaHorarioPublicadoInicio, List.of());

        if (cbSemanaHorarioPublicado != null) {
            atualizarOpcoesSemanaReferencia();
            cbSemanaHorarioPublicado.valueProperty().addListener((obs, antiga, nova) -> {
                if (aAtualizarSeletorSemana || nova == null
                        || Objects.equals(semanaHorarioPublicadoInicio, nova.inicio())) return;
                semanaHorarioPublicadoInicio = nova.inicio();
                atualizarCabecalhoSemanaHorarioPublicado();
                carregarHorarioPublicado();
            });
        }
    }

    private void configurarPainelHorarioMensal() {
        cbMesHorarioMensal.setItems(FXCollections.observableArrayList(MesOption.todos()));
        cbMesHorarioMensal.setValue(cbMesHorarioMensal.getItems().stream()
                .filter(item -> item.numero() == LocalDate.now().getMonthValue())
                .findFirst().orElse(null));
        cbColaboradorHorarioMensal.setItems(FXCollections.observableArrayList(ColaboradorFiltroOption.todos()));
        cbColaboradorHorarioMensal.setValue(ColaboradorFiltroOption.todos());

        spAnoHorarioMensal.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear() - 1, LocalDate.now().getYear() + 3, LocalDate.now().getYear()));
        spAnoHorarioMensal.setEditable(true);

        atualizarToggleVistaHome();
        renderizarCalendarioMensalLoja(YearMonth.now(), List.of());

        cbMesHorarioMensal.valueProperty().addListener((obs, ant, nov) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) carregarHorarioMensalLoja();
        });
        cbColaboradorHorarioMensal.valueProperty().addListener((obs, ant, nov) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) carregarHorarioMensalLoja();
        });
        spAnoHorarioMensal.valueProperty().addListener((obs, ant, nov) -> {
            if (secaoGrelhaEquipaMensal.isVisible()) carregarHorarioMensalLoja();
        });
    }

    // ── Visibilidade ────────────────────────────────────────────────────────

    private void configurarVisibilidadePainelMensal() {
        boolean podeGerirLoja = utilizadorLogado != null
                && gestaoLojaBLL.utilizadorPodeGerirLoja(utilizadorLogado.getId());

        secaoGrelhaEquipaMensal.setManaged(podeGerirLoja);
        secaoGrelhaEquipaMensal.setVisible(podeGerirLoja);

        if (podeGerirLoja) {
            carregarColaboradoresParaComboBox(cbColaboradorHorarioMensal);
            carregarHorarioMensalLoja();
            pedidosHelper.atualizarBannerPendentes(utilizadorLogado);
        } else {
            renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
            lblResumoHorarioMensal.setText(
                    "A vista mensal da loja está disponível apenas para perfis de gestão.");
            pedidosHelper.esconderBannerPendentes();
        }
    }

    // ── Carregamento de dados ────────────────────────────────────────────────

    private void carregarHorarioPublicado() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) return;

        try {
            LocalDate dataInicio = semanaHorarioPublicadoInicio != null
                    ? semanaHorarioPublicadoInicio
                    : CalendarioSemanalHelper.inicioSemana(LocalDate.now());
            LocalDate dataFim = dataInicio.plusDays(6);
            atualizarCabecalhoSemanaHorarioPublicado();

            List<Horario> turnos = horarioBll.listarHorarioPublicadoDoUtilizador(
                    utilizadorLogado.getId(), dataInicio, dataFim);
            renderizarCalendarioHorarioPublicado(dataInicio, turnos);

            if (turnos.isEmpty()) {
                lblResumoHorarioPublicado.setText(
                        "Não existe nenhum turno publicado para ti entre "
                                + formatarData(dataInicio) + " e " + formatarData(dataFim) + ".");
                return;
            }

            Horario proximoTurno = turnos.getFirst();
            lblResumoHorarioPublicado.setText(
                    turnos.size() + " turno(s) publicados entre "
                            + formatarData(dataInicio) + " e " + formatarData(dataFim)
                            + ". Próximo turno: " + formatarData(proximoTurno.getDataTurno())
                            + " | " + formatarPeriodo(proximoTurno) + ".");
        } catch (Exception e) {
            lblResumoHorarioPublicado.setText(
                    "Não foi possível carregar o horário publicado. Tenta novamente dentro de instantes.");
            renderizarCalendarioHorarioPublicado(
                    semanaHorarioPublicadoInicio != null
                            ? semanaHorarioPublicadoInicio
                            : CalendarioSemanalHelper.inicioSemana(LocalDate.now()),
                    List.of());
        }
    }

    private void carregarColaboradoresParaComboBox(ComboBox<ColaboradorFiltroOption> comboBox) {
        if (utilizadorLogado == null || comboBox == null) return;
        try {
            ColaboradorFiltroOption anterior = comboBox.getValue();
            // Mesmo isolamento da listagem de turnos: o filtro de colaborador só
            // lista colegas da loja activa da sessão.
            Integer idLojaAtiva = obterLojaAtivaSegura();
            List<ColaboradorFiltroOption> opcoes = (idLojaAtiva == null)
                    ? List.of()
                    : horarioBll
                    .listarColaboradoresAtivosDaLojaDoUtilizador(utilizadorLogado.getId(), idLojaAtiva).stream()
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

    private void carregarHorarioMensalLoja() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) return;

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
                    ? cbColaboradorHorarioMensal.getValue().idUtilizador() : null;
            String etiquetaColaborador = cbColaboradorHorarioMensal.getValue() != null
                    ? cbColaboradorHorarioMensal.getValue().label() : "Toda a equipa";

            // Isolamento estrito por loja activa: a Equipa só pode listar turnos da
            // loja onde o utilizador está logado em sessão. Sem loja activa válida,
            // mostra-se vazio (nunca a equipa de outra loja).
            Integer idLojaAtiva = obterLojaAtivaSegura();
            if (idLojaAtiva == null) {
                renderizarCalendarioMensalLoja(periodo, List.of());
                lblResumoHorarioMensal.setText(
                        "Sem loja activa na sessão. Entra numa loja para veres o horário da equipa.");
                return;
            }

            List<Horario> horarios = horarioBll.listarHorarioPublicadoDaLojaDoUtilizador(
                    utilizadorLogado.getId(), periodo.atDay(1), periodo.atEndOfMonth(), idColaborador, idLojaAtiva);

            renderizarCalendarioMensalLoja(periodo, horarios);

            if (horarios.isEmpty()) {
                lblResumoHorarioMensal.setText(
                        "Ainda não existem horários publicados para "
                                + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                                + " de " + anoSelecionado + ".");
                return;
            }

            if (idColaborador != null) {
                lblResumoHorarioMensal.setText(
                        "Vista mensal filtrada para " + etiquetaColaborador
                                + " em " + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                                + " de " + anoSelecionado
                                + ", com " + horarios.size() + " turno(s) publicados.");
                return;
            }

            long totalColaboradores = horarios.stream()
                    .map(Horario::getIdLojautilizador)
                    .filter(Objects::nonNull)
                    .map(r -> r.getIdUtilizador())
                    .filter(Objects::nonNull)
                    .map(Utilizador::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            lblResumoHorarioMensal.setText(
                    "Calendário mensal publicado da loja para "
                            + mesSelecionado.nome().toLowerCase(LOCALE_PT)
                            + " de " + anoSelecionado + ", com "
                            + totalColaboradores + " colaborador(es) e "
                            + horarios.size()
                            + " turno(s). Usa o filtro de colaborador para isolar uma escala individual.");
        } catch (Exception e) {
            renderizarCalendarioMensalLoja(YearMonth.now(), List.of());
            lblResumoHorarioMensal.setText("Não foi possível carregar o horário mensal da loja neste momento.");
        }
    }

    // ── Rendering ────────────────────────────────────────────────────────────

    private void renderizarCalendarioHorarioPublicado(LocalDate inicioSemana, List<Horario> horarios) {
        Map<LocalDate, List<String>> eventosPorDia = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            String nomeLoja = horario.getIdLojautilizador() != null
                    && horario.getIdLojautilizador().getIdLoja() != null
                    ? horario.getIdLojautilizador().getIdLoja().getNome() : "-";
            String evento = formatarPeriodo(horario) + " | " + nomeLoja;
            eventosPorDia.computeIfAbsent(horario.getDataTurno(), k -> new java.util.ArrayList<>())
                    .add(evento);
        }
        CalendarioSemanalHelper.preencherCalendario(boxSemanaHorarioPublicado, inicioSemana,
                eventosPorDia, "Sem turno publicado");
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
                    String nome = h.getIdLojautilizador() != null
                            && h.getIdLojautilizador().getIdUtilizador() != null
                            ? h.getIdLojautilizador().getIdUtilizador().getNome() : "?";
                    String cargo = h.getIdLojautilizador() != null
                            && h.getIdLojautilizador().getIdCargo() != null
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

    private void abrirDetalheDiaMensal(LocalDate data) {
        DetalheDiaDialog.abrirHorariosPublicados(data, horariosMensaisAtuais, obterJanela());
    }

    // ── Semana / Cabeçalho ───────────────────────────────────────────────────

    private void atualizarCabecalhoSemanaHorarioPublicado() {
        if (lblSemanaHorarioPublicadoIntervalo != null && semanaHorarioPublicadoInicio != null) {
            lblSemanaHorarioPublicadoIntervalo.setText(
                    CalendarioSemanalHelper.formatarIntervaloSemana(semanaHorarioPublicadoInicio));
        }
        atualizarOpcoesSemanaReferencia();
    }

    private void atualizarOpcoesSemanaReferencia() {
        if (cbSemanaHorarioPublicado == null || semanaHorarioPublicadoInicio == null) return;
        aAtualizarSeletorSemana = true;
        try {
            List<SemanaOption> semanas = java.util.stream.IntStream.rangeClosed(-12, 24)
                    .mapToObj(i -> new SemanaOption(semanaHorarioPublicadoInicio.plusWeeks(i)))
                    .toList();
            cbSemanaHorarioPublicado.setItems(FXCollections.observableArrayList(semanas));
            cbSemanaHorarioPublicado.getSelectionModel().select(
                    semanas.stream()
                            .filter(s -> Objects.equals(s.inicio(), semanaHorarioPublicadoInicio))
                            .findFirst()
                            .orElseGet(() -> new SemanaOption(semanaHorarioPublicadoInicio)));
        } finally {
            aAtualizarSeletorSemana = false;
        }
    }

    // ── Saudação e data ──────────────────────────────────────────────────────

    private String construirSaudacao(String nome) {
        String primeiroNome = nome != null && !nome.isBlank()
                ? nome.trim().split("\\s+")[0] : "Equipa";
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
        String diaCapitalizado = diaSemana.substring(0, 1).toUpperCase()
                + diaSemana.substring(1).toLowerCase(LOCALE_PT);
        lblDataHoje.setText(diaCapitalizado + ", " + dataFormatada);
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private javafx.stage.Window obterJanela() {
        if (lblBemVindo == null || lblBemVindo.getScene() == null) return null;
        return lblBemVindo.getScene().getWindow();
    }

    private String formatarData(LocalDate data) {
        return data == null ? "-" : DATA_FORMATTER.format(data);
    }

    private String formatarPeriodo(Horario horario) {
        if (horario == null || horario.getIdTurno() == null) return "-";
        String inicio = horario.getIdTurno().getHoraInicio() != null
                ? horario.getIdTurno().getHoraInicio().format(HORARIO_COMPACTO) : "--:--";
        String fim = horario.getIdTurno().getHoraFim() != null
                ? horario.getIdTurno().getHoraFim().format(HORARIO_COMPACTO) : "--:--";
        return inicio + " - " + fim;
    }

    // ── Records internos ─────────────────────────────────────────────────────

    private record ColaboradorFiltroOption(Integer idUtilizador, String label) {
        private static ColaboradorFiltroOption todos() {
            return new ColaboradorFiltroOption(null, "Toda a equipa");
        }

        @Override
        public String toString() { return label; }
    }

    private record SemanaOption(LocalDate inicio) {
        @Override
        public String toString() {
            return CalendarioSemanalHelper.formatarIntervaloSemana(inicio);
        }
    }
}
