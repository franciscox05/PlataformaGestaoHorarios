package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.RegrasLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegrasLojaRepository extends JpaRepository<RegrasLoja, Integer> {

    @Query("SELECT rl FROM RegrasLoja rl " +
            "JOIN FETCH rl.idRegra r " +
            "WHERE rl.idLoja.id = :idLoja " +
            "ORDER BY r.descricao ASC")
    List<RegrasLoja> findByIdLojaWithRegraOrderByDescricao(@Param("idLoja") Integer idLoja);

    Optional<RegrasLoja> findByIdLojaIdAndIdRegraId(Integer idLoja, Integer idRegra);
}
