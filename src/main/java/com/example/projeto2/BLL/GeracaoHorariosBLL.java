package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.HistoricoHorarioEstado;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.PropostaHorarioMensal;
import com.example.projeto2.Modules.Regra;
import com.example.projeto2.Modules.RegrasLoja;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.PreferenciaRepository;
import com.example.projeto2.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.Repositories.RegraRepository;
import com.example.projeto2.Repositories.RegrasLojaRepository;
import com.example.projeto2.Repositories.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class GeracaoHorariosBLL {

    private static final Set<String> CARGOS_COM_GERACAO = Set.of("gerente", "subgerente");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;
    private final PreferenciaRepository preferenciaRepository;
    private final PropostaHorarioMensalRepository propostaHorarioMensalRepository;
    private final RegrasLojaRepository regrasLojaRepository;
    private final RegraRepository regraRepository;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    public GeracaoHorariosBLL(LojautilizadorRepository lojautilizadorRepository,
                              HorarioRepository horarioRepository,
                              DayOffRepository dayOffRepository,
                              PreferenciaRepository preferenciaRepository,
                              PropostaHorarioMensalRepository propostaHorarioMensalRepository,
                              RegrasLojaRepository regrasLojaRepository,
                              RegraRepository regraRepository,
                              TurnoRepository turnoRepository,
                              HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.propostaHorarioMensalRepository = propostaHorarioMensalRepository;
        this.regrasLojaRepository = regrasLojaRepository;
        this.regraRepository = regraRepository;
        this.turnoRepository = turnoRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerarHorarios(Integer idUtilizador) {
        return obterLigacaoAtiva(idUtilizador)
                .map(this::temPermissaoDeGeracao)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public GeracaoContexto obterContexto(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        LocalDate hoje = LocalDate.now();

        return new GeracaoContexto(
                ligacaoAtiva.getIdLoja().getId(),
                valorOuTraco(ligacaoAtiva.getIdLoja().getNome()),
                valorOuTraco(ligacaoAtiva.getIdLoja().getLocalizacao()),
                hoje.getYear(),
                hoje.getMonthValue(),
                propostaHorarioMensalRepository.findFirstByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        ligacaoAtiva.getIdLoja().getId(),
                        hoje.getYear(),
                        hoje.getMonthValue()
                ).isPresent()
        );
    }

    @Transactional(readOnly = true)
    public PropostaResultado obterProposta(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        Optional<PropostaHorarioMensal> proposta = propostaHorarioMensalRepository
                .findFirstByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        ligacaoAtiva.getIdLoja().getId(),
                        anoNormalizado,
                        mesNormalizado
                );

        if (proposta.isEmpty()) {
            return null;
        }

        return construirResultado(proposta.get(), horarioRepository.findByIdPropostaHorarioId(proposta.get().getId()));
    }

    @Transactional
    public PropostaResultado gerarProposta(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        LocalDate dataInicio = LocalDate.of(anoNormalizado, mesNormalizado, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
        Integer idLoja = ligacaoAtiva.getIdLoja().getId();

        validarEstadoAtualDaLoja(idLoja, anoNormalizado, mesNormalizado, dataInicio, dataFim);

        List<Lojautilizador> colaboradoresAtivos = obterColaboradoresAtivos(idLoja);
        if (colaboradoresAtivos.isEmpty()) {
            throw new IllegalArgumentException("Nao e possivel gerar horarios sem equipa ativa na loja.");
        }

        List<Turno> turnos = turnoRepository.findAllByOrderByHoraInicioAsc();
        if (turnos.isEmpty()) {
            throw new IllegalArgumentException("Nao existem turnos base configurados para gerar a proposta.");
        }

        List<RegraAplicada> regras = obterRegrasAplicadas(idLoja);
        ParametrosGeracao parametros = resolverParametrosGeracao(regras, turnos);

        List<DayOff> dayOffsAprovados = dayOffRepository.findPedidosAprovadosDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        List<Preferencia> preferenciasAprovadas = preferenciaRepository.findPreferenciasAprovadasDaLojaEntreDatas(idLoja, dataInicio, dataFim);

        Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador = construirBloqueiosPorUtilizador(
                dataInicio,
                dataFim,
                dayOffsAprovados,
                preferenciasAprovadas
        );
        Map<Integer, List<Preferencia>> preferenciasTurnos = agruparPreferenciasPorTipo(preferenciasAprovadas, "turnos");
        Map<Integer, List<Preferencia>> preferenciasColegas = agruparPreferenciasPorTipo(preferenciasAprovadas, "colegas");

        PlaneamentoGerado planeamento = gerarPlaneamento(
                colaboradoresAtivos,
                turnos,
                parametros,
                bloqueiosPorUtilizador,
                preferenciasTurnos,
                preferenciasColegas,
                dataInicio,
                dataFim
        );

        PropostaHorarioMensal proposta = new PropostaHorarioMensal();
        proposta.setIdLoja(ligacaoAtiva.getIdLoja());
        proposta.setIdUtilizadorGeracao(ligacaoAtiva.getIdUtilizador());
        proposta.setAno(anoNormalizado);
        proposta.setMes(mesNormalizado);
        proposta.setEstado("pendente");
        proposta.setResumoGeracao(criarResumoGeracao(planeamento, parametros));
        proposta.setDataGeracao(LocalDateTime.now());
        proposta = propostaHorarioMensalRepository.save(proposta);

        List<Horario> horariosPersistidos = new ArrayList<>();
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (Horario horario : planeamento.horarios()) {
            horario.setIdPropostaHorario(proposta);
            horario.setEstado("pendente");
            Horario guardado = horarioRepository.save(horario);
            horariosPersistidos.add(guardado);

            HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
            historico.setIdHorario(guardado);
            historico.setEstadoNovo("pendente");
            historico.setDataRegisto(Instant.now());
            historico.setObservacoes("Gerado automaticamente na proposta mensal.");
            historicos.add(historico);
        }
        historicoHorarioEstadoRepository.saveAll(historicos);

        return construirResultado(proposta, horariosPersistidos);
    }

    private PlaneamentoGerado gerarPlaneamento(List<Lojautilizador> colaboradoresAtivos,
                                               List<Turno> turnos,
                                               ParametrosGeracao parametros,
                                               Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                               Map<Integer, List<Preferencia>> preferenciasTurnos,
                                               Map<Integer, List<Preferencia>> preferenciasColegas,
                                               LocalDate dataInicio,
                                               LocalDate dataFim) {
        Map<Integer, EstadoColaborador> estadoPorColaborador = new LinkedHashMap<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            estadoPorColaborador.put(
                    ligacao.getIdUtilizador().getId(),
                    new EstadoColaborador(ligacao)
            );
        }

        List<Horario> horarios = new ArrayList<>();
        Set<LocalDate> diasCobertos = new LinkedHashSet<>();

        for (LocalDate data = dataInicio; !data.isAfter(dataFim); data = data.plusDays(1)) {
            LocalDate dataAtual = data;
            for (Turno turno : turnos) {
                Integer minimoNecessario = parametros.minimosPorTurno().get(turno.getId());
                if (minimoNecessario == null || minimoNecessario <= 0) {
                    throw new IllegalArgumentException("Foi encontrada uma regra minima invalida para um dos turnos.");
                }

                List<EstadoColaborador> candidatos = estadoPorColaborador.values().stream()
                        .filter(estado -> estado.podeReceber(
                                dataAtual,
                                turno,
                                bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                                parametros.maxDiasConsecutivos(),
                                parametros.descansoMinimoHoras()
                        ))
                        .sorted(criarComparatorCandidatos(
                                dataAtual,
                                turno,
                                preferenciasTurnos,
                                preferenciasColegas,
                                horarios
                        ))
                        .toList();

                if (candidatos.size() < minimoNecessario) {
                    throw new IllegalArgumentException(
                            "Nao foi possivel cobrir o turno "
                                    + formatarTurno(turno)
                                    + " em "
                                    + dataAtual
                                    + ". Verifica equipa ativa, descansos e regras minimas."
                    );
                }

                List<EstadoColaborador> selecionados = new ArrayList<>();
                for (EstadoColaborador candidato : candidatos) {
                    if (selecionados.size() >= minimoNecessario) {
                        break;
                    }
                    selecionados.add(candidato);
                }

                for (EstadoColaborador selecionado : selecionados) {
                    Horario horario = new Horario();
                    horario.setIdLojautilizador(selecionado.ligacao());
                    horario.setIdTurno(turno);
                    horario.setDataTurno(dataAtual);
                    horarios.add(horario);

                    selecionado.registarAtribuicao(dataAtual, turno, calcularDuracaoEmMinutos(turno));
                    diasCobertos.add(dataAtual);
                }
            }
        }

        return new PlaneamentoGerado(horarios, new ArrayList<>(estadoPorColaborador.values()), diasCobertos);
    }

    private Comparator<EstadoColaborador> criarComparatorCandidatos(LocalDate data,
                                                                    Turno turno,
                                                                    Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                    Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                    List<Horario> horariosGerados) {
        return Comparator
                .comparing((EstadoColaborador estado) -> !temPreferenciaTurnoFavoravel(
                        preferenciasTurnos.getOrDefault(estado.idUtilizador(), List.of()),
                        data,
                        turno
                ))
                .thenComparing((EstadoColaborador estado) -> !temPreferenciaColegasFavoravel(
                        preferenciasColegas.getOrDefault(estado.idUtilizador(), List.of()),
                        data,
                        horariosGerados
                ))
                .thenComparingInt(EstadoColaborador::turnosAtribuidos)
                .thenComparingLong(EstadoColaborador::minutosAtribuidos)
                .thenComparingInt(estado -> estado.turnosDoTipo(normalizarTurno(turno)))
                .thenComparing(estado -> valorOuTraco(estado.ligacao().getIdUtilizador().getNome()), String.CASE_INSENSITIVE_ORDER);
    }

    private boolean temPreferenciaTurnoFavoravel(List<Preferencia> preferencias, LocalDate data, Turno turno) {
        if (preferencias == null || preferencias.isEmpty()) {
            return false;
        }

        String tipoTurno = normalizarTurno(turno);
        for (Preferencia preferencia : preferencias) {
            if (!preferenciaAtivaNaData(preferencia, data)) {
                continue;
            }

            String descricao = normalizarTexto(preferencia.getDescricao());
            for (String alias : aliasesTurno(tipoTurno)) {
                if (!alias.isBlank() && descricao.contains(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean temPreferenciaColegasFavoravel(List<Preferencia> preferencias, LocalDate data, List<Horario> horariosGerados) {
        if (preferencias == null || preferencias.isEmpty()) {
            return false;
        }

        List<String> nomesNoMesmoDia = horariosGerados.stream()
                .filter(horario -> Objects.equals(horario.getDataTurno(), data))
                .map(horario -> horario.getIdLojautilizador().getIdUtilizador().getNome())
                .filter(Objects::nonNull)
                .map(this::normalizarTexto)
                .toList();

        for (Preferencia preferencia : preferencias) {
            if (!preferenciaAtivaNaData(preferencia, data)) {
                continue;
            }

            String descricao = normalizarTexto(preferencia.getDescricao());
            for (String nome : nomesNoMesmoDia) {
                if (!nome.isBlank() && descricao.contains(nome)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<Integer, Set<LocalDate>> construirBloqueiosPorUtilizador(LocalDate dataInicio,
                                                                         LocalDate dataFim,
                                                                         List<DayOff> dayOffsAprovados,
                                                                         List<Preferencia> preferenciasAprovadas) {
        Map<Integer, Set<LocalDate>> bloqueios = new HashMap<>();

        for (DayOff dayOff : dayOffsAprovados) {
            if (dayOff.getIdUtilizador() == null || dayOff.getDataAusencia() == null) {
                continue;
            }

            bloqueios.computeIfAbsent(dayOff.getIdUtilizador(), ignored -> new LinkedHashSet<>())
                    .add(dayOff.getDataAusencia());
        }

        for (Preferencia preferencia : preferenciasAprovadas) {
            if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
                continue;
            }

            String tipo = normalizarTexto(preferencia.getTipo());
            if (!"folgas".equals(tipo) && !"ferias".equals(tipo)) {
                continue;
            }

            LocalDate inicio = preferencia.getDataInicio() != null ? preferencia.getDataInicio() : dataInicio;
            LocalDate fim = preferencia.getDataFim() != null ? preferencia.getDataFim() : inicio;
            if (inicio == null) {
                continue;
            }

            LocalDate dataAtual = inicio.isBefore(dataInicio) ? dataInicio : inicio;
            LocalDate ultimaData = fim.isAfter(dataFim) ? dataFim : fim;
            if (ultimaData.isBefore(dataAtual)) {
                continue;
            }

            Set<LocalDate> datas = bloqueios.computeIfAbsent(
                    preferencia.getIdUtilizador().getId(),
                    ignored -> new LinkedHashSet<>()
            );
            for (LocalDate data = dataAtual; !data.isAfter(ultimaData); data = data.plusDays(1)) {
                datas.add(data);
            }
        }

        return bloqueios;
    }

    private Map<Integer, List<Preferencia>> agruparPreferenciasPorTipo(List<Preferencia> preferencias, String tipo) {
        Map<Integer, List<Preferencia>> agrupadas = new HashMap<>();
        String tipoNormalizado = normalizarTexto(tipo);

        for (Preferencia preferencia : preferencias) {
            if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
                continue;
            }

            if (!tipoNormalizado.equals(normalizarTexto(preferencia.getTipo()))) {
                continue;
            }

            agrupadas.computeIfAbsent(preferencia.getIdUtilizador().getId(), ignored -> new ArrayList<>())
                    .add(preferencia);
        }

        return agrupadas;
    }

    private ParametrosGeracao resolverParametrosGeracao(List<RegraAplicada> regras, List<Turno> turnos) {
        Integer minimoGenerico = regras.stream()
                .filter(this::ehRegraDeMinimos)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(null);

        Map<Integer, Integer> minimosPorTurno = new LinkedHashMap<>();
        for (Turno turno : turnos) {
            String tipoTurno = normalizarTurno(turno);
            Integer minimoEspecifico = regras.stream()
                    .filter(this::ehRegraDeMinimos)
                    .filter(regra -> regraMencionaTurno(regra, tipoTurno))
                    .map(RegraAplicada::valor)
                    .filter(Objects::nonNull)
                    .filter(valor -> valor > 0)
                    .findFirst()
                    .orElse(null);

            Integer minimo = minimoEspecifico != null ? minimoEspecifico : minimoGenerico;
            if (minimo == null || minimo <= 0) {
                throw new IllegalArgumentException("Nao existe uma regra minima valida para gerar o turno " + formatarTurno(turno) + ".");
            }
            minimosPorTurno.put(turno.getId(), minimo);
        }

        int maxDiasConsecutivos = regras.stream()
                .filter(this::ehRegraDiasConsecutivos)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(5);

        int descansoMinimoHoras = regras.stream()
                .filter(this::ehRegraDescanso)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor > 0)
                .findFirst()
                .orElse(11);

        return new ParametrosGeracao(minimosPorTurno, maxDiasConsecutivos, descansoMinimoHoras);
    }

    private boolean ehRegraDeMinimos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("min") || texto.contains("minim"))
                && (texto.contains("turno") || texto.contains("colaborador") || texto.contains("equipa") || texto.contains("pessoas"));
    }

    private boolean ehRegraDiasConsecutivos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("consecut") || (texto.contains("dias") && texto.contains("seguid"));
    }

    private boolean ehRegraDescanso(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("descanso") && (texto.contains("hora") || texto.contains("interval"));
    }

    private boolean regraMencionaTurno(RegraAplicada regra, String tipoTurno) {
        for (String alias : aliasesTurno(tipoTurno)) {
            if (!alias.isBlank() && regra.textoNormalizado().contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private List<String> aliasesTurno(String tipoTurno) {
        return switch (tipoTurno) {
            case "manha" -> List.of("manha", "abertura", "morning");
            case "tarde" -> List.of("tarde", "afternoon");
            case "noite" -> List.of("noite", "fecho", "night");
            default -> List.of(tipoTurno);
        };
    }

    private List<RegraAplicada> obterRegrasAplicadas(Integer idLoja) {
        Map<Integer, RegrasLoja> overrides = new HashMap<>();
        for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(idLoja)) {
            if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null) {
                overrides.put(regraLoja.getIdRegra().getId(), regraLoja);
            }
        }

        List<RegraAplicada> regras = new ArrayList<>();
        for (Regra regra : regraRepository.findAllByOrderByDescricaoAsc()) {
            RegrasLoja override = overrides.get(regra.getId());
            Integer valor = override != null && override.getValorEspecifico() != null
                    ? override.getValorEspecifico()
                    : regra.getValorPadrao();
            regras.add(new RegraAplicada(
                    valorOuTraco(regra.getDescricao()),
                    valorOuTraco(regra.getTipo()),
                    valor
            ));
        }
        return regras;
    }

    private List<Lojautilizador> obterColaboradoresAtivos(Integer idLoja) {
        Map<Integer, Lojautilizador> ativos = new LinkedHashMap<>();
        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (ligacao.getDataFim() != null || ligacao.getIdUtilizador() == null || ligacao.getIdUtilizador().getId() == null) {
                continue;
            }
            ativos.putIfAbsent(ligacao.getIdUtilizador().getId(), ligacao);
        }
        return new ArrayList<>(ativos.values());
    }

    private void validarEstadoAtualDaLoja(Integer idLoja,
                                          Integer ano,
                                          Integer mes,
                                          LocalDate dataInicio,
                                          LocalDate dataFim) {
        Optional<PropostaHorarioMensal> propostaExistente = propostaHorarioMensalRepository
                .findFirstByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(idLoja, ano, mes);

        if (propostaExistente.isPresent()) {
            String estado = normalizarTexto(propostaExistente.get().getEstado());
            if ("rejeitado".equals(estado)) {
                horarioRepository.deleteByIdPropostaHorarioId(propostaExistente.get().getId());
                propostaHorarioMensalRepository.delete(propostaExistente.get());
            } else {
                throw new IllegalArgumentException("Ja existe uma proposta mensal para o periodo selecionado.");
            }
        }

        long horariosExistentes = horarioRepository.countHorariosVisiveisDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        if (horariosExistentes > 0) {
            throw new IllegalArgumentException("Ja existem horarios publicados neste periodo. Nao e seguro gerar uma nova proposta mensal.");
        }
    }

    private String criarResumoGeracao(PlaneamentoGerado planeamento, ParametrosGeracao parametros) {
        int colaboradoresComTurnos = (int) planeamento.estados().stream()
                .filter(estado -> estado.turnosAtribuidos() > 0)
                .count();
        return "Proposta gerada automaticamente com "
                + planeamento.horarios().size()
                + " turnos distribuidos por "
                + colaboradoresComTurnos
                + " colaboradores. Descanso minimo: "
                + parametros.descansoMinimoHoras()
                + "h. Maximo consecutivo: "
                + parametros.maxDiasConsecutivos()
                + " dias.";
    }

    private PropostaResultado construirResultado(PropostaHorarioMensal proposta, List<Horario> horarios) {
        List<HorarioLinha> linhas = horarios.stream()
                .sorted(Comparator
                        .comparing(Horario::getDataTurno, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(horario -> horario.getIdTurno() != null ? horario.getIdTurno().getHoraInicio() : LocalTime.MIN)
                        .thenComparing(horario -> horario.getIdLojautilizador().getIdUtilizador().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(this::mapearLinhaHorario)
                .toList();

        Map<Integer, ResumoAcumulado> acumulado = new LinkedHashMap<>();
        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null || horario.getIdLojautilizador().getIdUtilizador() == null) {
                continue;
            }
            Integer idColaborador = horario.getIdLojautilizador().getIdUtilizador().getId();
            ResumoAcumulado resumo = acumulado.computeIfAbsent(
                    idColaborador,
                    ignored -> new ResumoAcumulado(
                            idColaborador,
                            valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome()),
                            horario.getIdLojautilizador().getIdCargo() != null
                                    ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome())
                                    : "-"
                    )
            );
            acumulado.put(idColaborador, resumo.comTurno(calcularDuracaoEmMinutos(horario.getIdTurno())));
        }

        List<ResumoColaborador> resumoColaboradores = acumulado.values().stream()
                .sorted(Comparator.comparing(ResumoAcumulado::nomeColaborador, String.CASE_INSENSITIVE_ORDER))
                .map(resumo -> new ResumoColaborador(
                        resumo.idColaborador(),
                        resumo.nomeColaborador(),
                        resumo.cargo(),
                        resumo.turnos(),
                        resumo.minutos(),
                        formatarDuracao(resumo.minutos())
                ))
                .toList();

        Set<LocalDate> diasCobertos = new LinkedHashSet<>();
        for (Horario horario : horarios) {
            if (horario.getDataTurno() != null) {
                diasCobertos.add(horario.getDataTurno());
            }
        }

        return new PropostaResultado(
                proposta.getId(),
                valorOuTraco(proposta.getIdLoja().getNome()),
                proposta.getAno(),
                proposta.getMes(),
                nomeMes(proposta.getMes()),
                formatarEstado(proposta.getEstado()),
                proposta.getResumoGeracao(),
                valorOuTraco(proposta.getIdUtilizadorGeracao().getNome()),
                proposta.getDataGeracao() != null ? DATA_HORA_FORMATTER.format(proposta.getDataGeracao()) : "-",
                linhas,
                resumoColaboradores,
                new ResumoGeral(
                        resumoColaboradores.size(),
                        horarios.size(),
                        diasCobertos.size()
                )
        );
    }

    private HorarioLinha mapearLinhaHorario(Horario horario) {
        String colaborador = horario.getIdLojautilizador() != null && horario.getIdLojautilizador().getIdUtilizador() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome())
                : "-";
        String cargo = horario.getIdLojautilizador() != null && horario.getIdLojautilizador().getIdCargo() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome())
                : "-";

        return new HorarioLinha(
                horario.getDataTurno(),
                horario.getDataTurno() != null ? nomeDiaSemana(horario.getDataTurno()) : "-",
                formatarTurno(horario.getIdTurno()),
                formatarPeriodo(horario.getIdTurno()),
                colaborador,
                cargo,
                formatarEstado(horario.getEstado())
        );
    }

    private Optional<Lojautilizador> obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            return Optional.empty();
        }
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para o utilizador autenticado."));

        if (!temPermissaoDeGeracao(ligacaoAtiva)) {
            throw new IllegalArgumentException("Nao tens permissao para gerar propostas de horario.");
        }
        return ligacaoAtiva;
    }

    private boolean temPermissaoDeGeracao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        return tipoCargo != null && CARGOS_COM_GERACAO.contains(normalizarTexto(tipoCargo));
    }

    private int normalizarAno(Integer ano) {
        int anoAtual = LocalDate.now().getYear();
        if (ano == null || ano < anoAtual || ano > anoAtual + 5) {
            throw new IllegalArgumentException("Seleciona um ano valido para gerar a proposta mensal.");
        }
        return ano;
    }

    private int normalizarMes(Integer mes) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Seleciona um mes valido para gerar a proposta mensal.");
        }
        return mes;
    }

    private boolean preferenciaAtivaNaData(Preferencia preferencia, LocalDate data) {
        LocalDate dataInicio = preferencia.getDataInicio();
        LocalDate dataFim = preferencia.getDataFim();
        if (dataInicio != null && data.isBefore(dataInicio)) {
            return false;
        }
        if (dataFim != null && data.isAfter(dataFim)) {
            return false;
        }
        return true;
    }

    private long calcularDuracaoEmMinutos(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return 0;
        }

        LocalTime horaInicio = turno.getHoraInicio();
        LocalTime horaFim = turno.getHoraFim();
        if (!horaFim.isBefore(horaInicio)) {
            return Duration.between(horaInicio, horaFim).toMinutes();
        }
        return Duration.between(horaInicio, LocalTime.MAX).plusMinutes(1).toMinutes()
                + Duration.between(LocalTime.MIN, horaFim).toMinutes();
    }

    private long calcularHorasDescanso(LocalDate dataAnterior, Turno turnoAnterior, LocalDate dataAtual, Turno turnoAtual) {
        if (dataAnterior == null || turnoAnterior == null || dataAtual == null || turnoAtual == null
                || turnoAnterior.getHoraInicio() == null || turnoAnterior.getHoraFim() == null
                || turnoAtual.getHoraInicio() == null) {
            return Long.MAX_VALUE;
        }

        LocalDateTime fimAnterior = dataAnterior.atTime(turnoAnterior.getHoraFim());
        if (!turnoAnterior.getHoraFim().isAfter(turnoAnterior.getHoraInicio())) {
            fimAnterior = fimAnterior.plusDays(1);
        }

        LocalDateTime inicioAtual = dataAtual.atTime(turnoAtual.getHoraInicio());
        return Duration.between(fimAnterior, inicioAtual).toHours();
    }

    private String normalizarTurno(Turno turno) {
        return normalizarTexto(turno != null && turno.getTipo() != null ? String.valueOf(turno.getTipo()) : "");
    }

    private String formatarTurno(Turno turno) {
        if (turno == null || turno.getTipo() == null) {
            return "-";
        }
        String tipo = String.valueOf(turno.getTipo()).toLowerCase(Locale.ROOT);
        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1);
    }

    private String formatarPeriodo(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return "-";
        }
        return turno.getHoraInicio() + " - " + turno.getHoraFim();
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }
        String valor = estado.trim().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private String formatarDuracao(long minutosTotais) {
        long horas = minutosTotais / 60;
        long minutos = minutosTotais % 60;
        return horas + "h " + minutos + "m";
    }

    private String nomeMes(int mes) {
        return switch (Month.of(mes)) {
            case JANUARY -> "Janeiro";
            case FEBRUARY -> "Fevereiro";
            case MARCH -> "Marco";
            case APRIL -> "Abril";
            case MAY -> "Maio";
            case JUNE -> "Junho";
            case JULY -> "Julho";
            case AUGUST -> "Agosto";
            case SEPTEMBER -> "Setembro";
            case OCTOBER -> "Outubro";
            case NOVEMBER -> "Novembro";
            case DECEMBER -> "Dezembro";
        };
    }

    private String nomeDiaSemana(LocalDate data) {
        return switch (data.getDayOfWeek()) {
            case MONDAY -> "Segunda";
            case TUESDAY -> "Terca";
            case WEDNESDAY -> "Quarta";
            case THURSDAY -> "Quinta";
            case FRIDAY -> "Sexta";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }

    public record GeracaoContexto(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            Integer anoAtual,
            Integer mesAtual,
            boolean existePropostaAtual
    ) {
    }

    public record PropostaResultado(
            Integer idProposta,
            String nomeLoja,
            Integer ano,
            Integer mes,
            String nomeMes,
            String estado,
            String resumoGeracao,
            String geradoPor,
            String dataGeracao,
            List<HorarioLinha> linhas,
            List<ResumoColaborador> resumoColaboradores,
            ResumoGeral resumo
    ) {
    }

    public record HorarioLinha(
            LocalDate data,
            String diaSemana,
            String turno,
            String periodo,
            String colaborador,
            String cargo,
            String estado
    ) {
    }

    public record ResumoColaborador(
            Integer idColaborador,
            String nome,
            String cargo,
            int turnos,
            long minutos,
            String horasFormatadas
    ) {
    }

    public record ResumoGeral(
            int colaboradores,
            int turnos,
            int diasCobertos
    ) {
    }

    private record ParametrosGeracao(
            Map<Integer, Integer> minimosPorTurno,
            int maxDiasConsecutivos,
            int descansoMinimoHoras
    ) {
    }

    private record RegraAplicada(
            String descricao,
            String tipo,
            Integer valor
    ) {
        private String textoNormalizado() {
            return Normalizer.normalize((descricao + " " + tipo), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .trim();
        }
    }

    private record PlaneamentoGerado(
            List<Horario> horarios,
            List<EstadoColaborador> estados,
            Collection<LocalDate> diasCobertos
    ) {
    }

    private record ResumoAcumulado(
            Integer idColaborador,
            String nomeColaborador,
            String cargo,
            int turnos,
            long minutos
    ) {
        private ResumoAcumulado(Integer idColaborador, String nomeColaborador, String cargo) {
            this(idColaborador, nomeColaborador, cargo, 0, 0);
        }

        private ResumoAcumulado comTurno(long minutosTurno) {
            return new ResumoAcumulado(idColaborador, nomeColaborador, cargo, turnos + 1, minutos + minutosTurno);
        }
    }

    private final class EstadoColaborador {
        private final Lojautilizador ligacao;
        private LocalDate ultimaDataAtribuida;
        private int diasConsecutivos;
        private int turnosAtribuidos;
        private long minutosAtribuidos;
        private final Map<LocalDate, Turno> atribuicoes = new HashMap<>();
        private final Map<String, Integer> turnosPorTipo = new HashMap<>();

        private EstadoColaborador(Lojautilizador ligacao) {
            this.ligacao = ligacao;
        }

        private boolean podeReceber(LocalDate data,
                                    Turno turno,
                                    Set<LocalDate> bloqueios,
                                    int maxDiasConsecutivos,
                                    int descansoMinimoHoras) {
            if (bloqueios.contains(data) || atribuicoes.containsKey(data)) {
                return false;
            }

            int novoConsecutivo = 1;
            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                novoConsecutivo = diasConsecutivos + 1;
            }
            if (novoConsecutivo > maxDiasConsecutivos) {
                return false;
            }

            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                long horasDescanso = calcularHorasDescanso(
                        ultimaDataAtribuida,
                        atribuicoes.get(ultimaDataAtribuida),
                        data,
                        turno
                );
                if (horasDescanso < descansoMinimoHoras) {
                    return false;
                }
            }

            return true;
        }

        private void registarAtribuicao(LocalDate data, Turno turno, long minutosTurno) {
            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                diasConsecutivos++;
            } else {
                diasConsecutivos = 1;
            }
            ultimaDataAtribuida = data;
            turnosAtribuidos++;
            minutosAtribuidos += minutosTurno;
            atribuicoes.put(data, turno);
            turnosPorTipo.merge(normalizarTurno(turno), 1, Integer::sum);
        }

        private Integer idUtilizador() {
            return ligacao.getIdUtilizador().getId();
        }

        private Lojautilizador ligacao() {
            return ligacao;
        }

        private int turnosAtribuidos() {
            return turnosAtribuidos;
        }

        private long minutosAtribuidos() {
            return minutosAtribuidos;
        }

        private int turnosDoTipo(String tipo) {
            return turnosPorTipo.getOrDefault(tipo, 0);
        }
    }
}
