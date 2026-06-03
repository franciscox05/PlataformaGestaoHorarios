package com.example.projeto2.Modules;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "day_offs")
public class DayOff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dayoff", nullable = false)
    private Integer idDayoff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilizador", nullable = false)
    private Utilizador idUtilizador;

    @Column(name = "data_ausencia", nullable = false)
    private LocalDate dataAusencia;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "estado", length = 50)
    private String estado;

    public DayOff() {
    }

    public DayOff(Integer idDayoff, Utilizador idUtilizador, LocalDate dataAusencia, String motivo, String tipo, String estado) {
        this.idDayoff = idDayoff;
        this.idUtilizador = idUtilizador;
        this.dataAusencia = dataAusencia;
        this.motivo = motivo;
        this.tipo = tipo;
        this.estado = estado;
    }

    public Integer getIdDayoff() {
        return idDayoff;
    }

    public void setIdDayoff(Integer idDayoff) {
        this.idDayoff = idDayoff;
    }

    public Utilizador getIdUtilizador() {
        return idUtilizador;
    }

    public void setIdUtilizador(Utilizador idUtilizador) {
        this.idUtilizador = idUtilizador;
    }

    public LocalDate getDataAusencia() {
        return dataAusencia;
    }

    public void setDataAusencia(LocalDate dataAusencia) {
        this.dataAusencia = dataAusencia;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
