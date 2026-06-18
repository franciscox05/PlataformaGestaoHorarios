package com.example.projeto2.DESKTOP;

import com.example.projeto2.API.Services.GestaoLojaService;
import com.example.projeto2.DESKTOP.support.DialogosHelper;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.example.projeto2.DESKTOP.support.ClassificadorRegra;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Scope("prototype")
public class GestaoLojaController {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> OPCOES_HORA = gerarOpcoesHora();

    @FXML private Label lblNomeLoja;
    @FXML private Label lblLocalizacao;
    @FXML private Label lblCargoGestor;
    @FXML private Label lblHoraAbertura;
    @FXML private Label lblHoraFecho;
    @FXML private Label lblMensagem;
    @FXML private VBox regrasContainer;

    @FXML private TextField txtDescricaoExcecao;
    @FXML private DatePicker dpDataInicioExcecao;
    @FXML private DatePicker dpDataFimExcecao;
    @FXML private CheckBox chkLojaEncerrada;
    @FXML private ComboBox<String> cbHoraAberturaExcecao;
    @FXML private ComboBox<String> cbHoraFechoExcecao;
    @FXML private TextField txtMinimoExcecao;
    @FXML private TextArea txtObservacoesExcecao;
    @FXML private Label lblMensagemExcecao;
    @FXML private VBox emptyStateExcecoes;

    @FXML private TableView<GestaoLojaService.HorarioEspecialResumo> tabelaHorariosEspeciais;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colPeriodoExcecao;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colOperacaoExcecao;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colHorarioExcecao;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colMinimoExcecao;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colDescricaoExcecao;
    @FXML private TableColumn<GestaoLojaService.HorarioEspecialResumo, String> colTurnosCompativeisExcecao;

    @FXML private VBox turnosContainer;
    @FXML private VBox painelCriarTurno;
    @FXML private Label lblTituloFormTurno;
    @FXML private TextField txtNomeTurno;
    @FXML private ComboBox<String> cbHoraInicioTurno;
    @FXML private ComboBox<String> cbHoraFimTurno;
    @FXML private Label lblMensagemTurnos;

    private final GestaoLojaService gestaoLojaBLL;
    private final Map<Integer, TextField> camposValor = new LinkedHashMap<>();
    private final Map<Integer, CheckBox> camposBooleanos = new LinkedHashMap<>();
    private final Map<Integer, TextArea> camposObservacoes = new LinkedHashMap<>();
    private List<GestaoLojaService.RegraLojaResumo> todasRegras = new ArrayList<>();
    private final Set<Integer> regrasAdicionadasNaSessao = new LinkedHashSet<>();
    private Utilizador utilizadorLogado;
    private Integer idHorarioEspecialEmEdicao;
    private Integer idTurnoEmEdicao;
    private LocalTime horaAberturaAtual;
    private LocalTime horaFechoAtual;

    public GestaoLojaController(GestaoLojaService gestaoLojaBLL) {
        this.gestaoLojaBLL = gestaoLojaBLL;
    }

