package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Horario;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Resultado de uma geração: os horários produzidos, os resumos por colaborador
 * (para métricas) e os dias cobertos. Inclui ainda {@code avisos} — notas
 * informativas como folgas preferidas (soft) que não puderam ser honradas por
 * necessidade de cobertura, para explicar ao gestor o porquê.
 */
public record PlaneamentoGerado(
        List<Horario> horarios,
        List<EstadoColaboradorResumo> estados,
        Collection<LocalDate> diasCobertos,
        List<String> avisos
) {
}
