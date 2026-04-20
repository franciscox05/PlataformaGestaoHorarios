package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Preferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PreferenciaRepository extends JpaRepository<Preferencia, Integer> {

    List<Preferencia> findByIdUtilizadorIdOrderByDataInicioAscIdDesc(Integer idUtilizador);

    Optional<Preferencia> findByIdAndIdUtilizadorId(Integer idPreferencia, Integer idUtilizador);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Preferencia p " +
            "WHERE p.idUtilizador.id = :idUtilizador " +
            "AND LOWER(p.tipo) = LOWER(:tipo) " +
            "AND LOWER(p.descricao) = LOWER(:descricao) " +
            "AND p.prioridade = :prioridade " +
            "AND ((:dataInicio IS NULL AND p.dataInicio IS NULL) OR p.dataInicio = :dataInicio) " +
            "AND ((:dataFim IS NULL AND p.dataFim IS NULL) OR p.dataFim = :dataFim) " +
            "AND (:idIgnorado IS NULL OR p.id <> :idIgnorado)")
    boolean existsPreferenciaDuplicada(@Param("idUtilizador") Integer idUtilizador,
                                       @Param("tipo") String tipo,
                                       @Param("descricao") String descricao,
                                       @Param("prioridade") Integer prioridade,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       @Param("idIgnorado") Integer idIgnorado);
}
