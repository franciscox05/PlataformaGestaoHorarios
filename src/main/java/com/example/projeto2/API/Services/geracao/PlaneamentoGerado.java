package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public record PlaneamentoGerado(
        List<Horario> horarios,
        List<EstadoColaboradorResumo> estados,
        Collection<LocalDate> diasCobertos
) {
}
