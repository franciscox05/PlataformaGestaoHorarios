package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Utilizador;
import com.example.projeto2.Repositories.DayOffRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DayOffBLL {

    private static final Set<String> CARGOS_COM_APROVACAO = Set.of("gerente", "subgerente", "supervisor");

    private final DayOffRepository dayOffRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final UtilizadorRepository utilizadorRepository;

    public DayOffBLL(DayOffRepository dayOffRepository,
                     LojautilizadorRepository lojautilizadorRepository,
                     UtilizadorRepository utilizadorRepository) {
        this.dayOffRepository = dayOffRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.utilizadorRepository = utilizadorRepository;
    }

    @Transactional
    public DayOff registarPedidoFolga(DayOff pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("O pedido de folga nao pode ser nulo.");
        }

        if (pedido.getIdUtilizador() == null) {
            throw new IllegalArgumentException("O utilizador do pedido e obrigatorio.");
        }

        if (pedido.getDataAusencia() == null) {
            throw new IllegalArgumentException("A data de ausencia e obrigatoria.");
        }

        if (pedido.getDataAusencia().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de ausencia nao pode estar no passado.");
        }

        if (pedido.getTipo() == null || pedido.getTipo().isBlank()) {
            throw new IllegalArgumentException("O tipo de ausencia e obrigatorio.");
        }

        if (pedido.getMotivo() != null && pedido.getMotivo().isBlank()) {
            pedido.setMotivo(null);
        }

        pedido.setEstado("pendente");

        return dayOffRepository.save(pedido);
    }

    @Transactional(readOnly = true)
    public List<DayOff> listarPedidosPorUtilizador(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O id do utilizador e obrigatorio.");
        }

        return dayOffRepository.findByIdUtilizador(idUtilizador).stream()
                .sorted(Comparator
                        .comparing(DayOff::getDataAusencia, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DayOff::getIdDayoff, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAprovarFolgas(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getTipo() != null && CARGOS_COM_APROVACAO.contains(cargo.getTipo().toLowerCase()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<DayOff> listarPedidosPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

        return dayOffRepository.findPedidosPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    @Transactional(readOnly = true)
    public Map<Integer, String> listarNomesUtilizadores(Collection<Integer> idsUtilizadores) {
        if (idsUtilizadores == null || idsUtilizadores.isEmpty()) {
            return Map.of();
        }

        return utilizadorRepository.findAllById(idsUtilizadores).stream()
                .collect(Collectors.toMap(Utilizador::getId, Utilizador::getNome, (nome1, nome2) -> nome1));
    }

    @Transactional
    public DayOff aprovarPedidoFolga(Integer idDayOff, Integer idUtilizadorAprovador) {
        return atualizarEstadoPedido(idDayOff, idUtilizadorAprovador, "aprovado");
    }

    @Transactional
    public DayOff rejeitarPedidoFolga(Integer idDayOff, Integer idUtilizadorAprovador) {
        return atualizarEstadoPedido(idDayOff, idUtilizadorAprovador, "rejeitado");
    }

    private DayOff atualizarEstadoPedido(Integer idDayOff, Integer idUtilizadorAprovador, String novoEstado) {
        if (idDayOff == null) {
            throw new IllegalArgumentException("O pedido selecionado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

        DayOff pedido = dayOffRepository.findById(idDayOff)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de folga nao encontrado."));

        if (!"pendente".equalsIgnoreCase(pedido.getEstado())) {
            throw new IllegalArgumentException("Este pedido ja foi tratado.");
        }

        boolean pedidoVisivelAoAprovador = dayOffRepository.findPedidosPendentesDaLoja(
                        ligacaoAtiva.getIdLoja().getId(),
                        idUtilizadorAprovador)
                .stream()
                .anyMatch(dayOff -> dayOff.getIdDayoff().equals(idDayOff));

        if (!pedidoVisivelAoAprovador) {
            throw new IllegalArgumentException("Nao tens permissao para gerir este pedido.");
        }

        pedido.setEstado(novoEstado);
        return dayOffRepository.save(pedido);
    }

    private Lojautilizador obterLigacaoAtiva(Integer idUtilizador) {
        if (idUtilizador == null) {
            throw new IllegalArgumentException("O utilizador autenticado e obrigatorio.");
        }

        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .orElseThrow(() -> new IllegalArgumentException("Nao foi encontrada uma ligacao ativa para este utilizador."));
    }

    private void validarPermissaoDeAprovacao(Lojautilizador ligacaoAtiva) {
        String tipoCargo = ligacaoAtiva.getIdCargo() != null ? ligacaoAtiva.getIdCargo().getTipo() : null;
        if (tipoCargo == null || !CARGOS_COM_APROVACAO.contains(tipoCargo.toLowerCase())) {
            throw new IllegalArgumentException("Este utilizador nao tem permissao para aprovar folgas.");
        }
    }
}
