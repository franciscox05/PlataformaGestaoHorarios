package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Regra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraRepository extends JpaRepository<Regra, Integer> {

    List<Regra> findAllByOrderByDescricaoAsc();
}
