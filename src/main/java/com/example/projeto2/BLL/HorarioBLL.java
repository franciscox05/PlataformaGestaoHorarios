package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Repositories.HorarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HorarioBLL {

    private final HorarioRepository horarioRepository;

    public HorarioBLL(HorarioRepository horarioRepository) {
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public List<Horario> listarProximosTurnos(Integer idUtilizadorDesejado) {
        return horarioRepository.findProximosTurnosPorUtilizador(idUtilizadorDesejado);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarTurnosColegas(Integer idUtilizadorLogado) {
        return horarioRepository.findTurnosDosColegas(idUtilizadorLogado);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarEquipaDeHoje(Integer idUtilizadorLogado) {
        return horarioRepository.findEquipaDeHojeNaLojaDoUtilizador(idUtilizadorLogado);
    }
}
