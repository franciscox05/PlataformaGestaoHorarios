package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Notificacao;
import com.example.projeto2.API.Repositories.NotificacaoRepository;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final UtilizadorRepository utilizadorRepository;

    public NotificacaoService(NotificacaoRepository notificacaoRepository,
                              UtilizadorRepository utilizadorRepository) {
        this.notificacaoRepository = notificacaoRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    public List<Notificacao> listarNotificacoesPendentes(Integer utilizadorId) {
        return notificacaoRepository.findPendentes(utilizadorId);
    }

    public List<Notificacao> listarHistoricoNotificacoes(Integer utilizadorId) {
        return notificacaoRepository.findTop50ByIdUtilizadorIdOrderByDataEnvioDesc(utilizadorId);
    }

    /** Store-scoped variant — strict equality; only notifications stamped for this store. Null falls back to global list. */
    public List<Notificacao> listarHistoricoNotificacoes(Integer utilizadorId, Integer idLoja) {
        if (idLoja == null) return listarHistoricoNotificacoes(utilizadorId);
        return notificacaoRepository.findHistoricoByUtilizadorELoja(utilizadorId, idLoja);
    }

    public long contarNotificacoesPendentes(Integer utilizadorId) {
        return notificacaoRepository.countPendentes(utilizadorId);
    }

    /** Store-scoped badge count — strict equality; null-stamped notifications are excluded. */
    public long contarNotificacoesPendentes(Integer utilizadorId, Integer idLoja) {
        if (idLoja == null) return contarNotificacoesPendentes(utilizadorId);
        return notificacaoRepository.countPendentesPorLoja(utilizadorId, idLoja);
    }

    @Transactional
    public void marcarComoLidas(Integer utilizadorId) {
        notificacaoRepository.marcarTodasComoLidas(utilizadorId);
    }

    /** Store-scoped mark-read — does not affect other stores' unread notifications. */
    @Transactional
    public void marcarComoLidas(Integer utilizadorId, Integer idLoja) {
        if (idLoja == null) {
            marcarComoLidas(utilizadorId);
            return;
        }
        notificacaoRepository.marcarTodasComoLidasPorLoja(utilizadorId, idLoja);
    }

    /**
     * Arquiva permanentemente uma notificação do próprio utilizador.
     * O filtro por idUtilizador garante o isolamento — ninguém arquiva notificações de outrem.
     * @return true se uma linha foi efetivamente arquivada.
     */
    @Transactional
    public boolean arquivarNotificacao(Integer id, Integer utilizadorId) {
        if (id == null || utilizadorId == null) return false;
        return notificacaoRepository.arquivar(id, utilizadorId) > 0;
    }

    /**
     * Restaura uma notificação arquivada do próprio utilizador para o estado ativo.
     * O filtro por idUtilizador garante o isolamento.
     * @return true se uma linha foi efetivamente restaurada.
     */
    @Transactional
    public boolean desarquivarNotificacao(Integer id, Integer utilizadorId) {
        if (id == null || utilizadorId == null) return false;
        return notificacaoRepository.desarquivar(id, utilizadorId) > 0;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void criarNotificacao(Integer utilizadorId, String mensagem) {
        criarNotificacao(utilizadorId, mensagem, null);
    }

    /** Store-stamped notification — idLoja null creates a global notification visible in all stores. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void criarNotificacao(Integer utilizadorId, String mensagem, Integer idLoja) {
        if (utilizadorId == null || mensagem == null || mensagem.isBlank()) return;
        Notificacao notificacao = new Notificacao();
        // getReferenceById returns a session-managed Hibernate proxy — no SELECT fired,
        // just a valid FK reference for the INSERT.
        notificacao.setIdUtilizador(utilizadorRepository.getReferenceById(utilizadorId));
        notificacao.setMensagem(mensagem);
        notificacao.setLida(false);
        notificacao.setDataEnvio(LocalDateTime.now());
        notificacao.setIdLoja(idLoja);
        notificacaoRepository.save(notificacao);
    }
}
