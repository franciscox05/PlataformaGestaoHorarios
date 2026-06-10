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
import com.example.projeto2.API.Modules.Regra;
import com.example.projeto2.API.Modules.RegrasLoja;
import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Repositories.DayOffRepository;
import com.example.projeto2.API.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.API.Repositories.HorarioEspecialLojaRepository;
import com.example.projeto2.API.Repositories.HorarioRepository;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.PreferenciaRepository;
import com.example.projeto2.API.Repositories.PropostaHorarioMensalRepository;
import com.example.projeto2.API.Repositories.RegraRepository;
import com.example.projeto2.API.Repositories.RegrasLojaRepository;
import com.example.projeto2.API.Repositories.TurnoRepository;
import com.example.projeto2.API.Services.geracao.HorarioFormatters;
import com.example.projeto2.API.Services.geracao.LojaConfiguracaoBuilder;
import com.example.projeto2.API.Services.geracao.PreferenciasGeracaoBuilder;
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
public class GeracaoHorariosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeracaoHorariosService.class);
    private static final Set<String> CARGOS_COM_GERACAO = Set.of("gerente", "subgerente");
    private static final Set<String> CARGOS_COM_VALIDACAO = Set.of("supervisor");
    private static final Set<String> CARGOS_COM_PRESENCA_OBRIGATORIA_AO_SABADO = Set.of("gerente", "subgerente");
    private static final String ESTADO_RASCUNHO = "rascunho";
    private static final String ESTADO_PENDENTE = "pendente";
    private static final String ESTADO_APROVADO = "aprovado";
    private static final String ESTADO_REJEITADO = "rejeitado";
    private static final long DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS = 8 * 60L;
    // Formatadores movidos para HorarioFormatters (Fase 3.1)
    private static final DateTimeFormatter DATA_HORA_FORMATTER = HorarioFormatters.DATA_HORA_FORMATTER;
    private static final DateTimeFormatter DATA_FORMATTER = HorarioFormatters.DATA_FORMATTER;
    private static final Duration TEMPO_MAXIMO_GERACAO_ALTERNATIVA = Duration.ofSeconds(20);
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

    // Serviços extraídos na refatorização SRP (Fase 2)
    private final HorarioValidatorService validator;
    private final HorarioGeneratorEngine engine;

    public GeracaoHorariosService(LojautilizadorRepository lojautilizadorRepository,
                              HorarioRepository horarioRepository,
                              DayOffRepository dayOffRepository,
                              PreferenciaRepository preferenciaRepository,
                              PropostaHorarioMensalRepository propostaHorarioMensalRepository,
                              HorarioEspecialLojaRepository horarioEspecialLojaRepository,
                              RegrasLojaRepository regrasLojaRepository,
                              RegraRepository regraRepository,
                              TurnoRepository turnoRepository,
                              HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository,
                              HorarioValidatorService validator,
                              HorarioGeneratorEngine engine) {
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
        this.validator = validator;
        this.engine = engine;
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
    public int contarHorariosPendentesValidacao(Integer idUtilizador) {
        return obterLigacaoAtiva(idUtilizador)
                .filter(this::temPermissaoDeValidacao)
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
            horario.setEstado(EstadoHorario.pendente);
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
            horario.setEstado(EstadoHorario.valueOf(novoEstado.toLowerCase()));

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

    private void rejeitarPropostasPendentesConcorrentes(PropostaHorarioMensal propostaAprovada, com.example.projeto2.API.Modules.Utilizador decisor) {
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
                horario.setEstado(EstadoHorario.rejeitado);
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

        if (!validator.janelaRotacaoRespeiraMinimo(janelaRotacaoFinsDeSemanaSemanas)) {
            LOGGER.warn(
                    "A janela de rotacao de fins de semana configurada ({} semanas) e inferior ao minimo legal recomendado de 7 semanas. "
                    + "Considera atualizar a regra RFS08 da loja.",
                    janelaRotacaoFinsDeSemanaSemanas);
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
        for (EstadoColaboradorResumo estado : planeamento.estados()) {
            cargas.put(
                    estado.idUtilizador(),
                    new CargaColaborador(
                            estado.idUtilizador(),
                            valorOuTraco(estado.ligacao().getIdUtilizador().getNome()),
                            estado.minutosAtribuidos,
                            estado.cargaMaximaMinutos,
                            estado.totalFinsDeSemanaTrabalhados
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
        return proposta.nomeMes()
                + " " + proposta.ano()
                + " · #" + proposta.idProposta()
                + " · " + proposta.estado();
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
                horario.getId(),
                idColaborador,
                horario.getDataTurno(),
                horario.getDataTurno() != null ? nomeDiaSemana(horario.getDataTurno()) : "-",
                formatarTurno(horario.getIdTurno()),
                formatarPeriodo(horario.getIdTurno()),
                colaborador,
                cargo,
                formatarEstado(horario.getEstado() != null ? horario.getEstado().name() : null)
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

    // Formatadores delegados a HorarioFormatters (Fase 3.1) — wrappers manter callsites curtos
    private String formatarTurno(Turno turno)                 { return HorarioFormatters.formatarTurno(turno); }
    private String formatarPeriodo(Turno turno)               { return HorarioFormatters.formatarPeriodo(turno); }
    private String formatarPeriodoVinculo(Lojautilizador l)   { return HorarioFormatters.formatarPeriodoVinculo(l); }
    private String formatarEstado(String estado)              { return HorarioFormatters.formatarEstado(estado); }
    private String formatarDuracao(long minutosTotais)        { return HorarioFormatters.formatarDuracao(minutosTotais); }
    private String formatarDiferencaDuracao(long minutos)     { return HorarioFormatters.formatarDiferencaDuracao(minutos); }
    private String nomeMes(int mes)                           { return HorarioFormatters.nomeMes(mes); }
    private String nomeDiaSemana(LocalDate data)              { return HorarioFormatters.nomeDiaSemana(data); }
    private String valorOuTraco(String valor)                 { return HorarioFormatters.valorOuTraco(valor); }
    private String normalizarTexto(String texto)              { return HorarioFormatters.normalizarTexto(texto); }
    private String limparTexto(String texto)                  { return HorarioFormatters.limparTexto(texto); }

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

    public record ConfiguracaoDiaEspecial(
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
            Map<Integer, Set<LocalDate>> diasFolgaPreferidos,
            Map<Integer, List<Preferencia>> preferenciasTurnos,
            Map<Integer, List<Preferencia>> preferenciasColegas,
            Map<LocalDate, ConfiguracaoDiaEspecial> configuracoesPorData
    ) {
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
            List<EstadoColaboradorResumo> estados,
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


    // =========================================================================
    // EstadoColaboradorResumo — substitui EstadoColaborador para métricas pós-geração
    // Não contém lógica de algoritmo; apenas acumula carga para calcularMetricasPlaneamento.
    // =========================================================================
    private static final class EstadoColaboradorResumo {
        private final Lojautilizador ligacao;
        final long cargaMaximaMinutos;
        long minutosAtribuidos;
        int turnosAtribuidos;
        int totalFinsDeSemanaTrabalhados;

        EstadoColaboradorResumo(Lojautilizador ligacao, long cargaMaximaMinutos) {
            this.ligacao = ligacao;
            this.cargaMaximaMinutos = cargaMaximaMinutos;
        }

        Integer idUtilizador() {
            return ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getId() : null;
        }

        Lojautilizador ligacao() {
            return ligacao;
        }

        void registarTurno(long minutos) {
            minutosAtribuidos += minutos;
            turnosAtribuidos++;
        }

        int turnosAtribuidos() {
            return turnosAtribuidos;
        }
    }
}
