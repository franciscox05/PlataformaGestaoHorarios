package com.example.projeto2.Modules;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "horarios")
public class Horario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) // Mudar para EAGER
    @JoinColumn(name = "id_lojautilizador", nullable = false)
    private Lojautilizador idLojautilizador;

    @ManyToOne(fetch = FetchType.EAGER, optional = false) // Mudar para EAGER
    @JoinColumn(name = "id_turno", nullable = false)
    private Turno idTurno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proposta_horario")
    private PropostaHorarioMensal idPropostaHorario;

    @Column(name = "data_turno", nullable = false)
    private LocalDate dataTurno;

    @ColumnDefault("'pendente'")
    @Column(name = "estado", columnDefinition = "estado_horario_enum")
    private String estado;

    @OneToMany(mappedBy = "idHorario")
    private Set<HistoricoHorarioEstado> historicoHorarioEstados = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idHorarioOrigem")
    private Set<Permuta> permutasComoOrigem = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idHorarioDestino")
    private Set<Permuta> permutasComoDestino = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Lojautilizador getIdLojautilizador() {
        return idLojautilizador;
    }

    public void setIdLojautilizador(Lojautilizador idLojautilizador) {
        this.idLojautilizador = idLojautilizador;
    }

    public Turno getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(Turno idTurno) {
        this.idTurno = idTurno;
    }

    public LocalDate getDataTurno() {
        return dataTurno;
    }

    public void setDataTurno(LocalDate dataTurno) {
        this.dataTurno = dataTurno;
    }

    public PropostaHorarioMensal getIdPropostaHorario() {
        return idPropostaHorario;
    }

    public void setIdPropostaHorario(PropostaHorarioMensal idPropostaHorario) {
        this.idPropostaHorario = idPropostaHorario;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Set<HistoricoHorarioEstado> getHistoricoHorarioEstados() {
        return historicoHorarioEstados;
    }

    public void setHistoricoHorarioEstados(Set<HistoricoHorarioEstado> historicoHorarioEstados) {
        this.historicoHorarioEstados = historicoHorarioEstados;
    }

    public Set<Permuta> getPermutasComoOrigem() {
        return permutasComoOrigem;
    }

    public void setPermutasComoOrigem(Set<Permuta> permutasComoOrigem) {
        this.permutasComoOrigem = permutasComoOrigem;
    }

    public Set<Permuta> getPermutasComoDestino() {
        return permutasComoDestino;
    }

    public void setPermutasComoDestino(Set<Permuta> permutasComoDestino) {
        this.permutasComoDestino = permutasComoDestino;
    }

}
