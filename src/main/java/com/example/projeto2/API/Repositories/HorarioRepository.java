package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador AND h.dataTurno >= CURRENT_DATE " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findProximosTurnosPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosPublicadosPorUtilizadorEntreDatas(@Param("idUtilizador") Integer idUtilizador,
                                                                @Param("dataInicio") LocalDate dataInicio,
                                                                @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador " +
            "AND l.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosPublicadosPorUtilizadorELojaEntreDatas(@Param("idUtilizador") Integer idUtilizador,
                                                                      @Param("idLoja") Integer idLoja,
                                                                      @Param("dataInicio") LocalDate dataInicio,
                                                                      @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador " +
            "AND h.dataTurno >= CURRENT_DATE " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "    AND (p.idHorarioOrigem.id = h.id OR p.idHorarioDestino.id = h.id)" +
            ") " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findTurnosDisponiveisParaPermutaPorUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id != :meuId AND h.dataTurno >= CURRENT_DATE " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY h.dataTurno ASC")
    List<Horario> findTurnosDosColegas(@Param("meuId") Integer meuId);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id <> :idUtilizador " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND h.dataTurno = (" +
            "    SELECT origem.dataTurno FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "AND l.id = (" +
            "    SELECT origem.idLojautilizador.idLoja.id FROM Horario origem " +
            "    WHERE origem.id = :idHorarioOrigem" +
            ") " +
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "    AND (p.idHorarioOrigem.id = h.id OR p.idHorarioDestino.id = h.id)" +
            ") " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findTurnosElegiveisParaPermuta(@Param("idUtilizador") Integer idUtilizador,
                                                 @Param("idHorarioOrigem") Integer idHorarioOrigem);

    /**
     * Para Permuta de Folga: dada a seleção do turno-D de Func1 (idHorarioD), devolve os
     * turnos-Y de outros colaboradores que satisfazem simultaneamente:
     *   1. Func2 NÃO tem turno aprovado no dia D (tem folga — pode receber o turno de Func1)
     *   2. Func1 NÃO tem turno aprovado no dia Y (tem folga — pode receber o turno de Func2)
     *   3. Mesma loja que idHorarioD
     *   4. Não estão envolvidos em permutas ou permutas_folga pendentes
     */
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id <> :idFunc1 " +
            "AND l.id = (SELECT hd.idLojautilizador.idLoja.id FROM Horario hd WHERE hd.id = :idHorarioD) " +
            "AND h.dataTurno >= CURRENT_DATE " +
            "AND h.id <> :idHorarioD " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            // Func2 tem folga no dia D (sem turno aprovado nesse dia)
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Horario h2 " +
            "    LEFT JOIN h2.idPropostaHorario ph2 " +
            "    WHERE h2.idLojautilizador.idUtilizador.id = u.id " +
            "    AND h2.dataTurno = (SELECT hd2.dataTurno FROM Horario hd2 WHERE hd2.id = :idHorarioD) " +
            "    AND (ph2 IS NULL OR LOWER(ph2.estado) = 'aprovado') " +
            "    AND (h2.estado IS NULL OR LOWER(CAST(h2.estado AS string)) = 'aprovado') " +
            ") " +
            // Func1 tem folga no dia Y (sem turno aprovado nesse dia)
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Horario h3 " +
            "    LEFT JOIN h3.idPropostaHorario ph3 " +
            "    WHERE h3.idLojautilizador.idUtilizador.id = :idFunc1 " +
            "    AND h3.dataTurno = h.dataTurno " +
            "    AND (ph3 IS NULL OR LOWER(ph3.estado) = 'aprovado') " +
            "    AND (h3.estado IS NULL OR LOWER(CAST(h3.estado AS string)) = 'aprovado') " +
            ") " +
            // Não está numa permuta pendente
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM Permuta p " +
            "    WHERE LOWER(CAST(p.estado AS string)) = 'pendente' " +
            "    AND (p.idHorarioOrigem.id = h.id OR p.idHorarioDestino.id = h.id)" +
            ") " +
            // Não está numa permuta de folga pendente
            "AND NOT EXISTS (" +
            "    SELECT 1 FROM PermutaFolga pf " +
            "    WHERE LOWER(pf.estado) = 'pendente' " +
            "    AND (pf.idHorarioD.id = h.id OR pf.idHorarioY.id = h.id)" +
            ") " +
            "ORDER BY h.dataTurno ASC, u.nome ASC")
    List<Horario> findTurnosElegiveisParaPermutaFolga(@Param("idFunc1") Integer idFunc1,
                                                      @Param("idHorarioD") Integer idHorarioD);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE h.dataTurno = CURRENT_DATE " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND l.id = :idLoja " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findEquipaDeHojeNaLoja(@Param("idLoja") Integer idLoja);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno = :data " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY u.nome ASC, h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosDaLojaNoDia(@Param("idLoja") Integer idLoja,
                                          @Param("data") LocalDate data);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY u.nome ASC, h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                               @Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (:idColaborador IS NULL OR u.id = :idColaborador) " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC, u.nome ASC")
    List<Horario> findHorariosPublicadosDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                         @Param("dataInicio") LocalDate dataInicio,
                                                         @Param("dataFim") LocalDate dataFim,
                                                         @Param("idColaborador") Integer idColaborador);

    /** Context panel variant — includes horarios from any proposal state so the manager
     *  sees the full planned schedule when reviewing a pending request. */
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno = :data " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY u.nome ASC, h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosDeContextoLojaNoDia(@Param("idLoja") Integer idLoja,
                                                   @Param("data") LocalDate data);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY u.nome ASC, h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosDeContextoLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                                        @Param("dataInicio") LocalDate dataInicio,
                                                        @Param("dataFim") LocalDate dataFim);

    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "WHERE h.idPropostaHorario.id = :idPropostaHorario " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC, u.nome ASC")
    List<Horario> findByIdPropostaHorarioId(@Param("idPropostaHorario") Integer idPropostaHorario);

    @Query("SELECT COUNT(h) FROM Horario h " +
            "JOIN h.idLojautilizador lu " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE lu.idLoja.id = :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado')")
    long countHorariosVisiveisDaLojaEntreDatas(@Param("idLoja") Integer idLoja,
                                               @Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    // Employee "who is working with me" view — same store, same day, excluding self
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno = :data " +
            "AND u.id <> :idExcluir " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY t.horaInicio ASC, u.nome ASC")
    List<Horario> findColeaguesDaLojaNoDia(@Param("idLoja") Integer idLoja,
                                           @Param("data") LocalDate data,
                                           @Param("idExcluir") Integer idExcluir);

    // Employee "my own shifts on a given day" — same store, only self
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE l.id = :idLoja " +
            "AND h.dataTurno = :data " +
            "AND u.id = :idUtilizador " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND lu.dataFim IS NULL " +
            "ORDER BY t.horaInicio ASC")
    List<Horario> findMeusTurnosNoDia(@Param("idLoja") Integer idLoja,
                                      @Param("data") LocalDate data,
                                      @Param("idUtilizador") Integer idUtilizador);

    /**
     * Cross-store overlap guard: detects double-booking in OTHER stores only.
     * Intentionally excludes same-store shifts so that multiple rascunho proposals
     * from the same store do not block each other during generation.
     * Only published/approved shifts count as real conflicts — rascunho/pendente
     * proposals from other stores are not yet committed and must not block generation.
     * Returns the conflicting Horario objects for use in error messages.
     */
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id = :idUtilizador " +
            "AND lu.idLoja.id <> :idLoja " +
            "AND h.dataTurno = :data " +
            "AND :horaInicio < t.horaFim " +
            "AND :horaFim > t.horaInicio " +
            "AND (ph IS NULL OR LOWER(ph.estado) = 'aprovado') " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "ORDER BY t.horaInicio")
    List<Horario> findOverlappingShiftsInOtherStores(
            @Param("idUtilizador") Integer idUtilizador,
            @Param("idLoja") Integer idLoja,
            @Param("data") LocalDate data,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFim") LocalTime horaFim);

    /** Legacy count-only variant kept for callers that only need the boolean. */
    @Query("SELECT COUNT(h) FROM Horario h " +
            "WHERE h.idLojautilizador.idUtilizador.id = :idUtilizador " +
            "AND h.dataTurno = :data " +
            "AND :horaInicio < h.idTurno.horaFim " +
            "AND :horaFim > h.idTurno.horaInicio")
    long countGlobalOverlappingShifts(@Param("idUtilizador") Integer idUtilizador,
                                      @Param("data") LocalDate data,
                                      @Param("horaInicio") LocalTime horaInicio,
                                      @Param("horaFim") LocalTime horaFim);

    /**
     * Returns IDs of users from {@code ids} that already have approved or pending
     * shifts in any store other than {@code idLoja} for the given period.
     * Used to warn the manager before generation that those collaborators may
     * cause a cross-store conflict when the proposal is persisted.
     */
    @Query("SELECT DISTINCT lu.idUtilizador.id FROM Horario h " +
            "JOIN h.idLojautilizador lu " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE lu.idUtilizador.id IN :ids " +
            "AND lu.idLoja.id <> :idLoja " +
            "AND h.dataTurno >= :dataInicio AND h.dataTurno <= :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) IN ('pendente', 'aprovado'))")
    Set<Integer> findUtilizadoresComTurnosNoutraLoja(
            @Param("ids") List<Integer> ids,
            @Param("idLoja") Integer idLoja,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /**
     * Returns the actual Horario objects (with shift times) for collaborators
     * in OTHER stores during the generation period. Used by the engine to enforce
     * the 11h inter-store rest rule via respeitaDescansoVizinhanca().
     */
    @Query("SELECT h FROM Horario h " +
            "JOIN FETCH h.idLojautilizador lu " +
            "JOIN FETCH lu.idUtilizador u " +
            "JOIN FETCH lu.idCargo c " +
            "JOIN FETCH lu.idLoja l " +
            "JOIN FETCH h.idTurno t " +
            "LEFT JOIN h.idPropostaHorario ph " +
            "WHERE u.id IN :ids " +
            "AND l.id <> :idLoja " +
            "AND h.dataTurno BETWEEN :dataInicio AND :dataFim " +
            "AND (ph IS NULL OR LOWER(ph.estado) IN ('pendente', 'aprovado')) " +
            "AND (h.estado IS NULL OR LOWER(CAST(h.estado AS string)) = 'aprovado') " +
            "AND t.horaFim IS NOT NULL " +
            "ORDER BY h.dataTurno ASC, t.horaInicio ASC")
    List<Horario> findHorariosPorColaboradoresEmOutrasLojas(
            @Param("ids") List<Integer> ids,
            @Param("idLoja") Integer idLoja,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /** Same as above but excludes a specific Horario row — used when editing an existing record to avoid self-counting. */
    @Query("SELECT COUNT(h) FROM Horario h " +
            "WHERE h.idLojautilizador.idUtilizador.id = :idUtilizador " +
            "AND h.dataTurno = :data " +
            "AND h.id <> :idHorarioExcluir " +
            "AND :horaInicio < h.idTurno.horaFim " +
            "AND :horaFim > h.idTurno.horaInicio")
    long countGlobalOverlappingShiftsExcluding(@Param("idUtilizador") Integer idUtilizador,
                                               @Param("data") LocalDate data,
                                               @Param("horaInicio") LocalTime horaInicio,
                                               @Param("horaFim") LocalTime horaFim,
                                               @Param("idHorarioExcluir") Integer idHorarioExcluir);

    void deleteByIdPropostaHorarioId(Integer idPropostaHorario);

    boolean existsByIdLojautilizadorIdAndDataTurnoAndIdPropostaHorarioId(
            Integer idLojautilizador, LocalDate dataTurno, Integer idPropostaHorario);
}
