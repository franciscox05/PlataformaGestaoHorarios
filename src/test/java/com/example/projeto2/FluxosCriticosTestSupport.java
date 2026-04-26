package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.GestaoLojaBLL;
import com.example.projeto2.BLL.DayOffBLL;
import com.example.projeto2.BLL.HorarioBLL;
import com.example.projeto2.BLL.PermutaBLL;
import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.BLL.SegurancaBLL;
import com.example.projeto2.BLL.SessaoBLL;
import com.example.projeto2.BLL.SnapshotOperacionalLojaBLL;
import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Cargo;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.HistoricoHorarioEstado;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.HorarioEspecialLoja;
import com.example.projeto2.Modules.Loja;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Modules.Regra;
import com.example.projeto2.Modules.RegrasLoja;
import com.example.projeto2.Modules.Turno;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.CargoRepository;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.EventoAuditoriaRepository;
import com.example.projeto2.Repositories.HistoricoHorarioEstadoRepository;
import com.example.projeto2.Repositories.HorarioEspecialLojaRepository;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojaRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.PreferenciaRepository;
import com.example.projeto2.Repositories.RegraRepository;
import com.example.projeto2.Repositories.RegrasLojaRepository;
import com.example.projeto2.Repositories.TurnoRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

abstract class FluxosCriticosTestSupport {

    private static final List<SequenciaTabela> SEQUENCIAS_TABELAS = List.of(
            new SequenciaTabela("public.cargos", "id_cargo"),
            new SequenciaTabela("public.lojas", "id_loja"),
            new SequenciaTabela("public.utilizadores", "id_utilizador"),
            new SequenciaTabela("public.lojautilizador", "id_lojautilizador"),
            new SequenciaTabela("public.turnos", "id_turno"),
            new SequenciaTabela("public.regras", "id_regra"),
            new SequenciaTabela("public.regras_loja", "id_regra_loja"),
            new SequenciaTabela("public.day_offs", "id_dayoff"),
            new SequenciaTabela("public.preferencias", "id_preferencia"),
            new SequenciaTabela("public.propostas_horario_mensal", "id_proposta_horario"),
            new SequenciaTabela("public.horarios", "id_horario"),
            new SequenciaTabela("public.historico_horario_estados", "id_registo"),
            new SequenciaTabela("public.horarios_especiais_loja", "id_horario_especial"),
            new SequenciaTabela("public.permutas", "id_permuta"),
            new SequenciaTabela("public.eventos_auditoria", "id_evento")
    );

    private int sequenciaTelemovel = 930000000;

    @Autowired
    protected UtilizadorBLL utilizadorBLL;

    @Autowired
    protected PerfilBLL perfilBLL;

    @Autowired
    protected GestaoLojaBLL gestaoLojaBLL;

    @Autowired
    protected PreferenciaBLL preferenciaBLL;

    @Autowired
    protected DayOffBLL dayOffBLL;

    @Autowired
    protected HorarioBLL horarioBLL;

    @Autowired
    protected PermutaBLL permutaBLL;

    @Autowired
    protected GeracaoHorariosBLL geracaoHorariosBLL;

    @Autowired
    protected SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;

    @Autowired
    protected SegurancaBLL segurancaBLL;

    @Autowired
    protected SessaoBLL sessaoBLL;

    @Autowired
    protected UtilizadorRepository utilizadorRepository;

    @Autowired
    protected EventoAuditoriaRepository eventoAuditoriaRepository;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected CargoRepository cargoRepository;

    @Autowired
    protected LojaRepository lojaRepository;

    @Autowired
    protected LojautilizadorRepository lojautilizadorRepository;

    @Autowired
    protected TurnoRepository turnoRepository;

    @Autowired
    protected RegraRepository regraRepository;

    @Autowired
    protected RegrasLojaRepository regrasLojaRepository;

    @Autowired
    protected DayOffRepository dayOffRepository;

    @Autowired
    protected PreferenciaRepository preferenciaRepository;

    @Autowired
    protected HorarioRepository horarioRepository;

    @Autowired
    protected HistoricoHorarioEstadoRepository historicoHorarioEstadoRepository;

