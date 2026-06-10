package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    public HorarioService(HorarioRepository horarioRepository,
                      LojautilizadorRepository lojautilizadorRepository,
                      LojautilizadorHelper lojautilizadorHelper,
                      TurnoRepository turnoRepository,
                      HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository) {
        this.horarioRepository = horarioRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.turnoRepository = turnoRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
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
        return lojautilizadorHelper.findLigacaoAtiva(idUtilizador);
    }

    // ── Aprovar Horário ──────────────────────────────────────────────────────
    @Transactional
    public void aprovarHorario(Integer idHorario, Integer idUtilizadorAprovador) {
        if (idHorario == null || idUtilizadorAprovador == null) {
            throw new IllegalArgumentException("Id do horário e do aprovador são obrigatórios.");
        }

        Horario horario = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Horário não encontrado com id: " + idHorario));

        if (horario.getEstado() != EstadoHorario.pendente) {
            throw new IllegalStateException(
                    "Só é possível aprovar horários no estado PENDENTE. "
                            + "Estado atual: " + horario.getEstado());
        }

        horario.setEstado(EstadoHorario.aprovado);
        horarioRepository.save(horario);
    }

    /**
     * Lista todos os turnos disponíveis no sistema (para o picker de edição).
     */
    @Transactional(readOnly = true)
    public List<Turno> listarTodosOsTurnos() {
        return turnoRepository.findAllByOrderByHoraInicioAsc();
    }

    /**
     * Permite ao gerente alterar o turno de um horário publicado.
     * Valida descanso mínimo de 11h antes de persistir.
     *
     * @param idHorario         ID do registo Horario a alterar
     * @param idNovoTurno       ID do novo Turno a atribuir
     * @param idAprovador       ID do utilizador que faz a alteração (deve ser gestor da loja)
     * @param motivoAlteracao   Texto livre com o motivo (opcional)
     */
    @Transactional
    public Horario editarTurnoPublicado(Integer idHorario, Integer idNovoTurno, Integer idAprovador, String motivoAlteracao) {
        if (idHorario == null || idNovoTurno == null || idAprovador == null) {
            throw new IllegalArgumentException("ID do horário, do novo turno e do aprovador são obrigatórios.");
        }

        Lojautilizador ligacaoAprovador = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idAprovador, LojautilizadorHelper.APROVACAO,
                "Não tens permissão para editar turnos publicados.");

        Horario horario = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado."));

        Turno novoTurno = turnoRepository.findById(idNovoTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));

        Integer idColaborador = horario.getIdLojautilizador() != null
                && horario.getIdLojautilizador().getIdUtilizador() != null
                ? horario.getIdLojautilizador().getIdUtilizador().getId() : null;

        // Validar descanso mínimo pós-edição (11h)
        if (idColaborador != null && novoTurno.getHoraInicio() != null && novoTurno.getHoraFim() != null) {
            LocalDate data = horario.getDataTurno();
            final int DESCANSO_MINIMO_HORAS = 11;

            List<Horario> turnosD_1 = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                    idColaborador, data.minusDays(1), data.minusDays(1));
            for (Horario h : turnosD_1) {
                if (h.getId().equals(idHorario) || h.getIdTurno() == null || h.getIdTurno().getHoraFim() == null) continue;
                long gap = Duration.between(
                        LocalDateTime.of(data.minusDays(1), h.getIdTurno().getHoraFim()),
                        LocalDateTime.of(data, novoTurno.getHoraInicio())
                ).toHours();
                if (gap < DESCANSO_MINIMO_HORAS) {
                    throw new IllegalArgumentException(
                            "O novo turno viola o descanso mínimo de " + DESCANSO_MINIMO_HORAS + "h (gap: " + gap + "h com o turno de " + data.minusDays(1) + ").");
                }
            }

            List<Horario> turnosD1 = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                    idColaborador, data.plusDays(1), data.plusDays(1));
            for (Horario h : turnosD1) {
                if (h.getId().equals(idHorario) || h.getIdTurno() == null || h.getIdTurno().getHoraInicio() == null) continue;
                long gap = Duration.between(
                        LocalDateTime.of(data, novoTurno.getHoraFim()),
                        LocalDateTime.of(data.plusDays(1), h.getIdTurno().getHoraInicio())
                ).toHours();
                if (gap < DESCANSO_MINIMO_HORAS) {
                    throw new IllegalArgumentException(
                            "O novo turno viola o descanso mínimo de " + DESCANSO_MINIMO_HORAS + "h (gap: " + gap + "h com o turno de " + data.plusDays(1) + ").");
                }
            }
        }

        // Registar no histórico
        String turnoAnteriorDesc = horario.getIdTurno() != null
                ? (horario.getIdTurno().getHoraInicio() + "-" + horario.getIdTurno().getHoraFim())
                : "?";
        String turnoNovoDesc = novoTurno.getHoraInicio() + "-" + novoTurno.getHoraFim();
        String observacao = "Turno alterado de " + turnoAnteriorDesc + " para " + turnoNovoDesc
                + (motivoAlteracao != null && !motivoAlteracao.isBlank() ? ". Motivo: " + motivoAlteracao : ".");

        HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
        historico.setIdHorario(horario);
        historico.setEstadoNovo(horario.getEstado() != null ? horario.getEstado().name() : "aprovado");
        historico.setDataRegisto(Instant.now());
        historico.setObservacoes(observacao);
        historicoHorarioEstadoRepository.save(historico);

        // Aplicar alteração
        horario.setIdTurno(novoTurno);
        return horarioRepository.save(horario);
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
