package com.example.projeto2.API.Services.geracao;

import com.example.projeto2.API.Modules.Lojautilizador;

public final class EstadoColaboradorResumo {
    private final Lojautilizador ligacao;
    public final long cargaMaximaMinutos;
    public long minutosAtribuidos;
    public int turnosAtribuidos;
    public int totalFinsDeSemanaTrabalhados;

    public EstadoColaboradorResumo(Lojautilizador ligacao, long cargaMaximaMinutos) {
        this.ligacao = ligacao;
        this.cargaMaximaMinutos = cargaMaximaMinutos;
    }

    public Integer idUtilizador() {
        return ligacao.getIdUtilizador() != null ? ligacao.getIdUtilizador().getId() : null;
    }

    public Lojautilizador ligacao() {
        return ligacao;
    }

    public void registarTurno(long minutos) {
        minutosAtribuidos += minutos;
        turnosAtribuidos++;
    }

    public int turnosAtribuidos() {
        return turnosAtribuidos;
    }
}
