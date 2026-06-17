package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByHoraInicioAsc();

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
}
