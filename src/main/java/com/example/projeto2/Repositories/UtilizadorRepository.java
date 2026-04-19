package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UtilizadorRepository extends JpaRepository<Utilizador, Integer> {

    boolean existsByTelemovel(String telemovel);

    boolean existsByTelemovelAndIdNot(String telemovel, Integer id);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);
}
