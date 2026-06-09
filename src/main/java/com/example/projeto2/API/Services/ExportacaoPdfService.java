package com.example.projeto2.API.Services;

import com.example.projeto2.API.Services.GeracaoHorariosService.HorarioLinha;
import com.example.projeto2.API.Services.RelatorioHorasService.RelatorioLinha;
import com.example.projeto2.API.Services.RelatorioHorasService.RelatorioResumo;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ExportacaoPdfService {

    private static final float MARGEM       = 36f;
    private static final float PG_W         = PDRectangle.A4.getWidth();
    private static final float PG_H         = PDRectangle.A4.getHeight();
    private static final float UTIL_W       = PG_W - 2 * MARGEM;

    private static final float HEADER_H     = 52f;
    private static final float COL_HDR_H    = 18f;
    private static final float ROW_H        = 15f;
    private static final float Y_INICIO     = PG_H - MARGEM - HEADER_H - COL_HDR_H;

    private static final float[] COL_W = {0.27f, 0.12f, 0.13f, 0.22f, 0.14f, 0.12f};
    private static final String[] COL_N = {"Colaborador", "Data", "Dia", "Período", "Turno", "Estado"};

    private static final PDType1Font F_NORM = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font F_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public void exportarHorarioPdf(OutputStream destino,
                                   String nomeLoja,
                                   String nomeMes,
                                   int ano,
                                   String estadoProposta,
                                   String geradoPor,
                                   List<HorarioLinha> linhas) throws IOException {

        List<HorarioLinha> ordenadas = linhas.stream()
                .sorted(Comparator.comparing(HorarioLinha::data,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(HorarioLinha::colaborador,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        // Dividir as linhas em páginas
        int linhasPorPagina = (int) ((PG_H - MARGEM - HEADER_H - COL_HDR_H - MARGEM - 20f) / ROW_H);
        List<List<HorarioLinha>> paginas = new ArrayList<>();
        for (int i = 0; i < ordenadas.size(); i += linhasPorPagina) {
            paginas.add(ordenadas.subList(i, Math.min(i + linhasPorPagina, ordenadas.size())));
        }
        if (paginas.isEmpty()) paginas.add(List.of());

        try (PDDocument doc = new PDDocument()) {
            int numPagina = 1;
            for (List<HorarioLinha> bloco : paginas) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                doc.addPage(pagina);

                try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                    desenharPagina(cs, nomeLoja, nomeMes, ano, estadoProposta, geradoPor,
                            bloco, numPagina, paginas.size(), ordenadas.size());
                }
                numPagina++;
            }
            doc.save(destino);
        }
    }

    private void desenharPagina(PDPageContentStream cs,
                                String nomeLoja, String nomeMes, int ano,
                                String estado, String geradoPor,
                                List<HorarioLinha> linhas,
                                int numPagina, int totalPaginas,
                                int totalLinhas) throws IOException {
        // ── Cabeçalho de topo ────────────────────────────────────────────
        cs.setNonStrokingColor(0.79f, 0.008f, 0.12f);
        cs.addRect(0, PG_H - HEADER_H, PG_W, HEADER_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        texto(cs, F_BOLD, 13f, MARGEM, PG_H - MARGEM - 14f,
                "Horário Mensal — " + nvl(nomeLoja));
        texto(cs, F_NORM, 8.5f, MARGEM, PG_H - MARGEM - 28f,
                nvl(nomeMes) + " " + ano + "   |   Estado: " + nvl(estado)
                        + "   |   Gerado por: " + nvl(geradoPor));
        texto(cs, F_NORM, 7.5f, PG_W - MARGEM - 70f, PG_H - MARGEM - 14f,
                "Pág. " + numPagina + " / " + totalPaginas);

        // ── Cabeçalho da tabela ──────────────────────────────────────────
        float yTabHeader = PG_H - MARGEM - HEADER_H;
        cs.setNonStrokingColor(0.20f, 0.20f, 0.20f);
        cs.addRect(MARGEM, yTabHeader - COL_HDR_H, UTIL_W, COL_HDR_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        float xCursor = MARGEM + 3f;
        for (int i = 0; i < COL_N.length; i++) {
            texto(cs, F_BOLD, 7.5f, xCursor, yTabHeader - COL_HDR_H + 6f, COL_N[i]);
            xCursor += COL_W[i] * UTIL_W;
        }

        // ── Linhas de dados ──────────────────────────────────────────────
        float y = yTabHeader - COL_HDR_H;
        boolean fundo = false;
        for (HorarioLinha linha : linhas) {
            if (fundo) {
                cs.setNonStrokingColor(0.96f, 0.96f, 0.96f);
                cs.addRect(MARGEM, y - ROW_H, UTIL_W, ROW_H);
                cs.fill();
            }
            // Linha separadora
            cs.setStrokingColor(0.88f, 0.88f, 0.88f);
            cs.moveTo(MARGEM, y - ROW_H);
            cs.lineTo(MARGEM + UTIL_W, y - ROW_H);
            cs.stroke();

            String[] celulas = {
                    nvl(linha.colaborador()),
                    linha.data() != null ? linha.data().toString() : "-",
                    nvl(linha.diaSemana()),
                    nvl(linha.periodo()),
                    nvl(linha.turno()),
                    nvl(linha.estado())
            };
            xCursor = MARGEM + 3f;
            cs.setNonStrokingColor(0.15f, 0.15f, 0.15f);
            for (int i = 0; i < celulas.length; i++) {
                int maxChars = (int) (COL_W[i] * UTIL_W / 5.2f);
                texto(cs, F_NORM, 7f, xCursor, y - ROW_H + 5f, truncar(celulas[i], maxChars));
                xCursor += COL_W[i] * UTIL_W;
            }
            y -= ROW_H;
            fundo = !fundo;
        }

        // ── Rodapé ───────────────────────────────────────────────────────
        cs.setNonStrokingColor(0.55f, 0.55f, 0.55f);
        texto(cs, F_NORM, 6.5f, MARGEM, MARGEM - 12f,
                "Total de registos: " + totalLinhas
                        + "   |   Portal de Gestão Levi's Staff — gerado automaticamente");
    }

    private void texto(PDPageContentStream cs, PDType1Font fonte, float tamanho,
                       float x, float y, String texto) throws IOException {
        cs.beginText();
        cs.setFont(fonte, tamanho);
        cs.newLineAtOffset(x, y);
        cs.showText(texto != null ? texto : "");
        cs.endText();
    }

    // =========================================================================
    // Exportação do Relatório de Horas
    // =========================================================================

    public void exportarRelatorioPdf(OutputStream destino,
                                     String nomeLoja,
                                     String nomeMes,
                                     int ano,
                                     List<RelatorioLinha> linhas,
                                     RelatorioResumo resumo) throws IOException {

        float[] colW = {0.35f, 0.25f, 0.13f, 0.12f, 0.15f};
        String[] colN = {"Colaborador", "Cargo", "Turnos", "Folgas Aprov.", "Horas"};

        int linhasPorPagina = (int) ((PG_H - MARGEM - HEADER_H - COL_HDR_H - MARGEM - 20f) / ROW_H);
        List<List<RelatorioLinha>> paginas = new ArrayList<>();
        for (int i = 0; i < linhas.size(); i += linhasPorPagina) {
            paginas.add(linhas.subList(i, Math.min(i + linhasPorPagina, linhas.size())));
        }
        if (paginas.isEmpty()) paginas.add(List.of());

        try (PDDocument doc = new PDDocument()) {
            int numPagina = 1;
            for (List<RelatorioLinha> bloco : paginas) {
                PDPage pagina = new PDPage(PDRectangle.A4);
                doc.addPage(pagina);
                try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                    desenharPaginaRelatorio(cs, nomeLoja, nomeMes, ano, bloco, resumo,
                            colW, colN, numPagina, paginas.size());
                }
                numPagina++;
            }
            doc.save(destino);
        }
    }

    private void desenharPaginaRelatorio(PDPageContentStream cs,
                                         String nomeLoja, String nomeMes, int ano,
                                         List<RelatorioLinha> linhas,
                                         RelatorioResumo resumo,
                                         float[] colW, String[] colN,
                                         int numPagina, int totalPaginas) throws IOException {
        // Cabeçalho de topo
        cs.setNonStrokingColor(0.79f, 0.008f, 0.12f);
        cs.addRect(0, PG_H - HEADER_H, PG_W, HEADER_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        texto(cs, F_BOLD, 13f, MARGEM, PG_H - MARGEM - 14f,
                "Relatório de Horas — " + nvl(nomeLoja));
        texto(cs, F_NORM, 8.5f, MARGEM, PG_H - MARGEM - 28f,
                nvl(nomeMes) + " " + ano);
        texto(cs, F_NORM, 7.5f, PG_W - MARGEM - 70f, PG_H - MARGEM - 14f,
                "Pág. " + numPagina + " / " + totalPaginas);

        // Cabeçalho da tabela
        float yTabHeader = PG_H - MARGEM - HEADER_H;
        cs.setNonStrokingColor(0.20f, 0.20f, 0.20f);
        cs.addRect(MARGEM, yTabHeader - COL_HDR_H, UTIL_W, COL_HDR_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        float xCursor = MARGEM + 3f;
        for (int i = 0; i < colN.length; i++) {
            texto(cs, F_BOLD, 7.5f, xCursor, yTabHeader - COL_HDR_H + 6f, colN[i]);
            xCursor += colW[i] * UTIL_W;
        }

        // Linhas
        float y = yTabHeader - COL_HDR_H;
        boolean fundo = false;
        for (RelatorioLinha linha : linhas) {
            if (fundo) {
                cs.setNonStrokingColor(0.96f, 0.96f, 0.96f);
                cs.addRect(MARGEM, y - ROW_H, UTIL_W, ROW_H);
                cs.fill();
            }
            cs.setStrokingColor(0.88f, 0.88f, 0.88f);
            cs.moveTo(MARGEM, y - ROW_H);
            cs.lineTo(MARGEM + UTIL_W, y - ROW_H);
            cs.stroke();

            String[] celulas = {
                    nvl(linha.nomeColaborador()),
                    nvl(linha.cargo()),
                    String.valueOf(linha.turnos()),
                    String.valueOf(linha.folgasAprovadas()),
                    nvl(linha.horasFormatadas())
            };
            xCursor = MARGEM + 3f;
            cs.setNonStrokingColor(0.15f, 0.15f, 0.15f);
            for (int i = 0; i < celulas.length; i++) {
                int maxC = (int) (colW[i] * UTIL_W / 5.2f);
                texto(cs, F_NORM, 7f, xCursor, y - ROW_H + 5f, truncar(celulas[i], maxC));
                xCursor += colW[i] * UTIL_W;
            }
            y -= ROW_H;
            fundo = !fundo;
        }

        // Linha de totais (última página)
        if (numPagina == totalPaginas && resumo != null) {
            y -= 4f;
            cs.setNonStrokingColor(0.15f, 0.40f, 0.70f);
            cs.addRect(MARGEM, y - ROW_H, UTIL_W, ROW_H);
            cs.fill();
            cs.setNonStrokingColor(1f, 1f, 1f);
            String[] totais = {
                    "TOTAL (" + resumo.colaboradores() + " colaboradores)",
                    "",
                    String.valueOf(resumo.turnos()),
                    String.valueOf(resumo.folgasAprovadas()),
                    nvl(resumo.horasFormatadas())
            };
            xCursor = MARGEM + 3f;
            for (int i = 0; i < totais.length; i++) {
                texto(cs, F_BOLD, 7f, xCursor, y - ROW_H + 5f, totais[i]);
                xCursor += colW[i] * UTIL_W;
            }
        }

        // Rodapé
        cs.setNonStrokingColor(0.55f, 0.55f, 0.55f);
        texto(cs, F_NORM, 6.5f, MARGEM, MARGEM - 12f,
                "Portal de Gestão Levi's Staff — gerado automaticamente");
    }

    private String nvl(String valor) {
        return valor != null ? valor : "-";
    }

    private String truncar(String texto, int max) {
        if (texto == null) return "";
        if (max <= 0) return "";
        return texto.length() > max ? texto.substring(0, max - 1) + "…" : texto;
    }
}
