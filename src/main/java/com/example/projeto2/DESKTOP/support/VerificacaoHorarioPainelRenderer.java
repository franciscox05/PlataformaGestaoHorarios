package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.ColaboradorCumprimento;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.Estado;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.Item;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Renderiza a verificação de horário numa VBox (sub-página do passo "Rever").
 *
 * <p>Estrutura, de cima para baixo:
 * <ol>
 *   <li><b>Veredicto</b> — banner com o resumo de uma linha.</li>
 *   <li><b>Scorecard</b> — quatro indicadores: regras obrigatórias, preferências,
 *       carga horária e fins de semana.</li>
 *   <li><b>Filtro</b> — "mostrar só o que precisa de atenção".</li>
 *   <li><b>Regras obrigatórias</b> — cartões por regra hard (do {@link ValidadorHorarioProposta}).</li>
 *   <li><b>Por colaborador</b> — cartão por funcionário com folgas/turnos/colegas/carga
 *       verificados (do {@link AnaliseCumprimentoHorario}).</li>
 *   <li><b>Distribuição de fins de semana</b> e <b>recomendações</b>.</li>
 * </ol>
 */
public final class VerificacaoHorarioPainelRenderer {

    // Paleta por estado
    private static final String VERDE = "#16a34a", AMBAR = "#d97706", VERMELHO = "#dc2626", CINZA = "#64748b";

    private VerificacaoHorarioPainelRenderer() {}

    public static void renderizar(VBox container,
                                  ValidacaoHorarioResultado resultado,
                                  CriteriosGeracao criterios,
                                  List<HorarioLinha> linhas,
                                  int ano) {
        container.getChildren().clear();
        container.setSpacing(12);
        container.setPadding(new Insets(0, 0, 16, 0));

        AnaliseCumprimentoHorario.Resultado analise =
                AnaliseCumprimentoHorario.analisar(linhas, criterios, ano);

        // 1 + 2 — veredicto e scorecard (sempre visíveis)
        container.getChildren().add(construirVeredicto(resultado, analise));
        container.getChildren().add(construirScorecard(resultado, analise));

        // 3 — filtro
        CheckBox cbProblemas = new CheckBox("Mostrar só o que precisa de atenção");
        cbProblemas.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        HBox filtro = new HBox(cbProblemas);
        filtro.setPadding(new Insets(0, 2, 0, 2));

        // corpo redesenhável conforme o filtro
        VBox corpo = new VBox(10);
        Runnable redesenhar = () -> desenharCorpo(corpo, resultado, analise, linhas, cbProblemas.isSelected());
        cbProblemas.setOnAction(e -> redesenhar.run());

        container.getChildren().addAll(filtro, corpo);
        redesenhar.run();
    }

    // ── Corpo (responde ao filtro) ───────────────────────────────────────────

    private static void desenharCorpo(VBox corpo,
                                      ValidacaoHorarioResultado resultado,
                                      AnaliseCumprimentoHorario.Resultado analise,
                                      List<HorarioLinha> linhas,
                                      boolean soProblemas) {
        corpo.getChildren().clear();

        // ── Regras obrigatórias ───────────────────────────────────────────
        corpo.getChildren().add(kicker("REGRAS OBRIGATÓRIAS"));
        List<ValidacaoHorarioResultado.CategoriaValidacao> regras = resultado.categorias();
        long regrasOk = regras.stream().filter(ValidacaoHorarioResultado.CategoriaValidacao::semViolacoes).count();
        boolean mostrouRegra = false;
        for (ValidacaoHorarioResultado.CategoriaValidacao cat : regras) {
            if (soProblemas && cat.semViolacoes()) continue;
            corpo.getChildren().add(construirCardRegra(cat));
            mostrouRegra = true;
        }
        if (!mostrouRegra) {
            corpo.getChildren().add(notaOk(soProblemas && regrasOk == regras.size()
                    ? "Todas as " + regras.size() + " regras obrigatórias estão cumpridas."
                    : "Sem regras configuradas."));
        }

        // ── Por colaborador ───────────────────────────────────────────────
        if (!analise.colaboradores().isEmpty()) {
            corpo.getChildren().add(kicker("PREFERÊNCIAS E CARGA POR COLABORADOR"));
            boolean mostrouCol = false;
            for (ColaboradorCumprimento c : analise.colaboradores()) {
                if (soProblemas && !c.temProblema()) continue;
                corpo.getChildren().add(construirCardColaborador(c));
                mostrouCol = true;
            }
            if (!mostrouCol) {
                corpo.getChildren().add(notaOk(
                        "Todos os colaboradores têm preferências e carga horária em ordem."));
            }
        }

        // ── Distribuição de FDS (sempre, é informativo) ───────────────────
        VBox fdsCard = construirDistribuicaoFDS(linhas);
        if (fdsCard != null) {
            corpo.getChildren().add(kicker("DISTRIBUIÇÃO DE FINS DE SEMANA"));
            corpo.getChildren().add(fdsCard);
        }

        // ── Recomendações ─────────────────────────────────────────────────
        VBox recom = construirRecomendacoes(resultado, analise);
        if (recom != null) {
            corpo.getChildren().add(kicker("O QUE PODES FAZER"));
            corpo.getChildren().add(recom);
        }
    }

