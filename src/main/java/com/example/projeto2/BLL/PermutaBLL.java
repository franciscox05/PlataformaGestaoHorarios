package com.example.projeto2.BLL;

import com.example.projeto2.Enums.EstadoPermuta;
import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Lojautilizador;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Repositories.HorarioRepository;
import com.example.projeto2.Repositories.LojautilizadorRepository;
import com.example.projeto2.Repositories.PermutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class PermutaBLL {

    private static final Set<String> CARGOS_COM_APROVACAO = Set.of("gerente", "subgerente", "supervisor");

    private final PermutaRepository permutaRepository;
    private final LojautilizadorRepository lojautilizadorRepository;
    private final HorarioRepository horarioRepository;

    public PermutaBLL(PermutaRepository permutaRepository,
                      LojautilizadorRepository lojautilizadorRepository,
                      HorarioRepository horarioRepository) {
        this.permutaRepository = permutaRepository;
        this.lojautilizadorRepository = lojautilizadorRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional
    public Permuta registarPedidoTroca(Integer idUtilizadorLogado, Horario meuTurno, Horario turnoColega) {
        validarPedido(idUtilizadorLogado, meuTurno, turnoColega);

        Permuta novaPermuta = new Permuta();
        novaPermuta.setIdHorarioOrigem(meuTurno);
        novaPermuta.setIdHorarioDestino(turnoColega);
        novaPermuta.setEstado(EstadoPermuta.pendente);
        novaPermuta.setDataPedido(Instant.now());

        return permutaRepository.save(novaPermuta);
    }

    @Transactional(readOnly = true)
    public List<Permuta> listarPedidosEnviados(Integer idUtilizadorLogado) {
        if (idUtilizadorLogado == null) {
            return List.of();
        }

        return permutaRepository.findPedidosEnviadosPorUtilizador(idUtilizadorLogado).stream()
                .sorted(Comparator
                        .comparing(Permuta::getDataPedido, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Permuta::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean utilizadorPodeAprovarPermutas(Integer idUtilizador) {
        return lojautilizadorRepository.findLigacaoAtivaByIdUtilizador(idUtilizador)
                .map(Lojautilizador::getIdCargo)
                .map(cargo -> cargo.getTipo() != null && CARGOS_COM_APROVACAO.contains(cargo.getTipo().toLowerCase()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Permuta> listarPedidosPendentesParaAprovacao(Integer idUtilizadorAprovador) {
        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

        return permutaRepository.findPedidosPendentesDaLoja(
                ligacaoAtiva.getIdLoja().getId(),
                idUtilizadorAprovador
        );
    }

    @Transactional
    public Permuta aprovarPedidoPermuta(Integer idPermuta, Integer idUtilizadorAprovador) {
        Permuta pedido = obterPedidoPendenteGerivel(idPermuta, idUtilizadorAprovador);

        Horario horarioOrigem = pedido.getIdHorarioOrigem();
        Horario horarioDestino = pedido.getIdHorarioDestino();

        var turnoOrigem = horarioOrigem.getIdTurno();
        horarioOrigem.setIdTurno(horarioDestino.getIdTurno());
        horarioDestino.setIdTurno(turnoOrigem);

        horarioRepository.save(horarioOrigem);
        horarioRepository.save(horarioDestino);

        pedido.setEstado(EstadoPermuta.aprovado);
        Permuta pedidoAprovado = permutaRepository.save(pedido);

        List<Permuta> conflitos = permutaRepository.findPedidosPendentesConflitantes(
                pedido.getId(),
                Set.of(horarioOrigem.getId(), horarioDestino.getId())
        );
        conflitos.forEach(conflicto -> conflicto.setEstado(EstadoPermuta.rejeitado));
        permutaRepository.saveAll(conflitos);

        return pedidoAprovado;
    }

    @Transactional
    public Permuta rejeitarPedidoPermuta(Integer idPermuta, Integer idUtilizadorAprovador) {
        Permuta pedido = obterPedidoPendenteGerivel(idPermuta, idUtilizadorAprovador);
        pedido.setEstado(EstadoPermuta.rejeitado);
        return permutaRepository.save(pedido);
    }

    private void validarPedido(Integer idUtilizadorLogado, Horario meuTurno, Horario turnoColega) {
        if (idUtilizadorLogado == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar o utilizador autenticado.");
        }

        if (meuTurno == null || turnoColega == null) {
            throw new IllegalArgumentException("Seleciona o teu turno e o turno do colega.");
        }

        if (meuTurno.getId() == null || turnoColega.getId() == null) {
            throw new IllegalArgumentException("Os turnos selecionados nao sao validos.");
        }

        if (meuTurno.getId().equals(turnoColega.getId())) {
            throw new IllegalArgumentException("Nao podes pedir permuta do mesmo turno.");
        }

        Integer idDonoTurnoOrigem = meuTurno.getIdLojautilizador().getIdUtilizador().getId();
        Integer idDonoTurnoDestino = turnoColega.getIdLojautilizador().getIdUtilizador().getId();

        if (!idUtilizadorLogado.equals(idDonoTurnoOrigem)) {
            throw new IllegalArgumentException("O turno de origem tem de pertencer ao utilizador autenticado.");
        }

        if (idUtilizadorLogado.equals(idDonoTurnoDestino)) {
            throw new IllegalArgumentException("O turno de destino tem de pertencer a outro colaborador.");
        }

        if (meuTurno.getDataTurno() == null || turnoColega.getDataTurno() == null
                || !meuTurno.getDataTurno().equals(turnoColega.getDataTurno())) {
            throw new IllegalArgumentException("A permuta so pode ser feita com turnos do mesmo dia.");
        }

        validarAntecedenciaMinima(meuTurno, turnoColega);

        Integer idLojaOrigem = meuTurno.getIdLojautilizador().getIdLoja().getId();
        Integer idLojaDestino = turnoColega.getIdLojautilizador().getIdLoja().getId();

        if (idLojaOrigem == null || !idLojaOrigem.equals(idLojaDestino)) {
            throw new IllegalArgumentException("A permuta so pode ser feita com turnos da mesma loja.");
        }

        if (permutaRepository.existsPedidoPendentePorOrigemEDestino(meuTurno.getId(), turnoColega.getId())) {
            throw new IllegalArgumentException("Ja existe um pedido pendente para esta combinacao de turnos.");
        }

        if (permutaRepository.existsPedidoPendentePorHorario(meuTurno.getId())
                || permutaRepository.existsPedidoPendentePorHorario(turnoColega.getId())) {
            throw new IllegalArgumentException("Um dos turnos selecionados ja esta envolvido num pedido pendente.");
        }
    }

    private void validarAntecedenciaMinima(Horario meuTurno, Horario turnoColega) {
        LocalDateTime limiteMinimo = LocalDateTime.now().plusHours(24);

        boolean origemSemAntecedencia = inicioDoTurno(meuTurno).isBefore(limiteMinimo);
        boolean destinoSemAntecedencia = inicioDoTurno(turnoColega).isBefore(limiteMinimo);

        if (origemSemAntecedencia || destinoSemAntecedencia) {
            throw new IllegalArgumentException("As permutas precisam de ser pedidas com pelo menos 24 horas de antecedencia.");
        }
    }

    private LocalDateTime inicioDoTurno(Horario horario) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null
                || horario.getIdTurno().getHoraInicio() == null) {
            throw new IllegalArgumentException("Nao foi possivel identificar a hora de inicio dos turnos selecionados.");
        }

        return LocalDateTime.of(horario.getDataTurno(), horario.getIdTurno().getHoraInicio());
    }

    private Permuta obterPedidoPendenteGerivel(Integer idPermuta, Integer idUtilizadorAprovador) {
        if (idPermuta == null) {
            throw new IllegalArgumentException("O pedido de permuta selecionado e obrigatorio.");
        }

        Lojautilizador ligacaoAtiva = obterLigacaoAtiva(idUtilizadorAprovador);
        validarPermissaoDeAprovacao(ligacaoAtiva);

        Permuta pedido = permutaRepository.findDetalhadaById(idPermuta)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de permuta nao encontrado."));

        if (pedido.getEstado() != EstadoPermuta.pendente) {
            throw new IllegalArgumentException("Este pedido de permuta ja foi tratado.");
        }

        Integer idLojaAprovador = ligacaoAtiva.getIdLoja().getId();
        Integer idLojaOrigem = pedido.getIdHorarioOrigem().getIdLojautilizador().getIdLoja().getId();
        Integer idLojaDestino = pedido.getIdHorarioDestino().getIdLojautilizador().getIdLoja().getId();
        Integer idSolicitante = pedido.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getId();

        if (!idLojaAprovador.equals(idLojaOrigem) || !idLojaAprovador.equals(idLojaDestino) || idUtilizadorAprovador.equals(idSolicitante)) {
            throw new IllegalArgumentException("Nao tens permissao para gerir este pedido de permuta.");
        }

        return pedido;
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
            throw new IllegalArgumentException("Este utilizador nao tem permissao para aprovar permutas.");
        }
    }
}
