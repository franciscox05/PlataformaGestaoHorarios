package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.DayOffService;
import com.example.projeto2.API.Services.GestaoFuncionariosService;
import com.example.projeto2.API.Services.HorarioService;
import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.API.Services.PermutaService;
import com.example.projeto2.API.Services.PreferenciaService;
import com.example.projeto2.DESKTOP.support.CalendarioSemanalHelper;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableCell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
@Scope("prototype")
public class GestaoFuncionariosController {

    private static final String FILTRO_TODOS = "Todos";
    private static final String ESTADO_ATIVO = "Ativo";
    private static final String ESTADO_INATIVO = "Inativo";

    @FXML private Label lblNomeLoja;
    @FXML private Label lblLocalizacao;
    @FXML private Label lblCargoGestor;
    @FXML private Label lblResumoTabela;
    @FXML private Label lblTituloFormulario;
    @FXML private Label lblMensagem;
    @FXML private TextField txtPesquisa;
    @FXML private ComboBox<String> cbFiltroEstado;
    @FXML private ComboBox<FiltroCargoOption> cbFiltroCargo;
    @FXML private TableView<ColaboradorLinha> tabelaColaboradores;
    @FXML private TableColumn<ColaboradorLinha, String> colNome;
    @FXML private TableColumn<ColaboradorLinha, String> colEmail;
    @FXML private TableColumn<ColaboradorLinha, String> colTelemovel;
    @FXML private TableColumn<ColaboradorLinha, String> colCargo;
    @FXML private TableColumn<ColaboradorLinha, String> colEstado;
    @FXML private TextField txtNome;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelemovel;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<CargoOption> cbCargo;
    @FXML private ComboBox<String> cbEstado;
    @FXML private Button btnGuardar;
    @FXML private Button btnDesativar;
    @FXML private Button btnRedefinirPassword;
    @FXML private Label lblSemanaColaborador;
    @FXML private Label lblResumoHorarioColaborador;
    @FXML private HBox boxSemanaColaborador;
    @FXML private Label lblPerfilOperacionalResumo;
    @FXML private TableView<FolgaLinha> tabelaFolgasColaborador;
    @FXML private TableColumn<FolgaLinha, String> colFolgaData;
    @FXML private TableColumn<FolgaLinha, String> colFolgaTipo;
    @FXML private TableColumn<FolgaLinha, String> colFolgaEstado;
    @FXML private TableColumn<FolgaLinha, FolgaLinha> colFolgaAcao;
    @FXML private TableView<PreferenciaLinha> tabelaPreferenciasColaborador;
    @FXML private TableColumn<PreferenciaLinha, String> colPreferenciaTipo;
    @FXML private TableColumn<PreferenciaLinha, String> colPreferenciaPeriodo;
    @FXML private TableColumn<PreferenciaLinha, String> colPreferenciaEstado;
    @FXML private TableColumn<PreferenciaLinha, PreferenciaLinha> colPreferenciaAcao;
    @FXML private TableView<PermutaLinha> tabelaPermutasColaborador;
    @FXML private TableColumn<PermutaLinha, String> colPermutaData;
    @FXML private TableColumn<PermutaLinha, String> colPermutaEstado;
    @FXML private TableColumn<PermutaLinha, String> colPermutaTurnos;
    @FXML private TableColumn<PermutaLinha, PermutaLinha> colPermutaAcao;

    private final GestaoFuncionariosService gestaoFuncionariosBLL;
    private final HorarioService horarioBLL;
    private final DayOffService dayOffBLL;
    private final PreferenciaService preferenciaBLL;
    private final PermutaService permutaBLL;
    private final PerfilService perfilBLL;
    private final ObservableList<ColaboradorLinha> colaboradores = FXCollections.observableArrayList();
    private final FilteredList<ColaboradorLinha> colaboradoresFiltrados = new FilteredList<>(colaboradores, colaborador -> true);
    private final ObservableList<FolgaLinha> folgasColaborador = FXCollections.observableArrayList();
    private final ObservableList<PreferenciaLinha> preferenciasColaborador = FXCollections.observableArrayList();
    private final ObservableList<PermutaLinha> permutasColaborador = FXCollections.observableArrayList();
    private Utilizador utilizadorLogado;
    private Integer idColaboradorEmEdicao;
    private LocalDate semanaColaboradorInicio;

