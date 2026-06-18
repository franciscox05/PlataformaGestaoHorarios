package com.example.projeto2.API.Modules;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Integer idEvento;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "origem", nullable = false, length = 80)
    private String origem = "desktop";

    @Column(name = "id_utilizador")
    private Integer idUtilizador;

    @Column(name = "email_referencia", length = 150)
    private String emailReferencia;

    @Column(name = "identificador_sessao", length = 64)
    private String identificadorSessao;

    @Column(name = "data_evento", nullable = false)
    private LocalDateTime dataEvento = LocalDateTime.now();

    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes;

    public EventoAuditoria() {
    }

    public EventoAuditoria(String tipoEvento, String resultado, Integer idUtilizador,
                           String emailReferencia, String detalhes) {
        this.tipoEvento = tipoEvento;
        this.resultado = resultado;
        this.idUtilizador = idUtilizador;
        this.emailReferencia = emailReferencia;
        this.detalhes = detalhes;
    }

    public Integer getIdEvento() { return idEvento; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public Integer getIdUtilizador() { return idUtilizador; }
    public void setIdUtilizador(Integer idUtilizador) { this.idUtilizador = idUtilizador; }
    public String getEmailReferencia() { return emailReferencia; }
    public void setEmailReferencia(String emailReferencia) { this.emailReferencia = emailReferencia; }
    public String getIdentificadorSessao() { return identificadorSessao; }
    public void setIdentificadorSessao(String identificadorSessao) { this.identificadorSessao = identificadorSessao; }
    public LocalDateTime getDataEvento() { return dataEvento; }
    public void setDataEvento(LocalDateTime dataEvento) { this.dataEvento = dataEvento; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
}
