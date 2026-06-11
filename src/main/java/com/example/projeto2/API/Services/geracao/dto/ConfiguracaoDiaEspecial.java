package com.example.projeto2.API.Services.geracao.dto;

import com.example.projeto2.API.Modules.Turno;

import java.util.List;

/**
 * Configuração especial de um dia ao nível da camada de serviço (exceção de calendário,
 * horário especial de loja). É convertida na {@code geracao.ConfiguracaoDia} consumida pelo motor.
 */
public record ConfiguracaoDiaEspecial(
        boolean lojaEncerrada,
        List<Turno> turnosCompativeis,
        Integer minimoColaboradoresTurno,
        String descricao
) {
}
