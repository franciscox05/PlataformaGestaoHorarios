package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Services.geracao.dto.*;
import com.example.projeto2.API.Modules.HorarioEspecialLoja;
import com.example.projeto2.API.Modules.Loja;
import com.example.projeto2.API.Modules.Turno;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Constrói as configurações especiais por dia de loja (horários especiais, encerramentos,
 * mínimos de colaboradores por turno) a partir de {@link HorarioEspecialLoja}.
 *
 * <p>Sem estado e sem dependências de repositórios — todas as funções são puras.
 */
public final class LojaConfiguracaoBuilder {

    private LojaConfiguracaoBuilder() {
        // utilitário
    }

    /**
     * Para cada data no intervalo coberto pelos {@code horariosEspeciais}, constrói uma
     * {@link ConfiguracaoDiaEspecial} com os turnos compatíveis e
     * os mínimos de colaboradores.
     */
    public static Map<LocalDate, ConfiguracaoDiaEspecial> construirConfiguracoesEspeciaisPorData(
            Loja loja,
            List<Turno> turnosBase,
            List<HorarioEspecialLoja> horariosEspeciais) {

        Map<LocalDate, ConfiguracaoDiaEspecial> configuracoes = new LinkedHashMap<>();
        if (loja == null || horariosEspeciais == null || horariosEspeciais.isEmpty()) {
            return configuracoes;
        }

        for (HorarioEspecialLoja horarioEspecial : horariosEspeciais) {
            if (horarioEspecial.getDataInicio() == null || horarioEspecial.getDataFim() == null) {
                continue;
            }

            LocalDate dataAtual = horarioEspecial.getDataInicio();
            while (!dataAtual.isAfter(horarioEspecial.getDataFim())) {
                configuracoes.put(dataAtual, criarConfiguracaoDiaEspecial(turnosBase, horarioEspecial));
                dataAtual = dataAtual.plusDays(1);
            }
        }

        return configuracoes;
    }

    private static ConfiguracaoDiaEspecial criarConfiguracaoDiaEspecial(
            List<Turno> turnosBase,
            HorarioEspecialLoja horarioEspecial) {

        boolean lojaEncerrada = Boolean.TRUE.equals(horarioEspecial.getLojaEncerrada());
        if (lojaEncerrada) {
            return new ConfiguracaoDiaEspecial(
                    true, List.of(), null, horarioEspecial.getDescricao());
        }

        LocalTime horaAbertura = horarioEspecial.getHoraAbertura();
        LocalTime horaFecho = horarioEspecial.getHoraFecho();
        List<Turno> turnosCompativeis = (horaAbertura != null && horaFecho != null)
                ? filtrarTurnosCompativeis(turnosBase, horaAbertura, horaFecho)
                : turnosBase;

        return new ConfiguracaoDiaEspecial(
                false,
                turnosCompativeis,
                horarioEspecial.getMinimoColaboradoresTurno(),
                horarioEspecial.getDescricao()
        );
    }

    private static List<Turno> filtrarTurnosCompativeis(
            List<Turno> turnosBase, LocalTime horaAbertura, LocalTime horaFecho) {

        if (turnosBase == null || turnosBase.isEmpty()) {
            return List.of();
        }
        if (horaAbertura == null || horaFecho == null) {
            return turnosBase;
        }

        List<Turno> turnosComCorrespondenciaExata = turnosBase.stream()
                .filter(turno -> turno != null)
                .filter(turno -> horaAbertura.equals(turno.getHoraInicio())
                        && horaFecho.equals(turno.getHoraFim()))
                .toList();
        if (!turnosComCorrespondenciaExata.isEmpty()) {
            return turnosComCorrespondenciaExata;
        }

        return turnosBase.stream()
                .filter(turno -> turnoCabeNoHorario(turno, horaAbertura, horaFecho))
                .toList();
    }

    private static boolean turnoCabeNoHorario(
            Turno turno, LocalTime horaAbertura, LocalTime horaFecho) {

        if (turno == null || turno.getHoraInicio() == null || turno.getHoraFim() == null
                || horaAbertura == null || horaFecho == null) {
            return false;
        }
        return !turno.getHoraInicio().isBefore(horaAbertura)
                && !turno.getHoraFim().isAfter(horaFecho);
    }
}
