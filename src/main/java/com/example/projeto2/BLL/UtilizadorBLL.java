package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilizadorBLL {

    private final UtilizadorRepository utilizadorRepository;
    private final SegurancaBLL segurancaBLL;
    private final AuditoriaBLL auditoriaBLL;

    public UtilizadorBLL(UtilizadorRepository utilizadorRepository,
                         SegurancaBLL segurancaBLL,
                         AuditoriaBLL auditoriaBLL) {
        this.utilizadorRepository = utilizadorRepository;
        this.segurancaBLL = segurancaBLL;
        this.auditoriaBLL = auditoriaBLL;
    }

    @Transactional
    public Utilizador efetuarLogin(String email, String password) {
        String emailNormalizado = segurancaBLL.normalizarEmailLogin(email);
        String passwordNormalizada = segurancaBLL.normalizarPasswordLogin(password);

        if (emailNormalizado == null || passwordNormalizada == null) {
            return null;
        }

        Utilizador utilizador = utilizadorRepository.findByEmailIgnoreCase(emailNormalizado)
                .orElse(null);

        if (utilizador == null) {
            auditoriaBLL.registarFalhaLogin(emailNormalizado, "Credenciais invalidas.");
            return null;
        }

        if (!segurancaBLL.passwordCorresponde(passwordNormalizada, utilizador.getPasswordHash())) {
            auditoriaBLL.registarFalhaLogin(emailNormalizado, "Credenciais invalidas.");
            return null;
        }

        if (utilizador.getEstado() == null || !"ativo".equalsIgnoreCase(utilizador.getEstado())) {
            auditoriaBLL.registarFalhaLogin(emailNormalizado, "Conta sem permissao para autenticar.");
            return null;
        }

        if (segurancaBLL.precisaMigrarParaHash(utilizador.getPasswordHash())) {
            utilizador.setPasswordHash(segurancaBLL.gerarHash(passwordNormalizada));
            utilizador = utilizadorRepository.save(utilizador);
        }

        return utilizador;
    }
}
