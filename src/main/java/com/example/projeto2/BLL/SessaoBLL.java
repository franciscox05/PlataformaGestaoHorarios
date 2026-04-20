package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Utilizador;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class SessaoBLL {

    private static final Duration TEMPO_MAXIMO_INATIVIDADE = Duration.ofMinutes(15);

    private final AuditoriaBLL auditoriaBLL;
    private Utilizador utilizadorAutenticado;
    private Instant ultimaAtividade;
    private String identificadorSessao;

    public SessaoBLL(AuditoriaBLL auditoriaBLL) {
        this.auditoriaBLL = auditoriaBLL;
    }

    public synchronized void iniciarSessao(Utilizador utilizador) {
        limparSessaoInterna();

        if (utilizador == null || utilizador.getId() == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        this.utilizadorAutenticado = utilizador;
        this.ultimaAtividade = Instant.now();
        this.identificadorSessao = UUID.randomUUID().toString();
        auditoriaBLL.registarLoginSucesso(utilizador, identificadorSessao);
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

        auditoriaBLL.registarLogout(utilizadorAutenticado, identificadorSessao);
        limparSessaoInterna();
    }

    public synchronized void expirarSessao() {
        if (!temSessaoAtiva()) {
            return;
        }

        auditoriaBLL.registarSessaoExpirada(utilizadorAutenticado, identificadorSessao);
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
