package com.example.projeto2.Enums;

/**
 * Estados possíveis de um pedido de Permuta.
 * Mapeado para o tipo PostgreSQL: estado_permuta_enum
 */
public enum EstadoPermuta {
    pendente,
    aprovado,
    rejeitado,
    cancelado
}
