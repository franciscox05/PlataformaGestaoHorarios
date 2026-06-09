package com.example.projeto2;

import com.example.projeto2.DESKTOP.support.DialogosHelper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Projeto2Application extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Projeto2Application.class)
                .web(WebApplicationType.NONE)
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                Projeto2Application.class.getResource("login/login-view.fxml")
        );

        fxmlLoader.setControllerFactory(springContext::getBean);

        Scene scene = new Scene(fxmlLoader.load(), UIConstants.APP_WIDTH, UIConstants.APP_HEIGHT);

        primaryStage.setTitle("Levi's Staff Portal - Autenticação");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(UIConstants.APP_MIN_WIDTH);
        primaryStage.setMinHeight(UIConstants.APP_MIN_HEIGHT);
        primaryStage.setResizable(false);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.setFullScreen(true);
        primaryStage.setOnCloseRequest(event -> {
            boolean confirmado = DialogosHelper.confirmarAcao(
                    primaryStage,
                    "Fechar aplicação",
                    "Deseja fechar a aplicação?",
                    "A aplicação será encerrada neste dispositivo."
            );
            if (!confirmado) {
                event.consume();
            }
        });
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }

    public static void main(String[] args) {
        Application.launch(Projeto2Application.class, args);
    }
}
