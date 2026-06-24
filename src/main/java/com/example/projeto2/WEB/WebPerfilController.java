package com.example.projeto2.WEB;

import com.example.projeto2.API.Services.PerfilService;
import com.example.projeto2.API.Modules.Utilizador;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/web/perfil")
public class WebPerfilController {

    private final WebAppService webAppService;
    private final PerfilService perfilBLL;

    public WebPerfilController(WebAppService webAppService,
                               PerfilService perfilBLL) {
        this.webAppService = webAppService;
        this.perfilBLL = perfilBLL;
    }

    @GetMapping
    public String perfil(HttpSession session, Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        webAppService.preencherModeloBase(model, session, "perfil");

        try {
            Integer idLoja = webAppService.obterLojaAtual(session);
            Utilizador utilizador = perfilBLL.obterUtilizadorPorId(utilizadorId);
            PerfilService.PerfilResumo resumo = perfilBLL.obterResumoPerfil(utilizador, idLoja);
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
            redirectAttributes.addFlashAttribute("sucesso", "Telemóvel atualizado com sucesso.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/web/perfil";
    }

    @PostMapping("/foto")
    public String atualizarFoto(@RequestParam("foto") MultipartFile foto,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            if (foto == null || foto.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Nenhuma foto selecionada.");
                return "redirect:/web/perfil";
            }
            if (foto.getSize() > 2L * 1024 * 1024) {
                redirectAttributes.addFlashAttribute("erro", "A foto deve ter menos de 2 MB.");
                return "redirect:/web/perfil";
            }
            String base64 = java.util.Base64.getEncoder().encodeToString(foto.getBytes());
            Utilizador utilizadorAtualizado = perfilBLL.atualizarFotoPerfil(utilizadorId, base64);
            webAppService.sincronizarSessao(session, utilizadorAtualizado);
            redirectAttributes.addFlashAttribute("sucesso", "Foto de perfil atualizada.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao guardar a foto: " + ex.getMessage());
        }
        return "redirect:/web/perfil";
    }

    @PostMapping("/foto/remover")
    public String removerFoto(HttpSession session, RedirectAttributes redirectAttributes) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        try {
            Utilizador utilizadorAtualizado = perfilBLL.atualizarFotoPerfil(utilizadorId, null);
            webAppService.sincronizarSessao(session, utilizadorAtualizado);
            redirectAttributes.addFlashAttribute("sucesso", "Foto de perfil removida.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover a foto: " + ex.getMessage());
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
