package com.example.projeto2.BLL;

import com.example.projeto2.Modules.EventoAuditoria;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.EventoAuditoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuditoriaBLL {

    private final EventoAuditoriaRepository eventoAuditoriaRepository;

    public AuditoriaBLL(EventoAuditoriaRepository eventoAuditoriaRepository) {
        this.eventoAuditoriaRepository = eventoAuditoriaRepository;
    }

    @Transactional
    public void registarFalhaLogin(String emailReferencia, String detalhes) {
        registarEvento("login", "falha", null, emailReferencia, null, "autenticacao", detalhes);
    }

    @Transactional
    public void registarLoginSucesso(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "login",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "autenticacao",
                "Autenticacao concluida com sucesso."
        );
    }

    @Transactional
    public void registarLogout(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "logout",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "sessao",
                "Sessao terminada manualmente."
        );
    }

    @Transactional
    public void registarSessaoExpirada(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "sessao_expirada",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "sessao",
                "Sessao terminada por inatividade."
        );
    }

    @Transactional
    public void registarAlteracaoPassword(Utilizador utilizador, String identificadorSessao) {
        registarEvento(
                "alteracao_password",
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                "perfil",
                "Password atualizada com sucesso."
        );
    }

    @Transactional
    public void registarEventoSensivel(String tipoEvento,
                                       Utilizador utilizador,
                                       String identificadorSessao,
                                       String origem,
                                       String detalhes) {
        registarEvento(
                tipoEvento,
                "sucesso",
                utilizador,
                utilizador != null ? utilizador.getEmail() : null,
                identificadorSessao,
                origem,
                detalhes
        );
    }

    private void registarEvento(String tipoEvento,
                                String resultado,
                                Utilizador utilizador,
                                String emailReferencia,
                                String identificadorSessao,
                                String origem,
                                String detalhes) {
        EventoAuditoria evento = new EventoAuditoria();
        evento.setTipoEvento(normalizar(valorOuPadrao(tipoEvento, "evento")));
        evento.setResultado(normalizar(valorOuPadrao(resultado, "sucesso")));
        evento.setOrigem(normalizar(valorOuPadrao(origem, "sistema")));
        evento.setIdUtilizador(utilizador);
        evento.setEmailReferencia(normalizarEmail(emailReferencia));
        evento.setIdentificadorSessao(limparTexto(identificadorSessao));
        evento.setDataEvento(Instant.now());
        evento.setDetalhes(limparTexto(detalhes));
        eventoAuditoriaRepository.save(evento);
    }

    private String valorOuPadrao(String valor, String valorPadrao) {
        String valorLimpo = limparTexto(valor);
        return valorLimpo == null ? valorPadrao : valorLimpo;
    }

    private String normalizar(String valor) {
        return valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarEmail(String emailReferencia) {
        String emailNormalizado = limparTexto(emailReferencia);
        return emailNormalizado == null ? null : emailNormalizado.toLowerCase(Locale.ROOT);
    }

    private String limparTexto(String valor) {
        if (valor == null) {
            return null;
        }

        String valorNormalizado = valor.trim();
        return valorNormalizado.isEmpty() ? null : valorNormalizado;
    }
}
