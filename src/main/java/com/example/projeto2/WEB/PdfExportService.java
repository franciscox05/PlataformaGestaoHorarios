package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Horario;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a high-fidelity landscape-A4 PDF of an employee's monthly schedule.
 *
 * Page 1..N  — tabular list view with summary metrics row and shift badges.
 * Page N+1   — calendar grid view (one cell per day, colour-coded shift badges).
 *
 * Uses brand colours extracted from the Tailwind config in fragments.html:
 *   primary      #c0001f  sidebar   #1e1a19
 *   badge-warn   #fef3c7 / #92400e  (MANHA)
 *   badge-neu    #f1edec / #5c403e  (INTERMEDIO)
 *   badge-err    #fee2e2 / #991b1b  (NOITE)
 *   badge-ok     #d1fae5 / #065f46  (FOLGA)
 */
@Service
public class PdfExportService {

    // ── Page geometry — Landscape A4 ──────────────────────────────────────────
    private static final float PG_W   = PDRectangle.A4.getHeight(); // 841.89 pt
    private static final float PG_H   = PDRectangle.A4.getWidth();  // 595.28 pt
    private static final float MARGEM = 36f;
    private static final float UTIL_W = PG_W - 2f * MARGEM;

    // Shared zone heights
    private static final float HDR_H     = 58f;
    private static final float ACCENT_H  = 3f;
    private static final float FOOTER_H  = 20f;

    // List-view specific heights
    private static final float SUMMARY_H = 50f;
    private static final float COL_HDR_H = 20f;
    private static final float ROW_H     = 17f;

    // Calendar-view weekday row height
    private static final float CAL_WD_H  = 20f;

    // List-view column proportions (must sum to 1.0)
    // Date | Weekday | ShiftBadge | Start | End | Duration
    private static final float[] COL_RATIOS = {0.15f, 0.23f, 0.14f, 0.14f, 0.14f, 0.20f};
    private static final String[] COL_LABELS = {"Data", "Dia da Semana", "Turno", "Início", "Fim", "Duração"};

    // Calendar weekday header labels — WinAnsiEncoding supports Á (U+00C1) in Helvetica
    private static final String[] WD_LABELS = {"SEG", "TER", "QUA", "QUI", "SEX", "SÁB", "DOM"};

    // ── Brand colours  RGB [0..1] extracted from fragments.html ───────────────
    private static final float[] RED      = {0.753f, 0.000f, 0.122f}; // #c0001f primary
    private static final float[] RED_DARK = {0.478f, 0.000f, 0.078f}; // #7a0014 primary-dark
    private static final float[] SIDEBAR  = {0.118f, 0.102f, 0.098f}; // #1e1a19 inverse-surface
    private static final float[] INK      = {0.110f, 0.106f, 0.106f}; // #1c1b1b on-surface
    private static final float[] INK_SOFT = {0.361f, 0.251f, 0.243f}; // #5c403e on-surface-variant
    private static final float[] BORDER   = {0.898f, 0.741f, 0.729f}; // #e5bdba outline-variant
    private static final float[] STRIPE   = {0.969f, 0.949f, 0.949f}; // #f7f2f2 surface-container-low
    private static final float[] WHITE    = {1.000f, 1.000f, 1.000f};

    // Weekend cell tint (very subtle, distinguishable from white but not intrusive)
    private static final float[] WEEKEND_BG = {0.984f, 0.969f, 0.969f};

