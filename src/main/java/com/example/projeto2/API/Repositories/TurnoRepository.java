package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByHoraInicioAsc();

    @Query("SELECT t FROM Turno t WHERE t.ativo = true ORDER BY t.horaInicio ASC")
    List<Turno> findAllAtivosOrderByHoraInicioAsc();

    @Query(value = "SELECT COUNT(*) > 0 FROM horarios h " +
            "WHERE h.id_turno = :idTurno " +
            "AND h.data_turno >= CURRENT_DATE " +
            "AND LOWER(h.estado) IN ('aprovado', 'publicado')",
            nativeQuery = true)
    boolean existeEmHorariosFuturosAprovados(@Param("idTurno") Integer idTurno);

    @Query("""
            SELECT t FROM Turno t
            WHERE t.horaInicio < :horaFim AND t.horaFim > :horaInicio
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findSobrepostos(@Param("horaInicio") LocalTime horaInicio,
                                @Param("horaFim") LocalTime horaFim,
                                @Param("idExcluir") Integer idExcluir);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Horario h WHERE h.idTurno.id = :idTurno")
    boolean existeEmHorarios(@Param("idTurno") Integer idTurno);

    /** Turnos com o intervalo de tempo EXATAMENTE igual (mesma hora de início e de fim). */
    @Query("""
            SELECT t FROM Turno t
            WHERE t.horaInicio = :horaInicio AND t.horaFim = :horaFim
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findByIntervaloExato(@Param("horaInicio") LocalTime horaInicio,
                                     @Param("horaFim") LocalTime horaFim,
                                     @Param("idExcluir") Integer idExcluir);

    /** Turnos ATIVOS com o mesmo nome (case-insensitive, ignorando espaços nas pontas). */
    @Query("""
            SELECT t FROM Turno t
            WHERE t.ativo = true
              AND LOWER(TRIM(t.nome)) = LOWER(TRIM(:nome))
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findAtivosPorNome(@Param("nome") String nome,
                                  @Param("idExcluir") Integer idExcluir);

    /** Todos os turnos (ativos ou inativos) com o mesmo nome — protege o histórico de duplicados. */
    @Query("""
            SELECT t FROM Turno t
            WHERE LOWER(TRIM(t.nome)) = LOWER(TRIM(:nome))
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findTodosPorNome(@Param("nome") String nome,
                                 @Param("idExcluir") Integer idExcluir);
}
