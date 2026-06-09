package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "lojas")
public class Loja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_loja", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "localizacao")
    private String localizacao;

    @Column(name = "hora_abertura", nullable = false)
    private LocalTime horaAbertura;

    @Column(name = "hora_fecho", nullable = false)
    private LocalTime horaFecho;

    @OneToMany(mappedBy = "idLoja")
    private Set<Lojautilizador> lojautilizadors = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idLoja")
    private Set<RegrasLoja> regrasLojas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idLoja")
    private Set<HorarioEspecialLoja> horariosEspeciais = new LinkedHashSet<>();

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

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public LocalTime getHoraAbertura() {
        return horaAbertura;
    }

    public void setHoraAbertura(LocalTime horaAbertura) {
        this.horaAbertura = horaAbertura;
    }

    public LocalTime getHoraFecho() {
        return horaFecho;
    }

    public void setHoraFecho(LocalTime horaFecho) {
        this.horaFecho = horaFecho;
    }

    public Set<Lojautilizador> getLojautilizadors() {
        return lojautilizadors;
    }

    public void setLojautilizadors(Set<Lojautilizador> lojautilizadors) {
        this.lojautilizadors = lojautilizadors;
    }

    public Set<RegrasLoja> getRegrasLojas() {
        return regrasLojas;
    }

    public void setRegrasLojas(Set<RegrasLoja> regrasLojas) {
        this.regrasLojas = regrasLojas;
    }

    public Set<HorarioEspecialLoja> getHorariosEspeciais() {
        return horariosEspeciais;
    }

    public void setHorariosEspeciais(Set<HorarioEspecialLoja> horariosEspeciais) {
        this.horariosEspeciais = horariosEspeciais;
    }
}
