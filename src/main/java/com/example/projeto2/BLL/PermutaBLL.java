package com.example.projeto2.BLL;

import com.example.projeto2.Modules.Horario;
import com.example.projeto2.Modules.Permuta;
import com.example.projeto2.Repositories.PermutaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class PermutaBLL {

    private final PermutaRepository permutaRepository;

    public PermutaBLL(PermutaRepository permutaRepository) {
        this.permutaRepository = permutaRepository;
    }

    @Transactional
    public Permuta registarPedidoTroca(Integer idUtilizadorLogado, Horario meuTurno, Horario turnoColega) {
        validarPedido(idUtilizadorLogado, meuTurno, turnoColega);

        Permuta novaPermuta = new Permuta();
        novaPermuta.setIdHorarioOrigem(meuTurno);
        novaPermuta.setIdHorarioDestino(turnoColega);
        novaPermuta.setEstado("pendente");
        novaPermuta.setDataPedido(Instant.now());

        return permutaRepository.save(novaPermuta);
    }

    @Transactional(readOnly = true)
    public List<Permuta> listarPedidosEnviados(Integer idUtilizadorLogado) {
        if (idUtilizadorLogado == null) {
            return List.of();
        }

        return permutaRepository.findPedidosEnviadosPorUtilizador(idUtilizadorLogado);
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

        Integer idLojaOrigem = meuTurno.getIdLojautilizador().getIdLoja().getId();
        Integer idLojaDestino = turnoColega.getIdLojautilizador().getIdLoja().getId();

        if (idLojaOrigem == null || !idLojaOrigem.equals(idLojaDestino)) {
            throw new IllegalArgumentException("A permuta so pode ser feita com turnos da mesma loja.");
        }

        if (permutaRepository.existsPedidoPendentePorOrigemEDestino(meuTurno.getId(), turnoColega.getId())) {
            throw new IllegalArgumentException("Ja existe um pedido pendente para esta combinacao de turnos.");
        }

        if (permutaRepository.existsPedidoPendentePorHorarioOrigem(meuTurno.getId())) {
            throw new IllegalArgumentException("Ja existe um pedido pendente para o teu turno selecionado.");
        }
    }
}
