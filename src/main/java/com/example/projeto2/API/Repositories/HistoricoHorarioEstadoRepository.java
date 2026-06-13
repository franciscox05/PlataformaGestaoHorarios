package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import org.springframework.data.repository.CrudRepository;

public interface HistoricoHorarioEstadoRepository extends CrudRepository<HistoricoHorarioEstado, Integer> {

    /** Remove o histórico associado a um horário (necessário antes de o apagar — FK). */
    void deleteByIdHorario(Horario idHorario);
}
