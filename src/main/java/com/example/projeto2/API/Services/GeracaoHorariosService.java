package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoHorario;
import com.example.projeto2.API.Enums.EstadoUtilizador;
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
import com.example.projeto2.API.Services.geracao.HorarioFormatters;
import com.example.projeto2.API.Services.geracao.LojaConfiguracaoBuilder;
import com.example.projeto2.API.Services.geracao.CargaColaborador;
import com.example.projeto2.API.Services.geracao.EstadoColaboradorResumo;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamento;
import com.example.projeto2.API.Services.geracao.MetricasPlaneamentoCalculator;
import com.example.projeto2.API.Services.geracao.ParametrosGeracao;
import com.example.projeto2.API.Services.geracao.PerfilContratual;
import com.example.projeto2.API.Services.geracao.PlaneamentoGerado;
import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;
import com.example.projeto2.API.Services.geracao.PreferenciasGeracaoBuilder;
import com.example.projeto2.API.Services.geracao.RegraAplicada;
import com.example.projeto2.API.Services.geracao.RegraGeracaoResolver;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDuracao;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarDiferencaDuracao;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.formatarPeriodoVinculo;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.limparTexto;
import static com.example.projeto2.API.Services.geracao.HorarioFormatters.valorOuTraco;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class GeracaoHorariosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeracaoHorariosService.class);
    // Cargos definidos em LojautilizadorHelper.GESTAO e LojautilizadorHelper.VALIDACAO
    private static final Set<String> CARGOS_COM_PRESENCA_OBRIGATORIA_AO_SABADO = Set.of("gerente", "subgerente");
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
    private final PropostaPersistenciaHelper persistenciaHelper;
    private final TurnoRepository turnoRepository;
    private final HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    // Serviços extraídos na refatorização SRP (Fase 2)
    private final HorarioValidatorService validator;
    private final HorarioGeneratorEngine engine;

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
                              PropostaPersistenciaHelper persistenciaHelper,
                              TurnoRepository turnoRepository,
                              HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository,
                              HorarioValidatorService validator,
                              HorarioGeneratorEngine engine) {
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
        this.persistenciaHelper = persistenciaHelper;
        this.turnoRepository = turnoRepository;
        this.historicoHorarioEstadoRepository = historicoHorarioEstadoRepository;
        this.validator = validator;
        this.engine = engine;
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
            PoliticaOtimizacao politica = PoliticaOtimizacao.porIndice(alternativasExistentes + indice);
            // Cada alternativa tem uma semente única: base × primo × índice
            long sementeAlternativa = sementeBase ^ ((long)(alternativasExistentes + indice + 1) * 0x9e3779b97f4a7c15L);
            Instant inicioGeracao = Instant.now();
            PlaneamentoGerado planeamento = gerarPlaneamento(
                    dados.colaboradoresAtivos(),
                    dados.turnos(),
                    dados.parametros(),
                    dados.historicoHorarios(),
                    dados.bloqueiosPorUtilizador(),
                    dados.diasFolgaPreferidos(),
                    dados.preferenciasTurnos(),
                    dados.preferenciasColegas(),
                    dados.configuracoesPorData(),
                    dados.dataInicio(),
                    dados.dataFim(),
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

        return obterPropostasVisiveis(ligacaoAtiva, anoNormalizado, mesNormalizado)
                .stream()
                .map(proposta -> resultadoBuilder.construirResumoProposta(
                        proposta,
                        resultadoBuilder.construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()))
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

        return resultadoBuilder.construirResultado(proposta, horarioRepository.findByIdPropostaHorarioId(proposta.getId()));
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
                + resultadoBuilder.rotuloCurtoProposta(base)
                + " e "
                + resultadoBuilder.rotuloCurtoProposta(comparada)
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
                resultadoBuilder.rotuloCurtoProposta(base),
                resultadoBuilder.rotuloCurtoProposta(comparada),
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
    // Orquestrador — delega ao HorarioGeneratorEngine (SRP Fase 2)
    // =========================================================================

    /**
     * Monta o {@link PedidoGeracao} a partir dos dados já carregados e delega
     * a geração ao {@link HorarioGeneratorEngine}.
     * Esta classe deixa de conter qualquer lógica de algoritmo.
     */
    private PlaneamentoGerado gerarPlaneamento(List<Lojautilizador> colaboradoresAtivos,
                                               List<Turno> turnos,
                                               ParametrosGeracao parametros,
                                               List<Horario> historicoHorarios,
                                               Map<Integer, Set<LocalDate>> bloqueiosPorUtilizador,
                                               Map<Integer, Set<LocalDate>> diasFolgaPreferidos,
                                               Map<Integer, List<Preferencia>> preferenciasTurnos,
                                               Map<Integer, List<Preferencia>> preferenciasColegas,
                                               Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData,
                                               LocalDate dataInicio,
                                               LocalDate dataFim,
                                               PoliticaOtimizacao politica,
                                               Instant prazoLimiteGeracao,
                                               long sementeDiversificacao) {

        // --- a) Construir carga máxima por colaborador ---
        Map<Integer, Long> cargaMaximaPorColaborador = new LinkedHashMap<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            PerfilContratual perfilContratual = resolverPerfilContratual(ligacao)
                    .orElseThrow(() -> new IllegalArgumentException("Foi encontrado um colaborador sem perfil contratual valido para a geracao."));
            Long cargaMaximaMinutos = parametros.cargaMaximaMinutosPorPerfil().get(perfilContratual);
            if (cargaMaximaMinutos == null || cargaMaximaMinutos <= 0) {
                throw new IllegalArgumentException("Nao existe uma carga contratual mensal valida para o perfil " + perfilContratual.descricaoCurta() + ".");
            }
            cargaMaximaPorColaborador.put(ligacao.getIdUtilizador().getId(), cargaMaximaMinutos);
        }

        // --- b) Determinar IDs de chefias ao sábado ---
        Set<Integer> chefiasSabadoIds = new LinkedHashSet<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            if (ligacao.getIdCargo() != null
                    && validator.exigePresencaAoSabado(ligacao.getIdCargo().getNome())
                    && ligacao.getIdUtilizador() != null) {
                chefiasSabadoIds.add(ligacao.getIdUtilizador().getId());
            }
        }

        // --- c) Converter ConfiguracaoDiaEspecial → HorarioGeneratorEngine.ConfiguracaoDia ---
        Map<LocalDate, HorarioGeneratorEngine.ConfiguracaoDia> configsEngine = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, ConfiguracaoDiaEspecial> entry : configuracoesPorData.entrySet()) {
            ConfiguracaoDiaEspecial src = entry.getValue();
            configsEngine.put(entry.getKey(), new HorarioGeneratorEngine.ConfiguracaoDia(
                    src.lojaEncerrada(),
                    src.turnosCompativeis(),
                    src.minimoColaboradoresTurno(),
                    src.descricao()
            ));
        }
        // --- d) Construir pares de colegas preferidos (A3) ---
        Map<Integer, Set<Integer>> paresPreferisPorColaborador =
                PreferenciasGeracaoBuilder.construirParesPreferidos(preferenciasColegas, colaboradoresAtivos);

        // --- e) Montar o PedidoGeracao ---
        HorarioGeneratorEngine.PedidoGeracao pedido = new HorarioGeneratorEngine.PedidoGeracao(
                colaboradoresAtivos,
                turnos,
                dataInicio,
                dataFim,
                parametros.minimosPorTurno(),
                parametros.maxDiasConsecutivos(),
                parametros.descansoMinimoHoras(),
                parametros.descansoSemanalMinimoDias(),
                parametros.janelaRotacaoFinsDeSemanaSemanas(),
                parametros.exigirChefiaAoSabado(),
                chefiasSabadoIds,
                cargaMaximaPorColaborador,
                bloqueiosPorUtilizador,
                preferenciasTurnos,
                configsEngine,
                historicoHorarios,
                prazoLimiteGeracao,
                politica.nome(),
                diasFolgaPreferidos,
                paresPreferisPorColaborador,
                sementeDiversificacao
        );

        // --- e) Delegar ao motor ---
        List<Horario> horarios = engine.gerar(pedido);

        // --- f) Calcular estados para métricas (resumo) ---
        Set<LocalDate> diasCobertos = new LinkedHashSet<>();
        for (Horario h : horarios) {
            if (h.getDataTurno() != null) diasCobertos.add(h.getDataTurno());
        }

        // Reconstruir estados-resumo para métricas (sem algoritmo — só carga)
        Map<Integer, EstadoColaboradorResumo> resumos = new LinkedHashMap<>();
        for (Lojautilizador ligacao : colaboradoresAtivos) {
            if (ligacao.getIdUtilizador() == null) continue;
            Long carga = cargaMaximaPorColaborador.getOrDefault(ligacao.getIdUtilizador().getId(), 0L);
            resumos.put(ligacao.getIdUtilizador().getId(), new EstadoColaboradorResumo(ligacao, carga));
        }
        for (Horario h : horarios) {
            if (h.getIdLojautilizador() == null || h.getIdLojautilizador().getIdUtilizador() == null) continue;
            Integer idU = h.getIdLojautilizador().getIdUtilizador().getId();
            EstadoColaboradorResumo resumo = resumos.get(idU);
            if (resumo != null) resumo.registarTurno(calcularDuracaoEmMinutos(h.getIdTurno()));
        }

        return new PlaneamentoGerado(horarios, new ArrayList<>(resumos.values()), diasCobertos);
    }

    // construirBloqueiosPorUtilizador, construirDiasFolgaPreferidos, construirParesPreferidos
    // e agruparPreferenciasPorTipo movidos para PreferenciasGeracaoBuilder (Fase 3.2).
    // construirConfiguracoesEspeciaisPorData, criarConfiguracaoDiaEspecial,
    // filtrarTurnosCompativeis e turnoCabeNoHorario movidos para LojaConfiguracaoBuilder (Fase 3.3).
    // resolverParametrosGeracao, obterRegrasAplicadas e ehRegra* movidos para RegraGeracaoResolver (Fase 2).

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
                && EstadoUtilizador.ativo == ligacao.getIdUtilizador().getEstado();
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
            Integer idHorario,
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

    public record ConfiguracaoDiaEspecial(
            boolean lojaEncerrada,
            List<Turno> turnosCompativeis,
            Integer minimoColaboradoresTurno,
            String descricao
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
            Map<Integer, Set<LocalDate>> diasFolgaPreferidos,
            Map<Integer, List<Preferencia>> preferenciasTurnos,
            Map<Integer, List<Preferencia>> preferenciasColegas,
            Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData
    ) {
    }


}
