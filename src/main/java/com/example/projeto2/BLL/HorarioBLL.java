package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class HorarioBLL {

    private final HorarioRepository horarioRepository;
    private final LojautilizadorRepository lojautilizadorRepository;

    public HorarioBLL(HorarioRepository horarioRepository,
                      LojautilizadorRepository lojautilizadorRepository) {
        this.horarioRepository = horarioRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
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
    public List<Horario> listarMeusTurnosDisponiveisParaPermuta(Integer idUtilizadorLogado) {
        if (idUtilizadorLogado == null) {
            return List.of();
        }

        return horarioRepository.findTurnosDisponiveisParaPermutaPorUtilizador(idUtilizadorLogado);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarTurnosElegiveisParaPermuta(Integer idUtilizadorLogado, Integer idHorarioOrigem) {
        if (idUtilizadorLogado == null || idHorarioOrigem == null) {
            return List.of();
        }

        return horarioRepository.findTurnosElegiveisParaPermuta(idUtilizadorLogado, idHorarioOrigem);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarEquipaDeHoje(Integer idUtilizadorLogado) {
        return horarioRepository.findEquipaDeHojeNaLojaDoUtilizador(idUtilizadorLogado);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarHorarioPublicadoDoUtilizador(Integer idUtilizador,
                                                            LocalDate dataInicio,
                                                            LocalDate dataFim) {
        if (idUtilizador == null || dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            return List.of();
        }

        return horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idUtilizador, dataInicio, dataFim);
    }

    @Transactional(readOnly = true)
    public List<Horario> listarHorarioPublicadoDaLojaDoUtilizador(Integer idUtilizadorGestor,
                                                                  LocalDate dataInicio,
                                                                  LocalDate dataFim,
                                                                  Integer idColaborador) {
        if (idUtilizadorGestor == null || dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            return List.of();
        }

        return obterLigacaoAtiva(idUtilizadorGestor)
                .map(ligacao -> horarioRepository.findHorariosPublicadosDaLojaEntreDatas(
                        ligacao.getIdLoja().getId(),
                        dataInicio,
                        dataFim,
                        idColaborador
                ))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<Horario> listarHorarioPublicadoMensalDaLojaDoUtilizador(Integer idUtilizadorGestor,
                                                                        Integer ano,
                                                                        Integer mes) {
        if (idUtilizadorGestor == null || ano == null || mes == null || mes < 1 || mes > 12) {
            return List.of();
        }

        YearMonth periodo = YearMonth.of(ano, mes);
        return listarHorarioPublicadoDaLojaDoUtilizador(
                idUtilizadorGestor,
                periodo.atDay(1),
                periodo.atEndOfMonth(),
                null
        );
    }

    @Transactional(readOnly = true)
    public List<ColaboradorLoja> listarColaboradoresAtivosDaLojaDoUtilizador(Integer idUtilizadorGestor) {
        if (idUtilizadorGestor == null) {
            return List.of();
        }

        return obterLigacaoAtiva(idUtilizadorGestor)
                .map(ligacao -> lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(ligacao.getIdLoja().getId()).stream()
                        .filter(item -> item.getDataFim() == null)
                        .filter(item -> item.getIdUtilizador() != null && item.getIdUtilizador().getId() != null)
                        .map(item -> new ColaboradorLoja(
                                item.getIdUtilizador().getId(),
                                item.getIdUtilizador().getNome(),
                                item.getIdCargo() != null ? item.getIdCargo().getNome() : "-"
                        ))
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    private java.util.Optional<Lojautilizador> obterLigacaoAtiva(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
    }

    public record ColaboradorLoja(Integer idUtilizador, String nome, String cargo) {
        public String etiqueta() {
            if (cargo == null || cargo.isBlank()) {
                return nome;
            }
            return nome + " (" + cargo + ")";
        }
    }
}
