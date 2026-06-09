package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Diálogo "Horário individual": mostra todos os turnos de um colaborador na proposta,
 * agrupados por semana, com estatísticas no topo.
 */
public final class HorarioIndividualDialog {

    private HorarioIndividualDialog() {
        // utilitário
    }

    public static void abrir(GeracaoHorariosService.ResumoColaborador colaborador,
                             List<GeracaoHorariosService.HorarioLinha> linhasProposta,
                             Window owner) {
        if (colaborador == null || linhasProposta == null) return;

        List<GeracaoHorariosService.HorarioLinha> turnos = linhasProposta.stream()
                .filter(l -> colaborador.idColaborador() != null
                        && colaborador.idColaborador().equals(l.idColaborador()))
                .sorted(Comparator.comparing(GeracaoHorariosService.HorarioLinha::data,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        VBox conteudo = new VBox(16.0);
        conteudo.getStyleClass().add("horario-individual-conteudo");

        HBox statBar = new HBox(24.0);
        statBar.getStyleClass().add("horario-individual-stat-bar");
        statBar.setAlignment(Pos.CENTER_LEFT);
        statBar.getChildren().addAll(
                criarStatMini("Turnos no mês", String.valueOf(colaborador.turnos())),
                criarStatMini("Horas contratuais", colaborador.horasFormatadas())
        );
        conteudo.getChildren().add(statBar);

        if (turnos.isEmpty()) {
            Label vazio = new Label("Este colaborador não tem turnos atribuídos nesta proposta.");
            vazio.getStyleClass().add("horario-individual-vazio");
            vazio.setWrapText(true);
            conteudo.getChildren().add(vazio);
        } else {
            Map<LocalDate, List<GeracaoHorariosService.HorarioLinha>> porSemana = new LinkedHashMap<>();
            for (GeracaoHorariosService.HorarioLinha linha : turnos) {
                LocalDate inicioSem = CalendarioSemanalHelper.inicioSemana(linha.data());
                porSemana.computeIfAbsent(inicioSem, k -> new java.util.ArrayList<>()).add(linha);
            }
            for (Map.Entry<LocalDate, List<GeracaoHorariosService.HorarioLinha>> entrada : porSemana.entrySet()) {
                conteudo.getChildren().add(criarBlocoSemana(entrada.getKey(), entrada.getValue()));
            }
        }

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("horario-individual-scroll");
        scroll.setMaxHeight(500.0);
        scroll.setMinWidth(480.0);

        DialogosHelper.mostrarConteudo(
                owner,
                "HORÁRIO INDIVIDUAL",
                colaborador.nome() + "  ·  " + colaborador.cargo(),
                colaborador.turnos() + " turno(s) atribuído(s)  ·  " + colaborador.horasFormatadas(),
                scroll
        );
    }

    private static VBox criarStatMini(String etiqueta, String valor) {
        VBox box = new VBox(3.0);
        box.getStyleClass().add("horario-individual-stat");
        Label lEtiqueta = new Label(etiqueta.toUpperCase(Locale.ROOT));
        lEtiqueta.getStyleClass().add("horario-individual-stat-etiqueta");
        Label lValor = new Label(valor);
        lValor.getStyleClass().add("horario-individual-stat-valor");
        box.getChildren().addAll(lEtiqueta, lValor);
        return box;
    }

    private static VBox criarBlocoSemana(LocalDate inicioSemana,
                                          List<GeracaoHorariosService.HorarioLinha> linhas) {
        VBox bloco = new VBox(6.0);
        bloco.getStyleClass().add("horario-individual-semana");

        Label lblSemana = new Label(CalendarioSemanalHelper.formatarIntervaloSemana(inicioSemana));
        lblSemana.getStyleClass().add("horario-individual-semana-titulo");
        bloco.getChildren().add(lblSemana);

        for (GeracaoHorariosService.HorarioLinha linha : linhas) {
            bloco.getChildren().add(criarLinhaTurno(linha));
        }
        return bloco;
    }

    private static HBox criarLinhaTurno(GeracaoHorariosService.HorarioLinha linha) {
        HBox hbox = new HBox(12.0);
        hbox.getStyleClass().add("horario-individual-turno");
        hbox.setAlignment(Pos.CENTER_LEFT);

        String diaCurto = linha.diaSemana() != null && linha.diaSemana().length() >= 3
                ? linha.diaSemana().substring(0, 3).toUpperCase(Locale.ROOT)
                : "---";
        Label lblDia = new Label(diaCurto);
        lblDia.getStyleClass().add("horario-individual-dia");
        lblDia.setMinWidth(36.0);

        String dataStr = linha.data() != null
                ? linha.data().format(DateTimeFormatter.ofPattern("dd/MM"))
                : "--/--";
        Label lblData = new Label(dataStr);
        lblData.getStyleClass().add("horario-individual-data");
        lblData.setMinWidth(44.0);

        Label lblPeriodo = new Label(valorOuTraco(linha.periodo()));
        lblPeriodo.getStyleClass().add("horario-individual-periodo");
        HBox.setHgrow(lblPeriodo, Priority.ALWAYS);

        Label lblEstado = new Label(valorOuTraco(linha.estado()));
        lblEstado.getStyleClass().add("horario-individual-estado");

        hbox.getChildren().addAll(lblDia, lblData, lblPeriodo, lblEstado);
        return hbox;
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }
}
