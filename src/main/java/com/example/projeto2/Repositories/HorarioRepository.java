package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador AND h.dataTurno >= CURRENT_DATE " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findProximosTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador " +
            "AND h.dataTurno >= CURRENT_DATE " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(p.estado) = 'pendente' " +
            "    AND p.idHorarioOrigem.id = h.id" +
            ") " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosDisponiveisParaPermutaPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id != :meuId AND h.dataTurno >= CURRENT_DATE ORDER BY h.dataTurno ASC")
    List<Horario> findTurnosDosColegas(@Param("meuId") Integer meuId);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id <> :idUtilizador " +
            "AND h.dataTurno = (" +
            "    SELECT origem.dataTurno FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "AND l.id = (" +
            "    SELECT origem.idLojautilizador.idLoja.id FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findTurnosElegiveisParaPermuta(@Param("idUtilizador") Integer idUtilizador,
                                                 @Param("idHorarioOrigem") Integer idHorarioOrigem);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE h.dataTurno = CURRENT_DATE " +
            "AND l.id = (" +
            "    SELECT luAtivo.idLoja.id FROM Lojautilizador luAtivo " +
            "    WHERE luAtivo.idUtilizador.id = :idUtilizador " +
            "    AND luAtivo.dataFim IS NULL" +
            ") " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findEquipaDeHojeNaLojaDoUtilizador(@Param("idUtilizador") Integer idUtilizador);
}
