package com.example.projeto2.DESKTOP.support;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/** Dialog de compliance: mostra, regra a regra, se a proposta respeita os requisitos. */
public final class ValidacaoHorarioDialog {

    private ValidacaoHorarioDialog() {}

    public static void abrir(ValidacaoHorarioResultado resultado, String tituloContexto, Window owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) stage.initOwner(owner);
        stage.setTitle("Verificação do horário");
        stage.setResizable(false);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-pref-width: 560px;");

        // ── Cabeçalho ───────────────────────────────────────────────────────
        VBox header = new VBox(4);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        Label titulo = new Label("Verificação do horário");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");
        Label subtitulo = new Label(tituloContexto);
        subtitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        header.getChildren().addAll(titulo, subtitulo);

        // Banner de estado geral
        Node banner = construirBanner(resultado);
        header.getChildren().add(banner);
        root.getChildren().add(header);

        // ── Corpo: categorias ────────────────────────────────────────────────
        VBox corpo = new VBox(6);
        corpo.setPadding(new Insets(12, 16, 12, 16));

        if (resultado.categorias().isEmpty()) {
            Label vazio = new Label("Sem dados suficientes para validar o horário.");
            vazio.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px;");
            vazio.setPadding(new Insets(16));
            corpo.getChildren().add(vazio);
        } else {
            for (ValidacaoHorarioResultado.CategoriaValidacao cat : resultado.categorias()) {
                corpo.getChildren().add(construirCategoria(cat));
            }
        }

        ScrollPane scroll = new ScrollPane(corpo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        scroll.setMaxHeight(420);
        root.getChildren().add(scroll);

        // Nota de rodapé
        if (resultado.estadoGeral() == ValidacaoHorarioResultado.Estado.VIOLACAO) {
            Label nota = new Label(
                    "Para corrigir: edita turnos diretamente na grelha (clica numa célula) " +
                    "ou gera uma nova alternativa com \"Gerar proposta\".");
            nota.setWrapText(true);
            nota.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
            nota.setPadding(new Insets(8, 24, 4, 24));
            root.getChildren().add(nota);
        }

        // ── Botão fechar ─────────────────────────────────────────────────────
        HBox rodape = new HBox();
        rodape.setAlignment(Pos.CENTER_RIGHT);
        rodape.setPadding(new Insets(12, 24, 16, 24));
        rodape.setStyle("-fx-background-color: #f9fafb; -fx-border-color: #e5e7eb; -fx-border-width: 1 0 0 0;");
        Button btnFechar = new Button("Fechar");
        btnFechar.setStyle("-fx-background-color: #c91428; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-background-radius: 6px; -fx-padding: 8px 20px; -fx-cursor: hand;");
        btnFechar.setOnAction(e -> stage.close());
        rodape.getChildren().add(btnFechar);
        root.getChildren().add(rodape);

        stage.setScene(new Scene(root));
        stage.showAndWait();
    }

    private static Node construirBanner(ValidacaoHorarioResultado resultado) {
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(10, 12, 10, 12));
        banner.setStyle("-fx-background-radius: 8px;");

        boolean ok = resultado.estadoGeral() == ValidacaoHorarioResultado.Estado.OK;
        long nViolacoes = resultado.categorias().stream()
                .mapToLong(c -> c.violacoes() == null ? 0 : c.violacoes().size()).sum();

        if (ok) {
            banner.setStyle(banner.getStyle() + " -fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-width: 1px; -fx-border-radius: 8px;");
            Label icon = new Label("✓");
            icon.setStyle("-fx-font-size: 20px; -fx-text-fill: #16a34a; -fx-font-weight: 700;");
            Label msg = new Label("Tudo em ordem — o horário respeita todas as regras configuradas.");
            msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #15803d; -fx-font-weight: 600;");
            msg.setWrapText(true);
            HBox.setHgrow(msg, Priority.ALWAYS);
            banner.getChildren().addAll(icon, msg);
        } else {
            banner.setStyle(banner.getStyle() + " -fx-background-color: #fff7ed; -fx-border-color: #fed7aa; -fx-border-width: 1px; -fx-border-radius: 8px;");
            Label icon = new Label("⚠");
            icon.setStyle("-fx-font-size: 20px; -fx-text-fill: #c2410c;");
            Label msg = new Label(nViolacoes + " situação(ões) a corrigir — ver detalhes em cada regra abaixo.");
            msg.setStyle("-fx-font-size: 13px; -fx-text-fill: #9a3412; -fx-font-weight: 600;");
            msg.setWrapText(true);
            HBox.setHgrow(msg, Priority.ALWAYS);
            banner.getChildren().addAll(icon, msg);
        }
        VBox.setMargin(banner, new Insets(10, 0, 0, 0));
        return banner;
    }

    private static TitledPane construirCategoria(ValidacaoHorarioResultado.CategoriaValidacao cat) {
        boolean ok = cat.semViolacoes();

        // Conteúdo interno
        VBox conteudo = new VBox(4);
        conteudo.setPadding(new Insets(6, 12, 8, 12));

        if (ok) {
            Label lblOk = new Label("✓  " + cat.resumo());
            lblOk.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803d;");
            lblOk.setWrapText(true);
            conteudo.getChildren().add(lblOk);
        } else {
            Label lblResumo = new Label(cat.resumo() + ":");
            lblResumo.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f1d1d; -fx-font-weight: 600;");
            lblResumo.setWrapText(true);
            conteudo.getChildren().add(lblResumo);
            for (String v : limitarLista(cat.violacoes(), 10)) {
                Label lblV = new Label("  •  " + v);
                lblV.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
                lblV.setWrapText(true);
                conteudo.getChildren().add(lblV);
            }
            if (cat.violacoes().size() > 10) {
                Label mais = new Label("  … e mais " + (cat.violacoes().size() - 10) + " situação(ões)");
                mais.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
                conteudo.getChildren().add(mais);
            }
        }

        // Cabeçalho da TitledPane
        String icone = ok ? "✓" : "✗";
        String cor   = ok ? "#15803d" : "#b91c1c";
        String bgCor = ok ? "#f0fdf4" : "#fff1f2";

        Label cabecalho = new Label(icone + "  " + cat.nome());
        cabecalho.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: " + cor + ";");

        TitledPane pane = new TitledPane();
        pane.setGraphic(cabecalho);
        pane.setText("");
        pane.setContent(conteudo);
        pane.setExpanded(!ok);
        pane.setStyle("-fx-background-color: " + bgCor + "; -fx-border-radius: 6px;");
        return pane;
    }

    private static List<String> limitarLista(List<String> lista, int max) {
        if (lista == null) return List.of();
        return lista.size() <= max ? lista : lista.subList(0, max);
    }
}
