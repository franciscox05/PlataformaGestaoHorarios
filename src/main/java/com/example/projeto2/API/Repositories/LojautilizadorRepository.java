package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Lojautilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByIdUtilizadorIdAndDataFimIsNull(Integer idUtilizador);

    long countByIdLojaIdAndIdCargoTipoInAndDataFimIsNull(Integer idLoja, Collection<String> tiposCargo);
}
