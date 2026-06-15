package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Notificacao;
import com.example.projeto2.API.Services.NotificacaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/web/notificacoes")
public class WebNotificacoesController {

    private final WebAppService webAppService;
    private final NotificacaoService notificacaoBLL;

    public WebNotificacoesController(WebAppService webAppService,
                                     NotificacaoService notificacaoBLL) {
        this.webAppService = webAppService;
        this.notificacaoBLL = notificacaoBLL;
    }

    @GetMapping
    public String pagina(HttpSession session, Model model) {
        Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
        Integer idLoja = webAppService.obterLojaAtual(session);

        // Load full history — in-memory objects capture lida state before the mark-as-read below
        List<Notificacao> notificacoes = notificacaoBLL.listarHistoricoNotificacoes(utilizadorId, idLoja);
        long totalNaoLidas = notificacoes.stream().filter(n -> !n.isLida()).count();
        model.addAttribute("notificacoes", notificacoes);
        model.addAttribute("totalNaoLidas", totalNaoLidas);

        // Mark only the current store's notifications as read — other stores' badges stay intact
        notificacaoBLL.marcarComoLidas(utilizadorId, idLoja);

        // preencherModeloBase runs after marcarComoLidas so totalNotificacoesPendentes = 0
        webAppService.preencherModeloBase(model, session, "notificacoes");

        return "web/notificacoes";
    }
}
