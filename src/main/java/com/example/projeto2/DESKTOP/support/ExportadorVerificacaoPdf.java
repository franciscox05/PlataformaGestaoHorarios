package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Services.geracao.dto.CriteriosGeracao;
import com.example.projeto2.API.Services.geracao.dto.HorarioLinha;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.ColaboradorCumprimento;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.Estado;
import com.example.projeto2.DESKTOP.support.AnaliseCumprimentoHorario.Item;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Gera o <b>Relatório de Verificação do Horário</b> em PDF — o documento formal que o
 * gestor pode arquivar/partilhar com o supervisor. Estrutura:
 *
 * <ol>
 *   <li><b>Página 1 (paisagem)</b> — grelha do mês: colaboradores × dias, com a inicial
 *       do turno em cada célula e o total de horas à direita.</li>
 *   <li><b>Páginas seguintes (retrato)</b> — veredicto + indicadores, regras obrigatórias
 *       (cumpridas/violadas) e, por colaborador, o que foi e o que não foi cumprido
 *       (carga horária, folgas preferidas, turnos, colegas, ausências).</li>
 * </ol>
 *
 * <p>Usa Apache PDFBox diretamente. As fontes Standard-14 (Helvetica) só suportam
 * Latin-1, por isso usam-se etiquetas de texto ("OK"/"PARCIAL"/"FALHA") com cor — nunca
 * símbolos como ✓/✗ — para evitar erros de glifo.
 */
public final class ExportadorVerificacaoPdf {

    private static final PDType1Font F_NORM = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font F_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    // Paleta (RGB 0..1)
    private static final float[] VINHO    = {0.79f, 0.008f, 0.12f};
    private static final float[] BRANCO   = {1f, 1f, 1f};
    private static final float[] PRETO    = {0.13f, 0.13f, 0.13f};
    private static final float[] CINZA    = {0.45f, 0.45f, 0.45f};
    private static final float[] CINZA_CL = {0.93f, 0.94f, 0.96f};
    private static final float[] VERDE    = {0.086f, 0.64f, 0.29f};
    private static final float[] AMBAR    = {0.85f, 0.47f, 0.02f};
    private static final float[] VERMELHO = {0.86f, 0.15f, 0.15f};

    private static final String[] MESES_PT = {
            "janeiro", "fevereiro", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
    };

    private ExportadorVerificacaoPdf() {}

    public static void exportar(List<HorarioLinha> linhas,
                                CriteriosGeracao criterios,
                                int mes, int ano,
                                String nomeLoja,
                                String periodoLabel,
                                Window janela,
                                Consumer<String> onSucesso,
                                Consumer<String> onErro) {
        if (linhas == null || linhas.isEmpty()) {
            onErro.accept("Não há horário para exportar.");
            return;
        }
        String nomeMes = MESES_PT[Math.max(0, Math.min(11, mes - 1))];

        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar relatório de verificação para PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("verificacao-horario-" + ano + "-" + nomeMes + ".pdf");
        File ficheiro = fc.showSaveDialog(janela);
        if (ficheiro == null) return;

        ValidacaoHorarioResultado validacao = ValidadorHorarioProposta.validar(linhas, criterios);
        AnaliseCumprimentoHorario.Resultado analise = AnaliseCumprimentoHorario.analisar(linhas, criterios, ano);

        try (FileOutputStream fos = new FileOutputStream(ficheiro);
             PDDocument doc = new PDDocument()) {
            desenharGrelha(doc, linhas, analise, validacao, nvl(nomeLoja), periodoLabel, ano, mes);
            desenharRelatorio(doc, analise, validacao, nvl(nomeLoja), periodoLabel);
            doc.save(fos);
            onSucesso.accept("Relatório de verificação exportado com sucesso.");
        } catch (IOException e) {
            onErro.accept("Não foi possível exportar o relatório PDF.");
        }
    }

    // ════════════════════════════ PÁGINA 1 — GRELHA ════════════════════════════

