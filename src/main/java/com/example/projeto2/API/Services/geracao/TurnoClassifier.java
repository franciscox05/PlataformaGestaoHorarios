package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Turno;

import java.time.LocalTime;
import java.util.Locale;

/**
 * Classificação de turnos por período do dia (manhã, intermédio, tarde, noite),
 * partilhada pelo motor (agrupamento de slots) e pelo avaliador de pontuação
 * (correspondência com preferências). Função pura, sem estado.
 *
 * <p>Usa o {@code tipo} do turno quando presente; caso contrário deriva o período
 * a partir da hora de início.
 */
public final class TurnoClassifier {

    private static final LocalTime FRONTEIRA_MANHA = LocalTime.of(10, 0);
    private static final LocalTime FRONTEIRA_TARDE = LocalTime.of(17, 0);

    private TurnoClassifier() {
        // utilitário
    }

    /** Tipo normalizado: "manha", "intermedio", "tarde", "noite" ou "desconhecido". */
    public static String tipoNormalizado(Turno turno) {
        if (turno == null) return "desconhecido";
        String tipo = turno.getTipo() != null ? turno.getTipo().toString().toLowerCase(Locale.ROOT) : "";
        if (!tipo.isBlank()) return tipo;
        LocalTime inicio = turno.getHoraInicio();
        if (inicio == null) return "desconhecido";
        if (inicio.isBefore(FRONTEIRA_MANHA)) return "manha";
        if (inicio.isBefore(LocalTime.NOON)) return "intermedio";
        if (inicio.isBefore(FRONTEIRA_TARDE)) return "tarde";
        return "noite";
    }

    /**
     * Indica se o turno corresponde a algum dos períodos exigidos por uma preferência.
     * A correspondência usa o tipo normalizado e, como reforço, a hora de início.
     */
    public static boolean correspondePeriodo(Turno turno,
                                              boolean exigeManha,
                                              boolean exigeTarde,
                                              boolean exigeIntermedio,
                                              boolean exigeNoite) {
        String tipo = tipoNormalizado(turno);
        LocalTime inicio = turno.getHoraInicio();

        if (exigeManha && ("manha".equals(tipo)
                || (inicio != null && inicio.isBefore(FRONTEIRA_MANHA)))) {
            return true;
        }
        if (exigeIntermedio && ("intermedio".equals(tipo)
                || (inicio != null && !inicio.isBefore(FRONTEIRA_MANHA) && inicio.isBefore(LocalTime.NOON)))) {
            return true;
        }
        if (exigeTarde && ("tarde".equals(tipo)
                || (inicio != null && !inicio.isBefore(LocalTime.NOON) && inicio.isBefore(FRONTEIRA_TARDE)))) {
            return true;
        }
        return exigeNoite && ("noite".equals(tipo)
                || (inicio != null && !inicio.isBefore(FRONTEIRA_TARDE)));
    }
}
