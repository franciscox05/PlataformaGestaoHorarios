package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Lojautilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LojautilizadorRepository extends JpaRepository<Lojautilizador, Integer> {

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE lu.idUtilizador.id = :idUtilizador AND lu.dataFim IS NULL")
    Optional<Lojautilizador> findLigacaoAtivaByIdUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE l.id = :idLoja " +
            "ORDER BY LOWER(u.nome), lu.dataInicio DESC, lu.id DESC")
    List<Lojautilizador> findByIdLojaWithUtilizadorCargo(@Param("idLoja") Integer idLoja);

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE l.id = :idLoja AND u.id = :idUtilizador " +
            "ORDER BY lu.dataInicio DESC, lu.id DESC")
    List<Lojautilizador> findHistoricoByIdLojaAndIdUtilizador(@Param("idLoja") Integer idLoja,
                                                              @Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE u.id = :idUtilizador AND l.id = :idLoja AND lu.dataFim IS NULL")
    Optional<Lojautilizador> findLigacaoAtivaByIdUtilizadorAndIdLoja(@Param("idUtilizador") Integer idUtilizador,
                                                                     @Param("idLoja") Integer idLoja);

    // All active store-links for a user — supports multi-store context resolution
    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "WHERE lu.idUtilizador.id = :idUtilizador AND lu.dataFim IS NULL " +
            "ORDER BY l.nome ASC, lu.dataInicio DESC")
    List<Lojautilizador> findLigacoesAtivasByIdUtilizador(@Param("idUtilizador") Integer idUtilizador);

    long countByIdUtilizadorIdAndDataFimIsNull(Integer idUtilizador);

    long countByIdLojaIdAndIdCargoTipoInAndDataFimIsNull(Integer idLoja, Collection<String> tiposCargo);

    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "WHERE lu.idLoja.id = :idLoja AND lu.dataFim IS NULL AND c.tipo IN :tipos")
    List<Lojautilizador> findLigacoesAtivasByIdLojaAndCargos(@Param("idLoja") Integer idLoja,
                                                               @Param("tipos") Collection<String> tipos);

    // Schedule-derived day-off: active store members with no approved shift on the given date
    @Query("SELECT lu FROM Lojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "WHERE lu.idLoja.id = :idLoja " +
            "AND lu.dataFim IS NULL " +
            "AND lu.idUtilizador.id NOT IN (" +
            "    SELECT h.idLojautilizador.idUtilizador.id FROM Horario h " +
            "    LEFT JOIN h.idPropostaHorario ph " +
            "    WHERE h.dataTurno = :data " +
            "    AND h.idLojautilizador.idLoja.id = :idLoja " +
            "    AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "    AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado')" +
            ") " +
            "ORDER BY LOWER(u.nome) ASC")
    List<Lojautilizador> findFuncionariosDeFolgaNoDia(@Param("idLoja") Integer idLoja,
                                                      @Param("data") LocalDate data);
}
