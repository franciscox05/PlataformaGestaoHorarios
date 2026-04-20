package com.example.projeto2.Modules;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento", nullable = false)
    private Integer id;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "origem", nullable = false, length = 80)
    private String origem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilizador")
    private Utilizador idUtilizador;

    @Column(name = "email_referencia", length = 150)
    private String emailReferencia;

    @Column(name = "identificador_sessao", length = 64)
    private String identificadorSessao;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "data_evento", nullable = false)
    private Instant dataEvento;

    @Column(name = "detalhes", length = Integer.MAX_VALUE)
    private String detalhes;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public Utilizador getIdUtilizador() {
        return idUtilizador;
    }

    public void setIdUtilizador(Utilizador idUtilizador) {
        this.idUtilizador = idUtilizador;
    }

    public String getEmailReferencia() {
        return emailReferencia;
    }

    public void setEmailReferencia(String emailReferencia) {
        this.emailReferencia = emailReferencia;
    }

    public String getIdentificadorSessao() {
        return identificadorSessao;
    }

    public void setIdentificadorSessao(String identificadorSessao) {
        this.identificadorSessao = identificadorSessao;
    }

    public Instant getDataEvento() {
        return dataEvento;
    }

    public void setDataEvento(Instant dataEvento) {
        this.dataEvento = dataEvento;
    }

    public String getDetalhes() {
        return detalhes;
    }

    public void setDetalhes(String detalhes) {
        this.detalhes = detalhes;
    }
}
