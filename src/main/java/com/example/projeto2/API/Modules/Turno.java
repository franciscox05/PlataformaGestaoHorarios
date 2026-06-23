package com.example.projeto2.API.Modules;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "turnos")
public class Turno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno", nullable = false)
    private Integer id;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "nome", length = 100)
    private String nome;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /**
     * Loja a que o turno pertence. {@code null} = turno <b>global</b>, visível e
     * usável por todas as lojas (compatibilidade com o seed e os turnos partilhados
     * históricos). Turnos criados pela gestão de loja ficam scoped a essa loja.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_loja")
    private Loja idLoja;

    /**
     * Versionamento copy-on-write: a partir de quando esta versão do turno é válida
     * para a geração. {@code null} = válido desde sempre.
     */
    @Column(name = "data_inicio_vigencia")
    private LocalDate dataInicioVigencia;

    /**
     * Até quando esta versão é válida (inclusive). {@code null} = sem fim (versão
     * corrente). Uma versão arquivada (com {@code dataFimVigencia} preenchida) fica
     * ligada ao histórico publicado mas é excluída das gerações futuras.
     */
    @Column(name = "data_fim_vigencia")
    private LocalDate dataFimVigencia;

    @OneToMany(mappedBy = "idTurno")
    private Set<Horario> horarios = new LinkedHashSet<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFim() {
        return horaFim;
    }

    public void setHoraFim(LocalTime horaFim) {
        this.horaFim = horaFim;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Loja getIdLoja() {
        return idLoja;
    }

    public void setIdLoja(Loja idLoja) {
        this.idLoja = idLoja;
    }

    public LocalDate getDataInicioVigencia() {
        return dataInicioVigencia;
    }

    public void setDataInicioVigencia(LocalDate dataInicioVigencia) {
        this.dataInicioVigencia = dataInicioVigencia;
    }

    public LocalDate getDataFimVigencia() {
        return dataFimVigencia;
    }

    public void setDataFimVigencia(LocalDate dataFimVigencia) {
        this.dataFimVigencia = dataFimVigencia;
    }

    public Set<Horario> getHorarios() {
        return horarios;
    }

    public void setHorarios(Set<Horario> horarios) {
        this.horarios = horarios;
    }

    /**
     * Indica se esta versão do turno é válida em qualquer dia dentro do intervalo
     * {@code [inicio, fim]}. Vigência aberta de qualquer lado conta como sempre válida
     * desse lado. Usado pela geração para escolher a versão certa de cada mês.
     */
    public boolean vigenteNoPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            return true;
        }
        boolean comecaAntesOuDurante = dataInicioVigencia == null || !dataInicioVigencia.isAfter(fim);
        boolean terminaDepoisOuDurante = dataFimVigencia == null || !dataFimVigencia.isBefore(inicio);
        return comecaAntesOuDurante && terminaDepoisOuDurante;
    }

}
