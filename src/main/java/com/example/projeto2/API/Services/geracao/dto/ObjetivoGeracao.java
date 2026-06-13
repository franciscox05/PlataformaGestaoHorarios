package com.example.projeto2.API.Services.geracao.dto;

import com.example.projeto2.API.Services.geracao.PoliticaOtimizacao;

/**
 * Objetivo que o gestor escolhe no passo "Configurar" antes de gerar — o que a app
 * deve priorizar ao distribuir os turnos. Mapeia para a {@link PoliticaOtimizacao}
 * (conjunto de pesos) que o motor já usa internamente.
 *
 * <p>{@link #AUTOMATICO} preserva o comportamento clássico: as N alternativas percorrem
 * todas as políticas, dando variedade. Qualquer outro objetivo fixa a política em todas
 * as alternativas (a variedade vem então da semente de diversificação).
 */
public enum ObjetivoGeracao {

    AUTOMATICO("Automático (variar)", "Gera alternativas com estratégias diferentes para comparar."),
    EQUILIBRIO("Equilibrar carga", "Reparte as horas o mais uniformemente possível pela equipa."),
    PREFERENCIAS("Maximizar preferências", "Dá prioridade a turnos, folgas e colegas preferidos."),
    FINS_DE_SEMANA("Rotação de fins de semana", "Reforça a rotatividade e o uso da equipa de fim de semana."),
    CARGA_CONTRATUAL("Cumprir contratos", "Aproxima as horas de cada colaborador da sua carga contratual.");

    private final String rotulo;
    private final String descricao;

    ObjetivoGeracao(String rotulo, String descricao) {
        this.rotulo = rotulo;
        this.descricao = descricao;
    }

    public String rotulo() { return rotulo; }
    public String descricao() { return descricao; }

    /** A política de pesos correspondente, ou {@code null} para {@link #AUTOMATICO}. */
    public PoliticaOtimizacao paraPolitica() {
        return switch (this) {
            case AUTOMATICO -> null;
            case EQUILIBRIO -> PoliticaOtimizacao.EQUILIBRIO;
            case PREFERENCIAS -> PoliticaOtimizacao.PREFERENCIAS;
            case FINS_DE_SEMANA -> PoliticaOtimizacao.FINS_DE_SEMANA;
            case CARGA_CONTRATUAL -> PoliticaOtimizacao.CARGA_CONTRATUAL;
        };
    }
}
