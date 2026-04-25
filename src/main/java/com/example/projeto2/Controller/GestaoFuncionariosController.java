package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GestaoFuncionariosBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    private final GestaoFuncionariosBLL gestaoFuncionariosBLL;
    private final HorarioBLL horarioBLL;
    private final ObservableList<ColaboradorLinha> colaboradores = FXCollections.observableArrayList();
    private final FilteredList<ColaboradorLinha> colaboradoresFiltrados = new FilteredList<>(colaboradores, colaborador -> true);
    private Utilizador utilizadorLogado;
    private Integer idColaboradorEmEdicao;
    private LocalDate semanaColaboradorInicio;

    public GestaoFuncionariosController(GestaoFuncionariosBLL gestaoFuncionariosBLL,
                                        HorarioBLL horarioBLL) {
        this.gestaoFuncionariosBLL = gestaoFuncionariosBLL;
        this.horarioBLL = horarioBLL;
    }

    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltros();
        configurarFormulario();

        tabelaColaboradores.setItems(colaboradoresFiltrados);
        tabelaColaboradores.setPlaceholder(new Label("Ainda nao existem colaboradores associados a esta loja."));
        tabelaColaboradores.getSelectionModel().selectedItemProperty().addListener((observavel, anterior, selecionado) -> {
            if (selecionado != null) {
                preencherFormulario(selecionado);
                carregarHorarioSemanalColaborador(selecionado.idUtilizador(), selecionado.nome());
            } else {
                limparHorarioSemanalColaborador();
            }
        });

        esconderMensagem();
        limparFormularioParaNovo();
        semanaColaboradorInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        atualizarCabecalhoSemanaColaborador();
        limparHorarioSemanalColaborador();
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
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            CargoOption cargoSelecionado = cbCargo.getValue();
            if (cargoSelecionado == null || cargoSelecionado.idCargo() == null) {
                throw new IllegalArgumentException("Seleciona um cargo valido para o colaborador.");
            }

            boolean novoColaborador = idColaboradorEmEdicao == null;
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
            mostrarMensagem("Nao foi possivel guardar o registo do colaborador.", false);
        }
    }

    @FXML
    public void onDesativarClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
            }

            ColaboradorLinha selecionado = tabelaColaboradores.getSelectionModel().getSelectedItem();
            if (selecionado == null) {
                throw new IllegalArgumentException("Seleciona um colaborador primeiro.");
            }

            gestaoFuncionariosBLL.desativarColaborador(utilizadorLogado.getId(), selecionado.idUtilizador());
            mostrarMensagem("Colaborador desativado com sucesso.", true);
            carregarDados(null);
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Nao foi possivel desativar o colaborador selecionado.", false);
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
                throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
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
        } catch (IllegalArgumentException e) {
            colaboradores.clear();
            preencherCargos(List.of());
            atualizarFiltros();
            limparFormularioParaNovo();
            lblNomeLoja.setText("-");
            lblLocalizacao.setText("-");
            lblCargoGestor.setText("-");
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
                        nomeColaborador + " nao tem horario publicado nesta semana."
                );
            } else {
                lblResumoHorarioColaborador.setText(
                        nomeColaborador + " tem " + horarios.size() + " turno(s) publicados nesta semana."
                );
            }
        } catch (Exception e) {
            limparHorarioSemanalColaborador();
            lblResumoHorarioColaborador.setText("Nao foi possivel carregar o horario semanal deste colaborador.");
        }
    }

    private void limparHorarioSemanalColaborador() {
        CalendarioSemanalHelper.preencherCalendario(
                boxSemanaColaborador,
                semanaColaboradorInicio,
                Map.of(),
                "Seleciona um colaborador"
        );
        lblResumoHorarioColaborador.setText("Seleciona um colaborador para veres o horario publicado da semana.");
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
}
