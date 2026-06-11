package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.PermutaFolga;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermutaFolgaRepository extends JpaRepository<PermutaFolga, Integer> {

    /** Pedidos enviados pelo Func1 (dono do horario_d). */
    @Query("SELECT pf FROM PermutaFolga pf " +
            "JOIN FETCH pf.idHorarioD hd " +
            "JOIN FETCH hd.idTurno td " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "JOIN FETCH pf.idHorarioY hy " +
            "JOIN FETCH hy.idTurno ty " +
            "JOIN FETCH hy.idLojautilizador luy " +
            "JOIN FETCH luy.idUtilizador uy " +
            "WHERE ud.id = :idUtilizador " +
            "ORDER BY pf.dataPedido DESC, pf.id DESC")
    List<PermutaFolga> findPedidosEnviadosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    /** Pedidos pendentes da loja para o aprovador (excluindo os seus próprios). */
    @Query("SELECT pf FROM PermutaFolga pf " +
            "JOIN FETCH pf.idHorarioD hd " +
            "JOIN FETCH hd.idTurno td " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "JOIN FETCH pf.idHorarioY hy " +
            "JOIN FETCH hy.idTurno ty " +
            "JOIN FETCH hy.idLojautilizador luy " +
            "JOIN FETCH luy.idUtilizador uy " +
            "WHERE LOWER(pf.estado) = 'pendente' " +
            "AND ld.id = :idLoja " +
            "AND ud.id <> :idAprovador " +
            "ORDER BY pf.dataPedido ASC, pf.id ASC")
    List<PermutaFolga> findPedidosPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                                  @Param("idAprovador") Integer idAprovador);

    @Query("SELECT pf FROM PermutaFolga pf " +
            "JOIN FETCH pf.idHorarioD hd " +
            "JOIN FETCH hd.idTurno td " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "JOIN FETCH pf.idHorarioY hy " +
            "JOIN FETCH hy.idTurno ty " +
            "JOIN FETCH hy.idLojautilizador luy " +
            "JOIN FETCH luy.idUtilizador uy " +
            "WHERE pf.id = :id")
    Optional<PermutaFolga> findDetalhadaById(@Param("id") Integer id);

    @Query("SELECT COUNT(pf) FROM PermutaFolga pf " +
            "WHERE LOWER(pf.estado) = 'pendente' " +
            "AND pf.idHorarioD.idLojautilizador.idLoja.id = :idLoja " +
            "AND pf.idHorarioD.idLojautilizador.idUtilizador.id <> :idAprovador")
    long countPedidosPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                     @Param("idAprovador") Integer idAprovador);

    @Query("SELECT CASE WHEN COUNT(pf) > 0 THEN true ELSE false END FROM PermutaFolga pf " +
            "WHERE LOWER(pf.estado) = 'pendente' " +
            "AND (pf.idHorarioD.id = :idHorario OR pf.idHorarioY.id = :idHorario)")
    boolean existsPendentePorHorario(@Param("idHorario") Integer idHorario);

    /** Pedidos pendentes que partilham algum dos horários indicados (para rejeição em cascata). */
    @Query("SELECT pf FROM PermutaFolga pf " +
            "JOIN FETCH pf.idHorarioD hd " +
            "JOIN FETCH hd.idTurno td " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH pf.idHorarioY hy " +
            "JOIN FETCH hy.idTurno ty " +
            "JOIN FETCH hy.idLojautilizador luy " +
            "JOIN FETCH luy.idUtilizador uy " +
            "WHERE LOWER(pf.estado) = 'pendente' " +
            "AND pf.id <> :idIgnorado " +
            "AND (pf.idHorarioD.id IN :idsHorarios OR pf.idHorarioY.id IN :idsHorarios)")
    List<PermutaFolga> findPendentesConflitantes(@Param("idIgnorado") Integer idIgnorado,
                                                 @Param("idsHorarios") Collection<Integer> idsHorarios);
}