    @Autowired
    protected HorarioEspecialLojaRepository horarioEspecialLojaRepository;

    @PersistenceContext
    protected EntityManager entityManager;

    @BeforeEach
    void limparSessaoAntesDoTeste() {
        if (sessaoBLL.temSessaoAtiva()) {
            sessaoBLL.terminarSessaoManual();
        }

        garantirEstruturasDeTeste();
        sincronizarSequencias();
    }

    @AfterEach
    void limparSessaoDepoisDoTeste() {
        if (sessaoBLL.temSessaoAtiva()) {
            sessaoBLL.terminarSessaoManual();
        }
    }

    protected LojaFixture criarLojaComEquipaCompleta(String prefixo) {
        String sufixo = novoSufixo();
        Loja loja = new Loja();
        loja.setNome("Loja Teste " + prefixo + " " + sufixo);
        loja.setLocalizacao("Ambiente de testes");
        loja.setHoraAbertura(LocalTime.of(10, 0));
        loja.setHoraFecho(LocalTime.of(23, 0));
        loja = lojaRepository.save(loja);

        Cargo cargoGerente = obterOuCriarCargo("gerente", "Gerente de Loja");
        Cargo cargoSupervisor = obterOuCriarCargo("supervisor", "Supervisor de Equipa");
        Cargo cargoFullTime = obterOuCriarCargo("fulltime", "Assistente de Vendas FT");
        Cargo cargoPartTime = obterOuCriarCargo("parttime", "Assistente de Vendas PT");
        Cargo cargoReforco = obterOuCriarCargo("reforco_parttime", "Reforco Fim de Semana");

        Utilizador gerente = criarUtilizadorHashado("Gerente Teste " + sufixo, "gerente." + sufixo, "Gestor123");
        Utilizador supervisor = criarUtilizadorHashado("Supervisor Teste " + sufixo, "supervisor." + sufixo, "Supervisor123");

        List<Utilizador> colaboradores = new ArrayList<>();
        for (char indice = 'A'; indice <= 'E'; indice++) {
            colaboradores.add(
                    criarUtilizadorHashado(
                            "Colaborador " + indice + " " + sufixo,
                            "colaborador" + Character.toLowerCase(indice) + "." + sufixo,
                            "Colaborador123"
                    )
            );
        }
        for (char indice = 'F'; indice <= 'G'; indice++) {
            colaboradores.add(
                    criarUtilizadorHashado(
                            "Colaborador " + indice + " " + sufixo,
                            "colaborador" + Character.toLowerCase(indice) + "." + sufixo,
                            "Colaborador123"
                    )
            );
        }
        colaboradores.add(
                criarUtilizadorHashado(
                        "Colaborador H " + sufixo,
                        "colaboradorh." + sufixo,
                        "Colaborador123"
                )
        );

        criarLigacaoAtiva(gerente, loja, cargoGerente);
        criarLigacaoAtiva(supervisor, loja, cargoSupervisor);
        for (int indice = 0; indice < 5; indice++) {
            criarLigacaoAtiva(colaboradores.get(indice), loja, cargoFullTime);
        }
        criarLigacaoAtiva(colaboradores.get(5), loja, cargoPartTime);
        criarLigacaoAtiva(colaboradores.get(6), loja, cargoPartTime);
        criarLigacaoAtiva(colaboradores.get(7), loja, cargoReforco);

        flushAndClear();
        return new LojaFixture(loja, gerente, supervisor, colaboradores);
    }

    protected GeracaoFixture criarContextoGeracao(String prefixo) {
        LojaFixture lojaFixture = criarLojaComEquipaCompleta(prefixo);
        Cargo cargoSubgerente = obterOuCriarCargo("subgerente", "Sub-Gerente");
        Utilizador subgerente = criarUtilizadorHashado("Subgerente Teste " + prefixo, "subgerente." + prefixo, "Subgerente123");
        criarLigacaoAtiva(subgerente, lojaFixture.loja(), cargoSubgerente);
        garantirTurnosBase();
        criarRegrasGeracaoDeterministicas();
        aplicarOverridesGeracaoDeterministicos(lojaFixture.loja());
        flushAndClear();

        LocalDate referencia = LocalDate.now().plusMonths(2).withDayOfMonth(1);
        return new GeracaoFixture(lojaFixture, referencia, turnoRepository.findAllByOrderByHoraInicioAsc());
    }

