package com.example.projeto2.API.Modules;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "permutas_folga")
public class PermutaFolga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permuta_folga", nullable = false)
    private Integer id;

    /** Turno do Func1 no dia D (será transferido para Func2). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario_d", nullable = false)
    private Horario idHorarioD;

    /** Turno do Func2 no dia Y (compensação — será transferido para Func1). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario_y", nullable = false)
    private Horario idHorarioY;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "data_pedido", nullable = false)
    private Instant dataPedido;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Horario getIdHorarioD() { return idHorarioD; }
    public void setIdHorarioD(Horario idHorarioD) { this.idHorarioD = idHorarioD; }

    public Horario getIdHorarioY() { return idHorarioY; }
    public void setIdHorarioY(Horario idHorarioY) { this.idHorarioY = idHorarioY; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Instant getDataPedido() { return dataPedido; }
    public void setDataPedido(Instant dataPedido) { this.dataPedido = dataPedido; }
}
