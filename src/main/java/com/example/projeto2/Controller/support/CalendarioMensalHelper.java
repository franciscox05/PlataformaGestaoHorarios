package com.example.projeto2.Controller.support;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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

        List<String> eventosValidos = eventosPorDia == null ? List.of() : eventosPorDia.getOrDefault(data, List.of()).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(texto -> !texto.isBlank())
                .toList();

        Label lblDia = new Label(String.valueOf(data.getDayOfMonth()));
        lblDia.getStyleClass().add("calendario-mes-dia-numero");

        Label lblMeta = new Label(eventosValidos.isEmpty()
                ? "Sem turnos"
                : eventosValidos.size() + " turno(s)");
        lblMeta.getStyleClass().add("calendario-mes-dia-meta");

        Region espacador = new Region();
        HBox.setHgrow(espacador, Priority.ALWAYS);

        HBox topoDia = new HBox();
        topoDia.getStyleClass().add("calendario-mes-dia-topo");
        topoDia.getChildren().addAll(lblDia, espacador, lblMeta);

        VBox corpo = new VBox(6.0);

        if (eventosValidos.isEmpty()) {
            Label vazio = new Label(mensagemVazia != null ? mensagemVazia : "Sem horario");
            vazio.getStyleClass().add("calendario-mes-evento-vazio");
            vazio.setWrapText(true);
            corpo.getChildren().add(vazio);
        } else {
            for (String evento : eventosValidos) {
                corpo.getChildren().add(criarCardEvento(evento));
            }
        }

        card.getChildren().addAll(topoDia, corpo);
        VBox.setVgrow(corpo, Priority.ALWAYS);
        return card;
    }

    private static VBox criarCardEvento(String evento) {
        EventoMensalDetalhado detalhe = decomporEvento(evento);
        VBox cardEvento = new VBox(6.0);
        cardEvento.getStyleClass().add("calendario-mes-evento-card");

        if (detalhe == null) {
            Label fallback = new Label(evento);
            fallback.getStyleClass().add("calendario-mes-evento-texto");
            fallback.setWrapText(true);
            cardEvento.getChildren().add(fallback);
            return cardEvento;
        }

        cardEvento.getStyleClass().add("calendario-mes-evento-" + classificarBlocoHorario(detalhe.periodo()));

        HBox topo = new HBox(6.0);
        topo.getStyleClass().add("calendario-mes-evento-topo");

        Region ponto = new Region();
        ponto.getStyleClass().add("calendario-mes-evento-ponto");

        Label periodo = new Label(detalhe.periodo());
        periodo.getStyleClass().add("calendario-mes-evento-periodo");
        periodo.setWrapText(true);

        topo.getChildren().addAll(ponto, periodo);

        Label colaborador = new Label(detalhe.colaborador());
        colaborador.getStyleClass().add("calendario-mes-evento-colaborador");
        colaborador.setWrapText(true);

        Label cargo = new Label(abreviarCargo(detalhe.cargo()).toUpperCase(LOCALE_PT));
        cargo.getStyleClass().add("calendario-mes-evento-cargo");
        cargo.setWrapText(true);

        cardEvento.getChildren().addAll(topo, colaborador, cargo);
        return cardEvento;
    }

    private static EventoMensalDetalhado decomporEvento(String evento) {
        if (evento == null || evento.isBlank()) {
            return null;
        }

        int indiceSeparador = evento.indexOf('|');
        if (indiceSeparador < 0) {
            return null;
        }

        String periodo = evento.substring(0, indiceSeparador).trim();
        String restante = evento.substring(indiceSeparador + 1).trim();
        if (periodo.isBlank() || restante.isBlank()) {
            return null;
        }

        int indiceCargoInicio = restante.lastIndexOf('(');
        int indiceCargoFim = restante.lastIndexOf(')');
        if (indiceCargoInicio < 0 || indiceCargoFim <= indiceCargoInicio) {
            return new EventoMensalDetalhado(periodo, restante, "-");
        }

        String colaborador = restante.substring(0, indiceCargoInicio).trim();
        String cargo = restante.substring(indiceCargoInicio + 1, indiceCargoFim).trim();
        if (colaborador.isBlank()) {
            colaborador = restante;
        }
        if (cargo.isBlank()) {
            cargo = "-";
        }

        return new EventoMensalDetalhado(periodo, colaborador, cargo);
    }

    private static String classificarBlocoHorario(String periodo) {
        if (periodo == null || periodo.isBlank()) {
            return "generico";
        }

        try {
            String[] partes = periodo.split("-");
            if (partes.length == 0) {
                return "generico";
            }

            LocalTime inicio = LocalTime.parse(partes[0].trim());
            if (inicio.isBefore(LocalTime.NOON)) {
                return "manha";
            }
            if (inicio.isBefore(LocalTime.of(17, 0))) {
                return "intermedio";
            }
            return "noite";
        } catch (Exception e) {
            return "generico";
        }
    }

    private static String abreviarCargo(String cargo) {
        if (cargo == null || cargo.isBlank()) {
            return "-";
        }

        String cargoNormalizado = cargo.trim();
        String cargoMinusculo = normalizarTextoPesquisa(cargoNormalizado);

        if (cargoMinusculo.contains("sub-gerente") || cargoMinusculo.contains("subgerente")) {
            return "Sub-gerente";
        }
        if (cargoMinusculo.contains("gerente")) {
            return "Gerente";
        }
        if (cargoMinusculo.contains("supervisor")) {
            return "Supervisor";
        }
        if (cargoMinusculo.contains("reforco")) {
            return "Reforco FDS";
        }
        if (cargoMinusculo.contains("assistente de vendas ft")) {
            return "Vendas FT";
        }
        if (cargoMinusculo.contains("assistente de vendas pt")) {
            return "Vendas PT";
        }
        if (cargoMinusculo.contains("gestor operacional")) {
            return "Gestor Oper.";
        }

        return cargoNormalizado;
    }

    private static String normalizarTextoPesquisa(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(LOCALE_PT);
    }

    private static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "-";
        }

        String valor = texto.toLowerCase(LOCALE_PT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private record EventoMensalDetalhado(
            String periodo,
            String colaborador,
            String cargo
    ) {
    }
}
