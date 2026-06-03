package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayOffRepository extends JpaRepository<DayOff, Integer> {

    // Pesquisa por entidade Utilizador (relação JPA correcta)
    List<DayOff> findByIdUtilizador(Utilizador idUtilizador);

    // Convenência: pesquisa por ID do utilizador (evita carregar a entidade antes)
    @Query("SELECT d FROM DayOff d WHERE d.idUtilizador.id = :idUtilizador")
    List<DayOff> findByIdUtilizadorId(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT d FROM DayOff d " +
            "WHERE LOWER(CAST(d.estado AS string)) = 'pendente' " +
            "AND d.idUtilizador.id <> :idUtilizadorAprovador " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Lojautilizador lu " +
            "    WHERE lu.idUtilizador.id = d.idUtilizador.id " +
            "    AND lu.idLoja.id = :idLoja " +
            "    AND lu.dataFim IS NULL" +
            ") " +
            "ORDER BY d.dataAusencia ASC, d.idDayoff ASC")
    List<DayOff> findPedidosPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                            @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador);

    @Query("SELECT d FROM DayOff d " +
            "WHERE LOWER(CAST(d.estado AS string)) = 'aprovado' " +
            "AND d.dataAusencia BETWEEN :dataInicio AND :dataFim " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Lojautilizador lu " +
            "    WHERE lu.idUtilizador.id = d.idUtilizador.id " +
            "    AND lu.idLoja.id = :idLoja " +
            "    AND lu.dataFim IS NULL" +
            ") " +
            "ORDER BY d.dataAusencia ASC, d.idDayoff ASC")
    List<DayOff> findPedidosAprovadosDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                      @Param("dataInicio") LocalDate dataInicio,
                                                      @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM DayOff d " +
            "WHERE LOWER(CAST(d.estado AS string)) = 'pendente' " +
            "AND d.idUtilizador.id <> :idUtilizadorAprovador " +
            "AND d.dataAusencia BETWEEN :dataInicio AND :dataFim " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Lojautilizador lu " +
            "    WHERE lu.idUtilizador.id = d.idUtilizador.id " +
            "    AND lu.idLoja.id = :idLoja " +
            "    AND lu.dataFim IS NULL" +
            ") " +
            "ORDER BY d.dataAusencia ASC, d.idDayoff ASC")
    List<DayOff> findPedidosPendentesDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                      @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador,
                                                      @Param("dataInicio") LocalDate dataInicio,
                                                      @Param("dataFim") LocalDate dataFim);

    @Query("SELECT d FROM DayOff d " +
            "WHERE d.idDayoff = :idDayOff " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Lojautilizador lu " +
            "    WHERE lu.idUtilizador.id = d.idUtilizador.id " +
            "    AND lu.idLoja.id = :idLoja " +
            "    AND lu.dataFim IS NULL" +
            ")")
    Optional<DayOff> findPedidoDaLojaById(@Param("idLoja") Integer idLoja,
                                          @Param("idDayOff") Integer idDayOff);
}
