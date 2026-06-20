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
    private Integer idLojaAtiva;

    public SessaoService() {
    }

    public synchronized void iniciarSessao(Utilizador utilizador) {
        // Preserva a loja ativa: definirLojaAtiva() é chamado pelo LoginController
        // ou SelecionarLojaController ANTES de DashboardController.setUtilizadorLogado()
        // chamar este método — limparSessaoInterna() não deve apagá-la.
        Integer lojaAtiva = this.idLojaAtiva;
        limparSessaoInterna();
        this.idLojaAtiva = lojaAtiva;

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

    /**
     * Tranca a loja ativa escolhida pelo utilizador para o resto da sessão
     * Desktop — equivalente direto ao {@code WebSession.LOJA_ID} usado pela
     * Web. Ver Revisao.md, ponto 17 (Fase 1).
     */
    public synchronized void definirLojaAtiva(Integer idLoja) {
        this.idLojaAtiva = idLoja;
    }

    public synchronized Integer obterLojaAtiva() {
        return idLojaAtiva;
    }

    private void limparSessaoInterna() {
        this.utilizadorAutenticado = null;
        this.ultimaAtividade = null;
        this.identificadorSessao = null;
        this.idLojaAtiva = null;
    }
}