    @FXML
    public void initialize() {
        esconderMensagem();
        esconderMensagemExcecao();
        popularComboBoxesHoraExcecoes();
        cbHoraInicioTurno.getItems().setAll(OPCOES_HORA);
        cbHoraFimTurno.getItems().setAll(OPCOES_HORA);
        configurarOcultacaoFeedback();
        configurarTabelaHorariosEspeciais();
        configurarFormularioEncerrada();
        limparFormularioHorarioEspecial();
        tabelaHorariosEspeciais.setPlaceholder(
                new Label("Ainda não existem exceções de horário configuradas para esta loja."));
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
                    "Deseja guardar as regras da loja?",
                    "As regras ficam ativas na próxima geração de horários."
            )) {
                return;
            }

            LocalTime horaAbertura = horaAberturaAtual;
            LocalTime horaFecho = horaFechoAtual;

            List<GestaoLojaService.ConfiguracaoRegraRequest> regras = new ArrayList<>();
            for (Map.Entry<Integer, TextField> entry : camposValor.entrySet()) {
                Integer idRegra = entry.getKey();
                regras.add(new GestaoLojaService.ConfiguracaoRegraRequest(
                        idRegra, parseInteiroOpcional(entry.getValue().getText(), idRegra), observacoesDe(idRegra)));
            }
            for (Map.Entry<Integer, CheckBox> entry : camposBooleanos.entrySet()) {
                Integer idRegra = entry.getKey();
                regras.add(new GestaoLojaService.ConfiguracaoRegraRequest(
                        idRegra, entry.getValue().isSelected() ? 1 : 0, observacoesDe(idRegra)));
            }

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
                            parseHoraOpcionalComboBox(cbHoraAberturaExcecao.getValue(), "abertura especial"),
                            parseHoraOpcionalComboBox(cbHoraFechoExcecao.getValue(), "fecho especial"),
                            parseInteiroPositivoOpcional(txtMinimoExcecao.getText(), "mínimo especial por turno"),
                            txtObservacoesExcecao.getText()
                    )
            );

            mostrarMensagemExcecao(
                    novaExcecao ? "Exceção guardada com sucesso." : "Exceção atualizada com sucesso.",
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

            regrasAdicionadasNaSessao.clear();
            GestaoLojaService.GestaoLojaResumo resumo = gestaoLojaBLL.obterResumo(utilizadorLogado.getId());

            lblNomeLoja.setText(resumo.nomeLoja());
            lblLocalizacao.setText(resumo.localizacao());
            lblCargoGestor.setText(resumo.cargoGestor());
            lblHoraAbertura.setText(resumo.horaAbertura().isBlank() ? "-" : resumo.horaAbertura());
            lblHoraFecho.setText(resumo.horaFecho().isBlank() ? "-" : resumo.horaFecho());
            try {
                horaAberturaAtual = resumo.horaAbertura().isBlank() ? null
                        : LocalTime.parse(resumo.horaAbertura(), DateTimeFormatter.ofPattern("HH:mm"));
                horaFechoAtual = resumo.horaFecho().isBlank() ? null
                        : LocalTime.parse(resumo.horaFecho(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception ignored) {
                horaAberturaAtual = null;
                horaFechoAtual = null;
            }

            preencherRegras(resumo.regras());
            preencherHorariosEspeciais(resumo.horariosEspeciais());
            preencherTurnos(resumo.turnos());
        } catch (IllegalArgumentException e) {
            lblNomeLoja.setText("-");
            lblLocalizacao.setText("-");
            lblCargoGestor.setText("-");
            lblHoraAbertura.setText("-");
            lblHoraFecho.setText("-");
            horaAberturaAtual = null;
            horaFechoAtual = null;
            preencherRegras(List.of());
            preencherHorariosEspeciais(List.of());
            preencherTurnos(List.of());
            mostrarMensagem(e.getMessage(), false);
        }
    }

    private void preencherRegras(List<GestaoLojaService.RegraLojaResumo> regras) {
        regrasContainer.getChildren().clear();
        camposValor.clear();
        camposBooleanos.clear();
        camposObservacoes.clear();
        this.todasRegras = regras != null ? regras : List.of();

        if (todasRegras.isEmpty()) {
            Label semRegras = new Label("Não existem regras base configuradas para apresentar nesta loja.");
            semRegras.getStyleClass().add("subtitulo");
            regrasContainer.getChildren().add(semRegras);
            return;
        }

        List<GestaoLojaService.RegraLojaResumo> fixas = new ArrayList<>();
        List<GestaoLojaService.RegraLojaResumo> proprias = new ArrayList<>();
        for (GestaoLojaService.RegraLojaResumo regra : todasRegras) {
            if (ClassificadorRegra.ehFixaLegal(regra.descricao(), regra.tipo())) {
                fixas.add(regra);
            } else {
                proprias.add(regra);
            }
        }

        regrasContainer.getChildren().add(cabecalhoSeccao("Regras do motor de geração",
                "Parâmetros que controlam o planeamento automático desta loja. "
                        + "Os valores base do sistema podem ser ajustados para personalizar o motor. "
                        + "Usa \"Criar nota / regra livre\" para adicionar diretrizes específicas."));

        Button btnAdicionar = new Button("✏️ Criar nota / regra livre");
        btnAdicionar.getStyleClass().add("botao-secundario");
        btnAdicionar.setMaxWidth(Double.MAX_VALUE);
        btnAdicionar.setOnAction(e -> onAdicionarRegraClick());
        regrasContainer.getChildren().add(btnAdicionar);

        if (proprias.isEmpty()) {
            Label vazio = new Label("Ainda não adicionaste regras próprias. Usa o botão acima para criar "
                    + "qualquer diretriz ou nota específica para esta loja.");
            vazio.getStyleClass().add("subtitulo");
            vazio.setWrapText(true);
            regrasContainer.getChildren().add(vazio);
        } else {
            for (GestaoLojaService.RegraLojaResumo regra : proprias) {
                regrasContainer.getChildren().add(criarCardRegra(regra, true));
            }
        }

        if (!fixas.isEmpty()) {
            regrasContainer.getChildren().add(cabecalhoSeccao("Fixas por lei",
                    "Mínimos legais obrigatórios. Apresentadas para referência — não podem ser reduzidas nem removidas."));
            for (GestaoLojaService.RegraLojaResumo regra : fixas) {
                regrasContainer.getChildren().add(criarCardRegra(regra, false));
            }
        }
    }

    private void onAdicionarRegraClick() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Criar nota / regra livre");
        dialog.setHeaderText("Cria uma diretriz ou regra específica para esta loja.\n"
                + "Pode ser qualquer instrução — o motor de geração não a processa automaticamente.");
        Window janela = obterJanela();
        if (janela != null) dialog.initOwner(janela);

        ButtonType btnCriar = new ButtonType("Criar", ButtonType.OK.getButtonData());
        dialog.getDialogPane().getButtonTypes().addAll(btnCriar, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        Label lblDesc = new Label("Descrição:");
        TextField txtDesc = new TextField();
        txtDesc.setPromptText("Ex.: Toda a equipa trabalha na última sexta-feira do mês");
        txtDesc.setPrefWidth(360);
        grid.add(lblDesc, 0, 0);
        grid.add(txtDesc, 1, 0);

        Label lblObs = new Label("Observações:");
        TextArea txtObs = new TextArea();
        txtObs.setPromptText("Notas adicionais (opcional)");
        txtObs.setPrefRowCount(3);
        txtObs.setWrapText(true);
        grid.add(lblObs, 0, 1);
        grid.add(txtObs, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().lookupButton(btnCriar).setDisable(true);
        txtDesc.textProperty().addListener((obs, old, val) ->
                dialog.getDialogPane().lookupButton(btnCriar).setDisable(val.isBlank()));

        dialog.setResultConverter(bt -> bt == btnCriar
                ? new String[]{ txtDesc.getText(), txtObs.getText() } : null);

        dialog.showAndWait().ifPresent(resultado -> {
            try {
                gestaoLojaBLL.criarRegraLivre(utilizadorLogado.getId(), resultado[0], resultado[1]);
                mostrarMensagem("Regra criada com sucesso.", true);
                carregarDados();
            } catch (IllegalArgumentException e) {
                mostrarMensagem(e.getMessage(), false);
            } catch (Exception e) {
                mostrarMensagem("Não foi possível criar a regra.", false);
            }
        });
    }

    private void onDesativarRegraClick(GestaoLojaService.RegraLojaResumo regra) {
        try {
            regrasAdicionadasNaSessao.remove(regra.idRegra());
            gestaoLojaBLL.desativarRegraDaLoja(utilizadorLogado.getId(), regra.idRegra());
            mostrarMensagem("Regra desativada. Podes reativá-la a qualquer momento.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível desativar a regra.", false);
        }
    }

    private void onAtivarRegraClick(GestaoLojaService.RegraLojaResumo regra) {
        try {
            gestaoLojaBLL.ativarRegraDaLoja(utilizadorLogado.getId(), regra.idRegra());
            mostrarMensagem("Regra reativada.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível reativar a regra.", false);
        }
    }

    private void onRemoverRegraClick(GestaoLojaService.RegraLojaResumo regra) {
        if (!DialogosHelper.confirmarAcao(
                obterJanela(),
                "Remover regra",
                "Remover \"" + regra.descricao() + "\"?",
                "Esta ação não pode ser desfeita."
        )) {
            return;
        }
        try {
            gestaoLojaBLL.removerRegraLivre(utilizadorLogado.getId(), regra.idRegra());
            mostrarMensagem("Regra removida com sucesso.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível remover a regra.", false);
        }
    }

    private VBox cabecalhoSeccao(String titulo, String nota) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("card-titulo");
        Label lblNota = new Label(nota);
        lblNota.getStyleClass().add("subtitulo");
        lblNota.setWrapText(true);
        VBox box = new VBox(4, lblTitulo, lblNota);
        box.setPadding(new Insets(8, 0, 0, 0));
        return box;
    }

    private VBox criarCardRegra(GestaoLojaService.RegraLojaResumo regra, boolean editavel) {
        boolean ehNota = regra.privada() || "nota".equals(regra.tipo());
        ClassificadorRegra.Categoria categoria = ehNota
                ? ClassificadorRegra.Categoria.EDITAVEL
                : ClassificadorRegra.categoria(regra.descricao(), regra.tipo());

        VBox card = new VBox(16);
        card.getStyleClass().add("bento-card");
        card.setPadding(new Insets(28));
        if (!regra.ativo()) {
            card.setStyle("-fx-opacity: 0.65;");
        }

        Label lblTitulo = new Label(regra.descricao());
        lblTitulo.getStyleClass().addAll("card-titulo", "config-card-titulo");
        lblTitulo.setWrapText(true);

        HBox cabecalho = new HBox(12);
        cabecalho.setFillHeight(true);
        cabecalho.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);
        cabecalho.getChildren().add(lblTitulo);

        if (!regra.ativo()) {
            Label badgeDesativada = new Label("DESATIVADA");
            badgeDesativada.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; "
                    + "-fx-font-weight: 700; -fx-font-size: 10px; "
                    + "-fx-padding: 2 8 2 8; -fx-background-radius: 6;");
            cabecalho.getChildren().add(badgeDesativada);
        }

        if (regra.privada()) {
            Region espaco = new Region();
            HBox.setHgrow(espaco, Priority.ALWAYS);
            cabecalho.getChildren().add(espaco);

            Button btnRemover = new Button("Remover");
            btnRemover.getStyleClass().add("botao-perigo");
            btnRemover.setOnAction(e -> onRemoverRegraClick(regra));
            cabecalho.getChildren().add(btnRemover);

            if (regra.ativo()) {
                Button btnDesativar = new Button("Desativar");
                btnDesativar.getStyleClass().add("botao-secundario");
                btnDesativar.setOnAction(e -> onDesativarRegraClick(regra));
                cabecalho.getChildren().add(btnDesativar);
            } else {
                Button btnAtivar = new Button("Ativar");
                btnAtivar.getStyleClass().add("botao-acao");
                btnAtivar.setOnAction(e -> onAtivarRegraClick(regra));
                cabecalho.getChildren().add(btnAtivar);
            }
        }

        card.getChildren().add(cabecalho);

        if (ehNota) {
            Label lblNotaInfo = new Label("Nota / regra livre — apenas referência para o gestor.");
            lblNotaInfo.getStyleClass().add("subtitulo");
            card.getChildren().add(lblNotaInfo);
            if (regra.observacoes() != null && !regra.observacoes().isBlank()) {
                Label lblObs = new Label(regra.observacoes());
                lblObs.getStyleClass().add("subtitulo");
                lblObs.setWrapText(true);
                card.getChildren().add(lblObs);
            }
        } else {
            String valorPadrao = regra.valorPadrao() != null ? String.valueOf(regra.valorPadrao()) : "sem valor base";
            Label lblDetalhe = new Label("Tipo: " + formatarTipo(regra.tipo()) + " | Valor base: " + valorPadrao);
            lblDetalhe.getStyleClass().addAll("subtitulo", "config-card-desc");
            lblDetalhe.setWrapText(true);
            card.getChildren().add(lblDetalhe);

            switch (categoria) {
                case FIXA_LEGAL -> preencherCardFixaLegal(card, regra);
                case BOOLEANA -> preencherCardBooleana(card, regra);
                case EDITAVEL -> preencherCardEditavel(card, regra);
            }
            adicionarObservacoes(card, regra);
        }

        return card;
    }

    private void preencherCardFixaLegal(VBox card, GestaoLojaService.RegraLojaResumo regra) {
        Label badge = new Label("FIXO POR LEI");
        badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-font-weight: 700; "
                + "-fx-font-size: 10px; -fx-padding: 2 8 2 8; -fx-background-radius: 6;");

        Integer legalMinimo = ClassificadorRegra.valorLegalMinimo(regra.descricao(), regra.tipo());
        int atual = regra.valorEspecifico() != null ? regra.valorEspecifico()
                : (regra.valorPadrao() != null ? regra.valorPadrao() : 0);
        int valorMostrado = legalMinimo != null ? Math.max(atual, legalMinimo) : atual;

        Label lblValor = new Label("Valor aplicado");
        lblValor.getStyleClass().add("campo-titulo");
        TextField txtValor = new TextField(String.valueOf(valorMostrado));
        txtValor.getStyleClass().add("campo-input");
        txtValor.setEditable(false);
        txtValor.setDisable(true);

        Label lblNota = new Label(ClassificadorRegra.notaLegal(regra.descricao(), regra.tipo()));
        lblNota.getStyleClass().add("subtitulo");
        lblNota.setWrapText(true);

        HBox linhaValor = new HBox(12, lblValor, txtValor);
        HBox.setHgrow(txtValor, Priority.ALWAYS);
        card.getChildren().addAll(badge, linhaValor, lblNota);
        camposValor.put(regra.idRegra(), txtValor);
    }

    private void preencherCardBooleana(VBox card, GestaoLojaService.RegraLojaResumo regra) {
        boolean ativa = regra.valorEspecifico() != null
                ? regra.valorEspecifico() > 0
                : (regra.valorPadrao() != null && regra.valorPadrao() > 0);
        CheckBox chk = new CheckBox("Ativa nesta loja");
        chk.setSelected(ativa);
        card.getChildren().add(chk);
        camposBooleanos.put(regra.idRegra(), chk);
    }

    private void preencherCardEditavel(VBox card, GestaoLojaService.RegraLojaResumo regra) {
        Label lblValor = new Label("Valor próprio da loja");
        lblValor.getStyleClass().add("campo-titulo");

        TextField txtValor = new TextField();
        txtValor.getStyleClass().add("campo-input");
        txtValor.setPromptText(regra.valorPadrao() != null
                ? "Base do sistema: " + regra.valorPadrao()
                : "Usar valor base do sistema");
        if (regra.valorEspecifico() != null) {
            txtValor.setText(String.valueOf(regra.valorEspecifico()));
        } else if (regra.valorPadrao() != null) {
            txtValor.setText(String.valueOf(regra.valorPadrao()));
        }

        HBox linhaValor = new HBox(12, lblValor, txtValor);
        linhaValor.setFillHeight(true);
        HBox.setHgrow(txtValor, Priority.ALWAYS);
        card.getChildren().add(linhaValor);
        camposValor.put(regra.idRegra(), txtValor);
    }

    private void adicionarObservacoes(VBox card, GestaoLojaService.RegraLojaResumo regra) {
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

        card.getChildren().addAll(lblObs, txtObs);
        camposObservacoes.put(regra.idRegra(), txtObs);
    }

    private String observacoesDe(Integer idRegra) {
        TextArea campo = camposObservacoes.get(idRegra);
        return campo != null ? campo.getText() : null;
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
            if (novo == null) return;
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
        cbHoraAberturaExcecao.setValue(horarioEspecial.horaAbertura() != null
                ? horarioEspecial.horaAbertura().format(HORA_FORMATTER) : null);
        cbHoraFechoExcecao.setValue(horarioEspecial.horaFecho() != null
                ? horarioEspecial.horaFecho().format(HORA_FORMATTER) : null);
        txtMinimoExcecao.setText(horarioEspecial.minimoColaboradoresTurno() != null
                ? String.valueOf(horarioEspecial.minimoColaboradoresTurno()) : "");
        txtObservacoesExcecao.setText(horarioEspecial.observacoes() != null ? horarioEspecial.observacoes() : "");
        aplicarModoEncerrada();
    }

    private void limparFormularioHorarioEspecial() {
        idHorarioEspecialEmEdicao = null;
        txtDescricaoExcecao.clear();
        dpDataInicioExcecao.setValue(null);
        dpDataFimExcecao.setValue(null);
        chkLojaEncerrada.setSelected(false);
        cbHoraAberturaExcecao.setValue(null);
        cbHoraFechoExcecao.setValue(null);
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
        cbHoraAberturaExcecao.setDisable(encerrada);
        cbHoraFechoExcecao.setDisable(encerrada);
        txtMinimoExcecao.setDisable(encerrada);
        if (encerrada) {
            cbHoraAberturaExcecao.setValue(null);
            cbHoraFechoExcecao.setValue(null);
            txtMinimoExcecao.clear();
        }
    }

    private void configurarOcultacaoFeedback() {
        txtDescricaoExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        cbHoraAberturaExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        cbHoraFechoExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtMinimoExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        txtObservacoesExcecao.textProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        dpDataInicioExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
        dpDataFimExcecao.valueProperty().addListener((observavel, antigo, novo) -> esconderMensagemExcecao());
    }

    private void popularComboBoxesHoraExcecoes() {
        cbHoraAberturaExcecao.getItems().setAll(OPCOES_HORA);
        cbHoraFechoExcecao.getItems().setAll(OPCOES_HORA);
    }

    private static List<String> gerarOpcoesHora() {
        List<String> opcoes = new ArrayList<>();
        for (int h = 6; h <= 23; h++) {
            opcoes.add(String.format("%02d:00", h));
            opcoes.add(String.format("%02d:30", h));
        }
        return opcoes;
    }

    // ── Turnos ────────────────────────────────────────────────────────────────

    private void preencherTurnos(List<GestaoLojaService.TurnoResumo> turnos) {
        if (turnosContainer == null) return;
        turnosContainer.getChildren().clear();

        if (turnos == null || turnos.isEmpty()) {
            Label vazio = new Label("Ainda não existem turnos configurados. Usa o botão \"Criar turno\" acima para adicionar o primeiro.");
            vazio.getStyleClass().add("subtitulo");
            vazio.setWrapText(true);
            turnosContainer.getChildren().add(vazio);
        } else {
            for (GestaoLojaService.TurnoResumo turno : turnos) {
                turnosContainer.getChildren().add(criarCardTurno(turno));
            }
        }
    }

    @FXML
    public void onCriarTurnoClick() {
        idTurnoEmEdicao = null;
        txtNomeTurno.clear();
        cbHoraInicioTurno.setValue(null);
        cbHoraFimTurno.setValue(null);
        lblTituloFormTurno.setText("Criar turno");
        esconderMensagemTurnos();
        mostrarPainelTurno();
    }

    @FXML
    public void onGuardarTurnoClick() {
        try {
            if (utilizadorLogado == null || utilizadorLogado.getId() == null) {
                throw new IllegalArgumentException("Não foi possível identificar o utilizador autenticado.");
            }
            LocalTime horaInicio = parseHoraOpcionalComboBox(cbHoraInicioTurno.getValue(), "início do turno");
            LocalTime horaFim = parseHoraOpcionalComboBox(cbHoraFimTurno.getValue(), "fim do turno");

            if (idTurnoEmEdicao == null) {
                if (horaInicio == null || horaFim == null) {
                    throw new IllegalArgumentException("Indica a hora de início e a hora de fim do turno.");
                }
                gestaoLojaBLL.criarTurno(utilizadorLogado.getId(), txtNomeTurno.getText(), horaInicio, horaFim);
                mostrarMensagemTurnos("Turno criado com sucesso.", true);
            } else {
                gestaoLojaBLL.editarTurno(utilizadorLogado.getId(), idTurnoEmEdicao,
                        txtNomeTurno.getText(), horaInicio, horaFim);
                mostrarMensagemTurnos("Turno atualizado com sucesso.", true);
            }
            ocultarPainelTurno();
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagemTurnos(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagemTurnos("Não foi possível guardar o turno.", false);
        }
    }

    @FXML
    public void onCancelarTurnoClick() {
        ocultarPainelTurno();
        esconderMensagemTurnos();
    }

    private void onEditarTurnoClick(GestaoLojaService.TurnoResumo turno) {
        idTurnoEmEdicao = turno.idTurno();
        txtNomeTurno.setText(turno.nome());
        cbHoraInicioTurno.setValue(turno.horaInicio());
        cbHoraFimTurno.setValue(turno.horaFim());
        lblTituloFormTurno.setText("Editar: " + turno.nome());
        esconderMensagemTurnos();
        mostrarPainelTurno();
    }

    private void onDesativarTurnoClick(GestaoLojaService.TurnoResumo turno) {
        if (!DialogosHelper.confirmarAcao(
                obterJanela(),
                "Desativar turno",
                "Desativar \"" + turno.nome() + "\"?",
                "O turno deixará de ser usado em novas gerações. Os horários já atribuídos são preservados."
        )) return;
        try {
            gestaoLojaBLL.desativarTurno(utilizadorLogado.getId(), turno.idTurno());
            mostrarMensagem("Turno \"" + turno.nome() + "\" desativado.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível desativar o turno.", false);
        }
    }

    private void onAtivarTurnoClick(GestaoLojaService.TurnoResumo turno) {
        try {
            gestaoLojaBLL.ativarTurno(utilizadorLogado.getId(), turno.idTurno());
            mostrarMensagem("Turno \"" + turno.nome() + "\" reativado.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível reativar o turno.", false);
        }
    }

    private void onEliminarTurnoClick(GestaoLojaService.TurnoResumo turno) {
        if (!DialogosHelper.confirmarAcao(
                obterJanela(),
                "Eliminar turno",
                "Eliminar \"" + turno.nome() + "\" definitivamente?",
                "Só é possível eliminar turnos sem horários atribuídos. Esta ação não pode ser desfeita."
        )) return;
        try {
            gestaoLojaBLL.removerTurno(utilizadorLogado.getId(), turno.idTurno());
            mostrarMensagem("Turno \"" + turno.nome() + "\" eliminado.", true);
            carregarDados();
        } catch (IllegalArgumentException e) {
            mostrarMensagem(e.getMessage(), false);
        } catch (Exception e) {
            mostrarMensagem("Não foi possível eliminar o turno.", false);
        }
    }

    private HBox criarCardTurno(GestaoLojaService.TurnoResumo turno) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        boolean desativado = !turno.ativo();
        row.setStyle("-fx-background-color: " + (desativado ? "#f8fafc" : "#ffffff") + "; "
                + "-fx-background-radius: 8; -fx-border-color: "
                + (desativado ? "#e2e8f0" : "#bfdbfe") + "; -fx-border-radius: 8;");
        if (desativado) row.setOpacity(0.65);

        Label lblNome = new Label(turno.nome());
        lblNome.getStyleClass().add("campo-titulo");
        lblNome.setPrefWidth(140);

        Label lblHoras = new Label(turno.horaInicio() + " – " + turno.horaFim());
        lblHoras.getStyleClass().add("subtitulo");

        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);

        row.getChildren().addAll(lblNome, lblHoras, espacador);

        if (desativado) {
            Label badge = new Label("DESATIVADO");
            badge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; "
                    + "-fx-font-weight: 700; -fx-font-size: 10px; "
                    + "-fx-padding: 2 8 2 8; -fx-background-radius: 6;");
            row.getChildren().add(badge);

            Button btnAtivar = new Button("Ativar");
            btnAtivar.getStyleClass().add("botao-acao");
            btnAtivar.setOnAction(e -> onAtivarTurnoClick(turno));
            row.getChildren().add(btnAtivar);
        } else {
            Button btnEditar = new Button("Editar");
            btnEditar.getStyleClass().add("botao-secundario");
            btnEditar.setOnAction(e -> onEditarTurnoClick(turno));

            Button btnDesativar = new Button("Desativar");
            btnDesativar.getStyleClass().add("botao-secundario");
            btnDesativar.setOnAction(e -> onDesativarTurnoClick(turno));

            row.getChildren().addAll(btnEditar, btnDesativar);
        }

        Button btnEliminar = new Button("Eliminar");
        btnEliminar.getStyleClass().add("botao-perigo");
        btnEliminar.setOnAction(e -> onEliminarTurnoClick(turno));
        row.getChildren().add(btnEliminar);

        return row;
    }

    private void mostrarPainelTurno() {
        painelCriarTurno.setVisible(true);
        painelCriarTurno.setManaged(true);
    }

    private void ocultarPainelTurno() {
        painelCriarTurno.setVisible(false);
        painelCriarTurno.setManaged(false);
        idTurnoEmEdicao = null;
    }

    private void esconderMensagemTurnos() {
        if (lblMensagemTurnos == null) return;
        lblMensagemTurnos.setManaged(false);
        lblMensagemTurnos.setVisible(false);
        lblMensagemTurnos.setText("");
    }

    private void mostrarMensagemTurnos(String mensagem, boolean sucesso) {
        if (lblMensagemTurnos == null) return;
        lblMensagemTurnos.setText(mensagem);
        lblMensagemTurnos.getStyleClass().removeAll("mensagem-sucesso", "mensagem-erro");
        lblMensagemTurnos.getStyleClass().add(sucesso ? "mensagem-sucesso" : "mensagem-erro");
        lblMensagemTurnos.setManaged(true);
        lblMensagemTurnos.setVisible(true);
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private LocalTime parseHoraComboBox(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Seleciona a hora de " + campo + ".");
        }
        try {
            return LocalTime.parse(valor.trim(), HORA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A hora de " + campo + " é inválida.");
        }
    }

    private LocalTime parseHoraOpcionalComboBox(String valor, String campo) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return LocalTime.parse(valor.trim(), HORA_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("A hora de " + campo + " é inválida.");
        }
    }

    private Integer parseInteiroOpcional(String texto, Integer idRegra) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return Integer.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O valor específico da regra " + idRegra + " deve ser um número inteiro.");
        }
    }

    private Integer parseInteiroPositivoOpcional(String texto, String campo) {
        if (texto == null || texto.isBlank()) return null;
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
        if (tipo == null || tipo.isBlank()) return "geral";
        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1).toLowerCase();
    }

    // ── Feedback ─────────────────────────────────────────────────────────────

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
        if (lblNomeLoja == null || lblNomeLoja.getScene() == null) return null;
        return lblNomeLoja.getScene().getWindow();
    }
}
