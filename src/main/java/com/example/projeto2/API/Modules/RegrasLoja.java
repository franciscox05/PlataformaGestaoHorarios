package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

@Entity
@Table(name = "regras_loja")
public class RegrasLoja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regra_loja", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_loja", nullable = false)
    private Loja idLoja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_regra", nullable = false)
    private Regra idRegra;

    @Column(name = "valor_especifico")
    private Integer valorEspecifico;

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

    public Regra getIdRegra() {
        return idRegra;
    }

    public void setIdRegra(Regra idRegra) {
        this.idRegra = idRegra;
    }

    public Integer getValorEspecifico() {
        return valorEspecifico;
    }

    public void setValorEspecifico(Integer valorEspecifico) {
        this.valorEspecifico = valorEspecifico;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

}