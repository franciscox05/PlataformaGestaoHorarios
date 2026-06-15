package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {

    @Query("SELECT n FROM Notificacao n WHERE n.idUtilizador.id = :idUtilizador AND n.lida = false ORDER BY n.dataEnvio DESC")
    List<Notificacao> findPendentes(@Param("idUtilizador") Integer idUtilizador);

    List<Notificacao> findTop50ByIdUtilizadorIdOrderByDataEnvioDesc(Integer idUtilizador);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.idUtilizador.id = :idUtilizador AND n.lida = false")
    long countPendentes(@Param("idUtilizador") Integer idUtilizador);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.idUtilizador.id = :idUtilizador AND n.lida = false")
    void marcarTodasComoLidas(@Param("idUtilizador") Integer idUtilizador);

    /** Store-scoped variants — strict equality: only notifications stamped for exactly this store. */
    @Query(value = "SELECT * FROM notificacao WHERE id_utilizador = :idUtilizador AND id_loja = :idLoja ORDER BY data_envio DESC LIMIT 50", nativeQuery = true)
    List<Notificacao> findHistoricoByUtilizadorELoja(@Param("idUtilizador") Integer idUtilizador, @Param("idLoja") Integer idLoja);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.idUtilizador.id = :idUtilizador AND n.lida = false AND n.idLoja = :idLoja")
    long countPendentesPorLoja(@Param("idUtilizador") Integer idUtilizador, @Param("idLoja") Integer idLoja);

    @Modifying
    @Query("UPDATE Notificacao n SET n.lida = true WHERE n.idUtilizador.id = :idUtilizador AND n.lida = false AND n.idLoja = :idLoja")
    void marcarTodasComoLidasPorLoja(@Param("idUtilizador") Integer idUtilizador, @Param("idLoja") Integer idLoja);
}
