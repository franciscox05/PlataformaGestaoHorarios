package com.example.projeto2.Controller.support;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CalendarioSemanalHelper {

    private static final DateTimeFormatter DATA_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter INTERVALO = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("pt-PT"));
    private static final Locale LOCALE_PT = Locale.forLanguageTag("pt-PT");

    private CalendarioSemanalHelper() {
    }

    public static LocalDate inicioSemana(LocalDate dataBase) {
        if (dataBase == null) {
            dataBase = LocalDate.now();
        }
        return dataBase.with(DayOfWeek.MONDAY);
    }

    public static String formatarIntervaloSemana(LocalDate inicioSemana) {
        LocalDate fimSemana = inicioSemana.plusDays(6);
        return "Semana de " + INTERVALO.format(inicioSemana) + " a " + INTERVALO.format(fimSemana);
    }

    public static void preencherCalendario(HBox contentor,
                                           LocalDate inicioSemana,
                                           Map<LocalDate, List<String>> eventosPorDia,
                                           String mensagemVazia) {
        if (contentor == null || inicioSemana == null) {
            return;
        }

        contentor.getChildren().clear();

        for (int i = 0; i < 7; i++) {
            LocalDate dia = inicioSemana.plusDays(i);
            VBox card = criarCardDia(dia, eventosPorDia != null ? eventosPorDia.get(dia) : null, mensagemVazia);
            HBox.setHgrow(card, Priority.ALWAYS);
            contentor.getChildren().add(card);
        }
    }

    private static VBox criarCardDia(LocalDate dia, List<String> eventos, String mensagemVazia) {
        Label lblDiaSemana = new Label(
                capitalizar(dia.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, LOCALE_PT))
        );
        lblDiaSemana.getStyleClass().add("calendario-dia-semana");

        Label lblData = new Label(DATA_DIA.format(dia));
        lblData.getStyleClass().add("calendario-dia-data");

        VBox cabecalho = new VBox(2.0, lblDiaSemana, lblData);

        VBox corpo = new VBox(6.0);
        corpo.getStyleClass().add("calendario-dia-corpo");

        List<String> eventosValidos = eventos == null ? List.of() : eventos.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .toList();

        if (eventosValidos.isEmpty()) {
            Label vazio = new Label(mensagemVazia != null ? mensagemVazia : "Sem horário");
            vazio.getStyleClass().add("calendario-evento-vazio");
            vazio.setWrapText(true);
            corpo.getChildren().add(vazio);
        } else {
            for (String evento : eventosValidos) {
                Label item = new Label(evento);
                item.getStyleClass().add("calendario-evento");
                item.setWrapText(true);
                corpo.getChildren().add(item);
            }
        }

        VBox card = new VBox(10.0, cabecalho, corpo);
        card.getStyleClass().add("calendario-dia-card");
        card.setPadding(new Insets(14));
        card.setMinWidth(0);
        card.setPrefWidth(0);
        VBox.setVgrow(corpo, Priority.ALWAYS);
        return card;
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }

        String valor = texto.toLowerCase(LOCALE_PT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }
}
