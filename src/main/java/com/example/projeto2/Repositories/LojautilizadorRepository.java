package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Lojautilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LojautilizadorRepository extends JpaRepository<Lojautilizador, Integer> {

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE lu.idUtilizador.id = :idUtilizador AND lu.dataFim IS NULL")
    Optional<Lojautilizador> findLigacaoAtivaByIdUtilizador(@Param("idUtilizador") Integer idUtilizador);
}
