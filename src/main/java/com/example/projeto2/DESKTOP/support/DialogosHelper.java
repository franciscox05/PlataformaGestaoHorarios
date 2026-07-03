package com.example.projeto2.DESKTOP.support;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class DialogosHelper {

    private static final String CAMINHO_CSS_ALERTA = "/com/example/projeto2/dashboard/dashboard.css";

    private DialogosHelper() {
    }

    public static final class CarregamentoHandle {
        private final Stage stage;
        private final Label lblMensagem;

        CarregamentoHandle(Stage stage, Label lblMensagem) {
            this.stage = stage;
            this.lblMensagem = lblMensagem;
        }

        public void atualizarMensagem(String novoTexto) {
            if (Platform.isFxApplicationThread()) lblMensagem.setText(novoTexto);
            else Platform.runLater(() -> lblMensagem.setText(novoTexto));
        }

        public void fechar() {
            if (Platform.isFxApplicationThread()) stage.close();
            else Platform.runLater(stage::close);
        }
    }

    public static boolean confirmarAcao(Window owner, String titulo, String cabecalho, String conteudo) {
        return confirmarAcao(owner, titulo, cabecalho, conteudo, "Confirmar");
    }

    public static boolean confirmarAcao(Window owner,
                                        String titulo,
                                        String cabecalho,
                                        String conteudo,
                                        String textoConfirmar) {
        AtomicReference<Boolean> confirmado = new AtomicReference<>(false);

        Button btnConfirmar = criarBotao(textoConfirmar, "botao-acao");
        Button btnCancelar = criarBotao("Cancelar", "botao-secundario");

        Stage stage = construirStageDialogo(
                owner,
                titulo,
                cabecalho,
                conteudo,
                null,
                btnCancelar,
                btnConfirmar
        );

        btnCancelar.setOnAction(event -> {
            confirmado.set(false);
            stage.close();
        });
        btnConfirmar.setOnAction(event -> {
            confirmado.set(true);
            stage.close();
        });

        stage.setOnCloseRequest(event -> confirmado.set(false));
        stage.showAndWait();
        return confirmado.get();
    }

    /**
     * Diálogo de confirmação destrutiva com conteúdo em texto simples.
     */
    public static boolean confirmarAcaoDanger(Window owner,
                                              String titulo,
                                              String cabecalho,
                                              String conteudo,
                                              String textoConfirmar) {
        javafx.scene.text.Text t = new javafx.scene.text.Text(conteudo);
        t.getStyleClass().add("dialogo-mensagem");
        t.setWrappingWidth(504.0);
        javafx.scene.text.TextFlow tf = new javafx.scene.text.TextFlow(t);
        return confirmarAcaoDanger(owner, titulo, cabecalho, tf, textoConfirmar);
    }

    /**
     * Diálogo de confirmação destrutiva com conteúdo rico (TextFlow, Label, etc.).
     */
    public static boolean confirmarAcaoDanger(Window owner,
                                              String titulo,
                                              String cabecalho,
                                              javafx.scene.Node conteudoNode,
                                              String textoConfirmar) {
        AtomicReference<Boolean> confirmado = new AtomicReference<>(false);

        Button btnConfirmar = criarBotao(textoConfirmar, "botao-perigo");
        Button btnCancelar  = criarBotao("Cancelar", "botao-secundario");

        Stage stage = construirStageDialogoDanger(
                owner, titulo, cabecalho, conteudoNode, btnCancelar, btnConfirmar);

        btnCancelar.setOnAction(e -> { confirmado.set(false); stage.close(); });
        btnConfirmar.setOnAction(e -> { confirmado.set(true);  stage.close(); });
        stage.setOnCloseRequest(e -> confirmado.set(false));
        stage.showAndWait();
        return confirmado.get();
    }

    public static void mostrarErro(Window owner, String titulo, String cabecalho, String conteudo) {
        mostrarMensagem(owner, titulo, cabecalho, conteudo);
    }

    public static void mostrarInformacao(Window owner, String titulo, String cabecalho, String conteudo) {
        mostrarMensagem(owner, titulo, cabecalho, conteudo);
    }

    public static void mostrarConteudo(Window owner,
                                       String titulo,
                                       String cabecalho,
                                       String conteudo,
                                       Region conteudoExtra) {
        Button btnFechar = criarBotao("Fechar", "botao-acao");
        Stage stage = construirStageDialogo(
                owner,
                titulo,
                cabecalho,
                conteudo,
                conteudoExtra,
                btnFechar
        );
        btnFechar.setOnAction(event -> stage.close());
        stage.showAndWait();
    }

    public static Optional<String> pedirTexto(Window owner,
                                              String titulo,
                                              String cabecalho,
                                              String conteudo,
                                              String valorInicial) {
        AtomicReference<String> valorConfirmado = new AtomicReference<>();

        TextField campo = new TextField(valorInicial != null ? valorInicial : "");
        campo.setPromptText("Escreve aqui");
        campo.getStyleClass().add("dialogo-campo");

        Button btnConfirmar = criarBotao("Confirmar", "botao-acao");
        btnConfirmar.disableProperty().bind(Bindings.createBooleanBinding(
                () -> campo.getText() == null || campo.getText().trim().isBlank(),
                campo.textProperty()
        ));

        Button btnCancelar = criarBotao("Cancelar", "botao-secundario");

        Stage stage = construirStageDialogo(
                owner,
                titulo,
                cabecalho,
                conteudo,
                campo,
                btnCancelar,
                btnConfirmar
        );

        btnCancelar.setOnAction(event -> {
            valorConfirmado.set(null);
            stage.close();
        });
        btnConfirmar.setOnAction(event -> {
            String texto = campo.getText() != null ? campo.getText().trim() : "";
            valorConfirmado.set(texto.isBlank() ? null : texto);
            stage.close();
        });

        stage.setOnShown(event -> Platform.runLater(campo::requestFocus));
        stage.showAndWait();
        return Optional.ofNullable(valorConfirmado.get());
    }

    /**
     * Mostra um overlay de carregamento não-bloqueante sobre a janela owner.
     * Devolve um CarregamentoHandle que permite atualizar o texto e fechar o overlay.
     * Deve ser chamado a partir da FX thread.
     */
    public static CarregamentoHandle mostrarCarregamento(Window owner, String mensagem) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("dialogo-overlay");

        VBox card = new VBox(20.0);
        card.getStyleClass().add("dialogo-loading-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(420.0);
        // Trava a altura ao conteúdo: sem isto, o overlay (StackPane em ecrã
        // inteiro) estica o card verticalmente (maxHeight default = MAX_VALUE),
        // deixando-o gigante em vez de um modal compacto centrado.
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setFillWidth(true);

        ProgressIndicator spinner = new ProgressIndicator(-1.0);
        spinner.getStyleClass().add("dialogo-loading-spinner");
        spinner.setPrefSize(56, 56);

        Label lblMensagem = new Label(mensagem);
        lblMensagem.getStyleClass().add("dialogo-loading-titulo");
        lblMensagem.setWrapText(true);
        lblMensagem.setTextAlignment(TextAlignment.CENTER);
        lblMensagem.setMaxWidth(320.0);

        Label lblSub = new Label("Por favor aguarda...");
        lblSub.getStyleClass().add("dialogo-loading-subtitulo");

        card.getChildren().addAll(spinner, lblMensagem, lblSub);
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        carregarCss(scene);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle("");
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setScene(scene);

        Rectangle2D limites = obterLimites(owner);
        overlay.setPrefSize(limites.getWidth(), limites.getHeight());
        stage.setX(limites.getMinX());
        stage.setY(limites.getMinY());
        stage.setWidth(limites.getWidth());
        stage.setHeight(limites.getHeight());

        stage.show();
        return new CarregamentoHandle(stage, lblMensagem);
    }

    /**
     * Mostra uma notificação grande centrada (sucesso ou erro) após uma operação.
     * Bloqueante — showAndWait. Dispensa com Enter, Escape ou botão OK.
     */
    public static void mostrarNotificacaoGeracao(Window owner, boolean sucesso, String titulo, String mensagem) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("dialogo-overlay");

        VBox card = new VBox(20.0);
        card.getStyleClass().add("dialogo-notificacao-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(520.0);
        // Ver nota em mostrarCarregamento: trava a altura ao conteúdo para o
        // modal ficar compacto em vez de esticado pela altura do overlay.
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setFillWidth(true);

        // Ícone circular
        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add(sucesso
                ? "dialogo-notificacao-icone-sucesso"
                : "dialogo-notificacao-icone-erro");
        SVGPath icone = new SVGPath();
        icone.setContent(sucesso
                ? "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
                : "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z");
        icone.setFill(sucesso ? Color.web("#16a34a") : Color.web("#c91428"));
        icone.setScaleX(1.6);
        icone.setScaleY(1.6);
        iconCircle.getChildren().add(icone);

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dialogo-notificacao-titulo");
        lblTitulo.setWrapText(true);
        lblTitulo.setTextAlignment(TextAlignment.CENTER);
        lblTitulo.setMaxWidth(400.0);

        Label lblMensagem = new Label(mensagem);
        lblMensagem.getStyleClass().add("dialogo-notificacao-mensagem");
        lblMensagem.setWrapText(true);
        lblMensagem.setTextAlignment(TextAlignment.CENTER);
        lblMensagem.setMaxWidth(400.0);

        Button btnOk = criarBotao("OK", "botao-acao");

        card.getChildren().addAll(iconCircle, lblTitulo, lblMensagem, btnOk);
        overlay.getChildren().add(card);
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        carregarCss(scene);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle("");
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setScene(scene);

        Rectangle2D limites = obterLimites(owner);
        overlay.setPrefSize(limites.getWidth(), limites.getHeight());
        stage.setX(limites.getMinX());
        stage.setY(limites.getMinY());
        stage.setWidth(limites.getWidth());
        stage.setHeight(limites.getHeight());

        btnOk.setOnAction(event -> stage.close());
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.ENTER) {
                stage.close();
                event.consume();
            }
        });

        Platform.runLater(btnOk::requestFocus);
        stage.showAndWait();
    }

    private static void mostrarMensagem(Window owner, String titulo, String cabecalho, String conteudo) {
        Button btnFechar = criarBotao("Fechar", "botao-acao");
        Stage stage = construirStageDialogo(
                owner,
                titulo,
                cabecalho,
                conteudo,
                null,
                btnFechar
        );
        btnFechar.setOnAction(event -> stage.close());
        stage.showAndWait();
    }

    private static Stage construirStageDialogo(Window owner,
                                               String titulo,
                                               String cabecalho,
                                               String conteudo,
                                               Region conteudoExtra,
                                               Button... botoes) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("dialogo-overlay");

        VBox cartao = new VBox(0.0);
        cartao.getStyleClass().add("dialogo-card");
        cartao.setMaxWidth(580.0);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);
        cartao.setFillWidth(true);

        VBox cabecalhoBox = new VBox(6.0);
        cabecalhoBox.getStyleClass().add("dialogo-faixa");
        cabecalhoBox.setPadding(new Insets(24.0, 28.0, 18.0, 28.0));

        if (titulo != null && !titulo.isBlank()) {
            Label lblKicker = new Label(titulo);
            lblKicker.getStyleClass().add("dialogo-kicker");
            cabecalhoBox.getChildren().add(lblKicker);
        }

        Label lblCabecalho = new Label(cabecalho);
        lblCabecalho.getStyleClass().add("dialogo-titulo");
        lblCabecalho.setWrapText(true);
        cabecalhoBox.getChildren().add(lblCabecalho);

        VBox corpo = new VBox(16.0);
        corpo.getStyleClass().add("dialogo-corpo");
        corpo.setPadding(new Insets(22.0, 28.0, 26.0, 28.0));

        Label lblConteudo = new Label(conteudo);
        lblConteudo.getStyleClass().add("dialogo-mensagem");
        lblConteudo.setWrapText(true);
        corpo.getChildren().add(lblConteudo);

        if (conteudoExtra != null) {
            VBox.setVgrow(conteudoExtra, Priority.NEVER);
            corpo.getChildren().add(conteudoExtra);
        }

        HBox barraBotoes = new HBox(12.0);
        barraBotoes.getStyleClass().add("dialogo-botoes");
        barraBotoes.setAlignment(Pos.CENTER);
        barraBotoes.getChildren().addAll(botoes);

        cartao.getChildren().addAll(cabecalhoBox, corpo, barraBotoes);
        overlay.getChildren().add(cartao);
        StackPane.setAlignment(cartao, Pos.CENTER);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        carregarCss(scene);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle(titulo != null ? titulo : "");
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setScene(scene);

        Rectangle2D limites = obterLimites(owner);
        overlay.setPrefSize(limites.getWidth(), limites.getHeight());
        stage.setX(limites.getMinX());
        stage.setY(limites.getMinY());
        stage.setWidth(limites.getWidth());
        stage.setHeight(limites.getHeight());

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        // Fechar ao clicar no fundo (fora do cartão)
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                stage.close();
                event.consume();
            }
        });

        return stage;
    }

    /**
     * Diálogo de confirmação para ações destrutivas: faixa de topo vermelha com ícone
     * de aviso, para distinguir claramente de um diálogo de confirmação normal.
     */
    private static Stage construirStageDialogoDanger(Window owner,
                                                     String titulo,
                                                     String cabecalho,
                                                     javafx.scene.Node conteudoNode,
                                                     Button... botoes) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("dialogo-overlay");

        VBox cartao = new VBox(0.0);
        cartao.getStyleClass().addAll("dialogo-card", "dialogo-card-danger");
        cartao.setMaxWidth(560.0);
        cartao.setMaxHeight(Region.USE_PREF_SIZE);
        cartao.setFillWidth(true);

        // Faixa vermelha com ícone de aviso
        VBox faixa = new VBox(10.0);
        faixa.getStyleClass().addAll("dialogo-faixa", "dialogo-faixa-danger");
        faixa.setPadding(new Insets(22.0, 28.0, 18.0, 28.0));

        // Ícone de lixo (delete)
        StackPane iconCircle = new StackPane();
        iconCircle.getStyleClass().add("dialogo-danger-icone-wrap");
        iconCircle.setMinSize(44.0, 44.0);
        iconCircle.setPrefSize(44.0, 44.0);
        iconCircle.setMaxSize(44.0, 44.0);
        SVGPath icone = new SVGPath();
        icone.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
        icone.getStyleClass().add("dialogo-danger-icone");
        iconCircle.getChildren().add(icone);

        Label lblKicker = new Label(titulo != null ? titulo.toUpperCase() : "ATENÇÃO");
        lblKicker.getStyleClass().addAll("dialogo-kicker", "dialogo-kicker-danger");

        Label lblCabecalho = new Label(cabecalho);
        lblCabecalho.getStyleClass().add("dialogo-titulo");
        lblCabecalho.setWrapText(true);

        faixa.getChildren().addAll(iconCircle, lblKicker, lblCabecalho);

        VBox corpo = new VBox(0.0);
        corpo.getStyleClass().add("dialogo-corpo");
        corpo.setPadding(new Insets(20.0, 28.0, 8.0, 28.0));
        corpo.getChildren().add(conteudoNode);

        HBox barraBotoes = new HBox(12.0);
        barraBotoes.getStyleClass().add("dialogo-botoes");
        barraBotoes.setAlignment(Pos.CENTER);
        barraBotoes.setPadding(new Insets(16.0, 28.0, 24.0, 28.0));
        barraBotoes.getChildren().addAll(botoes);

        cartao.getChildren().addAll(faixa, corpo, barraBotoes);
        overlay.getChildren().add(cartao);
        StackPane.setAlignment(cartao, Pos.CENTER);

        Scene scene = new Scene(overlay);
        scene.setFill(Color.TRANSPARENT);
        carregarCss(scene);

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle(titulo != null ? titulo : "");
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setScene(scene);

        Rectangle2D limites = obterLimites(owner);
        overlay.setPrefSize(limites.getWidth(), limites.getHeight());
        stage.setX(limites.getMinX());
        stage.setY(limites.getMinY());
        stage.setWidth(limites.getWidth());
        stage.setHeight(limites.getHeight());

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) { stage.close(); event.consume(); }
        });
        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) { stage.close(); event.consume(); }
        });
        return stage;
    }

    private static Button criarBotao(String texto, String styleClass) {
        Button botao = new Button(texto);
        botao.getStyleClass().add(styleClass);
        botao.setMinWidth(152.0);
        return botao;
    }

    private static Rectangle2D obterLimites(Window owner) {
        Window janelaRaiz = obterJanelaRaiz(owner);
        if (janelaRaiz != null && janelaRaiz.getWidth() > 0 && janelaRaiz.getHeight() > 0) {
            return new Rectangle2D(janelaRaiz.getX(), janelaRaiz.getY(), janelaRaiz.getWidth(), janelaRaiz.getHeight());
        }

        Screen screen = Screen.getPrimary();
        return screen != null ? screen.getVisualBounds() : new Rectangle2D(0, 0, 1480, 920);
    }

    /**
     * Sobe a cadeia de janelas-dono até à janela principal da aplicação. Sem isto, um
     * diálogo aberto a partir de outro diálogo (ex.: confirmação dentro de "Editar Nome")
     * fica limitado ao tamanho da janela pequena que o abriu, em vez de cobrir o ecrã todo.
     */
    private static Window obterJanelaRaiz(Window owner) {
        Window atual = owner;
        while (atual instanceof Stage stageAtual && stageAtual.getOwner() != null) {
            atual = stageAtual.getOwner();
        }
        return atual;
    }

    private static void carregarCss(Scene scene) {
        if (scene == null) {
            return;
        }

        var recursoCss = DialogosHelper.class.getResource(CAMINHO_CSS_ALERTA);
        if (recursoCss != null && scene.getStylesheets().stream().noneMatch(css -> css.endsWith("dashboard.css"))) {
            scene.getStylesheets().add(recursoCss.toExternalForm());
        }
    }
}