    protected Utilizador criarUtilizadorComPasswordEmTexto(String nome, String emailPrefixo, String passwordEmTexto, String estado) {
        Utilizador utilizador = new Utilizador();
        utilizador.setNome(nome);
        utilizador.setEmail(emailPrefixo + "@testes.local");
        utilizador.setTelemovel(novoTelemovel());
        utilizador.setPasswordHash(passwordEmTexto);
        utilizador.setEstado(estado);
        return utilizadorRepository.save(utilizador);
    }

    protected Utilizador criarUtilizadorHashado(String nome, String emailPrefixo, String passwordEmTexto) {
        Utilizador utilizador = new Utilizador();
        utilizador.setNome(nome);
        utilizador.setEmail(emailPrefixo + "@testes.local");
        utilizador.setTelemovel(novoTelemovel());
        utilizador.setPasswordHash(segurancaBLL.gerarHash(passwordEmTexto));
        utilizador.setEstado("ativo");
        return utilizadorRepository.save(utilizador);
    }

    protected DayOff criarDayOffAprovado(Integer idUtilizador, LocalDate data, String motivo) {
        DayOff dayOff = new DayOff();
        dayOff.setIdUtilizador(idUtilizador);
        dayOff.setDataAusencia(data);
        dayOff.setMotivo(motivo);
        dayOff.setTipo("folgas");
        dayOff.setEstado("aprovado");
        return dayOffRepository.save(dayOff);
    }

    protected Preferencia criarPreferenciaAprovada(Utilizador colaborador,
                                                   Utilizador decisor,
                                                   String tipo,
                                                   LocalDate dataInicio,
                                                   LocalDate dataFim,
                                                   Integer prioridade,
                                                   String descricao) {
        Preferencia preferencia = new Preferencia();
        preferencia.setIdUtilizador(colaborador);
        preferencia.setTipo(tipo);
        preferencia.setDataInicio(dataInicio);
        preferencia.setDataFim(dataFim);
        preferencia.setPrioridade(prioridade);
        preferencia.setDescricao(descricao);
        preferencia.setEstado("aprovado");
        preferencia.setDecisao("Aprovado em fixture de teste.");
        preferencia.setIdDecisor(decisor);
        preferencia.setDataDecisao(LocalDateTime.now());
        return preferenciaRepository.save(preferencia);
    }

    protected Horario criarHorarioPublicadoSemProposta(Utilizador colaborador,
                                                       LocalDate dataTurno,
                                                       Turno turno) {
        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(colaborador.getId())
                .orElseThrow();

        Horario horario = new Horario();
        horario.setIdLojautilizador(ligacaoAtiva);
        horario.setIdTurno(turno);
        horario.setDataTurno(dataTurno);
        horario.setEstado("aprovado");
        horario.setIdPropostaHorario(null);
        return horarioRepository.save(horario);
    }

    protected long contarEventos(String tipoEvento, String resultado, String emailReferencia) {
        return eventoAuditoriaRepository.findAll().stream()
                .filter(evento -> tipoEvento.equalsIgnoreCase(valorOuVazio(evento.getTipoEvento())))
                .filter(evento -> resultado.equalsIgnoreCase(valorOuVazio(evento.getResultado())))
                .filter(evento -> emailReferencia.equalsIgnoreCase(valorOuVazio(evento.getEmailReferencia())))
                .count();
    }

