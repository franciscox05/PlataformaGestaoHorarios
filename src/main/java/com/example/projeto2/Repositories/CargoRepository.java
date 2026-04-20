package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Cargo;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CargoRepository extends CrudRepository<Cargo, Integer> {

    List<Cargo> findAllByOrderByNomeAsc();
}
