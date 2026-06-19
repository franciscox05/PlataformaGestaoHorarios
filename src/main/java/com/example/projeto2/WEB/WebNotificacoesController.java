package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Notificacao;
import com.example.projeto2.API.Services.NotificacaoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

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

        // Persistent split: "Ativas" = not archived, "Arquivadas" = archived
        List<Notificacao> ativas = notificacoes.stream().filter(n -> !n.isArquivada()).toList();
        List<Notificacao> arquivadas = notificacoes.stream().filter(Notificacao::isArquivada).toList();
        long totalNaoLidas = ativas.stream().filter(n -> !n.isLida()).count();

        model.addAttribute("notificacoes", notificacoes);
        model.addAttribute("ativas", ativas);
        model.addAttribute("arquivadas", arquivadas);
        model.addAttribute("totalNaoLidas", totalNaoLidas);

        // Mark only the current store's notifications as read — other stores' badges stay intact
        notificacaoBLL.marcarComoLidas(utilizadorId, idLoja);

        // preencherModeloBase runs after marcarComoLidas so totalNotificacoesPendentes = 0
        webAppService.preencherModeloBase(model, session, "notificacoes");

        return "web/notificacoes";
    }

    /**
     * Arquiva uma notificação de forma persistente (AJAX). Devolve JSON.
     * O serviço restringe a operação ao próprio utilizador (isolamento).
     */
    @PostMapping("/{id}/arquivar")
    @ResponseBody
    public ResponseEntity<?> arquivar(@PathVariable("id") Integer id, HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            boolean arquivada = notificacaoBLL.arquivarNotificacao(id, utilizadorId);
            if (!arquivada) {
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("erro", "Notificação não encontrada ou já arquivada."));
            }
            return ResponseEntity.ok(Map.of("mensagem", "Notificação arquivada."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Não foi possível arquivar a notificação."));
        }
    }

    /**
     * Restaura uma notificação arquivada para o estado ativo (AJAX). Devolve JSON.
     * Restrito ao próprio utilizador (isolamento). Suporta o "Desfazer" e o botão de desarquivar.
     */
    @PostMapping("/{id}/desarquivar")
    @ResponseBody
    public ResponseEntity<?> desarquivar(@PathVariable("id") Integer id, HttpSession session) {
        try {
            Integer utilizadorId = webAppService.obterUtilizadorIdObrigatorio(session);
            boolean restaurada = notificacaoBLL.desarquivarNotificacao(id, utilizadorId);
            if (!restaurada) {
                return ResponseEntity.unprocessableEntity()
                        .body(Map.of("erro", "Notificação não encontrada ou já ativa."));
            }
            return ResponseEntity.ok(Map.of("mensagem", "Notificação restaurada."));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(401).body(Map.of("erro", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("erro", "Não foi possível restaurar a notificação."));
        }
    }
}