    public GestaoFuncionariosController(GestaoFuncionariosService gestaoFuncionariosBLL,
                                        HorarioService horarioBLL,
                                        DayOffService dayOffBLL,
                                        PreferenciaService preferenciaBLL,
                                        PermutaService permutaBLL,
                                        PerfilService perfilBLL) {
        this.gestaoFuncionariosBLL = gestaoFuncionariosBLL;
        this.horarioBLL = horarioBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
        this.perfilBLL = perfilBLL;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarTabelasPerfilOperacional();
        configurarFiltros();
        configurarFormulario();
        configurarAtalhosRapidos();

        tabelaColaboradores.setItems(colaboradoresFiltrados);
        tabelaColaboradores.setPlaceholder(new Label("Ainda não existem colaboradores associados a esta loja."));

        // Tooltips explicativos nos botões condicionais
        if (btnRedefinirPassword != null) {
            btnRedefinirPassword.setTooltip(new javafx.scene.control.Tooltip("Seleciona um colaborador para redefinir a sua password"));
        }
        if (btnDesativar != null) {
            btnDesativar.setTooltip(new javafx.scene.control.Tooltip("Seleciona um colaborador ativo para desativar a conta"));
        }
        tabelaColaboradores.getSelectionModel().selectedItemProperty().addListener((observavel, anterior, selecionado) -> {
            if (selecionado != null) {
                preencherFormulario(selecionado);
                carregarHorarioSemanalColaborador(selecionado.idUtilizador(), selecionado.nome());
                carregarPerfilOperacionalColaborador(selecionado);
            } else {
                limparHorarioSemanalColaborador();
                limparPerfilOperacionalColaborador();
            }
        });

        esconderMensagem();
        limparFormularioParaNovo();
        semanaColaboradorInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        atualizarCabecalhoSemanaColaborador();
        limparHorarioSemanalColaborador();
        limparPerfilOperacionalColaborador();
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarDados(null);
    }

