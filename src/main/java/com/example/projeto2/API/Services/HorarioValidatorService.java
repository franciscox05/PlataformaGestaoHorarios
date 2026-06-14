package com.example.projeto2.API.Services;

import com.example.projeto2.API.Modules.Turno;
import com.example.projeto2.API.Services.geracao.HorarioFormatters;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Serviço responsável pela validação de regras de negócio de horários.
 * Extraído da GeracaoHorariosService como parte da refatorização SRP (Fase 2).
 *
 * Regras cobertas:
 *  - RFS03 : Chefia obrigatória ao sábado
 *  - RFS04 : Descanso semanal mínimo (dias de folga por semana)
 *  - RFS06 : Descanso mínimo entre turnos (horas)
 *  - RFS07 : Máximo de dias consecutivos
 *  - RFS08 : Rotação de fins de semana
 *  - RFS09 : Janela de lançamento de propostas
 *  - RFS10 : Carga contratual mensal
 *  - Helpers de reconhecimento de tipo de regra a partir da descrição textual
 */
@Service
public class HorarioValidatorService {

    // -------------------------------------------------------------------------
    // Constantes de validação
    // -------------------------------------------------------------------------

    private static final long DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS = 8 * 60L;

    // -------------------------------------------------------------------------
    // RFS06 — Descanso mínimo entre turnos consecutivos
    // -------------------------------------------------------------------------

    /**
     * Calcula as horas de descanso entre o fim do turno anterior e o início do turno seguinte.
     * Pressupõe que os turnos podem atravessar a meia-noite.
     */
    public long calcularHorasDescanso(LocalDate dataAnterior,
                                      Turno turnoAnterior,
                                      LocalDate dataAtual,
                                      Turno turnoAtual) {
        if (dataAnterior == null || turnoAnterior == null
                || dataAtual == null || turnoAtual == null) {
            return Long.MAX_VALUE;
        }

        LocalTime fimAnterior = turnoAnterior.getHoraFim();
        LocalTime inicioAtual = turnoAtual.getHoraInicio();
        if (fimAnterior == null || inicioAtual == null) {
            return Long.MAX_VALUE;
        }

        long minutosFimAnterior = dataAnterior.toEpochDay() * 24 * 60 + fimAnterior.toSecondOfDay() / 60;
        long minutosInicioAtual = dataAtual.toEpochDay() * 24 * 60 + inicioAtual.toSecondOfDay() / 60;

        // Turno anterior atravessa meia-noite: o fim pertence ao dia seguinte
        if (fimAnterior.isBefore(turnoAnterior.getHoraInicio() != null
                ? turnoAnterior.getHoraInicio() : LocalTime.MIDNIGHT)) {
            minutosFimAnterior += 24 * 60;
        }

        long descansoMinutos = minutosInicioAtual - minutosFimAnterior;
        return descansoMinutos / 60;
    }

    /**
     * Verifica se o descanso entre dois turnos consecutivos respeita o mínimo legal (RFS06).
     */
    public boolean respeitaDescansoMinimo(LocalDate dataAnterior,
                                         Turno turnoAnterior,
                                         LocalDate dataAtual,
                                         Turno turnoAtual,
                                         int descansoMinimoHoras) {
        long horas = calcularHorasDescanso(dataAnterior, turnoAnterior, dataAtual, turnoAtual);
        return horas >= descansoMinimoHoras;
    }

    // -------------------------------------------------------------------------
    // RFS07 — Máximo de dias consecutivos
    // -------------------------------------------------------------------------

    /**
     * Dado o número de dias consecutivos actuais e a data candidata,
     * verifica se acrescentar mais um dia violaría o limite (RFS07).
     */
    public boolean violaMaximoDiasConsecutivos(LocalDate ultimaDataAtribuida,
                                               int diasConsecutivosAtuais,
                                               LocalDate dataCandidata,
                                               int maxDiasConsecutivos) {
        if (ultimaDataAtribuida == null) {
            return false; // primeiro turno — sem consecutivos
        }
        if (!ultimaDataAtribuida.plusDays(1).equals(dataCandidata)) {
            return false; // não é um dia consecutivo
        }
        return (diasConsecutivosAtuais + 1) > maxDiasConsecutivos;
    }

    // -------------------------------------------------------------------------
    // RFS04 — Descanso semanal (dias de folga por semana)
    // -------------------------------------------------------------------------

    /**
     * Determina se atribuir um turno na {@code data} excederia o número máximo
     * de dias trabalhados por semana (= 7 - descansoSemanalMinimoDias).
     */
    public boolean excedeDiasTrabalhadosNaSemana(LocalDate data,
                                                 int diasTrabalhadosNaSemana,
                                                 int descansoSemanalMinimoDias) {
        int maxDiasTrabalhados = 7 - descansoSemanalMinimoDias;
        return diasTrabalhadosNaSemana >= maxDiasTrabalhados;
    }

