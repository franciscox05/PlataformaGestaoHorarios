package com.example.projeto2.Controller;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.Controller.support.CalendarioSemanalHelper;
import com.example.projeto2.Controller.support.DialogosHelper;
import com.example.projeto2.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class GeracaoHorariosController {

    @FXML
    private Label lblLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private ComboBox<MesOption> cbMes;

    @FXML
    private Spinner<Integer> spAno;

    @FXML
    private Button btnVerProposta;

    @FXML
    private Button btnGerarProposta;

    @FXML
    private Label lblFeedback;

    @FXML
    private VBox painelValidacaoSupervisor;

    @FXML
    private TextArea txtObservacoesSupervisor;

    @FXML
    private Button btnAprovarProposta;

    @FXML
    private Button btnRejeitarProposta;

    @FXML
    private Label lblFeedbackValidacao;

    @FXML
    private Label lblEstadoProposta;

    @FXML
    private Label lblOrigemPlaneamento;

    @FXML
    private Label lblGeradoPor;

    @FXML
    private Label lblDataGeracao;

    @FXML
    private Label lblDecididoPor;

    @FXML
    private Label lblDataDecisao;

    @FXML
    private Label lblObservacoesSupervisor;

    @FXML
    private Label lblResumoGeracao;

    @FXML
    private Label lblTotalColaboradores;

    @FXML
    private Label lblTotalTurnos;

    @FXML
    private Label lblTotalDiasCobertos;

    @FXML
    private ComboBox<FiltroColaboradorOption> cbFiltroColaborador;

    @FXML
    private Label lblSemanaPlaneamento;

    @FXML
    private HBox boxSemanaPlaneamento;

    @FXML
    private TableView<GeracaoHorariosBLL.ResumoColaborador> tabelaResumoColaboradores;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoColaborador;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoCargo;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoTurnos;

    @FXML
    private TableColumn<GeracaoHorariosBLL.ResumoColaborador, String> colResumoHoras;

    private final GeracaoHorariosBLL geracaoHorariosBLL;

    private Utilizador utilizadorLogado;
    private GeracaoHorariosBLL.PropostaResultado propostaAtual;
    private boolean podeGerar;
    private boolean podeValidar;
    private LocalDate semanaPlaneamentoInicio;

    public GeracaoHorariosController(GeracaoHorariosBLL geracaoHorariosBLL) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
    }

    @FXML
    public void initialize() {
        configurarFiltros();
        configurarTabelas();
        limparResultado();
        esconderFeedback();
        esconderFeedbackValidacao();
        painelValidacaoSupervisor.setManaged(false);
        painelValidacaoSupervisor.setVisible(false);

        tabelaResumoColaboradores.setPlaceholder(new Label("Ainda não existe proposta gerada para apresentar o resumo da equipa."));
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarContextoInicial();
    }

    @FXML
    public void onVerPropostaClick() {
        carregarPropostaSelecionada();
    }

    @FXML
    public void onGerarPropostaClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Gerar proposta",
                    "Deseja gerar a proposta mensal?",
                    "A proposta será recalculada para o período selecionado."
            )) {
                return;
            }

            MesOption mesSelecionado = obterMesSelecionado();
            GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.gerarProposta(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    mesSelecionado.numero()
            );

            preencherResultado(resultado);
            esconderFeedbackValidacao();
            mostrarSucesso("Proposta mensal gerada com sucesso.");
        } catch (IllegalArgumentException e) {
            if (tentarCarregarPlaneamentoExistente()) {
                esconderFeedbackValidacao();
                mostrarErro(e.getMessage() + " O planeamento atual foi carregado abaixo para te ajudar a analisar a situação.");
            } else {
                mostrarErro(e.getMessage());
            }
        } catch (Exception e) {
            mostrarErro("Não foi possível gerar a proposta mensal.");
        }
    }

    @FXML
    public void onAprovarPropostaClick() {
        decidirProposta(true);
    }

    @FXML
    public void onRejeitarPropostaClick() {
        decidirProposta(false);
    }

    @FXML
    public void onSemanaPlaneamentoAnteriorClick() {
        semanaPlaneamentoInicio = semanaPlaneamentoInicio.minusWeeks(1);
        atualizarCabecalhoSemanaPlaneamento();
        aplicarFiltroColaborador();
    }

    @FXML
    public void onSemanaPlaneamentoSeguinteClick() {
        semanaPlaneamentoInicio = semanaPlaneamentoInicio.plusWeeks(1);
        atualizarCabecalhoSemanaPlaneamento();
        aplicarFiltroColaborador();
    }

    private void configurarFiltros() {
        cbMes.setItems(FXCollections.observableArrayList(
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

        spAno.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                LocalDate.now().getYear(),
                LocalDate.now().getYear() + 5,
                LocalDate.now().getYear()
        ));
        spAno.setEditable(true);

        cbMes.valueProperty().addListener((observavel, antigo, novo) -> invalidarPropostaAtual());
        spAno.valueProperty().addListener((observavel, antigo, novo) -> invalidarPropostaAtual());

        cbFiltroColaborador.setItems(FXCollections.observableArrayList(FiltroColaboradorOption.todos()));
        cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        cbFiltroColaborador.valueProperty().addListener((observavel, antigo, novo) -> aplicarFiltroColaborador());
        reposicionarSemanaPlaneamentoParaMesSelecionado();
    }

    private void configurarTabelas() {
        colResumoColaborador.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().nome()));

        colResumoCargo.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().cargo()));

        colResumoTurnos.setCellValueFactory(cellData ->
                new SimpleStringProperty(String.valueOf(cellData.getValue().turnos())));

        colResumoHoras.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().horasFormatadas()));
    }

    private void carregarContextoInicial() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            GeracaoHorariosBLL.GeracaoContexto contexto = geracaoHorariosBLL.obterContexto(utilizadorLogado.getId());
            podeGerar = contexto.podeGerar();
            podeValidar = contexto.podeValidar();

            lblLoja.setText(contexto.nomeLoja());
            lblLocalizacao.setText(contexto.localizacao());
            spAno.getValueFactory().setValue(contexto.anoAtual());
            selecionarMes(contexto.mesAtual());
            configurarPermissoesEcra();
            carregarPropostaSelecionada();
        } catch (IllegalArgumentException e) {
            bloquearEcraSemPermissao(e.getMessage());
        } catch (Exception e) {
            bloquearEcraSemPermissao("Não foi possível carregar o contexto da geração de horários.");
        }
    }

    private void carregarPropostaSelecionada() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            MesOption mesSelecionado = obterMesSelecionado();
            GeracaoHorariosBLL.PropostaResultado resultado = geracaoHorariosBLL.obterPlaneamento(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    mesSelecionado.numero()
            );

            if (resultado == null) {
                limparResultado();
                mostrarInformacao("Ainda não existe proposta nem horários publicados para o período selecionado.");
                return;
            }

            preencherResultado(resultado);
            mostrarInformacao("Planeamento do período carregado com sucesso.");
        } catch (IllegalArgumentException e) {
            limparResultado();
            mostrarErro(e.getMessage());
        } catch (Exception e) {
            limparResultado();
            mostrarErro("Não foi possível carregar a proposta selecionada.");
        }
    }

    private void decidirProposta(boolean aprovar) {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            if (propostaAtual == null || propostaAtual.idProposta() == null) {
                throw new IllegalArgumentException("Seleciona primeiro uma proposta para validar.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    aprovar ? "Aprovar proposta" : "Rejeitar proposta",
                    aprovar ? "Deseja aprovar e publicar esta proposta?" : "Deseja rejeitar esta proposta?",
                    aprovar
                            ? "Os horários serão publicados para a loja."
                            : "A proposta ficará rejeitada e não será publicada."
            )) {
                return;
            }

            GeracaoHorariosBLL.PropostaResultado resultado = aprovar
                    ? geracaoHorariosBLL.aprovarProposta(
                    utilizadorLogado.getId(),
                    propostaAtual.idProposta(),
                    txtObservacoesSupervisor.getText()
            )
                    : geracaoHorariosBLL.rejeitarProposta(
                    utilizadorLogado.getId(),
                    propostaAtual.idProposta(),
                    txtObservacoesSupervisor.getText()
            );

            preencherResultado(resultado);
            mostrarFeedbackValidacao(
                    aprovar
                            ? "Proposta aprovada e horários publicados com sucesso."
                            : "Proposta rejeitada com sucesso.",
                    true
            );
        } catch (IllegalArgumentException e) {
            mostrarFeedbackValidacao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarFeedbackValidacao("Não foi possível atualizar a decisão da proposta.", false);
        }
    }

    private MesOption obterMesSelecionado() {
        MesOption mesSelecionado = cbMes.getValue();
        if (mesSelecionado == null) {
            throw new IllegalArgumentException("Seleciona um mês para consultar ou gerar a proposta.");
        }
        return mesSelecionado;
    }

    private void selecionarMes(int numeroMes) {
        cbMes.getItems().stream()
                .filter(item -> item.numero() == numeroMes)
                .findFirst()
                .ifPresent(cbMes::setValue);
    }

    private void preencherResultado(GeracaoHorariosBLL.PropostaResultado resultado) {
        propostaAtual = resultado;
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        lblEstadoProposta.setText(resultado.estado());
        lblOrigemPlaneamento.setText(resultado.origemPlaneamento());
        lblGeradoPor.setText(resultado.geradoPor());
        lblDataGeracao.setText(resultado.dataGeracao());
        lblDecididoPor.setText(resultado.decididoPor());
        lblDataDecisao.setText(resultado.dataDecisao());
        lblObservacoesSupervisor.setText(resultado.observacoesSupervisor());
        lblResumoGeracao.setText(resultado.resumoGeracao() != null ? resultado.resumoGeracao() : "-");

        lblTotalColaboradores.setText(String.valueOf(resultado.resumo().colaboradores()));
        lblTotalTurnos.setText(String.valueOf(resultado.resumo().turnos()));
        lblTotalDiasCobertos.setText(String.valueOf(resultado.resumo().diasCobertos()));

        tabelaResumoColaboradores.setItems(FXCollections.observableArrayList(resultado.resumoColaboradores()));
        atualizarFiltroColaborador(resultado);
        aplicarFiltroColaborador();
        atualizarPainelValidacao();
    }

    private void limparResultado() {
        propostaAtual = null;
        lblEstadoProposta.setText("-");
        lblOrigemPlaneamento.setText("-");
        lblGeradoPor.setText("-");
        lblDataGeracao.setText("-");
        lblDecididoPor.setText("-");
        lblDataDecisao.setText("-");
        lblObservacoesSupervisor.setText("-");
        lblResumoGeracao.setText("Ainda não existe uma proposta carregada.");
        txtObservacoesSupervisor.clear();

        lblTotalColaboradores.setText("0");
        lblTotalTurnos.setText("0");
        lblTotalDiasCobertos.setText("0");

        tabelaResumoColaboradores.setItems(FXCollections.observableArrayList());
        cbFiltroColaborador.setItems(FXCollections.observableArrayList(FiltroColaboradorOption.todos()));
        cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        renderizarCalendarioPlaneamento(List.of());
        atualizarPainelValidacao();
    }

    private void invalidarPropostaAtual() {
        if (utilizadorLogado == null) {
            return;
        }

        limparResultado();
        reposicionarSemanaPlaneamentoParaMesSelecionado();
        esconderFeedback();
    }

    private void bloquearEcraSemPermissao(String mensagem) {
        lblLoja.setText("-");
        lblLocalizacao.setText("-");
        limparResultado();

        cbMes.setDisable(true);
        spAno.setDisable(true);
        btnVerProposta.setDisable(true);
        btnGerarProposta.setDisable(true);
        painelValidacaoSupervisor.setManaged(false);
        painelValidacaoSupervisor.setVisible(false);
        mostrarErro(mensagem);
    }

    private void configurarPermissoesEcra() {
        btnGerarProposta.setManaged(podeGerar);
        btnGerarProposta.setVisible(podeGerar);

        painelValidacaoSupervisor.setManaged(podeValidar);
        painelValidacaoSupervisor.setVisible(podeValidar);
        atualizarPainelValidacao();
    }

    private void atualizarPainelValidacao() {
        if (!podeValidar) {
            return;
        }

        boolean podeDecidirAgora = propostaAtual != null && propostaAtual.podeSerDecidida();
        btnAprovarProposta.setDisable(!podeDecidirAgora);
        btnRejeitarProposta.setDisable(!podeDecidirAgora);
        txtObservacoesSupervisor.setDisable(!podeDecidirAgora);

        if (podeDecidirAgora) {
            esconderFeedbackValidacao();
        } else if (propostaAtual == null) {
            txtObservacoesSupervisor.clear();
        }
    }

    private void mostrarSucesso(String mensagem) {
        mostrarFeedback(mensagem, "mensagem-sucesso");
    }

    private void mostrarErro(String mensagem) {
        mostrarFeedback(mensagem, "mensagem-erro");
    }

    private void mostrarInformacao(String mensagem) {
        mostrarFeedback(mensagem, null);
    }

    private void mostrarFeedback(String mensagem, String estilo) {
        lblFeedback.setText(mensagem);
        lblFeedback.getStyleClass().removeAll("mensagem-feedback", "mensagem-sucesso", "mensagem-erro");
        lblFeedback.getStyleClass().add("mensagem-feedback");
        if (estilo != null) {
            lblFeedback.getStyleClass().add(estilo);
        }
        lblFeedback.setVisible(true);
        lblFeedback.setManaged(true);
    }

    private void esconderFeedback() {
        lblFeedback.setVisible(false);
        lblFeedback.setManaged(false);
        lblFeedback.setText("");
    }

    private void mostrarFeedbackValidacao(String mensagem, boolean sucesso) {
        lblFeedbackValidacao.setText(mensagem);
        lblFeedbackValidacao.getStyleClass().removeAll("mensagem-feedback", "mensagem-sucesso", "mensagem-erro");
        lblFeedbackValidacao.getStyleClass().add("mensagem-feedback");
        lblFeedbackValidacao.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblFeedbackValidacao.setVisible(true);
        lblFeedbackValidacao.setManaged(true);
    }

    private void esconderFeedbackValidacao() {
        lblFeedbackValidacao.setVisible(false);
        lblFeedbackValidacao.setManaged(false);
        lblFeedbackValidacao.setText("");
    }

    private boolean tentarCarregarPlaneamentoExistente() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                return false;
            }

            GeracaoHorariosBLL.PropostaResultado existente = geracaoHorariosBLL.obterPlaneamento(
                    utilizadorLogado.getId(),
                    spAno.getValue(),
                    obterMesSelecionado().numero()
            );
            if (existente == null) {
                return false;
            }

            preencherResultado(existente);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void atualizarFiltroColaborador(GeracaoHorariosBLL.PropostaResultado resultado) {
        FiltroColaboradorOption selecionadoAnteriormente = cbFiltroColaborador.getValue();
        Integer idAnterior = selecionadoAnteriormente != null ? selecionadoAnteriormente.idColaborador() : null;

        var opcoes = FXCollections.<FiltroColaboradorOption>observableArrayList();
        opcoes.add(FiltroColaboradorOption.todos());
        resultado.resumoColaboradores().forEach(colaborador -> opcoes.add(
                new FiltroColaboradorOption(colaborador.idColaborador(), colaborador.nome())
        ));

        cbFiltroColaborador.setItems(opcoes);
        if (idAnterior != null) {
            cbFiltroColaborador.getItems().stream()
                    .filter(opcao -> idAnterior.equals(opcao.idColaborador()))
                    .findFirst()
                    .ifPresentOrElse(cbFiltroColaborador::setValue, () -> cbFiltroColaborador.setValue(FiltroColaboradorOption.todos()));
        } else {
            cbFiltroColaborador.setValue(FiltroColaboradorOption.todos());
        }
    }

    private void aplicarFiltroColaborador() {
        if (propostaAtual == null) {
            renderizarCalendarioPlaneamento(List.of());
            return;
        }

        FiltroColaboradorOption filtro = cbFiltroColaborador.getValue();
        List<GeracaoHorariosBLL.HorarioLinha> linhas = propostaAtual.linhas();
        if (filtro != null && !filtro.isTodos()) {
            linhas = linhas.stream()
                    .filter(linha -> filtro.idColaborador().equals(linha.idColaborador()))
                    .toList();
        }

        LocalDate dataInicioSemana = semanaPlaneamentoInicio != null
                ? semanaPlaneamentoInicio
                : CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), obterMesSelecionado().numero(), 1));
        LocalDate dataFimSemana = dataInicioSemana.plusDays(6);

        List<GeracaoHorariosBLL.HorarioLinha> linhasDaSemana = linhas.stream()
                .filter(linha -> linha.data() != null
                        && !linha.data().isBefore(dataInicioSemana)
                        && !linha.data().isAfter(dataFimSemana))
                .toList();

        renderizarCalendarioPlaneamento(linhasDaSemana);
    }

    private void reposicionarSemanaPlaneamentoParaMesSelecionado() {
        try {
            if (cbMes.getValue() == null || spAno.getValue() == null) {
                semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
            } else {
                semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.of(spAno.getValue(), cbMes.getValue().numero(), 1));
            }
        } catch (Exception e) {
            semanaPlaneamentoInicio = CalendarioSemanalHelper.inicioSemana(LocalDate.now());
        }
        atualizarCabecalhoSemanaPlaneamento();
    }

    private void atualizarCabecalhoSemanaPlaneamento() {
        if (lblSemanaPlaneamento != null) {
            lblSemanaPlaneamento.setText(CalendarioSemanalHelper.formatarIntervaloSemana(semanaPlaneamentoInicio));
        }
    }

    private void renderizarCalendarioPlaneamento(List<GeracaoHorariosBLL.HorarioLinha> linhas) {
        Map<LocalDate, List<String>> eventos = new LinkedHashMap<>();
        for (GeracaoHorariosBLL.HorarioLinha linha : linhas) {
            String evento = linha.periodo() + " | " + linha.colaborador() + " (" + linha.cargo() + ")";
            eventos.computeIfAbsent(linha.data(), chave -> new java.util.ArrayList<>()).add(evento);
        }

        CalendarioSemanalHelper.preencherCalendario(
                boxSemanaPlaneamento,
                semanaPlaneamentoInicio,
                eventos,
                "Sem turnos"
        );
    }

    private Window obterJanela() {
        if (lblLoja == null || lblLoja.getScene() == null) {
            return null;
        }
        return lblLoja.getScene().getWindow();
    }

    private record MesOption(int numero, String nome) {
        @Override
        public String toString() {
            return nome;
        }
    }

    private record FiltroColaboradorOption(Integer idColaborador, String nome) {

        private static FiltroColaboradorOption todos() {
            return new FiltroColaboradorOption(null, "Toda a equipa");
        }

        private boolean isTodos() {
            return idColaborador == null;
        }

        @Override
        public String toString() {
            return nome;
        }
    }
}