    @FXML
    public void onNovoClick() {
        tabelaColaboradores.getSelectionModel().clearSelection();
        limparFormularioParaNovo();
        esconderMensagem();
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            CargoOption cargoSelecionado = cbCargo.getValue();
            if (cargoSelecionado == null || cargoSelecionado.idCargo() == null) {
                throw new IllegalArgumentException("Seleciona um cargo valido para o colaborador.");
            }
            boolean novoColaborador = idColaboradorEmEdicao == null;
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    novoColaborador ? "Criar colaborador" : "Atualizar colaborador",
                    novoColaborador ? "Deseja criar este colaborador?" : "Deseja guardar as alterações deste colaborador?",
                    novoColaborador
                            ? "O novo colaborador ficará associado à loja atual."
                            : "Os dados do colaborador serão atualizados."
            )) {
                return;
            }
            Integer idGuardado = gestaoFuncionariosBLL.guardarColaborador(
                    utilizadorLogado.getId(),
                    new GestaoFuncionariosService.ColaboradorRequest(
                            idColaboradorEmEdicao,
                            txtNome.getText(),
                            txtEmail.getText(),
                            txtTelemovel.getText(),
                            txtPassword.getText(),
                            cargoSelecionado.idCargo(),
                            mapearEstadoParaPersistencia(cbEstado.getValue())
                    )
            );
            mostrarMensagem(
                    novoColaborador ? "Colaborador criado com sucesso." : "Colaborador atualizado com sucesso.",
                    true
            );
            carregarDados(idGuardado);
            txtPassword.clear();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível guardar o registo do colaborador.", false);
        }
    }

    @FXML
    public void onDesativarClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                throw new IllegalArgumentException("Seleciona um colaborador primeiro.");
            }
            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Desativar colaborador",
                    "Deseja desativar este colaborador?",
                    "O colaborador deixará de ficar ativo na loja atual."
            )) {
                return;
            }
            gestaoFuncionariosBLL.desativarColaborador(utilizadorLogado.getId(), selecionado.idUtilizador());
            mostrarMensagem("Colaborador desativado com sucesso.", true);
            carregarDados(null);
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível desativar o colaborador selecionado.", false);
        }
    }

    @FXML
    public void onRedefinirPasswordClick() {
        if (utilizadorLogado == null || utilizadorLogado.getId() == null) return;

        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            mostrarMensagem("Seleciona um colaborador primeiro.", false);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Redefinir palavra-passe");
        dialog.setHeaderText("Redefinir password de: " + selecionado.nome());

        ButtonType btnConfirmar = new ButtonType("Confirmar",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelar = new ButtonType("Cancelar",
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, btnCancelar);

        PasswordField pfNova = new PasswordField();
        pfNova.setPromptText("Nova palavra-passe (mín. 6 caracteres)");
        PasswordField pfConfirma = new PasswordField();
        pfConfirma.setPromptText("Confirmar nova palavra-passe");
        Label lblDialogErro = new Label();
        lblDialogErro.setStyle("-fx-text-fill: #c9141e; -fx-font-size: 11px;");
        lblDialogErro.setWrapText(true);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Nova password:"), 0, 0);
        grid.add(pfNova, 1, 0);
        grid.add(new Label("Confirmar:"), 0, 1);
        grid.add(pfConfirma, 1, 1);

        javafx.scene.layout.VBox conteudo = new javafx.scene.layout.VBox(10, grid, lblDialogErro);
        conteudo.setPrefWidth(340);
        dialog.getDialogPane().setContent(conteudo);

        javafx.scene.Node botaoConfirmar = dialog.getDialogPane().lookupButton(btnConfirmar);
        botaoConfirmar.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblDialogErro.setText("");
            try {
                perfilBLL.redefinirPasswordPorGerente(
                        utilizadorLogado.getId(),
                        selecionado.idUtilizador(),
                        pfNova.getText(),
                        pfConfirma.getText());
            } catch (IllegalArgumentException e) {
                lblDialogErro.setText(e.getMessage());
                event.consume();
            }
        });

        dialog.initOwner(obterJanela());
        dialog.showAndWait().ifPresent(resultado -> {
            if (resultado == btnConfirmar) {
                mostrarMensagem("Palavra-passe redefinida com sucesso.", true);
            }
        });
    }

    @FXML
    public void onSemanaColaboradorAnteriorClick() {
        semanaColaboradorInicio = semanaColaboradorInicio.minusWeeks(1);
        atualizarCabecalhoSemanaColaborador();
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            carregarHorarioSemanalColaborador(selecionado.idUtilizador(), selecionado.nome());
        } else {
            limparHorarioSemanalColaborador();
        }
    }

    @FXML
    public void onSemanaColaboradorSeguinteClick() {
        semanaColaboradorInicio = semanaColaboradorInicio.plusWeeks(1);
        atualizarCabecalhoSemanaColaborador();
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado != null) {
            carregarHorarioSemanalColaborador(selecionado.idUtilizador(), selecionado.nome());
        } else {
            limparHorarioSemanalColaborador();
        }
    }

    private void configurarTabela() {
        colNome.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().nome()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().email()));
        colTelemovel.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().telemovel()));
        colCargo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().cargo()));
        colEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colEstado.setCellFactory(coluna -> criarCelulaBadgeEstado());
    }

    private void configurarTabelasPerfilOperacional() {
        colFolgaData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().data()));
        colFolgaTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tipo()));
        colFolgaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colFolgaTipo.setCellFactory(coluna -> criarCelulaBadgeTipoFolga());
        colFolgaEstado.setCellFactory(coluna -> criarCelulaBadgeEstado());
        colFolgaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colFolgaAcao.setCellFactory(coluna -> criarCelulaAcaoFolga());
        tabelaFolgasColaborador.setItems(folgasColaborador);
        tabelaFolgasColaborador.setPlaceholder(new Label("Sem folgas registadas para este colaborador."));

        colPreferenciaTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tipo()));
        colPreferenciaPeriodo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().periodo()));
        colPreferenciaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colPreferenciaEstado.setCellFactory(coluna -> criarCelulaBadgeEstado());
        colPreferenciaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colPreferenciaAcao.setCellFactory(coluna -> criarCelulaAcaoPreferencia());
        tabelaPreferenciasColaborador.setItems(preferenciasColaborador);
        tabelaPreferenciasColaborador.setPlaceholder(new Label("Sem preferências registadas para este colaborador."));

        colPermutaData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().dataTurno()));
        colPermutaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colPermutaTurnos.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().turnos()));
        colPermutaEstado.setCellFactory(coluna -> criarCelulaBadgeEstado());
        colPermutaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colPermutaAcao.setCellFactory(coluna -> criarCelulaAcaoPermuta());
        tabelaPermutasColaborador.setItems(permutasColaborador);
        tabelaPermutasColaborador.setPlaceholder(new Label("Sem permutas registadas para este colaborador."));
    }

    private TableCell<FolgaLinha, FolgaLinha> criarCelulaAcaoFolga() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);
            {
                btnAprovar.setOnAction(event -> { FolgaLinha linha = getItem(); if (linha != null) decidirFolga(linha, true); });
                btnRejeitar.setOnAction(event -> { FolgaLinha linha = getItem(); if (linha != null) decidirFolga(linha, false); });
            }
            @Override
            protected void updateItem(FolgaLinha item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null || !item.pendenteAprovacao() ? null : contentor);
            }
        };
    }

    private TableCell<PreferenciaLinha, PreferenciaLinha> criarCelulaAcaoPreferencia() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);
            {
                btnAprovar.setOnAction(event -> { PreferenciaLinha linha = getItem(); if (linha != null) decidirPreferencia(linha, true); });
                btnRejeitar.setOnAction(event -> { PreferenciaLinha linha = getItem(); if (linha != null) decidirPreferencia(linha, false); });
            }
            @Override
            protected void updateItem(PreferenciaLinha item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null || !item.pendenteAprovacao() ? null : contentor);
            }
        };
    }

    private TableCell<PermutaLinha, PermutaLinha> criarCelulaAcaoPermuta() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);
            {
                btnAprovar.setOnAction(event -> { PermutaLinha linha = getItem(); if (linha != null) decidirPermuta(linha, true); });
                btnRejeitar.setOnAction(event -> { PermutaLinha linha = getItem(); if (linha != null) decidirPermuta(linha, false); });
            }
            @Override
            protected void updateItem(PermutaLinha item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null || !item.pendenteAprovacao() ? null : contentor);
            }
        };
    }

    private void configurarFiltros() {
        cbFiltroEstado.setItems(FXCollections.observableArrayList(FILTRO_TODOS, ESTADO_ATIVO, ESTADO_INATIVO));
        cbFiltroEstado.setValue(FILTRO_TODOS);
        cbFiltroEstado.valueProperty().addListener((observavel, antigo, valor) -> { atualizarFiltros(); esconderMensagem(); });
        cbFiltroCargo.valueProperty().addListener((observavel, antigo, valor) -> { atualizarFiltros(); esconderMensagem(); });
        txtPesquisa.textProperty().addListener((observavel, antigo, valor) -> { atualizarFiltros(); esconderMensagem(); });
        txtPesquisa.setTooltip(new Tooltip("ENTER: selecionar primeiro resultado | ESC: limpar pesquisa"));
        txtPesquisa.setOnKeyPressed(evento -> {
            if (evento.getCode() == KeyCode.ESCAPE) { txtPesquisa.clear(); evento.consume(); return; }
            if (evento.getCode() == KeyCode.ENTER) { selecionarPrimeiroColaboradorFiltrado(); evento.consume(); }
        });
    }

    private void configurarFormulario() {
        cbEstado.setItems(FXCollections.observableArrayList(ESTADO_ATIVO, ESTADO_INATIVO));
        cbEstado.setValue(ESTADO_ATIVO);
        txtTelemovel.textProperty().addListener((observavel, antigo, valor) -> {
            if (valor == null) return;
            String apenasDigitos = valor.replaceAll("[^\\d]", "");
            if (apenasDigitos.length() > 9) apenasDigitos = apenasDigitos.substring(0, 9);
            if (!Objects.equals(valor, apenasDigitos)) txtTelemovel.setText(apenasDigitos);
        });
        txtNome.textProperty().addListener((observavel, antigo, valor) -> esconderMensagem());
        txtEmail.textProperty().addListener((observavel, antigo, valor) -> esconderMensagem());
        txtPassword.textProperty().addListener((observavel, antigo, valor) -> esconderMensagem());
        cbCargo.valueProperty().addListener((observavel, antigo, valor) -> esconderMensagem());
        cbEstado.valueProperty().addListener((observavel, antigo, valor) -> esconderMensagem());
    }

    private void carregarDados(Integer idPreferido) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            GestaoFuncionariosService.GestaoFuncionariosResumo resumo = gestaoFuncionariosBLL.obterResumo(utilizadorLogado.getId());
            lblNomeLoja.setText(resumo.nomeLoja());
            lblLocalizacao.setText(resumo.localizacao());
            lblCargoGestor.setText(resumo.cargoGestor());
            preencherCargos(resumo.cargosDisponiveis());
            colaboradores.setAll(resumo.colaboradores().stream().map(this::mapearLinhaTabela).toList());
            atualizarFiltros();
            if (idPreferido != null && selecionarColaborador(idPreferido)) return;
            if (!colaboradoresFiltrados.isEmpty()) { selecionarPrimeiroColaboradorFiltrado(); return; }
            limparFormularioParaNovo();
            limparHorarioSemanalColaborador();
            limparPerfilOperacionalColaborador();
        } catch (IllegalArgumentException e) {
            colaboradores.clear();
            preencherCargos(List.of());
            atualizarFiltros();
            limparFormularioParaNovo();
            lblNomeLoja.setText("-"); lblLocalizacao.setText("-"); lblCargoGestor.setText("-");
            limparPerfilOperacionalColaborador();
            mostrarMensagem(e.getMessage(), false);
        }
    }

    private void preencherCargos(List<GestaoFuncionariosService.CargoResumo> cargosDisponiveis) {
        Integer filtroCargoAtual = cbFiltroCargo.getValue() != null ? cbFiltroCargo.getValue().idCargo() : null;
        Integer cargoFormularioAtual = cbCargo.getValue() != null ? cbCargo.getValue().idCargo() : null;
        ObservableList<CargoOption> opcoesCargo = FXCollections.observableArrayList(
                cargosDisponiveis.stream().map(cargo -> new CargoOption(cargo.idCargo(), cargo.nome(), cargo.tipo())).toList()
        );
        cbCargo.setItems(opcoesCargo);
        ObservableList<FiltroCargoOption> opcoesFiltro = FXCollections.observableArrayList();
        opcoesFiltro.add(new FiltroCargoOption(null, FILTRO_TODOS));
        for (CargoOption cargo : opcoesCargo) opcoesFiltro.add(new FiltroCargoOption(cargo.idCargo(), cargo.nome()));
        cbFiltroCargo.setItems(opcoesFiltro);
        selecionarFiltroCargo(filtroCargoAtual);
        selecionarCargoFormulario(cargoFormularioAtual);
    }

    private void selecionarFiltroCargo(Integer idCargo) {
        for (FiltroCargoOption option : cbFiltroCargo.getItems()) {
            if (Objects.equals(option.idCargo(), idCargo)) { cbFiltroCargo.setValue(option); return; }
        }
        if (!cbFiltroCargo.getItems().isEmpty()) cbFiltroCargo.setValue(cbFiltroCargo.getItems().get(0));
    }

    private void selecionarCargoFormulario(Integer idCargo) {
        for (CargoOption option : cbCargo.getItems()) {
            if (Objects.equals(option.idCargo(), idCargo)) { cbCargo.setValue(option); return; }
        }
        cbCargo.setValue(!cbCargo.getItems().isEmpty() ? cbCargo.getItems().get(0) : null);
    }

    private void atualizarFiltros() {
        String pesquisa = txtPesquisa.getText() != null ? txtPesquisa.getText().trim().toLowerCase() : "";
        String filtroEstado = cbFiltroEstado.getValue();
        Integer filtroCargo = cbFiltroCargo.getValue() != null ? cbFiltroCargo.getValue().idCargo() : null;
        colaboradoresFiltrados.setPredicate(colaborador -> {
            boolean correspondePesquisa = pesquisa.isBlank()
                    || contemTexto(colaborador.nome(), pesquisa)
                    || contemTexto(colaborador.email(), pesquisa)
                    || contemTexto(colaborador.telemovel(), pesquisa)
                    || contemTexto(colaborador.cargo(), pesquisa);
            boolean correspondeEstado = FILTRO_TODOS.equalsIgnoreCase(filtroEstado)
                    || (ESTADO_ATIVO.equalsIgnoreCase(filtroEstado) && colaborador.ativo())
                    || (ESTADO_INATIVO.equalsIgnoreCase(filtroEstado) && !colaborador.ativo());
            boolean correspondeCargo = filtroCargo == null || Objects.equals(colaborador.idCargo(), filtroCargo);
            return correspondePesquisa && correspondeEstado && correspondeCargo;
        });
        lblResumoTabela.setText(colaboradoresFiltrados.size() + " colaborador(es) encontrado(s)");
        garantirSelecaoAposFiltro();
        atualizarEstadoAcoes();
    }

    private boolean contemTexto(String valor, String pesquisa) {
        return valor != null && valor.toLowerCase().contains(pesquisa);
    }

    private void configurarAtalhosRapidos() {
        btnGuardar.setTooltip(new Tooltip("Guardar colaborador (Ctrl+S)"));
        txtPesquisa.sceneProperty().addListener((obs, antiga, nova) -> {
            if (nova == null) return;
            nova.setOnKeyPressed(evento -> {
                if (!evento.isControlDown()) return;
                if (evento.getCode() == KeyCode.S) { onGuardarClick(); evento.consume(); }
                else if (evento.getCode() == KeyCode.N) { onNovoClick(); txtNome.requestFocus(); evento.consume(); }
            });
        });
    }

    private <T> TableCell<T, String> criarCelulaBadgeEstado() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) { setGraphic(null); setText(null); return; }
                Label badge = new Label(estado.toUpperCase(Locale.ROOT));
                badge.getStyleClass().add("badge-estado");
                String normalizado = normalizarTexto(estado);
                if (normalizado.contains("aprova") || normalizado.contains("ativo") || normalizado.contains("publicad"))
                    badge.getStyleClass().add("badge-aprovado");
                else if (normalizado.contains("rejeita") || normalizado.contains("inativo") || normalizado.contains("cancelad"))
                    badge.getStyleClass().add("badge-rejeitado");
                else if (normalizado.contains("pendente") || normalizado.contains("enviado"))
                    badge.getStyleClass().add("badge-enviado");
                else
                    badge.getStyleClass().add("badge-rascunho");
                setGraphic(badge); setText(null);
            }
        };
    }

    private <T> TableCell<T, String> criarCelulaBadgeTipoFolga() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null || tipo.isBlank()) { setGraphic(null); setText(null); return; }
                Label badge = new Label(tipo.toUpperCase(Locale.ROOT));
                badge.getStyleClass().addAll("badge-estado", "badge-folga");
                setGraphic(badge); setText(null);
            }
        };
    }

    private void selecionarPrimeiroColaboradorFiltrado() {
        if (colaboradoresFiltrados.isEmpty()) { tabelaColaboradores.getSelectionModel().clearSelection(); return; }
        tabelaColaboradores.getSelectionModel().selectFirst();
        tabelaColaboradores.scrollTo(0);
    }

    private void garantirSelecaoAposFiltro() {
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado != null && colaboradoresFiltrados.contains(selecionado)) return;
        if (!colaboradoresFiltrados.isEmpty()) selecionarPrimeiroColaboradorFiltrado();
        else tabelaColaboradores.getSelectionModel().clearSelection();
    }

    private String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).trim();
    }

    private ColaboradorLinha mapearLinhaTabela(GestaoFuncionariosService.ColaboradorResumo colaborador) {
        return new ColaboradorLinha(
                colaborador.idUtilizador(), colaborador.nome(), colaborador.email(),
                valorOuTraco(colaborador.telemovel()), colaborador.idCargo(),
                colaborador.cargoNome(), formatarEstado(colaborador.estado())
        );
    }

    private void preencherFormulario(ColaboradorLinha colaborador) {
        idColaboradorEmEdicao = colaborador.idUtilizador();
        lblTituloFormulario.setText("Editar Colaborador");
        txtNome.setText(valorOuVazio(colaborador.nome()));
        txtEmail.setText(valorOuVazio(colaborador.email()));
        txtTelemovel.setText("-".equals(colaborador.telemovel()) ? "" : colaborador.telemovel());
        txtPassword.clear();
        selecionarCargoFormulario(colaborador.idCargo());
        cbEstado.setValue(colaborador.ativo() ? ESTADO_ATIVO : ESTADO_INATIVO);
        atualizarEstadoAcoes();
    }

    private void limparFormularioParaNovo() {
        idColaboradorEmEdicao = null;
        lblTituloFormulario.setText("Novo Colaborador");
        txtNome.clear(); txtEmail.clear(); txtTelemovel.clear(); txtPassword.clear();
        cbEstado.setValue(ESTADO_ATIVO);
        selecionarCargoFormulario(null);
        atualizarEstadoAcoes();
    }

    private boolean selecionarColaborador(Integer idUtilizador) {
        for (ColaboradorLinha colaborador : tabelaColaboradores.getItems()) {
            if (Objects.equals(colaborador.idUtilizador(), idUtilizador)) {
                tabelaColaboradores.getSelectionModel().select(colaborador);
                tabelaColaboradores.scrollTo(colaborador);
                preencherFormulario(colaborador);
                return true;
            }
        }
        return false;
    }

    private void atualizarEstadoAcoes() {
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        btnDesativar.setDisable(selecionado == null || !selecionado.ativo());
        btnGuardar.setDisable(cbCargo.getItems().isEmpty());
        if (btnRedefinirPassword != null) {
            btnRedefinirPassword.setDisable(selecionado == null);
        }
    }

    private String mapearEstadoParaPersistencia(String estado) {
        return ESTADO_INATIVO.equalsIgnoreCase(estado) ? "inativo" : "ativo";
    }

    private String formatarEstado(String estado) {
        return "inativo".equalsIgnoreCase(estado) ? ESTADO_INATIVO : ESTADO_ATIVO;
    }

    private String valorOuTraco(String valor) {
        return (valor == null || valor.isBlank()) ? "-" : valor;
    }

    private String valorOuVazio(String valor) {
        return valor == null || "-".equals(valor) ? "" : valor;
    }

    private void atualizarCabecalhoSemanaColaborador() {
        lblSemanaColaborador.setText(CalendarioSemanalHelper.formatarIntervaloSemana(semanaColaboradorInicio));
    }

    private void carregarHorarioSemanalColaborador(Integer idColaborador, String nomeColaborador) {
        try {
            LocalDate dataFim = semanaColaboradorInicio.plusDays(6);
            List<Horario> horarios = horarioBLL.listarHorarioPublicadoDoUtilizador(idColaborador, semanaColaboradorInicio, dataFim);
            Map<LocalDate, List<String>> eventos = new LinkedHashMap<>();
            for (Horario horario : horarios) {
                String evento = horario.getIdTurno().getHoraInicio() + " - "
                        + horario.getIdTurno().getHoraFim() + " | "
                        + horario.getIdLojautilizador().getIdLoja().getNome();
                eventos.computeIfAbsent(horario.getDataTurno(), chave -> new java.util.ArrayList<>()).add(evento);
            }
            CalendarioSemanalHelper.preencherCalendario(boxSemanaColaborador, semanaColaboradorInicio, eventos, "Sem turno");
            lblResumoHorarioColaborador.setText(horarios.isEmpty()
                    ? nomeColaborador + " não tem horário publicado nesta semana."
                    : nomeColaborador + " tem " + horarios.size() + " turno(s) publicados nesta semana."
            );
        } catch (Exception e) {
            limparHorarioSemanalColaborador();
            lblResumoHorarioColaborador.setText("Não foi possível carregar o horário semanal deste colaborador.");
        }
    }

    private void limparHorarioSemanalColaborador() {
        CalendarioSemanalHelper.preencherCalendario(boxSemanaColaborador, semanaColaboradorInicio, Map.of(), "Seleciona um colaborador");
        lblResumoHorarioColaborador.setText("Seleciona um colaborador para veres o horário publicado da semana.");
    }

    private void carregarPerfilOperacionalColaborador(ColaboradorLinha colaborador) {
        if (colaborador == null || colaborador.idUtilizador() == null
                || utilizadorLogado == null || utilizadorLogado.getId() == null) {
            limparPerfilOperacionalColaborador();
            return;
        }
        Integer idGestor = utilizadorLogado.getId();
        Integer idColaborador = colaborador.idUtilizador();
        try {
            List<DayOff> historicoFolgas = dayOffBLL.listarPedidosPorUtilizador(idColaborador);
            Map<Integer, DayOff> pendentesFolga = dayOffBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> item.getIdUtilizador() != null && idColaborador.equals(item.getIdUtilizador().getId()))
                    .collect(java.util.stream.Collectors.toMap(DayOff::getIdDayoff, item -> item, (a, b) -> a));
            folgasColaborador.setAll(historicoFolgas.stream()
                    .sorted(java.util.Comparator.comparing(DayOff::getDataAusencia,
                            java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .limit(12)
                    .map(item -> new FolgaLinha(
                            item.getIdDayoff(),
                            formatarData(item.getDataAusencia()),
                            valorOuTraco(item.getTipo()),
                            valorOuTraco(item.getEstado()),
                            pendentesFolga.containsKey(item.getIdDayoff())
                    )).toList());

            List<Preferencia> historicoPreferencias = preferenciaBLL.listarPreferenciasPorUtilizador(idColaborador);
            Map<Integer, Preferencia> pendentesPreferencia = preferenciaBLL.listarPreferenciasPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> item.getIdUtilizador() != null && idColaborador.equals(item.getIdUtilizador().getId()))
                    .collect(java.util.stream.Collectors.toMap(Preferencia::getId, item -> item, (a, b) -> a));
            preferenciasColaborador.setAll(historicoPreferencias.stream()
                    .sorted(java.util.Comparator
                            .comparing(Preferencia::getDataInicio, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                            .thenComparing(Preferencia::getId, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .limit(12)
                    .map(item -> new PreferenciaLinha(
                            item.getId(),
                            valorOuTraco(item.getTipo()),
                            formatarPeriodo(item.getDataInicio(), item.getDataFim()),
                            valorOuTraco(item.getEstado()),
                            pendentesPreferencia.containsKey(item.getId())
                    )).toList());

            List<Permuta> historicoPermutas = permutaBLL.listarPedidosEnviados(idColaborador);
            Map<Integer, Permuta> pendentesPermuta = permutaBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> item.getIdHorarioOrigem() != null
                            && item.getIdHorarioOrigem().getIdLojautilizador() != null
                            && item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() != null
                            && idColaborador.equals(item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId()))
                    .collect(java.util.stream.Collectors.toMap(Permuta::getId, item -> item, (a, b) -> a));
            permutasColaborador.setAll(historicoPermutas.stream()
                    .limit(12)
                    .map(item -> new PermutaLinha(
                            item.getId(),
                            item.getIdHorarioOrigem() != null ? formatarData(item.getIdHorarioOrigem().getDataTurno()) : "-",
                            valorOuTraco(item.getEstado() != null ? item.getEstado().name() : null),
                            formatarTurnosPermuta(item),
                            pendentesPermuta.containsKey(item.getId())
                    )).toList());

            long pendentes = folgasColaborador.stream().filter(FolgaLinha::pendenteAprovacao).count()
                    + preferenciasColaborador.stream().filter(PreferenciaLinha::pendenteAprovacao).count()
                    + permutasColaborador.stream().filter(PermutaLinha::pendenteAprovacao).count();
            lblPerfilOperacionalResumo.setText("Perfil operacional de " + colaborador.nome()
                    + " | " + folgasColaborador.size() + " folga(s), "
                    + preferenciasColaborador.size() + " preferência(s), "
                    + permutasColaborador.size() + " permuta(s) | "
                    + pendentes + " pendente(s).");
        } catch (Exception ex) {
            limparPerfilOperacionalColaborador();
            lblPerfilOperacionalResumo.setText("Não foi possível carregar o perfil operacional deste colaborador.");
        }
    }

    private void limparPerfilOperacionalColaborador() {
        folgasColaborador.clear();
        preferenciasColaborador.clear();
        permutasColaborador.clear();
        if (lblPerfilOperacionalResumo != null)
            lblPerfilOperacionalResumo.setText("Seleciona um colaborador para veres folgas, preferências, permutas e decisões pendentes.");
    }

    private void decidirFolga(FolgaLinha linha, boolean aprovar) {
        if (linha == null || linha.idDayOff() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) return;
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        if (!DialogosHelper.confirmarAcao(obterJanela(),
                aprovar ? "Aprovar folga" : "Rejeitar folga",
                aprovar ? "Deseja aprovar este pedido de folga?" : "Deseja rejeitar este pedido de folga?",
                "A decisão será registada de imediato.")) return;
        try {
            if (aprovar) { dayOffBLL.aprovarPedidoFolga(linha.idDayOff(), utilizadorLogado.getId()); mostrarMensagem("Pedido de folga aprovado com sucesso.", true); }
            else { dayOffBLL.rejeitarPedidoFolga(linha.idDayOff(), utilizadorLogado.getId()); mostrarMensagem("Pedido de folga rejeitado com sucesso.", true); }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) { mostrarMensagem(ex.getMessage(), false); }
    }

    private void decidirPreferencia(PreferenciaLinha linha, boolean aprovar) {
        if (linha == null || linha.idPreferencia() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) return;
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        if (!DialogosHelper.confirmarAcao(obterJanela(),
                aprovar ? "Aprovar preferência" : "Rejeitar preferência",
                aprovar ? "Deseja aprovar esta preferência?" : "Deseja rejeitar esta preferência?",
                "A decisão será registada de imediato.")) return;
        try {
            if (aprovar) { preferenciaBLL.aprovarPreferencia(linha.idPreferencia(), utilizadorLogado.getId(), null); mostrarMensagem("Preferência aprovada com sucesso.", true); }
            else { preferenciaBLL.rejeitarPreferencia(linha.idPreferencia(), utilizadorLogado.getId(), null); mostrarMensagem("Preferência rejeitada com sucesso.", true); }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) { mostrarMensagem(ex.getMessage(), false); }
    }

    private void decidirPermuta(PermutaLinha linha, boolean aprovar) {
        if (linha == null || linha.idPermuta() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) return;
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        if (!DialogosHelper.confirmarAcao(obterJanela(),
                aprovar ? "Aprovar permuta" : "Rejeitar permuta",
                aprovar ? "Deseja aprovar este pedido de permuta?" : "Deseja rejeitar este pedido de permuta?",
                "A decisão será registada de imediato.")) return;
        try {
            if (aprovar) { permutaBLL.aprovarPedidoPermuta(linha.idPermuta(), utilizadorLogado.getId()); mostrarMensagem("Permuta aprovada com sucesso.", true); }
            else { permutaBLL.rejeitarPedidoPermuta(linha.idPermuta(), utilizadorLogado.getId()); mostrarMensagem("Permuta rejeitada com sucesso.", true); }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) { mostrarMensagem(ex.getMessage(), false); }
    }

    private Button criarBotaoAcao(String texto, boolean destaque) {
        Button botao = new Button(texto);
        botao.getStyleClass().add(destaque ? "botao-acao" : "botao-secundario");
        botao.getStyleClass().add("topo-botao-curto");
        return botao;
    }

    private String formatarData(LocalDate data) {
        return data == null ? "-" : data.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return formatarData(dataInicio) + " -> " + formatarData(dataFim);
    }

    private String formatarTurnosPermuta(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null || permuta.getIdHorarioDestino() == null) return "-";
        String origem = permuta.getIdHorarioOrigem().getIdTurno() != null
                ? permuta.getIdHorarioOrigem().getIdTurno().getHoraInicio() + "-" + permuta.getIdHorarioOrigem().getIdTurno().getHoraFim()
                : "-";
        String destino = permuta.getIdHorarioDestino().getIdTurno() != null
                ? permuta.getIdHorarioDestino().getIdTurno().getHoraInicio() + "-" + permuta.getIdHorarioDestino().getIdTurno().getHoraFim()
                : "-";
        return origem + " -> " + destino;
    }

    private void esconderMensagem() {
        lblMensagem.setManaged(false); lblMensagem.setVisible(false); lblMensagem.setText("");
    }

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        lblMensagem.setText(mensagem);
        lblMensagem.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagem.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagem.setManaged(true); lblMensagem.setVisible(true);
    }

    private Window obterJanela() {
        if (lblNomeLoja == null || lblNomeLoja.getScene() == null) return null;
        return lblNomeLoja.getScene().getWindow();
    }

    private record CargoOption(Integer idCargo, String nome, String tipo) {
        @Override public String toString() { return nome; }
    }

    private record FiltroCargoOption(Integer idCargo, String nome) {
        @Override public String toString() { return nome; }
    }

    private record ColaboradorLinha(
            Integer idUtilizador, String nome, String email, String telemovel,
            Integer idCargo, String cargo, String estado) {
        private boolean ativo() { return ESTADO_ATIVO.equalsIgnoreCase(estado); }
    }

    private record FolgaLinha(Integer idDayOff, String data, String tipo, String estado, boolean pendenteAprovacao) {}
    private record PreferenciaLinha(Integer idPreferencia, String tipo, String periodo, String estado, boolean pendenteAprovacao) {}
    private record PermutaLinha(Integer idPermuta, String dataTurno, String estado, String turnos, boolean pendenteAprovacao) {}
}
