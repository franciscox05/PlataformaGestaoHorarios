package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, Integer> {

    Optional<Utilizador> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelemovel(String telemovel);

    boolean existsByTelemovelAndIdNot(String telemovel, Integer id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