    protected List<HistoricoHorarioEstado> listarHistoricoPorHorarios(Set<Integer> idsHorario) {
        return StreamSupport.stream(historicoHorarioEstadoRepository.findAll().spliterator(), false)
                .filter(registo -> registo.getIdHorario() != null)
                .filter(registo -> registo.getIdHorario().getId() != null)
                .filter(registo -> idsHorario.contains(registo.getIdHorario().getId()))
                .toList();
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    protected int contarLigacoesAtivas(Integer idLoja) {
        return (int) lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(idLoja).stream()
                .filter(ligacao -> ligacao.getDataFim() == null)
                .count();
    }

    protected HorarioEspecialLoja criarHorarioEspecial(Integer idUtilizadorGerente,
                                                       String descricao,
                                                       LocalDate dataInicio,
                                                       LocalDate dataFim,
                                                       boolean lojaEncerrada,
                                                       LocalTime horaAbertura,
                                                       LocalTime horaFecho,
                                                       Integer minimoColaboradoresTurno,
                                                       String observacoes) {
        gestaoLojaBLL.guardarHorarioEspecial(
                idUtilizadorGerente,
                new GestaoLojaBLL.ConfiguracaoHorarioEspecialRequest(
                        null,
                        descricao,
                        dataInicio,
                        dataFim,
                        lojaEncerrada,
                        horaAbertura,
                        horaFecho,
                        minimoColaboradoresTurno,
                        observacoes
                )
        );

        return horarioEspecialLojaRepository.findAll().stream()
                .filter(horarioEspecial -> horarioEspecial.getIdLoja() != null)
                .filter(horarioEspecial -> horarioEspecial.getIdLoja().getId() != null)
                .filter(horarioEspecial -> horarioEspecial.getDataInicio() != null)
                .filter(horarioEspecial -> horarioEspecial.getDataFim() != null)
                .filter(horarioEspecial -> horarioEspecial.getDataInicio().equals(dataInicio))
                .filter(horarioEspecial -> horarioEspecial.getDataFim().equals(dataFim))
                .filter(horarioEspecial -> descricao.equals(horarioEspecial.getDescricao()))
                .findFirst()
                .orElseThrow();
    }

    protected String descreverTurnosBase() {
        return turnoRepository.findAllByOrderByHoraInicioAsc().stream()
                .map(turno -> turno.getTipo() + "=" + turno.getHoraInicio() + "-" + turno.getHoraFim())
                .collect(Collectors.joining(", "));
    }

    protected String descreverRegrasGeracaoDaLoja(Integer idLoja) {
        Map<Integer, Integer> overrides = new LinkedHashMap<>();
        for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(idLoja)) {
            if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null) {
                overrides.put(regraLoja.getIdRegra().getId(), regraLoja.getValorEspecifico());
            }
        }

