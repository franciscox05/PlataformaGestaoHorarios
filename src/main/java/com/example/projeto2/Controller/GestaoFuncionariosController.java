package com.example.projeto2.Controller;

import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.GestaoFuncionariosBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Scope("prototype")
public class GestaoFuncionariosController {

    private static final String FILTRO_TODOS = "Todos";
    private static final String ESTADO_ATIVO = "Ativo";
    private static final String ESTADO_INATIVO = "Inativo";

    @FXML
    private Label lblNomeLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private Label lblCargoGestor;

    @FXML
    private Label lblResumoTabela;

    @FXML
    private Label lblTituloFormulario;

    @FXML
    private Label lblMensagem;

    @FXML
    private TextField txtPesquisa;

    @FXML
    private ComboBox<String> cbFiltroEstado;

    @FXML
    private ComboBox<FiltroCargoOption> cbFiltroCargo;

    @FXML
    private TableView<ColaboradorLinha> tabelaColaboradores;

    @FXML
    private TableColumn<ColaboradorLinha, String> colNome;

    @FXML
    private TableColumn<ColaboradorLinha, String> colEmail;

    @FXML
    private TableColumn<ColaboradorLinha, String> colTelemovel;

    @FXML
    private TableColumn<ColaboradorLinha, String> colCargo;

    @FXML
    private TableColumn<ColaboradorLinha, String> colEstado;

    @FXML
    private TextField txtNome;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtTelemovel;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private ComboBox<CargoOption> cbCargo;

    @FXML
    private ComboBox<String> cbEstado;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnDesativar;

    @FXML
    private Label lblSemanaColaborador;

    @FXML
    private Label lblResumoHorarioColaborador;

    @FXML
    private HBox boxSemanaColaborador;

    @FXML
    private Label lblPerfilOperacionalResumo;

    @FXML
    private TableView<FolgaLinha> tabelaFolgasColaborador;

    @FXML
    private TableColumn<FolgaLinha, String> colFolgaData;

    @FXML
    private TableColumn<FolgaLinha, String> colFolgaTipo;

    @FXML
    private TableColumn<FolgaLinha, String> colFolgaEstado;

    @FXML
    private TableColumn<FolgaLinha, FolgaLinha> colFolgaAcao;

    @FXML
    private TableView<PreferenciaLinha> tabelaPreferenciasColaborador;

    @FXML
    private TableColumn<PreferenciaLinha, String> colPreferenciaTipo;

    @FXML
    private TableColumn<PreferenciaLinha, String> colPreferenciaPeriodo;

    @FXML
    private TableColumn<PreferenciaLinha, String> colPreferenciaEstado;

    @FXML
    private TableColumn<PreferenciaLinha, PreferenciaLinha> colPreferenciaAcao;

    @FXML
    private TableView<PermutaLinha> tabelaPermutasColaborador;

    @FXML
    private TableColumn<PermutaLinha, String> colPermutaData;

    @FXML
    private TableColumn<PermutaLinha, String> colPermutaEstado;

    @FXML
    private TableColumn<PermutaLinha, String> colPermutaTurnos;

    @FXML
    private TableColumn<PermutaLinha, PermutaLinha> colPermutaAcao;

    private final GestaoFuncionariosBLL gestaoFuncionariosBLL;
    private final HorarioBLL horarioBLL;
    private final DayOffBLL dayOffBLL;
    private final PreferenciaBLL preferenciaBLL;
    private final PermutaBLL permutaBLL;
    private final ObservableList<ColaboradorLinha> colaboradores = FXCollections.observableArrayList();
    private final FilteredList<ColaboradorLinha> colaboradoresFiltrados = new FilteredList<>(colaboradores, colaborador -> true);
    private final ObservableList<FolgaLinha> folgasColaborador = FXCollections.observableArrayList();
    private final ObservableList<PreferenciaLinha> preferenciasColaborador = FXCollections.observableArrayList();
    private final ObservableList<PermutaLinha> permutasColaborador = FXCollections.observableArrayList();
    private Utilizador utilizadorLogado;
    private Integer idColaboradorEmEdicao;
    private LocalDate semanaColaboradorInicio;

