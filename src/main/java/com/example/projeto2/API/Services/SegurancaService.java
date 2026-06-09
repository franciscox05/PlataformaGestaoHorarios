package com.example.projeto2.API.Services;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SegurancaService {

    private static final int TAMANHO_MINIMO_PASSWORD = 6;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String normalizarEmailLogin(String email) {
        String emailNormalizado = normalizarTexto(email);
        return emailNormalizado == null ? null : emailNormalizado.toLowerCase(Locale.ROOT);
    }

    public String normalizarPasswordLogin(String password) {
        return normalizarTexto(password);
    }

    public String prepararPasswordParaPersistencia(String password, boolean obrigatoria) {
        String passwordNormalizada = normalizarTexto(password);

        if (passwordNormalizada == null) {
            if (obrigatoria) {
                throw new IllegalArgumentException("Indica uma password valida.");
            }
            return null;
        }

        validarPasswordNova(passwordNormalizada);
        return passwordEncoder.encode(passwordNormalizada);
    }

    public void validarPasswordNova(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Indica uma password valida.");
        }

        if (password.length() < TAMANHO_MINIMO_PASSWORD) {
            throw new IllegalArgumentException("A password deve ter pelo menos 6 caracteres.");
        }
    }

    public boolean passwordCorresponde(String passwordEmTexto, String passwordGuardada) {
        if (passwordEmTexto == null || passwordGuardada == null) {
            return false;
        }

        String passwordTextoNormalizada = passwordEmTexto.trim();
        String passwordGuardadaNormalizada = passwordGuardada.trim();

        if (passwordGuardadaNormalizada.isEmpty()) {
            return false;
        }

        if (pareceHashBCrypt(passwordGuardadaNormalizada)) {
            return passwordEncoder.matches(passwordTextoNormalizada, passwordGuardadaNormalizada);
        }

        return passwordGuardadaNormalizada.equals(passwordTextoNormalizada);
    }

    public boolean precisaMigrarParaHash(String passwordGuardada) {
        if (passwordGuardada == null) {
            return false;
        }

        String passwordNormalizada = passwordGuardada.trim();
        return !passwordNormalizada.isEmpty() && !pareceHashBCrypt(passwordNormalizada);
    }

    public String gerarHash(String passwordEmTexto) {
        String passwordNormalizada = normalizarTexto(passwordEmTexto);
        if (passwordNormalizada == null) {
            throw new IllegalArgumentException("Indica uma password valida.");
        }
        return passwordEncoder.encode(passwordNormalizada);
    }

    private boolean pareceHashBCrypt(String passwordGuardada) {
        return passwordGuardada.startsWith("$2a$")
                || passwordGuardada.startsWith("$2b$")
                || passwordGuardada.startsWith("$2y$");
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }
}