    private static void desenharGrelha(PDDocument doc, List<HorarioLinha> linhas,
                                       AnaliseCumprimentoHorario.Resultado analise,
                                       ValidacaoHorarioResultado validacao,
                                       String loja, String periodo, int ano, int mes) throws IOException {
        final float PG_W = PDRectangle.A4.getHeight();   // paisagem
        final float PG_H = PDRectangle.A4.getWidth();
        final float MARG = 28f, HEADER = 46f;
        final float util = PG_W - 2 * MARG;

        YearMonth ym = YearMonth.of(ano, mes);
        int numDias = ym.lengthOfMonth();

        // Por colaborador/dia: [0]=sigla do turno, [1]=horas compactas (ex.: "8-16")
        Map<String, Map<Integer, String[]>> grelha = new LinkedHashMap<>();
        for (HorarioLinha l : linhas) {
            if (l.data() == null || l.colaborador() == null) continue;
            grelha.computeIfAbsent(l.colaborador().trim(), k -> new LinkedHashMap<>())
                    .put(l.data().getDayOfMonth(),
                            new String[]{letra(l.turno()), horasCompactas(l.periodo())});
        }
        Map<String, Integer> horasPorNome = new LinkedHashMap<>();
        for (ColaboradorCumprimento c : analise.colaboradores()) {
            if (c.horasTrabalhadas() != null) horasPorNome.put(c.nome(), c.horasTrabalhadas());
        }
        List<String> nomes = grelha.keySet().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();

        float nomeW = 92f, horasW = 34f;
        float diasArea = util - nomeW - horasW;
        float diaW = diasArea / numDias;
        float colHdrH = 16f, rowH = 20f;   // mais alta: sigla + horas por baixo

        PDPage page = new PDPage(new PDRectangle(PG_W, PG_H));
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            // Cabeçalho
            cor(cs, VINHO); cs.addRect(0, PG_H - HEADER, PG_W, HEADER); cs.fill();
            cor(cs, BRANCO);
            texto(cs, F_BOLD, 13f, MARG, PG_H - 20f, "Relatório de Verificação — " + loja);
            texto(cs, F_NORM, 8.5f, MARG, PG_H - 34f, "Horário do mês · " + nvl(periodo));

            // Linha-resumo
            AnaliseCumprimentoHorario.Resumo r = analise.resumo();
            long regrasOk = validacao.categorias().stream()
                    .filter(ValidacaoHorarioResultado.CategoriaValidacao::semViolacoes).count();
            cor(cs, PRETO);
            texto(cs, F_NORM, 8f, MARG, PG_H - HEADER - 12f,
                    "Regras obrigatórias: " + regrasOk + "/" + validacao.categorias().size()
                    + "      Preferências honradas: " + r.prefsHonradas() + "/" + r.prefsTotais()
                    + "      Carga no alvo: " + r.cargaOk() + "/" + r.cargaTotais()
                    + "      Com >=1 fim de semana livre: " + r.fdsComFolga() + "/" + r.fdsAvaliados());

            float yTop = PG_H - HEADER - 24f;
            float xDias = MARG + nomeW;

            // Sombreado de colunas de fim de semana (corpo todo)
            float alturaCorpo = colHdrH + nomes.size() * rowH;
            for (int d = 1; d <= numDias; d++) {
                DayOfWeek dow = ym.atDay(d).getDayOfWeek();
                if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    cor(cs, CINZA_CL);
                    cs.addRect(xDias + (d - 1) * diaW, yTop - alturaCorpo, diaW, alturaCorpo);
                    cs.fill();
                }
            }

            // Cabeçalho da tabela (dias)
            cor(cs, PRETO);
            texto(cs, F_BOLD, 7.5f, MARG, yTop - colHdrH + 5f, "Colaborador");
            for (int d = 1; d <= numDias; d++) {
                centrar(cs, F_BOLD, 6.5f, xDias + (d - 1) * diaW, diaW, yTop - colHdrH + 5f,
                        String.valueOf(d));
            }
            centrar(cs, F_BOLD, 7f, MARG + nomeW + diasArea, horasW, yTop - colHdrH + 5f, "Horas");
            cs.setStrokingColor(0.7f, 0.7f, 0.7f);
            cs.moveTo(MARG, yTop - colHdrH); cs.lineTo(MARG + util, yTop - colHdrH); cs.stroke();

            // Linhas
            float y = yTop - colHdrH;
            for (String nome : nomes) {
                Map<Integer, String[]> dias = grelha.get(nome);
                cor(cs, PRETO);
                texto(cs, F_NORM, 7f, MARG, y - rowH + 8f, truncar(nome, 18));
                for (int d = 1; d <= numDias; d++) {
                    String[] cel = dias.get(d);
                    if (cel != null && !cel[0].isEmpty()) {
                        float xc = xDias + (d - 1) * diaW;
                        cor(cs, corLetra(cel[0]));
                        centrar(cs, F_BOLD, 7.5f, xc, diaW, y - rowH + 11f, cel[0]);   // sigla
                        cor(cs, CINZA);
                        centrar(cs, F_NORM, 4.6f, xc, diaW, y - rowH + 3.5f, cel[1]);   // horas por baixo
                    }
                }
                Integer h = horasPorNome.get(nome);
                cor(cs, PRETO);
                centrar(cs, F_BOLD, 7f, MARG + nomeW + diasArea, horasW, y - rowH + 8f,
                        h != null ? h + "h" : "-");
                cs.setStrokingColor(0.9f, 0.9f, 0.9f);
                cs.moveTo(MARG, y - rowH); cs.lineTo(MARG + util, y - rowH); cs.stroke();
                y -= rowH;
            }

