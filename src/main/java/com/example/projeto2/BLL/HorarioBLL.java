package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Repositories.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // IMPORTANTE

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HorarioBLL {

    private final HorarioRepository horarioRepository;

    public HorarioBLL(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true) // ISTO RESOLVE O ERRO "NO SESSION"
    public List<Horario> listarProximosTurnos(Integer idUtilizadorDesejado) {
        List<Horario> todos = horarioRepository.findAll();

        return todos.stream()
                .filter(h -> h.getIdLojautilizador() != null &&
                        h.getIdLojautilizador().getIdUtilizador() != null &&
                        h.getIdLojautilizador().getIdUtilizador().getId().equals(idUtilizadorDesejado))
                .filter(h -> h.getDataTurno() != null &&
                        h.getDataTurno().isAfter(LocalDate.now().minusDays(1)))
                .collect(Collectors.toList());
    }

    // Method para a 2ª ComboBox das Permutas
    public List<Horario> listarTurnosColegas(Integer idUtilizadorLogado) {
        return horarioRepository.findTurnosDosColegas(idUtilizadorLogado);
    }
}