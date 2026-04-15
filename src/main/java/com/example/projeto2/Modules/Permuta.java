package com.example.projeto2.Modules;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "permutas")
public class Permuta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permuta", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario_origem", nullable = false)
    private Horario idHorarioOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario_destino", nullable = false)
    private Horario idHorarioDestino;

    @Column(name = "estado", columnDefinition = "estado_permuta_enum")
    private String estado;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "data_pedido")
    private Instant dataPedido;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Horario getIdHorarioOrigem() {
        return idHorarioOrigem;
    }

    public void setIdHorarioOrigem(Horario idHorarioOrigem) {
        this.idHorarioOrigem = idHorarioOrigem;
    }

    public Horario getIdHorarioDestino() {
        return idHorarioDestino;
    }

    public void setIdHorarioDestino(Horario idHorarioDestino) {
        this.idHorarioDestino = idHorarioDestino;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getDataPedido() {
        return dataPedido;
    }

    public void setDataPedido(Instant dataPedido) {
        this.dataPedido = dataPedido;
    }

}