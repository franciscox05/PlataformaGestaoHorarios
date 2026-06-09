package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "horarios_especiais_loja")
public class HorarioEspecialLoja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_especial", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_loja", nullable = false)
    private Loja idLoja;

    @Column(name = "descricao", nullable = false, length = 160)
    private String descricao;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(name = "hora_abertura")
    private LocalTime horaAbertura;

    @Column(name = "hora_fecho")
    private LocalTime horaFecho;

    @Column(name = "minimo_colaboradores_turno")
    private Integer minimoColaboradoresTurno;

    @Column(name = "loja_encerrada", nullable = false)
    private Boolean lojaEncerrada;

    @Column(name = "observacoes", length = Integer.MAX_VALUE)
    private String observacoes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Loja getIdLoja() {
        return idLoja;
    }

    public void setIdLoja(Loja idLoja) {
        this.idLoja = idLoja;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public LocalTime getHoraAbertura() {
        return horaAbertura;
    }

    public void setHoraAbertura(LocalTime horaAbertura) {
        this.horaAbertura = horaAbertura;
    }

    public LocalTime getHoraFecho() {
        return horaFecho;
    }

    public void setHoraFecho(LocalTime horaFecho) {
        this.horaFecho = horaFecho;
    }

    public Integer getMinimoColaboradoresTurno() {
        return minimoColaboradoresTurno;
    }

    public void setMinimoColaboradoresTurno(Integer minimoColaboradoresTurno) {
        this.minimoColaboradoresTurno = minimoColaboradoresTurno;
    }

    public Boolean getLojaEncerrada() {
        return lojaEncerrada;
    }

    public void setLojaEncerrada(Boolean lojaEncerrada) {
        this.lojaEncerrada = lojaEncerrada;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }
}
