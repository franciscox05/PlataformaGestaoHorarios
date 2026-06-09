package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Services.RelatorioHorasService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Serviço utilitário para gerar PDFs a partir de dados BLL.
 * Usa PDFBox 3.x (já no pom.xml).
 */
@Service
public class WebPdfService {

    private static final DateTimeFormatter DATA_PT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();

    // ── W6 — Relatório de horas ──────────────────────────────────────────────

    public byte[] gerarRelatorioHorasPdf(RelatorioHorasService.RelatorioResultado resultado) {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Título
                y = writeTitle(cs, "Relatório de Horas — " + resultado.nomeLoja(), y);
                y -= 4;
                y = writeSubtitle(cs, resultado.nomeMes() + " de " + resultado.ano(), y);
                y -= 16;

                // Cabeçalho da tabela
                String[] headers = { "Colaborador", "Cargo", "Turnos", "Folgas", "Horas" };
                float[] colWidths = { 180, 120, 50, 50, 60 };
                y = writeTableHeader(cs, headers, colWidths, y);

                // Linhas
                for (RelatorioHorasService.RelatorioLinha linha : resultado.linhas()) {
                    if (y < MARGIN + 20) {
                        // Nova página se necessário
                        cs.close();
                        PDPage newPage = new PDPage(PDRectangle.A4);
                        doc.addPage(newPage);
                        // Nota: PDFBox não suporta fechar+reabrir facilmente no mesmo try-with-resources,
                        // por isso limitamos a uma página por simplicidade
                        break;
                    }
                    String[] cells = {
                        truncate(linha.nomeColaborador(), 28),
                        truncate(linha.cargo(), 18),
                        String.valueOf(linha.turnos()),
                        String.valueOf(linha.folgasAprovadas()),
                        linha.horasFormatadas()
                    };
                    y = writeTableRow(cs, cells, colWidths, y);
                }

                // Rodapé
                writeFooter(cs, "Gerado em " + DATA_PT.format(LocalDate.now()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao gerar PDF do relatório.", e);
        }
    }

    // ── W7 — Horário mensal ──────────────────────────────────────────────────

    public byte[] gerarHorarioMensalPdf(List<com.example.projeto2.API.Modules.Horario> turnos,
                                         int ano, int mes, String nomeUtilizador) {
        String nomeMes = nomeMes(mes);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Título
                y = writeTitle(cs, "Horário de " + nomeMes + " " + ano, y);
                y -= 4;
                y = writeSubtitle(cs, nomeUtilizador, y);
                y -= 16;

                if (turnos == null || turnos.isEmpty()) {
                    writeText(cs, "Sem turnos publicados para este período.", MARGIN, y, 11);
                } else {
                    // Cabeçalho
                    String[] headers = { "Data", "Dia", "Turno", "Início", "Fim" };
                    float[] colWidths = { 80, 90, 100, 60, 60 };
                    y = writeTableHeader(cs, headers, colWidths, y);

                    DateTimeFormatter diaFmt = DateTimeFormatter.ofPattern("EEEE", new Locale("pt", "PT"));
                    for (com.example.projeto2.API.Modules.Horario h : turnos) {
                        if (y < MARGIN + 20) break; // limite simples
                        String data = h.getDataTurno() != null ? DATA_PT.format(h.getDataTurno()) : "-";
                        String dia = h.getDataTurno() != null
                                ? capitalize(diaFmt.format(h.getDataTurno())) : "-";
                        String tipo = h.getIdTurno() != null && h.getIdTurno().getTipo() != null
                                ? h.getIdTurno().getTipo() : "-";
                        String inicio = h.getIdTurno() != null && h.getIdTurno().getHoraInicio() != null
                                ? h.getIdTurno().getHoraInicio().toString() : "-";
                        String fim = h.getIdTurno() != null && h.getIdTurno().getHoraFim() != null
                                ? h.getIdTurno().getHoraFim().toString() : "-";
                        y = writeTableRow(cs, new String[]{ data, dia, tipo, inicio, fim }, colWidths, y);
                    }
                }

                writeFooter(cs, "Gerado em " + DATA_PT.format(LocalDate.now()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao gerar PDF do horário.", e);
        }
    }

    // ── Utilitários de desenho ───────────────────────────────────────────────

    private float writeTitle(PDPageContentStream cs, String text, float y) throws IOException {
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        cs.beginText();
        cs.setFont(fontBold, 16);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - 22;
    }

    private float writeSubtitle(PDPageContentStream cs, String text, float y) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.beginText();
        cs.setFont(font, 11);
        cs.newLineAtOffset(MARGIN, y);
        cs.showText(sanitize(text));
        cs.endText();
        return y - 16;
    }

    private void writeText(PDPageContentStream cs, String text, float x, float y, float size) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private float writeTableHeader(PDPageContentStream cs, String[] headers, float[] colWidths, float y) throws IOException {
        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        // Background line
        cs.setNonStrokingColor(0.9f, 0.9f, 0.9f);
        cs.addRect(MARGIN, y - 14, PAGE_WIDTH - 2 * MARGIN, 18);
        cs.fill();
        cs.setNonStrokingColor(0, 0, 0);

        float x = MARGIN + 4;
        cs.beginText();
        cs.setFont(fontBold, 9);
        for (int i = 0; i < headers.length; i++) {
            cs.newLineAtOffset(i == 0 ? x : colWidths[i - 1], 0);
            cs.showText(sanitize(headers[i]).toUpperCase());
            x += colWidths[i];
        }
        cs.endText();
        return y - 18;
    }

    private float writeTableRow(PDPageContentStream cs, String[] cells, float[] colWidths, float y) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float x = MARGIN + 4;
        cs.beginText();
        cs.setFont(font, 9);
        for (int i = 0; i < cells.length; i++) {
            cs.newLineAtOffset(i == 0 ? x : colWidths[i - 1], 0);
            cs.showText(sanitize(cells[i]));
            x += colWidths[i];
        }
        cs.endText();

        // Separator line
        cs.setStrokingColor(0.85f, 0.85f, 0.85f);
        cs.moveTo(MARGIN, y - 2);
        cs.lineTo(PAGE_WIDTH - MARGIN, y - 2);
        cs.stroke();
        cs.setStrokingColor(0, 0, 0);

        return y - 16;
    }

