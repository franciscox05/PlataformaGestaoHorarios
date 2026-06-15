package com.example.projeto2.API.Services.geracao.dto;

/**
 * Preferências do gestor para uma geração concreta, escolhidas no passo "Configurar".
 *
 * @param objetivo      o que priorizar (mapeia para a política de pesos do motor)
 * @param alvoPorTurno  nº alvo de pessoas por turno por dia aberto (soft): o motor tenta
 *                      atingi-lo no preenchimento de capacidade, sem nunca descer abaixo
 *                      do mínimo das regras nem ultrapassar a carga contratual da equipa.
 *                      {@code null} = automático (enche até à capacidade disponível).
 */
public record ConfiguracaoGeracao(ObjetivoGeracao objetivo, Integer alvoPorTurno) {

    public ConfiguracaoGeracao {
        if (objetivo == null) {
            objetivo = ObjetivoGeracao.AUTOMATICO;
        }
        if (alvoPorTurno != null && alvoPorTurno < 1) {
            alvoPorTurno = null;
        }
    }

    /** Configuração por omissão: comportamento clássico (varia políticas, enche capacidade). */
    public static ConfiguracaoGeracao padrao() {
        return new ConfiguracaoGeracao(ObjetivoGeracao.AUTOMATICO, null);
    }
}
