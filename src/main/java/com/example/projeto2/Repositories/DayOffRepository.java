package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.DayOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DayOffRepository extends JpaRepository<DayOff, Integer> {

    List<DayOff> findByIdUtilizador(Integer idUtilizador);

    @Query("SELECT d FROM DayOff d " +
            "WHERE d.estado = 'pendente' " +
            "AND d.idUtilizador <> :idUtilizadorAprovador " +
            "AND EXISTS (" +
            "    SELECT 1 FROM Lojautilizador lu " +
            "    WHERE lu.idUtilizador.id = d.idUtilizador " +
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
            "    WHERE lu.idUtilizador.id = d.idUtilizador " +
            "    AND lu.idLoja.id = :idLoja " +
            "    AND lu.dataFim IS NULL" +
            ") " +
            "ORDER BY d.dataAusencia ASC, d.idDayoff ASC")
    List<DayOff> findPedidosAprovadosDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                      @Param("dataInicio") LocalDate dataInicio,
                                                      @Param("dataFim") LocalDate dataFim);
}
