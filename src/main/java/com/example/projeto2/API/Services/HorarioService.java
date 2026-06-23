package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.API.Repositories.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;
    private final PropostaHorarioMensalRepository propostaRepository;
    private final SessaoService sessaoBLL;

    public HorarioService(HorarioRepository horarioRepository,
                      LojautilizadorRepository lojautilizadorRepository,
                      LojautilizadorHelper lojautilizadorHelper,
                      TurnoRepository turnoRepository,
                      HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository,
                      PropostaHorarioMensalRepository propostaRepository,
                      SessaoService sessaoBLL) {
        this.horarioRepository = horarioRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.turnoRepository = turnoRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
        this.propostaRepository = propostaRepository;
        this.sessaoBLL = sessaoBLL;
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

        List<Horario> elegiveis = horarioRepository.findTurnosElegiveisParaPermuta(idUtilizadorLogado, idHorarioOrigem);

        // Alinhamento com a validação de PermutaService: excluir turnos com horário
        // idêntico ao do turno de origem — permutar para as mesmas horas é redundante.
        Horario origem = horarioRepository.findById(idHorarioOrigem).orElse(null);
        if (origem == null || origem.getIdTurno() == null
                || origem.getIdTurno().getHoraInicio() == null
                || origem.getIdTurno().getHoraFim() == null) {
            return elegiveis;
        }
        var horaInicioOrigem = origem.getIdTurno().getHoraInicio();
        var horaFimOrigem = origem.getIdTurno().getHoraFim();

        return elegiveis.stream()
                .filter(h -> h.getIdTurno() == null
                        || !(horaInicioOrigem.equals(h.getIdTurno().getHoraInicio())
                             && horaFimOrigem.equals(h.getIdTurno().getHoraFim())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Horario> listarEquipaDeHoje(Integer idUtilizadorLogado) {
        return listarEquipaDeHoje(idUtilizadorLogado, null);
    }

    /**
     * Equipa escalada hoje na loja indicada. Store-scoped — a query antiga resolvia a
     * loja com uma subquery escalar {@code (SELECT idLoja ... WHERE idUtilizador=? AND
     * dataFim IS NULL)} que rebentava com "more than one row" para um utilizador
     * multi-loja (ex.: gerente em 2 lojas). Agora a loja é explícita: a Web passa a loja
     * activa da sessão; sem ela, resolve-se a primeira ligação activa (loja única).
     */
    @Transactional(readOnly = true)
    public List<Horario> listarEquipaDeHoje(Integer idUtilizadorLogado, Integer idLoja) {
        if (idUtilizadorLogado == null) {
            return List.of();
        }
        Integer idLojaEfetiva = idLoja;
        if (idLojaEfetiva == null) {
            idLojaEfetiva = lojautilizadorHelper.findLigacaoAtiva(idUtilizadorLogado)
                    .map(lu -> lu.getIdLoja().getId())
                    .orElse(null);
        }
        if (idLojaEfetiva == null) {
            return List.of();
        }
        return horarioRepository.findEquipaDeHojeNaLoja(idLojaEfetiva);
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
        return listarHorarioPublicadoDaLojaDoUtilizador(
                idUtilizadorGestor, dataInicio, dataFim, idColaborador, null);
    }

    /**
     * Variante store-explicit do horário publicado da equipa. O Desktop passa a
     * loja activa da sessão ({@code sessaoBLL.obterLojaAtiva()}) para garantir
     * isolamento estrito por loja na vista da Equipa — sem ela, um utilizador
     * multi-loja via a equipa da primeira ligação activa (ex.: Braga Parque) em
     * vez da loja onde está realmente logado (ex.: NorteShopping). Quando
     * {@code idLoja} é {@code null} ou inválido ({@code <= 0}), resolve-se a
     * primeira ligação activa — comportamento legado para loja única. Espelha o
     * padrão de {@link #listarEquipaDeHoje(Integer, Integer)}.
     */
    @Transactional(readOnly = true)
    public List<Horario> listarHorarioPublicadoDaLojaDoUtilizador(Integer idUtilizadorGestor,
                                                                  LocalDate dataInicio,
                                                                  LocalDate dataFim,
                                                                  Integer idColaborador,
                                                                  Integer idLoja) {
        if (idUtilizadorGestor == null || dataInicio == null || dataFim == null || dataFim.isBefore(dataInicio)) {
            return List.of();
        }

        Integer idLojaEfetiva = (idLoja != null && idLoja > 0)
                ? idLoja
                : obterLigacaoAtiva(idUtilizadorGestor).map(l -> l.getIdLoja().getId()).orElse(null);
        if (idLojaEfetiva == null) {
            return List.of();
        }

        return horarioRepository.findHorariosPublicadosDaLojaEntreDatas(
                idLojaEfetiva, dataInicio, dataFim, idColaborador);
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
        return listarColaboradoresAtivosDaLojaDoUtilizador(idUtilizadorGestor, null);
    }

    /**
     * Variante store-explicit dos colaboradores activos — alimenta o filtro de
     * colaborador da vista mensal da Equipa. Tem de respeitar a mesma loja activa
     * da sessão que a listagem de turnos, senão o filtro mostra colegas de outra
     * loja. Quando {@code idLoja} é {@code null}/inválido, resolve a primeira
     * ligação activa (loja única / legado).
     */
    @Transactional(readOnly = true)
    public List<ColaboradorLoja> listarColaboradoresAtivosDaLojaDoUtilizador(Integer idUtilizadorGestor, Integer idLoja) {
        if (idUtilizadorGestor == null) {
            return List.of();
        }

        Integer idLojaEfetiva = (idLoja != null && idLoja > 0)
                ? idLoja
                : obterLigacaoAtiva(idUtilizadorGestor).map(l -> l.getIdLoja().getId()).orElse(null);
        if (idLojaEfetiva == null) {
            return List.of();
        }

        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLojaEfetiva).stream()
                .filter(item -> item.getDataFim() == null)
                .filter(item -> item.getIdUtilizador() != null && item.getIdUtilizador().getId() != null)
                .map(item -> new ColaboradorLoja(
                        item.getIdUtilizador().getId(),
                        item.getIdUtilizador().getNome(),
                        item.getIdCargo() != null ? item.getIdCargo().getNome() : "-"
                ))
                .distinct()
                .toList();
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
     * Lista os turnos disponíveis para o picker de edição, scoped à loja activa da
     * sessão (turnos globais + próprios da loja). Sem loja activa (loja única ou
     * testes), devolve todos os turnos activos — comportamento legado seguro.
     */
    @Transactional(readOnly = true)
    public List<Turno> listarTodosOsTurnos() {
        Integer idLoja = sessaoBLL.obterLojaAtiva();
        Integer idLojaSegura = (idLoja != null && idLoja > 0) ? idLoja : null;
        return idLojaSegura == null
                ? turnoRepository.findAllAtivosOrderByHoraInicioAsc()
                : turnoRepository.findAtivosParaLojaOrderByHoraInicioAsc(idLojaSegura);
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

        // Cross-store overlap guard — excludes the current record to avoid counting its own old turno
        if (idColaborador != null && novoTurno.getHoraInicio() != null && novoTurno.getHoraFim() != null) {
            long sobreposicoes = horarioRepository.countGlobalOverlappingShiftsExcluding(
                    idColaborador, horario.getDataTurno(),
                    novoTurno.getHoraInicio(), novoTurno.getHoraFim(),
                    idHorario);
            if (sobreposicoes > 0) {
                throw new IllegalArgumentException(
                        "O novo turno sobrepõe-se a um turno já atribuído ao colaborador noutra loja.");
            }
        }

        // Aplicar alteração
        horario.setIdTurno(novoTurno);
        return horarioRepository.save(horario);
    }

    /**
     * Adiciona um turno a um colaborador num dia específico de uma proposta.
     * Útil quando o gerente removeu um turno e precisa de o restaurar, ou quando quer
     * cobrir um dia que ficou sem nenhum colaborador atribuído.
     */
    @Transactional
    public Horario adicionarTurno(Integer idProposta, Integer idLojautilizador,
                                  LocalDate data, Integer idTurno, Integer idAprovador) {
        if (idProposta == null || idLojautilizador == null || data == null
                || idTurno == null || idAprovador == null) {
            throw new IllegalArgumentException("Todos os campos são obrigatórios.");
        }
        lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idAprovador, LojautilizadorHelper.APROVACAO,
                "Não tens permissão para editar horários.");

        PropostaHorarioMensal proposta = propostaRepository.findById(idProposta)
                .orElseThrow(() -> new IllegalArgumentException("Proposta não encontrada."));

        Lojautilizador lojautilizador = lojautilizadorRepository.findById(idLojautilizador)
                .orElseThrow(() -> new IllegalArgumentException("Colaborador não encontrado."));

        if (horarioRepository.existsByIdLojautilizadorIdAndDataTurnoAndIdPropostaHorarioId(
                idLojautilizador, data, idProposta)) {
            throw new IllegalArgumentException("Este colaborador já tem um turno atribuído nesse dia.");
        }

        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new IllegalArgumentException("Turno não encontrado."));

        if (lojautilizador.getIdUtilizador() != null
                && turno.getHoraInicio() != null && turno.getHoraFim() != null) {
            long sobreposicoes = horarioRepository.countGlobalOverlappingShifts(
                    lojautilizador.getIdUtilizador().getId(), data,
                    turno.getHoraInicio(), turno.getHoraFim());
            if (sobreposicoes > 0) {
                throw new IllegalArgumentException(
                        "O colaborador já possui um turno atribuído noutra loja que se sobrepõe a este horário.");
            }
            // Descanso mínimo de 11h (Código do Trabalho, art. 214.º) calculado GLOBALMENTE
            // por colaborador — o descanso é um direito da pessoa, não da loja. Sem isto, um
            // gestor podia atribuir um turno numa loja que viola o descanso face a um turno já
            // publicado noutra loja do mesmo colaborador (ver Revisao.md, ponto 1).
            validarDescansoMinimoGlobal(lojautilizador.getIdUtilizador().getId(), turno, data);
        }

        Horario novo = new Horario();
        novo.setIdLojautilizador(lojautilizador);
        novo.setIdTurno(turno);
        novo.setIdPropostaHorario(proposta);
        novo.setDataTurno(data);
        try {
            novo.setEstado(EstadoHorario.valueOf(proposta.getEstado()));
        } catch (Exception e) {
            novo.setEstado(EstadoHorario.pendente);
        }
        return horarioRepository.save(novo);
    }

    /**
     * Valida o descanso mínimo de 11h entre o turno novo (em {@code data}) e os turnos
     * publicados do colaborador nos dias adjacentes — GLOBAL, sobre todas as lojas do
     * colaborador. Espelha {@code PermutaService.validarDescansoMinimoPosPermuta}, mas
     * para o caminho de atribuição direta ({@code adicionarTurno}), que era a única via
     * cross-store sem esta guarda (Revisao.md, ponto 1).
     */
    private void validarDescansoMinimoGlobal(Integer idColaborador, Turno turnoNovo, LocalDate data) {
        final int DESCANSO_MINIMO_HORAS = 11;
        if (turnoNovo == null || turnoNovo.getHoraInicio() == null || turnoNovo.getHoraFim() == null) {
            return;
        }

        List<Horario> turnosDiaAnterior = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.minusDays(1), data.minusDays(1));
        for (Horario h : turnosDiaAnterior) {
            if (h.getIdTurno() == null || h.getIdTurno().getHoraFim() == null) continue;
            long horasGap = Duration.between(
                    LocalDateTime.of(data.minusDays(1), h.getIdTurno().getHoraFim()),
                    LocalDateTime.of(data, turnoNovo.getHoraInicio())).toHours();
            if (horasGap < DESCANSO_MINIMO_HORAS) {
                throw new IllegalArgumentException(
                        "Esta atribuição viola o descanso mínimo de " + DESCANSO_MINIMO_HORAS
                                + "h entre turnos de dias consecutivos do colaborador (gap atual: "
                                + horasGap + "h), mesmo entre lojas diferentes.");
            }
        }

        List<Horario> turnosDiaSeguinte = horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(
                idColaborador, data.plusDays(1), data.plusDays(1));
        for (Horario h : turnosDiaSeguinte) {
            if (h.getIdTurno() == null || h.getIdTurno().getHoraInicio() == null) continue;
            long horasGap = Duration.between(
                    LocalDateTime.of(data, turnoNovo.getHoraFim()),
                    LocalDateTime.of(data.plusDays(1), h.getIdTurno().getHoraInicio())).toHours();
            if (horasGap < DESCANSO_MINIMO_HORAS) {
                throw new IllegalArgumentException(
                        "Esta atribuição viola o descanso mínimo de " + DESCANSO_MINIMO_HORAS
                                + "h entre turnos de dias consecutivos do colaborador (gap atual: "
                                + horasGap + "h), mesmo entre lojas diferentes.");
            }
        }
    }

    /**
     * Remove um turno do horário — o colaborador fica de folga nesse dia. Usado pelo
     * gestor para ajustar manualmente uma proposta/horário (ex.: libertar um fim de
     * semana). Apaga primeiro o histórico associado (FK) e depois o próprio turno.
     *
     * @param idHorario  ID do registo Horario a remover
     * @param idAprovador ID do utilizador (deve ter permissão de gestão da loja)
     */
    @Transactional
    public void removerTurno(Integer idHorario, Integer idAprovador) {
        if (idHorario == null || idAprovador == null) {
            throw new IllegalArgumentException("ID do horário e do aprovador são obrigatórios.");
        }
        lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idAprovador, LojautilizadorHelper.APROVACAO,
                "Não tens permissão para editar horários.");

        Horario horario = horarioRepository.findById(idHorario)
                .orElseThrow(() -> new IllegalArgumentException("Horário não encontrado."));

        historicoHorarioEstadoRepository.deleteByIdHorario(horario);
        horarioRepository.delete(horario);
    }

    public record ColaboradorLoja(Integer idUtilizador, String nome, String cargo) {
        public String etiqueta() {
            if (cargo == null || cargo.isBlank()) {
                return nome;
            }
            return nome + " (" + cargo + ")";
        }
    }

    /** Projection used by the employee "team on a day" endpoint. */
    public record ColaboradorNoDia(String nome, String cargo, String tipoTurno,
                                   String inicio, String fim, Integer horarioId, boolean meuTurno) {}

    @Transactional(readOnly = true)
    public List<ColaboradorNoDia> listarColegasNoDia(Integer idUtilizadorLogado, LocalDate data) {
        if (idUtilizadorLogado == null || data == null) return List.of();
        return lojautilizadorHelper.findLigacaoAtiva(idUtilizadorLogado)
                .map(lu -> colegasNoDiaInterno(idUtilizadorLogado, data, lu.getIdLoja().getId()))
                .orElse(List.of());
    }

    /** Store-scoped variant — uses the explicit idLoja from the session instead of resolving it internally. */
    @Transactional(readOnly = true)
    public List<ColaboradorNoDia> listarColegasNoDia(Integer idUtilizadorLogado, LocalDate data, Integer idLoja) {
        if (idUtilizadorLogado == null || data == null || idLoja == null)
            return listarColegasNoDia(idUtilizadorLogado, data);
        return lojautilizadorHelper.findLigacaoAtiva(idUtilizadorLogado, idLoja)
                .map(lu -> colegasNoDiaInterno(idUtilizadorLogado, data, idLoja))
                .orElse(List.of());
    }

    private List<ColaboradorNoDia> colegasNoDiaInterno(Integer idUtilizadorLogado, LocalDate data, Integer idLoja) {
        DateTimeFormatter hhmm = DateTimeFormatter.ofPattern("HH:mm");
        List<ColaboradorNoDia> proprios = horarioRepository
                .findMeusTurnosNoDia(idLoja, data, idUtilizadorLogado)
                .stream()
                .map(h -> new ColaboradorNoDia(
                        h.getIdLojautilizador().getIdUtilizador().getNome(),
                        h.getIdLojautilizador().getIdCargo() != null
                                ? h.getIdLojautilizador().getIdCargo().getNome() : "-",
                        h.getIdTurno().getTipo() != null ? h.getIdTurno().getTipo() : "-",
                        h.getIdTurno().getHoraInicio() != null
                                ? h.getIdTurno().getHoraInicio().format(hhmm) : "--:--",
                        h.getIdTurno().getHoraFim() != null
                                ? h.getIdTurno().getHoraFim().format(hhmm) : "--:--",
                        h.getId(), true))
                .toList();
        List<ColaboradorNoDia> colegas = horarioRepository
                .findColeaguesDaLojaNoDia(idLoja, data, idUtilizadorLogado)
                .stream()
                .map(h -> new ColaboradorNoDia(
                        h.getIdLojautilizador().getIdUtilizador().getNome(),
                        h.getIdLojautilizador().getIdCargo() != null
                                ? h.getIdLojautilizador().getIdCargo().getNome() : "-",
                        h.getIdTurno().getTipo() != null ? h.getIdTurno().getTipo() : "-",
                        h.getIdTurno().getHoraInicio() != null
                                ? h.getIdTurno().getHoraInicio().format(hhmm) : "--:--",
                        h.getIdTurno().getHoraFim() != null
                                ? h.getIdTurno().getHoraFim().format(hhmm) : "--:--",
                        null, false))
                .toList();
        var merged = new java.util.ArrayList<>(proprios);
        merged.addAll(colegas);
        return java.util.Collections.unmodifiableList(merged);
    }
}
