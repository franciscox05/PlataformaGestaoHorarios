package com.example.projeto2.Controller.support;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    public static void mostrarErro(Window owner, String titulo, String cabecalho, String conteudo) {
        mostrarMensagem(owner, titulo, cabecalho, conteudo);
    }

    public static void mostrarInformacao(Window owner, String titulo, String cabecalho, String conteudo) {
        mostrarMensagem(owner, titulo, cabecalho, conteudo);
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

        return stage;
    }

    private static Button criarBotao(String texto, String styleClass) {
        Button botao = new Button(texto);
        botao.getStyleClass().add(styleClass);
        botao.setMinWidth(152.0);
        return botao;
    }

    private static Rectangle2D obterLimites(Window owner) {
        if (owner != null && owner.getWidth() > 0 && owner.getHeight() > 0) {
            return new Rectangle2D(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
        }

        Screen screen = Screen.getPrimary();
        return screen != null ? screen.getVisualBounds() : new Rectangle2D(0, 0, 1480, 920);
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
