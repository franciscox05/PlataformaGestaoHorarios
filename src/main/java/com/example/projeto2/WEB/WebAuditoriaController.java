package com.example.projeto2.WEB;

import com.example.projeto2.BLL.AuditoriaBLL;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/web/auditoria")
public class WebAuditoriaController {

    private final WebAppService webAppService;
    private final AuditoriaBLL auditoriaBLL;

    public WebAuditoriaController(WebAppService webAppService, AuditoriaBLL auditoriaBLL) {
        this.webAppService = webAppService;
        this.auditoriaBLL = auditoriaBLL;
    }

    @GetMapping
    public String auditoria(
            @RequestParam(value = "tipo", required = false) String tipoEvento,
            @RequestParam(value = "colaborador", required = false) Integer idColaborador,
            @RequestParam(value = "dataInicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(value = "dataFim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            HttpSession session,
            Model model) {

        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "auditoria");

        try {
            AuditoriaBLL.FiltroAuditoria filtro = new AuditoriaBLL.FiltroAuditoria(
                    tipoEvento, idColaborador, dataInicio, dataFim);
            AuditoriaBLL.PainelAuditoriaSnapshot snapshot =
                    auditoriaBLL.carregarPainel(utilizadorId, filtro);

            model.addAttribute("snapshot", snapshot);
            model.addAttribute("contexto", snapshot.contexto());
            model.addAttribute("resumo", snapshot.resumo());
            model.addAttribute("eventos", snapshot.eventos());
            model.addAttribute("filtroTipo", tipoEvento);
            model.addAttribute("filtroColaborador", idColaborador);
            model.addAttribute("filtroDataInicio", dataInicio);
            model.addAttribute("filtroDataFim", dataFim);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
        }

        return "web/auditoria";
    }
}
