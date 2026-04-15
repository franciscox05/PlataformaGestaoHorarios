package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Permuta;
import org.springframework.data.jpa.repository.JpaRepository; // <-- Import atualizado

public interface PermutaRepository extends JpaRepository<Permuta, Integer> {
}