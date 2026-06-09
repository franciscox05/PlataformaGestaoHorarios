package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "regras")
public class Regra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regra", nullable = false)
    private Integer id;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "valor_padrao")
    private Integer valorPadrao;

    @Column(name = "tipo", length = 50)
    private String tipo;

    @OneToMany(mappedBy = "idRegra")
    private Set<RegrasLoja> regrasLojas = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getValorPadrao() {
        return valorPadrao;
    }

    public void setValorPadrao(Integer valorPadrao) {
        this.valorPadrao = valorPadrao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Set<RegrasLoja> getRegrasLojas() {
        return regrasLojas;
    }

    public void setRegrasLojas(Set<RegrasLoja> regrasLojas) {
        this.regrasLojas = regrasLojas;
    }

}