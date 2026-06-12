package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.List;

/**
 * Diálogo de transparência da geração: mostra ao gestor todas as regras hard,
 * cargas contratuais, mínimos por turno, o balanço capacidade/necessidade e o
 * detalhe (expansível) de cada preferência e ausência que o motor vai considerar.
 * Sem isto, a geração é uma caixa-negra — quando falha ou surpreende, o gestor
 * não sabe onde mexer.
 */
public final class CriteriosGeracaoDialog {

    private CriteriosGeracaoDialog() {
    }

    public static void abrir(CriteriosGeracao criterios, String periodo, Window owner) {
        VBox conteudo = new VBox(10);
        conteudo.setPadding(new Insets(4, 0, 0, 0));

        // Balanço de capacidade — a informação mais importante: chega ou não chega?
        conteudo.getChildren().add(construirBalancoCapacidade(criterios));

        conteudo.getChildren().add(seccaoExpansivel("Regras de trabalho (obrigatórias)", true, List.of(
                "Descanso mínimo entre turnos: " + criterios.descansoMinimoHoras() + " horas",
                "Máximo de dias seguidos de trabalho: " + criterios.maxDiasConsecutivos(),
                "Dias de folga por semana: " + criterios.descansoSemanalMinimoDias(),
                "Intervalo entre fins de semana trabalhados: " + criterios.janelaRotacaoFimDeSemana()
                        + " semana" + (criterios.janelaRotacaoFimDeSemana() == 1 ? "" : "s"),
                criterios.exigirChefiaAoSabado()
                        ? "Sábados exigem presença de gerente ou subgerente"
                        : "Sábados não exigem chefia obrigatória")));

        conteudo.getChildren().add(seccaoExpansivel("Cobertura mínima por turno", true,
                criterios.minimosPorTurno()));

        conteudo.getChildren().add(seccaoExpansivel("Carga contratual mensal por perfil", false,
                criterios.cargasPorPerfil()));

        conteudo.getChildren().add(seccaoExpansivel(
                "Equipa elegível (" + criterios.detalheColaboradores().size() + ")", false,
                criterios.detalheColaboradores()));

        conteudo.getChildren().add(seccaoExpansivel(
                "Ausências aprovadas — dias bloqueados (" + criterios.detalheAusencias().size() + ")", false,
                vazioOu(criterios.detalheAusencias(), "Sem ausências aprovadas neste período.")));

        conteudo.getChildren().add(seccaoExpansivel(
                "Folgas preferidas — o motor tenta honrar (" + criterios.detalheFolgasPreferidas().size() + ")", false,
                vazioOu(criterios.detalheFolgasPreferidas(), "Sem folgas preferidas aprovadas neste período.")));

        conteudo.getChildren().add(seccaoExpansivel(
                "Preferências de turno (" + criterios.detalhePreferenciasTurno().size() + ")", false,
                vazioOu(criterios.detalhePreferenciasTurno(), "Sem preferências de turno aprovadas.")));

        conteudo.getChildren().add(seccaoExpansivel(
                "Preferências de colegas (" + criterios.detalhePreferenciasColegas().size() + ")", false,
                vazioOu(criterios.detalhePreferenciasColegas(), "Sem preferências de colegas aprovadas.")));

        conteudo.getChildren().add(seccaoExpansivel(
                "Dias com horário especial (" + criterios.detalheDiasEspeciais().size() + ")", false,
                vazioOu(criterios.detalheDiasEspeciais(), "Sem dias especiais nem encerramentos.")));

        Label nota = new Label("As ausências aprovadas são respeitadas sempre. As preferências (folga, turno, colegas) "
                + "são tidas em conta na escolha dos candidatos, mas podem ceder quando a cobertura mínima o exigir.");
        nota.setWrapText(true);
        nota.getStyleClass().add("bento-card-subtitulo");
        conteudo.getChildren().add(nota);

        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(520);
        scroll.setMaxHeight(560);
        scroll.setStyle("-fx-background-color: transparent;");

        VBox wrapper = new VBox(scroll);
        wrapper.setMaxWidth(620);
        wrapper.setPrefWidth(620);

        DialogosHelper.mostrarConteudo(
                owner,
                "Critérios da geração",
                "O que o motor considera para " + periodo,
                "Expande cada secção para veres o detalhe ao pormenor:",
                wrapper
        );
    }

    private static VBox construirBalancoCapacidade(CriteriosGeracao criterios) {
        boolean suficiente = criterios.capacidadeSuficiente();
        long folga = criterios.capacidadeEquipaHoras() - criterios.necessidadeMinimaHoras();

        VBox box = new VBox(4);
        box.setPadding(new Insets(10));
        box.setStyle(suficiente
                ? "-fx-background-color: #f0fdf4; -fx-background-radius: 8; -fx-border-color: #bbf7d0; -fx-border-radius: 8;"
                : "-fx-background-color: #fef2f2; -fx-background-radius: 8; -fx-border-color: #fecaca; -fx-border-radius: 8;");

        Label titulo = new Label(suficiente
                ? "✓  Capacidade suficiente"
                : "⚠  Capacidade insuficiente — a geração vai falhar");
        titulo.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (suficiente ? "#15803d" : "#b91c1c") + ";");

        Label detalhe = new Label("A equipa tem " + criterios.capacidadeEquipaHoras()
                + "h de carga contratual; a cobertura mínima do mês exige ~"
                + criterios.necessidadeMinimaHoras() + "h"
                + (suficiente
                        ? " (folga de ~" + folga + "h para turnos extra)."
                        : " (faltam ~" + (-folga) + "h — reduz os mínimos por turno, inclui mais colaboradores ou aumenta as cargas)."));
        detalhe.setWrapText(true);

        box.getChildren().addAll(titulo, detalhe);
        return box;
    }

    private static TitledPane seccaoExpansivel(String titulo, boolean expandida, List<String> linhas) {
        VBox corpo = new VBox(5);
        corpo.setPadding(new Insets(6, 4, 6, 4));
        for (String linha : linhas) {
            Label lbl = new Label("•  " + linha);
            lbl.setWrapText(true);
            corpo.getChildren().add(lbl);
        }
        TitledPane pane = new TitledPane(titulo, corpo);
        pane.setExpanded(expandida);
        pane.setAnimated(false);
        return pane;
    }

    private static List<String> vazioOu(List<String> linhas, String mensagemVazio) {
        return linhas.isEmpty() ? List.of(mensagemVazio) : linhas;
    }
}
