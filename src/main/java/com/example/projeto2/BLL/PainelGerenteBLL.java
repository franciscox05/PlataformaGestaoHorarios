package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Modules.Preferencia;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PainelGerenteBLL {

    private static final Set<String> CARGOS_COM_PAINEL = Set.of("gerente", "subgerente");

    private final DayOffBLL dayOffBLL;
    private final PermutaBLL permutaBLL;
    private final PreferenciaBLL preferenciaBLL;
    private final SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL;
    private final LojautilizadorRepository lojautilizadorRepository;

    public PainelGerenteBLL(DayOffBLL dayOffBLL,
                            PermutaBLL permutaBLL,
                            PreferenciaBLL preferenciaBLL,
                            SnapshotOperacionalLojaBLL snapshotOperacionalLojaBLL,
                            LojautilizadorRepository lojautilizadorRepository) {
        this.dayOffBLL = dayOffBLL;
        this.permutaBLL = permutaBLL;
        this.preferenciaBLL = preferenciaBLL;
        this.snapshotOperacionalLojaBLL = snapshotOperacionalLojaBLL;
        this.lojautilizadorRepository = lojautilizadorRepository;
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAcederPainel(Integer idUtilizador) {
        if (idUtilizador == null) {
            return false;
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo != null && cargo.getTipo() != null
                        ? cargo.getTipo().toLowerCase()
                        : "")
                .filter(CARGOS_COM_PAINEL::contains)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public PainelGerenteSnapshot carregarPainel(Integer idUtilizadorGestor) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtivaComPermissao(idUtilizadorGestor);

        List<DayOff> folgasPendentes = dayOffBLL.listarPedidosPendentesParaAprovacao(idUtilizadorGestor);
        List<Permuta> permutasPendentes = permutaBLL.listarPedidosPendentesParaAprovacao(idUtilizadorGestor);
        List<Preferencia> preferenciasPendentes = preferenciaBLL.listarPreferenciasPendentesParaAprovacao(idUtilizadorGestor);

        Map<Integer, String> nomesFolgas = dayOffBLL.listarNomesUtilizadores(
                folgasPendentes.stream()
                        .map(d -> d.getIdUtilizador().getId())
                        .collect(Collectors.toSet())
        );

        ContextoPainel contexto = new ContextoPainel(
                ligacaoAtiva.getIdLoja() != null ? valorOuTraco(ligacaoAtiva.getIdLoja().getNome()) : "-",
                ligacaoAtiva.getIdLoja() != null ? valorOuTraco(ligacaoAtiva.getIdLoja().getLocalizacao()) : "-",
                ligacaoAtiva.getIdCargo() != null ? valorOuTraco(ligacaoAtiva.getIdCargo().getNome()) : "-"
        );

        ResumoPainel resumo = new ResumoPainel(
                folgasPendentes.size() + permutasPendentes.size() + preferenciasPendentes.size(),
                folgasPendentes.size(),
                permutasPendentes.size(),
                preferenciasPendentes.size()
        );

        return new PainelGerenteSnapshot(
                contexto,
                resumo,
                folgasPendentes,
                nomesFolgas,
                permutasPendentes,
                preferenciasPendentes
        );
    }

    @Transactional(readOnly = true)
    public SnapshotOperacionalLojaBLL.SnapshotOperacionalLoja carregarSnapshotOperacionalHoje(Integer idUtilizadorGestor) {
        validarAcesso(idUtilizadorGestor);
        return snapshotOperacionalLojaBLL.carregarSnapshot(idUtilizadorGestor, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public SnapshotOperacionalLojaBLL.ContextoPedidoOperacional carregarContextoPedido(Integer idUtilizadorGestor,
                                                                                       SnapshotOperacionalLojaBLL.TipoPedidoOperacional tipoPedido,
                                                                                       Integer idPedido) {
        validarAcesso(idUtilizadorGestor);
        return snapshotOperacionalLojaBLL.carregarContextoPedido(idUtilizadorGestor, tipoPedido, idPedido);
    }

    @Transactional
    public void aprovarFolga(Integer idPedido, Integer idUtilizadorGestor) {
        validarAcesso(idUtilizadorGestor);
        dayOffBLL.aprovarPedidoFolga(idPedido, idUtilizadorGestor);
    }

    @Transactional
    public void rejeitarFolga(Integer idPedido, Integer idUtilizadorGestor) {
        validarAcesso(idUtilizadorGestor);
        dayOffBLL.rejeitarPedidoFolga(idPedido, idUtilizadorGestor);
    }

    @Transactional
    public void aprovarPermuta(Integer idPermuta, Integer idUtilizadorGestor) {
        validarAcesso(idUtilizadorGestor);
        permutaBLL.aprovarPedidoPermuta(idPermuta, idUtilizadorGestor);
    }

    @Transactional
    public void rejeitarPermuta(Integer idPermuta, Integer idUtilizadorGestor) {
        validarAcesso(idUtilizadorGestor);
        permutaBLL.rejeitarPedidoPermuta(idPermuta, idUtilizadorGestor);
    }

    @Transactional
    public void aprovarPreferencia(Integer idPreferencia, Integer idUtilizadorGestor, String decisao) {
        validarAcesso(idUtilizadorGestor);
        preferenciaBLL.aprovarPreferencia(idPreferencia, idUtilizadorGestor, decisao);
    }

    @Transactional
    public void rejeitarPreferencia(Integer idPreferencia, Integer idUtilizadorGestor, String decisao) {
        validarAcesso(idUtilizadorGestor);
        preferenciaBLL.rejeitarPreferencia(idPreferencia, idUtilizadorGestor, decisao);
    }

    private void validarAcesso(Integer idUtilizadorGestor) {
        if (!utilizadorPodeAcederPainel(idUtilizadorGestor)) {
            throw new IllegalArgumentException("Nao tens permissao para usar este painel.");
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

        if (!CARGOS_COM_PAINEL.contains(tipoCargo)) {
            throw new IllegalArgumentException("Nao tens permissao para aceder ao painel do gerente.");
        }

        return ligacaoAtiva;
    }

    private String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    public record ContextoPainel(
            String nomeLoja,
            String localizacao,
            String cargoGestao
    ) {
    }

    public record ResumoPainel(
            int totalPendentes,
            int folgasPendentes,
            int permutasPendentes,
            int preferenciasPendentes
    ) {
    }

    public record PainelGerenteSnapshot(
            ContextoPainel contexto,
            ResumoPainel resumo,
            List<DayOff> folgasPendentes,
            Map<Integer, String> nomesFolgasPendentes,
            List<Permuta> permutasPendentes,
            List<Preferencia> preferenciasPendentes
    ) {
    }
}
