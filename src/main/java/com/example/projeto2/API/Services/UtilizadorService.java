package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoUtilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UtilizadorService {

    private final UtilizadorRepository utilizadorRepository;
    private final SegurancaService segurancaBLL;

    public UtilizadorService(UtilizadorRepository utilizadorRepository,
                         SegurancaService segurancaBLL) {
        this.utilizadorRepository = utilizadorRepository;
        this.segurancaBLL = segurancaBLL;
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
            return null;
        }

        if (!segurancaBLL.passwordCorresponde(passwordNormalizada, utilizador.getPasswordHash())) {
            return null;
        }

        if (utilizador.getEstado() == null || utilizador.getEstado() != EstadoUtilizador.ativo) {
            return null;
        }

        if (segurancaBLL.precisaMigrarParaHash(utilizador.getPasswordHash())) {
            utilizador.setPasswordHash(segurancaBLL.gerarHash(passwordNormalizada));
            utilizador = utilizadorRepository.save(utilizador);
        }

        return utilizador;
    }
}
