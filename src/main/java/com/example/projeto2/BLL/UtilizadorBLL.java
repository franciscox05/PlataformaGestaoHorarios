package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;

@Service
public class UtilizadorBLL {

    private final UtilizadorRepository repository;

    public UtilizadorBLL(UtilizadorRepository repository) {
        this.repository = repository;
    }

    public Utilizador efetuarLogin(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        return repository.findByEmailIgnoreCase(email.trim())
                .filter(utilizador -> utilizador.getPasswordHash() != null)
                .filter(utilizador -> utilizador.getPasswordHash().trim().equals(password.trim()))
                .filter(utilizador -> utilizador.getEstado() != null)
                .filter(utilizador -> "ativo".equalsIgnoreCase(utilizador.getEstado().toString()))
                .orElse(null);
    }
}