    // Badge colours [bgR,bgG,bgB, textR,textG,textB] — matches CSS badge classes
    private static final float[] B_MANHA = {0.996f,0.953f,0.780f, 0.573f,0.251f,0.055f}; // #fef3c7/#92400e
    private static final float[] B_INTER = {0.945f,0.929f,0.925f, 0.361f,0.251f,0.243f}; // #f1edec/#5c403e
    private static final float[] B_NOITE = {0.996f,0.886f,0.886f, 0.600f,0.106f,0.106f}; // #fee2e2/#991b1b
    private static final float[] B_FOLGA = {0.820f,0.980f,0.898f, 0.024f,0.373f,0.275f}; // #d1fae5/#065f46

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font NORM = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    // ── Formatters ────────────────────────────────────────────────────────────
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DAY  = DateTimeFormatter.ofPattern("EEEE", new Locale("pt", "PT"));
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a multi-page landscape-A4 PDF:
     * pages 1..N  = tabular list view;  page N+1 = calendar grid view.
     */
    public byte[] generateSchedulePdf(String nomeUtilizador,
                                       String cargo,
                                       List<Horario> horarios,
                                       YearMonth yearMonth) throws IOException {

        List<Horario> sorted = horarios == null ? List.of()
                : horarios.stream()
                          .filter(h -> h.getDataTurno() != null)
                          .sorted(Comparator.comparing(Horario::getDataTurno))
                          .toList();

        // ── Summary metrics ───────────────────────────────────────────────────
        long totalTurnos     = sorted.size();
        long diasTrabalhados = sorted.stream().map(Horario::getDataTurno).distinct().count();
        long totalMinutos    = sorted.stream().mapToLong(this::minutesBetween).sum();
        String horasLabel    = formatMinutes(totalMinutos);

        Map<String, Long> porTipo = sorted.stream()
                .filter(h -> h.getIdTurno() != null && h.getIdTurno().getTipo() != null)
                .collect(Collectors.groupingBy(
                        h -> h.getIdTurno().getTipo().toUpperCase(Locale.ROOT),
                        Collectors.counting()));
        String topTipo = porTipo.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("-");

        // ── Period label ──────────────────────────────────────────────────────
        String mesNome = yearMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "PT"));
        mesNome = Character.toUpperCase(mesNome.charAt(0)) + mesNome.substring(1);
        String periodoLabel = san(mesNome) + " de " + yearMonth.getYear();

        // ── List-view pagination ──────────────────────────────────────────────
        float dataStartY = PG_H - HDR_H - ACCENT_H - SUMMARY_H - COL_HDR_H;
        float dataEndY   = MARGEM + FOOTER_H + 4f;
        int rowsPerPage  = Math.max(1, (int) ((dataStartY - dataEndY) / ROW_H));

        List<List<Horario>> pages = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i += rowsPerPage) {
            pages.add(sorted.subList(i, Math.min(i + rowsPerPage, sorted.size())));
        }
        if (pages.isEmpty()) pages.add(List.of());

        int totalPages = pages.size() + 1;  // +1 for the calendar grid page

        // ── Build PDF ─────────────────────────────────────────────────────────
        try (PDDocument doc = new PDDocument()) {

            // List-view pages
            for (int p = 0; p < pages.size(); p++) {
                PDPage page = new PDPage(new PDRectangle(PG_W, PG_H));
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    drawListPage(cs, pages.get(p), nomeUtilizador, cargo, periodoLabel,
                                 p + 1, totalPages,
                                 totalTurnos, diasTrabalhados, horasLabel, topTipo);
                }
            }

            // Calendar grid page (always last)
            PDPage calPage = new PDPage(new PDRectangle(PG_W, PG_H));
            doc.addPage(calPage);
            try (PDPageContentStream cs = new PDPageContentStream(doc, calPage)) {
                drawCalendarPage(cs, sorted, yearMonth,
                        nomeUtilizador, cargo, periodoLabel,
                        totalPages, totalPages);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Page 1..N  — Tabular list view
    // ═════════════════════════════════════════════════════════════════════════

    private void drawListPage(PDPageContentStream cs,
                               List<Horario> rows,
                               String nomeUtilizador, String cargo, String periodo,
                               int page, int totalPages,
                               long totalTurnos, long diasTrabalhados,
                               String horasLabel, String topTipo) throws IOException {

        // ── 1. Brand header band ──────────────────────────────────────────────
        drawHeader(cs, "Horário Mensal  |  " + san(nomeUtilizador),
                   san(cargo) + "     |     " + periodo, page, totalPages);

        // ── 2. Red accent divider ─────────────────────────────────────────────
        float accentY = PG_H - HDR_H - ACCENT_H;
        fill(cs, RED);
        cs.addRect(MARGEM, accentY, UTIL_W, ACCENT_H);
        cs.fill();

        // ── 3. Summary metrics row ────────────────────────────────────────────
        float sumY = accentY - SUMMARY_H;
        fill(cs, STRIPE);
        cs.addRect(MARGEM, sumY, UTIL_W, SUMMARY_H);
        cs.fill();

        String[] mLabels = {"Turnos Publicados", "Dias Trabalhados", "Horas Estimadas", "Tipo Predominante"};
        String[] mValues = {String.valueOf(totalTurnos), String.valueOf(diasTrabalhados),
                            horasLabel, san(tipoDisplay(topTipo))};
        float mW = UTIL_W / 4f;

        for (int i = 0; i < 4; i++) {
            float mx = MARGEM + i * mW + 14f;
            fill(cs, RED);
            txt(cs, BOLD, 14f, mx, sumY + SUMMARY_H - 19f, mValues[i]);
            fill(cs, INK_SOFT);
            txt(cs, NORM, 7f,  mx, sumY + 9f, mLabels[i].toUpperCase());
            if (i < 3) {
                stroke(cs, BORDER);
                cs.moveTo(MARGEM + (i + 1) * mW, sumY + 7f);
                cs.lineTo(MARGEM + (i + 1) * mW, sumY + SUMMARY_H - 7f);
                cs.stroke();
            }
        }

        // ── 4. Column header row ──────────────────────────────────────────────
        float colHdrY = sumY - COL_HDR_H;
        fill(cs, INK);
        cs.addRect(MARGEM, colHdrY, UTIL_W, COL_HDR_H);
        cs.fill();

        // Absolute left edge of each column — reused in data rows below
        float[] colStarts = new float[COL_RATIOS.length];
        { float _x = MARGEM; for (int i = 0; i < COL_RATIOS.length; i++) { colStarts[i] = _x; _x += COL_RATIOS[i] * UTIL_W; } }

        fill(cs, WHITE);
        for (int i = 0; i < COL_LABELS.length; i++) {
            String label = COL_LABELS[i].toUpperCase();
            float hx = (i < 2)
                    ? colStarts[i] + 5f
                    : colStarts[i] + (COL_RATIOS[i] * UTIL_W - (BOLD.getStringWidth(label) / 1000f) * 7.5f) / 2f;
            txt(cs, BOLD, 7.5f, hx, colHdrY + 7f, label);
        }

        // ── 5. Data rows ──────────────────────────────────────────────────────
        float y     = colHdrY;
        boolean alt = false;

        for (Horario h : rows) {
            y -= ROW_H;

            if (alt) {
                fill(cs, STRIPE);
                cs.addRect(MARGEM, y, UTIL_W, ROW_H);
                cs.fill();
            }

            stroke(cs, BORDER);
            cs.moveTo(MARGEM, y);
            cs.lineTo(MARGEM + UTIL_W, y);
            cs.stroke();

            String data = h.getDataTurno() != null ? FMT_DATE.format(h.getDataTurno()) : "-";
            String dia  = h.getDataTurno() != null ? cap(san(FMT_DAY.format(h.getDataTurno()))) : "-";
            String tipo = (h.getIdTurno() != null && h.getIdTurno().getTipo() != null)
                          ? h.getIdTurno().getTipo() : "-";
            String ini  = (h.getIdTurno() != null && h.getIdTurno().getHoraInicio() != null)
                          ? FMT_TIME.format(h.getIdTurno().getHoraInicio()) : "-";
            String fim  = (h.getIdTurno() != null && h.getIdTurno().getHoraFim() != null)
                          ? FMT_TIME.format(h.getIdTurno().getHoraFim()) : "-";
            long mins   = minutesBetween(h);
            String dur  = mins > 0 ? formatMinutes(mins) : "-";

            float rowTextY = y + (ROW_H - 7f) / 2f + 1f;
            float[] badge  = badgeCols(tipo);

            // Col 0: Data — left-aligned
            fill(cs, INK);
            txt(cs, BOLD, 8.5f, colStarts[0] + 5f, rowTextY, data);

            // Col 1: Dia da Semana — left-aligned
            fill(cs, INK_SOFT);
            txt(cs, NORM, 8f, colStarts[1] + 5f, rowTextY, dia);

            // Col 2: Turno — badge rect centered, text centered within badge
            float colW2  = COL_RATIOS[2] * UTIL_W;
            float badgeW = colW2 - 10f;
            float badgeH = ROW_H - 4f;
            float bx     = colStarts[2] + (colW2 - badgeW) / 2f;
            cs.setNonStrokingColor(badge[0], badge[1], badge[2]);
            cs.addRect(bx, y + 2f, badgeW, badgeH);
            cs.fill();
            String tipoStr = san(tipoDisplay(tipo));
            float tipoW = (BOLD.getStringWidth(tipoStr) / 1000f) * 7.5f;
            cs.setNonStrokingColor(badge[3], badge[4], badge[5]);
            txt(cs, BOLD, 7.5f, bx + (badgeW - tipoW) / 2f, rowTextY, tipoStr);

            // Col 3: Início — centered in column
            float iniW = (NORM.getStringWidth(ini) / 1000f) * 8.5f;
            fill(cs, INK);
            txt(cs, NORM, 8.5f, colStarts[3] + (COL_RATIOS[3] * UTIL_W - iniW) / 2f, rowTextY, ini);

            // Col 4: Fim — centered in column
            float fimW = (NORM.getStringWidth(fim) / 1000f) * 8.5f;
            txt(cs, NORM, 8.5f, colStarts[4] + (COL_RATIOS[4] * UTIL_W - fimW) / 2f, rowTextY, fim);

            // Col 5: Duração — centered in column
            float durW = (BOLD.getStringWidth(dur) / 1000f) * 8.5f;
            txt(cs, BOLD, 8.5f, colStarts[5] + (COL_RATIOS[5] * UTIL_W - durW) / 2f, rowTextY, dur);

            alt = !alt;
        }

        if (rows.isEmpty()) {
            fill(cs, INK_SOFT);
            txt(cs, NORM, 10f, MARGEM + UTIL_W / 2f - 90f, colHdrY - 30f,
                "Sem turnos publicados para este periodo.");
        }

        drawFooter(cs, page, totalPages);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Page N+1  — Calendar grid view
    // ═════════════════════════════════════════════════════════════════════════

    private void drawCalendarPage(PDPageContentStream cs,
                                   List<Horario> horarios,
                                   YearMonth yearMonth,
                                   String nomeUtilizador, String cargo, String periodo,
                                   int page, int totalPages) throws IOException {

        // ── 1. Brand header ───────────────────────────────────────────────────
        drawHeader(cs, "Vista Calendário  |  " + san(nomeUtilizador),
                   san(cargo) + "     |     " + periodo, page, totalPages);

        // ── 2. Red accent divider ─────────────────────────────────────────────
        float accentY = PG_H - HDR_H - ACCENT_H;
        fill(cs, RED);
        cs.addRect(MARGEM, accentY, UTIL_W, ACCENT_H);
        cs.fill();

        // ── 3. Calendar geometry ──────────────────────────────────────────────
        // gridTopY = bottom of weekday header row = top edge of cell grid
        float gridTopY    = accentY - CAL_WD_H;
        float gridBottomY = MARGEM + FOOTER_H + 4f;
        float gridH       = gridTopY - gridBottomY;
        float cellW       = UTIL_W / 7f;

        // Weeks needed: ⌈(leadingOffset + daysInMonth) / 7⌉
        int firstDow  = yearMonth.atDay(1).getDayOfWeek().getValue(); // Mon=1..Sun=7
        int offset    = firstDow - 1;                                 // 0-indexed leading blanks
        int totalDays = yearMonth.lengthOfMonth();
        int weeks     = (int) Math.ceil((offset + totalDays) / 7.0);
        float cellH   = gridH / weeks;

        // ── 4. Weekday header row ─────────────────────────────────────────────
        fill(cs, INK);
        cs.addRect(MARGEM, gridTopY, UTIL_W, CAL_WD_H);
        cs.fill();

        // Column separators in the weekday header
        stroke(cs, INK_SOFT);
        for (int col = 1; col < 7; col++) {
            float lx = MARGEM + col * cellW;
            cs.moveTo(lx, gridTopY + 3f);
            cs.lineTo(lx, gridTopY + CAL_WD_H - 3f);
        }
        cs.stroke();

        // Weekday labels: weekends in primary red, workdays in white
        for (int col = 0; col < 7; col++) {
            fill(cs, col >= 5 ? RED : WHITE);
            float labelX = MARGEM + col * cellW + (cellW - 14f) / 2f;
            txt(cs, BOLD, 7.5f, labelX, gridTopY + 7f, WD_LABELS[col]);
        }

        // ── 5. Calendar grid — Pass 1: fill cell backgrounds ─────────────────
        Map<Integer, Horario> dayMap = buildDayMap(horarios);

        for (int week = 0; week < weeks; week++) {
            for (int col = 0; col < 7; col++) {
                int slot   = week * 7 + col;
                int dayNum = slot - offset + 1;
                float cx   = MARGEM + col * cellW;
                float cy   = gridTopY - (week + 1) * cellH;

                if (dayNum < 1 || dayNum > totalDays) {
                    // Leading / trailing blank cell
                    fill(cs, STRIPE);
                } else {
                    boolean isWeekend = (col == 5 || col == 6);
                    if (isWeekend) {
                        cs.setNonStrokingColor(WEEKEND_BG[0], WEEKEND_BG[1], WEEKEND_BG[2]);
                    } else {
                        fill(cs, WHITE);
                    }
                }
                cs.addRect(cx, cy, cellW, cellH);
                cs.fill();
            }
        }

        // ── 6. Calendar grid — Pass 2: stroke grid lines ──────────────────────
        stroke(cs, BORDER);
        for (int col = 0; col <= 7; col++) {
            float lx = MARGEM + col * cellW;
            cs.moveTo(lx, gridTopY);
            cs.lineTo(lx, gridBottomY);
        }
        for (int row = 0; row <= weeks; row++) {
            float ly = gridTopY - row * cellH;
            cs.moveTo(MARGEM, ly);
            cs.lineTo(MARGEM + UTIL_W, ly);
        }
        cs.stroke();

        // ── 7. Calendar grid — Pass 3: day numbers and shift badges ───────────
        for (int week = 0; week < weeks; week++) {
            for (int col = 0; col < 7; col++) {
                int slot   = week * 7 + col;
                int dayNum = slot - offset + 1;
                if (dayNum < 1 || dayNum > totalDays) continue;

                float cx = MARGEM + col * cellW;
                float cy = gridTopY - (week + 1) * cellH;

                // Day number at top-left of cell (red for weekends, dark for weekdays)
                boolean isWeekend = (col == 5 || col == 6);
                fill(cs, isWeekend ? RED : INK);
                txt(cs, BOLD, 9f, cx + 5f, cy + cellH - 14f, String.valueOf(dayNum));

                // Shift badge — centered in the body area below the day number
                Horario h = dayMap.get(dayNum);
                if (h != null && h.getIdTurno() != null) {
                    String tipo = h.getIdTurno().getTipo() != null
                                  ? h.getIdTurno().getTipo() : "?";
                    String ini  = h.getIdTurno().getHoraInicio() != null
                                  ? FMT_TIME.format(h.getIdTurno().getHoraInicio()) : "";
                    String fim  = h.getIdTurno().getHoraFim()   != null
                                  ? FMT_TIME.format(h.getIdTurno().getHoraFim())    : "";

                    float[] badge = badgeCols(tipo);

                    // Badge geometry: fills the body area (below day-number row)
                    float numAreaH = 18f;
                    float bodyH    = cellH - numAreaH;
                    float bH       = Math.min(36f, bodyH - 8f);
                    float bW       = cellW - 10f;
                    float bx       = cx + 5f;
                    float by       = cy + (bodyH - bH) / 2f;

                    // Fill badge background
                    cs.setNonStrokingColor(badge[0], badge[1], badge[2]);
                    cs.addRect(bx, by, bW, bH);
                    cs.fill();

                    // Shift type — bold, horizontally centered in cell
                    String tipoStr = san(tipoDisplay(tipo));
                    float shiftTextWidth = (BOLD.getStringWidth(tipoStr) / 1000f) * 7f;
                    float shiftX = cx + (cellW - shiftTextWidth) / 2f;
                    cs.setNonStrokingColor(badge[3], badge[4], badge[5]);
                    txt(cs, BOLD, 7f, shiftX, by + bH - 11f, tipoStr);

                    // Time range — normal, horizontally centered in cell
                    if (!ini.isEmpty() && !fim.isEmpty()) {
                        String timeStr = ini + "-" + fim;
                        float timeTextWidth = (NORM.getStringWidth(timeStr) / 1000f) * 6.5f;
                        float timeX = cx + (cellW - timeTextWidth) / 2f;
                        txt(cs, NORM, 6.5f, timeX, by + 4f, timeStr);
                    }
                }
            }
        }

        drawFooter(cs, page, totalPages);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Shared rendering helpers
    // ═════════════════════════════════════════════════════════════════════════

    /** Draws the dark brand header (shared by both page types). */
    private void drawHeader(PDPageContentStream cs, String title, String subtitle,
                             int page, int totalPages) throws IOException {
        // Background band
        fill(cs, SIDEBAR);
        cs.addRect(0, PG_H - HDR_H, PG_W, HDR_H);
        cs.fill();

        // Left red accent stripe
        fill(cs, RED);
        cs.addRect(0, PG_H - HDR_H, 4f, HDR_H);
        cs.fill();

        // Logo block — red square, "L" mathematically centred (matches web sidebar design)
        float logoSize  = 32f;
        float logoX     = MARGEM;
        float logoY     = PG_H - HDR_H + (HDR_H - logoSize) / 2f;
        fill(cs, RED);
        cs.addRect(logoX, logoY, logoSize, logoSize);
        cs.fill();
        float lFont = 17f;
        float lW    = (BOLD.getStringWidth("L") / 1000f) * lFont;
        float lAsc  = 0.718f * lFont;  // Helvetica Bold ascent = 718/1000 em
        fill(cs, WHITE);
        txt(cs, BOLD, lFont,
                logoX + (logoSize - lW)   / 2f,
                logoY + (logoSize - lAsc) / 2f,
                "L");

        // Title and subtitle — 10 pt gap after logo right edge
        float titleX = logoX + logoSize + 10f;
        fill(cs, WHITE);
        txt(cs, BOLD, 13.5f, titleX, PG_H - HDR_H + 35f, title);
        txt(cs, NORM, 8.5f,  titleX, PG_H - HDR_H + 19f, subtitle);
    }

    /** Draws the thin divider and branding footer (shared by both page types). */
    private void drawFooter(PDPageContentStream cs, int page, int totalPages) throws IOException {
        stroke(cs, BORDER);
        cs.moveTo(MARGEM, MARGEM + FOOTER_H - 2f);
        cs.lineTo(MARGEM + UTIL_W, MARGEM + FOOTER_H - 2f);
        cs.stroke();

        fill(cs, INK_SOFT);
        txt(cs, NORM, 7f, MARGEM, MARGEM + 5f,
                "Levis Staff Portal  |  Horário Mensal  |  Gerado em "
                + FMT_DATE.format(LocalDate.now()));
        txt(cs, NORM, 7f, PG_W - MARGEM - 65f, MARGEM + 5f,
                "Pag. " + page + " / " + totalPages);
    }

    // ── Domain helpers ────────────────────────────────────────────────────────

    /** Builds a map from day-of-month (1-based) to the first Horario on that day. */
    private Map<Integer, Horario> buildDayMap(List<Horario> horarios) {
        Map<Integer, Horario> map = new HashMap<>();
        for (Horario h : horarios) {
            if (h.getDataTurno() != null) {
                map.putIfAbsent(h.getDataTurno().getDayOfMonth(), h);
            }
        }
        return map;
    }

    private float[] badgeCols(String tipo) {
        if (tipo == null) return B_INTER;
        String t = tipo.toLowerCase(Locale.ROOT);
        if (t.contains("manh"))  return B_MANHA;
        if (t.contains("noite")) return B_NOITE;
        if (t.contains("folga")) return B_FOLGA;
        return B_INTER;
    }

    private String tipoDisplay(String tipo) {
        if (tipo == null) return "-";
        String t = tipo.toLowerCase(Locale.ROOT);
        if (t.contains("manh"))  return "MANHÃ";
        if (t.contains("inter")) return "INTERMÉDIO";
        if (t.contains("noite")) return "NOITE";
        if (t.contains("tarde")) return "TARDE";
        if (t.contains("folga")) return "FOLGA";
        return tipo.toUpperCase(Locale.ROOT);
    }

    private long minutesBetween(Horario h) {
        if (h.getIdTurno() == null
                || h.getIdTurno().getHoraInicio() == null
                || h.getIdTurno().getHoraFim() == null) return 0;
        return Duration.between(h.getIdTurno().getHoraInicio(),
                                h.getIdTurno().getHoraFim()).toMinutes();
    }

    private String formatMinutes(long mins) {
        if (mins <= 0) return "0h";
        long h = mins / 60;
        long m = mins % 60;
        return m == 0 ? h + "h" : h + "h" + String.format("%02d", m) + "m";
    }

    // ── Drawing primitives ────────────────────────────────────────────────────

    private void fill(PDPageContentStream cs, float[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private void stroke(PDPageContentStream cs, float[] rgb) throws IOException {
        cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private void txt(PDPageContentStream cs, PDType1Font font, float size,
                     float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    /**
     * Sanitizes a string for use with PDFBox's PDType1Font (Helvetica / WinAnsiEncoding).
     *
     * WinAnsiEncoding covers:
     *   U+0020–U+007E  standard ASCII printable characters
     *   U+00A0–U+00FF  ISO-8859-1 Latin-1 Supplement (all Portuguese accented letters,
     *                  ã, é, ê, á, à, â, í, ó, ô, õ, ú, ç and their uppercase forms)
     *
     * Characters in U+0080–U+009F (Windows-1252 private additions: €, ‚, ƒ, etc.) and
     * any character above U+00FF are silently dropped — they are not in the encoding and
     * would cause an IllegalArgumentException in PDPageContentStream.showText().
     */
    private String san(String v) {
        if (v == null) return "-";
        StringBuilder sb = new StringBuilder(v.length());
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if ((c >= 0x0020 && c <= 0x007E) || (c >= 0x00A0 && c <= 0x00FF)) {
                sb.append(c);
            }
            // 0x0080–0x009F: Windows-1252 additions not in Helvetica — drop silently
            // Above 0x00FF: outside WinAnsiEncoding — drop silently
        }
        return sb.length() > 0 ? sb.toString() : "-";
    }

    private String cap(String v) {
        if (v == null || v.isBlank()) return v != null ? v : "";
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }
}
