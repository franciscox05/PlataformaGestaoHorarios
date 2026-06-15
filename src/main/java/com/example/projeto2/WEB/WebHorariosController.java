package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.GeracaoHorariosService;
import com.example.projeto2.API.Modules.Horario;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/web/horarios")
public class WebHorariosController {

    private final GeracaoHorariosService geracaoHorariosBLL;
    private final WebAppService webAppService;
    private final WebPdfService webPdfService;

    public WebHorariosController(GeracaoHorariosService geracaoHorariosBLL,
                                 WebAppService webAppService,
                                 WebPdfService webPdfService) {
        this.geracaoHorariosBLL = geracaoHorariosBLL;
        this.webAppService = webAppService;
        this.webPdfService = webPdfService;
    }

    @GetMapping
    public String horarios(@RequestParam(value = "ano", required = false) Integer ano,
                           @RequestParam(value = "mes", required = false) Integer mes,
                           HttpSession session,
                           Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "horarios");

        LocalDate hoje = LocalDate.now();
        int anoConsulta = ano != null ? ano : hoje.getYear();
        int mesConsulta = mes != null ? mes : hoje.getMonthValue();

        model.addAttribute("ano", anoConsulta);
        model.addAttribute("mes", mesConsulta);
        model.addAttribute("hoje", hoje);
        model.addAttribute("meses", WebMesOption.todos());
        model.addAttribute("anos", WebMesOption.anosProximos(hoje.getYear(), 2));

        try {
            Integer idLoja = webAppService.obterLojaAtual(session);
            List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, idLoja, anoConsulta, mesConsulta);
            model.addAttribute("turnos", turnos);

            DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
            List<Map<String, Object>> turnosCalendario = turnos.stream()
                .filter(t -> t.getDataTurno() != null && t.getIdTurno() != null)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("dia", t.getDataTurno().getDayOfMonth());
                    m.put("tipo", t.getIdTurno().getTipo() != null ? t.getIdTurno().getTipo() : "");
                    m.put("inicio", t.getIdTurno().getHoraInicio() != null
                            ? t.getIdTurno().getHoraInicio().format(hhmm) : "");
                    m.put("fim", t.getIdTurno().getHoraFim() != null
                            ? t.getIdTurno().getHoraFim().format(hhmm) : "");
                    return m;
                })
                .toList();
            model.addAttribute("turnosCalendario", turnosCalendario);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            model.addAttribute("turnos", List.of());
            model.addAttribute("turnosCalendario", List.of());
        }

        return "web/horarios";
    }

    @GetMapping(value = "/exportar.pdf", produces = "application/pdf")
    public Object exportarPdf(@RequestParam("ano") Integer ano,
                              @RequestParam("mes") Integer mes,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            Integer idLoja = webAppService.obterLojaAtual(session);
            List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, idLoja, ano, mes);
            String nomeUtilizador = (String) session.getAttribute(WebSession.UTILIZADOR_NOME);
            byte[] conteudo = webPdfService.gerarHorarioMensalPdf(
                    turnos, ano, mes, nomeUtilizador != null ? nomeUtilizador : "");
            String nomeFicheiro = "horario-" + ano + "-" + String.format("%02d", mes) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeFicheiro + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(conteudo);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
        }
    }
}
