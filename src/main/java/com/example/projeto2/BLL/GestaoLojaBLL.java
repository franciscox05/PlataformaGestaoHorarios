package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Loja;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Regra;
import com.example.projeto2.Modules.RegrasLoja;
import com.example.projeto2.Repositories.LojaRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.RegraRepository;
import com.example.projeto2.Repositories.RegrasLojaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GestaoLojaBLL {

    private static final Set<String> CARGOS_COM_GESTAO_LOJA = Set.of("gerente", "subgerente");
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final LojautilizadorRepository lojautilizadorRepository;
    private final LojaRepository lojaRepository;
    private final RegraRepository regraRepository;
    private final RegrasLojaRepository regrasLojaRepository;

    public GestaoLojaBLL(LojautilizadorRepository lojautilizadorRepository,
                         LojaRepository lojaRepository,
                         RegraRepository regraRepository,
                         RegrasLojaRepository regrasLojaRepository) {
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.lojaRepository = lojaRepository;
        this.regraRepository = regraRepository;
        this.regrasLojaRepository = regrasLojaRepository;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeGerirLoja(Integer idUtilizador) {
        if (idUtilizador == null) {
            return false;
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getTipo() != null ? cargo.getTipo().toLowerCase() : "")
                .filter(CARGOS_COM_GESTAO_LOJA::contains)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public GestaoLojaResumo obterResumo(Integer idUtilizador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();

        Map<Integer, RegrasLoja> regrasEspecificas = new LinkedHashMap<>();
        for (RegrasLoja regraLoja : regrasLojaRepository.findByIdLojaWithRegraOrderByDescricao(loja.getId())) {
            if (regraLoja.getIdRegra() != null && regraLoja.getIdRegra().getId() != null) {
                regrasEspecificas.put(regraLoja.getIdRegra().getId(), regraLoja);
            }
        }

        List<RegraLojaResumo> regras = regraRepository.findAllByOrderByDescricaoAsc().stream()
                .sorted(Comparator.comparing(
                        regra -> regra.getDescricao() != null ? regra.getDescricao() : "",
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(regra -> criarResumoRegra(regra, regrasEspecificas.get(regra.getId())))
                .toList();

        return new GestaoLojaResumo(
                loja.getId(),
                valorOuTraco(loja.getNome()),
                valorOuTraco(loja.getLocalizacao()),
                formatarHora(loja.getHoraAbertura()),
                formatarHora(loja.getHoraFecho()),
                ligacaoAtiva.getIdCargo() != null ? valorOuTraco(ligacaoAtiva.getIdCargo().getNome()) : "-",
                regras
        );
    }

    @Transactional
    public void guardarConfiguracao(Integer idUtilizador, ConfiguracaoLojaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A configuracao da loja e obrigatoria.");
        }

        if (request.horaAbertura() == null || request.horaFecho() == null) {
            throw new IllegalArgumentException("As horas de abertura e fecho sao obrigatorias.");
        }

        if (!request.horaAbertura().isBefore(request.horaFecho())) {
            throw new IllegalArgumentException("A hora de abertura deve ser anterior a hora de fecho.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizador);
        Loja loja = ligacaoAtiva.getIdLoja();

        loja.setHoraAbertura(request.horaAbertura());
        loja.setHoraFecho(request.horaFecho());
        lojaRepository.save(loja);

        if (request.regras() == null) {
            return;
        }

        for (ConfiguracaoRegraRequest regraRequest : request.regras()) {
            if (regraRequest == null || regraRequest.idRegra() == null) {
                continue;
            }

            Regra regra = regraRepository.findById(regraRequest.idRegra())
                    .orElseThrow(() -> new IllegalArgumentException("Foi encontrada uma regra invalida no formulario."));

            Integer valorEspecifico = regraRequest.valorEspecifico();
            if (valorEspecifico != null && valorEspecifico < 0) {
                throw new IllegalArgumentException("Os valores especificos das regras nao podem ser negativos.");
            }

            String observacoes = limparTexto(regraRequest.observacoes());
            Optional<RegrasLoja> regraExistente = regrasLojaRepository.findByIdLojaIdAndIdRegraId(loja.getId(), regra.getId());

            if (valorEspecifico == null && observacoes == null) {
                regraExistente.ifPresent(regrasLojaRepository::delete);
                continue;
            }

            RegrasLoja regraLoja = regraExistente.orElseGet(RegrasLoja::new);
            regraLoja.setIdLoja(loja);
            regraLoja.setIdRegra(regra);
            regraLoja.setValorEspecifico(valorEspecifico);
            regraLoja.setObservacoes(observacoes);
            regrasLojaRepository.save(regraLoja);
        }
    }

    private Lojautilizador obterLigacaoAtivaComPermissao(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa a uma loja."));

        String tipoCargo = ligacaoAtiva.getIdCargo() != null && ligacaoAtiva.getIdCargo().getTipo() != null
                ? ligacaoAtiva.getIdCargo().getTipo().toLowerCase()
                : "";

        if (!CARGOS_COM_GESTAO_LOJA.contains(tipoCargo)) {
            throw new IllegalArgumentException("Nao tens permissao para gerir a configuracao da loja.");
        }

        return ligacaoAtiva;
    }

    private RegraLojaResumo criarResumoRegra(Regra regra, RegrasLoja regraLoja) {
        return new RegraLojaResumo(
                regra.getId(),
                valorOuTraco(regra.getDescricao()),
                regra.getTipo(),
                regra.getValorPadrao(),
                regraLoja != null ? regraLoja.getValorEspecifico() : null,
                regraLoja != null ? regraLoja.getObservacoes() : null
        );
    }

    private String limparTexto(String texto) {
        if (texto == null) {
            return null;
        }

        String textoLimpo = texto.trim();
        return textoLimpo.isEmpty() ? null : textoLimpo;
    }

    private String formatarHora(LocalTime hora) {
        if (hora == null) {
            return "";
        }

        return hora.format(HORA_FORMATTER);
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    public record GestaoLojaResumo(
            Integer idLoja,
            String nomeLoja,
            String localizacao,
            String horaAbertura,
            String horaFecho,
            String cargoGestor,
            List<RegraLojaResumo> regras
    ) {
    }

    public record RegraLojaResumo(
            Integer idRegra,
            String descricao,
            String tipo,
            Integer valorPadrao,
            Integer valorEspecifico,
            String observacoes
    ) {
    }

    public record ConfiguracaoLojaRequest(
            LocalTime horaAbertura,
            LocalTime horaFecho,
            List<ConfiguracaoRegraRequest> regras
    ) {
    }

    public record ConfiguracaoRegraRequest(
            Integer idRegra,
            Integer valorEspecifico,
            String observacoes
    ) {
    }
}
