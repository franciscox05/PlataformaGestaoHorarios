package com.example.projeto2.API.Modules;

import com.example.projeto2.API.Enums.EstadoUtilizador;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "utilizadores")
public class Utilizador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilizador", nullable = false)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "telemovel", length = 20)
    private String telemovel;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ColumnDefault("'ativo'")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", columnDefinition = "estado_user_enum")
    private EstadoUtilizador estado;

    @OneToMany(mappedBy = "idUtilizador")
    private Set<DayOff> dayOffs = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idUtilizador")
    private Set<Lojautilizador> lojautilizadors = new LinkedHashSet<>();

    @OneToMany(mappedBy = "idUtilizador")
    private Set<Preferencia> preferencias = new LinkedHashSet<>();

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelemovel() {
        return telemovel;
    }

    public void setTelemovel(String telemovel) {
        this.telemovel = telemovel;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public EstadoUtilizador getEstado() { return estado; }

    public void setEstado(EstadoUtilizador estado) { this.estado = estado; }

    public Set<DayOff> getDayOffs() {
        return dayOffs;
    }

    public void setDayOffs(Set<DayOff> dayOffs) {
        this.dayOffs = dayOffs;
    }

    public Set<Lojautilizador> getLojautilizadors() {
        return lojautilizadors;
    }

    public void setLojautilizadors(Set<Lojautilizador> lojautilizadors) {
        this.lojautilizadors = lojautilizadors;
    }

    public Set<Preferencia> getPreferencias() {
        return preferencias;
    }

    public void setPreferencias(Set<Preferencia> preferencias) {
        this.preferencias = preferencias;
    }

}