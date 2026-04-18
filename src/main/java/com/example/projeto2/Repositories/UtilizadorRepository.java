package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, Integer> {
    // Aqui não precisas de escrever código nenhum!
    // O Spring já sabe fazer SELECT, INSERT, UPDATE e DELETE sozinho.
    boolean existsByTelemovel(String telemovel);
}