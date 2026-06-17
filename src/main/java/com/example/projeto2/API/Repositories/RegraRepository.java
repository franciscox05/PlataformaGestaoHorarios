package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Regra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegraRepository extends JpaRepository<Regra, Integer> {

    List<Regra> findAllByOrderByDescricaoAsc();

    @Query("SELECT r FROM Regra r WHERE (r.idLojaPrivada IS NULL OR r.idLojaPrivada.id = :idLoja) ORDER BY r.descricao ASC")
    List<Regra> findGlobaisEPrivadasPorLoja(@Param("idLoja") Integer idLoja);
}
