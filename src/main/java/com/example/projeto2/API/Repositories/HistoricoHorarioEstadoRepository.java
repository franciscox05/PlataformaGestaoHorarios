package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface HistoricoHorarioEstadoRepository extends CrudRepository<HistoricoHorarioEstado, Integer> {

    /** Remove o histórico associado a um horário (necessário antes de o apagar — FK). */
    void deleteByIdHorario(Horario idHorario);

    /** Remove todos os históricos de todos os horários de uma proposta (necessário antes de apagar a proposta). */
    @Modifying
    @Query("DELETE FROM HistoricoHorarioEstado hhe WHERE hhe.idHorario IN " +
           "(SELECT h FROM Horario h WHERE h.idPropostaHorario.id = :idProposta)")
    void deleteByHorarioPropostaId(@Param("idProposta") Integer idProposta);
}
