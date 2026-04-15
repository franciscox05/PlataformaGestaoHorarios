package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.DayOff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DayOffRepository extends JpaRepository<DayOff, Integer> {

    List<DayOff> findByIdUtilizador(Integer idUtilizador);
}
