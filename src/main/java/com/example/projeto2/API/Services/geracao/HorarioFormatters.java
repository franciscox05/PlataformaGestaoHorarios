package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Lojautilizador;
import com.example.projeto2.API.Modules.Turno;

import java.time.Duration;
import java.time.LocalTime;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilitário de formatação de datas, durações e texto usado pela camada de geração
 * de horários e pelo seu mapeamento para DTOs. Apenas funções puras, sem estado.
 */
public final class HorarioFormatters {

    public static final DateTimeFormatter DATA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private HorarioFormatters() {
        // utilitário
    }

    public static String formatarTurno(Turno turno) {
        if (turno == null || turno.getTipo() == null) {
            return "-";
        }
        String tipo = String.valueOf(turno.getTipo()).toLowerCase(Locale.ROOT);
        return Character.toUpperCase(tipo.charAt(0)) + tipo.substring(1);
    }

    public static String formatarPeriodo(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return "-";
        }
        return turno.getHoraInicio() + " - " + turno.getHoraFim();
    }

    public static String formatarPeriodoVinculo(Lojautilizador ligacao) {
        if (ligacao == null || ligacao.getDataInicio() == null) {
            return "-";
        }
        String inicio = DATA_FORMATTER.format(ligacao.getDataInicio());
        String fim = ligacao.getDataFim() != null ? DATA_FORMATTER.format(ligacao.getDataFim()) : "sem fim";
        return inicio + " a " + fim;
    }

    public static String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return "-";
        }
        String valor = estado.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(valor.charAt(0)) + valor.substring(1);
    }

    public static String formatarDuracao(long minutosTotais) {
        long horas = minutosTotais / 60;
        long minutos = minutosTotais % 60;
        return horas + "h " + minutos + "m";
    }

    public static String formatarDiferencaDuracao(long minutosTotais) {
        String sinal = minutosTotais > 0 ? "+" : minutosTotais < 0 ? "-" : "";
        return sinal + formatarDuracao(Math.abs(minutosTotais));
    }

    public static String nomeMes(int mes) {
        return switch (Month.of(mes)) {
            case JANUARY -> "Janeiro";
            case FEBRUARY -> "Fevereiro";
            case MARCH -> "Março";
            case APRIL -> "Abril";
            case MAY -> "Maio";
            case JUNE -> "Junho";
            case JULY -> "Julho";
            case AUGUST -> "Agosto";
            case SEPTEMBER -> "Setembro";
            case OCTOBER -> "Outubro";
            case NOVEMBER -> "Novembro";
            case DECEMBER -> "Dezembro";
        };
    }

    public static String nomeDiaSemana(LocalDate data) {
        return switch (data.getDayOfWeek()) {
            case MONDAY -> "Segunda";
            case TUESDAY -> "Terca";
            case WEDNESDAY -> "Quarta";
            case THURSDAY -> "Quinta";
            case FRIDAY -> "Sexta";
            case SATURDAY -> "Sabado";
            case SUNDAY -> "Domingo";
        };
    }

    public static String valorOuTraco(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        return valor;
    }

    public static String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return semAcentos.toLowerCase(Locale.ROOT).trim();
    }

    public static String limparTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String textoLimpo = texto.trim();
        return textoLimpo.isEmpty() ? null : textoLimpo;
    }

    public static long calcularDuracaoEmMinutos(Turno turno) {
        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null) {
            return 0;
        }
        LocalTime inicio = turno.getHoraInicio();
        LocalTime fim = turno.getHoraFim();
        if (!fim.isBefore(inicio)) {
            return Duration.between(inicio, fim).toMinutes();
        }
        return Duration.between(inicio, LocalTime.MAX).plusMinutes(1).toMinutes()
                + Duration.between(LocalTime.MIN, fim).toMinutes();
    }
}
