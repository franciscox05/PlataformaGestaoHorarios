package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Preferencia;
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
            "ORDER BY CASE WHEN p.dataInicio IS NULL THEN 1 ELSE 0 END, p.dataInicio ASC, p.id DESC")
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
            "ORDER BY CASE WHEN p.dataInicio IS NULL THEN 1 ELSE 0 END, p.dataInicio ASC, p.id DESC")
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

    @Query("SELECT COUNT(p) FROM Preferencia p " +
            "JOIN p.idUtilizador u " +
            "JOIN u.lojautilizadors lu " +
            "WHERE lu.idLoja.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND LOWER(p.estado) = 'pendente' " +
            "AND u.id <> :idUtilizadorAprovador")
    long countPreferenciasPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                          @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador);

    @Query(value = "SELECT COUNT(*) > 0 FROM preferencias p " +
            "WHERE p.id_utilizador = :idUtilizador " +
            "AND LOWER(p.tipo) = LOWER(:tipo) " +
            "AND LOWER(COALESCE(p.estado,'')) <> 'rejeitado'",
            nativeQuery = true)
    boolean existsPreferenciaAtivaPorTipo(@Param("idUtilizador") Integer idUtilizador,
                                          @Param("tipo") String tipo);

    // IS NOT DISTINCT FROM is PostgreSQL's null-safe equality (NULL IS NOT DISTINCT FROM NULL = TRUE).
    // Used here because Hibernate 6 cannot infer types for null JPQL named parameters in IS NULL tests.
    @Query(value = "SELECT COUNT(*) > 0 " +
            "FROM preferencias p " +
            "WHERE p.id_utilizador = :idUtilizador " +
            "AND LOWER(p.tipo) = LOWER(:tipo) " +
            "AND LOWER(p.descricao) = LOWER(:descricao) " +
            "AND p.data_inicio IS NOT DISTINCT FROM CAST(:dataInicio AS DATE) " +
            "AND p.data_fim IS NOT DISTINCT FROM CAST(:dataFim AS DATE) " +
            "AND (CAST(:idIgnorado AS INTEGER) IS NULL OR p.id_preferencia <> :idIgnorado)",
            nativeQuery = true)
    boolean existsPreferenciaDuplicada(@Param("idUtilizador") Integer idUtilizador,
                                       @Param("tipo") String tipo,
                                       @Param("descricao") String descricao,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim,
                                       @Param("idIgnorado") Integer idIgnorado);
}
