package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UtilizadorBLL {

    @Autowired
    private UtilizadorRepository repository;

    public Utilizador efetuarLogin(String email, String password) {
        System.out.println("BLL: A tentar login para: " + email);

        List<Utilizador> todos = repository.findAll();

        for (Utilizador u : todos) {
            // Debug: Ver o que existe na BD
            // System.out.println("Comparando com: " + u.getEmail());

            if (u.getEmail().equalsIgnoreCase(email.trim())) {
                System.out.println("BLL: Utilizador encontrado! A validar password...");

                // O .trim() remove espaços invisíveis que o pgAdmin às vezes mete
                if (u.getPasswordHash().trim().equals(password.trim())) {

                    String estadoAtual = u.getEstado().toString();
                    System.out.println("BLL: Password correta. Estado: " + estadoAtual);

                    if ("ativo".equalsIgnoreCase(estadoAtual)) {
                        return u;
                    } else {
                        System.out.println("BLL: Bloqueado! O estado não é 'ativo'.");
                    }
                } else {
                    System.out.println("BLL: Password errada! Recebi [" + password + "] mas na BD está [" + u.getPasswordHash() + "]");
                }
            }
        }
        return null;
    }
}