    // ── 1. Veredicto ───────────────────────────────────────────────────────

    private static HBox construirVeredicto(ValidacaoHorarioResultado resultado,
                                           AnaliseCumprimentoHorario.Resultado analise) {
        long totalRegras = resultado.categorias().size();
        long regrasOk = resultado.categorias().stream()
                .filter(ValidacaoHorarioResultado.CategoriaValidacao::semViolacoes).count();
        boolean ausenciasOk = analise.resumo().ausenciasVioladas() == 0;
        boolean valido = regrasOk == totalRegras && ausenciasOk;

        String bg = valido ? "#f0fdf4" : "#fff1f2";
        String border = valido ? "#bbf7d0" : "#fecaca";
        String corTexto = valido ? "#15803d" : "#9a3412";

        HBox banner = new HBox(12);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(14, 16, 14, 16));
        banner.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

        Label icon = new Label(valido ? "✓" : "✗");
        icon.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: "
                + (valido ? VERDE : VERMELHO) + ";");

        VBox msgBox = new VBox(3);
        HBox.setHgrow(msgBox, Priority.ALWAYS);

        String titulo = valido
                ? "Horário válido — todas as regras obrigatórias cumpridas."
                : (totalRegras - regrasOk) + " regra(s) obrigatória(s) por cumprir"
                    + (ausenciasOk ? "." : " · ausências violadas!");
        Label msg = new Label(titulo);
        msg.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + corTexto + ";");
        msg.setWrapText(true);

        AnaliseCumprimentoHorario.Resumo r = analise.resumo();
        String sub = "Preferências honradas: " + r.prefsHonradas() + "/" + r.prefsTotais()
                + "   ·   Carga horária no alvo: " + r.cargaOk() + "/" + r.cargaTotais()
                + "   ·   Com ≥1 fim de semana livre: " + r.fdsComFolga() + "/" + r.fdsAvaliados();
        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (valido ? "#166534" : "#7f1d1d") + ";");
        subLbl.setWrapText(true);

        msgBox.getChildren().addAll(msg, subLbl);
        banner.getChildren().addAll(icon, msgBox);
        return banner;
    }

    // ── 2. Scorecard ─────────────────────────────────────────────────────────

    private static FlowPane construirScorecard(ValidacaoHorarioResultado resultado,
                                               AnaliseCumprimentoHorario.Resultado analise) {
        long totalRegras = resultado.categorias().size();
        long regrasOk = resultado.categorias().stream()
                .filter(ValidacaoHorarioResultado.CategoriaValidacao::semViolacoes).count();
        AnaliseCumprimentoHorario.Resumo r = analise.resumo();

        FlowPane grid = new FlowPane(10, 10);

        grid.getChildren().add(tile("Regras obrigatórias",
                regrasOk + "/" + totalRegras,
                regrasOk == totalRegras ? VERDE : VERMELHO,
                regrasOk == totalRegras ? "Tudo cumprido" : (totalRegras - regrasOk) + " por cumprir"));

        grid.getChildren().add(tile("Preferências",
                r.prefsHonradas() + "/" + r.prefsTotais(),
                corFracao(r.prefsHonradas(), r.prefsTotais()),
                r.prefsTotais() == 0 ? "Nenhuma registada" : "folgas, turnos, colegas"));

        grid.getChildren().add(tile("Carga horária",
                r.cargaOk() + "/" + r.cargaTotais(),
                corFracao(r.cargaOk(), r.cargaTotais()),
                "dentro do previsto"));

        grid.getChildren().add(tile("Fins de semana",
                r.fdsComFolga() + "/" + r.fdsAvaliados(),
                corFracao(r.fdsComFolga(), r.fdsAvaliados()),
                "com ≥1 FDS livre"));

        return grid;
    }

    private static VBox tile(String label, String valor, String cor, String sub) {
        VBox tile = new VBox(2);
        tile.setMinWidth(150);
        tile.setPrefWidth(168);
        HBox.setHgrow(tile, Priority.ALWAYS);
        tile.setPadding(new Insets(10, 12, 10, 12));
        tile.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; "
                + "-fx-border-width: 1px; -fx-border-radius: 10px; -fx-background-radius: 10px;");

        Label lblTopo = new Label(label.toUpperCase(Locale.ROOT));
        lblTopo.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        Label lblValor = new Label(valor);
        lblValor.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + cor + ";");
        Label lblSub = new Label(sub);
        lblSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
        lblSub.setWrapText(true);

        tile.getChildren().addAll(lblTopo, lblValor, lblSub);
        return tile;
    }

    // ── 4. Card de regra obrigatória ─────────────────────────────────────────

    private static VBox construirCardRegra(ValidacaoHorarioResultado.CategoriaValidacao cat) {
        boolean ok = cat.semViolacoes();
        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: " + (ok ? "#f9fafb" : "#fff1f2")
                + "; -fx-border-color: " + (ok ? "#e5e7eb" : "#fecaca")
                + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        HBox titulo = new HBox(8);
        titulo.setAlignment(Pos.CENTER_LEFT);
        titulo.getChildren().add(iconeEstado(ok ? Estado.CUMPRIDO : Estado.NAO_CUMPRIDO));
        Label nome = new Label(cat.nome());
        nome.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: "
                + (ok ? "#166534" : "#7f1d1d") + ";");
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        Label badge = new Label(ok ? "OK" : cat.violacoes().size() + " caso(s)");
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: "
                + (ok ? VERDE : VERMELHO) + ";");
        titulo.getChildren().addAll(nome, esp, badge);
        card.getChildren().add(titulo);

        Label resumo = new Label(cat.resumo());
        resumo.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (ok ? "#166534" : "#7f1d1d") + ";");
        resumo.setWrapText(true);
        card.getChildren().add(resumo);

        if (!ok) {
            List<String> lista = cat.violacoes().size() > 8 ? cat.violacoes().subList(0, 8) : cat.violacoes();
            for (String v : lista) {
                Label lv = new Label("•  " + v);
                lv.setStyle("-fx-font-size: 11px; -fx-text-fill: #374151;");
                lv.setWrapText(true);
                VBox.setMargin(lv, new Insets(0, 0, 0, 8));
                card.getChildren().add(lv);
            }
            if (cat.violacoes().size() > 8) {
                Label mais = new Label("… e mais " + (cat.violacoes().size() - 8) + " situação(ões)");
                mais.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
                VBox.setMargin(mais, new Insets(0, 0, 0, 8));
                card.getChildren().add(mais);
            }
        }
        return card;
    }

    // ── 5. Card por colaborador ──────────────────────────────────────────────

    private static VBox construirCardColaborador(ColaboradorCumprimento c) {
        Estado geral = c.estadoGeral();
        String border = switch (geral) {
            case NAO_CUMPRIDO -> "#fecaca";
            case PARCIAL      -> "#fde68a";
            default           -> "#e2e8f0";
        };
        String bg = switch (geral) {
            case NAO_CUMPRIDO -> "#fff5f5";
            case PARCIAL      -> "#fffbeb";
            default           -> "#f8fafc";
        };

        VBox card = new VBox(5);
        card.setPadding(new Insets(9, 12, 9, 12));
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        // Cabeçalho: nome + cargo + badge
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nomeLbl = new Label(c.nome());
        nomeLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");
        header.getChildren().add(nomeLbl);
        if (c.cargo() != null && !c.cargo().isBlank()) {
            Label cargoLbl = new Label(c.cargo());
            cargoLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            header.getChildren().add(cargoLbl);
        }
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        if (c.totalPreferencias() > 0) {
            Label badge = new Label(c.honradas() + "/" + c.totalPreferencias() + " preferências ✓");
            badge.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 1 8 1 8; "
                    + "-fx-background-radius: 10px; -fx-text-fill: white; -fx-background-color: " + corEstado(geral) + ";");
            header.getChildren().add(badge);
        }
        card.getChildren().addAll(header, separador());

        // ── Carga contratual ──────────────────────────────────────────────
        card.getChildren().add(miniTitulo("Carga contratual"));
        if (c.horasPrevistas() != null && c.horasTrabalhadas() != null) {
            int desvio = c.horasTrabalhadas() - c.horasPrevistas();
            String desvioTxt = desvio == 0 ? "no alvo"
                    : (desvio > 0 ? "+" + desvio + "h acima" : desvio + "h");
            card.getChildren().add(linhaItem("Horas",
                    c.horasTrabalhadas() + "h / " + c.horasPrevistas() + "h previstas (" + desvioTxt + ")",
                    c.estadoCarga()));
        } else {
            card.getChildren().add(linhaInfo("Sem carga contratual registada"));
        }

        // ── Preferências ──────────────────────────────────────────────────
        card.getChildren().add(miniTitulo("Preferências"));
        boolean temPref = false;
        if (c.folga() != null)  { card.getChildren().add(linhaItem("Folga preferida", c.folga().texto(), c.folga().estado())); temPref = true; }
        if (c.turno() != null)  { card.getChildren().add(linhaItem("Turno preferido", c.turno().texto(), c.turno().estado())); temPref = true; }
        if (c.colegas() != null) for (Item i : c.colegas()) { card.getChildren().add(linhaItem("Colega preferido", i.texto(), i.estado())); temPref = true; }
        if (!temPref) card.getChildren().add(linhaInfo("Sem preferências registadas"));

        // ── Fins de semana + ausências ────────────────────────────────────
        if (c.finsDeSemana() != null || c.ausencias() != null) {
            card.getChildren().add(miniTitulo("Fins de semana e ausências"));
            if (c.finsDeSemana() != null) card.getChildren().add(linhaItem("Fins de semana", c.finsDeSemana().texto(), c.finsDeSemana().estado()));
            if (c.ausencias() != null)    card.getChildren().add(linhaItem("Ausências", c.ausencias().texto(), c.ausencias().estado()));
        }

        return card;
    }

    private static Label miniTitulo(String texto) {
        Label lbl = new Label(texto.toUpperCase(Locale.ROOT));
        lbl.setStyle("-fx-font-size: 8.5px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
        VBox.setMargin(lbl, new Insets(4, 0, 0, 0));
        return lbl;
    }

    private static Label linhaInfo(String texto) {
        Label lbl = new Label("•  " + texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
        return lbl;
    }

    /** Linha "ícone  Prefixo: texto" com cor por estado. */
    private static HBox linhaItem(String prefixo, String texto, Estado estado) {
        HBox linha = new HBox(7);
        linha.setAlignment(Pos.TOP_LEFT);
        linha.getChildren().add(iconeEstado(estado));
        Label lbl = new Label(prefixo + ": " + texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + corTextoEstado(estado) + ";"
                + (estado == Estado.NAO_CUMPRIDO ? " -fx-font-weight: 600;" : ""));
        lbl.setWrapText(true);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        linha.getChildren().add(lbl);
        return linha;
    }

    private static Label iconeEstado(Estado estado) {
        String simbolo = switch (estado) {
            case CUMPRIDO     -> "✓";
            case PARCIAL      -> "⚠";
            case NAO_CUMPRIDO -> "✗";
            default           -> "•";
        };
        Label icon = new Label(simbolo);
        icon.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-min-width: 14px; -fx-text-fill: "
                + corEstado(estado) + ";");
        return icon;
    }

    // ── Distribuição de fins de semana (mantida) ─────────────────────────────

    private static VBox construirDistribuicaoFDS(List<HorarioLinha> linhas) {
        if (linhas == null || linhas.isEmpty()) return null;

        Map<String, Set<LocalDate>> fdsPorColaborador = new LinkedHashMap<>();
        for (HorarioLinha l : linhas) {
            if (l.data() == null || l.colaborador() == null) continue;
            if (l.data().getDayOfWeek() != DayOfWeek.SATURDAY
                    && l.data().getDayOfWeek() != DayOfWeek.SUNDAY) continue;
            String cargo = l.cargo() == null ? "" : l.cargo().toLowerCase(Locale.ROOT);
            if (cargo.contains("reforco_parttime") || cargo.contains("reforço_parttime")) continue;
            LocalDate sabado = l.data().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
            fdsPorColaborador.computeIfAbsent(l.colaborador(), k -> new java.util.LinkedHashSet<>()).add(sabado);
        }
        if (fdsPorColaborador.isEmpty()) return null;

        List<Map.Entry<String, Set<LocalDate>>> ordenados = fdsPorColaborador.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Set<LocalDate>>>comparingInt(e -> e.getValue().size()).reversed())
                .toList();
        int maxFds = ordenados.getFirst().getValue().size();
        if (maxFds == 0) return null;

        VBox card = new VBox(4);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; "
                + "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");

        Label nota = new Label("Fins de semana trabalhados por colaborador (exclui reforço parttime):");
        nota.setStyle("-fx-font-size: 10px; -fx-text-fill: #6b7280; -fx-font-style: italic;");
        nota.setWrapText(true);
        card.getChildren().add(nota);

        for (Map.Entry<String, Set<LocalDate>> entry : ordenados) {
            int n = entry.getValue().size();
            String barra = "█".repeat(n) + "░".repeat(Math.max(0, maxFds - n));
            String cor = n <= 1 ? VERDE : (n == 2 ? AMBAR : VERMELHO);
            HBox linha = new HBox(6);
            linha.setAlignment(Pos.CENTER_LEFT);
            Label nomeLbl = new Label(entry.getKey());
            nomeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #374151; -fx-min-width: 120px; -fx-max-width: 120px;");
            Label barraLbl = new Label(barra);
            barraLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + cor + ";");
            Label cntLbl = new Label(n + " FDS");
            cntLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + cor + "; -fx-font-weight: 700;");
            linha.getChildren().addAll(nomeLbl, barraLbl, cntLbl);
            card.getChildren().add(linha);
        }
        return card;
    }

    // ── Recomendações ────────────────────────────────────────────────────────

    private static VBox construirRecomendacoes(ValidacaoHorarioResultado resultado,
                                               AnaliseCumprimentoHorario.Resultado analise) {
        List<String> recs = new ArrayList<>();
        for (ValidacaoHorarioResultado.CategoriaValidacao cat : resultado.categorias()) {
            if (cat.semViolacoes()) continue;
            String nome = cat.nome().toLowerCase(Locale.ROOT);
            if (nome.contains("rotação") || nome.contains("fim de semana") || nome.contains("fins de semana")) {
                recs.add("Fins de semana: há colaboradores sem nenhum fim de semana livre no mês. Se a equipa é pequena, considera adicionar um reforço parttime (que cobre os FDS) ou alternar manualmente quem trabalha cada fim de semana.");
            } else if (nome.contains("descanso entre")) {
                recs.add("Descanso entre turnos: verifica as transições noite → manhã nos dias assinalados — considera intercalar uma folga.");
            } else if (nome.contains("consecutivos")) {
                recs.add("Dias consecutivos: redistribui os turnos nos picos assinalados para intercalar pelo menos uma folga.");
            } else if (nome.contains("folgas semanais")) {
                recs.add("Folgas semanais: nas semanas assinaladas algum colaborador ficou sem folga — edita manualmente um turno para folga.");
            } else if (nome.contains("chefia")) {
                recs.add("Chefia ao sábado: nos sábados sem cobertura, considera uma permuta ou edição direta do turno de gerente/subgerente.");
            }
        }
        if (analise.resumo().ausenciasVioladas() > 0) {
            recs.add("Ausências violadas: há colaboradores escalados em dias de ausência aprovada — corrige esses turnos antes de enviar.");
        }
        if (analise.resumo().prefsTotais() > 0
                && analise.resumo().prefsHonradas() < analise.resumo().prefsTotais()) {
            recs.add("Preferências não honradas costumam ser inevitáveis quando a cobertura é apertada; se forem muitas, considera gerar uma alternativa ou rever os mínimos por turno.");
        }
        if (recs.isEmpty()) return null;

        VBox card = new VBox(5);
        card.setPadding(new Insets(10, 12, 10, 12));
        card.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #bfdbfe; "
                + "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px;");
        for (String rec : recs) {
            Label lbl = new Label("•  " + rec);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #1e3a8a;");
            lbl.setWrapText(true);
            card.getChildren().add(lbl);
        }
        return card;
    }

    // ── Helpers de estilo ────────────────────────────────────────────────────

    private static String corEstado(Estado e) {
        return switch (e) {
            case CUMPRIDO     -> VERDE;
            case PARCIAL      -> AMBAR;
            case NAO_CUMPRIDO -> VERMELHO;
            default           -> CINZA;
        };
    }

    private static String corTextoEstado(Estado e) {
        return switch (e) {
            case CUMPRIDO     -> "#166534";
            case PARCIAL      -> "#92400e";
            case NAO_CUMPRIDO -> "#b91c1c";
            default           -> "#475569";
        };
    }

    private static String corFracao(int parte, int total) {
        if (total == 0) return CINZA;
        if (parte >= total) return VERDE;
        if (parte >= total * 0.6) return AMBAR;
        return VERMELHO;
    }

    private static Region separador() {
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setStyle("-fx-background-color: rgba(15,23,42,0.06);");
        VBox.setMargin(sep, new Insets(1, 0, 2, 0));
        return sep;
    }

    private static Label notaOk(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #16a34a; -fx-padding: 4 0 4 2;");
        lbl.setWrapText(true);
        return lbl;
    }

    private static Label kicker(String texto) {
        Label lbl = new Label(texto);
        lbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 700; -fx-text-fill: #9ca3af;");
        VBox.setMargin(lbl, new Insets(6, 0, 0, 0));
        return lbl;
    }
}
