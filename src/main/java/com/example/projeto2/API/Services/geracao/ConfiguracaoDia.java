package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Turno;

import java.util.List;

/**
 * Configuração especial do motor para um dia concreto (feriado, horário especial
 * de loja, encerramento). É a forma "achatada" que o {@code HorarioGeneratorEngine}
 * consome — distinta do DTO {@code ConfiguracaoDiaEspecial} da camada de serviço.
 */
public record ConfiguracaoDia(
        boolean lojaEncerrada,
        List<Turno> turnosCompativeis,
        Integer minimoColaboradoresTurno,
        String descricao
) {
}
