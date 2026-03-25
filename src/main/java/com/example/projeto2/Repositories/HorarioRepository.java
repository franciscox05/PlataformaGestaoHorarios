package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Horario;
import org.springframework.data.jpa.repository.JpaRepository; // Mudámos o import

// Mudámos de CrudRepository para JpaRepository
public interface HorarioRepository extends JpaRepository<Horario, Integer> {
}