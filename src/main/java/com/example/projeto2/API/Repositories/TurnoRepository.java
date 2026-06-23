package com.example.projeto2.API.Repositories;

import com.example.projeto2.API.Modules.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer> {

    List<Turno> findAllByOrderByHoraInicioAsc();

    @Query("SELECT t FROM Turno t WHERE t.ativo = true ORDER BY t.horaInicio ASC")
    List<Turno> findAllAtivosOrderByHoraInicioAsc();

    // ── Scoping por loja (id_loja NULL = turno global, visível a todas as lojas) ──

    /** Todos os turnos CORRENTES (não arquivados) visíveis para a loja, ordenados. */
    @Query("""
            SELECT t FROM Turno t
            WHERE (t.idLoja IS NULL OR t.idLoja.id = :idLoja)
              AND t.dataFimVigencia IS NULL
            ORDER BY t.horaInicio ASC
            """)
    List<Turno> findParaLojaOrderByHoraInicioAsc(@Param("idLoja") Integer idLoja);

    /** Turnos ATIVOS CORRENTES (não arquivados) visíveis para a loja, ordenados. */
    @Query("""
            SELECT t FROM Turno t
            WHERE t.ativo = true AND (t.idLoja IS NULL OR t.idLoja.id = :idLoja)
              AND t.dataFimVigencia IS NULL
            ORDER BY t.horaInicio ASC
            """)
    List<Turno> findAtivosParaLojaOrderByHoraInicioAsc(@Param("idLoja") Integer idLoja);

    /**
     * Turnos ATIVOS da loja (globais + próprios) cuja vigência se sobrepõe ao período
     * {@code [dataInicio, dataFim]}. Vigência aberta (datas null) conta como sempre
     * válida desse lado. É a query que a geração usa para escolher a versão certa de
     * cada mês — versões arquivadas (data_fim_vigencia anterior ao período) ficam de fora.
     */
    @Query("""
            SELECT t FROM Turno t
            WHERE t.ativo = true
              AND (t.idLoja IS NULL OR t.idLoja.id = :idLoja)
              AND (t.dataInicioVigencia IS NULL OR t.dataInicioVigencia <= :dataFim)
              AND (t.dataFimVigencia IS NULL OR t.dataFimVigencia >= :dataInicio)
            ORDER BY t.horaInicio ASC
            """)
    List<Turno> findAtivosVigentesParaLojaNoPeriodo(@Param("idLoja") Integer idLoja,
                                                    @Param("dataInicio") LocalDate dataInicio,
                                                    @Param("dataFim") LocalDate dataFim);

    @Query(value = "SELECT COUNT(*) > 0 FROM horarios h " +
            "WHERE h.id_turno = :idTurno " +
            "AND h.data_turno >= CURRENT_DATE " +
            "AND LOWER(h.estado) IN ('aprovado', 'publicado')",
            nativeQuery = true)
    boolean existeEmHorariosFuturosAprovados(@Param("idTurno") Integer idTurno);

    @Query("""
            SELECT t FROM Turno t
            WHERE t.horaInicio < :horaFim AND t.horaFim > :horaInicio
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findSobrepostos(@Param("horaInicio") LocalTime horaInicio,
                                @Param("horaFim") LocalTime horaFim,
                                @Param("idExcluir") Integer idExcluir);

    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM Horario h WHERE h.idTurno.id = :idTurno")
    boolean existeEmHorarios(@Param("idTurno") Integer idTurno);

    // ── Validações loja-scoped (ignoram versões arquivadas para não colidir com o copy-on-write) ──

    /**
     * Turnos correntes (não arquivados) visíveis para a loja com o mesmo nome.
     * Ignora versões com {@code data_fim_vigencia} preenchida — uma versão antiga
     * arquivada não deve bloquear a criação da versão nova com o mesmo nome.
     */
    @Query("""
            SELECT t FROM Turno t
            WHERE LOWER(TRIM(t.nome)) = LOWER(TRIM(:nome))
              AND (t.idLoja IS NULL OR t.idLoja.id = :idLoja)
              AND t.dataFimVigencia IS NULL
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findCorrentesPorNomeParaLoja(@Param("nome") String nome,
                                             @Param("idLoja") Integer idLoja,
                                             @Param("idExcluir") Integer idExcluir);

    /**
     * Turnos correntes (não arquivados) visíveis para a loja com o intervalo de tempo
     * EXATAMENTE igual. Ignora versões arquivadas, pela mesma razão.
     */
    @Query("""
            SELECT t FROM Turno t
            WHERE t.horaInicio = :horaInicio AND t.horaFim = :horaFim
              AND (t.idLoja IS NULL OR t.idLoja.id = :idLoja)
              AND t.dataFimVigencia IS NULL
              AND (:idExcluir IS NULL OR t.id <> :idExcluir)
            """)
    List<Turno> findCorrentesPorIntervaloParaLoja(@Param("horaInicio") LocalTime horaInicio,
                                                  @Param("horaFim") LocalTime horaFim,
                                                  @Param("idLoja") Integer idLoja,
                                                  @Param("idExcluir") Integer idExcluir);
}
