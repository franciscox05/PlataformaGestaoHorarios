package com.example.projeto2.DESKTOP.support;

import com.example.projeto2.API.Modules.Preferencia;
import com.example.projeto2.API.Modules.Utilizador;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PreferenciaFormatters {

    public static final DateTimeFormatter DATA_DECISAO_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PreferenciaFormatters() {}

    public static String formatarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) return "-";
        return switch (tipo.toLowerCase()) {
            case "folgas"          -> "Folgas";
            case "ferias"          -> "Férias";
            case "folga_preferida" -> "Folga preferida";
            case "colegas"         -> "Colegas";
            case "turnos"          -> "Turnos";
            default                -> tipo;
        };
    }

    public static String formatarEstado(String estado) {
        if (estado == null || estado.isBlank()) return "Pendente";
        return switch (estado.toLowerCase()) {
            case "pendente"  -> "Pendente";
            case "aprovado"  -> "Aprovado";
            case "rejeitado" -> "Rejeitado";
            default          -> estado;
        };
    }

    public static String formatarDecisao(String decisao) {
        return (decisao == null || decisao.isBlank()) ? "-" : decisao;
    }

    public static String formatarPeriodo(Preferencia preferencia) {
        if (preferencia == null) return "Sem período definido";
        return formatarPeriodo(preferencia.getDataInicio(), preferencia.getDataFim(), preferencia.getTipo());
    }

    public static String formatarPeriodo(LocalDate dataInicio, LocalDate dataFim, String tipo) {
        if (dataInicio == null && dataFim == null) return "Sem período definido";
        if (dataInicio != null && dataFim == null) return "Desde " + DATA_FORMATTER.format(dataInicio);
        if (dataInicio != null)                    return DATA_FORMATTER.format(dataInicio) + " a " + DATA_FORMATTER.format(dataFim);
        return DATA_FORMATTER.format(dataFim);
    }

    public static String formatarDataDecisao(LocalDateTime dataDecisao) {
        return dataDecisao == null ? "-" : DATA_DECISAO_FORMATTER.format(dataDecisao);
    }

    public static String obterNomeUtilizador(Utilizador utilizador) {
        if (utilizador == null || utilizador.getNome() == null || utilizador.getNome().isBlank()) return "-";
        return utilizador.getNome();
    }

    public static String formatarDescricao(String descricao) {
        return (descricao == null || descricao.isBlank()) ? "-" : descricao;
    }

    public static String formatarVigencia(Preferencia preferencia) {
        if (preferencia == null) return "-";
        if (preferencia.getDataFim() == null && preferencia.getDataInicio() != null) return "Permanente";
        if (preferencia.getDataInicio() != null || preferencia.getDataFim() != null)  return "Temporária";
        return "Sem período";
    }

    public static String normalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    public static String limparTexto(String texto) {
        if (texto == null) return null;
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    public static String normalizarTipo(String tipoSelecionado) {
        return tipoSelecionado == null ? "" : tipoSelecionado.trim().toLowerCase(Locale.ROOT);
    }

    public static <S> TableCell<S, String> criarCelulaBadgeEstado() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null || estado.isBlank()) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label badge = new Label(estado);
                badge.getStyleClass().add("badge-estado");
                switch (estado.toLowerCase()) {
                    case "pendente"  -> badge.getStyleClass().add("badge-pendente");
                    case "aprovado"  -> badge.getStyleClass().add("badge-aprovado");
                    case "rejeitado" -> badge.getStyleClass().add("badge-rejeitado");
                    default          -> badge.getStyleClass().add("badge-rascunho");
                }
                setGraphic(badge);
                setText(null);
            }
        };
    }
}
