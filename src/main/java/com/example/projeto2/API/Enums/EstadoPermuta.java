package com.example.projeto2.API.Enums;

/**
 * Estados possíveis de um pedido de Permuta.
 * Mapeado para o tipo PostgreSQL: estado_permuta_enum
 *
 * As constantes têm de cobrir TODAS as labels existentes no tipo enum do
 * PostgreSQL, caso contrário o Hibernate falha ao ler a linha com
 * IllegalArgumentException ("No enum constant ...EstadoPermuta.X").
 *
 * Por isso mantêm-se aqui também as variantes femininas/sinónimas que os dados
 * de seed gravam ({@code aprovada}, {@code recusada}) — à semelhança de
 * {@link EstadoHorario}, que carrega {@code rejeitado} e {@code recusado}.
 * O código escreve sempre as formas canónicas ({@code aprovado},
 * {@code rejeitado}, {@code cancelado}).
 */
public enum EstadoPermuta {
    pendente,
    aprovado,
    aprovada,
    rejeitado,
    recusada,
    cancelado
}
