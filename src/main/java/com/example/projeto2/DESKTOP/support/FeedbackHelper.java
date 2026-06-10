package com.example.projeto2.DESKTOP.support;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

public final class FeedbackHelper {

    private static final String CLASSE_BASE = "mensagem-feedback";
    private static final String CLASSE_SUCESSO = "mensagem-sucesso";
    private static final String CLASSE_ERRO = "mensagem-erro";
    private static final Duration DURACAO_AUTO_DISMISS = Duration.seconds(5);

    private FeedbackHelper() {
    }

    public static void mostrar(Label label, String mensagem, boolean sucesso) {
        label.setText(mensagem);
        label.getStyleClass().removeAll(CLASSE_SUCESSO, CLASSE_ERRO);
        if (!label.getStyleClass().contains(CLASSE_BASE)) {
            label.getStyleClass().add(CLASSE_BASE);
        }
        label.getStyleClass().add(sucesso ? CLASSE_SUCESSO : CLASSE_ERRO);
        label.setManaged(true);
        label.setVisible(true);

        if (sucesso) {
            PauseTransition pausa = new PauseTransition(DURACAO_AUTO_DISMISS);
            pausa.setOnFinished(e -> esconder(label));
            pausa.play();
        }
    }

    public static void esconder(Label label) {
        label.setText("");
        label.setManaged(false);
        label.setVisible(false);
        label.getStyleClass().removeAll(CLASSE_SUCESSO, CLASSE_ERRO);
        if (!label.getStyleClass().contains(CLASSE_BASE)) {
            label.getStyleClass().add(CLASSE_BASE);
        }
    }
}
