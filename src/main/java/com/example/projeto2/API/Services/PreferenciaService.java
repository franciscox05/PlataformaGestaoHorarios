package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Utilizador;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import com.example.projeto2.API.Repositories.PreferenciaRepository;
import com.example.projeto2.API.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class PreferenciaService {

    // "folga_preferida": folga recorrente semanal (soft) — o algoritmo dá-lhe muita atenção
    // mas pode escalar o colaborador se for necessário para a cobertura (ao contrário de
    // "folgas"/"ferias", que são ausências concedidas = bloqueio absoluto).
    private static final Set<String> TIPOS_VALIDOS = Set.of("folgas", "ferias", "colegas", "turnos", "folga_preferida");
    private static final Set<String> ESTADOS_VALIDOS = Set.of("pendente", "aprovado", "rejeitado");

    private final PreferenciaRepository preferenciaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final LojautilizadorHelper lojautilizadorHelper;
    private final NotificacaoService notificacaoService;
    private final AuditoriaService auditoriaService;

    public PreferenciaService(PreferenciaRepository preferenciaRepository,
                          UtilizadorRepository utilizadorRepository,
                          LojautilizadorRepository lojautilizadorRepository,
                          LojautilizadorHelper lojautilizadorHelper,
                          NotificacaoService notificacaoService,
                          AuditoriaService auditoriaService) {
        this.preferenciaRepository = preferenciaRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.lojautilizadorHelper = lojautilizadorHelper;
        this.notificacaoService = notificacaoService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarPreferenciasPorUtilizador(Integer idUtilizador) {
        validarUtilizador(idUtilizador);
        return preferenciaRepository.findByIdUtilizadorIdOrderByDataInicioAscIdDesc(idUtilizador);
    }

    @Transactional(readOnly = true)
    public Preferencia obterPreferenciaDoUtilizador(Integer idUtilizador, Integer idPreferencia) {
        validarUtilizador(idUtilizador);

        if (idPreferencia == null) {
            throw new IllegalArgumentException("A preferencia selecionada e obrigatoria.");
        }

        return preferenciaRepository.findByIdAndIdUtilizadorId(idPreferencia, idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar a preferencia selecionada."));
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAprovarPreferencias(Integer idUtilizador) {
        return lojautilizadorHelper.temCargo(idUtilizador, LojautilizadorHelper.GESTAO);
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAprovarPreferencias(Integer idUtilizador, Integer idLoja) {
        return lojautilizadorHelper.temCargo(idUtilizador, idLoja, LojautilizadorHelper.GESTAO);
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarPreferenciasPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para aprovar preferencias.");

        return preferenciaRepository.findPreferenciasPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    /** Store-scoped variant — uses the explicit idLoja from the session. */
    @Transactional(readOnly = true)
    public List<Preferencia> listarPreferenciasPendentesParaAprovacao(Integer idUtilizadorAprovador,
                                                                       Integer idLoja) {
        if (idLoja == null) return listarPreferenciasPendentesParaAprovacao(idUtilizadorAprovador);
        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, idLoja, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para aprovar preferencias.");
        return preferenciaRepository.findPreferenciasPendentesDaLoja(ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador);
    }

    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador) {
        return lojautilizadorHelper.findLigacaoAtivaComCargo(idUtilizador, LojautilizadorHelper.GESTAO)
                .map(lu -> (int) preferenciaRepository.countPreferenciasPendentesDaLoja(
                        lu.getIdLoja().getId(), idUtilizador))
                .orElse(0);
    }

    /** Store-scoped badge count — conta apenas as pendentes da loja activa da sessão. */
    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador, Integer idLoja) {
        if (idLoja == null) return contarPendentesParaAprovacao(idUtilizador);
        return lojautilizadorHelper.findLigacaoAtivaComCargo(idUtilizador, idLoja, LojautilizadorHelper.GESTAO)
                .map(lu -> (int) preferenciaRepository.countPreferenciasPendentesDaLoja(
                        lu.getIdLoja().getId(), idUtilizador))
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarHistoricoDecisoesDaLoja(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para aprovar preferencias.");

        return preferenciaRepository.findHistoricoDecisoesDaLoja(ligacaoAtiva.getIdLoja().getId());
    }

    /** Store-scoped variant — uses the explicit idLoja from the session. */
    @Transactional(readOnly = true)
    public List<Preferencia> listarHistoricoDecisoesDaLoja(Integer idUtilizadorAprovador, Integer idLoja) {
        if (idLoja == null) return listarHistoricoDecisoesDaLoja(idUtilizadorAprovador);
        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, idLoja, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para aprovar preferencias.");
        return preferenciaRepository.findHistoricoDecisoesDaLoja(ligacaoAtiva.getIdLoja().getId());
    }

    /** Backward-compatible 1-arg overload — delegates with null store context. */
    @Transactional(readOnly = true)
    public List<String> listarColegasDaLoja(Integer idUtilizador) {
        return listarColegasDaLoja(idUtilizador, null);
    }

    /** Store-scoped variant — uses idLoja directly to avoid NonUniqueResultException for multi-store users. */
    @Transactional(readOnly = true)
    public List<String> listarColegasDaLoja(Integer idUtilizador, Integer idLoja) {
        Integer lojaId = idLoja;
        if (lojaId == null) {
            Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtiva(idUtilizador);
            lojaId = ligacaoAtiva.getIdLoja().getId();
        }
        final Integer lojaIdFinal = lojaId;
        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(lojaIdFinal).stream()
                .filter(ligacao -> ligacao.getIdUtilizador() != null && ligacao.getIdUtilizador().getId() != null)
                .filter(ligacao -> !Objects.equals(ligacao.getIdUtilizador().getId(), idUtilizador))
                .filter(ligacao -> ligacao.getDataFim() == null)
                .filter(ligacao -> ligacao.getIdUtilizador().getEstado() != null
                        && com.example.projeto2.API.Enums.EstadoUtilizador.ativo == ligacao.getIdUtilizador().getEstado())
                .map(ligacao -> ligacao.getIdUtilizador().getNome())
                .filter(nome -> nome != null && !nome.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(Comparator.comparing(nome -> nome.toLowerCase()))
                .toList();
    }

    @Transactional
    public Preferencia guardarPreferencia(Integer idUtilizador, Preferencia preferenciaRecebida) {
        Utilizador utilizador = obterUtilizador(idUtilizador);

        if (preferenciaRecebida == null) {
            throw new IllegalArgumentException("A preferencia e obrigatoria.");
        }

        Preferencia preferenciaPersistida;
        if (preferenciaRecebida.getId() != null) {
            preferenciaPersistida = preferenciaRepository.findByIdAndIdUtilizadorId(preferenciaRecebida.getId(), idUtilizador)
                    .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel atualizar a preferencia selecionada."));
        } else {
            preferenciaPersistida = new Preferencia();
            preferenciaPersistida.setIdUtilizador(utilizador);
        }

        String tipoNormalizado = normalizarTipo(preferenciaRecebida.getTipo());

        if (preferenciaPersistida.getId() == null
                && preferenciaRepository.existsPreferenciaAtivaPorTipo(idUtilizador, tipoNormalizado)) {
            throw new IllegalArgumentException(
                    "Já tens uma preferência de " + formatarTipoParaMensagem(tipoNormalizado)
                    + " registada. Edita ou remove a existente antes de criar uma nova.");
        }
        String descricaoNormalizada = normalizarDescricao(preferenciaRecebida.getDescricao());
        Integer prioridadeNormalizada = normalizarPrioridade(preferenciaRecebida.getPrioridade(), tipoNormalizado);
        LocalDate dataInicio = normalizarDataInicio(
                tipoNormalizado,
                preferenciaRecebida.getDataInicio(),
                preferenciaRecebida.getDataFim()
        );
        LocalDate dataFim = preferenciaRecebida.getDataFim();

        validarPeriodo(tipoNormalizado, dataInicio, dataFim);
        validarRegistoOperacional(idUtilizador, tipoNormalizado);

        if (existePreferenciaDuplicada(
                idUtilizador,
                tipoNormalizado,
                descricaoNormalizada,
                prioridadeNormalizada,
                dataInicio,
                dataFim,
                preferenciaPersistida.getId()
        )) {
            throw new IllegalArgumentException("Ja tens uma preferencia igual registada.");
        }

        preferenciaPersistida.setIdUtilizador(utilizador);
        preferenciaPersistida.setTipo(tipoNormalizado);
        preferenciaPersistida.setDataInicio(dataInicio);
        preferenciaPersistida.setDataFim(dataFim);
        preferenciaPersistida.setPrioridade(prioridadeNormalizada);
        preferenciaPersistida.setDescricao(descricaoNormalizada);
        preferenciaPersistida.setEstado("pendente");
        preferenciaPersistida.setDecisao(null);
        preferenciaPersistida.setIdDecisor(null);
        preferenciaPersistida.setDataDecisao(null);

        Preferencia preferenciaGuardada = preferenciaRepository.save(preferenciaPersistida);

        // Notify store managers — wrapped so any failure never rolls back the save above
        try {
            lojautilizadorHelper.findLigacaoAtiva(idUtilizador).ifPresent(ligacao -> {
                String nomeColaborador = utilizador.getNome() != null ? utilizador.getNome() : "Um colaborador";
                String mensagem = "O funcionário " + nomeColaborador
                        + " submeteu uma nova preferência de horário que aguarda a tua revisão.";
                lojautilizadorHelper.listarIdsComCargoPorLoja(
                                ligacao.getIdLoja().getId(), LojautilizadorHelper.GESTAO)
                        .forEach(idGerente -> notificacaoService.criarNotificacao(idGerente, mensagem, ligacao.getIdLoja().getId()));
            });
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(PreferenciaService.class)
                    .warn("Notificação de preferência não enviada para id={}: {}", idUtilizador, e.getMessage());
        }

        return preferenciaGuardada;
    }

    @Transactional
    public void removerPreferencia(Integer idUtilizador, Integer idPreferencia) {
        Preferencia preferencia = obterPreferenciaDoUtilizador(idUtilizador, idPreferencia);
        preferenciaRepository.delete(preferencia);
    }

    @Transactional
    public Preferencia aprovarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador, String decisao) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, null, "aprovado", decisao);
    }

    @Transactional
    public Preferencia rejeitarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador, String decisao) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, null, "rejeitado", decisao);
    }

    /** Store-scoped approval — permission validated strictly within the given store. */
    @Transactional
    public Preferencia aprovarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador,
                                          String decisao, Integer idLoja) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, idLoja, "aprovado", decisao);
    }

    /** Store-scoped rejection — permission validated strictly within the given store. */
    @Transactional
    public Preferencia rejeitarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador,
                                           String decisao, Integer idLoja) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, idLoja, "rejeitado", decisao);
    }

    @Transactional(readOnly = true)
    public boolean estadoValido(String estado) {
        if (estado == null || estado.isBlank()) {
            return false;
        }
        return ESTADOS_VALIDOS.contains(estado.trim().toLowerCase());
    }

    private void validarUtilizador(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
        }
    }

    private Utilizador obterUtilizador(Integer idUtilizador) {
        validarUtilizador(idUtilizador);

        return utilizadorRepository.findById(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar o utilizador autenticado."));
    }

    private Preferencia decidirPreferencia(Integer idPreferencia,
                                           Integer idUtilizadorAprovador,
                                           Integer idLoja,
                                           String novoEstado,
                                           String decisaoRecebida) {
        if (idPreferencia == null) {
            throw new IllegalArgumentException("A preferencia selecionada e obrigatoria.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorHelper.obterLigacaoAtivaComCargo(
                idUtilizadorAprovador, idLoja, LojautilizadorHelper.GESTAO,
                "Nao tens permissao para aprovar preferencias.");

        Preferencia preferencia = preferenciaRepository.findPreferenciaDaLoja(idPreferencia, ligacaoAtiva.getIdLoja().getId())
                .orElseThrow(() -> new IllegalArgumentException("Nao foi possivel encontrar a preferencia selecionada."));

        if (!"pendente".equalsIgnoreCase(preferencia.getEstado())) {
            throw new IllegalArgumentException("Esta preferencia ja foi decidida.");
        }

        if (preferencia.getIdUtilizador() == null || preferencia.getIdUtilizador().getId() == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar o colaborador desta preferencia.");
        }

        if (Objects.equals(preferencia.getIdUtilizador().getId(), idUtilizadorAprovador)) {
            throw new IllegalArgumentException("Nao podes decidir as tuas proprias preferencias.");
        }

        preferencia.setEstado(novoEstado);
        preferencia.setDecisao(normalizarDecisao(decisaoRecebida));
        preferencia.setIdDecisor(ligacaoAtiva.getIdUtilizador());
        preferencia.setDataDecisao(LocalDateTime.now());

        Preferencia preferenciaGuardada = preferenciaRepository.save(preferencia);

        Integer idFuncionario = preferenciaGuardada.getIdUtilizador() != null
                ? preferenciaGuardada.getIdUtilizador().getId() : null;
        if (idFuncionario != null) {
            String tipoFormatado = preferenciaGuardada.getTipo() != null
                    ? preferenciaGuardada.getTipo().replace("_", " ") : "horário";
            String decisaoLabel = "aprovado".equalsIgnoreCase(novoEstado) ? "Aprovada" : "Rejeitada";
            notificacaoService.criarNotificacao(idFuncionario,
                    "A tua preferência de horário (" + tipoFormatado + ") foi " + decisaoLabel + " pela gerência.",
                    ligacaoAtiva.getIdLoja().getId());
        }

        String tipoEvento = "aprovado".equalsIgnoreCase(novoEstado) ? "PREFERENCIA_APROVADA" : "PREFERENCIA_REJEITADA";
        String desc = preferenciaGuardada.getTipo() != null ? preferenciaGuardada.getTipo() : "?";
        String detalhe = "Preferência #" + preferenciaGuardada.getId() + " (" + desc + ")"
                + (idFuncionario != null ? " de colaborador #" + idFuncionario : "")
                + (decisaoRecebida != null && !decisaoRecebida.isBlank() ? " — " + decisaoRecebida : "");
        auditoriaService.registar(tipoEvento, idUtilizadorAprovador, detalhe);

        return preferenciaGuardada;
    }

    private String normalizarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Seleciona um tipo de preferencia.");
        }

        String tipoNormalizado = tipo.trim().toLowerCase();
        if (!TIPOS_VALIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException("O tipo de preferencia selecionado e invalido.");
        }

        return tipoNormalizado;
    }

    private String normalizarDescricao(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("A descricao da preferencia e obrigatoria.");
        }

        String descricaoNormalizada = descricao.trim();
        if (descricaoNormalizada.length() < 5) {
            throw new IllegalArgumentException("A descricao deve ter pelo menos 5 caracteres.");
        }

        if (descricaoNormalizada.length() > 1000) {
            throw new IllegalArgumentException("A descricao nao pode ter mais de 1000 caracteres.");
        }

        return descricaoNormalizada;
    }

    private String normalizarDecisao(String decisao) {
        if (decisao == null) {
            return null;
        }

        String decisaoNormalizada = decisao.trim();
        if (decisaoNormalizada.isEmpty()) {
            return null;
        }

        if (decisaoNormalizada.length() > 1000) {
            throw new IllegalArgumentException("A observacao da decisao nao pode ter mais de 1000 caracteres.");
        }

        return decisaoNormalizada;
    }

    private Integer normalizarPrioridade(Integer prioridade, String tipo) {
        if (prioridade == null) {
            return prioridadePorOmissao(tipo);
        }

        if (prioridade < 1 || prioridade > 5) {
            throw new IllegalArgumentException("A prioridade deve estar entre 1 e 5.");
        }

        return prioridade;
    }

    private Integer prioridadePorOmissao(String tipo) {
        return switch (tipo) {
            case "folgas", "ferias", "folga_preferida" -> 4;
            case "colegas", "turnos" -> 3;
            default -> 3;
        };
    }

    private LocalDate normalizarDataInicio(String tipo, LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null) {
            return dataInicio;
        }

        if (dataFim == null) {
            return LocalDate.now();
        }

        return null;
    }

    private void validarPeriodo(String tipo, LocalDate dataInicio, LocalDate dataFim) {
        if (dataFim != null && dataInicio == null) {
            throw new IllegalArgumentException("Seleciona a data inicial antes da data final.");
        }

        // Uma preferência aplica-se sempre a horários ainda por gerar — datas no passado
        // são sempre inválidas (mesma regra de negócio que os pedidos de folga).
        LocalDate hoje = LocalDate.now();
        if (dataInicio != null && dataInicio.isBefore(hoje)) {
            throw new IllegalArgumentException("A data inicial da preferencia nao pode estar no passado.");
        }
        if (dataFim != null && dataFim.isBefore(hoje)) {
            throw new IllegalArgumentException("A data final da preferencia nao pode estar no passado.");
        }

        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        if (("folgas".equals(tipo) || "ferias".equals(tipo) || "folga_preferida".equals(tipo)) && dataInicio == null) {
            throw new IllegalArgumentException("As preferencias de folgas e ferias precisam de pelo menos uma data inicial.");
        }
    }

    /**
     * Preferencias com data (folgas/ferias/folga_preferida) só fazem sentido para um
     * colaborador com um registo de loja ativo — sem isso não há horário a influenciar.
     */
    private void validarRegistoOperacional(Integer idUtilizador, String tipo) {
        boolean exigeRegistoAtivo = "folgas".equals(tipo) || "ferias".equals(tipo) || "folga_preferida".equals(tipo);
        if (!exigeRegistoAtivo) {
            return;
        }

        if (lojautilizadorHelper.findLigacaoAtiva(idUtilizador).isEmpty()) {
            throw new IllegalArgumentException(
                    "Nao tens nenhuma loja ativa associada; nao e possivel registar esta preferencia.");
        }
    }

    private boolean existePreferenciaDuplicada(Integer idUtilizador,
                                               String tipo,
                                               String descricao,
                                               Integer prioridade,
                                               LocalDate dataInicio,
                                               LocalDate dataFim,
                                               Integer idIgnorado) {
        return preferenciaRepository.existsPreferenciaDuplicada(
                idUtilizador, tipo, descricao, prioridade, dataInicio, dataFim, idIgnorado);
    }

    private String formatarTipoParaMensagem(String tipo) {
        if (tipo == null) return "preferência";
        return switch (tipo) {
            case "folga_preferida" -> "folga preferida semanal";
            case "colegas"         -> "colegas preferidos";
            case "turnos"          -> "turnos preferidos";
            case "folgas"          -> "folgas";
            case "ferias"          -> "férias";
            default                -> tipo;
        };
    }

    private boolean preferenciaPodeSerEditada(Preferencia preferencia) {
        return preferencia != null
                && (preferencia.getEstado() == null
                || preferencia.getEstado().isBlank()
                || "pendente".equalsIgnoreCase(preferencia.getEstado()));
    }
}
