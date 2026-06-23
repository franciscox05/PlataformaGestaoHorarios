-- ============================================================================
-- Limpeza de turnos duplicados (junho 2026)
-- ============================================================================
-- Contexto: historicamente os turnos eram globais e sem restricao de unicidade,
-- pelo que se acumularam turnos com o MESMO nome, MESMAS horas e MESMA loja
-- (ou globais). Este script funde cada grupo de duplicados num turno canonico:
--   * canonico   = o de menor id_turno do grupo (id_loja, nome, hora_inicio, hora_fim)
--   * duplicados = os restantes do grupo
-- Os horarios que apontavam para um duplicado sao repointados para o canonico,
-- e os duplicados (agora sem referencias) sao removidos. NENHUM horario publicado
-- e perdido. Idempotente: correr varias vezes nao causa dano.
--
-- Agrupamento: COALESCE(id_loja, -1) garante que turnos globais (id_loja NULL)
-- so se fundem entre si. So se fundem turnos com nome E horas iguais.
--
-- DICA: corre primeiro a CONSULTA DE PRE-VISUALIZACAO (em baixo, comentada) para
-- veres o que vai ser fundido antes de aplicar.
-- ============================================================================

BEGIN;

-- 1) Mapa duplicado -> canonico
CREATE TEMP TABLE _turnos_dups ON COMMIT DROP AS
SELECT t.id_turno AS id_duplicado,
       (SELECT MIN(c.id_turno)
          FROM public.turnos c
         WHERE COALESCE(c.id_loja, -1) = COALESCE(t.id_loja, -1)
           AND LOWER(TRIM(COALESCE(c.nome, ''))) = LOWER(TRIM(COALESCE(t.nome, '')))
           AND c.hora_inicio = t.hora_inicio
           AND c.hora_fim    = t.hora_fim
       ) AS id_canonico
FROM public.turnos t;

-- Mantem so as linhas que sao mesmo duplicadas (existe um canonico de id menor).
DELETE FROM _turnos_dups WHERE id_canonico IS NULL OR id_canonico = id_duplicado;

-- 2) Repointa os horarios dos duplicados para o turno canonico.
UPDATE public.horarios h
   SET id_turno = m.id_canonico
  FROM _turnos_dups m
 WHERE h.id_turno = m.id_duplicado;

-- 3) Remove os turnos duplicados (ja sem referencias).
DELETE FROM public.turnos t
 USING _turnos_dups m
 WHERE t.id_turno = m.id_duplicado;

COMMIT;

-- ============================================================================
-- PRE-VISUALIZACAO (opcional) — corre isto ANTES, fora da transacao, para veres
-- quais os grupos com duplicados:
--
-- SELECT COALESCE(id_loja, -1) AS loja, LOWER(TRIM(nome)) AS nome,
--        hora_inicio, hora_fim, COUNT(*) AS qtd,
--        MIN(id_turno) AS canonico, ARRAY_AGG(id_turno ORDER BY id_turno) AS ids
--   FROM public.turnos
--  GROUP BY COALESCE(id_loja, -1), LOWER(TRIM(nome)), hora_inicio, hora_fim
-- HAVING COUNT(*) > 1
--  ORDER BY loja, nome, hora_inicio;
-- ============================================================================