    // -------------------------------------------------------------------------
    // RFS08 — Rotação de fins de semana
    // -------------------------------------------------------------------------

    /**
     * Verifica se atribuir um turno num fim de semana na {@code data} violaria
     * a janela mínima de rotação entre fins de semana trabalhados (RFS08).
     */
    public boolean violaRotacaoDeFimDeSemana(LocalDate data,
                                             int totalFinsDeSemanaTrabalhados,
                                             LocalDate ultimoFimDeSemanaInicio,
                                             int janelaRotacaoFinsDeSemanaSemanas) {
        if (!ehFimDeSemana(data) || janelaRotacaoFinsDeSemanaSemanas <= 0) {
            return false;
        }
        if (ultimoFimDeSemanaInicio == null || totalFinsDeSemanaTrabalhados == 0) {
            return false;
        }

        LocalDate inicioFimDeSemanaAtual = inicioFimDeSemana(data);
        long semanasDesdeFimDeSemanaAnterior = inicioFimDeSemanaAtual.toEpochDay() / 7
                - ultimoFimDeSemanaInicio.toEpochDay() / 7;

        // semanas == 0 significa que estamos no mesmo fim de semana (sábado E domingo):
        // trabalhar ambos os dias do mesmo FDS não é uma violação de rotação.
        return semanasDesdeFimDeSemanaAnterior > 0
                && semanasDesdeFimDeSemanaAnterior < janelaRotacaoFinsDeSemanaSemanas;
    }

    // -------------------------------------------------------------------------
    // RFS08 extra — Validação da janela de rotação mínima legal
    // -------------------------------------------------------------------------

    /**
     * Verifica se a janela de rotação de fins de semana respeita o mínimo legal
     * recomendado (1 FDS livre por cada 7 semanas trabalhadas).
     * Não é um hard block: é usado apenas para emitir um aviso ao gerente.
     */
    public boolean janelaRotacaoRespeiraMinimo(int janelaRotacaoSemanas) {
        return janelaRotacaoSemanas >= 7;
    }

    // -------------------------------------------------------------------------
    // RFS03 — Chefia obrigatória ao sábado
    // -------------------------------------------------------------------------

    /**
     * Indica se um cargo (pelo tipo normalizado) obriga presença ao sábado (RFS03).
     */
    public boolean exigePresencaAoSabado(String tipoCargo) {
        String normalizado = HorarioFormatters.normalizarTexto(tipoCargo);
        return normalizado.contains("gerente") || normalizado.contains("subgerente");
    }

    // -------------------------------------------------------------------------
    // Validação de turno
    // -------------------------------------------------------------------------

    /**
     * Verifica se a duração em minutos de um turno o qualifica como turno de
     * tempo inteiro (mínimo 8 horas).
     */
    public boolean ehTurnoTempoInteiro(long duracaoMinutos) {
        return duracaoMinutos >= DURACAO_MINIMA_TURNO_TEMPO_INTEIRO_MINUTOS;
    }

    /**
     * Calcula a duração em minutos de um turno.
     * Suporta turnos que atravessam a meia-noite.
     */
    public long calcularDuracaoEmMinutos(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return 0;
        }
        long inicio = turno.getHoraInicio().toSecondOfDay() / 60L;
        long fim = turno.getHoraFim().toSecondOfDay() / 60L;
        if (fim <= inicio) {
            fim += 24 * 60;
        }
        return fim - inicio;
    }

    /**
     * Verifica se um turno cabe dentro de um horário especial (hora abertura/fecho).
     */
    public boolean turnoCabeNoHorario(Turno turno, LocalTime horaAbertura, LocalTime horaFecho) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null
                || horaAbertura == null || horaFecho == null) {
            return false;
        }
        return !turno.getHoraInicio().isBefore(horaAbertura)
                && !turno.getHoraFim().isAfter(horaFecho);
    }

    // -------------------------------------------------------------------------
    // Helpers de datas
    // -------------------------------------------------------------------------

    /** Verifica se uma data é sábado ou domingo. */
    public boolean ehFimDeSemana(LocalDate data) {
        if (data == null) {
            return false;
        }
        DayOfWeek dow = data.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** Retorna o sábado da semana à qual pertence a data. */
    public LocalDate inicioFimDeSemana(LocalDate data) {
        if (data == null) {
            return null;
        }
        return data.with(DayOfWeek.SATURDAY);
    }

    /** Retorna a segunda-feira da semana à qual pertence a data. */
    public LocalDate inicioSemana(LocalDate data) {
        if (data == null) {
            return null;
        }
        return data.with(DayOfWeek.MONDAY);
    }

}
