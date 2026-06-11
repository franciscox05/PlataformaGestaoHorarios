package com.example.projeto2.API.Services.geracao.dto;

import java.time.LocalDate;

/** Uma linha do horário: o turno de um colaborador num dia, com o seu estado. */
public record HorarioLinha(
        Integer idHorario,
        Integer idColaborador,
        LocalDate data,
        String diaSemana,
        String turno,
        String periodo,
        String colaborador,
        String cargo,
        String estado
) {
}
