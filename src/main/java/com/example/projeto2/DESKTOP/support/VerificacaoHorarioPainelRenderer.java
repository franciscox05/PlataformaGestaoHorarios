package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Renderiza os resultados de verificação de horário numa VBox (para a sub-página de verificação). */
public final class VerificacaoHorarioPainelRenderer {

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM");

    private VerificacaoHorarioPainelRenderer() {}

    public static void renderizar(VBox container,
                                  ValidacaoHorarioResultado resultado,
                                  CriteriosGeracao criterios,
                                  List<HorarioLinha> linhas,
                                  int ano) {
        container.getChildren().clear();
        container.setSpacing(10);
        container.setPadding(new Insets(0, 0, 16, 0));

        // ── Banner geral ───────────────────────────────────────────────────
        container.getChildren().add(construirBanner(resultado));

        // ── Secção: Regras ─────────────────────────────────────────────────
        container.getChildren().add(kicker("REGRAS CONFIGURADAS"));
        for (ValidacaoHorarioResultado.CategoriaValidacao cat : resultado.categorias()) {
            container.getChildren().add(construirCard(cat));
        }

        // ── Secção: Preferências de folga ──────────────────────────────────
        List<FolgaCumprimento> folgas = verificarFolgas(criterios.detalheFolgasPreferidas(), linhas, ano);
        if (!folgas.isEmpty()) {
            container.getChildren().add(kicker("FOLGAS PREFERIDAS"));
            container.getChildren().add(construirFolgasCard(folgas));
        }

        // ── Secção: Turno preferido (informativo) ──────────────────────────
        if (criterios.detalhePreferenciasTurno() != null && !criterios.detalhePreferenciasTurno().isEmpty()) {
            container.getChildren().add(kicker("PREFERÊNCIAS DE TURNO (INFORMATIVO)"));
            container.getChildren().add(construirListaInfoCard(
                    criterios.detalhePreferenciasTurno(),
                    "O motor tenta respeitar as preferências de turno, mas podem ser ultrapassadas por folgas ou carga."));
        }
    }

    // ── Construtores de nós ───────────────────────────────────────────────

    private static HBox construirBanner(ValidacaoHorarioResultado resultado) {
        boolean ok = resultado.estadoGeral() == ValidacaoHorarioResultado.Estado.OK;
        long total = resultado.categorias().stream()
                .mapToLong(c -> c.violacoes() == null ? 0 : c.violacoes().size()).sum();

        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(12, 14, 12, 14));
        String bg = ok ? "#f0fdf4" : "#fff7ed";
        String border = ok ? "#bbf7d0" : "#fed7aa";
        banner.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-width: 1px; -fx-border-radius: 8px; -fx-background-radius: 8px;");

