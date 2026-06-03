package com.example.projeto2.WEB;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web/modulos")
public class WebModulosController {

    private final WebLayoutService webLayoutService;

    public WebModulosController(WebLayoutService webLayoutService) {
        this.webLayoutService = webLayoutService;
    }

    @GetMapping("/gestao-loja")
    public String gestaoLoja(HttpSession session, Model model) {
        return moduloPlaceholder(session, model, "gestao-loja", "Gestao de loja");
    }

    @GetMapping("/relatorios")
    public String relatorios(HttpSession session, Model model) {
        return moduloPlaceholder(session, model, "relatorios", "Relatorios");
    }

    private String moduloPlaceholder(HttpSession session, Model model, String paginaAtiva, String titulo) {
        Integer utilizadorId = (Integer) session.getAttribute(WebSession.UTILIZADOR_ID);
        if (utilizadorId == null) {
            return "redirect:/web/login";
        }
        model.addAttribute("utilizadorNome", session.getAttribute(WebSession.UTILIZADOR_NOME));
        webLayoutService.aplicar(model, utilizadorId, paginaAtiva);
        model.addAttribute("tituloModulo", titulo);
        return "web/modulo-placeholder";
    }
}
