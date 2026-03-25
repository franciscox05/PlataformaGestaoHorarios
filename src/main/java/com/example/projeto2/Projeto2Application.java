package com.example.projeto2;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Projeto2Application extends Application {

    private ConfigurableApplicationContext springContext;

    // 1. O motor do Spring Boot arranca e liga à Base de Dados
    @Override
    public void init() throws Exception {
        springContext = new SpringApplicationBuilder(Projeto2Application.class).run();
    }

    // 2. O motor do JavaFX desenha a janela no ecrã
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Por agora, é apenas uma janela branca com texto para testar a ligação
        StackPane root = new StackPane();
        root.getChildren().add(new Text("A carregar a interface da Levi's..."));

        Scene scene = new Scene(root, 800, 600);

        primaryStage.setTitle("Levi's Staff Portal");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // 3. Quando fechas a janela no "X", o Spring Boot desliga-se em segurança
    @Override
    public void stop() throws Exception {
        springContext.close();
        Platform.exit();
    }

    // O main agora chama o JavaFX, que por sua vez chama o Spring Boot
    public static void main(String[] args) {
        Application.launch(Projeto2Application.class, args);
    }
}