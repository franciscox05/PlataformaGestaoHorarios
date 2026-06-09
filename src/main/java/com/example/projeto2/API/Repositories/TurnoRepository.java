package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Turno;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TurnoRepository extends CrudRepository<Turno, Integer> {

    List<Turno> findAllByOrderByHoraInicioAsc();
}
