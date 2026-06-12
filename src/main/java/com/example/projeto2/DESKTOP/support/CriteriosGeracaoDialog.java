package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Diálogo de transparência da geração: mostra ao gestor todas as regras hard,
 * cargas contratuais, mínimos por turno e o volume de preferências/ausências que
 * o motor vai ter em conta no período selecionado. Sem isto, a geração é uma
 * caixa-negra — quando falha ou surpreende, o gestor não sabe onde mexer.
 */
public final class CriteriosGeracaoDialog {

    private CriteriosGeracaoDialog() {
    }

    public static void abrir(CriteriosGeracao criterios, String periodo, Window owner) {
        VBox conteudo = new VBox(14);
        conteudo.setPadding(new Insets(4, 0, 0, 0));
        conteudo.setMaxWidth(560);

        conteudo.getChildren().add(seccao("Regras de trabalho (obrigatórias)",
                "Descanso mínimo entre turnos: " + criterios.descansoMinimoHoras() + " horas",
                "Máximo de dias seguidos de trabalho: " + criterios.maxDiasConsecutivos(),
                "Dias de folga por semana: " + criterios.descansoSemanalMinimoDias(),
                "Intervalo entre fins de semana trabalhados: " + criterios.janelaRotacaoFimDeSemana() + " semana"
                        + (criterios.janelaRotacaoFimDeSemana() == 1 ? "" : "s"),
                criterios.exigirChefiaAoSabado()
                        ? "Sábados exigem presença de gerente ou subgerente"
                        : "Sábados não exigem chefia obrigatória"));

        conteudo.getChildren().add(seccao("Cobertura mínima por turno",
                criterios.minimosPorTurno().toArray(String[]::new)));

        conteudo.getChildren().add(seccao("Carga contratual mensal por perfil",
                criterios.cargasPorPerfil().toArray(String[]::new)));

        conteudo.getChildren().add(seccao("Equipa e pedidos no período",
                criterios.totalColaboradoresElegiveis() + " colaboradores elegíveis",
                criterios.totalAusenciasAprovadas() + " ausências aprovadas (folgas/férias — dias bloqueados)",
                criterios.totalFolgasPreferidas() + " folgas preferidas (o motor tenta honrar)",
                criterios.totalPreferenciasTurno() + " preferências de turno aprovadas (favorecidas na atribuição)",
                criterios.totalPreferenciasColegas() + " preferências de colegas aprovadas",
                criterios.totalDiasEspeciais() + " dias com horário especial ou encerramento"));

        Label nota = new Label("As ausências aprovadas são respeitadas sempre. As preferências (folga, turno, colegas) "
                + "são tidas em conta na escolha dos candidatos, mas podem ceder quando a cobertura mínima o exigir.");
        nota.setWrapText(true);
        nota.getStyleClass().add("bento-card-subtitulo");
        conteudo.getChildren().add(nota);

        DialogosHelper.mostrarConteudo(
                owner,
                "Critérios da geração",
                "O que o motor considera para " + periodo,
                "Tudo o que entra na criação do horário deste período:",
                conteudo
        );
    }

    private static VBox seccao(String titulo, String... linhas) {
        VBox box = new VBox(5);
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("campo-titulo");
        box.getChildren().add(lblTitulo);
        for (String linha : linhas) {
            Label lbl = new Label("•  " + linha);
            lbl.setWrapText(true);
            box.getChildren().add(lbl);
        }
        return box;
    }
}
