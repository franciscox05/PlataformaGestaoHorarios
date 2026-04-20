package com.example.projeto2;

import com.example.projeto2.BLL.GeracaoHorariosBLL;
import com.example.projeto2.BLL.PerfilBLL;
import com.example.projeto2.BLL.PreferenciaBLL;
import com.example.projeto2.BLL.SegurancaBLL;
import com.example.projeto2.BLL.SessaoBLL;
import com.example.projeto2.BLL.UtilizadorBLL;
import com.example.projeto2.Modules.Cargo;
import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.HistoricoHorarioEstado;
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

    private int sequenciaTelemovel = 930000000;

    @Autowired
    protected UtilizadorBLL utilizadorBLL;

    @Autowired
    protected PerfilBLL perfilBLL;

    @Autowired
    protected PreferenciaBLL preferenciaBLL;

    @Autowired
    protected GeracaoHorariosBLL geracaoHorariosBLL;

    @Autowired
    protected SegurancaBLL segurancaBLL;

    @Autowired
    protected SessaoBLL sessaoBLL;

    @Autowired
    protected UtilizadorRepository utilizadorRepository;

    @Autowired
    protected EventoAuditoriaRepository eventoAuditoriaRepository;

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

    @PersistenceContext
    protected EntityManager entityManager;

    @BeforeEach
    void limparSessaoAntesDoTeste() {
        if (sessaoBLL.temSessaoAtiva()) {
            sessaoBLL.terminarSessaoManual();
        }
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
        Cargo cargoColaborador = obterOuCriarCargo("fulltime", "Assistente de Vendas FT");

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

        criarLigacaoAtiva(gerente, loja, cargoGerente);
        criarLigacaoAtiva(supervisor, loja, cargoSupervisor);
        for (Utilizador colaborador : colaboradores) {
            criarLigacaoAtiva(colaborador, loja, cargoColaborador);
        }

        flushAndClear();
        return new LojaFixture(loja, gerente, supervisor, colaboradores);
    }

    protected GeracaoFixture criarContextoGeracao(String prefixo) {
        LojaFixture lojaFixture = criarLojaComEquipaCompleta(prefixo);
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

    private Lojautilizador criarLigacaoAtiva(Utilizador utilizador, Loja loja, Cargo cargo) {
        Lojautilizador ligacao = new Lojautilizador();
        ligacao.setIdUtilizador(utilizador);
        ligacao.setIdLoja(loja);
        ligacao.setIdCargo(cargo);
        ligacao.setDataInicio(LocalDate.now().minusDays(30));
        ligacao.setDataFim(null);
        return lojautilizadorRepository.save(ligacao);
    }

    private Cargo obterOuCriarCargo(String tipo, String nomePadrao) {
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

        if ((texto.contains("min") || texto.contains("minim"))
                && (texto.contains("turno") || texto.contains("colaborador") || texto.contains("equipa") || texto.contains("pessoas"))) {
            return 1;
        }
        if (texto.contains("consecut") || (texto.contains("dias") && texto.contains("seguid"))) {
            return 31;
        }
        if (texto.contains("descanso") && (texto.contains("hora") || texto.contains("interval"))) {
            return 11;
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
}
