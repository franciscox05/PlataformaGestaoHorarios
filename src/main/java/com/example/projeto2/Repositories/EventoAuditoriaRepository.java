package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Integer> {

    List<EventoAuditoria> findAllByOrderByDataEventoDesc(Pageable pageable);
}
