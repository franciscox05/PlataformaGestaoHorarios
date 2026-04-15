package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Repositories.PermutaRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PermutaBLL {

    private final PermutaRepository permutaRepository;

    public PermutaBLL(PermutaRepository permutaRepository) {
        this.permutaRepository = permutaRepository;
    }

    public void registarPedidoTroca(Horario meuTurno, Horario turnoColega) {
        Permuta novaPermuta = new Permuta();
        novaPermuta.setIdHorarioOrigem(meuTurno);
        novaPermuta.setIdHorarioDestino(turnoColega);

        // Enviamos o estado como String, a BD converte para o Enum
        novaPermuta.setEstado("pendente");
        novaPermuta.setDataPedido(Instant.now());

        // Grava na Base de Dados!
        permutaRepository.save(novaPermuta);
    }
}