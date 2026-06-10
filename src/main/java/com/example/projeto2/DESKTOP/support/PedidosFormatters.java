package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Horario;
import com.example.projeto2.API.Modules.Permuta;
import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Services.SnapshotOperacionalLojaService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class PedidosFormatters {

    public static final DateTimeFormatter DATA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final Locale LOCALE_PT = Locale.forLanguageTag("pt-PT");

    private PedidosFormatters() {}

    public static String formatarData(LocalDate data) {
        return data == null ? "-" : DATA_FORMATTER.format(data);
    }

    public static String formatarDataHora(Instant dataPedido) {
        return dataPedido == null ? "-"
                : DATA_HORA_FORMATTER.format(dataPedido.atZone(ZoneId.systemDefault()));
    }

    public static String formatarTipoFolga(String tipo) {
        if (tipo == null || tipo.isBlank()) return "-";
        return switch (tipo.toLowerCase(LOCALE_PT)) {
            case "ferias" -> "Férias";
            case "folgas" -> "Folgas";
            case "baixa"  -> "Baixa";
            default       -> tipo;
        };
    }

    public static String formatarTipoPreferencia(String tipo) {
        if (tipo == null || tipo.isBlank()) return "-";
        return switch (tipo.toLowerCase(LOCALE_PT)) {
            case "folgas"   -> "Folgas";
            case "ferias"   -> "Férias";
            case "colegas"  -> "Colegas";
            case "turnos"   -> "Turnos";
            default         -> tipo;
        };
    }

    public static String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio == null && dataFim == null) return "Sem período";
        if (dataInicio != null && dataFim != null)
            return DATA_FORMATTER.format(dataInicio) + " a " + DATA_FORMATTER.format(dataFim);
        return dataInicio != null ? formatarData(dataInicio) : formatarData(dataFim);
    }

    public static String formatarVigencia(Preferencia preferencia) {
        if (preferencia == null) return "-";
        if (preferencia.getDataFim() == null && preferencia.getDataInicio() != null) {
            return ("colegas".equalsIgnoreCase(preferencia.getTipo())
                    || "turnos".equalsIgnoreCase(preferencia.getTipo()))
                    ? "Permanente" : "Data única";
        }
        if (preferencia.getDataInicio() != null || preferencia.getDataFim() != null)
            return "Temporária";
        return "Sem período";
    }

    public static String formatarTexto(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    public static String obterNomePermuta(Permuta permuta) {
        if (permuta == null
                || permuta.getIdHorarioOrigem() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador() == null
                || permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador() == null) {
            return "-";
        }
        return formatarTexto(permuta.getIdHorarioOrigem().getIdLojautilizador().getIdUtilizador().getNome());
    }

    public static String obterNomePreferencia(Preferencia preferencia) {
        if (preferencia == null || preferencia.getIdUtilizador() == null) return "-";
        return formatarTexto(preferencia.getIdUtilizador().getNome());
    }

    public static String formatarTurno(Horario horario, boolean incluirNome) {
        if (horario == null || horario.getDataTurno() == null || horario.getIdTurno() == null)
            return "-";
        String base = DATA_FORMATTER.format(horario.getDataTurno())
                + " | "
                + horario.getIdTurno().getHoraInicio()
                + " - "
                + horario.getIdTurno().getHoraFim();
        if (!incluirNome
                || horario.getIdLojautilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador() == null
                || horario.getIdLojautilizador().getIdUtilizador().getNome() == null) {
            return base;
        }
        return horario.getIdLojautilizador().getIdUtilizador().getNome() + " | " + base;
    }

    public static String formatarTurnosColaborador(
            List<SnapshotOperacionalLojaService.TurnoPlaneado> turnos) {
        if (turnos == null || turnos.isEmpty()) return "Sem turnos planeados no período";
        return turnos.stream()
                .map(t -> formatarData(t.data()) + " | " + formatarTexto(t.periodo()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("-");
    }

    public static String formatarAusenciasColaborador(
            List<SnapshotOperacionalLojaService.AusenciaOperacional> ausencias) {
        if (ausencias == null || ausencias.isEmpty()) return "Sem ausências aprovadas no período";
        return ausencias.stream()
                .map(a -> formatarData(a.data()) + " | " + formatarTexto(a.tipo()))
                .reduce((a, b) -> a + "; " + b)
                .orElse("-");
    }

    public static String descreverPedido(SnapshotOperacionalLojaService.ContextoPedidoOperacional contexto) {
        if (contexto == null || contexto.pedido() == null) return "Contexto operacional indisponível";
        return switch (contexto.pedido().tipo()) {
            case FOLGA       -> "Pedido de folga de " + contexto.pedido().colaboradorPrincipal();
            case PERMUTA     -> "Pedido de permuta de " + contexto.pedido().colaboradorPrincipal();
            case PREFERENCIA -> "Preferência de " + contexto.pedido().colaboradorPrincipal();
        };
    }

    public static String descreverPeriodoContexto(
            SnapshotOperacionalLojaService.IntervaloOperacional intervalo) {
        if (intervalo == null) return "-";
        if (intervalo.unicoDia())
            return "Contexto de loja para " + formatarData(intervalo.dataInicio());
        return "Contexto de loja de "
                + formatarData(intervalo.dataInicio())
                + " a "
                + formatarData(intervalo.dataFim());
    }
}
