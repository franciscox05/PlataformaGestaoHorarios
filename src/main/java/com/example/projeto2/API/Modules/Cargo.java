package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "cargos")
public class Cargo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cargo", nullable = false)
    private Integer id;

    @Column(name = "nome", length = 100)
    private String nome;

    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_cargo_enum")
    private String tipo;

    @Column(name = "descricao", length = Integer.MAX_VALUE)
    private String descricao;

    @OneToMany(mappedBy = "idCargo")
    private Set<Lojautilizador> lojautilizadors = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTipo() { return tipo; }

    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Set<Lojautilizador> getLojautilizadors() {
        return lojautilizadors;
    }

    public void setLojautilizadors(Set<Lojautilizador> lojautilizadors) {
        this.lojautilizadors = lojautilizadors;
    }

}