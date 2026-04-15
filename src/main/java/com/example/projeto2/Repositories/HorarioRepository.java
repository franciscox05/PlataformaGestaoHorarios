package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    // QUERY: Com JOIN FETCH para trazer os dados completos do Utilizador e do Turno para a memória
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH h.idTurno t " +
            "WHERE u.id != :meuId AND h.dataTurno >= CURRENT_DATE ORDER BY h.dataTurno ASC")
    List<Horario> findTurnosDosColegas(@Param("meuId") Integer meuId);
}