    private void writeFooter(PDPageContentStream cs, String text) throws IOException {
        writeText(cs, text, MARGIN, MARGIN - 10, 8);
    }

    private String sanitize(String value) {
        if (value == null) return "-";
        // PDFBox Type1 fonts only support ISO-8859-1; transliterate common accented chars
        return value
                .replace("é", "e").replace("ê", "e").replace("è", "e")
                .replace("à", "a").replace("á", "a").replace("â", "a")
                .replace("ã", "a").replace("ä", "a")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u").replace("û", "u")
                .replace("í", "i").replace("ï", "i")
                .replace("ç", "c")
                .replace("É", "E").replace("Ê", "E").replace("È", "E")
                .replace("À", "A").replace("Á", "A").replace("Â", "A")
                .replace("Ã", "A")
                .replace("Ó", "O").replace("Ô", "O").replace("Õ", "O")
                .replace("Ú", "U")
                .replace("Í", "I")
                .replace("Ç", "C");
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "-";
        return value.length() > maxLen ? value.substring(0, maxLen - 1) + "…" : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String nomeMes(int mes) {
        return switch (mes) {
            case 1 -> "Janeiro"; case 2 -> "Fevereiro"; case 3 -> "Marco";
            case 4 -> "Abril"; case 5 -> "Maio"; case 6 -> "Junho";
            case 7 -> "Julho"; case 8 -> "Agosto"; case 9 -> "Setembro";
            case 10 -> "Outubro"; case 11 -> "Novembro"; case 12 -> "Dezembro";
            default -> String.valueOf(mes);
        };
    }
}
