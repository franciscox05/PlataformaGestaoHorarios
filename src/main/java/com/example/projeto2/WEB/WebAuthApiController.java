package com.example.projeto2.WEB;

import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Services.UtilizadorService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST API de autenticação por sessão HTTP.
 *
 * POST /api/auth/login   → autentica e cria sessão; retorna info do utilizador
 * POST /api/auth/logout  → invalida a sessão atual
 */
@RestController
@RequestMapping("/api/auth")
public class WebAuthApiController {

    private final UtilizadorService utilizadorBLL;
    private final WebAppService webAppService;

    public WebAuthApiController(UtilizadorService utilizadorBLL,
                                WebAppService webAppService) {
        this.utilizadorBLL = utilizadorBLL;
        this.webAppService = webAppService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> corpo,
                                   HttpSession session) {
        String email    = corpo.get("email");
        String password = corpo.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erro", "email e password sao obrigatorios"));
        }

        try {
            Utilizador utilizador = utilizadorBLL.efetuarLogin(email, password);
            if (utilizador == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("erro", "Credenciais invalidas"));
            }

            webAppService.sincronizarSessao(session, utilizador);

            return ResponseEntity.ok(Map.of(
                    "id",    utilizador.getId(),
                    "nome",  utilizador.getNome(),
                    "email", utilizador.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(Map.of("erro", "Credenciais invalidas"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("mensagem", "Sessao terminada"));
    }
}
