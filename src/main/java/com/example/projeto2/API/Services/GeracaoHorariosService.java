package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Modules.DayOff;
import com.example.projeto2.API.Modules.HistoricoHorarioEstado;
import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.HorarioEspecialLoja;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.PropostaHorarioMensal;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.DayOffRepository;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioEspecialLojaRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.PreferenciaRepository;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.API.Repositories.TurnoRepository;
import com.example.projeto2.API.Services.geracao.DadosGeracao;
import com.example.projeto2.API.Services.geracao.EstadoColaboradorResumo;
import com.example.projeto2.API.Services.geracao.HorarioFormatters;
import com.example.projeto2.API.Services.geracao.LojaConfiguracaoBuilder;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamento;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamentoCalculator;
import com.example.projeto2.API.Services.geracao.ParametrosGeracao;
import com.example.projeto2.API.Services.geracao.PedidoGeracaoMontador;
import com.example.projeto2.API.Services.geracao.PerfilContratual;
import com.example.projeto2.API.Services.geracao.PlaneamentoGerado;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Services.geracao.PreferenciasGeracaoBuilder;
import com.example.projeto2.API.Services.geracao.RegraAplicada;
import com.example.projeto2.API.Services.geracao.RegraGeracaoResolver;
import com.example.projeto2.API.Services.geracao.dto.ColaboradorElegivel;
import com.example.projeto2.API.Services.geracao.dto.ComparacaoPropostas;
import com.example.projeto2.API.Services.geracao.dto.ConfiguracaoDiaEspecial;
import com.example.projeto2.API.Services.geracao.dto.GeracaoContexto;
import com.example.projeto2.API.Services.geracao.dto.PropostaResultado;
import com.example.projeto2.API.Services.geracao.dto.PropostaResumo;
import com.example.projeto2.API.Services.geracao.dto.ResumoColaborador;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarPeriodoVinculo;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
@Service
public class GeracaoHorariosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeracaoHorariosService.class);
    private static final String ESTADO_RASCUNHO = "rascunho";
    private static final String ESTADO_PENDENTE = "pendente";
    private static final String ESTADO_APROVADO = "aprovado";
    private static final String ESTADO_REJEITADO = "rejeitado";
    // Formatadores movidos para HorarioFormatters (Fase 3.1)
    private static final DateTimeFormatter DATA_HORA_FORMATTER = HorarioFormatters.DATA_HORA_FORMATTER;
    private static final DateTimeFormatter DATA_FORMATTER = HorarioFormatters.DATA_FORMATTER;
    private static final Duration TEMPO_MAXIMO_GERACAO_ALTERNATIVA = Duration.ofSeconds(20);
    private final LojautilizadorRepository lojautilizadorRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final HorarioRepository horarioRepository;
    private final DayOffRepository dayOffRepository;
    private final PreferenciaRepository preferenciaRepository;
    private final PropostaHorarioMensalRepository propostaHorarioMensalRepository;
    private final HorarioEspecialLojaRepository horarioEspecialLojaRepository;
    private final RegraGeracaoResolver regraGeracaoResolver;
    private final MetricasPlaneamentoCalculator metricasCalculator;
    private final PropostaResultadoBuilder resultadoBuilder;
    private final ComparacaoPropostasService comparacaoService;
    private final PropostaPersistenciaHelper persistenciaHelper;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    // Serviços extraídos na refatorização SRP (Fase 2)
    private final HorarioValidatorService validator;
    private final HorarioGeneratorEngine engine;
    private final PedidoGeracaoMontador pedidoMontador;

    public GeracaoHorariosService(LojautilizadorRepository lojautilizadorRepository,
                              LojautilizadorHelper lojautilizadorHelper,
                              HorarioRepository horarioRepository,
                              DayOffRepository dayOffRepository,
                              PreferenciaRepository preferenciaRepository,
                              PropostaHorarioMensalRepository propostaHorarioMensalRepository,
                              HorarioEspecialLojaRepository horarioEspecialLojaRepository,
                              RegraGeracaoResolver regraGeracaoResolver,
                              MetricasPlaneamentoCalculator metricasCalculator,
                              PropostaResultadoBuilder resultadoBuilder,
                              ComparacaoPropostasService comparacaoService,
                              PropostaPersistenciaHelper persistenciaHelper,
                              TurnoRepository turnoRepository,
                              HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository,
                              HorarioValidatorService validator,
                              HorarioGeneratorEngine engine,
                              PedidoGeracaoMontador pedidoMontador) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.horarioRepository = horarioRepository;
        this.dayOffRepository = dayOffRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.propostaHorarioMensalRepository = propostaHorarioMensalRepository;
        this.horarioEspecialLojaRepository = horarioEspecialLojaRepository;
        this.regraGeracaoResolver = regraGeracaoResolver;
        this.metricasCalculator = metricasCalculator;
        this.resultadoBuilder = resultadoBuilder;
        this.comparacaoService = comparacaoService;
        this.persistenciaHelper = persistenciaHelper;
        this.turnoRepository = turnoRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
        this.validator = validator;
        this.engine = engine;
        this.pedidoMontador = pedidoMontador;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerarHorarios(Integer idUtilizador) {
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.GESTAO);
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeValidarHorarios(Integer idUtilizador) {
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.VALIDACAO);
    }

    @Transactional(readOnly = true)
    public int contarHorariosPendentesValidacao(Integer idUtilizador) {
        return lojautilizadorHelper.findLigacaoAtivaComCargo(idUtilizador, LojautilizadorHelper.VALIDACAO)
                .map(lu -> (int) propostaHorarioMensalRepository.countByIdLojaIdAndEstadoIgnoreCase(
                        lu.getIdLoja().getId(), ESTADO_PENDENTE))
                .orElse(0);
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
                lojautilizadorHelper.temCargo(ligacaoAtiva, LojautilizadorHelper.GESTAO),
                lojautilizadorHelper.temCargo(ligacaoAtiva, LojautilizadorHelper.VALIDACAO),
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
            return resultadoBuilder.construirResultado(proposta.get(), horarioRepository.findByIdPropostaHorarioId(proposta.get().getId()));
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

        return resultadoBuilder.construirResultadoHorariosPublicados(ligacaoAtiva.getIdLoja(), anoNormalizado, mesNormalizado, horariosPublicados);
    }

    @Transactional(readOnly = true)
    public List<Horario> obterMeusHorarios(Integer idUtilizador, Integer ano, Integer mes) {
        int anoNorm = normalizarAno(ano);
        int mesNorm = normalizarMes(mes);
        LocalDate inicio = LocalDate.of(anoNorm, mesNorm, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        return horarioRepository.findHorariosPublicadosPorUtilizadorEntreDatas(idUtilizador, inicio, fim);
    }

    @Transactional
    public PropostaResultado aprovarProposta(Integer idUtilizador, Integer idProposta, String observacoesSupervisor) {
        return persistenciaHelper.decidirProposta(
                obterLigacaoAtivaComPermissaoDeValidacao(idUtilizador), idProposta, "aprovado", observacoesSupervisor);
    }

    @Transactional
    public PropostaResultado rejeitarProposta(Integer idUtilizador, Integer idProposta, String observacoesSupervisor) {
        return persistenciaHelper.decidirProposta(
                obterLigacaoAtivaComPermissaoDeValidacao(idUtilizador), idProposta, "rejeitado", observacoesSupervisor);
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
            resultados.add(resultadoBuilder.construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId())));
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
        return gerarPropostas(idUtilizador, ano, mes, quantidade, idsColaboradoresSelecionados, null);
    }

    @Transactional
    public List<PropostaResultado> gerarPropostas(Integer idUtilizador,
                                                  Integer ano,
                                                  Integer mes,
                                                  Integer quantidade,
                                                  Collection<Integer> idsColaboradoresSelecionados,
                                                  Consumer<String> onProgresso) {
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
        // Semente-base única por invocação: garante que gerações distintas
        // produzem horários diferentes mesmo com a mesma política de otimização.
        long sementeBase = System.nanoTime();
        for (int indice = 0; indice < quantidadeNormalizada; indice++) {
            if (onProgresso != null && quantidadeNormalizada > 1) {
                onProgresso.accept("A gerar alternativa " + (indice + 1) + " de " + quantidadeNormalizada + "...");
            }
            PoliticaOtimizacao politica = PoliticaOtimizacao.porIndice(alternativasExistentes + indice);
            // Cada alternativa tem uma semente única: base × primo × índice
            long sementeAlternativa = sementeBase ^ ((long)(alternativasExistentes + indice + 1) * 0x9e3779b97f4a7c15L);
            Instant inicioGeracao = Instant.now();
            PlaneamentoGerado planeamento = gerarPlaneamento(
                    dados,
                    politica,
                    inicioGeracao.plus(TEMPO_MAXIMO_GERACAO_ALTERNATIVA),
                    sementeAlternativa
            );

            MetricasPlaneamento metricas = metricasCalculator.calcular(planeamento, politica);
            PropostaHorarioMensal proposta = persistenciaHelper.persistirProposta(dados.ligacaoAtiva(), dados.ano(), dados.mes(), planeamento, politica, metricas);
            List<Horario> horariosPersistidos = horarioRepository.findByIdPropostaHorarioId(proposta.getId());
            LOGGER.info(
                    "Alternativa {} gerada para {}/{} em {} ms com a politica {}.",
                    proposta.getId(),
                    String.format(Locale.ROOT, "%02d", dados.mes()),
                    dados.ano(),
                    Duration.between(inicioGeracao, Instant.now()).toMillis(),
                    politica.nome()
            );
            resultados.add(resultadoBuilder.construirResultado(proposta, horariosPersistidos));
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public List<PropostaResumo> listarPropostas(Integer idUtilizador, Integer ano, Integer mes) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComAcessoAoPainel(idUtilizador);
        int anoNormalizado = normalizarAno(ano);
        int mesNormalizado = normalizarMes(mes);

        List<PropostaResumo> resumos = obterPropostasVisiveis(ligacaoAtiva, anoNormalizado, mesNormalizado)
                .stream()
                .map(proposta -> resultadoBuilder.construirResumoProposta(
                        proposta,
                        resultadoBuilder.construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()))
                ))
                .toList();
        return marcarMelhorAlternativa(resumos);
    }

    /**
     * O próprio algoritmo identifica a alternativa mais ótima: entre as propostas
     * ainda em decisão (rascunho/pendente), marca como {@code recomendada} a de menor
     * pontuação (função-objetivo global — menor é melhor). Empate resolvido pela
     * proposta mais recente (primeira da lista, já ordenada por data desc).
     */
    private List<PropostaResumo> marcarMelhorAlternativa(List<PropostaResumo> resumos) {
        Integer idMelhor = resumos.stream()
                .filter(r -> {
                    String estado = normalizarTexto(r.estado());
                    return ESTADO_RASCUNHO.equals(estado) || ESTADO_PENDENTE.equals(estado);
                })
                .min(Comparator.comparingInt(PropostaResumo::pontuacao))
                .map(PropostaResumo::idProposta)
                .orElse(null);

        if (idMelhor == null) {
            return resumos;
        }
        return resumos.stream()
                .map(r -> r.comRecomendacao(idMelhor.equals(r.idProposta())))
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

        return resultadoBuilder.construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()));
    }

    @Transactional(readOnly = true)
    public ComparacaoPropostas compararPropostas(Integer idUtilizador, Integer idPropostaBase, Integer idPropostaComparada) {
        PropostaResultado base = obterPropostaPorId(idUtilizador, idPropostaBase);
        PropostaResultado comparada = obterPropostaPorId(idUtilizador, idPropostaComparada);
        if (!Objects.equals(base.ano(), comparada.ano()) || !Objects.equals(base.mes(), comparada.mes())) {
            throw new IllegalArgumentException("Seleciona duas propostas do mesmo mes para comparar.");
        }
        return comparacaoService.comparar(base, comparada,
                resultadoBuilder.rotuloCurtoProposta(base),
                resultadoBuilder.rotuloCurtoProposta(comparada));
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
        if (lojautilizadorHelper.temCargo(ligacaoAtiva, LojautilizadorHelper.GESTAO)) {
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

        List<RegraAplicada> regras = regraGeracaoResolver.obterRegrasAplicadas(idLoja);
        ParametrosGeracao parametros = regraGeracaoResolver.resolverParametrosGeracao(regras, turnos);
        validarJanelaDeLancamento(dataInicio, parametros.diaLimiteLancamento());
        LocalDate inicioHistorico = resolverInicioHistoricoParaGeracao(dataInicio, parametros);

        List<DayOff> dayOffsAprovados = dayOffRepository.findPedidosAprovadosDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        List<Preferencia> preferenciasAprovadas = preferenciaRepository.findPreferenciasAprovadasDaLojaEntreDatas(idLoja, dataInicio, dataFim);
        List<HorarioEspecialLoja> horariosEspeciais = horarioEspecialLojaRepository.findAtivosNoPeriodo(idLoja, dataInicio, dataFim);
        List<Horario> historicoHorarios = inicioHistorico.isBefore(dataInicio)
                ? horarioRepository.findHorariosDaLojaEntreDatas(idLoja, inicioHistorico, dataInicio.minusDays(1))
                : List.of();

        Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador = PreferenciasGeracaoBuilder.construirBloqueiosPorUtilizador(
                dataInicio,
                dataFim,
                dayOffsAprovados,
                preferenciasAprovadas
        );
        Map<Integer, Set<LocalDate>> diasFolgaPreferidos = PreferenciasGeracaoBuilder.construirDiasFolgaPreferidos(
                dataInicio,
                dataFim,
                preferenciasAprovadas
        );
        Map<Integer, List<Preferencia>> preferenciasTurnos = PreferenciasGeracaoBuilder.agruparPorTipo(preferenciasAprovadas, "turnos");
        Map<Integer, List<Preferencia>> preferenciasColegas = PreferenciasGeracaoBuilder.agruparPorTipo(preferenciasAprovadas, "colegas");
        Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData =
                LojaConfiguracaoBuilder.construirConfiguracoesEspeciaisPorData(loja, turnos, horariosEspeciais);

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
                diasFolgaPreferidos,
                preferenciasTurnos,
                preferenciasColegas,
                configuracoesPorData
        );
    }

    // =========================================================================
    // Orquestrador — delega ao PedidoGeracaoMontador e ao HorarioGeneratorEngine
    // =========================================================================

    private PlaneamentoGerado gerarPlaneamento(DadosGeracao dados,
                                               PoliticaOtimizacao politica,
                                               Instant prazoLimiteGeracao,
                                               long sementeDiversificacao) {
        var pedido = pedidoMontador.montar(dados, politica, prazoLimiteGeracao, sementeDiversificacao);
        List<Horario> horarios = engine.gerar(pedido);

        Set<LocalDate> diasCobertos = new LinkedHashSet<>();
        for (Horario h : horarios) {
            if (h.getDataTurno() != null) diasCobertos.add(h.getDataTurno());
        }

        Map<Integer, EstadoColaboradorResumo> resumos = new LinkedHashMap<>();
        for (Lojautilizador ligacao : dados.colaboradoresAtivos()) {
            if (ligacao.getIdUtilizador() == null) continue;
            Long carga = pedido.cargaMaximaPorColaborador()
                    .getOrDefault(ligacao.getIdUtilizador().getId(), 0L);
            resumos.put(ligacao.getIdUtilizador().getId(), new EstadoColaboradorResumo(ligacao, carga));
        }
        for (Horario h : horarios) {
            if (h.getIdLojautilizador() == null || h.getIdLojautilizador().getIdUtilizador() == null) continue;
            EstadoColaboradorResumo resumo = resumos.get(h.getIdLojautilizador().getIdUtilizador().getId());
            if (resumo != null) resumo.registarTurno(HorarioFormatters.calcularDuracaoEmMinutos(h.getIdTurno()));
        }

        List<String> avisos = PreferenciasGeracaoBuilder.construirAvisosNaoHonrados(
                dados.colaboradoresAtivos(), dados.diasFolgaPreferidos(), horarios);

        return new PlaneamentoGerado(horarios, new ArrayList<>(resumos.values()), diasCobertos, avisos);
    }

    // construirBloqueiosPorUtilizador, construirDiasFolgaPreferidos, construirParesPreferidos,
    // construirAvisosNaoHonrados e agruparPreferenciasPorTipo → PreferenciasGeracaoBuilder.
    // construirConfiguracoesEspeciaisPorData → LojaConfiguracaoBuilder.
    // resolverParametrosGeracao → RegraGeracaoResolver.
    // resolverCargasMaximas, resolverChefiasSabado, converterConfiguracoes → PedidoGeracaoMontador.

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

    private List<Lojautilizador> obterColaboradoresElegiveis(Integer idLoja, LocalDate dataInicio, LocalDate dataFim) {
        Map<Integer, Lojautilizador> ativos = new LinkedHashMap<>();
        for (Lojautilizador ligacao : lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja)) {
            if (!lojautilizadorHelper.ligacaoTemRelevanciaNoPeriodo(ligacao, dataInicio, dataFim)) continue;
            if (!lojautilizadorHelper.utilizadorEstaAtivo(ligacao)) continue;
            if (resolverPerfilContratual(ligacao).isEmpty()) continue;
            ativos.merge(ligacao.getIdUtilizador().getId(), ligacao,
                    lojautilizadorHelper::preferirLigacaoMaisRecente);
        }
        return new ArrayList<>(ativos.values());
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
                            + HorarioFormatters.nomeMes(dataInicioPeriodo.getMonthValue())
                            + " de "
                            + dataInicioPeriodo.getYear()
                            + " tinha de ser lancada ate "
                            + DATA_FORMATTER.format(dataLimite)
                            + "."
            );
        }
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

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        return lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizador, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para gerar propostas de horario.");
    }

    private Lojautilizador obterLigacaoAtivaComAcessoAoPainel(Integer idUtilizador) {
        return lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizador, LojautilizadorHelper.APROVACAO,
                "Nao tens permissao para consultar o painel de horarios da loja.");
    }

    private Lojautilizador obterLigacaoAtivaComPermissaoDeValidacao(Integer idUtilizador) {
        return lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizador, LojautilizadorHelper.VALIDACAO,
                "Nao tens permissao para validar a proposta mensal.");
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

    private LocalDate inicioSemana(LocalDate data) {
        return data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

}
