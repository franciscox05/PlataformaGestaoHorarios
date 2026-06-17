package com.example.projeto2.API.Services;

import com.example.projeto2.API.Enums.EstadoUtilizador;
import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Utilitário central para obter e validar a ligação activa de um utilizador a uma loja.
 * Elimina a duplicação do padrão findLigacaoAtiva + verificação de cargo que existia
 * em 10 services distintos.
 */
@Component
public class LojautilizadorHelper {

    /** Cargos com poder de aprovação de folgas e permutas (gerente/subgerente/supervisor). */
    public static final Set<String> APROVACAO = Set.of("gerente", "subgerente", "supervisor");

    /** Cargos de gestão geral da loja (gerente/subgerente). */
    public static final Set<String> GESTAO = Set.of("gerente", "subgerente");

    /** Cargo responsável pela validação de propostas de horário. */
    public static final Set<String> VALIDACAO = Set.of("supervisor");

    private final LojautilizadorRepository lojautilizadorRepository;

    public LojautilizadorHelper(LojautilizadorRepository lojautilizadorRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
    }

    /**
     * Devolve a ligação activa do utilizador, ou {@link Optional#empty()} se idUtilizador for null
     * ou se não houver ligação activa. Para utilizadores multi-loja, devolve a primeira ligação
     * activa (ordenada por nome da loja ASC, dataInicio DESC).
     */
    public Optional<Lojautilizador> findLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            return Optional.empty();
        }
        List<Lojautilizador> ligacoes = lojautilizadorRepository.findLigacoesAtivasByIdUtilizador(idUtilizador);
        return ligacoes.isEmpty() ? Optional.empty() : Optional.of(ligacoes.get(0));
    }

    /**
     * Devolve a ligação activa do utilizador. Lança {@link IllegalArgumentException} se
     * idUtilizador for null ou se não houver ligação activa.
     */
    public Lojautilizador obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }
        List<Lojautilizador> ligacoes = lojautilizadorRepository.findLigacoesAtivasByIdUtilizador(idUtilizador);
        if (ligacoes.isEmpty()) {
            throw new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador.");
        }
        return ligacoes.get(0);
    }

    /**
     * Devolve a ligação activa filtrada por cargo, ou {@link Optional#empty()} se o utilizador
     * não tiver o cargo necessário em nenhuma loja activa. Seguro para utilizadores multi-loja.
     */
    public Optional<Lojautilizador> findLigacaoAtivaComCargo(Integer idUtilizador,
                                                              Set<String> cargosPermitidos) {
        if (idUtilizador == null) return Optional.empty();
        return lojautilizadorRepository.findLigacoesAtivasByIdUtilizador(idUtilizador)
                .stream().filter(lu -> temCargo(lu, cargosPermitidos)).findFirst();
    }

    /**
     * Devolve a ligação activa do utilizador após validar o cargo. Lança
     * {@link IllegalArgumentException} se não houver ligação activa ou se o cargo não
     * pertencer ao conjunto permitido.
     */
    public Lojautilizador obterLigacaoAtivaComCargo(Integer idUtilizador,
                                                     Set<String> cargosPermitidos,
                                                     String mensagemErro) {
        Lojautilizador ligacao = obterLigacaoAtiva(idUtilizador);
        if (!temCargo(ligacao, cargosPermitidos)) {
            throw new IllegalArgumentException(mensagemErro);
        }
        return ligacao;
    }

    /** true se o utilizador tiver ligação activa com um dos cargos indicados. */
    public boolean temCargo(Integer idUtilizador, Set<String> cargosPermitidos) {
        return findLigacaoAtivaComCargo(idUtilizador, cargosPermitidos).isPresent();
    }

    /** Store-scoped variant — uses idLoja to avoid NonUniqueResultException for multi-store users. */
    public boolean temCargo(Integer idUtilizador, Integer idLoja, Set<String> cargosPermitidos) {
        if (idLoja == null) return temCargo(idUtilizador, cargosPermitidos);
        return findLigacaoAtivaComCargo(idUtilizador, idLoja, cargosPermitidos).isPresent();
    }

    /** true se a ligação fornecida tiver um dos cargos indicados. */
    public boolean temCargo(Lojautilizador ligacao, Set<String> cargosPermitidos) {
        String tipo = ligacao.getIdCargo() != null ? ligacao.getIdCargo().getTipo() : null;
        return tipo != null && cargosPermitidos.contains(tipo.toLowerCase());
    }

    // -------------------------------------------------------------------------
    // Helpers de filtro de colaboradores (usados na geração de horários)
    // -------------------------------------------------------------------------

    /**
     * Verifica se uma ligação loja-utilizador tem relevância no período indicado.
     * Filtra ligações sem dados obrigatórios, que só começam depois do período,
     * ou que já terminaram antes de ele começar.
     */
    public boolean ligacaoTemRelevanciaNoPeriodo(Lojautilizador ligacao,
                                                  LocalDate dataInicio,
                                                  LocalDate dataFim) {
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

    /**
     * Devolve os IDs dos utilizadores com ligação activa à loja nos cargos indicados.
     * Filtra utilizadores inactivos. Resultado sem duplicados.
     */
    public List<Integer> listarIdsComCargoPorLoja(Integer idLoja, Set<String> cargos) {
        if (idLoja == null || cargos == null || cargos.isEmpty()) return List.of();
        return lojautilizadorRepository.findLigacoesAtivasByIdLojaAndCargos(idLoja, cargos)
                .stream()
                .filter(this::utilizadorEstaAtivo)
                .map(lu -> lu.getIdUtilizador().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** true se o utilizador associado à ligação estiver no estado {@code ativo}. */
    public boolean utilizadorEstaAtivo(Lojautilizador ligacao) {
        return ligacao.getIdUtilizador() != null
                && EstadoUtilizador.ativo == ligacao.getIdUtilizador().getEstado();
    }

    /**
     * Merge-function para {@code Map.merge}: devolve a ligação com {@code dataInicio} mais
     * recente; em caso de empate prefere a ligação sem {@code dataFim} (ativa).
     */
    public Lojautilizador preferirLigacaoMaisRecente(Lojautilizador ligacaoAtual,
                                                      Lojautilizador novaLigacao) {
        if (ligacaoAtual == null) return novaLigacao;
        if (novaLigacao == null) return ligacaoAtual;

        LocalDate inicioAtual = ligacaoAtual.getDataInicio();
        LocalDate inicioNovo  = novaLigacao.getDataInicio();
        if (inicioAtual == null) return novaLigacao;
        if (inicioNovo  == null) return ligacaoAtual;
        if (inicioNovo.isAfter(inicioAtual))  return novaLigacao;
        if (inicioAtual.isAfter(inicioNovo))  return ligacaoAtual;

        if (ligacaoAtual.getDataFim() == null && novaLigacao.getDataFim() != null) return ligacaoAtual;
        if (ligacaoAtual.getDataFim() != null && novaLigacao.getDataFim() == null) return novaLigacao;
        return ligacaoAtual;
    }

    // -------------------------------------------------------------------------
    // Store-scoped overloads — safe for multi-store users
    // -------------------------------------------------------------------------

    /**
     * Devolve a ligação activa do utilizador filtrada pela loja indicada.
     * Usa o par (idUtilizador, idLoja) para eliminar a ambiguidade quando o mesmo
     * utilizador tem ligações activas em mais de uma loja.
     */
    public Optional<Lojautilizador> findLigacaoAtiva(Integer idUtilizador, Integer idLoja) {
        if (idUtilizador == null || idLoja == null) return Optional.empty();
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja);
    }

    /**
     * Versão obrigatória do lookup por loja. Lança {@link IllegalArgumentException} se
     * não existir ligação activa do utilizador na loja indicada.
     */
    public Lojautilizador obterLigacaoAtiva(Integer idUtilizador, Integer idLoja) {
        if (idUtilizador == null || idLoja == null) {
            throw new IllegalArgumentException("O utilizador e a loja sao obrigatorios.");
        }
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizadorAndIdLoja(idUtilizador, idLoja)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nao foi encontrada uma ligacao ativa para este utilizador nesta loja."));
    }

    /**
     * Verifica se o utilizador tem um dos cargos indicados na loja especificada.
     * Equivalente a {@link #findLigacaoAtivaComCargo(Integer, Set)} mas com contexto de loja explícito.
     */
    public Optional<Lojautilizador> findLigacaoAtivaComCargo(Integer idUtilizador,
                                                              Integer idLoja,
                                                              Set<String> cargosPermitidos) {
        return findLigacaoAtiva(idUtilizador, idLoja)
                .filter(lu -> temCargo(lu, cargosPermitidos));
    }

    /**
     * Versão store-scoped de {@link #obterLigacaoAtivaComCargo}: valida o cargo
     * estritamente dentro da loja indicada. Cai em {@link #obterLigacaoAtivaComCargo(Integer, Set, String)}
     * quando {@code idLoja} é {@code null} (compatibilidade com sessões de loja única).
     */
    public Lojautilizador obterLigacaoAtivaComCargo(Integer idUtilizador,
                                                     Integer idLoja,
                                                     Set<String> cargosPermitidos,
                                                     String mensagemErro) {
        if (idLoja == null) return obterLigacaoAtivaComCargo(idUtilizador, cargosPermitidos, mensagemErro);
        Lojautilizador ligacao = obterLigacaoAtiva(idUtilizador, idLoja);
        if (!temCargo(ligacao, cargosPermitidos)) {
            throw new IllegalArgumentException(mensagemErro);
        }
        return ligacao;
    }
}
