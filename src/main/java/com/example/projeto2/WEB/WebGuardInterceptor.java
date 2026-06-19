package com.example.projeto2.WEB;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class WebGuardInterceptor implements HandlerInterceptor {

    private final WebAppService webAppService;

    public WebGuardInterceptor(WebAppService webAppService) {
        this.webAppService = webAppService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/web")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Integer idUtilizador = webAppService.obterUtilizadorId(session);
        if (idUtilizador == null) {
            response.sendRedirect("/web/login");
            return false;
        }

        Integer idLoja = webAppService.obterLojaAtual(session);
        if (!podeAcederAoModulo(path, idUtilizador, idLoja)) {
            response.sendRedirect("/web/painel?acessoNegado=true");
            return false;
        }

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        return true;
    }

    private boolean podeAcederAoModulo(String path, Integer idUtilizador, Integer idLoja) {
        // Gestao de loja e relatorios sao modulos administrativos exclusivos da Aplicacao
        // Desktop; o portal web so os expoe como placeholders, mas o acesso direto por URL
        // tem de ficar restrito ao mesmo cargo (gerente/subgerente) que ja governa estas
        // capacidades no resto da aplicacao.
        if (path.startsWith("/web/modulos/gestao-loja")) {
            return webAppService.obterPermissoes(idUtilizador, idLoja).podeGerirLoja();
        }

        if (path.startsWith("/web/modulos/relatorios")) {
            return webAppService.obterPermissoes(idUtilizador, idLoja).podeVerRelatorios();
        }

        return true;
    }
}
