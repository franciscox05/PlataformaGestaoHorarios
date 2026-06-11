-- =============================================================================
-- SEED: Dados de teste completos — Junho 2026
-- Corre DEPOIS de demo-entrega.sql. Não usa TRUNCATE.
-- Usa sequences automáticas (sem IDs fixos) para evitar conflitos.
-- =============================================================================

BEGIN;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. PROPOSTA DE HORÁRIO — Junho 2026 (aprovada, Loja 1)
-- ─────────────────────────────────────────────────────────────────────────────
DO $$
DECLARE
  v_prop_jun INTEGER;
  v_prop_mai INTEGER;
  v_h_orig   INTEGER;
  v_h_dest   INTEGER;
BEGIN

  -- Proposta Jun 2026 aprovada (Loja 1)
  INSERT INTO propostas_horario_mensal
    (id_loja, id_utilizador_geracao, ano, mes, estado,
     resumo_geracao, data_geracao, data_decisao, id_utilizador_decisao)
  VALUES
    (1, 1, 2026, 6, 'aprovado',
     'Horário de Junho 2026 — gerado para testes (cobertura >=5/dia)',
     '2026-05-15 10:00:00', '2026-05-16 14:30:00', 1)
  RETURNING id_proposta_horario INTO v_prop_jun;

  -- Proposta Mai 2026 aprovada (Loja 1) — histórico
  INSERT INTO propostas_horario_mensal
    (id_loja, id_utilizador_geracao, ano, mes, estado,
     resumo_geracao, data_geracao, data_decisao, id_utilizador_decisao)
  VALUES
    (1, 1, 2026, 5, 'aprovado',
     'Horário de Maio 2026 — histórico',
     '2026-04-14 09:00:00', '2026-04-15 11:00:00', 1)
  RETURNING id_proposta_horario INTO v_prop_mai;

  -- Proposta Jul 2026 rascunho (Loja 1)
  INSERT INTO propostas_horario_mensal
    (id_loja, id_utilizador_geracao, ano, mes, estado, data_geracao)
  VALUES (1, 1, 2026, 7, 'rascunho', '2026-06-10 16:00:00');

  -- Proposta Jun 2026 pendente (Loja 2)
  INSERT INTO propostas_horario_mensal
    (id_loja, id_utilizador_geracao, ano, mes, estado, data_geracao)
  VALUES (2, 2, 2026, 6, 'pendente', '2026-05-16 08:00:00');

  -- ─────────────────────────────────────────────────────────────────────────
  -- 2. HORÁRIOS — Junho 2026 (estado='publicado')
  --
  -- Turnos disponíveis (Loja 1):
  --   FT: 1=manha(10-19), 2=intermedio(12-21), 3=noite(14-23)
  --   PT: 4=manha(10-14:30), 5=intermedio(14-18:30), 6=noite(18:30-23)
  --
  -- Folgas por colaborador (2 dias/semana, rotativos):
  --   LU1: 6,7,13,14,20,21,27,28  (Sáb/Dom)
  --   LU2: 1,2,8,9,15,16,22,23    (Seg/Ter)
  --   LU3: 3,4,10,11,17,18,24,25  (Qua/Qui)
  --   LU4: 5,6,12,13,19,20,26,27  (Sex/Sáb)
  --   LU5: 7,8,14,15,21,22,28,29  (Dom/Seg)
  --   LU6: 2,3,9,10,16,17,23,24   (Ter/Qua)
  --   LU7: 4,5,11,12,18,19,25,26  (Qui/Sex)
  --
  -- Cobertura diária: sempre >=5 pessoas (regra da loja: minimo 3)
  -- ─────────────────────────────────────────────────────────────────────────

  -- LU1 — turno 1 (Manha FT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 1, 1, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (6,7,13,14,20,21,27,28);

  -- LU2 — turno 2 (Intermedio FT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 2, 2, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (1,2,8,9,15,16,22,23);

  -- LU3 — turno 3 (Noite FT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 3, 3, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (3,4,10,11,17,18,24,25);

  -- LU4 — turno 1 (Manha FT — reforco de manha)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 4, 1, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (5,6,12,13,19,20,26,27);

  -- LU5 — turno 4 (Manha PT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 5, 4, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (7,8,14,15,21,22,28,29);

  -- LU6 — turno 5 (Intermedio PT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 6, 5, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (2,3,9,10,16,17,23,24);

  -- LU7 — turno 6 (Noite PT)
  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT 7, 6, d::date, 'aprovado', v_prop_jun
  FROM generate_series('2026-06-01'::date, '2026-06-30'::date, '1 day') d
  WHERE EXTRACT(DAY FROM d::date)::int NOT IN (4,5,11,12,18,19,25,26);

  -- ─────────────────────────────────────────────────────────────────────────
  -- 3. HORÁRIOS — Maio 2026 (historico, aprovado, apenas dias uteis)
  -- ─────────────────────────────────────────────────────────────────────────

  INSERT INTO horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
  SELECT lu, turno, d::date, 'aprovado', v_prop_mai
  FROM generate_series('2026-05-01'::date, '2026-05-31'::date, '1 day') d
  CROSS JOIN (VALUES (1,1),(2,2),(3,3),(5,4),(6,5)) AS t(lu, turno)
  WHERE EXTRACT(DOW FROM d::date) NOT IN (0,6);

  -- ─────────────────────────────────────────────────────────────────────────
  -- 4. PERMUTAS — referenciando horarios de Junho 2026
  -- ─────────────────────────────────────────────────────────────────────────

  -- Permuta pendente: LU1 dia 2 <-> LU2 dia 3
  SELECT id_horario INTO v_h_orig FROM horarios
  WHERE id_lojautilizador=1 AND data_turno='2026-06-02' AND estado='aprovado' LIMIT 1;
  SELECT id_horario INTO v_h_dest FROM horarios
  WHERE id_lojautilizador=2 AND data_turno='2026-06-03' AND estado='aprovado' LIMIT 1;
  IF v_h_orig IS NOT NULL AND v_h_dest IS NOT NULL THEN
    INSERT INTO permutas (id_horario_origem, id_horario_destino, estado, data_pedido)
    VALUES (v_h_orig, v_h_dest, 'pendente', '2026-06-01 09:00:00');
  END IF;

  -- Permuta aprovada: LU3 dia 5 <-> LU4 dia 7
  SELECT id_horario INTO v_h_orig FROM horarios
  WHERE id_lojautilizador=3 AND data_turno='2026-06-05' AND estado='aprovado' LIMIT 1;
  SELECT id_horario INTO v_h_dest FROM horarios
  WHERE id_lojautilizador=4 AND data_turno='2026-06-07' AND estado='aprovado' LIMIT 1;
  IF v_h_orig IS NOT NULL AND v_h_dest IS NOT NULL THEN
    INSERT INTO permutas (id_horario_origem, id_horario_destino, estado, data_pedido)
    VALUES (v_h_orig, v_h_dest, 'aprovada', '2026-06-03 14:00:00');
  END IF;

  -- Permuta recusada: LU6 dia 8 <-> LU7 dia 9
  SELECT id_horario INTO v_h_orig FROM horarios
  WHERE id_lojautilizador=6 AND data_turno='2026-06-08' AND estado='aprovado' LIMIT 1;
  SELECT id_horario INTO v_h_dest FROM horarios
  WHERE id_lojautilizador=7 AND data_turno='2026-06-09' AND estado='aprovado' LIMIT 1;
  IF v_h_orig IS NOT NULL AND v_h_dest IS NOT NULL THEN
    INSERT INTO permutas (id_horario_origem, id_horario_destino, estado, data_pedido)
    VALUES (v_h_orig, v_h_dest, 'recusada', '2026-06-05 10:00:00');
  END IF;

END $$;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. PREFERÊNCIAS — varios tipos e estados
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO preferencias (id_utilizador, descricao, tipo, data_inicio, data_fim, prioridade, estado, decisao, id_decisor, data_decisao)
VALUES
  -- folga_preferida (soft — 1 por semana)
  (2, 'Prefiro folga as segundas-feiras',   'folga_preferida', NULL, NULL, 2, 'aprovado',  'Aprovado pelo gerente',                     1, '2026-05-20 10:00:00'),
  (3, 'Gostaria de ter folga as quartas',   'folga_preferida', NULL, NULL, 3, 'pendente',  NULL,                                        NULL, NULL),
  (6, 'Prefiro folga ao fim de semana',     'folga_preferida', NULL, NULL, 2, 'rejeitado', 'Nao e possivel garantir sempre',            1, '2026-05-25 09:00:00'),
  -- turnos
  (1, 'Prefiro turnos da manha (10-19)',    'turnos',          NULL, NULL, 1, 'aprovado',  'Respeitado na geracao',                     1, '2026-05-18 11:00:00'),
  (4, 'Nao consigo fazer turno apos as 23h','turnos',          NULL, NULL, 1, 'pendente',  NULL,                                        NULL, NULL),
  (7, 'Prefiro turno intermedio',           'turnos',          NULL, NULL, 3, 'aprovado',  'Anotado',                                   1, '2026-05-19 15:00:00'),
  -- colegas
  (2, 'Trabalho bem com o colaborador 3',  'colegas',         NULL, NULL, 4, 'pendente',  NULL,                                        NULL, NULL),
  (5, 'Prefiro nao trabalhar com user 7',  'colegas',         NULL, NULL, 3, 'rejeitado', 'Nao e criterio de afetacao',                1, '2026-05-22 14:00:00'),
  -- ferias
  (3, 'Ferias: 15-22 Agosto 2026',         'ferias',  '2026-08-15', '2026-08-22', 2, 'aprovado',  'Aprovado. Marcar no sistema.', 1, '2026-05-30 10:00:00'),
  (6, 'Ferias: 1-7 Julho 2026',            'ferias',  '2026-07-01', '2026-07-07', 2, 'pendente',  NULL,                           NULL, NULL),
  -- folgas (ausencia hard)
  (1, 'Consulta medica dia 18 Junho',      'folgas',  '2026-06-18', '2026-06-18', 1, 'aprovado',  'Aprovado',                          1, '2026-06-10 09:00:00'),
  (4, 'Compromisso pessoal dia 25 Junho',  'folgas',  '2026-06-25', '2026-06-25', 2, 'pendente',  NULL,                                NULL, NULL),
  (7, 'Evento familiar dia 30 Junho',      'folgas',  '2026-06-30', '2026-06-30', 3, 'rejeitado', 'Dia com necessidade de pessoal',    1, '2026-06-09 16:00:00');

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. DAY OFFS — varios tipos e estados
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO day_offs (id_utilizador, data_ausencia, motivo, tipo, estado)
VALUES
  (1, '2026-06-18', 'Consulta medica',          'folgas',  'aprovado'),
  (2, '2026-06-23', 'Folga compensatoria',       'folgas',  'aprovado'),
  (3, '2026-07-01', 'Primeiro dia de ferias',    'ferias',  'aprovado'),
  (5, '2026-06-29', 'Folga de verao aprovada',   'folgas',  'aprovado'),
  (4, '2026-06-25', 'Compromisso pessoal',       'folgas',  'pendente'),
  (6, '2026-07-02', 'Inicio de ferias',          'ferias',  'pendente'),
  (7, '2026-06-30', 'Evento familiar',           'folgas',  'pendente'),
  (1, '2026-06-10', 'Pedido de folga extra',     'folgas',  'recusado'::estado_dayoff_enum),
  (5, '2026-06-15', 'Pedido nao justificado',    'folgas',  'recusado'::estado_dayoff_enum),
  (3, '2026-06-03', 'Baixa por doenca',          'baixa',   'aprovado'),
  (2, '2026-06-04', 'Continuacao de baixa',      'baixa',   'pendente');

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. HORÁRIOS ESPECIAIS DA LOJA
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO horarios_especiais_loja
  (id_loja, data_inicio, data_fim, descricao, loja_encerrada, hora_abertura, hora_fecho, minimo_colaboradores_turno)
VALUES
  (1, '2026-06-24', '2026-06-24', 'Festa de Sao Joao — horario reduzido', FALSE, '12:00', '22:00', 2),
  (1, '2026-07-01', '2026-07-01', 'Inventario anual — loja encerrada',    TRUE,  NULL,    NULL,    NULL),
  (1, '2026-07-15', '2026-07-31', 'Promocoes de verao — reforco',         FALSE, '09:00', '23:00', 4),
  (2, '2026-07-06', '2026-07-10', 'Obras Loja 2 — encerrada',             TRUE,  NULL,    NULL,    NULL);

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. EVENTOS DE AUDITORIA
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO eventos_auditoria
  (data_evento, tipo_evento, id_utilizador, email_referencia, detalhes, resultado, origem)
VALUES
  ('2026-05-16 14:30:00', 'horario_aprovado',      1, 'admin@loja.pt',  'Proposta Jun 2026 aprovada',         'sucesso', 'desktop'),
  ('2026-05-20 10:05:00', 'preferencia_aprovada',  1, 'admin@loja.pt',  'Preferencia turno manha (user 1)',   'sucesso', 'desktop'),
  ('2026-05-25 09:02:00', 'preferencia_rejeitada', 1, 'admin@loja.pt',  'Preferencia folga FDS (user 6)',     'sucesso', 'desktop'),
  ('2026-06-01 09:10:00', 'permuta_criada',         2, 'colab2@loja.pt', 'Permuta LU1<->LU2 Jun',             'sucesso', 'web'),
  ('2026-06-03 14:05:00', 'permuta_aprovada',       1, 'admin@loja.pt',  'Permuta LU3<->LU4 aprovada',        'sucesso', 'desktop'),
  ('2026-06-05 10:01:00', 'permuta_recusada',       1, 'admin@loja.pt',  'Permuta LU6<->LU7 recusada',        'sucesso', 'desktop'),
  ('2026-06-10 09:05:00', 'dayoff_aprovado',        1, 'admin@loja.pt',  'Day-off 18 Jun para user 1',        'sucesso', 'desktop'),
  ('2026-06-10 16:00:00', 'horario_gerado',         1, 'admin@loja.pt',  'Rascunho Jul 2026 criado',          'sucesso', 'desktop'),
  ('2026-06-11 00:00:00', 'login',                  1, 'admin@loja.pt',  'Login desktop',                     'sucesso', 'desktop');

COMMIT;
