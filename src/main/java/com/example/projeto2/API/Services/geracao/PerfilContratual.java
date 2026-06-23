package com.example.projeto2.API.Services.geracao;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static com.example.projeto2.API.Services.geracao.HorarioFormatters.normalizarTexto;

public enum PerfilContratual {
    GESTAO(Set.of("gerente", "subgerente", "supervisor"), 176, false),
    FULLTIME(Set.of("fulltime"), 176, false),
    // 22 dias x 4h: os turnos part-time passaram de 4h30 para 4h porque, ao contrário dos
    // turnos full-time (8h de trabalho + 1h de almoço = 9h de turno), um turno part-time
    // não tem pausa de almoço — pelo que 4h é metade das 8h de trabalho diárias, não de 9h.
    PARTTIME(Set.of("parttime"), 88, false),
    REFORCO_FIM_DE_SEMANA(Set.of("reforco_parttime"), 64, true);

    // 8 horas em minutos — o mínimo para turnos de tempo inteiro
    public static final long DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS = 8 * 60L;

    // 8 horas de trabalho em minutos — limite legal diário (CT art. 203.º), independente
    // de quantos turnos compõem o dia. Um colaborador com maxTurnosPorDia ≥ 2 pode juntar
    // dois turnos curtos consecutivos para completar o dia, mas a SOMA nunca pode exceder
    // este teto — sem isto, um turno de 4h seguido de um turno de 9h (8h de trabalho)
    // resultava em 12h de trabalho no mesmo dia.
    public static final long LIMITE_DIARIO_TRABALHO_MINUTOS = 8 * 60L;

    private final Set<String> tiposCargo;
    private final int cargaMensalHorasPadrao;
    private final boolean apenasFimDeSemana;

    PerfilContratual(Set<String> tiposCargo, int cargaMensalHorasPadrao, boolean apenasFimDeSemana) {
        this.tiposCargo = tiposCargo;
        this.cargaMensalHorasPadrao = cargaMensalHorasPadrao;
        this.apenasFimDeSemana = apenasFimDeSemana;
    }

    public static Optional<PerfilContratual> fromCargoTipo(String tipoCargo) {
        String tipoNormalizado = normalizarTexto(tipoCargo);
        if (tipoNormalizado.isBlank()) {
            return Optional.empty();
        }
        for (PerfilContratual perfil : values()) {
            if (perfil.tiposCargo.contains(tipoNormalizado)) {
                return Optional.of(perfil);
            }
        }
        return Optional.empty();
    }

    public int cargaMensalHorasPadrao() {
        return cargaMensalHorasPadrao;
    }

    public boolean permiteData(LocalDate data) {
        if (!apenasFimDeSemana || data == null) {
            return true;
        }
        DayOfWeek dayOfWeek = data.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    public boolean permiteTurno(long minutosTurno) {
        if (this != GESTAO && this != FULLTIME) {
            return true;
        }
        return minutosTurno >= DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS;
    }

    public boolean correspondeRegra(String textoNormalizado) {
        return switch (this) {
            case GESTAO -> textoNormalizado.contains("gestao")
                    || textoNormalizado.contains("gerencia")
                    || textoNormalizado.contains("gestor")
                    || textoNormalizado.contains("supervisor");
            case FULLTIME -> textoNormalizado.contains("fulltime")
                    || (textoNormalizado.contains("full") && textoNormalizado.contains("time"))
                    || textoNormalizado.contains("tempo inteiro");
            case PARTTIME -> textoNormalizado.contains("parttime")
                    || (textoNormalizado.contains("part") && textoNormalizado.contains("time"))
                    || textoNormalizado.contains("tempo parcial");
            case REFORCO_FIM_DE_SEMANA -> textoNormalizado.contains("reforco")
                    || textoNormalizado.contains("fim de semana")
                    || textoNormalizado.contains("weekend");
        };
    }

    public String descricaoCurta() {
        return switch (this) {
            case GESTAO -> "gestao";
            case FULLTIME -> "full-time";
            case PARTTIME -> "part-time";
            case REFORCO_FIM_DE_SEMANA -> "reforco de fim de semana";
        };
    }
}
