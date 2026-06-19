package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Services.GeracaoHorariosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/**
 * REST endpoint for the high-fidelity branded PDF export of a monthly schedule.
 *
 * GET /api/horarios/exportar-pdf?ano=2026&mes=6
 *   → application/pdf download, landscape A4 with brand colours, shift badges,
 *     and a summary metrics row.
 *
 * Distinct from /web/horarios/exportar.pdf (the legacy portrait-A4 export in
 * WebHorariosController); both endpoints can coexist safely.
 */
@RestController
@RequestMapping("/api/horarios")
public class HorarioExportController {

    private final PdfExportService pdfService;
    private final GeracaoHorariosService geracaoHorariosBLL;
    private final WebAppService webAppService;
    private final LojautilizadorRepository lojautilizadorRepo;

    public HorarioExportController(PdfExportService pdfService,
                                    GeracaoHorariosService geracaoHorariosBLL,
                                    WebAppService webAppService,
                                    LojautilizadorRepository lojautilizadorRepo) {
        this.pdfService          = pdfService;
        this.geracaoHorariosBLL  = geracaoHorariosBLL;
        this.webAppService       = webAppService;
        this.lojautilizadorRepo  = lojautilizadorRepo;
    }

    @GetMapping(value = "/exportar-pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam("ano") int ano,
            @RequestParam("mes") int mes,
            HttpSession session) {

        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            List<Horario> horarios = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, ano, mes);

            String nome = (String) session.getAttribute(WebSession.UTILIZADOR_NOME);

            String cargo = lojautilizadorRepo
                    .findLigacaoAtivaByIdUtilizador(utilizadorId)
                    .map(lu -> lu.getIdCargo() != null ? lu.getIdCargo().getNome() : "")
                    .orElse("");

            byte[] pdf = pdfService.generateSchedulePdf(
                    nome   != null ? nome  : "",
                    cargo,
                    horarios,
                    YearMonth.of(ano, mes));

            String filename = "horario-" + ano + "-" + String.format("%02d", mes) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
