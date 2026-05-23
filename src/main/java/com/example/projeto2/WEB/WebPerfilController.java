package com.example.projeto2.WEB;

import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.Modules.Utilizador;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/perfil")
public class WebPerfilController {

    private final WebAppService webAppService;
    private final PerfilBLL perfilBLL;

    public WebPerfilController(WebAppService webAppService,
                               PerfilBLL perfilBLL) {
        this.webAppService = webAppService;
        this.perfilBLL = perfilBLL;
    }

    @GetMapping
    public String perfil(HttpSession session, Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "perfil");

        try {
            Utilizador utilizador = perfilBLL.obterUtilizadorPorId(utilizadorId);
            PerfilBLL.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizador);
            model.addAttribute("resumo", resumo);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("erro", ex.getMessage());
        }

        return "web/perfil";
    }

    @PostMapping("/nome")
    public String atualizarNome(@RequestParam("nome") String nome,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            Utilizador utilizadorAtualizado = perfilBLL.atualizarNome(utilizadorId, nome);
            webAppService.sincronizarSessao(session, utilizadorAtualizado);
            redirectAttributes.addFlashAttribute("sucesso", "Nome atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/perfil";
    }

    @PostMapping("/email")
    public String atualizarEmail(@RequestParam("email") String email,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            Utilizador utilizadorAtualizado = perfilBLL.atualizarEmail(utilizadorId, email);
            webAppService.sincronizarSessao(session, utilizadorAtualizado);
            redirectAttributes.addFlashAttribute("sucesso", "Email atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/perfil";
    }

    @PostMapping("/telemovel")
    public String atualizarTelemovel(@RequestParam("telemovel") String telemovel,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            perfilBLL.atualizarTelemovel(utilizadorId, telemovel);
            redirectAttributes.addFlashAttribute("sucesso", "Telemovel atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/perfil";
    }

    @PostMapping("/password")
    public String atualizarPassword(@RequestParam("passwordAtual") String passwordAtual,
                                    @RequestParam("novaPassword") String novaPassword,
                                    @RequestParam("confirmarPassword") String confirmarPassword,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            perfilBLL.atualizarPassword(utilizadorId, passwordAtual, novaPassword, confirmarPassword);
            redirectAttributes.addFlashAttribute("sucesso", "Palavra-passe atualizada com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/perfil";
    }
}
