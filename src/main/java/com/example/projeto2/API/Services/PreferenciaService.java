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

    private static final Set<String> TIPOS_VALIDOS = Set.of("folgas", "ferias", "colegas", "turnos");
    private static final Set<String> ESTADOS_VALIDOS = Set.of("pendente", "aprovado", "rejeitado");
    private static final Set<String> CARGOS_COM_APROVACAO = Set.of("gerente", "subgerente");

    private final PreferenciaRepository preferenciaRepository;
    private final UtilizadorRepository utilizadorRepository;
    private final LojautilizadorRepository lojautilizadorRepository;

    public PreferenciaService(PreferenciaRepository preferenciaRepository,
                          UtilizadorRepository utilizadorRepository,
                          LojautilizadorRepository lojautilizadorRepository) {
        this.preferenciaRepository = preferenciaRepository;
        this.utilizadorRepository = utilizadorRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
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
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo != null
                        && cargo.getTipo() != null
                        && CARGOS_COM_APROVACAO.contains(cargo.getTipo().toLowerCase()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarPreferenciasPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoAprovacao(ligacaoAtiva);

        return preferenciaRepository.findPreferenciasPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    @Transactional(readOnly = true)
    public int contarPendentesParaAprovacao(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .filter(lu -> lu.getIdCargo() != null
                        && lu.getIdCargo().getTipo() != null
                        && CARGOS_COM_APROVACAO.contains(lu.getIdCargo().getTipo().toLowerCase()))
                .map(lu -> (int) preferenciaRepository.countPreferenciasPendentesDaLoja(
                        lu.getIdLoja().getId(), idUtilizador))
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public List<Preferencia> listarHistoricoDecisoesDaLoja(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoAprovacao(ligacaoAtiva);

        return preferenciaRepository.findHistoricoDecisoesDaLoja(ligacaoAtiva.getIdLoja().getId());
    }

    @Transactional(readOnly = true)
    public List<String> listarColegasDaLoja(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizador);

        return lojautilizadorRepository.findByIdLojaWithUtilizadorCargo(ligacaoAtiva.getIdLoja().getId()).stream()
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

            if (!preferenciaPodeSerEditada(preferenciaPersistida)) {
                throw new IllegalArgumentException("So podes editar preferencias pendentes.");
            }
        } else {
            preferenciaPersistida = new Preferencia();
            preferenciaPersistida.setIdUtilizador(utilizador);
        }

        String tipoNormalizado = normalizarTipo(preferenciaRecebida.getTipo());
        String descricaoNormalizada = normalizarDescricao(preferenciaRecebida.getDescricao());
        Integer prioridadeNormalizada = normalizarPrioridade(preferenciaRecebida.getPrioridade(), tipoNormalizado);
        LocalDate dataInicio = normalizarDataInicio(
                tipoNormalizado,
                preferenciaRecebida.getDataInicio(),
                preferenciaRecebida.getDataFim()
        );
        LocalDate dataFim = preferenciaRecebida.getDataFim();

        validarPeriodo(tipoNormalizado, dataInicio, dataFim);

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

        return preferenciaRepository.save(preferenciaPersistida);
    }

    @Transactional
    public void removerPreferencia(Integer idUtilizador, Integer idPreferencia) {
        Preferencia preferencia = obterPreferenciaDoUtilizador(idUtilizador, idPreferencia);

        if (!preferenciaPodeSerEditada(preferencia)) {
            throw new IllegalArgumentException("So podes remover preferencias pendentes.");
        }

        preferenciaRepository.delete(preferencia);
    }

    @Transactional
    public Preferencia aprovarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador, String decisao) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, "aprovado", decisao);
    }

    @Transactional
    public Preferencia rejeitarPreferencia(Integer idPreferencia, Integer idUtilizadorAprovador, String decisao) {
        return decidirPreferencia(idPreferencia, idUtilizadorAprovador, "rejeitado", decisao);
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

    private Lojautilizador obterLigacaoAtiva(Integer idUtilizador) {
        validarUtilizador(idUtilizador);

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador."));
    }

    private void validarPermissaoAprovacao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        if (tipoCargo == null || !CARGOS_COM_APROVACAO.contains(tipoCargo.toLowerCase())) {
            throw new IllegalArgumentException("Nao tens permissao para aprovar preferencias.");
        }
    }

    private Preferencia decidirPreferencia(Integer idPreferencia,
                                           Integer idUtilizadorAprovador,
                                           String novoEstado,
                                           String decisaoRecebida) {
        if (idPreferencia == null) {
            throw new IllegalArgumentException("A preferencia selecionada e obrigatoria.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoAprovacao(ligacaoAtiva);

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

        return preferenciaRepository.save(preferencia);
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
            case "folgas", "ferias" -> 4;
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

        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final nao pode ser anterior a data inicial.");
        }

        if (("folgas".equals(tipo) || "ferias".equals(tipo)) && dataInicio == null) {
            throw new IllegalArgumentException("As preferencias de folgas e ferias precisam de pelo menos uma data inicial.");
        }
    }

    private boolean existePreferenciaDuplicada(Integer idUtilizador,
                                               String tipo,
                                               String descricao,
                                               Integer prioridade,
                                               LocalDate dataInicio,
                                               LocalDate dataFim,
                                               Integer idIgnorado) {
        return preferenciaRepository.findByIdUtilizadorIdOrderByDataInicioAscIdDesc(idUtilizador).stream()
                .filter(preferencia -> idIgnorado == null || !Objects.equals(preferencia.getId(), idIgnorado))
                .anyMatch(preferencia ->
                        equalsIgnoreCase(preferencia.getTipo(), tipo)
                                && equalsIgnoreCase(preferencia.getDescricao(), descricao)
                                && Objects.equals(preferencia.getPrioridade(), prioridade)
                                && Objects.equals(preferencia.getDataInicio(), dataInicio)
                                && Objects.equals(preferencia.getDataFim(), dataFim)
                );
    }

    private boolean equalsIgnoreCase(String valor1, String valor2) {
        if (valor1 == null || valor2 == null) {
            return Objects.equals(valor1, valor2);
        }
        return valor1.equalsIgnoreCase(valor2);
    }

    private boolean preferenciaPodeSerEditada(Preferencia preferencia) {
        return preferencia != null
                && (preferencia.getEstado() == null
                || preferencia.getEstado().isBlank()
                || "pendente".equalsIgnoreCase(preferencia.getEstado()));
    }
}
