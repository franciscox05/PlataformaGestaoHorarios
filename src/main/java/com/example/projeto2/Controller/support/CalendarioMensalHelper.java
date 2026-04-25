package com.example.projeto2.Controller.support;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CalendarioMensalHelper {

    private static final Locale LOCALE_PT = Locale.forLanguageTag("pt-PT");

    private CalendarioMensalHelper() {
    }

    public static void preencherCalendario(GridPane grelha,
                                           YearMonth periodo,
                                           Map<LocalDate, List<String>> eventosPorDia,
                                           String mensagemVazia) {
        if (grelha == null || periodo == null) {
            return;
        }

        grelha.getChildren().clear();
        grelha.getColumnConstraints().clear();
        grelha.getRowConstraints().clear();
        grelha.setHgap(10.0);
        grelha.setVgap(10.0);

        for (int coluna = 0; coluna < 7; coluna++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setHgrow(Priority.ALWAYS);
            constraints.setFillWidth(true);
            constraints.setPercentWidth(100.0 / 7.0);
            grelha.getColumnConstraints().add(constraints);
        }

        RowConstraints cabecalho = new RowConstraints();
        cabecalho.setVgrow(Priority.NEVER);
        grelha.getRowConstraints().add(cabecalho);

        for (int i = 0; i < 6; i++) {
            RowConstraints linha = new RowConstraints();
            linha.setVgrow(Priority.ALWAYS);
            linha.setFillHeight(true);
            grelha.getRowConstraints().add(linha);
        }

        adicionarCabecalhos(grelha);
        adicionarDias(grelha, periodo, eventosPorDia, mensagemVazia);
    }

    private static void adicionarCabecalhos(GridPane grelha) {
        DayOfWeek[] ordem = {
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SUNDAY
        };

        for (int coluna = 0; coluna < ordem.length; coluna++) {
            Label cabecalho = new Label(
                    capitalizar(ordem[coluna].getDisplayName(TextStyle.SHORT, LOCALE_PT).replace(".", ""))
            );
            cabecalho.getStyleClass().add("calendario-mes-cabecalho");
            cabecalho.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHalignment(cabecalho, HPos.CENTER);
            grelha.add(cabecalho, coluna, 0);
        }
    }

    private static void adicionarDias(GridPane grelha,
                                      YearMonth periodo,
                                      Map<LocalDate, List<String>> eventosPorDia,
                                      String mensagemVazia) {
        LocalDate primeiroDia = periodo.atDay(1);
        int deslocamentoInicial = primeiroDia.getDayOfWeek().getValue() - 1;
        int totalDias = periodo.lengthOfMonth();
        int indiceCelula = 0;

        for (int linha = 1; linha <= 6; linha++) {
            for (int coluna = 0; coluna < 7; coluna++) {
                LocalDate data = null;
                int diaMes = indiceCelula - deslocamentoInicial + 1;
                if (diaMes >= 1 && diaMes <= totalDias) {
                    data = periodo.atDay(diaMes);
                }

                VBox card = criarCardDia(data, eventosPorDia, mensagemVazia);
                grelha.add(card, coluna, linha);
                indiceCelula++;
            }
        }
    }

    private static VBox criarCardDia(LocalDate data,
                                     Map<LocalDate, List<String>> eventosPorDia,
                                     String mensagemVazia) {
        VBox card = new VBox(8.0);
        card.getStyleClass().add("calendario-mes-dia-card");
        card.setPadding(new Insets(12.0));
        card.setFillWidth(true);
        card.setMinHeight(160.0);

        if (data == null) {
            card.getStyleClass().add("calendario-mes-dia-card-vazio");
            return card;
        }

        if (LocalDate.now().equals(data)) {
            card.getStyleClass().add("calendario-mes-dia-card-hoje");
        }

        Label lblDia = new Label(String.valueOf(data.getDayOfMonth()));
        lblDia.getStyleClass().add("calendario-mes-dia-numero");
        card.getChildren().add(lblDia);

        VBox corpo = new VBox(6.0);
        List<String> eventosValidos = eventosPorDia == null ? List.of() : eventosPorDia.getOrDefault(data, List.of()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .toList();

        if (eventosValidos.isEmpty()) {
            Label vazio = new Label(mensagemVazia != null ? mensagemVazia : "Sem horário");
            vazio.getStyleClass().add("calendario-mes-evento-vazio");
            vazio.setWrapText(true);
            corpo.getChildren().add(vazio);
        } else {
            for (String evento : eventosValidos) {
                Label lblEvento = new Label(evento);
                lblEvento.getStyleClass().add("calendario-mes-evento");
                lblEvento.setWrapText(true);
                corpo.getChildren().add(lblEvento);
            }
        }

        card.getChildren().add(corpo);
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
