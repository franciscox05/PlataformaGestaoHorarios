package com.example.projeto2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Projeto2Application extends Application {

    private static final double APP_WIDTH = 1480;
    private static final double APP_HEIGHT = 920;
    private static final double APP_MIN_WIDTH = 1280;
    private static final double APP_MIN_HEIGHT = 780;

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(Projeto2Application.class).run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                Projeto2Application.class.getResource("login/login-view.fxml")
        );

        fxmlLoader.setControllerFactory(springContext::getBean);

        Scene scene = new Scene(fxmlLoader.load(), APP_WIDTH, APP_HEIGHT);

        primaryStage.setTitle("Levi's Staff Portal - Autenticacao");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(APP_MIN_WIDTH);
        primaryStage.setMinHeight(APP_MIN_HEIGHT);
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
