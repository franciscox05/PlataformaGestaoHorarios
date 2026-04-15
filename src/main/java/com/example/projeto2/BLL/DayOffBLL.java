package com.example.projeto2.BLL;

import com.example.projeto2.Modules.DayOff;
import com.example.projeto2.Repositories.DayOffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

        if (pedido.getDataAusencia() == null) {
            throw new IllegalArgumentException("A data de ausencia e obrigatoria.");
        }

        if (pedido.getDataAusencia().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de ausencia nao pode estar no passado.");
        }

        pedido.setEstado("pendente");

        return dayOffRepository.save(pedido);
    }
}