    public GestaoFuncionariosController(GestaoFuncionariosBLL gestaoFuncionariosBLL,
                                        HorarioBLL horarioBLL,
                                        DayOffBLL dayOffBLL,
                                        PreferenciaBLL preferenciaBLL,
                                        PermutaBLL permutaBLL) {
        this.gestaoFuncionariosBLL = gestaoFuncionariosBLL;
        this.horarioBLL = horarioBLL;
        this.dayOffBLL = dayOffBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.permutaBLL = permutaBLL;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarTabelasPerfilOperacional();
        configurarFiltros();
        configurarFormulario();

        tabelaColaboradores.setItems(colaboradoresFiltrados);
        tabelaColaboradores.setPlaceholder(new Label("Ainda não existem colaboradores associados a esta loja."));
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
                throw new IllegalArgumentException("Seleciona um cargo válido para o colaborador.");
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
                    new GestaoFuncionariosBLL.ColaboradorRequest(
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
    }

    private void configurarTabelasPerfilOperacional() {
        colFolgaData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().data()));
        colFolgaTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tipo()));
        colFolgaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colFolgaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colFolgaAcao.setCellFactory(coluna -> criarCelulaAcaoFolga());
        tabelaFolgasColaborador.setItems(folgasColaborador);

        colPreferenciaTipo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tipo()));
        colPreferenciaPeriodo.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().periodo()));
        colPreferenciaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colPreferenciaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colPreferenciaAcao.setCellFactory(coluna -> criarCelulaAcaoPreferencia());
        tabelaPreferenciasColaborador.setItems(preferenciasColaborador);

        colPermutaData.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().dataTurno()));
        colPermutaEstado.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().estado()));
        colPermutaTurnos.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().turnos()));
        colPermutaAcao.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        colPermutaAcao.setCellFactory(coluna -> criarCelulaAcaoPermuta());
        tabelaPermutasColaborador.setItems(permutasColaborador);
    }

    private TableCell<FolgaLinha, FolgaLinha> criarCelulaAcaoFolga() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);

            {
                btnAprovar.setOnAction(event -> {
                    FolgaLinha linha = getItem();
                    if (linha != null) {
                        decidirFolga(linha, true);
                    }
                });
                btnRejeitar.setOnAction(event -> {
                    FolgaLinha linha = getItem();
                    if (linha != null) {
                        decidirFolga(linha, false);
                    }
                });
            }

            @Override
            protected void updateItem(FolgaLinha item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.pendenteAprovacao()) {
                    setGraphic(null);
                } else {
                    setGraphic(contentor);
                }
            }
        };
    }

    private TableCell<PreferenciaLinha, PreferenciaLinha> criarCelulaAcaoPreferencia() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);

            {
                btnAprovar.setOnAction(event -> {
                    PreferenciaLinha linha = getItem();
                    if (linha != null) {
                        decidirPreferencia(linha, true);
                    }
                });
                btnRejeitar.setOnAction(event -> {
                    PreferenciaLinha linha = getItem();
                    if (linha != null) {
                        decidirPreferencia(linha, false);
                    }
                });
            }

            @Override
            protected void updateItem(PreferenciaLinha item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.pendenteAprovacao()) {
                    setGraphic(null);
                } else {
                    setGraphic(contentor);
                }
            }
        };
    }

    private TableCell<PermutaLinha, PermutaLinha> criarCelulaAcaoPermuta() {
        return new TableCell<>() {
            private final Button btnAprovar = criarBotaoAcao("Aprovar", true);
            private final Button btnRejeitar = criarBotaoAcao("Rejeitar", false);
            private final HBox contentor = new HBox(8.0, btnAprovar, btnRejeitar);

            {
                btnAprovar.setOnAction(event -> {
                    PermutaLinha linha = getItem();
                    if (linha != null) {
                        decidirPermuta(linha, true);
                    }
                });
                btnRejeitar.setOnAction(event -> {
                    PermutaLinha linha = getItem();
                    if (linha != null) {
                        decidirPermuta(linha, false);
                    }
                });
            }

            @Override
            protected void updateItem(PermutaLinha item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.pendenteAprovacao()) {
                    setGraphic(null);
                } else {
                    setGraphic(contentor);
                }
            }
        };
    }

    private void configurarFiltros() {
        cbFiltroEstado.setItems(FXCollections.observableArrayList(FILTRO_TODOS, ESTADO_ATIVO, ESTADO_INATIVO));
        cbFiltroEstado.setValue(FILTRO_TODOS);
        cbFiltroEstado.valueProperty().addListener((observavel, antigo, valor) -> {
            atualizarFiltros();
            esconderMensagem();
        });

        cbFiltroCargo.valueProperty().addListener((observavel, antigo, valor) -> {
            atualizarFiltros();
            esconderMensagem();
        });
        txtPesquisa.textProperty().addListener((observavel, antigo, valor) -> {
            atualizarFiltros();
            esconderMensagem();
        });
    }

    private void configurarFormulario() {
        cbEstado.setItems(FXCollections.observableArrayList(ESTADO_ATIVO, ESTADO_INATIVO));
        cbEstado.setValue(ESTADO_ATIVO);

        txtTelemovel.textProperty().addListener((observavel, antigo, valor) -> {
            if (valor == null) {
                return;
            }

            String apenasDigitos = valor.replaceAll("[^\\d]", "");
            if (apenasDigitos.length() > 9) {
                apenasDigitos = apenasDigitos.substring(0, 9);
            }

            if (!Objects.equals(valor, apenasDigitos)) {
                txtTelemovel.setText(apenasDigitos);
            }
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

            GestaoFuncionariosBLL.GestaoFuncionariosResumo resumo = gestaoFuncionariosBLL.obterResumo(utilizadorLogado.getId());

            lblNomeLoja.setText(resumo.nomeLoja());
            lblLocalizacao.setText(resumo.localizacao());
            lblCargoGestor.setText(resumo.cargoGestor());

            preencherCargos(resumo.cargosDisponiveis());
            colaboradores.setAll(resumo.colaboradores().stream()
                    .map(this::mapearLinhaTabela)
                    .toList());

            atualizarFiltros();

        if (idPreferido != null && selecionarColaborador(idPreferido)) {
            return;
        }

        limparFormularioParaNovo();
        limparHorarioSemanalColaborador();
        limparPerfilOperacionalColaborador();
        } catch (IllegalArgumentException e) {
            colaboradores.clear();
            preencherCargos(List.of());
            atualizarFiltros();
            limparFormularioParaNovo();
            lblNomeLoja.setText("-");
            lblLocalizacao.setText("-");
            lblCargoGestor.setText("-");
            limparPerfilOperacionalColaborador();
            mostrarMensagem(e.getMessage(), false);
        }
    }

    private void preencherCargos(List<GestaoFuncionariosBLL.CargoResumo> cargosDisponiveis) {
        Integer filtroCargoAtual = cbFiltroCargo.getValue() != null ? cbFiltroCargo.getValue().idCargo() : null;
        Integer cargoFormularioAtual = cbCargo.getValue() != null ? cbCargo.getValue().idCargo() : null;

        ObservableList<CargoOption> opcoesCargo = FXCollections.observableArrayList(
                cargosDisponiveis.stream()
                        .map(cargo -> new CargoOption(cargo.idCargo(), cargo.nome(), cargo.tipo()))
                        .toList()
        );
        cbCargo.setItems(opcoesCargo);

        ObservableList<FiltroCargoOption> opcoesFiltro = FXCollections.observableArrayList();
        opcoesFiltro.add(new FiltroCargoOption(null, FILTRO_TODOS));
        for (CargoOption cargo : opcoesCargo) {
            opcoesFiltro.add(new FiltroCargoOption(cargo.idCargo(), cargo.nome()));
        }
        cbFiltroCargo.setItems(opcoesFiltro);

        selecionarFiltroCargo(filtroCargoAtual);
        selecionarCargoFormulario(cargoFormularioAtual);
    }

    private void selecionarFiltroCargo(Integer idCargo) {
        for (FiltroCargoOption option : cbFiltroCargo.getItems()) {
            if (Objects.equals(option.idCargo(), idCargo)) {
                cbFiltroCargo.setValue(option);
                return;
            }
        }

        if (!cbFiltroCargo.getItems().isEmpty()) {
            cbFiltroCargo.setValue(cbFiltroCargo.getItems().get(0));
        }
    }

    private void selecionarCargoFormulario(Integer idCargo) {
        for (CargoOption option : cbCargo.getItems()) {
            if (Objects.equals(option.idCargo(), idCargo)) {
                cbCargo.setValue(option);
                return;
            }
        }

        if (!cbCargo.getItems().isEmpty()) {
            cbCargo.setValue(cbCargo.getItems().get(0));
        } else {
            cbCargo.setValue(null);
        }
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
        atualizarEstadoAcoes();
    }

    private boolean contemTexto(String valor, String pesquisa) {
        return valor != null && valor.toLowerCase().contains(pesquisa);
    }

    private ColaboradorLinha mapearLinhaTabela(GestaoFuncionariosBLL.ColaboradorResumo colaborador) {
        return new ColaboradorLinha(
                colaborador.idUtilizador(),
                colaborador.nome(),
                colaborador.email(),
                valorOuTraco(colaborador.telemovel()),
                colaborador.idCargo(),
                colaborador.cargoNome(),
                formatarEstado(colaborador.estado())
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
        txtNome.clear();
        txtEmail.clear();
        txtTelemovel.clear();
        txtPassword.clear();
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
    }

    private String mapearEstadoParaPersistencia(String estado) {
        if (ESTADO_INATIVO.equalsIgnoreCase(estado)) {
            return "inativo";
        }
        return "ativo";
    }

    private String formatarEstado(String estado) {
        if ("inativo".equalsIgnoreCase(estado)) {
            return ESTADO_INATIVO;
        }
        return ESTADO_ATIVO;
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
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
                String evento = horario.getIdTurno().getHoraInicio()
                        + " - "
                        + horario.getIdTurno().getHoraFim()
                        + " | "
                        + horario.getIdLojautilizador().getIdLoja().getNome();
                eventos.computeIfAbsent(horario.getDataTurno(), chave -> new java.util.ArrayList<>()).add(evento);
            }

            CalendarioSemanalHelper.preencherCalendario(
                    boxSemanaColaborador,
                    semanaColaboradorInicio,
                    eventos,
                    "Sem turno"
            );

            if (horarios.isEmpty()) {
                lblResumoHorarioColaborador.setText(
                        nomeColaborador + " não tem horário publicado nesta semana."
                );
            } else {
                lblResumoHorarioColaborador.setText(
                        nomeColaborador + " tem " + horarios.size() + " turno(s) publicados nesta semana."
                );
            }
        } catch (Exception e) {
            limparHorarioSemanalColaborador();
            lblResumoHorarioColaborador.setText("Não foi possível carregar o horário semanal deste colaborador.");
        }
    }

    private void limparHorarioSemanalColaborador() {
        CalendarioSemanalHelper.preencherCalendario(
                boxSemanaColaborador,
                semanaColaboradorInicio,
                Map.of(),
                "Seleciona um colaborador"
        );
        lblResumoHorarioColaborador.setText("Seleciona um colaborador para veres o horário publicado da semana.");
    }

    private void carregarPerfilOperacionalColaborador(ColaboradorLinha colaborador) {
        if (colaborador == null || colaborador.idUtilizador() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) {
            limparPerfilOperacionalColaborador();
            return;
        }

        Integer idGestor = utilizadorLogado.getId();
        Integer idColaborador = colaborador.idUtilizador();

        try {
            List<DayOff> historicoFolgas = dayOffBLL.listarPedidosPorUtilizador(idColaborador);
            Map<Integer, DayOff> pendentesFolga = dayOffBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> idColaborador.equals(item.getIdUtilizador()))
                    .collect(java.util.stream.Collectors.toMap(DayOff::getIdDayoff, item -> item, (a, b) -> a));

            List<FolgaLinha> linhasFolga = historicoFolgas.stream()
                    .sorted(java.util.Comparator.comparing(DayOff::getDataAusencia, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                    .limit(12)
                    .map(item -> new FolgaLinha(
                            item.getIdDayoff(),
                            formatarData(item.getDataAusencia()),
                            valorOuTraco(item.getTipo()),
                            valorOuTraco(item.getEstado()),
                            pendentesFolga.containsKey(item.getIdDayoff())
                    ))
                    .toList();
            folgasColaborador.setAll(linhasFolga);

            List<Preferencia> historicoPreferencias = preferenciaBLL.listarPreferenciasPorUtilizador(idColaborador);
            Map<Integer, Preferencia> pendentesPreferencia = preferenciaBLL.listarPreferenciasPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> item.getIdUtilizador() != null && idColaborador.equals(item.getIdUtilizador().getId()))
                    .collect(java.util.stream.Collectors.toMap(Preferencia::getId, item -> item, (a, b) -> a));

            List<PreferenciaLinha> linhasPreferencia = historicoPreferencias.stream()
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
                    ))
                    .toList();
            preferenciasColaborador.setAll(linhasPreferencia);

            List<Permuta> historicoPermutas = permutaBLL.listarPedidosEnviados(idColaborador);
            Map<Integer, Permuta> pendentesPermuta = permutaBLL.listarPedidosPendentesParaAprovacao(idGestor).stream()
                    .filter(item -> item.getIdHorarioOrigem() != null
                            && item.getIdHorarioOrigem().getIdLojautilizador() != null
                            && item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() != null
                            && idColaborador.equals(item.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId()))
                    .collect(java.util.stream.Collectors.toMap(Permuta::getId, item -> item, (a, b) -> a));

            List<PermutaLinha> linhasPermuta = historicoPermutas.stream()
                    .limit(12)
                    .map(item -> new PermutaLinha(
                            item.getId(),
                            item.getIdHorarioOrigem() != null ? formatarData(item.getIdHorarioOrigem().getDataTurno()) : "-",
                            valorOuTraco(item.getEstado()),
                            formatarTurnosPermuta(item),
                            pendentesPermuta.containsKey(item.getId())
                    ))
                    .toList();
            permutasColaborador.setAll(linhasPermuta);

            long pendentes = linhasFolga.stream().filter(FolgaLinha::pendenteAprovacao).count()
                    + linhasPreferencia.stream().filter(PreferenciaLinha::pendenteAprovacao).count()
                    + linhasPermuta.stream().filter(PermutaLinha::pendenteAprovacao).count();

            lblPerfilOperacionalResumo.setText(
                    "Perfil operacional de "
                            + colaborador.nome()
                            + " | "
                            + linhasFolga.size() + " folga(s), "
                            + linhasPreferencia.size() + " preferencia(s), "
                            + linhasPermuta.size() + " permuta(s) | "
                            + pendentes + " pendente(s)."
            );
        } catch (Exception ex) {
            limparPerfilOperacionalColaborador();
            lblPerfilOperacionalResumo.setText("Nao foi possivel carregar o perfil operacional deste colaborador.");
        }
    }

    private void limparPerfilOperacionalColaborador() {
        folgasColaborador.clear();
        preferenciasColaborador.clear();
        permutasColaborador.clear();
        if (lblPerfilOperacionalResumo != null) {
            lblPerfilOperacionalResumo.setText("Seleciona um colaborador para veres folgas, preferencias, permutas e decisoes pendentes.");
        }
    }

    private void decidirFolga(FolgaLinha linha, boolean aprovar) {
        if (linha == null || linha.idDayOff() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }

        boolean confirmado = DialogosHelper.confirmarAcao(
                obterJanela(),
                aprovar ? "Aprovar folga" : "Rejeitar folga",
                aprovar ? "Deseja aprovar este pedido de folga?" : "Deseja rejeitar este pedido de folga?",
                "A decisao sera registada de imediato."
        );
        if (!confirmado) {
            return;
        }

        try {
            if (aprovar) {
                dayOffBLL.aprovarPedidoFolga(linha.idDayOff(), utilizadorLogado.getId());
                mostrarMensagem("Pedido de folga aprovado com sucesso.", true);
            } else {
                dayOffBLL.rejeitarPedidoFolga(linha.idDayOff(), utilizadorLogado.getId());
                mostrarMensagem("Pedido de folga rejeitado com sucesso.", true);
            }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) {
            mostrarMensagem(ex.getMessage(), false);
        }
    }

    private void decidirPreferencia(PreferenciaLinha linha, boolean aprovar) {
        if (linha == null || linha.idPreferencia() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }

        boolean confirmado = DialogosHelper.confirmarAcao(
                obterJanela(),
                aprovar ? "Aprovar preferencia" : "Rejeitar preferencia",
                aprovar ? "Deseja aprovar esta preferencia?" : "Deseja rejeitar esta preferencia?",
                "A decisao sera registada de imediato."
        );
        if (!confirmado) {
            return;
        }

        try {
            if (aprovar) {
                preferenciaBLL.aprovarPreferencia(linha.idPreferencia(), utilizadorLogado.getId(), null);
                mostrarMensagem("Preferencia aprovada com sucesso.", true);
            } else {
                preferenciaBLL.rejeitarPreferencia(linha.idPreferencia(), utilizadorLogado.getId(), null);
                mostrarMensagem("Preferencia rejeitada com sucesso.", true);
            }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) {
            mostrarMensagem(ex.getMessage(), false);
        }
    }

    private void decidirPermuta(PermutaLinha linha, boolean aprovar) {
        if (linha == null || linha.idPermuta() == null || utilizadorLogado == null || utilizadorLogado.getId() == null) {
            return;
        }
        ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            return;
        }

        boolean confirmado = DialogosHelper.confirmarAcao(
                obterJanela(),
                aprovar ? "Aprovar permuta" : "Rejeitar permuta",
                aprovar ? "Deseja aprovar este pedido de permuta?" : "Deseja rejeitar este pedido de permuta?",
                "A decisao sera registada de imediato."
        );
        if (!confirmado) {
            return;
        }

        try {
            if (aprovar) {
                permutaBLL.aprovarPedidoPermuta(linha.idPermuta(), utilizadorLogado.getId());
                mostrarMensagem("Permuta aprovada com sucesso.", true);
            } else {
                permutaBLL.rejeitarPedidoPermuta(linha.idPermuta(), utilizadorLogado.getId());
                mostrarMensagem("Permuta rejeitada com sucesso.", true);
            }
            carregarPerfilOperacionalColaborador(selecionado);
        } catch (IllegalArgumentException ex) {
            mostrarMensagem(ex.getMessage(), false);
        }
    }

    private Button criarBotaoAcao(String texto, boolean destaque) {
        Button botao = new Button(texto);
        botao.getStyleClass().add(destaque ? "botao-acao" : "botao-secundario");
        botao.getStyleClass().add("topo-botao-curto");
        return botao;
    }

    private String formatarData(LocalDate data) {
        if (data == null) {
            return "-";
        }
        return data.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return formatarData(dataInicio) + " -> " + formatarData(dataFim);
    }

    private String formatarTurnosPermuta(Permuta permuta) {
        if (permuta == null || permuta.getIdHorarioOrigem() == null || permuta.getIdHorarioDestino() == null) {
            return "-";
        }

        String origem = permuta.getIdHorarioOrigem().getIdTurno() != null
                ? permuta.getIdHorarioOrigem().getIdTurno().getHoraInicio() + "-" + permuta.getIdHorarioOrigem().getIdTurno().getHoraFim()
                : "-";
        String destino = permuta.getIdHorarioDestino().getIdTurno() != null
                ? permuta.getIdHorarioDestino().getIdTurno().getHoraInicio() + "-" + permuta.getIdHorarioDestino().getIdTurno().getHoraFim()
                : "-";
        return origem + " -> " + destino;
    }

    private void esconderMensagem() {
        lblMensagem.setManaged(false);
        lblMensagem.setVisible(false);
        lblMensagem.setText("");
    }

    private void mostrarMensagem(String mensagem, boolean sucesso) {
        lblMensagem.setText(mensagem);
        lblMensagem.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagem.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagem.setManaged(true);
        lblMensagem.setVisible(true);
    }

    private Window obterJanela() {
        if (lblNomeLoja == null || lblNomeLoja.getScene() == null) {
            return null;
        }
        return lblNomeLoja.getScene().getWindow();
    }

    private record CargoOption(Integer idCargo, String nome, String tipo) {
        @Override
        public String toString() {
            return nome;
        }
    }

    private record FiltroCargoOption(Integer idCargo, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    private record ColaboradorLinha(
            Integer idUtilizador,
            String nome,
            String email,
            String telemovel,
            Integer idCargo,
            String cargo,
            String estado
    ) {
        private boolean ativo() {
            return ESTADO_ATIVO.equalsIgnoreCase(estado);
        }
    }

    private record FolgaLinha(
            Integer idDayOff,
            String data,
            String tipo,
            String estado,
            boolean pendenteAprovacao
    ) {
    }

    private record PreferenciaLinha(
            Integer idPreferencia,
            String tipo,
            String periodo,
            String estado,
            boolean pendenteAprovacao
    ) {
    }

    private record PermutaLinha(
            Integer idPermuta,
            String dataTurno,
            String estado,
            String turnos,
            boolean pendenteAprovacao
    ) {
    }
}