            // Legenda + rodapé
            cor(cs, CINZA);
            texto(cs, F_NORM, 7f, MARG, y - 14f,
                    "Legenda:  M = Manhã    T = Tarde    N = Noite    I = Intermédio    (vazio) = Folga"
                    + "      (horas do turno por baixo da sigla)");
            texto(cs, F_NORM, 6.5f, MARG, MARG - 12f,
                    "Portal de Gestão Levi's Staff — relatório gerado automaticamente");
        }
    }

    // ════════════════════════ PÁGINAS 2+ — RELATÓRIO ═══════════════════════════

    private static void desenharRelatorio(PDDocument doc,
                                          AnaliseCumprimentoHorario.Resultado analise,
                                          ValidacaoHorarioResultado validacao,
                                          String loja, String periodo) throws IOException {
        Ctx c = new Ctx(doc, loja, periodo);
        c.novaPagina();

        // Veredicto
        long regrasOk = validacao.categorias().stream()
                .filter(ValidacaoHorarioResultado.CategoriaValidacao::semViolacoes).count();
        boolean valido = regrasOk == validacao.categorias().size()
                && analise.resumo().ausenciasVioladas() == 0;
        c.garantir(28f);
        cor(c.cs, valido ? VERDE : VERMELHO);
        texto(c.cs, F_BOLD, 12f, Ctx.MARG, c.y,
                valido ? "Horário válido — todas as regras obrigatórias cumpridas."
                       : (validacao.categorias().size() - regrasOk) + " regra(s) obrigatória(s) por cumprir.");
        c.y -= 16f;
        AnaliseCumprimentoHorario.Resumo r = analise.resumo();
        cor(c.cs, PRETO);
        texto(c.cs, F_NORM, 9f, Ctx.MARG, c.y,
                "Preferências honradas: " + r.prefsHonradas() + "/" + r.prefsTotais()
                + "      Carga no alvo: " + r.cargaOk() + "/" + r.cargaTotais()
                + "      Com >=1 fim de semana livre: " + r.fdsComFolga() + "/" + r.fdsAvaliados());
        c.y -= 22f;

        // Regras obrigatórias
        seccao(c, "REGRAS OBRIGATÓRIAS");
        for (ValidacaoHorarioResultado.CategoriaValidacao cat : validacao.categorias()) {
            boolean ok = cat.semViolacoes();
            c.garantir(16f);
            etiqueta(c.cs, Ctx.MARG, c.y, ok ? "OK" : "FALHA", ok ? VERDE : VERMELHO);
            cor(c.cs, PRETO);
            texto(c.cs, F_BOLD, 9f, Ctx.MARG + 42f, c.y, cat.nome());
            c.y -= 12f;
            c.garantir(12f);
            cor(c.cs, CINZA);
            texto(c.cs, F_NORM, 8f, Ctx.MARG + 42f, c.y, truncar(cat.resumo(), 110));
            c.y -= 12f;
            if (!ok) {
                int max = Math.min(cat.violacoes().size(), 6);
                for (int i = 0; i < max; i++) {
                    c.garantir(11f);
                    cor(c.cs, PRETO);
                    texto(c.cs, F_NORM, 8f, Ctx.MARG + 50f, c.y, "- " + truncar(cat.violacoes().get(i), 105));
                    c.y -= 11f;
                }
                if (cat.violacoes().size() > max) {
                    c.garantir(11f);
                    cor(c.cs, CINZA);
                    texto(c.cs, F_NORM, 7.5f, Ctx.MARG + 50f, c.y,
                            "... e mais " + (cat.violacoes().size() - max) + " situação(ões)");
                    c.y -= 11f;
                }
            }
            c.y -= 4f;
        }
        c.y -= 8f;

        // Por colaborador
        seccao(c, "CUMPRIMENTO POR COLABORADOR");
        for (ColaboradorCumprimento col : analise.colaboradores()) {
            // Cabeçalho do bloco (mantém junto pelo menos o nome + 1 linha)
            c.garantir(26f);
            cor(c.cs, PRETO);
            texto(c.cs, F_BOLD, 9.5f, Ctx.MARG, c.y, col.nome()
                    + (col.cargo() != null && !col.cargo().isBlank() ? "   (" + col.cargo() + ")" : ""));
            if (col.totalPreferencias() > 0) {
                String badge = col.honradas() + "/" + col.totalPreferencias() + " preferências honradas";
                cor(c.cs, corEstado(col.estadoGeral()));
                float w = larguraTexto(F_BOLD, 8f, badge);
                texto(c.cs, F_BOLD, 8f, Ctx.MARG + Ctx.UTIL - w, c.y, badge);
            }
            c.y -= 13f;

            // Carga contratual
            subTitulo(c, "Carga contratual");
            if (col.horasPrevistas() != null && col.horasTrabalhadas() != null) {
                int desvio = col.horasTrabalhadas() - col.horasPrevistas();
                String d = desvio == 0 ? "no alvo" : (desvio > 0 ? "+" + desvio + "h acima do previsto" : desvio + "h");
                linhaItem(c, col.estadoCarga(), "Horas: "
                        + col.horasTrabalhadas() + "h / " + col.horasPrevistas() + "h previstas (" + d + ")");
            } else {
                linhaInfo(c, "Sem carga contratual registada");
            }

            // Preferências
            subTitulo(c, "Preferências");
            boolean temPref = false;
            if (col.folga() != null)  { linhaItem(c, col.folga().estado(), "Folga preferida: " + col.folga().texto()); temPref = true; }
            if (col.turno() != null)  { linhaItem(c, col.turno().estado(), "Turno preferido: " + col.turno().texto()); temPref = true; }
            if (col.colegas() != null) for (Item i : col.colegas()) { linhaItem(c, i.estado(), "Colega preferido: " + i.texto()); temPref = true; }
            if (!temPref) linhaInfo(c, "Sem preferências registadas");

            // Fins de semana e ausências
            if (col.finsDeSemana() != null || col.ausencias() != null) {
                subTitulo(c, "Fins de semana e ausências");
                if (col.finsDeSemana() != null) linhaItem(c, col.finsDeSemana().estado(), "Fins de semana: " + col.finsDeSemana().texto());
                if (col.ausencias() != null)    linhaItem(c, col.ausencias().estado(), "Ausências: " + col.ausencias().texto());
            }
            c.y -= 8f;
        }

        c.fechar();
    }

    private static void linhaItem(Ctx c, Estado estado, String texto) throws IOException {
        c.garantir(12f);
        etiqueta(c.cs, Ctx.MARG + 16f, c.y, tag(estado), corEstado(estado));
        cor(c.cs, PRETO);
        texto(c.cs, F_NORM, 8.5f, Ctx.MARG + 16f + 48f, c.y, truncar(texto, 95));
        c.y -= 12f;
    }

    private static void linhaInfo(Ctx c, String texto) throws IOException {
        c.garantir(12f);
        cor(c.cs, CINZA);
        texto(c.cs, F_NORM, 8.5f, Ctx.MARG + 16f, c.y, "- " + truncar(texto, 100));
        c.y -= 12f;
    }

    private static void subTitulo(Ctx c, String t) throws IOException {
        c.garantir(13f);
        cor(c.cs, CINZA);
        texto(c.cs, F_BOLD, 7.5f, Ctx.MARG + 8f, c.y, t.toUpperCase(java.util.Locale.ROOT));
        c.y -= 11f;
    }

    private static String horasCompactas(String periodo) {
        if (periodo == null) return "";
        String[] p = periodo.trim().split(" - | – ", 2);
        if (p.length < 2) return "";
        return curtaHora(p[0]) + "-" + curtaHora(p[1]);
    }

    private static String curtaHora(String hhmm) {
        String s = hhmm.trim();
        int i = s.indexOf(':');
        if (i < 0) return s;
        String hh = s.substring(0, i);
        String mm = s.substring(i + 1);
        try { hh = String.valueOf(Integer.parseInt(hh)); } catch (NumberFormatException ignored) {}
        return "00".equals(mm) ? hh : hh + ":" + mm;
    }

    private static void seccao(Ctx c, String titulo) throws IOException {
        c.garantir(18f);
        cor(c.cs, CINZA);
        texto(c.cs, F_BOLD, 8.5f, Ctx.MARG, c.y, titulo);
        c.cs.setStrokingColor(0.8f, 0.8f, 0.8f);
        c.cs.moveTo(Ctx.MARG, c.y - 4f); c.cs.lineTo(Ctx.MARG + Ctx.UTIL, c.y - 4f); c.cs.stroke();
        c.y -= 16f;
    }

    /** Etiqueta colorida tipo "OK"/"PARCIAL"/"FALHA". */
    private static void etiqueta(PDPageContentStream cs, float x, float y, String t, float[] cor) throws IOException {
        cor(cs, cor);
        texto(cs, F_BOLD, 8f, x, y, "[" + t + "]");
    }

    // ── Contexto de paginação (retrato) ────────────────────────────────────────

    private static final class Ctx {
        static final float PG_W = PDRectangle.A4.getWidth();
        static final float PG_H = PDRectangle.A4.getHeight();
        static final float MARG = 40f, HEADER = 48f;
        static final float UTIL = PG_W - 2 * MARG;
        static final float BOTTOM = MARG + 16f;

        final PDDocument doc;
        final String loja, periodo;
        PDPageContentStream cs;
        float y;

        Ctx(PDDocument doc, String loja, String periodo) {
            this.doc = doc; this.loja = loja; this.periodo = periodo;
        }

        void novaPagina() throws IOException {
            if (cs != null) cs.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            cor(cs, VINHO); cs.addRect(0, PG_H - HEADER, PG_W, HEADER); cs.fill();
            cor(cs, BRANCO);
            texto(cs, F_BOLD, 13f, MARG, PG_H - 20f, "Relatório de Verificação — " + loja);
            texto(cs, F_NORM, 8.5f, MARG, PG_H - 34f, nvl(periodo));
            cor(cs, CINZA);
            texto(cs, F_NORM, 6.5f, MARG, MARG - 12f,
                    "Portal de Gestão Levi's Staff — relatório gerado automaticamente");
            y = PG_H - HEADER - 18f;
        }

        void garantir(float h) throws IOException {
            if (y - h < BOTTOM) novaPagina();
        }

        void fechar() throws IOException {
            if (cs != null) { cs.close(); cs = null; }
        }
    }

    // ── Auxiliares de desenho ──────────────────────────────────────────────────

    private static void texto(PDPageContentStream cs, PDType1Font f, float t, float x, float y, String s)
            throws IOException {
        cs.beginText();
        cs.setFont(f, t);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizar(s));
        cs.endText();
    }

    private static void centrar(PDPageContentStream cs, PDType1Font f, float t,
                                float x, float largura, float y, String s) throws IOException {
        float w = larguraTexto(f, t, s);
        texto(cs, f, t, x + (largura - w) / 2f, y, s);
    }

    private static float larguraTexto(PDType1Font f, float t, String s) {
        try {
            return f.getStringWidth(sanitizar(s)) / 1000f * t;
        } catch (IOException e) {
            return s.length() * t * 0.5f;
        }
    }

    private static void cor(PDPageContentStream cs, float[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private static String letra(String turno) {
        return switch (GrelhaHorarioRenderer.turnoChave(turno)) {
            case "manha"      -> "M";
            case "tarde"      -> "T";
            case "noite"      -> "N";
            case "intermedio" -> "I";
            default           -> "";
        };
    }

    private static float[] corLetra(String letra) {
        return switch (letra) {
            case "M" -> new float[]{0.15f, 0.39f, 0.92f}; // azul
            case "T" -> new float[]{0.55f, 0.36f, 0.96f}; // roxo claro
            case "N" -> new float[]{0.42f, 0.28f, 0.80f}; // roxo
            case "I" -> AMBAR;
            default  -> PRETO;
        };
    }

    private static String tag(Estado e) {
        return switch (e) {
            case CUMPRIDO     -> "OK";
            case PARCIAL      -> "PARCIAL";
            case NAO_CUMPRIDO -> "FALHA";
            default           -> "INFO";
        };
    }

    private static float[] corEstado(Estado e) {
        return switch (e) {
            case CUMPRIDO     -> VERDE;
            case PARCIAL      -> AMBAR;
            case NAO_CUMPRIDO -> VERMELHO;
            default           -> CINZA;
        };
    }

    private static String nvl(String s) { return s != null && !s.isBlank() ? s : "-"; }

    private static String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, Math.max(0, max - 1)) + "…" : s;
    }

    /** Remove caracteres fora de WinAnsi que partiriam o PDFBox (ex.: símbolos exóticos). */
    private static String sanitizar(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch == '…') { sb.append("..."); continue; }   // …
            if (ch == '–' || ch == '—') { sb.append('-'); continue; } // – —
            if (ch == '‘' || ch == '’') { sb.append('\''); continue; }
            if (ch == '“' || ch == '”') { sb.append('"'); continue; }
            if (ch >= 0x20 && ch <= 0xFF) sb.append(ch);
            else sb.append('?');
        }
        return sb.toString();
    }
}
