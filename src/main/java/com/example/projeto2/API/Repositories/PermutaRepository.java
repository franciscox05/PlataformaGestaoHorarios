package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Permuta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermutaRepository extends JpaRepository<Permuta, Integer> {

    @Query("SELECT p FROM Permuta p " +
            "JOIN FETCH p.idHorarioOrigem ho " +
            "JOIN FETCH ho.idTurno hto " +
            "JOIN FETCH ho.idLojautilizador luo " +
            "JOIN FETCH luo.idUtilizador uo " +
            "JOIN FETCH luo.idLoja lo " +
            "JOIN FETCH p.idHorarioDestino hd " +
            "JOIN FETCH hd.idTurno htd " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "WHERE uo.id = :idUtilizador " +
            "ORDER BY p.dataPedido DESC, p.id DESC")
    List<Permuta> findPedidosEnviadosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT p FROM Permuta p " +
            "JOIN FETCH p.idHorarioOrigem ho " +
            "JOIN FETCH ho.idTurno hto " +
            "JOIN FETCH ho.idLojautilizador luo " +
            "JOIN FETCH luo.idUtilizador uo " +
            "JOIN FETCH luo.idLoja lo " +
            "JOIN FETCH p.idHorarioDestino hd " +
            "JOIN FETCH hd.idTurno htd " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND lo.id = :idLoja " +
            "AND uo.id <> :idUtilizadorAprovador " +
            "ORDER BY p.dataPedido ASC, p.id ASC")
    List<Permuta> findPedidosPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                             @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador);

    @Query("SELECT p FROM Permuta p " +
            "JOIN FETCH p.idHorarioOrigem ho " +
            "JOIN FETCH ho.idTurno hto " +
            "JOIN FETCH ho.idLojautilizador luo " +
            "JOIN FETCH luo.idUtilizador uo " +
            "JOIN FETCH luo.idLoja lo " +
            "JOIN FETCH p.idHorarioDestino hd " +
            "JOIN FETCH hd.idTurno htd " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND lo.id = :idLoja " +
            "AND uo.id <> :idUtilizadorAprovador " +
            "AND ho.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "ORDER BY ho.dataTurno ASC, p.dataPedido ASC, p.id ASC")
    List<Permuta> findPedidosPendentesDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                       @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador,
                                                       @Param("dataInicio") java.time.LocalDate dataInicio,
                                                       @Param("dataFim") java.time.LocalDate dataFim);

    @Query("SELECT p FROM Permuta p " +
            "JOIN FETCH p.idHorarioOrigem ho " +
            "JOIN FETCH ho.idTurno hto " +
            "JOIN FETCH ho.idLojautilizador luo " +
            "JOIN FETCH luo.idUtilizador uo " +
            "JOIN FETCH luo.idLoja lo " +
            "JOIN FETCH p.idHorarioDestino hd " +
            "JOIN FETCH hd.idTurno htd " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "WHERE p.id = :idPermuta")
    java.util.Optional<Permuta> findDetalhadaById(@Param("idPermuta") Integer idPermuta);

    @Query("SELECT COUNT(p) FROM Permuta p " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND p.idHorarioOrigem.idLojautilizador.idLoja.id = :idLoja " +
            "AND p.idHorarioOrigem.idLojautilizador.idUtilizador.id <> :idUtilizadorAprovador")
    long countPedidosPendentesDaLoja(@Param("idLoja") Integer idLoja,
                                     @Param("idUtilizadorAprovador") Integer idUtilizadorAprovador);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Permuta p " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND p.idHorarioOrigem.id = :idHorarioOrigem")
    boolean existsPedidoPendentePorHorarioOrigem(@Param("idHorarioOrigem") Integer idHorarioOrigem);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Permuta p " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND (p.idHorarioOrigem.id = :idHorario OR p.idHorarioDestino.id = :idHorario)")
    boolean existsPedidoPendentePorHorario(@Param("idHorario") Integer idHorario);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Permuta p " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND p.idHorarioOrigem.id = :idHorarioOrigem " +
            "AND p.idHorarioDestino.id = :idHorarioDestino")
    boolean existsPedidoPendentePorOrigemEDestino(@Param("idHorarioOrigem") Integer idHorarioOrigem,
                                                  @Param("idHorarioDestino") Integer idHorarioDestino);

    @Query("SELECT p FROM Permuta p " +
            "JOIN FETCH p.idHorarioOrigem ho " +
            "JOIN FETCH ho.idTurno hto " +
            "JOIN FETCH ho.idLojautilizador luo " +
            "JOIN FETCH luo.idUtilizador uo " +
            "JOIN FETCH luo.idLoja lo " +
            "JOIN FETCH p.idHorarioDestino hd " +
            "JOIN FETCH hd.idTurno htd " +
            "JOIN FETCH hd.idLojautilizador lud " +
            "JOIN FETCH lud.idUtilizador ud " +
            "JOIN FETCH lud.idLoja ld " +
            "WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "AND p.id <> :idPermutaIgnorada " +
            "AND (" +
            "    p.idHorarioOrigem.id IN :idsHorarios " +
            "    OR p.idHorarioDestino.id IN :idsHorarios" +
            ")")
    List<Permuta> findPedidosPendentesConflitantes(@Param("idPermutaIgnorada") Integer idPermutaIgnorada,
                                                   @Param("idsHorarios") java.util.Collection<Integer> idsHorarios);
}