        Label icon = new Label(ok ? "✓" : "⚠");
        icon.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + (ok ? "#16a34a" : "#c2410c") + ";");

        Label msg = new Label(ok
                ? "Tudo em ordem — todas as regras respeitadas."
                : total + " situação(ões) identificada(s) nas regras configuradas.");
        msg.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: "
                + (ok ? "#15803d" : "#9a3412") + ";");
        msg.setWrapText(true);
        HBox.setHgrow(msg, Priority.ALWAYS);
        banner.getChildren().addAll(icon, msg);
        return banner;
    }

    private static VBox construirCard(ValidacaoHorarioResultado.CategoriaValidacao cat) {
        boolean ok = cat.semViolacoes();
        VBox card = new VBox(6);
        card.setPadding(new Insets(10, 12, 10, 12));
        String bg = ok ? "#f9fafb" : "#fff1f2";
        String border = ok ? "#e5e7eb" : "#fecaca";
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        HBox titulo = new HBox(8);
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(ok ? "✓" : "✗");
        icon.setStyle("-fx-font-weight: 800; -fx-text-fill: " + (ok ? "#16a34a" : "#dc2626") + "; -fx-font-size: 13px;");
        Label nome = new Label(cat.nome());
        nome.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: " + (ok ? "#166534" : "#7f1d1d") + ";");
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        Label resumoLabel = new Label(ok ? "OK" : cat.violacoes().size() + " caso(s)");
        resumoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#15803d" : "#b91c1c") + "; -fx-font-weight: 600;");
        titulo.getChildren().addAll(icon, nome, esp, resumoLabel);
        card.getChildren().add(titulo);

        if (!ok) {
            Label resumo = new Label(cat.resumo() + ":");
            resumo.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f1d1d;");
            resumo.setWrapText(true);
            card.getChildren().add(resumo);
            List<String> lista = cat.violacoes().size() > 8 ? cat.violacoes().subList(0, 8) : cat.violacoes();
            for (String v : lista) {
                Label lv = new Label("  • " + v);
                lv.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
                lv.setWrapText(true);
                card.getChildren().add(lv);
            }
            if (cat.violacoes().size() > 8) {
                Label mais = new Label("  … e mais " + (cat.violacoes().size() - 8) + " situação(ões)");
                mais.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
                card.getChildren().add(mais);
            }
        } else {
            Label ok2 = new Label(cat.resumo());
            ok2.setStyle("-fx-font-size: 11px; -fx-text-fill: #166534;");
            ok2.setWrapText(true);
            card.getChildren().add(ok2);
        }
        return card;
    }

    private static VBox construirFolgasCard(List<FolgaCumprimento> folgas) {
        long cumpridas = folgas.stream().filter(f -> f.cumprida()).count();
        boolean todasOk = cumpridas == folgas.size();

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        String bg = todasOk ? "#f9fafb" : "#fffbeb";
        String border = todasOk ? "#e5e7eb" : "#fde68a";
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(todasOk ? "✓" : "~");
        icon.setStyle("-fx-font-weight: 800; -fx-text-fill: " + (todasOk ? "#16a34a" : "#92400e") + "; -fx-font-size: 13px;");
        Label titulo = new Label("Folgas preferidas");
        titulo.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: " + (todasOk ? "#166534" : "#78350f") + ";");
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        Label cnt = new Label(cumpridas + "/" + folgas.size() + " cumpridas");
        cnt.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + (todasOk ? "#15803d" : "#b45309") + ";");
        header.getChildren().addAll(icon, titulo, esp, cnt);
        card.getChildren().add(header);

        DateTimeFormatter fmtDisplay = DateTimeFormatter.ofPattern("d/MM");
        for (FolgaCumprimento f : folgas) {
            boolean ok = f.cumprida();
            Label lbl = new Label((ok ? "  ✓  " : "  ✗  ")
                    + f.nome() + " — " + fmtDisplay.format(f.data())
                    + (ok ? "" : "  (trabalhou neste dia)"));
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#166534" : "#b91c1c") + ";");
            lbl.setWrapText(true);
            card.getChildren().add(lbl);
        }
        return card;
    }

    private static VBox construirListaInfoCard(List<String> itens, String nota) {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; "
                + "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        Label notaLabel = new Label(nota);
        notaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
        notaLabel.setWrapText(true);
        card.getChildren().add(notaLabel);

        List<String> mostrar = itens.size() > 10 ? itens.subList(0, 10) : itens;
        for (String item : mostrar) {
            Label lbl = new Label("  •  " + item);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
            lbl.setWrapText(true);
            card.getChildren().add(lbl);
        }
        if (itens.size() > 10) {
            Label mais = new Label("  … e mais " + (itens.size() - 10));
            mais.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
            card.getChildren().add(mais);
        }
        return card;
    }

    private static Label kicker(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #9ca3af; "
                + "-fx-letter-spacing: 0.5px;");
        VBox.setMargin(lbl, new Insets(6, 0, 0, 0));
        return lbl;
    }

    // ── Preferências de folga ─────────────────────────────────────────────

    private record FolgaCumprimento(String nome, LocalDate data, boolean cumprida) {}

    private static List<FolgaCumprimento> verificarFolgas(List<String> detalhes,
                                                           List<HorarioLinha> linhas,
                                                           int ano) {
        if (detalhes == null || detalhes.isEmpty()) return List.of();

        Set<String> trabalhouEm = linhas == null ? Set.of() : linhas.stream()
                .filter(l -> l.data() != null && l.colaborador() != null)
                .map(l -> l.colaborador().trim() + "|" + l.data())
                .collect(Collectors.toSet());

        List<FolgaCumprimento> resultado = new ArrayList<>();
        for (String detalhe : detalhes) {
            String[] partes = detalhe.split(" — ", 2);
            if (partes.length < 2) continue;
            String nome = partes[0].trim();
            String dataStr = partes[1].trim();
            try {
                MonthDay md = MonthDay.parse(dataStr, FMT_DATA);
                LocalDate data = LocalDate.of(ano, md.getMonth(), md.getDayOfMonth());
                boolean trabalhou = trabalhouEm.contains(nome + "|" + data);
                resultado.add(new FolgaCumprimento(nome, data, !trabalhou));
            } catch (Exception e) {
                // skip unparseable entries
            }
        }
        return resultado;
    }
}
