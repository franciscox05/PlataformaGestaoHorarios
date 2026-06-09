package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class GestaoLojaController {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private Label lblNomeLoja;

    @FXML
    private Label lblLocalizacao;

    @FXML
    private Label lblCargoGestor;

    @FXML
    private TextField txtHoraAbertura;

    @FXML
    private TextField txtHoraFecho;

    @FXML
    private Label lblMensagem;

    @FXML
    private VBox regrasContainer;

    @FXML
    private TextField txtDescricaoExcecao;

    @FXML
    private DatePicker dpDataInicioExcecao;

    @FXML
    private DatePicker dpDataFimExcecao;

    @FXML
    private CheckBox chkLojaEncerrada;

    @FXML
    private TextField txtHoraAberturaExcecao;

    @FXML
    private TextField txtHoraFechoExcecao;

    @FXML
    private TextField txtMinimoExcecao;

    @FXML
    private TextArea txtObservacoesExcecao;

    @FXML
    private Label lblMensagemExcecao;

    @FXML
    private VBox emptyStateExcecoes;

    @FXML
    private TableView<GestaoLojaService.HorarioEspecialResumo> tabelaHorariosEspeciais;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colPeriodoExcecao;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colOperacaoExcecao;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colHorarioExcecao;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colMinimoExcecao;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colDescricaoExcecao;

    @FXML
    private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colTurnosCompativeisExcecao;

    private final GestaoLojaService gestaoLojaBLL;
    private final Map<Integer, TextField> camposValor = new LinkedHashMap<>();
    private final Map<Integer, TextArea> camposObservacoes = new LinkedHashMap<>();
    private Utilizador utilizadorLogado;
    private Integer idHorarioEspecialEmEdicao;

    public GestaoLojaController(GestaoLojaService gestaoLojaBLL) {
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    @FXML
    public void initialize() {
        esconderMensagem();
        esconderMensagemExcecao();
        configurarCamposHora();
        configurarOcultacaoFeedback();
        configurarTabelaHorariosEspeciais();
        configurarFormularioEncerrada();
        limparFormularioHorarioEspecial();

        tabelaHorariosEspeciais.setPlaceholder(new Label("Ainda não existem exceções de horário configuradas para esta loja."));
    }

    public void setUtilizadorLogado(Utilizador utilizadorLogado) {
        this.utilizadorLogado = utilizadorLogado;
        carregarDados();
    }

    @FXML
    public void onGuardarClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Guardar configuração",
                    "Deseja guardar a configuração da loja?",
                    "As regras base e o horário de funcionamento serão atualizados."
            )) {
                return;
            }

            LocalTime horaAbertura = parseHora(txtHoraAbertura.getText(), "abertura");
            LocalTime horaFecho = parseHora(txtHoraFecho.getText(), "fecho");

            List<GestaoLojaService.ConfiguracaoRegraRequest> regras = camposValor.entrySet().stream()
                    .map(entry -> {
                        Integer idRegra = entry.getKey();
                        Integer valorEspecifico = parseInteiroOpcional(entry.getValue().getText(), idRegra);
                        TextArea campoObservacoes = camposObservacoes.get(idRegra);
                        String observacoes = campoObservacoes != null ? campoObservacoes.getText() : null;
                        return new GestaoLojaService.ConfiguracaoRegraRequest(idRegra, valorEspecifico, observacoes);
                    })
                    .toList();

            gestaoLojaBLL.guardarConfiguracao(
                    utilizadorLogado.getId(),
                    new GestaoLojaService.ConfiguracaoLojaRequest(horaAbertura, horaFecho, regras)
            );

            mostrarMensagem("Configuração da loja guardada com sucesso.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível guardar a configuração da loja.", false);
        }
    }

    @FXML
    public void onGuardarHorarioEspecialClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            boolean novaExcecao = idHorarioEspecialEmEdicao == null;
            String cabecalho = novaExcecao ? "Deseja guardar esta exceção?" : "Deseja atualizar esta exceção?";
            String conteudo = novaExcecao
                    ? "A exceção ficará disponível para o planeamento da loja."
                    : "As alterações ficarão disponíveis para o planeamento da loja.";

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    novaExcecao ? "Guardar exceção" : "Atualizar exceção",
                    cabecalho,
                    conteudo
            )) {
                return;
            }

            gestaoLojaBLL.guardarHorarioEspecial(
                    utilizadorLogado.getId(),
                    new GestaoLojaService.ConfiguracaoHorarioEspecialRequest(
                            idHorarioEspecialEmEdicao,
                            txtDescricaoExcecao.getText(),
                            dpDataInicioExcecao.getValue(),
                            dpDataFimExcecao.getValue(),
                            chkLojaEncerrada.isSelected(),
                            parseHoraOpcional(txtHoraAberturaExcecao.getText(), "abertura especial"),
                            parseHoraOpcional(txtHoraFechoExcecao.getText(), "fecho especial"),
                            parseInteiroPositivoOpcional(txtMinimoExcecao.getText(), "mínimo especial por turno"),
                            txtObservacoesExcecao.getText()
                    )
            );

            mostrarMensagemExcecao(
                    novaExcecao
                            ? "Exceção guardada com sucesso."
                            : "Exceção atualizada com sucesso.",
                    true
            );
            carregarDados();
            limparFormularioHorarioEspecial();
        } catch (IllegalArgumentException e) {
            mostrarMensagemExcecao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagemExcecao("Não foi possível guardar a exceção de horário.", false);
        }
    }

    @FXML
    public void onLimparHorarioEspecialClick() {
        limparFormularioHorarioEspecial();
        esconderMensagemExcecao();
    }

    @FXML
    public void onRemoverHorarioEspecialClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            if (idHorarioEspecialEmEdicao == null) {
                throw new IllegalArgumentException("Seleciona uma exceção antes de a remover.");
            }

            if (!DialogosHelper.confirmarAcao(
                    obterJanela(),
                    "Remover exceção",
                    "Deseja remover este horário especial?",
                    "Esta ação vai retirar a exceção selecionada do planeamento da loja."
            )) {
                return;
            }

            gestaoLojaBLL.removerHorarioEspecial(utilizadorLogado.getId(), idHorarioEspecialEmEdicao);
            mostrarMensagemExcecao("Exceção removida com sucesso.", true);
            carregarDados();
            limparFormularioHorarioEspecial();
        } catch (IllegalArgumentException e) {
            mostrarMensagemExcecao(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagemExcecao("Não foi possível remover a exceção selecionada.", false);
        }
    }

    private void carregarDados() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }

            GestaoLojaService.GestaoLojaResumo resumo = gestaoLojaBLL.obterResumo(utilizadorLogado.getId());

            lblNomeLoja.setText(resumo.nomeLoja());
            lblLocalizacao.setText(resumo.localizacao());
            lblCargoGestor.setText(resumo.cargoGestor());
            txtHoraAbertura.setText(resumo.horaAbertura());
            txtHoraFecho.setText(resumo.horaFecho());

            preencherRegras(resumo.regras());
            preencherHorariosEspeciais(resumo.horariosEspeciais());
        } catch (IllegalArgumentException e) {
            lblNomeLoja.setText("-");
            lblLocalizacao.setText("-");
            lblCargoGestor.setText("-");
            txtHoraAbertura.clear();
            txtHoraFecho.clear();
            preencherRegras(List.of());
            preencherHorariosEspeciais(List.of());
            mostrarMensagem(e.getMessage(), false);
        }
    }

    private void preencherRegras(List<GestaoLojaService.RegraLojaResumo> regras) {
        regrasContainer.getChildren().clear();
        camposValor.clear();
        camposObservacoes.clear();

        if (regras == null || regras.isEmpty()) {
            Label semRegras = new Label("Não existem regras base configuradas para apresentar nesta loja.");
            semRegras.getStyleClass().add("subtitulo");
            regrasContainer.getChildren().add(semRegras);
            return;
        }

        for (GestaoLojaService.RegraLojaResumo regra : regras) {
            regrasContainer.getChildren().add(criarCardRegra(regra));
        }
    }

    private VBox criarCardRegra(GestaoLojaService.RegraLojaResumo regra) {
        VBox card = new VBox(16);
        card.getStyleClass().add("bento-card");
        card.setPadding(new Insets(28));

        Label lblTitulo = new Label(regra.descricao());
        lblTitulo.getStyleClass().addAll("card-titulo", "config-card-titulo");
        lblTitulo.setWrapText(true);

        String valorPadrao = regra.valorPadrao() != null ? String.valueOf(regra.valorPadrao()) : "sem valor base";
        Label lblDetalhe = new Label("Tipo: " + formatarTipo(regra.tipo()) + " | Valor base: " + valorPadrao);
        lblDetalhe.getStyleClass().addAll("subtitulo", "config-card-desc");
        lblDetalhe.setWrapText(true);

        Label lblValor = new Label("Valor específico da loja");
        lblValor.getStyleClass().add("campo-titulo");

        TextField txtValor = new TextField();
        txtValor.getStyleClass().add("campo-input");
        txtValor.setPromptText("Usar valor base");
        if (regra.valorEspecifico() != null) {
            txtValor.setText(String.valueOf(regra.valorEspecifico()));
        }

        Label lblObs = new Label("Observações");
        lblObs.getStyleClass().add("campo-titulo");

        TextArea txtObs = new TextArea();
        txtObs.getStyleClass().add("campo-textarea");
        txtObs.setPromptText("Notas internas sobre esta regra");
        txtObs.setWrapText(true);
        txtObs.setPrefRowCount(2);
        if (regra.observacoes() != null) {
            txtObs.setText(regra.observacoes());
        }

        HBox linhaValor = new HBox(12, lblValor, txtValor);
        linhaValor.setFillHeight(true);
        HBox.setHgrow(txtValor, Priority.ALWAYS);

        card.getChildren().addAll(lblTitulo, lblDetalhe, linhaValor, lblObs, txtObs);

        camposValor.put(regra.idRegra(), txtValor);
        camposObservacoes.put(regra.idRegra(), txtObs);

        return card;
    }

    private void preencherHorariosEspeciais(List<GestaoLojaService.HorarioEspecialResumo> horariosEspeciais) {
        tabelaHorariosEspeciais.getItems().setAll(horariosEspeciais == null ? List.of() : horariosEspeciais);
        boolean temExcecoes = !tabelaHorariosEspeciais.getItems().isEmpty();
        if (emptyStateExcecoes != null) {
            emptyStateExcecoes.setVisible(!temExcecoes);
            emptyStateExcecoes.setManaged(!temExcecoes);
        }
        tabelaHorariosEspeciais.setVisible(temExcecoes);
        tabelaHorariosEspeciais.setManaged(temExcecoes);
    }

    private void configurarTabelaHorariosEspeciais() {
        colPeriodoExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().periodo()));
        colOperacaoExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().tipoOperacao()));
        colHorarioExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().horarioAplicado()));
        colMinimoExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().minimoColaboradoresTurno() != null
                        ? String.valueOf(cellData.getValue().minimoColaboradoresTurno())
                        : "-"));
        colDescricaoExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().descricao()));
        colTurnosCompativeisExcecao.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().turnosCompativeis()));

        tabelaHorariosEspeciais.getSelectionModel().selectedItemProperty().addListener((observavel, antigo, novo) -> {
            if (novo == null) {
                return;
            }
            preencherFormularioHorarioEspecial(novo);
            esconderMensagemExcecao();
        });
    }

    private void preencherFormularioHorarioEspecial(GestaoLojaService.HorarioEspecialResumo horarioEspecial) {
        idHorarioEspecialEmEdicao = horarioEspecial.idHorarioEspecial();
        txtDescricaoExcecao.setText(horarioEspecial.descricao());
        dpDataInicioExcecao.setValue(horarioEspecial.dataInicio());
        dpDataFimExcecao.setValue(horarioEspecial.dataFim());
        chkLojaEncerrada.setSelected(horarioEspecial.lojaEncerrada());
        txtHoraAberturaExcecao.setText(horarioEspecial.horaAbertura() != null ? horarioEspecial.horaAbertura().format(HORA_FORMATTER) : "");
        txtHoraFechoExcecao.setText(horarioEspecial.horaFecho() != null ? horarioEspecial.horaFecho().format(HORA_FORMATTER) : "");
        txtMinimoExcecao.setText(horarioEspecial.minimoColaboradoresTurno() != null
                ? String.valueOf(horarioEspecial.minimoColaboradoresTurno())
                : "");
        txtObservacoesExcecao.setText(horarioEspecial.observacoes() != null ? horarioEspecial.observacoes() : "");
        aplicarModoEncerrada();
    }

    private void limparFormularioHorarioEspecial() {
        idHorarioEspecialEmEdicao = null;
        txtDescricaoExcecao.clear();
        dpDataInicioExcecao.setValue(null);
        dpDataFimExcecao.setValue(null);
        chkLojaEncerrada.setSelected(false);
        txtHoraAberturaExcecao.clear();
        txtHoraFechoExcecao.clear();
        txtMinimoExcecao.clear();
        txtObservacoesExcecao.clear();
        tabelaHorariosEspeciais.getSelectionModel().clearSelection();
        aplicarModoEncerrada();
    }

    private void configurarFormularioEncerrada() {
        chkLojaEncerrada.selectedProperty().addListener((observavel, antigo, novo) -> {
            aplicarModoEncerrada();
            esconderMensagemExcecao();
        });
    }

    private void aplicarModoEncerrada() {
        boolean encerrada = chkLojaEncerrada.isSelected();
        txtHoraAberturaExcecao.setDisable(encerrada);
        txtHoraFechoExcecao.setDisable(encerrada);
        txtMinimoExcecao.setDisable(encerrada);
        if (encerrada) {
            txtHoraAberturaExcecao.clear();
            txtHoraFechoExcecao.clear();
            txtMinimoExcecao.clear();
        }
    }

    private void configurarOcultacaoFeedback() {
        txtHoraAbertura.textProperty().addListener((observavel, antigo, novo) -> esconderMensagem());
        txtHoraFecho.textProperty().addListener((observavel, antigo, novo) -> esconderMensagem());
        txtDescricaoExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtHoraAberturaExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtHoraFechoExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtMinimoExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtObservacoesExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        dpDataInicioExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        dpDataFimExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
    }

    private void configurarCamposHora() {
        configurarCampoHora(txtHoraAbertura, "HH:mm");
        configurarCampoHora(txtHoraFecho, "HH:mm");
        configurarCampoHora(txtHoraAberturaExcecao, "HH:mm");
        configurarCampoHora(txtHoraFechoExcecao, "HH:mm");
    }

    private void configurarCampoHora(TextField campo, String prompt) {
        campo.setPromptText(prompt);
        campo.focusedProperty().addListener((observavel, estavaFocado, estaFocado) -> {
            if (!estaFocado && campo.getText() != null && !campo.getText().isBlank()) {
                try {
                    campo.setText(LocalTime.parse(campo.getText().trim(), HORA_FORMATTER).format(HORA_FORMATTER));
                } catch (DateTimeParseException ignored) {
                    // Mantém o valor original para a validação formal no guardar.
                }
            }
        });
    }

    private LocalTime parseHora(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Indica a hora de " + campo + " no formato HH:mm.");
        }

        try {
            return LocalTime.parse(texto.trim(), HORA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A hora de " + campo + " deve seguir o formato HH:mm.");
        }
    }

    private LocalTime parseHoraOpcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(texto.trim(), HORA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A hora de " + campo + " deve seguir o formato HH:mm.");
        }
    }

    private Integer parseInteiroOpcional(String texto, Integer idRegra) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor específico da regra " + idRegra + " deve ser um número inteiro.");
        }
    }

    private Integer parseInteiroPositivoOpcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        try {
            int valor = Integer.parseInt(texto.trim());
            if (valor <= 0) {
                throw new IllegalArgumentException("O campo " + campo + " deve ser superior a zero.");
            }
            return valor;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O campo " + campo + " deve ser um número inteiro.");
        }
    }

    private String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "geral";
        }

        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1).toLowerCase();
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

    private void esconderMensagemExcecao() {
        lblMensagemExcecao.setManaged(false);
        lblMensagemExcecao.setVisible(false);
        lblMensagemExcecao.setText("");
    }

    private void mostrarMensagemExcecao(String mensagem, boolean sucesso) {
        lblMensagemExcecao.setText(mensagem);
        lblMensagemExcecao.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagemExcecao.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagemExcecao.setManaged(true);
        lblMensagemExcecao.setVisible(true);
    }

    private Window obterJanela() {
        if (lblNomeLoja == null || lblNomeLoja.getScene() == null) {
            return null;
        }
        return lblNomeLoja.getScene().getWindow();
    }
}
