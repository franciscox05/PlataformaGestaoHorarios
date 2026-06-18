package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.EventoAuditoria;
import com.example.projeto2.API.Repositories.EventoAuditoriaRepository;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regista decisões de gestão (aprovações, rejeições) na tabela eventos_auditoria
 * para rastreabilidade e conformidade.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final EventoAuditoriaRepository repo;
    private final UtilizadorRepository utilizadorRepository;

    public AuditoriaService(EventoAuditoriaRepository repo,
                            UtilizadorRepository utilizadorRepository) {
        this.repo = repo;
        this.utilizadorRepository = utilizadorRepository;
    }

    /**
     * Regista um evento de auditoria. Corre na mesma transação do chamador (REQUIRED)
     * para evitar conflitos de lock em contextos de teste transacional.
     */
    @Transactional
    public void registar(String tipoEvento, Integer idAprovador, String detalhes) {
        try {
            String email = null;
            if (idAprovador != null) {
                email = utilizadorRepository.findById(idAprovador)
                        .map(u -> u.getEmail())
                        .orElse(null);
            }
            EventoAuditoria evento = new EventoAuditoria(tipoEvento, "sucesso", idAprovador, email, detalhes);
            repo.save(evento);
        } catch (Exception e) {
            log.warn("Nao foi possivel registar evento de auditoria '{}': {}", tipoEvento, e.getMessage());
        }
    }
}
