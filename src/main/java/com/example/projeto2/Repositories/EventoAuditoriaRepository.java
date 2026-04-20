package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Integer> {
}
