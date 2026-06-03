package com.example.projeto2.Enums;

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
