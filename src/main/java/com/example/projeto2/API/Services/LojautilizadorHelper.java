package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Component;

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
     * ou se não houver ligação activa.
     */
    public Optional<Lojautilizador> findLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            return Optional.empty();
        }
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador);
    }

    /**
     * Devolve a ligação activa do utilizador. Lança {@link IllegalArgumentException} se
     * idUtilizador for null ou se não houver ligação activa.
     */
    public Lojautilizador obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nao foi encontrada uma ligacao ativa para este utilizador."));
    }

    /**
     * Devolve a ligação activa filtrada por cargo, ou {@link Optional#empty()} se o utilizador
     * não tiver o cargo necessário. Útil para contagens condicionais.
     */
    public Optional<Lojautilizador> findLigacaoAtivaComCargo(Integer idUtilizador,
                                                              Set<String> cargosPermitidos) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .filter(lu -> temCargo(lu, cargosPermitidos));
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

    /** true se a ligação fornecida tiver um dos cargos indicados. */
    public boolean temCargo(Lojautilizador ligacao, Set<String> cargosPermitidos) {
        String tipo = ligacao.getIdCargo() != null ? ligacao.getIdCargo().getTipo() : null;
        return tipo != null && cargosPermitidos.contains(tipo.toLowerCase());
    }
}
