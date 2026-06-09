package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Utilizador;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessaoService {

    private static final Duration TEMPO_MAXIMO_INATIVIDADE = Duration.ofMinutes(15);

    private Utilizador utilizadorAutenticado;
    private Instant ultimaAtividade;
    private String identificadorSessao;

    public SessaoService() {
    }

    public synchronized void iniciarSessao(Utilizador utilizador) {
        limparSessaoInterna();

        if (utilizador == null || utilizador.getId() == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        this.utilizadorAutenticado = utilizador;
        this.ultimaAtividade = Instant.now();
        this.identificadorSessao = UUID.randomUUID().toString();
    }

    public synchronized void registarAtividade() {
        if (!temSessaoAtiva()) {
            return;
        }

        this.ultimaAtividade = Instant.now();
    }

    public synchronized void terminarSessaoManual() {
        if (!temSessaoAtiva()) {
            return;
        }

        limparSessaoInterna();
    }

    public synchronized void expirarSessao() {
        if (!temSessaoAtiva()) {
            return;
        }

        limparSessaoInterna();
    }

    public synchronized boolean temSessaoAtiva() {
        return utilizadorAutenticado != null && identificadorSessao != null && ultimaAtividade != null;
    }

    public Duration obterTempoMaximoInatividade() {
        return TEMPO_MAXIMO_INATIVIDADE;
    }

    public synchronized String obterIdentificadorSessao() {
        return identificadorSessao;
    }

    public synchronized Utilizador obterUtilizadorAutenticado() {
        return utilizadorAutenticado;
    }

    private void limparSessaoInterna() {
        this.utilizadorAutenticado = null;
        this.ultimaAtividade = null;
        this.identificadorSessao = null;
    }
}