        return regraRepository.findAllByOrderByDescricaoAsc().stream()
                .map(regra -> regra.getDescricao() + "=" + overrides.getOrDefault(regra.getId(), regra.getValorPadrao()))
                .collect(Collectors.joining(", "));
    }

    protected Lojautilizador criarLigacao(Utilizador utilizador,
                                          Loja loja,
                                          Cargo cargo,
                                          LocalDate dataInicio,
                                          LocalDate dataFim) {
        Lojautilizador ligacao = new Lojautilizador();
        ligacao.setIdUtilizador(utilizador);
        ligacao.setIdLoja(loja);
        ligacao.setIdCargo(cargo);
        ligacao.setDataInicio(dataInicio);
        ligacao.setDataFim(dataFim);
        return lojautilizadorRepository.save(ligacao);
    }

    protected Lojautilizador criarLigacaoAtiva(Utilizador utilizador, Loja loja, Cargo cargo) {
        return criarLigacao(utilizador, loja, cargo, LocalDate.now().minusDays(30), null);
    }

    protected Cargo obterOuCriarCargo(String tipo, String nomePadrao) {
        return cargoRepository.findAllByOrderByNomeAsc().stream()
                .filter(cargo -> cargo.getTipo() != null)
                .filter(cargo -> tipo.equalsIgnoreCase(cargo.getTipo()))
                .findFirst()
                .orElseGet(() -> {
                    Cargo cargo = new Cargo();
                    cargo.setNome(nomePadrao);
                    cargo.setTipo(tipo);
                    cargo.setDescricao("Cargo criado automaticamente para testes.");
                    return cargoRepository.save(cargo);
                });
    }

    private void garantirTurnosBase() {
        if (!turnoRepository.findAllByOrderByHoraInicioAsc().isEmpty()) {
            return;
        }

        criarTurno("manha", LocalTime.of(10, 0), LocalTime.of(19, 0));
        criarTurno("intermedio", LocalTime.of(12, 0), LocalTime.of(21, 0));
        criarTurno("noite", LocalTime.of(14, 0), LocalTime.of(23, 0));
        criarTurno("manha", LocalTime.of(10, 0), LocalTime.of(14, 30));
        criarTurno("intermedio", LocalTime.of(14, 0), LocalTime.of(18, 30));
        criarTurno("noite", LocalTime.of(18, 30), LocalTime.of(23, 0));
    }

    private void criarTurno(String tipo, LocalTime horaInicio, LocalTime horaFim) {
        Turno turno = new Turno();
        turno.setTipo(tipo);
        turno.setHoraInicio(horaInicio);
        turno.setHoraFim(horaFim);
        turnoRepository.save(turno);
    }

    private void criarRegrasGeracaoDeterministicas() {
        criarRegra("000 Descanso minimo entre turnos", 11, "legal");
        criarRegra("000 Maximo de dias consecutivos", 31, "legal");
        criarRegra("000 Minimo de colaboradores por turno", 1, "operacional");
        criarRegra("000 Carga contratual mensal gestao (horas)", 176, "contratual");
        criarRegra("000 Carga contratual mensal full-time (horas)", 176, "contratual");
        criarRegra("000 Carga contratual mensal part-time (horas)", 96, "contratual");
        criarRegra("000 Carga contratual mensal reforco de fim de semana (horas)", 64, "contratual");
        criarRegra("000 Descanso semanal minimo (dias)", 2, "descanso");
        criarRegra("000 Janela de rotacao de fins de semana (semanas)", 2, "descanso");
        criarRegra("000 Dia limite de lancamento do horario mensal", 15, "administrativo");
        criarRegra("000 Presenca de gerente ou subgerente aos sabados", 1, "operacional");
    }

    private void aplicarOverridesGeracaoDeterministicos(Loja loja) {
        for (Regra regra : regraRepository.findAllByOrderByDescricaoAsc()) {
            Integer valor = valorDeterministicoParaRegra(regra);
            if (valor == null) {
                continue;
            }

            RegrasLoja regraLoja = regrasLojaRepository
                    .findByIdLojaIdAndIdRegraId(loja.getId(), regra.getId())
                    .orElseGet(RegrasLoja::new);
            regraLoja.setIdLoja(loja);
            regraLoja.setIdRegra(regra);
            regraLoja.setValorEspecifico(valor);
            regraLoja.setObservacoes("Override criado automaticamente para testes de integracao.");
            regrasLojaRepository.save(regraLoja);
        }
    }

    private void criarRegra(String descricao, Integer valorPadrao, String tipo) {
        boolean regraJaExiste = regraRepository.findAllByOrderByDescricaoAsc().stream()
                .anyMatch(regraExistente -> normalizarTexto(regraExistente.getDescricao()).equals(normalizarTexto(descricao)));
        if (regraJaExiste) {
            return;
        }

        Regra regra = new Regra();
        regra.setDescricao(descricao);
        regra.setValorPadrao(valorPadrao);
        regra.setTipo(tipo);
        regraRepository.save(regra);
    }

    private Integer valorDeterministicoParaRegra(Regra regra) {
        String texto = normalizarTexto(
                (regra.getDescricao() == null ? "" : regra.getDescricao()) + " "
                        + (regra.getTipo() == null ? "" : regra.getTipo())
        );

        if (texto.contains("descanso") && (texto.contains("hora") || texto.contains("interval"))) {
            return 11;
        }
        if (texto.contains("entre") && texto.contains("turno")) {
            return 11;
        }
        if (texto.contains("descanso") && (texto.contains("seman") || texto.contains("folga")) && texto.contains("dia")) {
            return 2;
        }
        if ((texto.contains("dias") || texto.contains("dia"))
                && (texto.contains("consecut") || texto.contains("seguid"))
                && !texto.contains("hora")) {
            return 31;
        }
        if ((texto.contains("min") || texto.contains("minim"))
                && (texto.contains("colaborador") || texto.contains("equipa") || texto.contains("pessoas")
                || texto.contains("funcionario")
                || (texto.contains("turno") && (texto.contains("por") || texto.contains("cobertura") || texto.contains("loja"))))) {
            return 1;
        }
        if ((texto.contains("rotacao") || texto.contains("janela")) && (texto.contains("fim de semana") || texto.contains("weekend"))) {
            return 2;
        }
        if (texto.contains("dia") && texto.contains("limite") && (texto.contains("lancamento") || texto.contains("publicacao") || texto.contains("publicar"))) {
            return 15;
        }
        if (texto.contains("sabado") && (texto.contains("gerente") || texto.contains("subgerente") || texto.contains("chefia") || texto.contains("gestao"))) {
            return 1;
        }
        if (texto.contains("carga") && (texto.contains("contrat") || texto.contains("mensal"))) {
            if (texto.contains("gestao") || texto.contains("gerencia") || texto.contains("gestor") || texto.contains("supervisor")) {
                return 176;
            }
            if (texto.contains("fulltime") || (texto.contains("full") && texto.contains("time")) || texto.contains("tempo inteiro")) {
                return 176;
            }
            if (texto.contains("parttime") || (texto.contains("part") && texto.contains("time")) || texto.contains("tempo parcial")) {
                return 96;
            }
            if (texto.contains("reforco") || texto.contains("fim de semana") || texto.contains("weekend")) {
                return 64;
            }
        }
        return null;
    }

    private String novoSufixo() {
        return UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.ROOT);
    }

    private String novoTelemovel() {
        sequenciaTelemovel += 1;
        return String.valueOf(sequenciaTelemovel);
    }

    private String valorOuVazio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private void sincronizarSequencias() {
        for (SequenciaTabela sequenciaTabela : SEQUENCIAS_TABELAS) {
            Boolean tabelaExiste = jdbcTemplate.queryForObject(
                    "SELECT to_regclass('%s') IS NOT NULL".formatted(sequenciaTabela.tabela()),
                    Boolean.class
            );
            if (!Boolean.TRUE.equals(tabelaExiste)) {
                continue;
            }

            String nomeSequencia = jdbcTemplate.queryForObject(
                    "SELECT pg_get_serial_sequence('%s', '%s')".formatted(
                            sequenciaTabela.tabela(),
                            sequenciaTabela.colunaId()
                    ),
                    String.class
            );
            if (nomeSequencia == null || nomeSequencia.isBlank()) {
                continue;
            }

            jdbcTemplate.execute(
                    """
                    SELECT setval(
                        '%s',
                        COALESCE((SELECT MAX(%s) FROM %s), 1),
                        true
                    )
                    """.formatted(
                            nomeSequencia,
                            sequenciaTabela.colunaId(),
                            sequenciaTabela.tabela()
                    )
            );
        }
    }

    private void garantirEstruturasDeTeste() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS public.eventos_auditoria (
                    id_evento SERIAL PRIMARY KEY,
                    id_utilizador INTEGER NULL,
                    email_referencia VARCHAR(160) NULL,
                    tipo_evento VARCHAR(80) NOT NULL,
                    resultado VARCHAR(40) NOT NULL,
                    detalhe TEXT NULL,
                    identificador_sessao VARCHAR(120) NULL,
                    data_evento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_eventos_auditoria_utilizador
                        FOREIGN KEY (id_utilizador) REFERENCES public.utilizadores(id_utilizador)
                )
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.eventos_auditoria
                    ADD COLUMN IF NOT EXISTS origem VARCHAR(80)
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.eventos_auditoria
                    ADD COLUMN IF NOT EXISTS detalhes TEXT
                """
        );

        jdbcTemplate.execute(
                """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'eventos_auditoria'
                          AND column_name = 'detalhe'
                    ) THEN
                        EXECUTE '
                            UPDATE public.eventos_auditoria
                            SET detalhes = COALESCE(detalhes, detalhe)
                        ';
                    END IF;
                END $$;
                """
        );

        jdbcTemplate.execute(
                """
                UPDATE public.eventos_auditoria
                SET origem = COALESCE(NULLIF(origem, ''), 'sistema')
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.eventos_auditoria
                    ALTER COLUMN origem SET DEFAULT 'sistema'
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.eventos_auditoria
                    ALTER COLUMN origem SET NOT NULL
                """
        );

        jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_data
                    ON public.eventos_auditoria (data_evento DESC)
                """
        );

        jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_utilizador
                    ON public.eventos_auditoria (id_utilizador, data_evento DESC)
                """
        );

        jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_sessao
                    ON public.eventos_auditoria (identificador_sessao)
                """
        );

        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS public.propostas_horario_mensal (
                    id_proposta_horario SERIAL PRIMARY KEY,
                    id_loja INTEGER NOT NULL,
                    id_utilizador_geracao INTEGER NOT NULL,
                    ano INTEGER NOT NULL,
                    mes INTEGER NOT NULL,
                    estado VARCHAR(50) NOT NULL DEFAULT 'pendente',
                    resumo_geracao TEXT NULL,
                    data_geracao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_proposta_horario_loja
                        FOREIGN KEY (id_loja) REFERENCES public.lojas(id_loja),
                    CONSTRAINT fk_proposta_horario_utilizador
                        FOREIGN KEY (id_utilizador_geracao) REFERENCES public.utilizadores(id_utilizador)
                )
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.propostas_horario_mensal
                    ADD COLUMN IF NOT EXISTS id_utilizador_decisao INTEGER
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.propostas_horario_mensal
                    ADD COLUMN IF NOT EXISTS data_decisao TIMESTAMP
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.propostas_horario_mensal
                    ADD COLUMN IF NOT EXISTS observacoes_supervisor TEXT
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.propostas_horario_mensal
                    DROP CONSTRAINT IF EXISTS fk_proposta_horario_decisao_utilizador
                """
        );

        jdbcTemplate.execute(
                """
                ALTER TABLE public.propostas_horario_mensal
                    ADD CONSTRAINT fk_proposta_horario_decisao_utilizador
                        FOREIGN KEY (id_utilizador_decisao) REFERENCES public.utilizadores(id_utilizador)
                """
        );

        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS public.horarios_especiais_loja (
                    id_horario_especial SERIAL PRIMARY KEY,
                    id_loja INTEGER NOT NULL,
                    descricao VARCHAR(160) NOT NULL,
                    data_inicio DATE NOT NULL,
                    data_fim DATE NOT NULL,
                    hora_abertura TIME NULL,
                    hora_fecho TIME NULL,
                    minimo_colaboradores_turno INTEGER NULL,
                    loja_encerrada BOOLEAN NOT NULL DEFAULT FALSE,
                    observacoes TEXT NULL,
                    CONSTRAINT fk_horarios_especiais_loja
                        FOREIGN KEY (id_loja) REFERENCES public.lojas(id_loja),
                    CONSTRAINT ck_horarios_especiais_periodo
                        CHECK (data_inicio <= data_fim),
                    CONSTRAINT ck_horarios_especiais_horas
                        CHECK (
                            (hora_abertura IS NULL AND hora_fecho IS NULL)
                            OR (hora_abertura IS NOT NULL AND hora_fecho IS NOT NULL AND hora_abertura < hora_fecho)
                        ),
                    CONSTRAINT ck_horarios_especiais_minimo
                        CHECK (minimo_colaboradores_turno IS NULL OR minimo_colaboradores_turno > 0)
                )
                """
        );

        jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_horarios_especiais_loja_periodo
                    ON public.horarios_especiais_loja (id_loja, data_inicio, data_fim)
                """
        );
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    protected record LojaFixture(
            Loja loja,
            Utilizador gerente,
            Utilizador supervisor,
            List<Utilizador> colaboradores
    ) {
    }

    protected record GeracaoFixture(
            LojaFixture lojaFixture,
            LocalDate referencia,
            List<Turno> turnos
    ) {
    }

    private record SequenciaTabela(
            String tabela,
            String colunaId
    ) {
    }
}
