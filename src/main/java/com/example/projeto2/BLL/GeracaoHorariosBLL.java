package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.HistoricoHorarioEstado;
import com.example.projeto2.Modules.HorarioEspecialLoja;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Loja;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.PropostaHorarioMensal;
import com.example.projeto2.Modules.Regra;
import com.example.projeto2.Modules.RegrasLoja;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.HorarioEspecialLojaRepository;
import com.example.projeto2.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.PreferenciaRepository;
import com.example.projeto2.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.Repositories.RegraRepository;
import com.example.projeto2.Repositories.RegrasLojaRepository;
import com.example.projeto2.Repositories.TurnoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(GeracaoHorariosBLL.class);
    private static final Set<String> CARGOS_COM_GERACAO = Set.of("gerente", "subgerente");
    private static final Set<String> CARGOS_COM_VALIDACAO = Set.of("supervisor");
    private static final Set<String> CARGOS_COM_PRESENCA_OBRIGATORIA_AO_SABADO = Set.of("gerente", "subgerente");
    private static final String ESTADO_RASCUNHO = "rascunho";
    private static final String ESTADO_PENDENTE = "pendente";
    private static final String ESTADO_APROVADO = "aprovado";
    private static final String ESTADO_REJEITADO = "rejeitado";
    private static final long DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS = 8 * 60L;
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Duration TEMPO_MAXIMO_GERACAO_ALTERNATIVA = Duration.ofSeconds(20);
    private static final int LIMITE_CANDIDATOS_POR_SLOT_BASE = 8;
    private static final int LIMITE_CANDIDATOS_POR_SLOT_ALARGADO = 12;
    private static final int LIMITE_CANDIDATOS_POR_SLOT_EXCECAO = 16;
    private static final int LIMITE_NOS_PESQUISA_BASE = 12_000;
    private static final int LIMITE_NOS_PESQUISA_ALARGADO = 24_000;
    private static final int LIMITE_NOS_PESQUISA_EXCECAO = 40_000;

    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;
    private final PreferenciaRepository preferenciaRepository;
    private final PropostaHorarioMensalRepository propostaHorarioMensalRepository;
    private final HorarioEspecialLojaRepository horarioEspecialLojaRepository;
    private final RegrasLojaRepository regrasLojaRepository;
    private final RegraRepository regraRepository;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    public GeracaoHorariosBLL(LojautilizadorRepository lojautilizadorRepository,
                              HorarioRepository horarioRepository,
                              DayOffRepository dayOffRepository,
                              PreferenciaRepository preferenciaRepository,
                              PropostaHorarioMensalRepository propostaHorarioMensalRepository,
                              HorarioEspecialLojaRepository horarioEspecialLojaRepository,
                              RegrasLojaRepository regrasLojaRepository,
                              RegraRepository regraRepository,
                              TurnoRepository turnoRepository,
                              HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.propostaHorarioMensalRepository = propostaHorarioMensalRepository;
        this.horarioEspecialLojaRepository = horarioEspecialLojaRepository;
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
    public boolean utilizadorPodeValidarHorarios(Integer idUtilizador) {
        return obterLigacaoAtiva(idUtilizador)
                .map(this::temPermissaoDeValidacao)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public GeracaoContexto obterContexto(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComAcessoAoPainel(idUtilizador);
        LocalDate hoje = LocalDate.now();

        return new GeracaoContexto(
                ligacaoAtiva.getIdLoja().getId(),
                valorOuTraco(ligacaoAtiva.getIdLoja().getNome()),
                valorOuTraco(ligacaoAtiva.getIdLoja().getLocalizacao()),
                hoje.getYear(),
                hoje.getMonthValue(),
                temPermissaoDeGeracao(ligacaoAtiva),
                temPermissaoDeValidacao(ligacaoAtiva),
                !obterPropostasVisiveis(ligacaoAtiva, hoje.getYear(), hoje.getMonthValue()).isEmpty()
        );
    }

    @Transactional(readOnly = true)
    public PropostaResultado obterProposta(Integer idUtilizador, Integer ano, Integer mes) {
        return obterPlaneamento(idUtilizador, ano, mes);
    }

    @Transactional(readOnly = true)
    public PropostaResultado obterPlaneamento(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComAcessoAoPainel(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        Optional<PropostaHorarioMensal> proposta = obterPropostasVisiveis(
                ligacaoAtiva,
                anoNormalizado,
                mesNormalizado
        ).stream().findFirst();

        if (proposta.isPresent()) {
            return construirResultado(proposta.get(), horarioRepository.findByIdPropostaHorarioId(proposta.get().getId()));
        }

        LocalDate dataInicio = LocalDate.of(anoNormalizado, mesNormalizado, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
        List<Horario> horariosPublicados = horarioRepository.findHorariosDaLojaEntreDatas(
                ligacaoAtiva.getIdLoja().getId(),
                dataInicio,
                dataFim
        );
        if (horariosPublicados.isEmpty()) {
            return null;
        }

        return construirResultadoHorariosPublicados(ligacaoAtiva.getIdLoja(), anoNormalizado, mesNormalizado, horariosPublicados);
    }

    @Transactional
    public PropostaResultado aprovarProposta(Integer idUtilizador, Integer idProposta, String observacoesSupervisor) {
        return decidirProposta(idUtilizador, idProposta, "aprovado", observacoesSupervisor);
    }

    @Transactional
    public PropostaResultado rejeitarProposta(Integer idUtilizador, Integer idProposta, String observacoesSupervisor) {
        return decidirProposta(idUtilizador, idProposta, "rejeitado", observacoesSupervisor);
    }

    @Transactional(readOnly = true)
    public List<ColaboradorElegivel> listarColaboradoresElegiveis(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);
        LocalDate dataInicio = LocalDate.of(anoNormalizado, mesNormalizado, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());

        return obterColaboradoresElegiveis(ligacaoAtiva.getIdLoja().getId(), dataInicio, dataFim).stream()
                .map(ligacao -> new ColaboradorElegivel(
                        ligacao.getIdUtilizador().getId(),
                        valorOuTraco(ligacao.getIdUtilizador().getNome()),
                        ligacao.getIdCargo() != null ? valorOuTraco(ligacao.getIdCargo().getNome()) : "-",
                        resolverPerfilContratual(ligacao)
                                .map(PerfilContratual::descricaoCurta)
                                .orElse("-"),
                        formatarPeriodoVinculo(ligacao),
                        true
                ))
                .toList();
    }

    @Transactional
    public List<PropostaResultado> enviarPropostasParaValidacao(Integer idUtilizador, Collection<Integer> idsPropostas) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Set<Integer> idsNormalizados = normalizarIds(idsPropostas);
        if (idsNormalizados.isEmpty()) {
            throw new IllegalArgumentException("Seleciona pelo menos uma alternativa para enviar ao supervisor.");
        }

        List<PropostaResultado> resultados = new ArrayList<>();
        for (Integer idProposta : idsNormalizados) {
            PropostaHorarioMensal proposta = propostaHorarioMensalRepository.findByIdAndIdLojaId(
                            idProposta,
                            ligacaoAtiva.getIdLoja().getId()
                    )
                    .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada a proposta #" + idProposta + " na tua loja."));

            String estadoAtual = normalizarTexto(proposta.getEstado());
            if (ESTADO_APROVADO.equals(estadoAtual) || ESTADO_REJEITADO.equals(estadoAtual)) {
                throw new IllegalArgumentException("A proposta #" + idProposta + " ja foi decidida pelo supervisor.");
            }
            if (!ESTADO_RASCUNHO.equals(estadoAtual) && !ESTADO_PENDENTE.equals(estadoAtual)) {
                throw new IllegalArgumentException("A proposta #" + idProposta + " esta num estado que nao permite envio ao supervisor.");
            }

            if (ESTADO_RASCUNHO.equals(estadoAtual)) {
                proposta.setEstado(ESTADO_PENDENTE);
                proposta = propostaHorarioMensalRepository.save(proposta);
            }
            resultados.add(construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId())));
        }
        return resultados;
    }

    @Transactional
    public PropostaResultado gerarProposta(Integer idUtilizador, Integer ano, Integer mes) {
        return gerarPropostas(idUtilizador, ano, mes, 1).getFirst();
    }

    @Transactional
    public List<PropostaResultado> gerarPropostas(Integer idUtilizador, Integer ano, Integer mes, Integer quantidade) {
        return gerarPropostas(idUtilizador, ano, mes, quantidade, null);
    }

    @Transactional
    public PropostaResultado gerarProposta(Integer idUtilizador,
                                           Integer ano,
                                           Integer mes,
                                           Collection<Integer> idsColaboradoresSelecionados) {
        return gerarPropostas(idUtilizador, ano, mes, 1, idsColaboradoresSelecionados).getFirst();
    }

    @Transactional
    public List<PropostaResultado> gerarPropostas(Integer idUtilizador,
                                                  Integer ano,
                                                  Integer mes,
                                                  Integer quantidade,
                                                  Collection<Integer> idsColaboradoresSelecionados) {
        DadosGeracao dados = prepararDadosGeracao(idUtilizador, ano, mes, idsColaboradoresSelecionados);
        int quantidadeNormalizada = normalizarQuantidadeAlternativas(quantidade);
        int alternativasExistentes = Math.toIntExact(Math.min(
                Integer.MAX_VALUE,
                propostaHorarioMensalRepository.countByIdLojaIdAndAnoAndMes(
                        dados.loja().getId(),
                        dados.ano(),
                        dados.mes()
                )
        ));

        List<PropostaResultado> resultados = new ArrayList<>();
        LOGGER.info(
                "A gerar {} alternativa(s) para a loja {} no periodo {}/{}.",
                quantidadeNormalizada,
                valorOuTraco(dados.loja().getNome()),
                String.format(Locale.ROOT, "%02d", dados.mes()),
                dados.ano()
        );
        for (int indice = 0; indice < quantidadeNormalizada; indice++) {
            PoliticaOtimizacao politica = PoliticaOtimizacao.porIndice(alternativasExistentes + indice);
            Instant inicioGeracao = Instant.now();
            PlaneamentoGerado planeamento = gerarPlaneamento(
                    dados.colaboradoresAtivos(),
                    dados.turnos(),
                    dados.parametros(),
                    dados.historicoHorarios(),
                    dados.bloqueiosPorUtilizador(),
                    dados.preferenciasTurnos(),
                    dados.preferenciasColegas(),
                    dados.configuracoesPorData(),
                    dados.dataInicio(),
                    dados.dataFim(),
                    politica,
                    inicioGeracao.plus(TEMPO_MAXIMO_GERACAO_ALTERNATIVA)
            );

            MetricasPlaneamento metricas = calcularMetricasPlaneamento(planeamento, politica);
            PropostaHorarioMensal proposta = persistirProposta(dados.ligacaoAtiva(), dados.ano(), dados.mes(), planeamento, politica, metricas);
            List<Horario> horariosPersistidos = horarioRepository.findByIdPropostaHorarioId(proposta.getId());
            LOGGER.info(
                    "Alternativa {} gerada para {}/{} em {} ms com a politica {}.",
                    proposta.getId(),
                    String.format(Locale.ROOT, "%02d", dados.mes()),
                    dados.ano(),
                    Duration.between(inicioGeracao, Instant.now()).toMillis(),
                    politica.nome()
            );
            resultados.add(construirResultado(proposta, horariosPersistidos));
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public List<PropostaResumo> listarPropostas(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComAcessoAoPainel(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        return obterPropostasVisiveis(ligacaoAtiva, anoNormalizado, mesNormalizado)
                .stream()
                .map(proposta -> construirResumoProposta(
                        proposta,
                        construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()))
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public PropostaResultado obterPropostaPorId(Integer idUtilizador, Integer idProposta) {
        if (idProposta == null) {
            throw new IllegalArgumentException("Seleciona uma proposta para consultar.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComAcessoAoPainel(idUtilizador);
        PropostaHorarioMensal proposta = propostaHorarioMensalRepository.findByIdAndIdLojaId(
                        idProposta,
                        ligacaoAtiva.getIdLoja().getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada nenhuma proposta para a tua loja com esse identificador."));
        if (!propostaVisivelParaUtilizador(ligacaoAtiva, proposta)) {
            throw new IllegalArgumentException("Esta proposta ainda nao foi enviada ao supervisor.");
        }

        return construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()));
    }

    @Transactional(readOnly = true)
    public ComparacaoPropostas compararPropostas(Integer idUtilizador, Integer idPropostaBase, Integer idPropostaComparada) {
        PropostaResultado base = obterPropostaPorId(idUtilizador, idPropostaBase);
        PropostaResultado comparada = obterPropostaPorId(idUtilizador, idPropostaComparada);
        if (!Objects.equals(base.ano(), comparada.ano()) || !Objects.equals(base.mes(), comparada.mes())) {
            throw new IllegalArgumentException("Seleciona duas propostas do mesmo mes para comparar.");
        }

        Map<Integer, ResumoColaborador> basePorColaborador = indexarResumoPorColaborador(base.resumoColaboradores());
        Map<Integer, ResumoColaborador> comparadaPorColaborador = indexarResumoPorColaborador(comparada.resumoColaboradores());
        Set<Integer> idsColaboradores = new LinkedHashSet<>();
        idsColaboradores.addAll(basePorColaborador.keySet());
        idsColaboradores.addAll(comparadaPorColaborador.keySet());

        List<DiferencaColaborador> diferencas = idsColaboradores.stream()
                .map(idColaborador -> construirDiferencaColaborador(
                        basePorColaborador.get(idColaborador),
                        comparadaPorColaborador.get(idColaborador)
                ))
                .sorted(Comparator.comparing(DiferencaColaborador::colaborador, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<String, String> atribuicoesBase = indexarAtribuicoesPorSlot(base.linhas());
        Map<String, String> atribuicoesComparada = indexarAtribuicoesPorSlot(comparada.linhas());
        Set<String> slots = new LinkedHashSet<>();
        slots.addAll(atribuicoesBase.keySet());
        slots.addAll(atribuicoesComparada.keySet());

        int turnosDiferentes = 0;
        Set<LocalDate> diasAfetados = new LinkedHashSet<>();
        for (String slot : slots) {
            if (!Objects.equals(atribuicoesBase.get(slot), atribuicoesComparada.get(slot))) {
                turnosDiferentes++;
                extrairDataDoSlot(slot).ifPresent(diasAfetados::add);
            }
        }

        String resumo = "Comparacao entre "
                + rotuloCurtoProposta(base)
                + " e "
                + rotuloCurtoProposta(comparada)
                + ": "
                + turnosDiferentes
                + " turnos mudam de colaborador em "
                + diasAfetados.size()
                + " dias. Pontuacao IO "
                + base.metricas().pontuacao()
                + " vs "
                + comparada.metricas().pontuacao()
                + " (menor e melhor).";

        return new ComparacaoPropostas(
                base.idProposta(),
                comparada.idProposta(),
                rotuloCurtoProposta(base),
                rotuloCurtoProposta(comparada),
                resumo,
                turnosDiferentes,
                diasAfetados.size(),
                diferencas
        );
    }

    private List<PropostaHorarioMensal> obterPropostasVisiveis(Lojautilizador ligacaoAtiva, int ano, int mes) {
        return propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        ligacaoAtiva.getIdLoja().getId(),
                        ano,
                        mes
                )
                .stream()
                .filter(proposta -> propostaVisivelParaUtilizador(ligacaoAtiva, proposta))
                .toList();
    }

    private boolean propostaVisivelParaUtilizador(Lojautilizador ligacaoAtiva, PropostaHorarioMensal proposta) {
        if (temPermissaoDeGeracao(ligacaoAtiva)) {
            return true;
        }
        return !ESTADO_RASCUNHO.equals(normalizarTexto(proposta.getEstado()));
    }

    private DadosGeracao prepararDadosGeracao(Integer idUtilizador,
                                              Integer ano,
                                              Integer mes,
                                              Collection<Integer> idsColaboradoresSelecionados) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        LocalDate dataInicio = LocalDate.of(anoNormalizado, mesNormalizado, 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
        Integer idLoja = loja.getId();

        validarEstadoAtualDaLoja(idLoja, anoNormalizado, mesNormalizado, dataInicio, dataFim);

        List<Lojautilizador> colaboradoresAtivos = filtrarColaboradoresSelecionados(
                obterColaboradoresElegiveis(idLoja, dataInicio, dataFim),
                idsColaboradoresSelecionados
        );
        if (colaboradoresAtivos.isEmpty()) {
            throw new IllegalArgumentException("Nao e possivel gerar horarios sem colaboradores elegiveis e com vinculo valido na loja.");
        }

        List<Turno> turnos = turnoRepository.findAllByOrderByHoraInicioAsc();
        if (turnos.isEmpty()) {
            throw new IllegalArgumentException("Nao existem turnos base configurados para gerar a proposta.");
        }

        List<RegraAplicada> regras = obterRegrasAplicadas(idLoja);
        ParametrosGeracao parametros = resolverParametrosGeracao(regras, turnos);
        validarJanelaDeLancamento(dataInicio, parametros.diaLimiteLancamento());
        LocalDate inicioHistorico = resolverInicioHistoricoParaGeracao(dataInicio, parametros);

        List<DayOff> dayOffsAprovados = dayOffRepository.findPedidosAprovadosDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        List<Preferencia> preferenciasAprovadas = preferenciaRepository.findPreferenciasAprovadasDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        List<HorarioEspecialLoja> horariosEspeciais = horarioEspecialLojaRepository.findAtivosNoPeriodo(idLoja, dataInicio, dataFim);
        List<Horario> historicoHorarios = inicioHistorico.isBefore(dataInicio)
                ? horarioRepository.findHorariosDaLojaEntreDatas(idLoja, inicioHistorico, dataInicio.minusDays(1))
                : List.of();

        Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador = construirBloqueiosPorUtilizador(
                dataInicio,
                dataFim,
                dayOffsAprovados,
                preferenciasAprovadas
        );
        Map<Integer, List<Preferencia>> preferenciasTurnos = agruparPreferenciasPorTipo(preferenciasAprovadas, "turnos");
        Map<Integer, List<Preferencia>> preferenciasColegas = agruparPreferenciasPorTipo(preferenciasAprovadas, "colegas");
        Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData = construirConfiguracoesEspeciaisPorData(
                loja,
                turnos,
                horariosEspeciais
        );

        return new DadosGeracao(
                ligacaoAtiva,
                loja,
                anoNormalizado,
                mesNormalizado,
                dataInicio,
                dataFim,
                colaboradoresAtivos,
                turnos,
                parametros,
                historicoHorarios,
                bloqueiosPorUtilizador,
                preferenciasTurnos,
                preferenciasColegas,
                configuracoesPorData
        );
    }

    private PropostaHorarioMensal persistirProposta(Lojautilizador ligacaoAtiva,
                                                    int anoNormalizado,
                                                    int mesNormalizado,
                                                    PlaneamentoGerado planeamento,
                                                    PoliticaOtimizacao politica,
                                                    MetricasPlaneamento metricas) {
        PropostaHorarioMensal proposta = new PropostaHorarioMensal();
        proposta.setIdLoja(ligacaoAtiva.getIdLoja());
        proposta.setIdUtilizadorGeracao(ligacaoAtiva.getIdUtilizador());
        proposta.setAno(anoNormalizado);
        proposta.setMes(mesNormalizado);
        proposta.setEstado(ESTADO_RASCUNHO);
        proposta.setResumoGeracao(criarResumoGeracao(planeamento, politica, metricas));
        proposta.setDataGeracao(LocalDateTime.now());
        proposta = propostaHorarioMensalRepository.save(proposta);

        List<Horario> horariosPersistidos = new ArrayList<>();
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (Horario horario : planeamento.horarios()) {
            horario.setIdPropostaHorario(proposta);
            horario.setEstado(ESTADO_PENDENTE);
            Horario guardado = horarioRepository.save(horario);
            horariosPersistidos.add(guardado);

            HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
            historico.setIdHorario(guardado);
            historico.setEstadoNovo(ESTADO_PENDENTE);
            historico.setDataRegisto(Instant.now());
            historico.setObservacoes("Gerado automaticamente na proposta mensal.");
            historicos.add(historico);
        }
        historicoHorarioEstadoRepository.saveAll(historicos);

        return proposta;
    }

    private PropostaResultado decidirProposta(Integer idUtilizador,
                                              Integer idProposta,
                                              String novoEstado,
                                              String observacoesSupervisor) {
        if (idProposta == null) {
            throw new IllegalArgumentException("Seleciona uma proposta antes de tomar uma decisao.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissaoDeValidacao(idUtilizador);
        PropostaHorarioMensal proposta = propostaHorarioMensalRepository.findByIdAndIdLojaId(
                        idProposta,
                        ligacaoAtiva.getIdLoja().getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada nenhuma proposta para a tua loja com esse identificador."));

        if (ESTADO_RASCUNHO.equals(normalizarTexto(proposta.getEstado()))) {
            throw new IllegalArgumentException("Esta proposta ainda esta em rascunho. O gerente tem de a enviar ao supervisor antes da validacao.");
        }

        if (!ESTADO_PENDENTE.equals(normalizarTexto(proposta.getEstado()))) {
            throw new IllegalArgumentException("Esta proposta ja foi decidida e nao pode voltar a ser alterada.");
        }

        if (ESTADO_APROVADO.equals(normalizarTexto(novoEstado))) {
            validarAprovacaoSemConflitos(proposta);
        }

        proposta.setEstado(novoEstado);
        proposta.setIdUtilizadorDecisao(ligacaoAtiva.getIdUtilizador());
        proposta.setDataDecisao(LocalDateTime.now());
        proposta.setObservacoesSupervisor(limparTexto(observacoesSupervisor));
        proposta = propostaHorarioMensalRepository.save(proposta);

        List<Horario> horarios = horarioRepository.findByIdPropostaHorarioId(proposta.getId());
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (Horario horario : horarios) {
            horario.setEstado(novoEstado);

            HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
            historico.setIdHorario(horario);
            historico.setEstadoNovo(novoEstado);
            historico.setDataRegisto(Instant.now());
            historico.setObservacoes(criarObservacaoHistoricoDecisao(proposta, novoEstado));
            historicos.add(historico);
        }

        horarioRepository.saveAll(horarios);
        historicoHorarioEstadoRepository.saveAll(historicos);
        if (ESTADO_APROVADO.equals(normalizarTexto(novoEstado))) {
            rejeitarPropostasPendentesConcorrentes(proposta, ligacaoAtiva.getIdUtilizador());
        }

        return construirResultado(proposta, horarios);
    }

    private void validarAprovacaoSemConflitos(PropostaHorarioMensal proposta) {
        List<PropostaHorarioMensal> propostasDoPeriodo = propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        proposta.getIdLoja().getId(),
                        proposta.getAno(),
                        proposta.getMes()
                );
        boolean existeOutraAprovada = propostasDoPeriodo.stream()
                .filter(outra -> !Objects.equals(outra.getId(), proposta.getId()))
                .anyMatch(outra -> ESTADO_APROVADO.equals(normalizarTexto(outra.getEstado())));
        if (existeOutraAprovada) {
            throw new IllegalArgumentException("Ja existe uma proposta aprovada para este periodo.");
        }

        LocalDate dataInicio = LocalDate.of(proposta.getAno(), proposta.getMes(), 1);
        LocalDate dataFim = dataInicio.withDayOfMonth(dataInicio.lengthOfMonth());
        long horariosVisiveis = horarioRepository.countHorariosVisiveisDaLojaEntreDatas(
                proposta.getIdLoja().getId(),
                dataInicio,
                dataFim
        );
        if (horariosVisiveis > 0) {
            throw new IllegalArgumentException("Ja existem horarios publicados neste periodo. Nao e seguro publicar outra alternativa.");
        }
    }

    private void rejeitarPropostasPendentesConcorrentes(PropostaHorarioMensal propostaAprovada, com.example.projeto2.Modules.Utilizador decisor) {
        List<PropostaHorarioMensal> propostasDoPeriodo = propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(
                        propostaAprovada.getIdLoja().getId(),
                        propostaAprovada.getAno(),
                        propostaAprovada.getMes()
                );

        List<PropostaHorarioMensal> propostasParaRejeitar = propostasDoPeriodo.stream()
                .filter(proposta -> !Objects.equals(proposta.getId(), propostaAprovada.getId()))
                .filter(proposta -> ESTADO_PENDENTE.equals(normalizarTexto(proposta.getEstado())))
                .toList();
        if (propostasParaRejeitar.isEmpty()) {
            return;
        }

        List<Horario> horariosParaAtualizar = new ArrayList<>();
        List<HistoricoHorarioEstado> historicos = new ArrayList<>();
        for (PropostaHorarioMensal proposta : propostasParaRejeitar) {
            proposta.setEstado(ESTADO_REJEITADO);
            proposta.setIdUtilizadorDecisao(decisor);
            proposta.setDataDecisao(LocalDateTime.now());
            proposta.setObservacoesSupervisor(
                    "Rejeitada automaticamente porque a proposta #"
                            + propostaAprovada.getId()
                            + " foi aprovada para o mesmo periodo."
            );

            for (Horario horario : horarioRepository.findByIdPropostaHorarioId(proposta.getId())) {
                horario.setEstado(ESTADO_REJEITADO);
                horariosParaAtualizar.add(horario);

                HistoricoHorarioEstado historico = new HistoricoHorarioEstado();
                historico.setIdHorario(horario);
                historico.setEstadoNovo(ESTADO_REJEITADO);
                historico.setDataRegisto(Instant.now());
                historico.setObservacoes("Rejeitado automaticamente apos aprovacao de uma alternativa concorrente.");
                historicos.add(historico);
            }
        }

        propostaHorarioMensalRepository.saveAll(propostasParaRejeitar);
        horarioRepository.saveAll(horariosParaAtualizar);
        historicoHorarioEstadoRepository.saveAll(historicos);
    }

    private PlaneamentoGerado gerarPlaneamento(List<Lojautilizador> colaboradoresAtivos,
                                               List<Turno> turnos,
                                               ParametrosGeracao parametros,
                                               List<Horario> historicoHorarios,
                                               Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                               Map<Integer, List<Preferencia>> preferenciasTurnos,
                                               Map<Integer, List<Preferencia>> preferenciasColegas,
                                               Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData,
                                               LocalDate dataInicio,
                                               LocalDate dataFim,
                                               PoliticaOtimizacao politica,
                                               Instant prazoLimiteGeracao) {
        Map<Integer, EstadoColaborador> estadoPorColaborador = new LinkedHashMap<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            PerfilContratual perfilContratual = resolverPerfilContratual(ligacao)
                    .orElseThrow(() -> new IllegalArgumentException("Foi encontrado um colaborador sem perfil contratual valido para a geracao."));
            Long cargaMaximaMinutos = parametros.cargaMaximaMinutosPorPerfil().get(perfilContratual);
            if (cargaMaximaMinutos == null || cargaMaximaMinutos <= 0) {
                throw new IllegalArgumentException("Nao existe uma carga contratual mensal valida para o perfil " + perfilContratual.descricaoCurta() + ".");
            }

            estadoPorColaborador.put(
                    ligacao.getIdUtilizador().getId(),
                    new EstadoColaborador(ligacao, perfilContratual, cargaMaximaMinutos)
            );
        }

        Map<Integer, List<Horario>> historicoPorUtilizador = new HashMap<>();
        for (Horario horario : historicoHorarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null
                    || horario.getDataTurno() == null
                    || horario.getIdTurno() == null) {
                continue;
            }
            historicoPorUtilizador
                    .computeIfAbsent(horario.getIdLojautilizador().getIdUtilizador().getId(), ignored -> new ArrayList<>())
                    .add(horario);
        }
        for (List<Horario> historicoDoColaborador : historicoPorUtilizador.values()) {
            historicoDoColaborador.sort(Comparator
                    .comparing(Horario::getDataTurno)
                    .thenComparing(horario -> horario.getIdTurno().getHoraInicio(), Comparator.nullsLast(Comparator.naturalOrder())));
        }
        for (EstadoColaborador estado : estadoPorColaborador.values()) {
            estado.inicializarComHistorico(historicoPorUtilizador.getOrDefault(estado.idUtilizador(), List.of()));
        }

        List<Horario> horarios = new ArrayList<>();
        Set<LocalDate> diasCobertos = new LinkedHashSet<>();
        Set<LocalDate> sabadosComChefia = new LinkedHashSet<>();
        Map<LocalDate, ReservaOperacionalFimDeSemana> reservaOperacionalFimDeSemanaPorSemana = new HashMap<>();

        for (LocalDate data = dataInicio; !data.isAfter(dataFim); data = data.plusDays(1)) {
            validarPrazoGeracao(prazoLimiteGeracao, dataInicio, politica);
            LocalDate dataAtual = data;
            LocalDate inicioSemanaAtual = inicioSemana(dataAtual);
            ConfiguracaoDiaEspecial configuracaoDia = configuracoesPorData.get(dataAtual);
            if (configuracaoDia != null && configuracaoDia.lojaEncerrada()) {
                continue;
            }

            ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana = construirContextoPreservacaoFimDeSemana(
                    dataAtual,
                    dataFim,
                    turnos,
                    parametros,
                    configuracoesPorData
            );

            ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana = reservaOperacionalFimDeSemanaPorSemana.computeIfAbsent(
                    inicioSemanaAtual,
                    ignored -> identificarReservaOperacionalFimDeSemana(
                            estadoPorColaborador.values(),
                            turnos,
                            parametros,
                            bloqueiosPorUtilizador,
                            configuracoesPorData,
                            inicioSemanaAtual,
                            dataFim
                    )
            );

            List<Turno> turnosDoDia = configuracaoDia != null
                    ? configuracaoDia.turnosCompativeis()
                    : turnos;
            if (turnosDoDia.isEmpty()) {
                throw new IllegalArgumentException(
                        "Nao existem turnos base compativeis com a excecao \""
                                + configuracaoDia.descricao()
                                + "\" em "
                                + dataAtual
                                + "."
                );
            }

            List<AtribuicaoPlaneadaDia> atribuicoesDoDia = planearAtribuicoesDoDia(
                    dataAtual,
                    turnosDoDia,
                    configuracaoDia,
                    estadoPorColaborador,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana,
                    contextoPreservacaoFimDeSemana,
                    preferenciasTurnos,
                    preferenciasColegas,
                    horarios,
                    sabadosComChefia.contains(dataAtual),
                    politica,
                    prazoLimiteGeracao
            );

            for (AtribuicaoPlaneadaDia atribuicao : atribuicoesDoDia) {
                Horario horario = new Horario();
                horario.setIdLojautilizador(atribuicao.estado().ligacao());
                horario.setIdTurno(atribuicao.turno());
                horario.setDataTurno(dataAtual);
                horarios.add(horario);

                atribuicao.estado().registarAtribuicao(dataAtual, atribuicao.turno(), atribuicao.minutosTurno());
                if (dataAtual.getDayOfWeek() == DayOfWeek.SATURDAY && atribuicao.estado().cumpreChefiaObrigatoriaAoSabado()) {
                    sabadosComChefia.add(dataAtual);
                }
                diasCobertos.add(dataAtual);
            }
        }

        return new PlaneamentoGerado(horarios, new ArrayList<>(estadoPorColaborador.values()), diasCobertos);
    }

    private String diagnosticoDisponibilidade(Collection<EstadoColaborador> estados,
                                              LocalDate data,
                                              Turno turno,
                                              long minutosTurno,
                                              Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                              ParametrosGeracao parametros,
                                              ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        List<String> detalhes = new ArrayList<>();
        for (EstadoColaborador estado : estados) {
            List<String> motivos = estado.motivosIndisponibilidade(
                    data,
                    turno,
                    minutosTurno,
                    bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            );
            if (motivos.isEmpty()) {
                detalhes.add(
                        valorOuTraco(estado.ligacao().getIdUtilizador().getNome())
                                + "=ok"
                                + "(semana=" + estado.diasTrabalhadosNaSemana(data)
                                + ",min=" + estado.minutosAtribuidos()
                                + ",fds=" + estado.totalFinsDeSemanaTrabalhados()
                                + ")"
                );
            } else {
                detalhes.add(
                        valorOuTraco(estado.ligacao().getIdUtilizador().getNome())
                                + "="
                                + String.join("+", motivos)
                                + "(semana=" + estado.diasTrabalhadosNaSemana(data)
                                + ",min=" + estado.minutosAtribuidos()
                                + ",fds=" + estado.totalFinsDeSemanaTrabalhados()
                                + ")"
                );
            }
        }
        return String.join("; ", detalhes);
    }

    private List<AtribuicaoPlaneadaDia> planearAtribuicoesDoDia(LocalDate data,
                                                                List<Turno> turnosDoDia,
                                                                ConfiguracaoDiaEspecial configuracaoDia,
                                                                Map<Integer, EstadoColaborador> estadoPorColaborador,
                                                                Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                ParametrosGeracao parametros,
                                                                ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                                Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                List<Horario> horariosGerados,
                                                                boolean sabadoJaTemChefia,
                                                                PoliticaOtimizacao politica,
                                                                Instant prazoLimiteGeracao) {
        List<SlotTurnoDia> slots = construirSlotsDoDia(turnosDoDia, configuracaoDia, parametros);

        boolean precisaChefiaAoSabado = parametros.exigirChefiaAoSabado()
                && data.getDayOfWeek() == DayOfWeek.SATURDAY
                && !sabadoJaTemChefia;

        if (precisaChefiaAoSabado) {
            if (!existeChefiaPossivelNoDia(
                    data,
                    slots,
                    estadoPorColaborador.values(),
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana
            )) {
                throw new IllegalArgumentException(
                        "Nao foi possivel garantir presenca de gerente ou subgerente no sabado "
                                + DATA_FORMATTER.format(data)
                                + "."
                );
            }
        }

        int minimoChefiasNoDia = ehFimDeSemana(data) ? 1 : 0;
        if (precisaChefiaAoSabado) {
            minimoChefiasNoDia = Math.max(minimoChefiasNoDia, 1);
        }

        ResultadoTentativaDistribuicao tentativaBase = executarTentativaDistribuicaoDoDia(
                data,
                slots,
                estadoPorColaborador,
                bloqueiosPorUtilizador,
                parametros,
                reservaOperacionalFimDeSemana,
                contextoPreservacaoFimDeSemana,
                preferenciasTurnos,
                preferenciasColegas,
                horariosGerados,
                politica,
                true,
                false,
                false,
                minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_BASE,
                LIMITE_NOS_PESQUISA_BASE,
                prazoLimiteGeracao
        );
        if (tentativaBase.encontrou()) {
            return tentativaBase.atribuicoes();
        }
        registarTentativaLimitada(tentativaBase, data, "base");

        ResultadoTentativaDistribuicao tentativaAlargada = executarTentativaDistribuicaoDoDia(
                data,
                slots,
                estadoPorColaborador,
                bloqueiosPorUtilizador,
                parametros,
                reservaOperacionalFimDeSemana,
                contextoPreservacaoFimDeSemana,
                preferenciasTurnos,
                preferenciasColegas,
                horariosGerados,
                politica,
                true,
                false,
                false,
                minimoChefiasNoDia,
                LIMITE_CANDIDATOS_POR_SLOT_ALARGADO,
                LIMITE_NOS_PESQUISA_ALARGADO,
                prazoLimiteGeracao
        );
        if (tentativaAlargada.encontrou()) {
            return tentativaAlargada.atribuicoes();
        }
        registarTentativaLimitada(tentativaAlargada, data, "alargada");

        ResultadoTentativaDistribuicao ultimaTentativa = tentativaAlargada;
        boolean existeSlotSemCandidatosPorPreservacao = tentativaBase.candidatosPorSlot().values().stream().anyMatch(List::isEmpty);
        if (contextoPreservacaoFimDeSemana != null
                && (ehFimDeSemana(data) || existeSlotSemCandidatosPorPreservacao)) {
            ResultadoTentativaDistribuicao tentativaPreservacaoRelaxada = executarTentativaDistribuicaoDoDia(
                    data,
                    slots,
                    estadoPorColaborador,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana,
                    contextoPreservacaoFimDeSemana,
                    preferenciasTurnos,
                    preferenciasColegas,
                    horariosGerados,
                    politica,
                    false,
                    false,
                    false,
                    minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_ALARGADO,
                    LIMITE_NOS_PESQUISA_ALARGADO,
                    prazoLimiteGeracao
            );
            ultimaTentativa = tentativaPreservacaoRelaxada;
            if (tentativaPreservacaoRelaxada.encontrou()) {
                return tentativaPreservacaoRelaxada.atribuicoes();
            }
            registarTentativaLimitada(tentativaPreservacaoRelaxada, data, "sem-preservacao");
        }

        if (ehFimDeSemana(data) && parametros.janelaRotacaoFinsDeSemanaSemanas() >= 2) {
            ResultadoTentativaDistribuicao tentativaRotacaoRelaxada = executarTentativaDistribuicaoDoDia(
                    data,
                    slots,
                    estadoPorColaborador,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana,
                    contextoPreservacaoFimDeSemana,
                    preferenciasTurnos,
                    preferenciasColegas,
                    horariosGerados,
                    politica,
                    false,
                    true,
                    false,
                    minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO,
                    LIMITE_NOS_PESQUISA_EXCECAO,
                    prazoLimiteGeracao
            );
            ultimaTentativa = tentativaRotacaoRelaxada;
            if (tentativaRotacaoRelaxada.encontrou()) {
                return tentativaRotacaoRelaxada.atribuicoes();
            }
            registarTentativaLimitada(tentativaRotacaoRelaxada, data, "rotacao-relaxada");
        }

        if (ehFimDeSemana(data)) {
            ResultadoTentativaDistribuicao tentativaExcecaoOperacional = executarTentativaDistribuicaoDoDia(
                    data,
                    slots,
                    estadoPorColaborador,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana,
                    contextoPreservacaoFimDeSemana,
                    preferenciasTurnos,
                    preferenciasColegas,
                    horariosGerados,
                    politica,
                    false,
                    true,
                    true,
                    minimoChefiasNoDia,
                    LIMITE_CANDIDATOS_POR_SLOT_EXCECAO,
                    LIMITE_NOS_PESQUISA_EXCECAO,
                    prazoLimiteGeracao
            );
            ultimaTentativa = tentativaExcecaoOperacional;
            if (tentativaExcecaoOperacional.encontrou()) {
                return tentativaExcecaoOperacional.atribuicoes();
            }
            registarTentativaLimitada(tentativaExcecaoOperacional, data, "excecao-operacional");
        }

        if (prazoLimiteGeracao != null && Instant.now().isAfter(prazoLimiteGeracao)) {
            throw new IllegalArgumentException(
                    "A geracao do planeamento demorou demasiado tempo para "
                            + data.getMonthValue()
                            + "/"
                            + data.getYear()
                            + ". Tenta gerar menos alternativas de cada vez ou reduzir restricoes muito apertadas."
            );
        }

        if (ultimaTentativa.contextoPesquisa().atingiuLimitePesquisa()) {
            throw new IllegalArgumentException(
                    "A distribuicao de turnos em "
                            + DATA_FORMATTER.format(data)
                            + " exigiu combinacoes demais para a pesquisa automatica. Revê minimos por turno, bloqueios, folgas e regras de chefia desse periodo."
            );
        }

        if (!ultimaTentativa.encontrou()) {
            List<SlotTurnoDia> slotsOrdenados = ultimaTentativa.slotsOrdenados();
            Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot = ultimaTentativa.candidatosPorSlot();
            SlotTurnoDia slotMaisCritico = slotsOrdenados.getFirst();
            String diagnostico = diagnosticoDisponibilidade(
                    estadoPorColaborador.values(),
                    data,
                    slotMaisCritico.turno(),
                    calcularDuracaoEmMinutos(slotMaisCritico.turno()),
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana
            );
            LOGGER.warn(
                    "Falha ao cobrir turno {} em {}. Diagnostico: [{}]. Candidatos por slot: [{}]",
                    formatarTurno(slotMaisCritico.turno()),
                    data,
                    diagnostico,
                    descreverCandidatosPorSlot(candidatosPorSlot)
            );
            throw criarMensagemFalhaCobertura(
                    data,
                    slotMaisCritico,
                    estadoPorColaborador.values(),
                    slotsOrdenados,
                    candidatosPorSlot,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana
            );
        }

        return ultimaTentativa.atribuicoes();
    }

    private FalhaGeracaoHorarioException criarMensagemFalhaCobertura(LocalDate data,
                                                                     SlotTurnoDia slotMaisCritico,
                                                                     Collection<EstadoColaborador> estados,
                                                                     List<SlotTurnoDia> slotsOrdenados,
                                                                     Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot,
                                                                     Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                     ParametrosGeracao parametros,
                                                                     ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        String turno = formatarTurno(slotMaisCritico.turno());
        String dataFormatada = data != null ? DATA_FORMATTER.format(data) : "data selecionada";
        int colaboradoresConsiderados = estados != null ? estados.size() : 0;
        List<MotivoFalhaGeracao> motivos = resumirMotivosFalhaCobertura(
                estados,
                data,
                slotMaisCritico.turno(),
                calcularDuracaoEmMinutos(slotMaisCritico.turno()),
                bloqueiosPorUtilizador,
                parametros,
                reservaOperacionalFimDeSemana
        );
        String motivoPrincipal = motivos.isEmpty()
                ? "Nao havia colaboradores suficientes sem violar as regras configuradas."
                : motivos.getFirst().descricao();
        GargaloCobertura gargalo = calcularGargaloCobertura(slotMaisCritico, slotsOrdenados, candidatosPorSlot);
        List<SugestaoFalhaGeracao> sugestoes = criarSugestoesFalhaGeracao(slotMaisCritico, motivos, gargalo);

        String mensagem = "Nao foi possivel gerar o horario porque o turno "
                + turno
                + " de "
                + dataFormatada
                + " ficou sem colaboradores disponiveis. Foram considerados "
                + colaboradoresConsiderados
                + " colaboradores selecionados. Revê a selecao da equipa, folgas/preferencias aprovadas, descanso entre turnos, limite de horas e minimo exigido por turno.";

        return new FalhaGeracaoHorarioException(
                mensagem,
                turno,
                dataFormatada,
                colaboradoresConsiderados,
                motivoPrincipal,
                motivos,
                sugestoes
        );
    }

    private GargaloCobertura calcularGargaloCobertura(SlotTurnoDia slotMaisCritico,
                                                      List<SlotTurnoDia> slotsOrdenados,
                                                      Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot) {
        if (slotMaisCritico == null || slotsOrdenados == null || slotsOrdenados.isEmpty()) {
            return new GargaloCobertura(1, 0, 1);
        }

        String turnoCritico = normalizarTurno(slotMaisCritico.turno());
        String periodoCritico = formatarPeriodo(slotMaisCritico.turno());
        List<SlotTurnoDia> slotsDoTurno = slotsOrdenados.stream()
                .filter(slot -> Objects.equals(turnoCritico, normalizarTurno(slot.turno())))
                .filter(slot -> Objects.equals(periodoCritico, formatarPeriodo(slot.turno())))
                .toList();

        Set<Integer> candidatosUnicos = new LinkedHashSet<>();
        for (SlotTurnoDia slot : slotsDoTurno) {
            for (AtribuicaoPlaneadaDia candidato : candidatosPorSlot.getOrDefault(slot, List.of())) {
                if (candidato != null && candidato.estado() != null && candidato.estado().idUtilizador() != null) {
                    candidatosUnicos.add(candidato.estado().idUtilizador());
                }
            }
        }

        int procura = Math.max(1, slotsDoTurno.size());
        int oferta = candidatosUnicos.size();
        int deficit = Math.max(1, procura - oferta);
        return new GargaloCobertura(procura, oferta, deficit);
    }

    private List<SugestaoFalhaGeracao> criarSugestoesFalhaGeracao(SlotTurnoDia slotMaisCritico,
                                                                  List<MotivoFalhaGeracao> motivos,
                                                                  GargaloCobertura gargalo) {
        List<SugestaoFalhaGeracao> sugestoes = new ArrayList<>();
        String turno = formatarTurno(slotMaisCritico.turno());
        String periodo = formatarPeriodo(slotMaisCritico.turno());
        int deficit = Math.max(1, gargalo.deficit());
        String perfilRecomendado = recomendarPerfilReforco(slotMaisCritico.turno());

        if (gargalo.oferta() < gargalo.procura()) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "capacidade",
                    "Seleciona pelo menos mais " + deficit + " colaborador(es) elegivel(eis) para " + turno
                            + " (" + periodo + "). Neste gargalo eram precisas " + gargalo.procura()
                            + " pessoa(s), mas so havia " + gargalo.oferta()
                            + " candidato(s) que podiam realmente ser usados.",
                    perfilRecomendado
            ));
        } else {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "reorganizar",
                    "Nao parece faltar gente para este turno isolado: havia " + gargalo.oferta()
                            + " candidato(s) para " + gargalo.procura()
                            + " lugar(es). O problema e a combinacao do mes. Revê primeiro quem ja acumulou muitas horas antes deste dia e tenta libertar essas pessoas para "
                            + turno
                            + ".",
                    perfilRecomendado
            ));
        }

        Set<String> codigos = motivos.stream()
                .map(MotivoFalhaGeracao::codigo)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (codigos.contains("turno-curto")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "turno-curto",
                    "Ha FT/gestao bloqueados porque este turno e curto. Resolve selecionando part-time suficientes para este turno ou alterando o turno para 8 horas, se a loja puder operar assim.",
                    "Assistente de Vendas PT"
            ));
        }
        if (codigos.contains("perfil")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "perfil",
                    "Ha pessoas selecionadas que nao podem trabalhar neste dia pelo perfil. Se forem reforcos de fim de semana, nao contam para uma quarta-feira; inclui mais part-time/full-time validos para dias uteis.",
                    perfilRecomendado
            ));
        }
        if (codigos.contains("bloqueio")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "bloqueio",
                    "Verifica folgas, ferias e preferencias aprovadas neste dia. Se uma ausencia puder mudar de dia, muda primeiro a de alguem que consiga fazer " + turno + ".",
                    perfilRecomendado
            ));
        }
        if (codigos.contains("descanso-turno")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "descanso-turno",
                    "Ha pessoas que trabalharam tarde no dia anterior e nao podem abrir de manha. Move essas pessoas para tarde/noite ou troca a noite do dia anterior com alguem que nao precise abrir.",
                    perfilRecomendado
            ));
        }
        if (codigos.contains("carga")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "carga",
                    "A carga mensal esta a bloquear pessoas. Para julho, tira alguns turnos anteriores desses colaboradores ou adiciona alguem com horas livres no mes.",
                    perfilRecomendado
            ));
        }
        if (codigos.contains("descanso-semanal") || codigos.contains("consecutivo")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "descanso-semanal",
                    "Ha sequencias de muitos dias seguidos. Coloca uma folga antes do dia critico para quem podia fazer " + turno + ", ou troca um turno dessa pessoa nos dias anteriores.",
                    perfilRecomendado
            ));
        }
        if (codigos.contains("sem-combinacao")) {
            sugestoes.add(new SugestaoFalhaGeracao(
                    "combinacao",
                    "Como ha candidatos isolados mas o conjunto nao fecha, tenta gerar so 1 alternativa e reduz primeiro as restricoes menos criticas: preferencias, rotacao de fins de semana ou minimos desse dia.",
                    perfilRecomendado
            ));
        }

        sugestoes.add(new SugestaoFalhaGeracao(
                "minimos",
                "Se operacionalmente for aceitavel, baixa temporariamente o minimo de colaboradores no turno "
                        + turno + " para este periodo. Esta e a forma direta de relaxar a restricao que tornou o horario inviavel.",
                "Sem novo colaborador"
        ));

        return sugestoes.stream().limit(5).toList();
    }

    private String recomendarPerfilReforco(Turno turno) {
        long duracao = calcularDuracaoEmMinutos(turno);
        String tipo = normalizarTurno(turno);
        boolean fimDeSemana = tipo.contains("sabado") || tipo.contains("domingo");
        if (fimDeSemana) {
            return "Reforco Fim de Semana";
        }
        if (duracao > 0 && duracao < DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS) {
            return "Assistente de Vendas PT";
        }
        return "Assistente de Vendas FT com margem de horas";
    }

    private List<MotivoFalhaGeracao> resumirMotivosFalhaCobertura(Collection<EstadoColaborador> estados,
                                                                  LocalDate data,
                                                                  Turno turno,
                                                                  long minutosTurno,
                                                                  Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                  ParametrosGeracao parametros,
                                                                  ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        if (estados == null || estados.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> nomesPorMotivo = new LinkedHashMap<>();
        for (EstadoColaborador estado : estados) {
            List<String> motivos = estado.motivosIndisponibilidade(
                    data,
                    turno,
                    minutosTurno,
                    bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            );
            String motivo = motivos.isEmpty() ? "sem-combinacao" : motivos.getFirst();
            nomesPorMotivo.computeIfAbsent(motivo, ignorado -> new ArrayList<>())
                    .add(valorOuTraco(estado.ligacao().getIdUtilizador().getNome()));
        }

        return nomesPorMotivo.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(entry -> {
                    List<String> nomes = entry.getValue().stream()
                            .distinct()
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .toList();
                    return new MotivoFalhaGeracao(
                            entry.getKey(),
                            nomes.size(),
                            descreverMotivoFalha(entry.getKey()),
                            nomes
                    );
                })
                .toList();
    }

    private String descreverMotivoFalha(String motivo) {
        return switch (motivo) {
            case "bloqueio" -> "Folga, ferias ou preferencia aprovada bloqueia este dia.";
            case "ja-atribuido" -> "Colaborador ja tinha outro turno nesse dia.";
            case "vinculo" -> "Colaborador sem vinculo valido nessa data.";
            case "turno-curto" -> "Full-time/gestao nao podem fazer este turno curto.";
            case "perfil" -> "Perfil contratual nao permite trabalhar neste dia ou turno.";
            case "carga" -> "Limite de horas mensal ja foi atingido ou ficaria ultrapassado.";
            case "descanso-semanal" -> "Descanso semanal minimo ficaria comprometido.";
            case "rotacao" -> "Rotacao de fins de semana impede nova atribuicao.";
            case "consecutivo" -> "Maximo de dias consecutivos seria ultrapassado.";
            case "descanso-turno" -> "Descanso minimo entre turnos nao seria cumprido.";
            case "sem-combinacao" -> "Colaborador podia fazer este turno isoladamente, mas a combinacao completa do dia/mes nao fecha.";
            default -> "Outras regras impediram a atribuicao.";
        };
    }

    private ResultadoTentativaDistribuicao executarTentativaDistribuicaoDoDia(LocalDate data,
                                                                              List<SlotTurnoDia> slots,
                                                                              Map<Integer, EstadoColaborador> estadoPorColaborador,
                                                                              Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                              ParametrosGeracao parametros,
                                                                              ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                              ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                                              Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                              Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                              List<Horario> horariosGerados,
                                                                              PoliticaOtimizacao politica,
                                                                              boolean aplicarPreservacaoFimDeSemana,
                                                                              boolean ignorarRotacaoFimDeSemana,
                                                                              boolean ignorarDescansoSemanal,
                                                                              int minimoChefiasNoDia,
                                                                              int limiteCandidatosPorSlot,
                                                                              int limiteNosPesquisa,
                                                                              Instant prazoLimiteGeracao) {
        Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot = new LinkedHashMap<>();
        for (SlotTurnoDia slot : slots) {
            List<AtribuicaoPlaneadaDia> candidatos = limitarAtribuicoesPotenciais(
                    resolverAtribuicoesOrdenadas(
                            data,
                            slot.turnosCompativeis(),
                            estadoPorColaborador.values(),
                            bloqueiosPorUtilizador,
                            parametros,
                            reservaOperacionalFimDeSemana,
                            contextoPreservacaoFimDeSemana,
                            aplicarPreservacaoFimDeSemana,
                            preferenciasTurnos,
                            preferenciasColegas,
                            horariosGerados,
                            ignorarRotacaoFimDeSemana,
                            ignorarDescansoSemanal,
                            politica
                    ),
                    limiteCandidatosPorSlot
            );
            candidatosPorSlot.put(slot, candidatos);
        }

        List<SlotTurnoDia> slotsOrdenados = ordenarSlotsPorEscassez(slots, candidatosPorSlot);
        long candidatosDistintos = candidatosPorSlot.values().stream()
                .flatMap(Collection::stream)
                .map(AtribuicaoPlaneadaDia::estado)
                .map(EstadoColaborador::idUtilizador)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        if (candidatosDistintos < slotsOrdenados.size()) {
            return new ResultadoTentativaDistribuicao(
                    false,
                    List.of(),
                    candidatosPorSlot,
                    slotsOrdenados,
                    new PesquisaDistribuicaoContexto(prazoLimiteGeracao, limiteNosPesquisa)
            );
        }

        List<AtribuicaoPlaneadaDia> atribuicoesGulosas = tentarDistribuicaoGulosaDoDia(
                slotsOrdenados,
                minimoChefiasNoDia,
                candidatosPorSlot,
                estadoPorColaborador.values(),
                data,
                aplicarPreservacaoFimDeSemana ? contextoPreservacaoFimDeSemana : null,
                parametros,
                bloqueiosPorUtilizador,
                reservaOperacionalFimDeSemana
        );
        if (atribuicoesGulosas != null) {
            return new ResultadoTentativaDistribuicao(
                    true,
                    List.copyOf(atribuicoesGulosas),
                    candidatosPorSlot,
                    slotsOrdenados,
                    new PesquisaDistribuicaoContexto(prazoLimiteGeracao, limiteNosPesquisa)
            );
        }

        List<AtribuicaoPlaneadaDia> atribuicoes = new ArrayList<>();
        PesquisaDistribuicaoContexto contextoPesquisa = new PesquisaDistribuicaoContexto(prazoLimiteGeracao, limiteNosPesquisa);
        boolean encontrou = tentarDistribuicaoDoDia(
                slotsOrdenados,
                0,
                minimoChefiasNoDia,
                0,
                candidatosPorSlot,
                new LinkedHashSet<>(),
                atribuicoes,
                estadoPorColaborador.values(),
                data,
                aplicarPreservacaoFimDeSemana ? contextoPreservacaoFimDeSemana : null,
                parametros,
                bloqueiosPorUtilizador,
                reservaOperacionalFimDeSemana,
                contextoPesquisa
        );

        return new ResultadoTentativaDistribuicao(
                encontrou,
                List.copyOf(atribuicoes),
                candidatosPorSlot,
                slotsOrdenados,
                contextoPesquisa
        );
    }

    private List<AtribuicaoPlaneadaDia> tentarDistribuicaoGulosaDoDia(List<SlotTurnoDia> slotsOrdenados,
                                                                       int minimoChefiasNoDia,
                                                                       Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot,
                                                                       Collection<EstadoColaborador> estados,
                                                                       LocalDate data,
                                                                       ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                                       ParametrosGeracao parametros,
                                                                       Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                       ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        Set<Integer> colaboradoresUsados = new LinkedHashSet<>();
        List<AtribuicaoPlaneadaDia> atribuicoes = new ArrayList<>();
        int chefiasAtribuidas = 0;

        for (int indice = 0; indice < slotsOrdenados.size(); indice++) {
            SlotTurnoDia slot = slotsOrdenados.get(indice);
            boolean atribuido = false;

            for (AtribuicaoPlaneadaDia candidato : candidatosPorSlot.getOrDefault(slot, List.of())) {
                if (colaboradoresUsados.contains(candidato.estado().idUtilizador())) {
                    continue;
                }

                colaboradoresUsados.add(candidato.estado().idUtilizador());
                atribuicoes.add(candidato);

                int proximasChefiasAtribuidas = chefiasAtribuidas + (candidato.estado().cumpreChefiaObrigatoriaAoSabado() ? 1 : 0);
                boolean mantemChefiasPossiveis = proximasChefiasAtribuidas + contarChefiasDisponiveisNosSlotsRestantes(
                        slotsOrdenados,
                        indice + 1,
                        candidatosPorSlot,
                        colaboradoresUsados
                ) >= minimoChefiasNoDia;

                boolean mantemCapacidade = mantemCapacidadeFuturaAposAtribuicoes(
                        estados,
                        data,
                        contextoPreservacaoFimDeSemana,
                        parametros,
                        bloqueiosPorUtilizador,
                        reservaOperacionalFimDeSemana,
                        atribuicoes
                );

                if (mantemChefiasPossiveis && mantemCapacidade) {
                    chefiasAtribuidas = proximasChefiasAtribuidas;
                    atribuido = true;
                    break;
                }

                atribuicoes.removeLast();
                colaboradoresUsados.remove(candidato.estado().idUtilizador());
            }

            if (!atribuido) {
                return null;
            }
        }

        return chefiasAtribuidas >= minimoChefiasNoDia ? atribuicoes : null;
    }

    private List<SlotTurnoDia> ordenarSlotsPorEscassez(List<SlotTurnoDia> slots,
                                                       Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot) {
        List<SlotTurnoDia> slotsOrdenados = new ArrayList<>(slots);
        slotsOrdenados.sort(Comparator
                .comparingInt((SlotTurnoDia slot) -> candidatosPorSlot.getOrDefault(slot, List.of()).size())
                .thenComparing(slot -> slot.turno().getHoraInicio(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(slot -> valorOuTraco(
                        slot.turno().getTipo() != null ? String.valueOf(slot.turno().getTipo()) : null
                ), String.CASE_INSENSITIVE_ORDER));
        return slotsOrdenados;
    }

    private List<SlotTurnoDia> construirSlotsDoDia(List<Turno> turnosDoDia,
                                                   ConfiguracaoDiaEspecial configuracaoDia,
                                                   ParametrosGeracao parametros) {
        Map<String, List<Turno>> turnosPorTipo = new LinkedHashMap<>();
        for (Turno turno : turnosDoDia) {
            turnosPorTipo.computeIfAbsent(normalizarTurno(turno), ignored -> new ArrayList<>()).add(turno);
        }

        List<SlotTurnoDia> slots = new ArrayList<>();
        for (List<Turno> grupo : turnosPorTipo.values()) {
            if (grupo == null || grupo.isEmpty()) {
                continue;
            }

            Turno turnoRepresentativo = selecionarTurnoRepresentativo(grupo);
            int minimoNecessario = grupo.stream()
                    .map(turno -> resolverMinimoNecessarioTurno(turno, configuracaoDia, parametros))
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(0);
            if (minimoNecessario <= 0) {
                throw new IllegalArgumentException("Foi encontrada uma regra minima invalida para um dos turnos.");
            }

            long minutosTurno = calcularDuracaoEmMinutos(turnoRepresentativo);
            for (int indice = 0; indice < minimoNecessario; indice++) {
                slots.add(new SlotTurnoDia(turnoRepresentativo, List.copyOf(grupo), indice));
            }
        }
        return slots;
    }

    private Integer resolverMinimoNecessarioTurno(Turno turno,
                                                  ConfiguracaoDiaEspecial configuracaoDia,
                                                  ParametrosGeracao parametros) {
        if (configuracaoDia != null && configuracaoDia.minimoColaboradoresTurno() != null) {
            return configuracaoDia.minimoColaboradoresTurno();
        }
        return turno != null ? parametros.minimosPorTurno().get(turno.getId()) : null;
    }

    private Turno selecionarTurnoRepresentativo(List<Turno> grupo) {
        return grupo.stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparingLong(this::calcularDuracaoEmMinutos)
                        .thenComparing(Turno::getHoraInicio, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Turno::getHoraFim, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Turno::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel determinar um turno representativo."));
    }

    private List<AtribuicaoPlaneadaDia> limitarAtribuicoesPotenciais(List<AtribuicaoPlaneadaDia> candidatos, int limiteCandidatosPorSlot) {
        if (candidatos == null || candidatos.isEmpty() || limiteCandidatosPorSlot <= 0 || candidatos.size() <= limiteCandidatosPorSlot) {
            return candidatos;
        }
        return List.copyOf(candidatos.subList(0, limiteCandidatosPorSlot));
    }

    private void registarTentativaLimitada(ResultadoTentativaDistribuicao tentativa, LocalDate data, String contextoTentativa) {
        if (tentativa == null || tentativa.encontrou() || tentativa.contextoPesquisa() == null) {
            return;
        }
        if (tentativa.contextoPesquisa().atingiuLimitePesquisa() || tentativa.contextoPesquisa().prazoEsgotado()) {
            LOGGER.warn(
                    "A tentativa {} da distribuicao de {} nao concluiu dentro do orcamento (nos explorados={}, limite={}, prazo esgotado={}).",
                    contextoTentativa,
                    DATA_FORMATTER.format(data),
                    tentativa.contextoPesquisa().nosExplorados(),
                    tentativa.contextoPesquisa().limiteNosPesquisa(),
                    tentativa.contextoPesquisa().prazoEsgotado()
            );
        }
    }

    private void validarPrazoGeracao(Instant prazoLimiteGeracao, LocalDate dataReferencia, PoliticaOtimizacao politica) {
        if (prazoLimiteGeracao != null && Instant.now().isAfter(prazoLimiteGeracao)) {
            throw new IllegalArgumentException(
                    "A geracao da alternativa com a politica "
                            + (politica != null ? politica.nome() : "equilibrio")
                            + " ultrapassou o tempo maximo para "
                            + Month.of(dataReferencia.getMonthValue()).name().toLowerCase(Locale.ROOT)
                            + "/"
                            + dataReferencia.getYear()
                            + "."
            );
        }
    }

    private boolean existeChefiaPossivelNoDia(LocalDate data,
                                              List<SlotTurnoDia> slots,
                                              Collection<EstadoColaborador> estados,
                                              Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                              ParametrosGeracao parametros,
                                              ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        for (EstadoColaborador estado : estados) {
            if (!estado.cumpreChefiaObrigatoriaAoSabado()) {
                continue;
            }
            for (SlotTurnoDia slot : slots) {
                for (Turno turnoCompativel : slot.turnosCompativeis()) {
                    if (estado.podeReceber(
                            data,
                            turnoCompativel,
                            calcularDuracaoEmMinutos(turnoCompativel),
                            bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                            parametros.maxDiasConsecutivos(),
                            parametros.descansoMinimoHoras(),
                            parametros.descansoSemanalMinimoDias(),
                            parametros.janelaRotacaoFinsDeSemanaSemanas(),
                            reservaOperacionalFimDeSemana,
                            true,
                            true
                    )) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private String descreverCandidatosPorSlot(Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot) {
        List<String> detalhes = new ArrayList<>();
        for (Map.Entry<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> entry : candidatosPorSlot.entrySet()) {
            String nomes = entry.getValue().stream()
                    .map(atribuicao -> valorOuTraco(atribuicao.estado().ligacao().getIdUtilizador().getNome())
                            + "@"
                            + formatarPeriodo(atribuicao.turno()))
                    .toList()
                    .toString();
            detalhes.add(formatarTurno(entry.getKey().turno()) + "#" + entry.getKey().ordemNoTurno() + "=" + nomes);
        }
        return String.join("; ", detalhes);
    }

    private List<AtribuicaoPlaneadaDia> resolverAtribuicoesOrdenadas(LocalDate data,
                                                                     List<Turno> turnosCompativeis,
                                                                     Collection<EstadoColaborador> estados,
                                                                     Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                     ParametrosGeracao parametros,
                                                                     ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                     ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                                     boolean aplicarPreservacaoFimDeSemana,
                                                                     Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                     Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                     List<Horario> horariosGerados,
                                                                     boolean ignorarRotacaoFimDeSemana,
                                                                     boolean ignorarDescansoSemanal,
                                                                     PoliticaOtimizacao politica) {
        Map<Integer, AtribuicaoPlaneadaDia> melhorPorUtilizador = new LinkedHashMap<>();
        for (Turno turnoCompativel : turnosCompativeis) {
            long minutosTurno = calcularDuracaoEmMinutos(turnoCompativel);
            List<EstadoColaborador> candidatos = resolverCandidatosOrdenados(
                    data,
                    turnoCompativel,
                    minutosTurno,
                    estados,
                    bloqueiosPorUtilizador,
                    parametros,
                    reservaOperacionalFimDeSemana,
                    contextoPreservacaoFimDeSemana,
                    aplicarPreservacaoFimDeSemana,
                    preferenciasTurnos,
                    preferenciasColegas,
                    horariosGerados,
                    ignorarRotacaoFimDeSemana,
                    ignorarDescansoSemanal,
                    politica
            );
            for (EstadoColaborador candidato : candidatos) {
                AtribuicaoPlaneadaDia proposta = new AtribuicaoPlaneadaDia(candidato, turnoCompativel, minutosTurno);
                melhorPorUtilizador.merge(
                        candidato.idUtilizador(),
                        proposta,
                        (atual, novo) -> criarComparatorAtribuicoesPotenciais(
                                data,
                                parametros.exigirChefiaAoSabado(),
                                parametros.descansoSemanalMinimoDias(),
                                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                                reservaOperacionalFimDeSemana,
                                preferenciasTurnos,
                                preferenciasColegas,
                                horariosGerados,
                                politica
                        ).compare(atual, novo) <= 0 ? atual : novo
                );
            }
        }

        List<AtribuicaoPlaneadaDia> atribuicoes = new ArrayList<>(melhorPorUtilizador.values());
        atribuicoes.sort(criarComparatorAtribuicoesPotenciais(
                data,
                parametros.exigirChefiaAoSabado(),
                parametros.descansoSemanalMinimoDias(),
                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                reservaOperacionalFimDeSemana,
                preferenciasTurnos,
                preferenciasColegas,
                horariosGerados,
                politica
        ));
        return atribuicoes;
    }

    private List<EstadoColaborador> resolverCandidatosOrdenados(LocalDate data,
                                                                Turno turno,
                                                                long minutosTurno,
                                                                Collection<EstadoColaborador> estados,
                                                                Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                ParametrosGeracao parametros,
                                                                ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                                boolean aplicarPreservacaoFimDeSemana,
                                                                Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                List<Horario> horariosGerados,
                                                                boolean ignorarRotacaoFimDeSemana,
                                                                boolean ignorarDescansoSemanal,
                                                                PoliticaOtimizacao politica) {
        Comparator<EstadoColaborador> comparator = criarComparatorCandidatos(
                data,
                turno,
                minutosTurno,
                parametros.exigirChefiaAoSabado(),
                parametros.descansoSemanalMinimoDias(),
                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                reservaOperacionalFimDeSemana,
                preferenciasTurnos,
                preferenciasColegas,
                horariosGerados,
                politica
        );
        Comparator<EstadoColaborador> comparatorComViabilidadeFutura = Comparator
                .comparingInt((EstadoColaborador estado) -> prioridadeImpactoNoDomingo(
                        estado,
                        data,
                        turno,
                        minutosTurno,
                        bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                        parametros,
                        reservaOperacionalFimDeSemana,
                        contextoPreservacaoFimDeSemana
                ))
                .thenComparingInt(estado -> prioridadeUsoExcecionalDaRotacao(
                        estado,
                        data,
                        parametros.janelaRotacaoFinsDeSemanaSemanas(),
                        ignorarRotacaoFimDeSemana
                ))
                .thenComparingInt(estado -> prioridadeUsoExcecionalDoDescansoSemanal(
                        estado,
                        data,
                        parametros.descansoSemanalMinimoDias(),
                        ignorarDescansoSemanal
                ))
                .thenComparing(comparator);

        return estados.stream()
                .filter(estado -> estado.podeReceber(
                        data,
                        turno,
                        minutosTurno,
                        bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                        parametros.maxDiasConsecutivos(),
                        parametros.descansoMinimoHoras(),
                        parametros.descansoSemanalMinimoDias(),
                        parametros.janelaRotacaoFinsDeSemanaSemanas(),
                        reservaOperacionalFimDeSemana,
                        ignorarRotacaoFimDeSemana,
                        ignorarDescansoSemanal
                ))
                .filter(estado -> mantemViabilidadeDoFimDeSemana(
                        estado,
                        estados,
                        data,
                        turno,
                        minutosTurno,
                        bloqueiosPorUtilizador,
                        parametros,
                        reservaOperacionalFimDeSemana,
                        contextoPreservacaoFimDeSemana,
                        aplicarPreservacaoFimDeSemana
                ))
                .sorted(comparatorComViabilidadeFutura)
                .toList();
    }

    private Comparator<AtribuicaoPlaneadaDia> criarComparatorAtribuicoesPotenciais(LocalDate data,
                                                                                    boolean exigirChefiaAoSabado,
                                                                                    int descansoSemanalMinimoDias,
                                                                                    int janelaRotacaoFinsDeSemanaSemanas,
                                                                                    ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                                    Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                                    Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                                    List<Horario> horariosGerados,
                                                                                    PoliticaOtimizacao politica) {
        return Comparator
                .comparingInt((AtribuicaoPlaneadaDia atribuicao) -> prioridadeChefiaNaDistribuicao(
                        atribuicao.estado(),
                        data,
                        exigirChefiaAoSabado
                ))
                .thenComparingInt(atribuicao -> custoObjetivo(
                        atribuicao.estado(),
                        data,
                        atribuicao.turno(),
                        atribuicao.minutosTurno(),
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas,
                        reservaOperacionalFimDeSemana,
                        preferenciasTurnos,
                        preferenciasColegas,
                        horariosGerados,
                        politica
                ))
                .thenComparingInt(atribuicao -> prioridadeConcentracaoFimDeSemana(atribuicao.estado(), data))
                .thenComparingInt(atribuicao -> prioridadeGrupoRotacaoPlaneado(atribuicao.estado(), data))
                .thenComparingInt(atribuicao -> prioridadeReservaOperacionalDaSemana(
                        atribuicao.estado(),
                        data,
                        descansoSemanalMinimoDias,
                        reservaOperacionalFimDeSemana
                ))
                .thenComparingInt(atribuicao -> prioridadePreservacaoDoProximoFimDeSemana(
                        atribuicao.estado(),
                        data,
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas
                ))
                .thenComparingInt(atribuicao -> atribuicao.estado().diasTrabalhadosNaSemana(data))
                .thenComparingDouble(atribuicao -> atribuicao.estado().utilizacaoContratualProjetada(atribuicao.minutosTurno()))
                .thenComparingInt(atribuicao -> prioridadePerfilNoFimDeSemana(atribuicao.estado(), data))
                .thenComparingInt(atribuicao -> prioridadeSustentabilidadeDoFimDeSemana(
                        atribuicao.estado(),
                        data,
                        descansoSemanalMinimoDias
                ))
                .thenComparingInt(atribuicao -> prioridadeReservaParaFimDeSemana(
                        atribuicao.estado(),
                        data,
                        janelaRotacaoFinsDeSemanaSemanas
                ))
                .thenComparing(atribuicao -> !temPreferenciaTurnoFavoravel(
                        preferenciasTurnos.getOrDefault(atribuicao.estado().idUtilizador(), List.of()),
                        data,
                        atribuicao.turno()
                ))
                .thenComparing(atribuicao -> !temPreferenciaColegasFavoravel(
                        preferenciasColegas.getOrDefault(atribuicao.estado().idUtilizador(), List.of()),
                        data,
                        horariosGerados
                ))
                .thenComparingInt(atribuicao -> atribuicao.estado().turnosAtribuidos())
                .thenComparingLong(atribuicao -> atribuicao.estado().minutosAtribuidos())
                .thenComparingInt(atribuicao -> atribuicao.estado().turnosDoTipo(normalizarTurno(atribuicao.turno())))
                .thenComparingInt(atribuicao -> prioridadeDiversificacao(
                        atribuicao.estado(),
                        data,
                        atribuicao.turno(),
                        politica
                ))
                .thenComparing(atribuicao -> valorOuTraco(
                        atribuicao.estado().ligacao().getIdUtilizador().getNome()
                ), String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(AtribuicaoPlaneadaDia::minutosTurno);
    }

    private int prioridadeUsoExcecionalDaRotacao(EstadoColaborador estado,
                                                 LocalDate data,
                                                 int janelaRotacaoFinsDeSemanaSemanas,
                                                 boolean ignorarRotacaoFimDeSemana) {
        if (!ignorarRotacaoFimDeSemana
                || estado == null
                || !estado.violariaRotacaoDeFimDeSemana(data, janelaRotacaoFinsDeSemanaSemanas)) {
            return 0;
        }
        return 6;
    }

    private int prioridadeUsoExcecionalDoDescansoSemanal(EstadoColaborador estado,
                                                         LocalDate data,
                                                         int descansoSemanalMinimoDias,
                                                         boolean ignorarDescansoSemanal) {
        if (!ignorarDescansoSemanal
                || estado == null
                || !estado.excedeMaximoDiasTrabalhadosNaSemana(data, descansoSemanalMinimoDias)) {
            return 0;
        }
        return 8;
    }

    private int prioridadeImpactoNoDomingo(EstadoColaborador estado,
                                           LocalDate data,
                                           Turno turno,
                                           long minutosTurno,
                                           Set<LocalDate> bloqueios,
                                           ParametrosGeracao parametros,
                                           ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                           ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana) {
        if (estado == null
                || data == null
                || data.getDayOfWeek() != DayOfWeek.SATURDAY
                || contextoPreservacaoFimDeSemana == null
                || contextoPreservacaoFimDeSemana.domingo() == null
                || contextoPreservacaoFimDeSemana.minimoDomingo() <= 0) {
            return 0;
        }

        boolean podeCobrirDomingoAntes = estado.consegueCobrirAlgumTurnoNoDia(
                contextoPreservacaoFimDeSemana.domingo(),
                contextoPreservacaoFimDeSemana.turnosDomingo(),
                bloqueios,
                parametros.maxDiasConsecutivos(),
                parametros.descansoMinimoHoras(),
                parametros.descansoSemanalMinimoDias(),
                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                reservaOperacionalFimDeSemana
        );
        if (!podeCobrirDomingoAntes) {
            return 0;
        }

        EstadoColaborador estadoSimulado = estado.copiarComAtribuicao(data, turno, minutosTurno);
        boolean continuaACobrirDomingo = estadoSimulado.consegueCobrirAlgumTurnoNoDia(
                contextoPreservacaoFimDeSemana.domingo(),
                contextoPreservacaoFimDeSemana.turnosDomingo(),
                bloqueios,
                parametros.maxDiasConsecutivos(),
                parametros.descansoMinimoHoras(),
                parametros.descansoSemanalMinimoDias(),
                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                reservaOperacionalFimDeSemana
        );

        return continuaACobrirDomingo ? 1 : 4;
    }

    private ContextoPreservacaoFimDeSemana construirContextoPreservacaoFimDeSemana(LocalDate dataAtual,
                                                                                    LocalDate dataFim,
                                                                                    List<Turno> turnosBase,
                                                                                    ParametrosGeracao parametros,
                                                                                    Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData) {
        if (dataAtual == null || dataAtual.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return null;
        }

        if (dataAtual.getDayOfWeek() == DayOfWeek.SATURDAY) {
            LocalDate domingo = dataAtual.plusDays(1);
            if (domingo.isAfter(dataFim)) {
                return null;
            }

            ConfiguracaoDiaEspecial configuracaoDomingo = configuracoesPorData.get(domingo);
            if (configuracaoDomingo != null && configuracaoDomingo.lojaEncerrada()) {
                return null;
            }

            List<Turno> turnosDomingo = configuracaoDomingo != null
                    ? configuracaoDomingo.turnosCompativeis()
                    : turnosBase;
            int necessidadeDomingo = calcularNecessidadeMinimaDoDia(domingo, turnosBase, parametros, configuracoesPorData);
            if (necessidadeDomingo <= 0) {
                return null;
            }

            return new ContextoPreservacaoFimDeSemana(
                    null,
                    List.of(),
                    0,
                    domingo,
                    turnosDomingo,
                    necessidadeDomingo,
                    0
            );
        }

        LocalDate sabado = dataAtual.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        if (sabado.isAfter(dataFim) || !inicioSemana(sabado).equals(inicioSemana(dataAtual))) {
            return null;
        }

        ConfiguracaoDiaEspecial configuracaoSabado = configuracoesPorData.get(sabado);
        if (configuracaoSabado != null && configuracaoSabado.lojaEncerrada()) {
            return null;
        }

        List<Turno> turnosSabado = configuracaoSabado != null
                ? configuracaoSabado.turnosCompativeis()
                : turnosBase;
        if (turnosSabado == null || turnosSabado.isEmpty()) {
            return null;
        }

        int necessidadeSabado = calcularNecessidadeMinimaDoDia(sabado, turnosBase, parametros, configuracoesPorData);
        if (necessidadeSabado <= 0) {
            return null;
        }

        LocalDate domingo = sabado.plusDays(1);
        ConfiguracaoDiaEspecial configuracaoDomingo = domingo.isAfter(dataFim) ? null : configuracoesPorData.get(domingo);
        List<Turno> turnosDomingo = domingo.isAfter(dataFim)
                ? List.of()
                : configuracaoDomingo != null
                ? configuracaoDomingo.turnosCompativeis()
                : turnosBase;
        int necessidadeDomingo = domingo.isAfter(dataFim)
                ? 0
                : calcularNecessidadeMinimaDoDia(domingo, turnosBase, parametros, configuracoesPorData);
        int minimoFimDeSemanaCompleto = necessidadeDomingo > 0
                ? calcularTamanhoNucleoReservaOperacional(necessidadeSabado, necessidadeDomingo)
                : Math.min(necessidadeSabado, calcularTamanhoNucleoReservaOperacional(necessidadeSabado, 0));

        return new ContextoPreservacaoFimDeSemana(
                sabado,
                turnosSabado,
                necessidadeSabado,
                domingo.isAfter(dataFim) ? null : domingo,
                turnosDomingo,
                necessidadeDomingo,
                minimoFimDeSemanaCompleto
        );
    }

    private boolean mantemViabilidadeDoFimDeSemana(EstadoColaborador candidato,
                                                   Collection<EstadoColaborador> estados,
                                                   LocalDate dataAtual,
                                                   Turno turnoAtual,
                                                   long minutosTurnoAtual,
                                                   Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                   ParametrosGeracao parametros,
                                                   ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                   ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                   boolean aplicarPreservacaoFimDeSemana) {
        if (!aplicarPreservacaoFimDeSemana
                || candidato == null
                || contextoPreservacaoFimDeSemana == null
                || dataAtual == null
                || dataAtual.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        int elegiveisParaSabado = 0;
        int elegiveisParaDomingo = 0;
        int elegiveisFimDeSemanaCompleto = 0;
        for (EstadoColaborador estado : estados) {
            EstadoColaborador estadoSimulado = estado.idUtilizador().equals(candidato.idUtilizador())
                    ? estado.copiarComAtribuicao(dataAtual, turnoAtual, minutosTurnoAtual)
                    : estado;
            Set<LocalDate> bloqueios = bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of());
            if (estadoSimulado.consegueCobrirAlgumTurnoNoDia(
                    contextoPreservacaoFimDeSemana.sabado(),
                    contextoPreservacaoFimDeSemana.turnosSabado(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisParaSabado++;
            }
            if (contextoPreservacaoFimDeSemana.domingo() != null
                    && estadoSimulado.consegueCobrirAlgumTurnoNoDia(
                    contextoPreservacaoFimDeSemana.domingo(),
                    contextoPreservacaoFimDeSemana.turnosDomingo(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisParaDomingo++;
            }
            if (estadoSimulado.consegueCobrirFimDeSemanaCompleto(
                    contextoPreservacaoFimDeSemana.sabado(),
                    contextoPreservacaoFimDeSemana.turnosSabado(),
                    contextoPreservacaoFimDeSemana.domingo(),
                    contextoPreservacaoFimDeSemana.turnosDomingo(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisFimDeSemanaCompleto++;
            }
        }
        return elegiveisParaSabado >= contextoPreservacaoFimDeSemana.minimoSabado()
                && elegiveisParaDomingo >= contextoPreservacaoFimDeSemana.minimoDomingo()
                && elegiveisFimDeSemanaCompleto >= contextoPreservacaoFimDeSemana.minimoFimDeSemanaCompleto();
    }

    private boolean tentarDistribuicaoDoDia(List<SlotTurnoDia> slotsOrdenados,
                                            int indiceAtual,
                                            int minimoChefiasNoDia,
                                            int chefiasAtribuidas,
                                            Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot,
                                            Set<Integer> colaboradoresUsados,
                                            List<AtribuicaoPlaneadaDia> atribuicoes,
                                            Collection<EstadoColaborador> estados,
                                            LocalDate data,
                                            ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                            ParametrosGeracao parametros,
                                            Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                            ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                            PesquisaDistribuicaoContexto contextoPesquisa) {
        if (contextoPesquisa != null && !contextoPesquisa.permiteContinuar()) {
            return false;
        }
        if (indiceAtual >= slotsOrdenados.size()) {
            return chefiasAtribuidas >= minimoChefiasNoDia;
        }

        if (chefiasAtribuidas + contarChefiasDisponiveisNosSlotsRestantes(
                slotsOrdenados,
                indiceAtual,
                candidatosPorSlot,
                colaboradoresUsados
        ) < minimoChefiasNoDia) {
            return false;
        }

        SlotTurnoDia slot = slotsOrdenados.get(indiceAtual);
        for (AtribuicaoPlaneadaDia candidato : candidatosPorSlot.getOrDefault(slot, List.of())) {
            if (colaboradoresUsados.contains(candidato.estado().idUtilizador())) {
                continue;
            }

            colaboradoresUsados.add(candidato.estado().idUtilizador());
            atribuicoes.add(candidato);

            int proximasChefiasAtribuidas = chefiasAtribuidas + (candidato.estado().cumpreChefiaObrigatoriaAoSabado() ? 1 : 0);
            if (mantemCapacidadeFuturaAposAtribuicoes(
                    estados,
                    data,
                    contextoPreservacaoFimDeSemana,
                    parametros,
                    bloqueiosPorUtilizador,
                    reservaOperacionalFimDeSemana,
                    atribuicoes
            ) && tentarDistribuicaoDoDia(
                    slotsOrdenados,
                    indiceAtual + 1,
                    minimoChefiasNoDia,
                    proximasChefiasAtribuidas,
                    candidatosPorSlot,
                    colaboradoresUsados,
                    atribuicoes,
                    estados,
                    data,
                    contextoPreservacaoFimDeSemana,
                    parametros,
                    bloqueiosPorUtilizador,
                    reservaOperacionalFimDeSemana,
                    contextoPesquisa
            )) {
                return true;
            }

            atribuicoes.removeLast();
            colaboradoresUsados.remove(candidato.estado().idUtilizador());
        }

        return false;
    }

    private boolean mantemCapacidadeFuturaAposAtribuicoes(Collection<EstadoColaborador> estados,
                                                          LocalDate data,
                                                          ContextoPreservacaoFimDeSemana contextoPreservacaoFimDeSemana,
                                                          ParametrosGeracao parametros,
                                                          Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                          ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                          List<AtribuicaoPlaneadaDia> atribuicoesParciais) {
        if (estados == null
                || estados.isEmpty()
                || data == null
                || contextoPreservacaoFimDeSemana == null) {
            return true;
        }

        Map<Integer, AtribuicaoPlaneadaDia> atribuicoesPorUtilizador = new HashMap<>();
        for (AtribuicaoPlaneadaDia atribuicao : atribuicoesParciais) {
            if (atribuicao.estado() != null && atribuicao.estado().idUtilizador() != null) {
                atribuicoesPorUtilizador.put(atribuicao.estado().idUtilizador(), atribuicao);
            }
        }

        int elegiveisSabado = 0;
        int elegiveisDomingo = 0;
        int elegiveisFimDeSemanaCompleto = 0;
        for (EstadoColaborador estado : estados) {
            EstadoColaborador estadoProjetado = estado;
            AtribuicaoPlaneadaDia atribuicao = atribuicoesPorUtilizador.get(estado.idUtilizador());
            if (atribuicao != null) {
                estadoProjetado = estado.copiarComAtribuicao(data, atribuicao.turno(), atribuicao.minutosTurno());
            }

            Set<LocalDate> bloqueios = bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of());
            if (contextoPreservacaoFimDeSemana.sabado() != null
                    && estadoProjetado.consegueCobrirAlgumTurnoNoDia(
                    contextoPreservacaoFimDeSemana.sabado(),
                    contextoPreservacaoFimDeSemana.turnosSabado(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisSabado++;
            }
            if (contextoPreservacaoFimDeSemana.domingo() != null
                    && estadoProjetado.consegueCobrirAlgumTurnoNoDia(
                    contextoPreservacaoFimDeSemana.domingo(),
                    contextoPreservacaoFimDeSemana.turnosDomingo(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisDomingo++;
            }
            if (contextoPreservacaoFimDeSemana.minimoFimDeSemanaCompleto() > 0
                    && estadoProjetado.consegueCobrirFimDeSemanaCompleto(
                    contextoPreservacaoFimDeSemana.sabado(),
                    contextoPreservacaoFimDeSemana.turnosSabado(),
                    contextoPreservacaoFimDeSemana.domingo(),
                    contextoPreservacaoFimDeSemana.turnosDomingo(),
                    bloqueios,
                    parametros.maxDiasConsecutivos(),
                    parametros.descansoMinimoHoras(),
                    parametros.descansoSemanalMinimoDias(),
                    parametros.janelaRotacaoFinsDeSemanaSemanas(),
                    reservaOperacionalFimDeSemana
            )) {
                elegiveisFimDeSemanaCompleto++;
            }
        }

        return elegiveisSabado >= contextoPreservacaoFimDeSemana.minimoSabado()
                && elegiveisDomingo >= contextoPreservacaoFimDeSemana.minimoDomingo()
                && elegiveisFimDeSemanaCompleto >= contextoPreservacaoFimDeSemana.minimoFimDeSemanaCompleto();
    }

    private int contarChefiasDisponiveisNosSlotsRestantes(List<SlotTurnoDia> slotsOrdenados,
                                                          int indiceAtual,
                                                          Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot,
                                                          Set<Integer> colaboradoresUsados) {
        Set<Integer> chefiasDisponiveis = new LinkedHashSet<>();
        for (int indice = indiceAtual; indice < slotsOrdenados.size(); indice++) {
            SlotTurnoDia slot = slotsOrdenados.get(indice);
            candidatosPorSlot.getOrDefault(slot, List.of()).stream()
                    .map(AtribuicaoPlaneadaDia::estado)
                    .filter(EstadoColaborador::cumpreChefiaObrigatoriaAoSabado)
                    .map(EstadoColaborador::idUtilizador)
                    .filter(idUtilizador -> !colaboradoresUsados.contains(idUtilizador))
                    .forEach(chefiasDisponiveis::add);
        }
        return chefiasDisponiveis.size();
    }

    private Comparator<EstadoColaborador> criarComparatorCandidatos(LocalDate data,
                                                                    Turno turno,
                                                                    long minutosTurno,
                                                                    boolean exigirChefiaAoSabado,
                                                                    int descansoSemanalMinimoDias,
                                                                    int janelaRotacaoFinsDeSemanaSemanas,
                                                                    ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                                                    Map<Integer, List<Preferencia>> preferenciasTurnos,
                                                                    Map<Integer, List<Preferencia>> preferenciasColegas,
                                                                    List<Horario> horariosGerados,
                                                                    PoliticaOtimizacao politica) {
        return Comparator
                .comparingInt((EstadoColaborador estado) -> prioridadeChefiaNaDistribuicao(estado, data, exigirChefiaAoSabado))
                .thenComparingInt((EstadoColaborador estado) -> custoObjetivo(
                        estado,
                        data,
                        turno,
                        minutosTurno,
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas,
                        reservaOperacionalFimDeSemana,
                        preferenciasTurnos,
                        preferenciasColegas,
                        horariosGerados,
                        politica
                ))
                .thenComparingInt((EstadoColaborador estado) -> prioridadeConcentracaoFimDeSemana(estado, data))
                .thenComparingInt((EstadoColaborador estado) -> prioridadeGrupoRotacaoPlaneado(estado, data))
                .thenComparingInt((EstadoColaborador estado) -> prioridadeReservaOperacionalDaSemana(
                        estado,
                        data,
                        descansoSemanalMinimoDias,
                        reservaOperacionalFimDeSemana
                ))
                .thenComparingInt((EstadoColaborador estado) -> prioridadePreservacaoDoProximoFimDeSemana(
                        estado,
                        data,
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas
                ))
                .thenComparingInt(estado -> estado.diasTrabalhadosNaSemana(data))
                .thenComparingDouble((EstadoColaborador estado) -> estado.utilizacaoContratualProjetada(minutosTurno))
                .thenComparingInt((EstadoColaborador estado) -> prioridadePerfilNoFimDeSemana(estado, data))
                .thenComparingInt((EstadoColaborador estado) -> prioridadeSustentabilidadeDoFimDeSemana(estado, data, descansoSemanalMinimoDias))
                .thenComparingInt((EstadoColaborador estado) -> prioridadeReservaParaFimDeSemana(estado, data, janelaRotacaoFinsDeSemanaSemanas))
                .thenComparing((EstadoColaborador estado) -> !temPreferenciaTurnoFavoravel(
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
                .thenComparingInt(estado -> prioridadeDiversificacao(estado, data, turno, politica))
                .thenComparing(estado -> valorOuTraco(estado.ligacao().getIdUtilizador().getNome()), String.CASE_INSENSITIVE_ORDER);
    }

    private int custoObjetivo(EstadoColaborador estado,
                              LocalDate data,
                              Turno turno,
                              long minutosTurno,
                              int descansoSemanalMinimoDias,
                              int janelaRotacaoFinsDeSemanaSemanas,
                              ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                              Map<Integer, List<Preferencia>> preferenciasTurnos,
                              Map<Integer, List<Preferencia>> preferenciasColegas,
                              List<Horario> horariosGerados,
                              PoliticaOtimizacao politica) {
        PoliticaOtimizacao politicaSegura = politica != null ? politica : PoliticaOtimizacao.EQUILIBRIO;
        if (politicaSegura == PoliticaOtimizacao.EQUILIBRIO) {
            return 0;
        }

        int custo = 0;
        custo += politicaSegura.pesoEquilibrioCarga() * (int) Math.round(estado.utilizacaoContratualProjetada(minutosTurno) * 100);
        custo += politicaSegura.pesoEquilibrioCarga() * estado.diasTrabalhadosNaSemana(data) * 12;
        custo += politicaSegura.pesoTurnoRepetido() * estado.turnosDoTipo(normalizarTurno(turno)) * 10;

        if (!temPreferenciaTurnoFavoravel(
                preferenciasTurnos.getOrDefault(estado.idUtilizador(), List.of()),
                data,
                turno
        )) {
            custo += politicaSegura.pesoPreferencias() * 18;
        }
        if (!temPreferenciaColegasFavoravel(
                preferenciasColegas.getOrDefault(estado.idUtilizador(), List.of()),
                data,
                horariosGerados
        )) {
            custo += politicaSegura.pesoPreferencias() * 8;
        }

        if (ehFimDeSemana(data)) {
            custo += politicaSegura.pesoFinsDeSemana() * prioridadePerfilNoFimDeSemana(estado, data) * 9;
            custo += politicaSegura.pesoFinsDeSemana() * prioridadeGrupoRotacaoPlaneado(estado, data) * 12;
            custo += politicaSegura.pesoFinsDeSemana() * estado.totalFinsDeSemanaTrabalhados() * 8;
        } else {
            custo += politicaSegura.pesoFinsDeSemana() * prioridadePreservacaoDoProximoFimDeSemana(
                    estado,
                    data,
                    descansoSemanalMinimoDias,
                    janelaRotacaoFinsDeSemanaSemanas
            ) * 10;
        }

        custo += politicaSegura.pesoReservaOperacional() * prioridadeReservaOperacionalDaSemana(
                estado,
                data,
                descansoSemanalMinimoDias,
                reservaOperacionalFimDeSemana
        ) * 8;
        custo += politicaSegura.pesoDiversificacao() * prioridadeDiversificacao(estado, data, turno, politicaSegura);
        return custo;
    }

    private int prioridadeDiversificacao(EstadoColaborador estado, LocalDate data, Turno turno, PoliticaOtimizacao politica) {
        if (estado == null || politica == null || politica.pesoDiversificacao() <= 0) {
            return 0;
        }

        int idTurno = turno != null && turno.getId() != null ? turno.getId() : 0;
        int dia = data != null ? data.getDayOfYear() : 0;
        return Math.floorMod((estado.idUtilizador() * 31) + (idTurno * 17) + dia + politica.sementeDiversificacao(), 23);
    }

    private int prioridadeChefiaNaDistribuicao(EstadoColaborador estado, LocalDate data, boolean exigirChefiaAoSabado) {
        if (!exigirChefiaAoSabado || data == null || data.getDayOfWeek() != DayOfWeek.SATURDAY) {
            return 0;
        }
        return estado.cumpreChefiaObrigatoriaAoSabado() ? 0 : 1;
    }

    private int prioridadeConcentracaoFimDeSemana(EstadoColaborador estado, LocalDate data) {
        if (!ehFimDeSemana(data)) {
            return 0;
        }
        return estado.trabalhouNoMesmoFimDeSemana(data) ? 0 : 1;
    }

    private int prioridadeGrupoRotacaoPlaneado(EstadoColaborador estado, LocalDate data) {
        if (estado == null
                || data == null
                || !ehFimDeSemana(data)
                || estado.cumpreChefiaObrigatoriaAoSabado()
                || estado.trabalhouNoMesmoFimDeSemana(data)) {
            return 0;
        }

        return pertenceAoGrupoPlaneadoDoFimDeSemana(estado, data) ? 0 : 2;
    }

    private boolean pertenceAoGrupoPlaneadoDoFimDeSemana(EstadoColaborador estado, LocalDate data) {
        if (estado == null || data == null) {
            return false;
        }

        int indiceFimDeSemana = indiceFimDeSemanaNoMes(data);
        int grupoColaborador = Math.floorMod(estado.idUtilizador(), 2);
        return grupoColaborador == Math.floorMod(indiceFimDeSemana, 2);
    }

    private int indiceFimDeSemanaNoMes(LocalDate data) {
        LocalDate inicioFimDeSemana = inicioFimDeSemana(data);
        LocalDate primeiroSabado = data.withDayOfMonth(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        if (inicioFimDeSemana.isBefore(primeiroSabado)) {
            return 0;
        }
        return (int) (Duration.between(primeiroSabado.atStartOfDay(), inicioFimDeSemana.atStartOfDay()).toDays() / 7);
    }

    private int prioridadePerfilNoFimDeSemana(EstadoColaborador estado, LocalDate data) {
        if (!ehFimDeSemana(data)) {
            return 0;
        }
        if (estado.cumpreChefiaObrigatoriaAoSabado()) {
            return 0;
        }
        if (estado.ehPartTime()) {
            return 1;
        }
        if (estado.ehReforcoFimDeSemana()) {
            return 2;
        }
        if (estado.ehGestao()) {
            return 3;
        }
        return 4;
    }

    private int prioridadeReservaOperacionalDaSemana(EstadoColaborador estado,
                                                     LocalDate data,
                                                     int descansoSemanalMinimoDias,
                                                     ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
        if (data == null || reservaOperacionalFimDeSemana == null || reservaOperacionalFimDeSemana.estaVazia()) {
            return 0;
        }

        boolean estaNaReserva = reservaOperacionalFimDeSemana.contem(estado.idUtilizador());
        boolean estaNoNucleo = reservaOperacionalFimDeSemana.ehNuclear(estado.idUtilizador());
        if (ehFimDeSemana(data)) {
            return estaNoNucleo ? 0 : 1;
        }
        if (!estaNaReserva) {
            return 0;
        }

        int maxDiasTrabalhados = Math.max(0, 7 - descansoSemanalMinimoDias);
        int limiteConfortavelAntesDoFimDeSemana = Math.max(0, maxDiasTrabalhados - 2);
        int diasTrabalhados = estado.diasTrabalhadosNaSemana(data);
        if (estaNoNucleo) {
            return diasTrabalhados >= limiteConfortavelAntesDoFimDeSemana ? 4 : 2;
        }
        return diasTrabalhados >= limiteConfortavelAntesDoFimDeSemana ? 3 : 1;
    }

    private int prioridadeSustentabilidadeDoFimDeSemana(EstadoColaborador estado,
                                                         LocalDate data,
                                                         int descansoSemanalMinimoDias) {
        if (data == null || data.getDayOfWeek() != DayOfWeek.SATURDAY || estado.trabalhouNoMesmoFimDeSemana(data)) {
            return 0;
        }

        int maxDiasTrabalhados = Math.max(0, 7 - descansoSemanalMinimoDias);
        int diasTrabalhados = estado.diasTrabalhadosNaSemana(data);
        return diasTrabalhados <= Math.max(0, maxDiasTrabalhados - 2) ? 0 : 1;
    }

    private int prioridadePreservacaoDoProximoFimDeSemana(EstadoColaborador estado,
                                                          LocalDate data,
                                                          int descansoSemanalMinimoDias,
                                                          int janelaRotacaoFinsDeSemanaSemanas) {
        if (data == null || ehFimDeSemana(data) || data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 0;
        }

        LocalDate proximoSabado = data.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        if (!inicioSemana(proximoSabado).equals(inicioSemana(data))
                || estado.cumpreChefiaObrigatoriaAoSabado()
                || estado.estariaBloqueadoNoFimDeSemana(proximoSabado, janelaRotacaoFinsDeSemanaSemanas)) {
            return 0;
        }

        int maxDiasTrabalhados = Math.max(0, 7 - descansoSemanalMinimoDias);
        int diasTrabalhados = estado.diasTrabalhadosNaSemana(data);
        if (diasTrabalhados >= Math.max(0, maxDiasTrabalhados - 1)) {
            return 3;
        }
        if (data.getDayOfWeek() == DayOfWeek.FRIDAY && diasTrabalhados >= Math.max(0, maxDiasTrabalhados - 2)) {
            return 2;
        }
        if ((data.getDayOfWeek() == DayOfWeek.THURSDAY || data.getDayOfWeek() == DayOfWeek.FRIDAY)
                && diasTrabalhados >= Math.max(0, maxDiasTrabalhados - 3)) {
            return 1;
        }
        return 0;
    }

    private int prioridadeReservaParaFimDeSemana(EstadoColaborador estado, LocalDate data, int janelaRotacaoFinsDeSemanaSemanas) {
        if (data == null || ehFimDeSemana(data) || data.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return 0;
        }

        LocalDate proximoSabado = data.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        boolean jaEstaBloqueadoNoProximoFimDeSemana = estado.estariaBloqueadoNoFimDeSemana(proximoSabado, janelaRotacaoFinsDeSemanaSemanas);
        return jaEstaBloqueadoNoProximoFimDeSemana ? 0 : 1;
    }

    private ReservaOperacionalFimDeSemana identificarReservaOperacionalFimDeSemana(Collection<EstadoColaborador> estados,
                                                                                   List<Turno> turnosBase,
                                                                                   ParametrosGeracao parametros,
                                                                                   Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                                                                   Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData,
                                                                                   LocalDate inicioSemana,
                                                                                   LocalDate dataFim) {
        if (inicioSemana == null || estados == null || estados.isEmpty()) {
            return ReservaOperacionalFimDeSemana.vazia();
        }

        LocalDate sabado = inicioSemana.plusDays(5);
        LocalDate domingo = inicioSemana.plusDays(6);
        if (sabado.isAfter(dataFim)) {
            return ReservaOperacionalFimDeSemana.vazia();
        }

        int necessidadeSabado = calcularNecessidadeMinimaDoDia(sabado, turnosBase, parametros, configuracoesPorData);
        int necessidadeDomingo = domingo.isAfter(dataFim)
                ? 0
                : calcularNecessidadeMinimaDoDia(domingo, turnosBase, parametros, configuracoesPorData);
        int bufferReserva = domingo.isAfter(dataFim) ? 1 : 2;
        int necessidadeReserva = Math.max(necessidadeSabado, necessidadeDomingo) + bufferReserva;
        if (necessidadeReserva <= 0) {
            return ReservaOperacionalFimDeSemana.vazia();
        }

        List<EstadoColaborador> candidatosOrdenados = estados.stream()
                .filter(estado -> consegueIntegrarReservaOperacional(
                        estado,
                        sabado,
                        domingo.isAfter(dataFim) ? null : domingo,
                        bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                        parametros.janelaRotacaoFinsDeSemanaSemanas()
                ))
                .sorted(Comparator
                        .comparingInt(this::prioridadePerfilParaReservaDeFimDeSemana)
                        .thenComparingInt(estado -> prioridadeGrupoRotacaoPlaneado(estado, sabado))
                        .thenComparingDouble(EstadoColaborador::utilizacaoContratualAtual)
                        .thenComparing((EstadoColaborador estado) -> !estado.consegueCobrirFimDeSemanaCompleto(
                                sabado,
                                domingo.isAfter(dataFim) ? null : domingo,
                                bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                                parametros.janelaRotacaoFinsDeSemanaSemanas()
                        ))
                        .thenComparingInt(EstadoColaborador::turnosAtribuidos)
                        .thenComparingLong(EstadoColaborador::minutosAtribuidos)
                        .thenComparing(estado -> valorOuTraco(estado.ligacao().getIdUtilizador().getNome()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (candidatosOrdenados.isEmpty()) {
            return ReservaOperacionalFimDeSemana.vazia();
        }

        List<EstadoColaborador> candidatosNucleares = candidatosOrdenados.stream()
                .filter(estado -> estado.consegueCobrirFimDeSemanaCompleto(
                        sabado,
                        domingo.isAfter(dataFim) ? null : domingo,
                        bloqueiosPorUtilizador.getOrDefault(estado.idUtilizador(), Set.of()),
                        parametros.janelaRotacaoFinsDeSemanaSemanas()
                ))
                .toList();

        int necessidadeNucleo = calcularTamanhoNucleoReservaOperacional(necessidadeSabado, necessidadeDomingo);
        LinkedHashSet<Integer> nucleares = selecionarReserva(
                candidatosNucleares.isEmpty() ? candidatosOrdenados : candidatosNucleares,
                necessidadeNucleo
        );
        if (nucleares.size() < necessidadeNucleo) {
            nucleares.addAll(selecionarReserva(candidatosOrdenados, necessidadeNucleo));
        }

        LinkedHashSet<Integer> apoio = selecionarReserva(
                candidatosOrdenados.stream()
                        .filter(estado -> !nucleares.contains(estado.idUtilizador()))
                        .toList(),
                Math.max(0, necessidadeReserva - nucleares.size())
        );
        return new ReservaOperacionalFimDeSemana(nucleares, apoio);
    }

    private int calcularTamanhoNucleoReservaOperacional(int necessidadeSabado, int necessidadeDomingo) {
        int necessidadeMaxima = Math.max(necessidadeSabado, necessidadeDomingo);
        if (necessidadeMaxima <= 0) {
            return 0;
        }

        // Preservamos um nucleo forte para o fim de semana, mas evitamos reservar
        // logo toda a lotacao do dia. Isso ajuda a alternar melhor a equipa entre
        // fins de semana consecutivos quando existem preferencias permanentes,
        // reforcos part-time e rotacao obrigatoria.
        int restantesAposChefia = Math.max(0, necessidadeMaxima - 2);
        return Math.max(2, 2 + (restantesAposChefia / 2));
    }

    private LinkedHashSet<Integer> selecionarReserva(Collection<EstadoColaborador> estados, int limite) {
        if (estados == null || estados.isEmpty() || limite <= 0) {
            return new LinkedHashSet<>();
        }

        LinkedHashSet<Integer> selecionados = new LinkedHashSet<>();
        for (EstadoColaborador estado : estados) {
            if (selecionados.size() >= limite) {
                break;
            }
            selecionados.add(estado.idUtilizador());
        }
        return selecionados;
    }

    private int calcularNecessidadeMinimaDoDia(LocalDate data,
                                               List<Turno> turnosBase,
                                               ParametrosGeracao parametros,
                                               Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData) {
        if (data == null) {
            return 0;
        }

        ConfiguracaoDiaEspecial configuracaoDia = configuracoesPorData.get(data);
        if (configuracaoDia != null && configuracaoDia.lojaEncerrada()) {
            return 0;
        }

        List<Turno> turnosDoDia = configuracaoDia != null ? configuracaoDia.turnosCompativeis() : turnosBase;
        if (turnosDoDia == null || turnosDoDia.isEmpty()) {
            return 0;
        }

        int minimoGenerico = configuracaoDia != null && configuracaoDia.minimoColaboradoresTurno() != null
                ? configuracaoDia.minimoColaboradoresTurno()
                : 0;

        int total = 0;
        for (Turno turno : turnosDoDia) {
            Integer minimoTurno = configuracaoDia != null && configuracaoDia.minimoColaboradoresTurno() != null
                    ? configuracaoDia.minimoColaboradoresTurno()
                    : parametros.minimosPorTurno().get(turno.getId());
            int minimo = minimoTurno != null ? minimoTurno : minimoGenerico;
            total += Math.max(minimo, 0);
        }
        return total;
    }

    private boolean consegueIntegrarReservaOperacional(EstadoColaborador estado,
                                                       LocalDate sabado,
                                                       LocalDate domingo,
                                                       Set<LocalDate> bloqueios,
                                                       int janelaRotacaoFinsDeSemanaSemanas) {
        if (estado == null || sabado == null) {
            return false;
        }

        boolean podeSabado = estado.podeIntegrarReservaNoDia(sabado, bloqueios, janelaRotacaoFinsDeSemanaSemanas);
        boolean podeDomingo = domingo == null || estado.podeIntegrarReservaNoDia(domingo, bloqueios, janelaRotacaoFinsDeSemanaSemanas);
        return podeSabado || podeDomingo;
    }

    private int prioridadePerfilParaReservaDeFimDeSemana(EstadoColaborador estado) {
        if (estado.cumpreChefiaObrigatoriaAoSabado()) {
            return 0;
        }
        if (estado.ehPartTime()) {
            return 1;
        }
        if (estado.ehReforcoFimDeSemana()) {
            return 2;
        }
        if (estado.ehGestao()) {
            return 3;
        }
        return 4;
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
            boolean correspondeTipo = false;
            for (String alias : aliasesTurno(tipoTurno)) {
                if (!alias.isBlank() && descricao.contains(alias)) {
                    correspondeTipo = true;
                    break;
                }
            }
            if (!correspondeTipo) {
                continue;
            }

            if (!respeitaDuracaoPreferida(descricao, turno)) {
                continue;
            }
            return true;
        }
        return false;
    }

    private boolean respeitaDuracaoPreferida(String descricaoPreferencia, Turno turno) {
        if (descricaoPreferencia == null || descricaoPreferencia.isBlank() || turno == null) {
            return true;
        }

        boolean prefereCurto = descricaoPreferencia.contains("duracao preferida: curto")
                || descricaoPreferencia.contains("turnos curtos")
                || descricaoPreferencia.contains("turno curto");
        boolean prefereLongo = descricaoPreferencia.contains("duracao preferida: longo")
                || descricaoPreferencia.contains("turnos longos")
                || descricaoPreferencia.contains("turno longo");

        if (!prefereCurto && !prefereLongo) {
            return true;
        }

        boolean turnoCurto = calcularDuracaoEmMinutos(turno) <= 300;
        if (prefereCurto) {
            return turnoCurto;
        }
        return !turnoCurto;
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
            LocalDate fim = preferencia.getDataFim() != null ? preferencia.getDataFim() : dataFim;
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

    private Map<LocalDate, ConfiguracaoDiaEspecial> construirConfiguracoesEspeciaisPorData(Loja loja,
                                                                                            List<Turno> turnosBase,
                                                                                            List<HorarioEspecialLoja> horariosEspeciais) {
        Map<LocalDate, ConfiguracaoDiaEspecial> configuracoes = new LinkedHashMap<>();
        if (loja == null || horariosEspeciais == null || horariosEspeciais.isEmpty()) {
            return configuracoes;
        }

        for (HorarioEspecialLoja horarioEspecial : horariosEspeciais) {
            if (horarioEspecial.getDataInicio() == null || horarioEspecial.getDataFim() == null) {
                continue;
            }

            LocalDate dataAtual = horarioEspecial.getDataInicio();
            while (!dataAtual.isAfter(horarioEspecial.getDataFim())) {
                configuracoes.put(dataAtual, criarConfiguracaoDiaEspecial(loja, turnosBase, horarioEspecial));
                dataAtual = dataAtual.plusDays(1);
            }
        }

        return configuracoes;
    }

    private ConfiguracaoDiaEspecial criarConfiguracaoDiaEspecial(Loja loja,
                                                                 List<Turno> turnosBase,
                                                                 HorarioEspecialLoja horarioEspecial) {
        boolean lojaEncerrada = Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada());
        if (lojaEncerrada) {
            return new ConfiguracaoDiaEspecial(true, List.of(), null, horarioEspecial.getDescricao());
        }

        LocalTime horaAbertura = horarioEspecial.getHoraAbertura();
        LocalTime horaFecho = horarioEspecial.getHoraFecho();
        List<Turno> turnosCompativeis = (horaAbertura != null && horaFecho != null)
                ? filtrarTurnosCompativeis(turnosBase, horaAbertura, horaFecho)
                : turnosBase;

        return new ConfiguracaoDiaEspecial(
                false,
                turnosCompativeis,
                horarioEspecial.getMinimoColaboradoresTurno(),
                horarioEspecial.getDescricao()
        );
    }

    private List<Turno> filtrarTurnosCompativeis(List<Turno> turnosBase, LocalTime horaAbertura, LocalTime horaFecho) {
        if (turnosBase == null || turnosBase.isEmpty()) {
            return List.of();
        }
        if (horaAbertura == null || horaFecho == null) {
            return turnosBase;
        }

        List<Turno> turnosComCorrespondenciaExata = turnosBase.stream()
                .filter(turno -> turno != null)
                .filter(turno -> horaAbertura.equals(turno.getHoraInicio()) && horaFecho.equals(turno.getHoraFim()))
                .toList();
        if (!turnosComCorrespondenciaExata.isEmpty()) {
            return turnosComCorrespondenciaExata;
        }

        return turnosBase.stream()
                .filter(turno -> turnoCabeNoHorario(turno, horaAbertura, horaFecho))
                .toList();
    }

    private boolean turnoCabeNoHorario(Turno turno, LocalTime horaAbertura, LocalTime horaFecho) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null
                || horaAbertura == null || horaFecho == null) {
            return false;
        }
        return !turno.getHoraInicio().isBefore(horaAbertura)
                && !turno.getHoraFim().isAfter(horaFecho);
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
                .orElse(8);

        int descansoSemanalMinimoDias = regras.stream()
                .filter(this::ehRegraDescansoSemanal)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 1 && valor <= 6)
                .findFirst()
                .orElse(2);

        int janelaRotacaoFinsDeSemanaSemanas = regras.stream()
                .filter(this::ehRegraRotacaoFinsDeSemana)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 2)
                .findFirst()
                .orElse(2);

        int diaLimiteLancamento = regras.stream()
                .filter(this::ehRegraDiaLimiteLancamento)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .filter(valor -> valor >= 1 && valor <= 31)
                .findFirst()
                .orElse(15);

        boolean exigirChefiaAoSabado = regras.stream()
                .filter(this::ehRegraChefiaAoSabado)
                .map(RegraAplicada::valor)
                .filter(Objects::nonNull)
                .findFirst()
                .map(valor -> valor > 0)
                .orElse(true);

        Map<PerfilContratual, Long> cargaMaximaMinutosPorPerfil = new EnumMap<>(PerfilContratual.class);
        for (PerfilContratual perfilContratual : PerfilContratual.values()) {
            Integer horasMensais = regras.stream()
                    .filter(this::ehRegraCargaContratual)
                    .filter(regra -> regraMencionaPerfilContratual(regra, perfilContratual))
                    .map(RegraAplicada::valor)
                    .filter(Objects::nonNull)
                    .filter(valor -> valor > 0)
                    .findFirst()
                    .orElse(perfilContratual.cargaMensalHorasPadrao());

            if (horasMensais == null || horasMensais <= 0) {
                throw new IllegalArgumentException("Nao existe uma regra de carga contratual valida para o perfil " + perfilContratual.descricaoCurta() + ".");
            }

            cargaMaximaMinutosPorPerfil.put(perfilContratual, horasMensais * 60L);
        }

        return new ParametrosGeracao(
                minimosPorTurno,
                maxDiasConsecutivos,
                descansoMinimoHoras,
                descansoSemanalMinimoDias,
                janelaRotacaoFinsDeSemanaSemanas,
                diaLimiteLancamento,
                exigirChefiaAoSabado,
                cargaMaximaMinutosPorPerfil
        );
    }

    private boolean ehRegraDeMinimos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        boolean mencionaMinimo = texto.contains("min") || texto.contains("minim");
        boolean mencionaCobertura = texto.contains("colaborador")
                || texto.contains("equipa")
                || texto.contains("pessoas")
                || texto.contains("funcionario")
                || (texto.contains("turno") && (texto.contains("por") || texto.contains("cobertura") || texto.contains("loja")));
        boolean pareceOutraRegra = texto.contains("descanso")
                || texto.contains("carga")
                || texto.contains("contrat")
                || texto.contains("lancamento")
                || texto.contains("publicacao");
        return mencionaMinimo && mencionaCobertura && !pareceOutraRegra;
    }

    private boolean ehRegraDiasConsecutivos(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("dias") || texto.contains("dia"))
                && (texto.contains("consecut") || texto.contains("seguid"))
                && !texto.contains("hora");
    }

    private boolean ehRegraDescanso(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("descanso") && (texto.contains("hora") || texto.contains("interval")))
                || (texto.contains("entre") && texto.contains("turno"));
    }

    private boolean ehRegraDescansoSemanal(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("descanso")
                && (texto.contains("seman") || texto.contains("folga"))
                && texto.contains("dia");
    }

    private boolean ehRegraRotacaoFinsDeSemana(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return (texto.contains("rotacao") || texto.contains("janela"))
                && (texto.contains("fim de semana") || texto.contains("weekend"));
    }

    private boolean ehRegraDiaLimiteLancamento(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("dia")
                && texto.contains("limite")
                && (texto.contains("lancamento") || texto.contains("publicacao") || texto.contains("publicar"));
    }

    private boolean ehRegraChefiaAoSabado(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("sabado")
                && (texto.contains("gerente") || texto.contains("subgerente") || texto.contains("chefia") || texto.contains("gestao"));
    }

    private boolean ehRegraCargaContratual(RegraAplicada regra) {
        String texto = regra.textoNormalizado();
        return texto.contains("carga")
                && (texto.contains("contrat") || texto.contains("mensal"))
                && (texto.contains("hora") || texto.contains("horas"));
    }

    private boolean regraMencionaTurno(RegraAplicada regra, String tipoTurno) {
        for (String alias : aliasesTurno(tipoTurno)) {
            if (!alias.isBlank() && regra.textoNormalizado().contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private boolean regraMencionaPerfilContratual(RegraAplicada regra, PerfilContratual perfilContratual) {
        return perfilContratual.correspondeRegra(regra.textoNormalizado());
    }

    private List<Lojautilizador> filtrarColaboradoresSelecionados(List<Lojautilizador> colaboradoresElegiveis,
                                                                  Collection<Integer> idsColaboradoresSelecionados) {
        if (idsColaboradoresSelecionados == null) {
            return colaboradoresElegiveis;
        }

        Set<Integer> idsSelecionados = normalizarIds(idsColaboradoresSelecionados);
        if (idsSelecionados.isEmpty()) {
            throw new IllegalArgumentException("Seleciona pelo menos um colaborador para gerar o horario.");
        }

        Set<Integer> idsElegiveis = new LinkedHashSet<>();
        for (Lojautilizador ligacao : colaboradoresElegiveis) {
            if (ligacao.getIdUtilizador() != null && ligacao.getIdUtilizador().getId() != null) {
                idsElegiveis.add(ligacao.getIdUtilizador().getId());
            }
        }

        List<Integer> idsInvalidos = idsSelecionados.stream()
                .filter(id -> !idsElegiveis.contains(id))
                .toList();
        if (!idsInvalidos.isEmpty()) {
            throw new IllegalArgumentException(
                    "Foram selecionados colaboradores que nao estao elegiveis neste periodo: "
                            + String.join(", ", idsInvalidos.stream().map(String::valueOf).toList())
                            + "."
            );
        }

        return colaboradoresElegiveis.stream()
                .filter(ligacao -> ligacao.getIdUtilizador() != null)
                .filter(ligacao -> idsSelecionados.contains(ligacao.getIdUtilizador().getId()))
                .toList();
    }

    private List<String> aliasesTurno(String tipoTurno) {
        return switch (tipoTurno) {
            case "manha" -> List.of("manha", "abertura", "morning", "cedo");
            case "intermedio", "tarde" -> List.of(
                    "intermedio",
                    "tarde",
                    "afternoon",
                    "meio dia",
                    "meio-dia",
                    "central"
            );
            case "noite" -> List.of("noite", "fecho", "encerramento", "night");
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

    private List<Lojautilizador> obterColaboradoresElegiveis(Integer idLoja, LocalDate dataInicio, LocalDate dataFim) {
        Map<Integer, Lojautilizador> ativos = new LinkedHashMap<>();
        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (!ligacaoTemRelevanciaNoPeriodo(ligacao, dataInicio, dataFim)) {
                continue;
            }
            if (!utilizadorEstaAtivo(ligacao)) {
                continue;
            }
            if (resolverPerfilContratual(ligacao).isEmpty()) {
                continue;
            }
            ativos.merge(ligacao.getIdUtilizador().getId(), ligacao, this::preferirLigacaoMaisRecente);
        }
        return new ArrayList<>(ativos.values());
    }

    private boolean ligacaoTemRelevanciaNoPeriodo(Lojautilizador ligacao, LocalDate dataInicio, LocalDate dataFim) {
        if (ligacao == null
                || ligacao.getIdUtilizador() == null
                || ligacao.getIdUtilizador().getId() == null
                || ligacao.getIdCargo() == null
                || ligacao.getIdLoja() == null
                || ligacao.getIdLoja().getId() == null
                || ligacao.getDataInicio() == null) {
            return false;
        }

        if (ligacao.getDataInicio().isAfter(dataFim)) {
            return false;
        }

        return ligacao.getDataFim() == null || !ligacao.getDataFim().isBefore(dataInicio);
    }

    private boolean utilizadorEstaAtivo(Lojautilizador ligacao) {
        return ligacao.getIdUtilizador() != null
                && "ativo".equals(normalizarTexto(ligacao.getIdUtilizador().getEstado()));
    }

    private LocalDate resolverInicioHistoricoParaGeracao(LocalDate dataInicio, ParametrosGeracao parametros) {
        LocalDate inicioConsecutivos = dataInicio.minusDays(Math.max(1, parametros.maxDiasConsecutivos()));
        LocalDate inicioSemanaAtual = inicioSemana(dataInicio);
        LocalDate inicioRotacao = dataInicio.minusWeeks(Math.max(1, parametros.janelaRotacaoFinsDeSemanaSemanas()));

        LocalDate inicioHistorico = inicioConsecutivos;
        if (inicioSemanaAtual.isBefore(inicioHistorico)) {
            inicioHistorico = inicioSemanaAtual;
        }
        if (inicioRotacao.isBefore(inicioHistorico)) {
            inicioHistorico = inicioRotacao;
        }
        return inicioHistorico;
    }

    private void validarJanelaDeLancamento(LocalDate dataInicioPeriodo, int diaLimiteLancamento) {
        LocalDate mesAnterior = dataInicioPeriodo.minusMonths(1);
        int diaNormalizado = Math.min(diaLimiteLancamento, mesAnterior.lengthOfMonth());
        LocalDate dataLimite = mesAnterior.withDayOfMonth(diaNormalizado);

        if (LocalDate.now().isAfter(dataLimite)) {
            throw new IllegalArgumentException(
                    "A proposta de "
                            + nomeMes(dataInicioPeriodo.getMonthValue())
                            + " de "
                            + dataInicioPeriodo.getYear()
                            + " tinha de ser lancada ate "
                            + DATA_FORMATTER.format(dataLimite)
                            + "."
            );
        }
    }

    private Lojautilizador preferirLigacaoMaisRecente(Lojautilizador ligacaoAtual, Lojautilizador novaLigacao) {
        if (ligacaoAtual == null) {
            return novaLigacao;
        }
        if (novaLigacao == null) {
            return ligacaoAtual;
        }

        LocalDate inicioAtual = ligacaoAtual.getDataInicio();
        LocalDate inicioNovo = novaLigacao.getDataInicio();
        if (inicioAtual == null) {
            return novaLigacao;
        }
        if (inicioNovo == null) {
            return ligacaoAtual;
        }
        if (inicioNovo.isAfter(inicioAtual)) {
            return novaLigacao;
        }
        if (inicioAtual.isAfter(inicioNovo)) {
            return ligacaoAtual;
        }

        if (ligacaoAtual.getDataFim() == null && novaLigacao.getDataFim() != null) {
            return ligacaoAtual;
        }
        if (ligacaoAtual.getDataFim() != null && novaLigacao.getDataFim() == null) {
            return novaLigacao;
        }
        return ligacaoAtual;
    }

    private void validarEstadoAtualDaLoja(Integer idLoja,
                                          Integer ano,
                                          Integer mes,
                                          LocalDate dataInicio,
                                          LocalDate dataFim) {
        boolean existePropostaAprovada = propostaHorarioMensalRepository
                .findByIdLojaIdAndAnoAndMesOrderByDataGeracaoDesc(idLoja, ano, mes)
                .stream()
                .anyMatch(proposta -> ESTADO_APROVADO.equals(normalizarTexto(proposta.getEstado())));

        if (existePropostaAprovada) {
            throw new IllegalArgumentException("Ja existe uma proposta aprovada para o periodo selecionado.");
        }

        long horariosExistentes = horarioRepository.countHorariosVisiveisDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        if (horariosExistentes > 0) {
            throw new IllegalArgumentException("Ja existem horarios publicados neste periodo. Nao e seguro gerar uma nova proposta mensal.");
        }
    }

    private String criarResumoGeracao(PlaneamentoGerado planeamento,
                                      PoliticaOtimizacao politica,
                                      MetricasPlaneamento metricas) {
        int colaboradoresComTurnos = (int) planeamento.estados().stream()
                .filter(estado -> estado.turnosAtribuidos() > 0)
                .count();
        return "Modelo IO: satisfacao de restricoes com funcao objetivo ponderada ("
                + politica.nome()
                + "). Proposta gerada automaticamente com "
                + planeamento.horarios().size()
                + " turnos distribuidos por "
                + colaboradoresComTurnos
                + " colaboradores. Pontuacao: "
                + metricas.pontuacao()
                + " (menor e melhor). Carga: desvio medio "
                + metricas.desvioMedioHoras()
                + ", amplitude "
                + metricas.amplitudeHoras()
                + ". Politica: "
                + politica.descricao()
                + ".";
    }

    private MetricasPlaneamento calcularMetricasPlaneamento(PlaneamentoGerado planeamento, PoliticaOtimizacao politica) {
        Map<Integer, CargaColaborador> cargas = new LinkedHashMap<>();
        for (EstadoColaborador estado : planeamento.estados()) {
            cargas.put(
                    estado.idUtilizador(),
                    new CargaColaborador(
                            estado.idUtilizador(),
                            valorOuTraco(estado.ligacao().getIdUtilizador().getNome()),
                            estado.minutosAtribuidos(),
                            estado.cargaMaximaMensalMinutos(),
                            estado.totalFinsDeSemanaTrabalhados()
                    )
            );
        }
        return calcularMetricasPlaneamento(planeamento.horarios(), cargas, politica);
    }

    private MetricasPlaneamento calcularMetricasPlaneamento(List<Horario> horarios, PoliticaOtimizacao politica) {
        Map<Integer, CargaColaborador> cargas = new LinkedHashMap<>();
        Map<Integer, Set<LocalDate>> finsDeSemanaPorColaborador = new HashMap<>();
        for (Horario horario : horarios) {
            if (horario.getIdLojautilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador() == null
                    || horario.getIdLojautilizador().getIdUtilizador().getId() == null) {
                continue;
            }

            Integer idColaborador = horario.getIdLojautilizador().getIdUtilizador().getId();
            long minutosTurno = calcularDuracaoEmMinutos(horario.getIdTurno());
            CargaColaborador atual = cargas.get(idColaborador);
            long minutos = (atual != null ? atual.minutos() : 0) + minutosTurno;
            long cargaMaxima = resolverPerfilContratual(horario.getIdLojautilizador())
                    .map(PerfilContratual::cargaMensalHorasPadrao)
                    .map(horas -> horas * 60L)
                    .orElse(0L);

            if (horario.getDataTurno() != null && ehFimDeSemana(horario.getDataTurno())) {
                finsDeSemanaPorColaborador
                        .computeIfAbsent(idColaborador, ignored -> new LinkedHashSet<>())
                        .add(inicioFimDeSemana(horario.getDataTurno()));
            }

            cargas.put(
                    idColaborador,
                    new CargaColaborador(
                            idColaborador,
                            valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome()),
                            minutos,
                            cargaMaxima,
                            finsDeSemanaPorColaborador.getOrDefault(idColaborador, Set.of()).size()
                    )
            );
        }
        return calcularMetricasPlaneamento(horarios, cargas, politica);
    }

    private MetricasPlaneamento calcularMetricasPlaneamento(List<Horario> horarios,
                                                           Map<Integer, CargaColaborador> cargas,
                                                           PoliticaOtimizacao politica) {
        PoliticaOtimizacao politicaSegura = politica != null ? politica : PoliticaOtimizacao.EQUILIBRIO;
        if (cargas.isEmpty()) {
            return new MetricasPlaneamento(
                    1000,
                    "Sem dados",
                    politicaSegura.nome(),
                    politicaSegura.descricao(),
                    "0h 0m",
                    "0h 0m",
                    0,
                    "Sem colaboradores elegiveis para avaliar."
            );
        }

        List<Long> minutos = cargas.values().stream()
                .map(CargaColaborador::minutos)
                .toList();
        long minimo = minutos.stream().mapToLong(Long::longValue).min().orElse(0);
        long maximo = minutos.stream().mapToLong(Long::longValue).max().orElse(0);
        double media = minutos.stream().mapToLong(Long::longValue).average().orElse(0);
        long desvioMedio = Math.round(minutos.stream()
                .mapToDouble(valor -> Math.abs(valor - media))
                .average()
                .orElse(0));

        List<Integer> finsDeSemana = cargas.values().stream()
                .map(CargaColaborador::finsDeSemanaTrabalhados)
                .toList();
        int minimoFinsDeSemana = finsDeSemana.stream().mapToInt(Integer::intValue).min().orElse(0);
        int maximoFinsDeSemana = finsDeSemana.stream().mapToInt(Integer::intValue).max().orElse(0);
        int amplitudeFinsDeSemana = maximoFinsDeSemana - minimoFinsDeSemana;

        long amplitude = maximo - minimo;
        int pontuacao = (int) Math.round(
                (amplitude / 30.0)
                        + (desvioMedio / 15.0)
                        + (amplitudeFinsDeSemana * 20.0)
                        + penalizacaoUtilizacaoContratual(cargas.values())
        );
        String qualidade;
        if (pontuacao <= 60) {
            qualidade = "Alta";
        } else if (pontuacao <= 120) {
            qualidade = "Boa";
        } else {
            qualidade = "A rever";
        }

        String resumo = qualidade
                + " · score "
                + pontuacao
                + " · desvio medio "
                + formatarDuracao(desvioMedio)
                + " · amplitude "
                + formatarDuracao(amplitude)
                + " · amplitude FDS "
                + amplitudeFinsDeSemana;

        return new MetricasPlaneamento(
                pontuacao,
                qualidade,
                politicaSegura.nome(),
                politicaSegura.descricao(),
                formatarDuracao(amplitude),
                formatarDuracao(desvioMedio),
                amplitudeFinsDeSemana,
                resumo
        );
    }

    private double penalizacaoUtilizacaoContratual(Collection<CargaColaborador> cargas) {
        List<Double> utilizacoes = cargas.stream()
                .filter(carga -> carga.cargaMaximaMinutos() > 0)
                .map(carga -> carga.minutos() / (double) carga.cargaMaximaMinutos())
                .toList();
        if (utilizacoes.isEmpty()) {
            return 0;
        }

        double media = utilizacoes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return utilizacoes.stream()
                .mapToDouble(valor -> Math.abs(valor - media) * 35)
                .sum();
    }

    private PoliticaOtimizacao extrairPoliticaOtimizacao(String resumoGeracao) {
        String resumoNormalizado = normalizarTexto(resumoGeracao);
        for (PoliticaOtimizacao politica : PoliticaOtimizacao.values()) {
            if (resumoNormalizado.contains(normalizarTexto(politica.nome()))) {
                return politica;
            }
        }
        return PoliticaOtimizacao.EQUILIBRIO;
    }

    private PropostaResumo construirResumoProposta(PropostaHorarioMensal proposta, PropostaResultado resultado) {
        return new PropostaResumo(
                proposta.getId(),
                rotuloCurtoProposta(resultado),
                resultado.estado(),
                resultado.dataGeracao(),
                resultado.geradoPor(),
                resultado.metricas().politicaOtimizacao(),
                resultado.metricas().pontuacao(),
                resultado.metricas().qualidade(),
                resultado.metricas().desvioMedioHoras(),
                resultado.metricas().amplitudeHoras(),
                resultado.resumo().colaboradores(),
                resultado.resumo().turnos(),
                resultado.resumo().diasCobertos()
        );
    }

    private Map<Integer, ResumoColaborador> indexarResumoPorColaborador(List<ResumoColaborador> resumos) {
        Map<Integer, ResumoColaborador> porColaborador = new LinkedHashMap<>();
        for (ResumoColaborador resumo : resumos) {
            if (resumo.idColaborador() != null) {
                porColaborador.put(resumo.idColaborador(), resumo);
            }
        }
        return porColaborador;
    }

    private DiferencaColaborador construirDiferencaColaborador(ResumoColaborador base, ResumoColaborador comparada) {
        ResumoColaborador referencia = base != null ? base : comparada;
        long minutosBase = base != null ? base.minutos() : 0;
        long minutosComparada = comparada != null ? comparada.minutos() : 0;
        int turnosBase = base != null ? base.turnos() : 0;
        int turnosComparada = comparada != null ? comparada.turnos() : 0;

        return new DiferencaColaborador(
                referencia.idColaborador(),
                referencia.nome(),
                referencia.cargo(),
                turnosBase,
                formatarDuracao(minutosBase),
                turnosComparada,
                formatarDuracao(minutosComparada),
                turnosComparada - turnosBase,
                formatarDiferencaDuracao(minutosComparada - minutosBase)
        );
    }

    private Map<String, String> indexarAtribuicoesPorSlot(List<HorarioLinha> linhas) {
        Map<String, String> atribuicoes = new LinkedHashMap<>();
        for (HorarioLinha linha : linhas) {
            if (linha.data() == null) {
                continue;
            }
            String chave = linha.data() + "|" + linha.turno() + "|" + linha.periodo();
            atribuicoes.put(chave, linha.colaborador());
        }
        return atribuicoes;
    }

    private Optional<LocalDate> extrairDataDoSlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return Optional.empty();
        }
        int separador = slot.indexOf('|');
        String data = separador >= 0 ? slot.substring(0, separador) : slot;
        try {
            return Optional.of(LocalDate.parse(data));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String rotuloCurtoProposta(PropostaResultado proposta) {
        if (proposta == null || proposta.idProposta() == null) {
            return "Horarios publicados";
        }
        return "#"
                + proposta.idProposta()
                + " · "
                + proposta.estado()
                + " · "
                + proposta.metricas().politicaOtimizacao();
    }

    private String criarObservacaoHistoricoDecisao(PropostaHorarioMensal proposta, String novoEstado) {
        String acao = ESTADO_APROVADO.equals(normalizarTexto(novoEstado)) ? "aprovado" : "rejeitado";
        String observacoes = limparTexto(proposta.getObservacoesSupervisor());
        if (observacoes == null) {
            return "Horario " + acao + " pelo supervisor.";
        }
        return "Horario " + acao + " pelo supervisor. Observacoes: " + observacoes;
    }

    private PropostaResultado construirResultado(PropostaHorarioMensal proposta, List<Horario> horarios) {
        return construirResultado(
                proposta.getId(),
                valorOuTraco(proposta.getIdLoja().getNome()),
                proposta.getAno(),
                proposta.getMes(),
                nomeMes(proposta.getMes()),
                formatarEstado(proposta.getEstado()),
                construirOrigemPlaneamento(proposta),
                proposta.getResumoGeracao(),
                valorOuTraco(proposta.getIdUtilizadorGeracao().getNome()),
                proposta.getDataGeracao() != null ? DATA_HORA_FORMATTER.format(proposta.getDataGeracao()) : "-",
                proposta.getIdUtilizadorDecisao() != null ? valorOuTraco(proposta.getIdUtilizadorDecisao().getNome()) : "-",
                proposta.getDataDecisao() != null ? DATA_HORA_FORMATTER.format(proposta.getDataDecisao()) : "-",
                valorOuTraco(proposta.getObservacoesSupervisor()),
                ESTADO_PENDENTE.equals(normalizarTexto(proposta.getEstado())),
                horarios
        );
    }

    private PropostaResultado construirResultadoHorariosPublicados(Loja loja,
                                                                   int ano,
                                                                   int mes,
                                                                   List<Horario> horarios) {
        String resumoPublicacao = "Foram encontrados "
                + horarios.size()
                + " turnos ja publicados para "
                + nomeMes(mes).toLowerCase(Locale.ROOT)
                + " de "
                + ano
                + ". Podes consultar o planeamento atual, mesmo sem existir uma proposta mensal guardada.";

        return construirResultado(
                null,
                valorOuTraco(loja.getNome()),
                ano,
                mes,
                nomeMes(mes),
                "Publicado",
                "Horarios publicados",
                resumoPublicacao,
                "-",
                "-",
                "-",
                "-",
                "Estes horarios ja estao publicados na loja e podem ser analisados diretamente neste ecra.",
                false,
                horarios
        );
    }

    private PropostaResultado construirResultado(Integer idProposta,
                                                 String nomeLoja,
                                                 Integer ano,
                                                 Integer mes,
                                                 String nomeMes,
                                                 String estado,
                                                 String origemPlaneamento,
                                                 String resumoGeracao,
                                                 String geradoPor,
                                                 String dataGeracao,
                                                 String decididoPor,
                                                 String dataDecisao,
                                                 String observacoesSupervisor,
                                                 boolean podeSerDecidida,
                                                 List<Horario> horarios) {
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
                idProposta,
                nomeLoja,
                ano,
                mes,
                nomeMes,
                estado,
                origemPlaneamento,
                resumoGeracao,
                geradoPor,
                dataGeracao,
                decididoPor,
                dataDecisao,
                observacoesSupervisor,
                podeSerDecidida,
                linhas,
                resumoColaboradores,
                calcularMetricasPlaneamento(horarios, extrairPoliticaOtimizacao(resumoGeracao)),
                new ResumoGeral(
                        resumoColaboradores.size(),
                        horarios.size(),
                        diasCobertos.size()
                )
        );
    }

    private HorarioLinha mapearLinhaHorario(Horario horario) {
        Integer idColaborador = horario.getIdLojautilizador() != null && horario.getIdLojautilizador().getIdUtilizador() != null
                ? horario.getIdLojautilizador().getIdUtilizador().getId()
                : null;
        String colaborador = horario.getIdLojautilizador() != null && horario.getIdLojautilizador().getIdUtilizador() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdUtilizador().getNome())
                : "-";
        String cargo = horario.getIdLojautilizador() != null && horario.getIdLojautilizador().getIdCargo() != null
                ? valorOuTraco(horario.getIdLojautilizador().getIdCargo().getNome())
                : "-";

        return new HorarioLinha(
                idColaborador,
                horario.getDataTurno(),
                horario.getDataTurno() != null ? nomeDiaSemana(horario.getDataTurno()) : "-",
                formatarTurno(horario.getIdTurno()),
                formatarPeriodo(horario.getIdTurno()),
                colaborador,
                cargo,
                formatarEstado(horario.getEstado())
        );
    }

    private String construirOrigemPlaneamento(PropostaHorarioMensal proposta) {
        String estadoNormalizado = normalizarTexto(proposta.getEstado());
        return switch (estadoNormalizado) {
            case ESTADO_APROVADO -> "Horarios publicados a partir de proposta aprovada";
            case ESTADO_REJEITADO -> "Proposta mensal rejeitada";
            case ESTADO_RASCUNHO -> "Rascunho do gerente";
            case ESTADO_PENDENTE -> "Enviada ao supervisor";
            default -> "Proposta mensal";
        };
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

    private Lojautilizador obterLigacaoAtivaComAcessoAoPainel(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para o utilizador autenticado."));

        if (!temPermissaoDeGeracao(ligacaoAtiva) && !temPermissaoDeValidacao(ligacaoAtiva)) {
            throw new IllegalArgumentException("Nao tens permissao para consultar o painel de horarios da loja.");
        }
        return ligacaoAtiva;
    }

    private Lojautilizador obterLigacaoAtivaComPermissaoDeValidacao(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para o utilizador autenticado."));

        if (!temPermissaoDeValidacao(ligacaoAtiva)) {
            throw new IllegalArgumentException("Nao tens permissao para validar a proposta mensal.");
        }
        return ligacaoAtiva;
    }

    private boolean temPermissaoDeGeracao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        return tipoCargo != null && CARGOS_COM_GERACAO.contains(normalizarTexto(tipoCargo));
    }

    private boolean temPermissaoDeValidacao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        return tipoCargo != null && CARGOS_COM_VALIDACAO.contains(normalizarTexto(tipoCargo));
    }

    private Optional<PerfilContratual> resolverPerfilContratual(Lojautilizador ligacao) {
        if (ligacao == null || ligacao.getIdCargo() == null) {
            return Optional.empty();
        }
        return PerfilContratual.fromCargoTipo(ligacao.getIdCargo().getTipo());
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

    private int normalizarQuantidadeAlternativas(Integer quantidade) {
        if (quantidade == null) {
            return 1;
        }
        if (quantidade < 1 || quantidade > 20) {
            throw new IllegalArgumentException("Seleciona entre 1 e 20 alternativas por geracao.");
        }
        return quantidade;
    }

    private Set<Integer> normalizarIds(Collection<Integer> ids) {
        Set<Integer> idsNormalizados = new LinkedHashSet<>();
        if (ids == null) {
            return idsNormalizados;
        }

        for (Integer id : ids) {
            if (id != null && id > 0) {
                idsNormalizados.add(id);
            }
        }
        return idsNormalizados;
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

    private LocalDate inicioSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate inicioFimDeSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));
    }

    private boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek diaSemana = data.getDayOfWeek();
        return diaSemana == DayOfWeek.SATURDAY || diaSemana == DayOfWeek.SUNDAY;
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

    private String formatarPeriodoVinculo(Lojautilizador ligacao) {
        if (ligacao == null || ligacao.getDataInicio() == null) {
            return "-";
        }
        String inicio = DATA_FORMATTER.format(ligacao.getDataInicio());
        String fim = ligacao.getDataFim() != null ? DATA_FORMATTER.format(ligacao.getDataFim()) : "sem fim";
        return inicio + " a " + fim;
    }

    private String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }
        String valor = estado.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    private String formatarDuracao(long minutosTotais) {
        long horas = minutosTotais / 60;
        long minutos = minutosTotais % 60;
        return horas + "h " + minutos + "m";
    }

    private String formatarDiferencaDuracao(long minutosTotais) {
        String sinal = minutosTotais > 0 ? "+" : minutosTotais < 0 ? "-" : "";
        return sinal + formatarDuracao(Math.abs(minutosTotais));
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

    private String limparTexto(String texto) {
        if (texto == null) {
            return null;
        }

        String textoLimpo = texto.trim();
        return textoLimpo.isEmpty() ? null : textoLimpo;
    }

    public record GeracaoContexto(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            Integer anoAtual,
            Integer mesAtual,
            boolean podeGerar,
            boolean podeValidar,
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
            String origemPlaneamento,
            String resumoGeracao,
            String geradoPor,
            String dataGeracao,
            String decididoPor,
            String dataDecisao,
            String observacoesSupervisor,
            boolean podeSerDecidida,
            List<HorarioLinha> linhas,
            List<ResumoColaborador> resumoColaboradores,
            MetricasPlaneamento metricas,
            ResumoGeral resumo
    ) {
    }

    public record HorarioLinha(
            Integer idColaborador,
            LocalDate data,
            String diaSemana,
            String turno,
            String periodo,
            String colaborador,
            String cargo,
            String estado
    ) {
    }

    public record ColaboradorElegivel(
            Integer idColaborador,
            String nome,
            String cargo,
            String perfilContratual,
            String periodoVinculo,
            boolean selecionadoPorDefeito
    ) {
        @Override
        public String toString() {
            return nome + " - " + cargo;
        }
    }

    public record MotivoFalhaGeracao(
            String codigo,
            int total,
            String descricao,
            List<String> nomes
    ) {
    }

    public record SugestaoFalhaGeracao(
            String codigo,
            String texto,
            String perfilRecomendado
    ) {
    }

    public static class FalhaGeracaoHorarioException extends IllegalArgumentException {
        private final String turno;
        private final String data;
        private final int colaboradoresConsiderados;
        private final String motivoPrincipal;
        private final List<MotivoFalhaGeracao> motivos;
        private final List<SugestaoFalhaGeracao> sugestoes;

        public FalhaGeracaoHorarioException(String mensagem,
                                            String turno,
                                            String data,
                                            int colaboradoresConsiderados,
                                            String motivoPrincipal,
                                            List<MotivoFalhaGeracao> motivos,
                                            List<SugestaoFalhaGeracao> sugestoes) {
            super(mensagem);
            this.turno = turno;
            this.data = data;
            this.colaboradoresConsiderados = colaboradoresConsiderados;
            this.motivoPrincipal = motivoPrincipal;
            this.motivos = motivos != null ? List.copyOf(motivos) : List.of();
            this.sugestoes = sugestoes != null ? List.copyOf(sugestoes) : List.of();
        }

        public String turno() {
            return turno;
        }

        public String data() {
            return data;
        }

        public int colaboradoresConsiderados() {
            return colaboradoresConsiderados;
        }

        public String motivoPrincipal() {
            return motivoPrincipal;
        }

        public List<MotivoFalhaGeracao> motivos() {
            return motivos;
        }

        public List<SugestaoFalhaGeracao> sugestoes() {
            return sugestoes;
        }
    }

    private record GargaloCobertura(
            int procura,
            int oferta,
            int deficit
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

    public record MetricasPlaneamento(
            int pontuacao,
            String qualidade,
            String politicaOtimizacao,
            String descricaoPolitica,
            String amplitudeHoras,
            String desvioMedioHoras,
            int amplitudeFinsDeSemana,
            String resumo
    ) {
    }

    public record PropostaResumo(
            Integer idProposta,
            String rotulo,
            String estado,
            String dataGeracao,
            String geradoPor,
            String politicaOtimizacao,
            int pontuacao,
            String qualidade,
            String desvioMedioHoras,
            String amplitudeHoras,
            int colaboradores,
            int turnos,
            int diasCobertos
    ) {
        @Override
        public String toString() {
            return rotulo + " · score " + pontuacao;
        }
    }

    public record ComparacaoPropostas(
            Integer idPropostaBase,
            Integer idPropostaComparada,
            String rotuloBase,
            String rotuloComparada,
            String resumo,
            int turnosDiferentes,
            int diasAfetados,
            List<DiferencaColaborador> diferencas
    ) {
    }

    public record DiferencaColaborador(
            Integer idColaborador,
            String colaborador,
            String cargo,
            int turnosBase,
            String horasBase,
            int turnosComparada,
            String horasComparada,
            int diferencaTurnos,
            String diferencaHoras
    ) {
    }

    private record ConfiguracaoDiaEspecial(
            boolean lojaEncerrada,
            List<Turno> turnosCompativeis,
            Integer minimoColaboradoresTurno,
            String descricao
    ) {
    }

    private record ParametrosGeracao(
            Map<Integer, Integer> minimosPorTurno,
            int maxDiasConsecutivos,
            int descansoMinimoHoras,
            int descansoSemanalMinimoDias,
            int janelaRotacaoFinsDeSemanaSemanas,
            int diaLimiteLancamento,
            boolean exigirChefiaAoSabado,
            Map<PerfilContratual, Long> cargaMaximaMinutosPorPerfil
    ) {
    }

    private record DadosGeracao(
            Lojautilizador ligacaoAtiva,
            Loja loja,
            int ano,
            int mes,
            LocalDate dataInicio,
            LocalDate dataFim,
            List<Lojautilizador> colaboradoresAtivos,
            List<Turno> turnos,
            ParametrosGeracao parametros,
            List<Horario> historicoHorarios,
            Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
            Map<Integer, List<Preferencia>> preferenciasTurnos,
            Map<Integer, List<Preferencia>> preferenciasColegas,
            Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData
    ) {
    }

    private record SlotTurnoDia(
            Turno turno,
            List<Turno> turnosCompativeis,
            int ordemNoTurno
    ) {
    }

    private record AtribuicaoPlaneadaDia(
            EstadoColaborador estado,
            Turno turno,
            long minutosTurno
    ) {
    }

    private record ContextoPreservacaoFimDeSemana(
            LocalDate sabado,
            List<Turno> turnosSabado,
            int minimoSabado,
            LocalDate domingo,
            List<Turno> turnosDomingo,
            int minimoDomingo,
            int minimoFimDeSemanaCompleto
    ) {
    }

    private record ReservaOperacionalFimDeSemana(
            Set<Integer> nucleares,
            Set<Integer> apoio
    ) {
        private static ReservaOperacionalFimDeSemana vazia() {
            return new ReservaOperacionalFimDeSemana(Set.of(), Set.of());
        }

        private boolean contem(Integer idUtilizador) {
            return idUtilizador != null && (nucleares.contains(idUtilizador) || apoio.contains(idUtilizador));
        }

        private boolean ehNuclear(Integer idUtilizador) {
            return idUtilizador != null && nucleares.contains(idUtilizador);
        }

        private boolean estaVazia() {
            return nucleares.isEmpty() && apoio.isEmpty();
        }
    }

    private enum PerfilContratual {
        GESTAO(Set.of("gerente", "subgerente", "supervisor"), 176, false),
        FULLTIME(Set.of("fulltime"), 176, false),
        PARTTIME(Set.of("parttime"), 96, false),
        REFORCO_FIM_DE_SEMANA(Set.of("reforco_parttime"), 64, true);

        private final Set<String> tiposCargo;
        private final int cargaMensalHorasPadrao;
        private final boolean apenasFimDeSemana;

        PerfilContratual(Set<String> tiposCargo, int cargaMensalHorasPadrao, boolean apenasFimDeSemana) {
            this.tiposCargo = tiposCargo;
            this.cargaMensalHorasPadrao = cargaMensalHorasPadrao;
            this.apenasFimDeSemana = apenasFimDeSemana;
        }

        private static Optional<PerfilContratual> fromCargoTipo(String tipoCargo) {
            String tipoNormalizado = normalizarTextoEstatico(tipoCargo);
            if (tipoNormalizado.isBlank()) {
                return Optional.empty();
            }

            for (PerfilContratual perfilContratual : values()) {
                if (perfilContratual.tiposCargo.contains(tipoNormalizado)) {
                    return Optional.of(perfilContratual);
                }
            }
            return Optional.empty();
        }

        private int cargaMensalHorasPadrao() {
            return cargaMensalHorasPadrao;
        }

        private boolean permiteData(LocalDate data) {
            if (!apenasFimDeSemana || data == null) {
                return true;
            }
            DayOfWeek dayOfWeek = data.getDayOfWeek();
            return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        }

        private boolean permiteTurno(long minutosTurno) {
            if (this != GESTAO && this != FULLTIME) {
                return true;
            }
            return minutosTurno >= DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS;
        }

        private boolean correspondeRegra(String textoNormalizado) {
            return switch (this) {
                case GESTAO -> textoNormalizado.contains("gestao")
                        || textoNormalizado.contains("gerencia")
                        || textoNormalizado.contains("gestor")
                        || textoNormalizado.contains("supervisor");
                case FULLTIME -> textoNormalizado.contains("fulltime")
                        || (textoNormalizado.contains("full") && textoNormalizado.contains("time"))
                        || textoNormalizado.contains("tempo inteiro");
                case PARTTIME -> textoNormalizado.contains("parttime")
                        || (textoNormalizado.contains("part") && textoNormalizado.contains("time"))
                        || textoNormalizado.contains("tempo parcial");
                case REFORCO_FIM_DE_SEMANA -> textoNormalizado.contains("reforco")
                        || textoNormalizado.contains("fim de semana")
                        || textoNormalizado.contains("weekend");
            };
        }

        private String descricaoCurta() {
            return switch (this) {
                case GESTAO -> "gestao";
                case FULLTIME -> "full-time";
                case PARTTIME -> "part-time";
                case REFORCO_FIM_DE_SEMANA -> "reforco de fim de semana";
            };
        }

        private static String normalizarTextoEstatico(String valor) {
            if (valor == null) {
                return "";
            }
            return Normalizer.normalize(valor, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "")
                    .toLowerCase(Locale.ROOT)
                    .trim();
        }
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

    private record CargaColaborador(
            Integer idColaborador,
            String nome,
            long minutos,
            long cargaMaximaMinutos,
            int finsDeSemanaTrabalhados
    ) {
    }

    private record PlaneamentoGerado(
            List<Horario> horarios,
            List<EstadoColaborador> estados,
            Collection<LocalDate> diasCobertos
    ) {
    }

    private record ResultadoTentativaDistribuicao(
            boolean encontrou,
            List<AtribuicaoPlaneadaDia> atribuicoes,
            Map<SlotTurnoDia, List<AtribuicaoPlaneadaDia>> candidatosPorSlot,
            List<SlotTurnoDia> slotsOrdenados,
            PesquisaDistribuicaoContexto contextoPesquisa
    ) {
    }

    private static final class PesquisaDistribuicaoContexto {
        private final Instant prazoLimiteGeracao;
        private final int limiteNosPesquisa;
        private int nosExplorados;
        private boolean prazoEsgotado;
        private boolean limitePesquisaAtingido;

        private PesquisaDistribuicaoContexto(Instant prazoLimiteGeracao, int limiteNosPesquisa) {
            this.prazoLimiteGeracao = prazoLimiteGeracao;
            this.limiteNosPesquisa = limiteNosPesquisa;
        }

        private boolean permiteContinuar() {
            if (prazoLimiteGeracao != null && Instant.now().isAfter(prazoLimiteGeracao)) {
                prazoEsgotado = true;
                return false;
            }

            nosExplorados++;
            if (limiteNosPesquisa > 0 && nosExplorados > limiteNosPesquisa) {
                limitePesquisaAtingido = true;
                return false;
            }
            return true;
        }

        private boolean prazoEsgotado() {
            return prazoEsgotado;
        }

        private boolean atingiuLimitePesquisa() {
            return limitePesquisaAtingido;
        }

        private int nosExplorados() {
            return nosExplorados;
        }

        private int limiteNosPesquisa() {
            return limiteNosPesquisa;
        }
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

    private enum PoliticaOtimizacao {
        EQUILIBRIO("Equilibrio", "minimizar desvios de carga e evitar concentracao semanal", 4, 1, 2, 2, 2, 0, 0),
        PREFERENCIAS("Preferencias", "maximizar preferencias aprovadas sem violar restricoes legais", 2, 5, 2, 1, 2, 7, 1),
        FINS_DE_SEMANA("Fins de semana", "reforcar rotacao e uso adequado da equipa de fim de semana", 2, 1, 5, 2, 2, 13, 1),
        CARGA_CONTRATUAL("Carga contratual", "aproximar utilizacao de cada contrato ao perfil esperado", 5, 1, 2, 3, 1, 19, 1),
        DIVERSIFICADA("Diversificada", "explorar uma alternativa viavel com desempates diferentes", 3, 2, 3, 2, 4, 29, 2);

        private final String nome;
        private final String descricao;
        private final int pesoEquilibrioCarga;
        private final int pesoPreferencias;
        private final int pesoFinsDeSemana;
        private final int pesoReservaOperacional;
        private final int pesoTurnoRepetido;
        private final int sementeDiversificacao;
        private final int pesoDiversificacao;

        PoliticaOtimizacao(String nome,
                           String descricao,
                           int pesoEquilibrioCarga,
                           int pesoPreferencias,
                           int pesoFinsDeSemana,
                           int pesoReservaOperacional,
                           int pesoTurnoRepetido,
                           int sementeDiversificacao,
                           int pesoDiversificacao) {
            this.nome = nome;
            this.descricao = descricao;
            this.pesoEquilibrioCarga = pesoEquilibrioCarga;
            this.pesoPreferencias = pesoPreferencias;
            this.pesoFinsDeSemana = pesoFinsDeSemana;
            this.pesoReservaOperacional = pesoReservaOperacional;
            this.pesoTurnoRepetido = pesoTurnoRepetido;
            this.sementeDiversificacao = sementeDiversificacao;
            this.pesoDiversificacao = pesoDiversificacao;
        }

        private static PoliticaOtimizacao porIndice(int indice) {
            PoliticaOtimizacao[] valores = values();
            return valores[Math.floorMod(indice, valores.length)];
        }

        private String nome() {
            return nome;
        }

        private String descricao() {
            return descricao;
        }

        private int pesoEquilibrioCarga() {
            return pesoEquilibrioCarga;
        }

        private int pesoPreferencias() {
            return pesoPreferencias;
        }

        private int pesoFinsDeSemana() {
            return pesoFinsDeSemana;
        }

        private int pesoReservaOperacional() {
            return pesoReservaOperacional;
        }

        private int pesoTurnoRepetido() {
            return pesoTurnoRepetido;
        }

        private int sementeDiversificacao() {
            return sementeDiversificacao;
        }

        private int pesoDiversificacao() {
            return pesoDiversificacao;
        }
    }

    private final class EstadoColaborador {
        private final Lojautilizador ligacao;
        private final PerfilContratual perfilContratual;
        private final long cargaMaximaMensalMinutos;
        private LocalDate ultimaDataAtribuida;
        private int diasConsecutivos;
        private int turnosAtribuidos;
        private long minutosAtribuidos;
        private final Map<LocalDate, Turno> atribuicoesConhecidas = new HashMap<>();
        private final Map<LocalDate, Integer> diasTrabalhadosPorSemana = new HashMap<>();
        private final Set<LocalDate> finsDeSemanaTrabalhados = new LinkedHashSet<>();
        private final Map<String, Integer> turnosPorTipo = new HashMap<>();

        private EstadoColaborador(Lojautilizador ligacao,
                                  PerfilContratual perfilContratual,
                                  long cargaMaximaMensalMinutos) {
            this.ligacao = ligacao;
            this.perfilContratual = perfilContratual;
            this.cargaMaximaMensalMinutos = cargaMaximaMensalMinutos;
        }

        private boolean podeReceber(LocalDate data,
                                    Turno turno,
                                    long minutosTurno,
                                    Set<LocalDate> bloqueios,
                                    int maxDiasConsecutivos,
                                    int descansoMinimoHoras,
                                    int descansoSemanalMinimoDias,
                                    int janelaRotacaoFinsDeSemanaSemanas,
                                    ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            return podeReceber(
                    data,
                    turno,
                    minutosTurno,
                    bloqueios,
                    maxDiasConsecutivos,
                    descansoMinimoHoras,
                    descansoSemanalMinimoDias,
                    janelaRotacaoFinsDeSemanaSemanas,
                    reservaOperacionalFimDeSemana,
                    false,
                    false
            );
        }

        private boolean podeReceber(LocalDate data,
                                    Turno turno,
                                    long minutosTurno,
                                    Set<LocalDate> bloqueios,
                                    int maxDiasConsecutivos,
                                    int descansoMinimoHoras,
                                    int descansoSemanalMinimoDias,
                                    int janelaRotacaoFinsDeSemanaSemanas,
                                    ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                    boolean ignorarRotacaoFimDeSemana) {
            return podeReceber(
                    data,
                    turno,
                    minutosTurno,
                    bloqueios,
                    maxDiasConsecutivos,
                    descansoMinimoHoras,
                    descansoSemanalMinimoDias,
                    janelaRotacaoFinsDeSemanaSemanas,
                    reservaOperacionalFimDeSemana,
                    ignorarRotacaoFimDeSemana,
                    false
            );
        }

        private boolean podeReceber(LocalDate data,
                                    Turno turno,
                                    long minutosTurno,
                                    Set<LocalDate> bloqueios,
                                    int maxDiasConsecutivos,
                                    int descansoMinimoHoras,
                                    int descansoSemanalMinimoDias,
                                    int janelaRotacaoFinsDeSemanaSemanas,
                                    ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana,
                                    boolean ignorarRotacaoFimDeSemana,
                                    boolean ignorarDescansoSemanal) {
            if (bloqueios.contains(data)
                    || atribuicoesConhecidas.containsKey(data)
                    || !temVinculoValidoNaData(data)
                    || !perfilContratual.permiteData(data)
                    || !perfilContratual.permiteTurno(minutosTurno)
                    || (minutosAtribuidos + minutosTurno) > cargaMaximaMensalMinutos
                    || (!ignorarDescansoSemanal && excedeMaximoDiasTrabalhadosNaSemana(data, descansoSemanalMinimoDias))
                    || (!ignorarRotacaoFimDeSemana && violariaRotacaoDeFimDeSemana(data, janelaRotacaoFinsDeSemanaSemanas))) {
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
                        atribuicoesConhecidas.get(ultimaDataAtribuida),
                        data,
                        turno
                );
                if (horasDescanso < descansoMinimoHoras) {
                    return false;
                }
            }

            return true;
        }

        private boolean podeReceberComoChefiaAoSabado(LocalDate data,
                                                      Turno turno,
                                                      long minutosTurno,
                                                      Set<LocalDate> bloqueios,
                                                      int maxDiasConsecutivos,
                                                      int descansoMinimoHoras,
                                                      int descansoSemanalMinimoDias,
                                                      ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            return podeReceber(
                    data,
                    turno,
                    minutosTurno,
                    bloqueios,
                    maxDiasConsecutivos,
                    descansoMinimoHoras,
                    descansoSemanalMinimoDias,
                    0,
                    reservaOperacionalFimDeSemana,
                    true
            );
        }

        private boolean podeReceberIgnorandoRotacaoFimDeSemana(LocalDate data,
                                                               Turno turno,
                                                               long minutosTurno,
                                                               Set<LocalDate> bloqueios,
                                                               int maxDiasConsecutivos,
                                                               int descansoMinimoHoras,
                                                               int descansoSemanalMinimoDias,
                                                               ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            return podeReceber(
                    data,
                    turno,
                    minutosTurno,
                    bloqueios,
                    maxDiasConsecutivos,
                    descansoMinimoHoras,
                    descansoSemanalMinimoDias,
                    0,
                    reservaOperacionalFimDeSemana,
                    true
            );
        }

        private List<String> motivosIndisponibilidade(LocalDate data,
                                                      Turno turno,
                                                      long minutosTurno,
                                                      Set<LocalDate> bloqueios,
                                                      int maxDiasConsecutivos,
                                                      int descansoMinimoHoras,
                                                      int descansoSemanalMinimoDias,
                                                      int janelaRotacaoFinsDeSemanaSemanas,
                                                      ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            List<String> motivos = new ArrayList<>();
            if (bloqueios.contains(data)) {
                motivos.add("bloqueio");
            }
            if (atribuicoesConhecidas.containsKey(data)) {
                motivos.add("ja-atribuido");
            }
            if (!temVinculoValidoNaData(data)) {
                motivos.add("vinculo");
            }
            if (!perfilContratual.permiteData(data)) {
                motivos.add("perfil");
            }
            if (!perfilContratual.permiteTurno(minutosTurno)) {
                motivos.add("turno-curto");
            }
            if ((minutosAtribuidos + minutosTurno) > cargaMaximaMensalMinutos) {
                motivos.add("carga");
            }
            if (excedeMaximoDiasTrabalhadosNaSemana(data, descansoSemanalMinimoDias)) {
                motivos.add("descanso-semanal");
            }
            if (violariaRotacaoDeFimDeSemana(data, janelaRotacaoFinsDeSemanaSemanas)) {
                motivos.add("rotacao");
            }

            int novoConsecutivo = 1;
            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                novoConsecutivo = diasConsecutivos + 1;
            }
            if (novoConsecutivo > maxDiasConsecutivos) {
                motivos.add("consecutivo");
            }

            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                long horasDescanso = calcularHorasDescanso(
                        ultimaDataAtribuida,
                        atribuicoesConhecidas.get(ultimaDataAtribuida),
                        data,
                        turno
                );
                if (horasDescanso < descansoMinimoHoras) {
                    motivos.add("descanso-turno");
                }
            }
            return motivos;
        }

        private void registarAtribuicao(LocalDate data, Turno turno, long minutosTurno) {
            registarAtribuicaoConhecida(data, turno, minutosTurno, true);
        }

        private void inicializarComHistorico(List<Horario> historicoHorarios) {
            for (Horario horario : historicoHorarios) {
                if (horario.getDataTurno() == null || horario.getIdTurno() == null) {
                    continue;
                }
                registarAtribuicaoConhecida(horario.getDataTurno(), horario.getIdTurno(), 0, false);
            }
        }

        private void registarAtribuicaoConhecida(LocalDate data,
                                                 Turno turno,
                                                 long minutosTurno,
                                                 boolean contarParaDistribuicaoAtual) {
            if (data == null || turno == null || atribuicoesConhecidas.containsKey(data)) {
                return;
            }

            if (ultimaDataAtribuida != null && ultimaDataAtribuida.plusDays(1).equals(data)) {
                diasConsecutivos++;
            } else {
                diasConsecutivos = 1;
            }
            ultimaDataAtribuida = data;
            atribuicoesConhecidas.put(data, turno);
            diasTrabalhadosPorSemana.merge(inicioSemana(data), 1, Integer::sum);
            if (ehFimDeSemana(data)) {
                finsDeSemanaTrabalhados.add(inicioFimDeSemana(data));
            }

            if (contarParaDistribuicaoAtual) {
                turnosAtribuidos++;
                minutosAtribuidos += minutosTurno;
                turnosPorTipo.merge(normalizarTurno(turno), 1, Integer::sum);
            }
        }

        private EstadoColaborador copiarComAtribuicao(LocalDate data, Turno turno, long minutosTurno) {
            EstadoColaborador copia = new EstadoColaborador(ligacao, perfilContratual, cargaMaximaMensalMinutos);
            copia.ultimaDataAtribuida = ultimaDataAtribuida;
            copia.diasConsecutivos = diasConsecutivos;
            copia.turnosAtribuidos = turnosAtribuidos;
            copia.minutosAtribuidos = minutosAtribuidos;
            copia.atribuicoesConhecidas.putAll(atribuicoesConhecidas);
            copia.diasTrabalhadosPorSemana.putAll(diasTrabalhadosPorSemana);
            copia.finsDeSemanaTrabalhados.addAll(finsDeSemanaTrabalhados);
            copia.turnosPorTipo.putAll(turnosPorTipo);
            copia.registarAtribuicao(data, turno, minutosTurno);
            return copia;
        }

        private Integer idUtilizador() {
            return ligacao.getIdUtilizador().getId();
        }

        private boolean cumpreChefiaObrigatoriaAoSabado() {
            String tipoCargo = ligacao.getIdCargo() != null ? ligacao.getIdCargo().getTipo() : null;
            return tipoCargo != null && CARGOS_COM_PRESENCA_OBRIGATORIA_AO_SABADO.contains(normalizarTexto(tipoCargo));
        }

        private boolean estaIsentoDaRotacaoFimDeSemana() {
            return cumpreChefiaObrigatoriaAoSabado();
        }

        private boolean ehReforcoFimDeSemana() {
            return perfilContratual == PerfilContratual.REFORCO_FIM_DE_SEMANA;
        }

        private boolean ehPartTime() {
            return perfilContratual == PerfilContratual.PARTTIME;
        }

        private boolean ehGestao() {
            return perfilContratual == PerfilContratual.GESTAO;
        }

        private boolean ehPerfilPreferencialParaFimDeSemana() {
            return ehReforcoFimDeSemana() || ehPartTime() || ehGestao();
        }

        private boolean permiteData(LocalDate data) {
            return perfilContratual.permiteData(data);
        }

        private boolean podeIntegrarReservaNoDia(LocalDate data,
                                                 Set<LocalDate> bloqueios,
                                                 int janelaRotacaoFinsDeSemanaSemanas) {
            if (data == null) {
                return false;
            }
            if (bloqueios.contains(data)
                    || !temVinculoValidoNaData(data)
                    || !permiteData(data)
                    || violariaRotacaoDeFimDeSemana(data, janelaRotacaoFinsDeSemanaSemanas)) {
                return false;
            }
            return !atribuicoesConhecidas.containsKey(data);
        }

        private boolean consegueCobrirFimDeSemanaCompleto(LocalDate sabado,
                                                          LocalDate domingo,
                                                          Set<LocalDate> bloqueios,
                                                          int janelaRotacaoFinsDeSemanaSemanas) {
            if (sabado == null) {
                return false;
            }

            boolean podeSabado = podeIntegrarReservaNoDia(sabado, bloqueios, janelaRotacaoFinsDeSemanaSemanas);
            boolean podeDomingo = domingo == null || podeIntegrarReservaNoDia(domingo, bloqueios, janelaRotacaoFinsDeSemanaSemanas);
            return podeSabado && podeDomingo;
        }

        private boolean consegueCobrirFimDeSemanaCompleto(LocalDate sabado,
                                                          List<Turno> turnosSabado,
                                                          LocalDate domingo,
                                                          List<Turno> turnosDomingo,
                                                          Set<LocalDate> bloqueios,
                                                          int maxDiasConsecutivos,
                                                          int descansoMinimoHoras,
                                                          int descansoSemanalMinimoDias,
                                                          int janelaRotacaoFinsDeSemanaSemanas,
                                                          ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            if (sabado == null || turnosSabado == null || turnosSabado.isEmpty()) {
                return false;
            }

            for (Turno turnoSabado : turnosSabado) {
                long minutosSabado = calcularDuracaoEmMinutos(turnoSabado);
                if (!podeReceber(
                        sabado,
                        turnoSabado,
                        minutosSabado,
                        bloqueios,
                        maxDiasConsecutivos,
                        descansoMinimoHoras,
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas,
                        reservaOperacionalFimDeSemana
                )) {
                    continue;
                }

                if (domingo == null || turnosDomingo == null || turnosDomingo.isEmpty()) {
                    return true;
                }

                EstadoColaborador copia = copiarComAtribuicao(sabado, turnoSabado, minutosSabado);
                for (Turno turnoDomingo : turnosDomingo) {
                    long minutosDomingo = calcularDuracaoEmMinutos(turnoDomingo);
                    if (copia.podeReceber(
                            domingo,
                            turnoDomingo,
                            minutosDomingo,
                            bloqueios,
                            maxDiasConsecutivos,
                            descansoMinimoHoras,
                            descansoSemanalMinimoDias,
                            janelaRotacaoFinsDeSemanaSemanas,
                            reservaOperacionalFimDeSemana
                    )) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean consegueCobrirAlgumTurnoNoDia(LocalDate data,
                                                      List<Turno> turnos,
                                                      Set<LocalDate> bloqueios,
                                                      int maxDiasConsecutivos,
                                                      int descansoMinimoHoras,
                                                      int descansoSemanalMinimoDias,
                                                      int janelaRotacaoFinsDeSemanaSemanas,
                                                      ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            if (data == null || turnos == null || turnos.isEmpty()) {
                return false;
            }

            for (Turno turno : turnos) {
                long minutosTurno = calcularDuracaoEmMinutos(turno);
                if (podeReceber(
                        data,
                        turno,
                        minutosTurno,
                        bloqueios,
                        maxDiasConsecutivos,
                        descansoMinimoHoras,
                        descansoSemanalMinimoDias,
                        janelaRotacaoFinsDeSemanaSemanas,
                        reservaOperacionalFimDeSemana
                )) {
                    return true;
                }
            }
            return false;
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

        private long cargaMaximaMensalMinutos() {
            return cargaMaximaMensalMinutos;
        }

        private double utilizacaoContratualProjetada(long minutosTurno) {
            if (cargaMaximaMensalMinutos <= 0) {
                return Double.MAX_VALUE;
            }
            return (minutosAtribuidos + minutosTurno) / (double) cargaMaximaMensalMinutos;
        }

        private double utilizacaoContratualAtual() {
            if (cargaMaximaMensalMinutos <= 0) {
                return Double.MAX_VALUE;
            }
            return minutosAtribuidos / (double) cargaMaximaMensalMinutos;
        }

        private boolean excedeMaximoDiasTrabalhadosNaSemana(LocalDate data, int descansoSemanalMinimoDias) {
            int maxDiasTrabalhados = Math.max(0, 7 - descansoSemanalMinimoDias);
            LocalDate inicioSemana = inicioSemana(data);
            return diasTrabalhadosPorSemana.getOrDefault(inicioSemana, 0) + 1 > maxDiasTrabalhados;
        }

        private boolean excedeLimiteSemanalDaReservaOperacional(LocalDate data,
                                                                int descansoSemanalMinimoDias,
                                                                ReservaOperacionalFimDeSemana reservaOperacionalFimDeSemana) {
            if (data == null
                    || ehFimDeSemana(data)
                    || reservaOperacionalFimDeSemana == null
                    || !reservaOperacionalFimDeSemana.ehNuclear(idUtilizador())) {
                return false;
            }

            LocalDate proximoSabado = data.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            if (!inicioSemana(proximoSabado).equals(inicioSemana(data))) {
                return false;
            }

            int maxDiasTrabalhados = Math.max(0, 7 - descansoSemanalMinimoDias);
            int limiteAntesDoFimDeSemana = Math.max(0, maxDiasTrabalhados - 2);
            return diasTrabalhadosNaSemana(data) + 1 > limiteAntesDoFimDeSemana;
        }

        private boolean violariaRotacaoDeFimDeSemana(LocalDate data, int janelaRotacaoFinsDeSemanaSemanas) {
            if (estaIsentoDaRotacaoFimDeSemana() || !ehFimDeSemana(data) || janelaRotacaoFinsDeSemanaSemanas < 2) {
                return false;
            }

            LocalDate fimDeSemanaAtual = inicioFimDeSemana(data);
            if (finsDeSemanaTrabalhados.contains(fimDeSemanaAtual)) {
                return false;
            }

            for (int deslocamento = janelaRotacaoFinsDeSemanaSemanas - 1; deslocamento >= 0; deslocamento--) {
                LocalDate inicioJanela = fimDeSemanaAtual.minusWeeks(deslocamento);
                int finsDeSemanaComTrabalho = 0;
                for (int indice = 0; indice < janelaRotacaoFinsDeSemanaSemanas; indice++) {
                    LocalDate fimDeSemanaNaJanela = inicioJanela.plusWeeks(indice);
                    if (fimDeSemanaNaJanela.equals(fimDeSemanaAtual) || finsDeSemanaTrabalhados.contains(fimDeSemanaNaJanela)) {
                        finsDeSemanaComTrabalho++;
                    }
                }
                if (finsDeSemanaComTrabalho >= janelaRotacaoFinsDeSemanaSemanas) {
                    return true;
                }
            }
            return false;
        }

        private boolean temVinculoValidoNaData(LocalDate data) {
            if (data == null || ligacao.getDataInicio() == null) {
                return false;
            }
            if (data.isBefore(ligacao.getDataInicio())) {
                return false;
            }
            return ligacao.getDataFim() == null || !data.isAfter(ligacao.getDataFim());
        }

        private int turnosDoTipo(String tipo) {
            return turnosPorTipo.getOrDefault(tipo, 0);
        }

        private boolean trabalhouNoMesmoFimDeSemana(LocalDate data) {
            return data != null && ehFimDeSemana(data) && finsDeSemanaTrabalhados.contains(inicioFimDeSemana(data));
        }

        private boolean estariaBloqueadoNoFimDeSemana(LocalDate dataFimDeSemana, int janelaRotacaoFinsDeSemanaSemanas) {
            if (dataFimDeSemana == null || !ehFimDeSemana(dataFimDeSemana)) {
                return false;
            }
            return !temVinculoValidoNaData(dataFimDeSemana)
                    || !perfilContratual.permiteData(dataFimDeSemana)
                    || violariaRotacaoDeFimDeSemana(dataFimDeSemana, janelaRotacaoFinsDeSemanaSemanas);
        }

        private int diasTrabalhadosNaSemana(LocalDate data) {
            if (data == null) {
                return 0;
            }
            return diasTrabalhadosPorSemana.getOrDefault(inicioSemana(data), 0);
        }

        private int totalFinsDeSemanaTrabalhados() {
            return finsDeSemanaTrabalhados.size();
        }
    }
}
