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

    @Query("SELECT p " +
            "FROM Preferencia p " +
            "JOIN FETCH p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "JOIN FETCH lu.idLoja l " +
            "LEFT JOIN FETCH p.idDecisor d " +
            "WHERE l.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND LOWER(p.estado) = 'pendente' " +
            "AND u.id <> :idUtilizadorAprovador " +
            "ORDER BY CASE WHEN p.dataInicio IS NULL THEN 1 ELSE 0 END, p.dataInicio ASC, p.prioridade DESC, p.id DESC")
    List<Preferencia> findPreferenciasPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                                      @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador);

    @Query("SELECT p " +
            "FROM Preferencia p " +
            "JOIN FETCH p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "JOIN FETCH lu.idLoja l " +
            "LEFT JOIN FETCH p.idDecisor d " +
            "WHERE l.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND LOWER(p.estado) <> 'pendente' " +
            "ORDER BY p.dataDecisao DESC, p.id DESC")
    List<Preferencia> findHistoricoDecisoesDaLoja(@Param("idLoja") Integer idLoja);

    @Query("SELECT p " +
            "FROM Preferencia p " +
            "JOIN FETCH p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "JOIN FETCH lu.idLoja l " +
            "LEFT JOIN FETCH p.idDecisor d " +
            "WHERE l.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND LOWER(p.estado) = 'pendente' " +
            "AND u.id <> :idUtilizadorAprovador " +
            "AND ((p.dataInicio IS NULL) OR p.dataInicio <= :dataFim) " +
            "AND ((p.dataFim IS NULL) OR p.dataFim >= :dataInicio) " +
            "ORDER BY CASE WHEN p.dataInicio IS NULL THEN 1 ELSE 0 END, p.dataInicio ASC, p.prioridade DESC, p.id DESC")
    List<Preferencia> findPreferenciasPendentesRelevantesDaLoja(@Param("idLoja") Integer idLoja,
                                                                @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador,
                                                                @Param("dataInicio") LocalDate dataInicio,
                                                                @Param("dataFim") LocalDate dataFim);

    @Query("SELECT p " +
            "FROM Preferencia p " +
            "JOIN FETCH p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "JOIN FETCH lu.idLoja l " +
            "LEFT JOIN FETCH p.idDecisor d " +
            "WHERE p.id = :idPreferencia " +
            "AND l.id = :idLoja " +
            "AND lu.dataFim IS NULL")
    Optional<Preferencia> findPreferenciaDaLoja(@Param("idPreferencia") Integer idPreferencia,
                                                @Param("idLoja") Integer idLoja);

    @Query("SELECT p " +
            "FROM Preferencia p " +
            "JOIN FETCH p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "JOIN FETCH lu.idLoja l " +
            "LEFT JOIN FETCH p.idDecisor d " +
            "WHERE l.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND LOWER(p.estado) = 'aprovado' " +
            "AND ((p.dataInicio IS NULL) OR p.dataInicio <= :dataFim) " +
            "AND ((p.dataFim IS NULL) OR p.dataFim >= :dataInicio) " +
            "ORDER BY u.nome ASC, p.id ASC")
    List<Preferencia> findPreferenciasAprovadasDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                                @Param("dataInicio") LocalDate dataInicio,
                                                                @Param("dataFim") LocalDate dataFim);

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
