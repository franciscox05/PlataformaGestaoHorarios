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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

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
            List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, anoConsulta, mesConsulta);
            model.addAttribute("turnos", turnos);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
            model.addAttribute("turnos", List.of());
        }

        // Carrega propostas mensais para gerentes/subgerentes
        if (geracaoHorariosBLL.utilizadorPodeGerarHorarios(utilizadorId)) {
            try {
                List<GeracaoHorariosService.PropostaResumo> propostas =
                        geracaoHorariosBLL.listarPropostas(utilizadorId, anoConsulta, mesConsulta);
                model.addAttribute("propostas", propostas);
            } catch (IllegalArgumentException ex) {
                model.addAttribute("propostas", List.of());
            }
        }

        return "web/horarios";
    }

    /** W2 — Gerar nova proposta de horário mensal */
    @PostMapping("/gerar")
    public String gerarHorario(@RequestParam("ano") Integer ano,
                               @RequestParam("mes") Integer mes,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            GeracaoHorariosService.PropostaResultado resultado =
                    geracaoHorariosBLL.gerarProposta(utilizadorId, ano, mes);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Proposta gerada com sucesso (" + resultado.metricas().qualidade()
                    + ", score " + resultado.metricas().pontuacao() + ").");
        } catch (GeracaoHorariosService.FalhaGeracaoHorarioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage()
                    + (ex.motivoPrincipal() != null ? " — " + ex.motivoPrincipal() : ""));
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/horarios?ano=" + ano + "&mes=" + mes;
    }

    /** W7 — Exportar horário mensal como PDF */
    @GetMapping(value = "/exportar.pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> exportarPdf(@RequestParam("ano") Integer ano,
                                               @RequestParam("mes") Integer mes,
                                               HttpSession session) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        List<Horario> turnos = geracaoHorariosBLL.obterMeusHorarios(utilizadorId, ano, mes);

        String nomeUtilizador = (String) session.getAttribute(WebSession.UTILIZADOR_NOME);
        byte[] conteudo = webPdfService.gerarHorarioMensalPdf(
                turnos, ano, mes, nomeUtilizador != null ? nomeUtilizador : "");

        String nomeFicheiro = "horario-" + ano + "-" + String.format("%02d", mes) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeFicheiro + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(conteudo);
    }

    /** W1 — Enviar proposta para validação pelo supervisor */
    @PostMapping("/propostas/{idProposta}/enviar")
    public String enviarParaSupervisor(@PathVariable("idProposta") Integer idProposta,
                                       @RequestParam(value = "ano", required = false) Integer ano,
                                       @RequestParam(value = "mes", required = false) Integer mes,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            geracaoHorariosBLL.enviarPropostasParaValidacao(utilizadorId, List.of(idProposta));
            redirectAttributes.addFlashAttribute("sucesso", "Proposta enviada ao supervisor para validação.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        String redirect = "/web/horarios";
        if (ano != null && mes != null) {
            redirect += "?ano=" + ano + "&mes=" + mes;
        }
        return "redirect:" + redirect;
    }
}
