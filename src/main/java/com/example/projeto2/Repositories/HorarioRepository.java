package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador AND h.dataTurno >= CURRENT_DATE " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findProximosTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id = :idUtilizador " +
            "AND h.dataTurno >= CURRENT_DATE " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "    AND (p.idHorarioOrigem.id = h.id OR p.idHorarioDestino.id = h.id)" +
            ") " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosDisponiveisParaPermutaPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id != :meuId AND h.dataTurno >= CURRENT_DATE " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "ORDER BY h.dataTurno ASC")
    List<Horario> findTurnosDosColegas(@Param("meuId") Integer meuId);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id <> :idUtilizador " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "AND h.dataTurno = (" +
            "    SELECT origem.dataTurno FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "AND l.id = (" +
            "    SELECT origem.idLojautilizador.idLoja.id FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "    AND (p.idHorarioOrigem.id = h.id OR p.idHorarioDestino.id = h.id)" +
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
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "AND l.id = (" +
            "    SELECT luAtivo.idLoja.id FROM Lojautilizador luAtivo " +
            "    WHERE luAtivo.idUtilizador.id = :idUtilizador " +
            "    AND luAtivo.dataFim IS NULL" +
            ") " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findEquipaDeHojeNaLojaDoUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY u.nome ASC, h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                               @Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE h.idPropostaHorario.id = :idPropostaHorario " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC, u.nome ASC")
    List<Horario> findByIdPropostaHorarioId(@Param("idPropostaHorario") Integer idPropostaHorario);

    @Query("SELECT COUNT(h) FROM Horario h " +
            "JOIN h.idLojautilizador lu " +
            "WHERE lu.idLoja.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (h.idPropostaHorario IS NULL OR LOWER(CAST(h.estado AS string)) <> 'pendente')")
    long countHorariosVisiveisDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                               @Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    void deleteByIdPropostaHorarioId(Integer idPropostaHorario);
}
