package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.HorarioEspecialLoja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HorarioEspecialLojaRepository extends JpaRepository<HorarioEspecialLoja, Integer> {

    @Query("SELECT h FROM HorarioEspecialLoja h " +
            "WHERE h.idLoja.id = :idLoja " +
            "ORDER BY h.dataInicio ASC, h.dataFim ASC, h.id DESC")
    List<HorarioEspecialLoja> findByIdLojaOrderByPeriodo(@Param("idLoja") Integer idLoja);

    @Query("SELECT h FROM HorarioEspecialLoja h " +
            "WHERE h.idLoja.id = :idLoja " +
            "AND h.dataInicio <= :dataFim " +
            "AND h.dataFim >= :dataInicio " +
            "ORDER BY h.dataInicio ASC, h.id ASC")
    List<HorarioEspecialLoja> findAtivosNoPeriodo(@Param("idLoja") Integer idLoja,
                                                  @Param("dataInicio") LocalDate dataInicio,
                                                  @Param("dataFim") LocalDate dataFim);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END " +
            "FROM HorarioEspecialLoja h " +
            "WHERE h.idLoja.id = :idLoja " +
            "AND h.dataInicio <= :dataFim " +
            "AND h.dataFim >= :dataInicio " +
            "AND (:idIgnorado IS NULL OR h.id <> :idIgnorado)")
    boolean existsConflitoDePeriodo(@Param("idLoja") Integer idLoja,
                                    @Param("dataInicio") LocalDate dataInicio,
                                    @Param("dataFim") LocalDate dataFim,
                                    @Param("idIgnorado") Integer idIgnorado);

    Optional<HorarioEspecialLoja> findByIdAndIdLojaId(Integer idHorarioEspecial, Integer idLoja);
}
