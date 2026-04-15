package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Repositories.DayOffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class DayOffBLL {

    private final DayOffRepository dayOffRepository;

    public DayOffBLL(DayOffRepository dayOffRepository) {
        this.dayOffRepository = dayOffRepository;
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
}
