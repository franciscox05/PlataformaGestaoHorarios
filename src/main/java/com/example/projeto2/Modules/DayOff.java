package com.example.projeto2.Modules;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Table(name = "day_offs")
public class DayOff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dayoff", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilizador", nullable = false)
    private Utilizador idUtilizador;

    @Column(name = "data_ausencia", nullable = false)
    private LocalDate dataAusencia;

    @Column(name = "motivo", length = Integer.MAX_VALUE)
    private String motivo;

    @Column(name = "tipo", columnDefinition = "tipo_dayoff_enum not null")
    private Object tipo;

    @ColumnDefault("'pendente'")
    @Column(name = "estado", columnDefinition = "estado_dayoff_enum")
    private Object estado;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Object getTipo() {
        return tipo;
    }

    public void setTipo(Object tipo) {
        this.tipo = tipo;
    }

    public Object getEstado() {
        return estado;
    }

    public void setEstado(Object estado) {
        this.estado = estado;
    }

}