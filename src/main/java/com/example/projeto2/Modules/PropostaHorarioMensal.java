package com.example.projeto2.Modules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "propostas_horario_mensal")
public class PropostaHorarioMensal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_proposta_horario", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_loja", nullable = false)
    private Loja idLoja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilizador_geracao", nullable = false)
    private Utilizador idUtilizadorGeracao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilizador_decisao")
    private Utilizador idUtilizadorDecisao;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    @Column(name = "mes", nullable = false)
    private Integer mes;

    @ColumnDefault("'pendente'")
    @Column(name = "estado", nullable = false, length = 50)
    private String estado;

    @Column(name = "resumo_geracao", columnDefinition = "TEXT")
    private String resumoGeracao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;

    @Column(name = "observacoes_supervisor", columnDefinition = "TEXT")
    private String observacoesSupervisor;

    @OneToMany(mappedBy = "idPropostaHorario")
    private Set<Horario> horarios = new LinkedHashSet<>();

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

    public Utilizador getIdUtilizadorGeracao() {
        return idUtilizadorGeracao;
    }

    public void setIdUtilizadorGeracao(Utilizador idUtilizadorGeracao) {
        this.idUtilizadorGeracao = idUtilizadorGeracao;
    }

    public Utilizador getIdUtilizadorDecisao() {
        return idUtilizadorDecisao;
    }

    public void setIdUtilizadorDecisao(Utilizador idUtilizadorDecisao) {
        this.idUtilizadorDecisao = idUtilizadorDecisao;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getResumoGeracao() {
        return resumoGeracao;
    }

    public void setResumoGeracao(String resumoGeracao) {
        this.resumoGeracao = resumoGeracao;
    }

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDateTime dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public LocalDateTime getDataDecisao() {
        return dataDecisao;
    }

    public void setDataDecisao(LocalDateTime dataDecisao) {
        this.dataDecisao = dataDecisao;
    }

    public String getObservacoesSupervisor() {
        return observacoesSupervisor;
    }

    public void setObservacoesSupervisor(String observacoesSupervisor) {
        this.observacoesSupervisor = observacoesSupervisor;
    }

    public Set<Horario> getHorarios() {
        return horarios;
    }

    public void setHorarios(Set<Horario> horarios) {
        this.horarios = horarios;
    }
}
