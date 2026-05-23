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

        if (!podeAcederAoModulo(path, idUtilizador)) {
            response.sendRedirect("/web/painel?acessoNegado=true");
            return false;
        }

        return true;
    }

    private boolean podeAcederAoModulo(String path, Integer idUtilizador) {
        if (path.startsWith("/web/gestao-loja")) {
            return webAppService.obterPermissoes(idUtilizador).podeGerirLoja();
        }

        if (path.startsWith("/web/relatorios")) {
            return webAppService.obterPermissoes(idUtilizador).podeVerRelatorios();
        }

        return true;
    }
}
