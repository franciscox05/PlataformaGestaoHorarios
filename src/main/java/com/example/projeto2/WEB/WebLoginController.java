package com.example.projeto2.WEB;

import com.example.projeto2.BLL.UtilizadorBLL;
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
@RequestMapping("/web")
public class WebLoginController {

    private final UtilizadorBLL utilizadorBLL;

    public WebLoginController(UtilizadorBLL utilizadorBLL) {
        this.utilizadorBLL = utilizadorBLL;
    }

    @GetMapping({"", "/", "/login"})
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute(WebSession.UTILIZADOR_ID) != null) {
            return "redirect:/web/horarios";
        }

        if (!model.containsAttribute("erro")) {
            model.addAttribute("erro", null);
        }
        return "web/login";
    }

    @PostMapping("/login")
    public String autenticar(@RequestParam("email") String email,
                             @RequestParam("password") String password,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Utilizador utilizador = utilizadorBLL.efetuarLogin(email, password);
        if (utilizador == null) {
            redirectAttributes.addFlashAttribute("erro", "Email ou palavra-passe incorretos.");
            return "redirect:/web/login";
        }

        session.setAttribute(WebSession.UTILIZADOR_ID, utilizador.getId());
        session.setAttribute(WebSession.UTILIZADOR_NOME, utilizador.getNome());
        return "redirect:/web/horarios";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/web/login";
    }
}
