package com.example.projeto2.API.Modules;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "historico_horario_estados")
public class HistoricoHorarioEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registo", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_horario", nullable = false)
    private Horario idHorario;

    @Column(name = "estado_novo", columnDefinition = "estado_horario_enum not null")
    private String estadoNovo;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "data_registo")
    private Instant dataRegisto;

    @Column(name = "observacoes", length = Integer.MAX_VALUE)
    private String observacoes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Horario getIdHorario() {
        return idHorario;
    }

    public void setIdHorario(Horario idHorario) {
        this.idHorario = idHorario;
    }

    public String getEstadoNovo() {
        return estadoNovo;
    }

    public void setEstadoNovo(String estadoNovo) {
        this.estadoNovo = estadoNovo;
    }

    public Instant getDataRegisto() {
        return dataRegisto;
    }

    public void setDataRegisto(Instant dataRegisto) {
        this.dataRegisto = dataRegisto;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

}
