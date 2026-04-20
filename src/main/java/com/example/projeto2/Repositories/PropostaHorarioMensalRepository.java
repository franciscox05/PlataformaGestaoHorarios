package com.example.projeto2.Repositories;

import com.example.projeto2.Modules.PropostaHorarioMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropostaHorarioMensalRepository extends JpaRepository<PropostaHorarioMensal, Integer> {

    Optional<PropostaHorarioMensal> findFirstByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(Integer idLoja,
                                                                                           Integer ano,
                                                                                           Integer mes);
}
