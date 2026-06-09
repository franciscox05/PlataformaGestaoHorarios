package com.example.projeto2.API.Enums;

/**
 * Estados possíveis de um registo de Horario.
 * Mapeado para o tipo PostgreSQL: estado_horario_enum
 */
public enum EstadoHorario {
    pendente,
    aprovado,
    rejeitado,
    recusado,
    publicado
}
