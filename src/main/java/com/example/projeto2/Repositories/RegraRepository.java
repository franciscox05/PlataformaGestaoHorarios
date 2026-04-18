package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.Regra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegraRepository extends JpaRepository<Regra, Integer> {

    List<Regra> findAllByOrderByDescricaoAsc();
}
