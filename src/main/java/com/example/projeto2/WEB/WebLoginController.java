package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Services.UtilizadorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/web")
public class WebLoginController {

    /** Session-safe DTO — no Hibernate proxies, no lazy-init risk. */
    public record LojaInfo(Integer id, String nome, String localizacao) {}

    private static final String SESSION_LOJAS_DISPONIVEIS = "webLojasDisponiveis";

    private final UtilizadorService utilizadorBLL;
    private final WebAppService webAppService;
    private final LojautilizadorRepository lojautilizadorRepository;

    public WebLoginController(UtilizadorService utilizadorBLL,
                              WebAppService webAppService,
                              LojautilizadorRepository lojautilizadorRepository) {
        this.utilizadorBLL = utilizadorBLL;
        this.webAppService = webAppService;
        this.lojautilizadorRepository = lojautilizadorRepository;
    }

    @GetMapping({"", "/", "/login"})
    public String loginPage(HttpSession session, Model model) {
        // Already authenticated — bounce to the store switcher (single-store users auto-forward to /horarios)
        if (session.getAttribute(WebSession.UTILIZADOR_ID) != null) {
            return "redirect:/web/selecionar-loja";
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

        webAppService.sincronizarSessao(session, utilizador);

        List<Lojautilizador> ligacoes = lojautilizadorRepository
                .findLigacoesAtivasByIdUtilizador(utilizador.getId());

        if (ligacoes.size() == 1) {
            session.setAttribute(WebSession.LOJA_ID, ligacoes.get(0).getIdLoja().getId());
            return "redirect:/web/horarios";
        }

        if (ligacoes.size() > 1) {
            List<LojaInfo> lojas = ligacoes.stream()
                    .map(lu -> new LojaInfo(
                            lu.getIdLoja().getId(),
                            lu.getIdLoja().getNome(),
                            lu.getIdLoja().getLocalizacao()))
                    .toList();
            session.setAttribute(SESSION_LOJAS_DISPONIVEIS, lojas);
            return "redirect:/web/selecionar-loja";
        }

        return "redirect:/web/horarios";
    }

    @GetMapping("/selecionar-loja")
    public String selecionarLojaPage(HttpSession session, Model model) {
        Integer idUtilizador = webAppService.obterUtilizadorId(session);
        if (idUtilizador == null) {
            return "redirect:/web/login";
        }

        @SuppressWarnings("unchecked")
        List<LojaInfo> lojas = (List<LojaInfo>) session.getAttribute(SESSION_LOJAS_DISPONIVEIS);

        if (lojas == null || lojas.isEmpty()) {
            lojas = lojautilizadorRepository
                    .findLigacoesAtivasByIdUtilizador(idUtilizador)
                    .stream()
                    .map(lu -> new LojaInfo(
                            lu.getIdLoja().getId(),
                            lu.getIdLoja().getNome(),
                            lu.getIdLoja().getLocalizacao()))
                    .toList();
        }

        if (lojas.size() == 1) {
            session.setAttribute(WebSession.LOJA_ID, lojas.get(0).id());
            session.removeAttribute(SESSION_LOJAS_DISPONIVEIS);
            return "redirect:/web/horarios";
        }

        model.addAttribute("lojas", lojas);
        model.addAttribute("nomeUtilizador", session.getAttribute(WebSession.UTILIZADOR_NOME));
        return "web/selecionar-loja";
    }

    @PostMapping("/selecionar-loja/escolher")
    public String escolherLoja(@RequestParam("idLoja") Integer idLoja,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Integer idUtilizador = webAppService.obterUtilizadorId(session);
        if (idUtilizador == null) {
            return "redirect:/web/login";
        }

        boolean pertence = lojautilizadorRepository
                .findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja)
                .isPresent();

        if (!pertence) {
            redirectAttributes.addFlashAttribute("erro", "Loja inválida.");
            return "redirect:/web/selecionar-loja";
        }

        session.setAttribute(WebSession.LOJA_ID, idLoja);
        session.removeAttribute(SESSION_LOJAS_DISPONIVEIS);
        return "redirect:/web/horarios";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/web/login";
    }
}
