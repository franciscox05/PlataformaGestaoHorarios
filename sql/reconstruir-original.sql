-- =============================================================================
-- RECONSTRUCAO DA BASE DE DADOS ORIGINAL
-- Restaura: Loja Levis Braga Parque, 6 turnos, 8 colaboradores originais
-- IDs preservados conforme preferencias-demo.sql (2,7,38-43)
-- Correr depois: preferencias-demo.sql
-- =============================================================================

BEGIN;

-- Limpar tudo
TRUNCATE TABLE
    public.eventos_auditoria,
    public.notificacao,
    public.historico_horario_estados,
    public.permutas,
    public.permutas_folga,
    public.horarios_especiais_loja,
    public.horarios,
    public.propostas_horario_mensal,
    public.day_offs,
    public.preferencias,
    public.lojautilizador,
    public.utilizadores,
    public.regras_loja,
    public.regras,
    public.turnos,
    public.cargos,
    public.lojas
RESTART IDENTITY CASCADE;

-- CARGOS
INSERT INTO public.cargos (id_cargo, nome, tipo, descricao) OVERRIDING SYSTEM VALUE VALUES
    (1, 'Gerente de Loja',            'gerente',          'Responsavel maximo pela loja'),
    (2, 'Supervisor de Equipa',        'supervisor',       'Validacao operacional e apoio'),
    (3, 'Sub-Gerente',                 'subgerente',       'Apoio direto a gerencia'),
    (4, 'Assistente de Vendas FT',     'fulltime',         'Vendedor a tempo inteiro'),
    (5, 'Assistente de Vendas PT',     'parttime',         'Vendedor a tempo parcial'),
    (6, 'Reforco Fim de Semana',       'reforco_parttime', 'Apoio de fim de semana');
SELECT setval('public.cargos_id_cargo_seq', 6, true);

-- LOJA ORIGINAL (1 loja)
INSERT INTO public.lojas (id_loja, nome, localizacao, hora_abertura, hora_fecho) OVERRIDING SYSTEM VALUE VALUES
    (1, 'Levis Braga Parque', 'Braga', '10:00', '23:00');
SELECT setval('public.lojas_id_loja_seq', 1, true);

-- REGRAS GLOBAIS
INSERT INTO public.regras (id_regra, descricao, valor_padrao, tipo) OVERRIDING SYSTEM VALUE VALUES
    (1,  'Minimo de funcionarios por turno',                          2,   'operacional'),
    (2,  'Dia limite de lancamento do horario mensal',                15,  'administrativo'),
    (3,  'Maximo de horas consecutivas',                              8,   'legal'),
    (4,  'Carga contratual mensal gestao (horas)',                    176, 'contratual'),
    (5,  'Carga contratual mensal full-time (horas)',                 176, 'contratual'),
    (6,  'Carga contratual mensal part-time (horas)',                 96,  'contratual'),
    (7,  'Carga contratual mensal reforco de fim de semana (horas)',  64,  'contratual'),
    (8,  'Descanso semanal minimo (dias)',                            2,   'descanso'),
    (9,  'Janela de rotacao de fins de semana (semanas)',             2,   'descanso'),
    (10, 'Presenca de gerente ou subgerente aos sabados',             1,   'operacional');
SELECT setval('public.regras_id_regra_seq', 10, true);

-- TURNOS ORIGINAIS (6 turnos: 3 FT + 3 PT)
-- Estes sao os mesmos usados no FluxosCriticosTestSupport.garantirTurnosBase()
INSERT INTO public.turnos (id_turno, tipo, nome, hora_inicio, hora_fim, ativo) OVERRIDING SYSTEM VALUE VALUES
    (1, 'manha',      'Manha FT',        '10:00', '19:00', true),
    (2, 'intermedio', 'Intermedio FT',   '12:00', '21:00', true),
    (3, 'noite',      'Noite FT',        '14:00', '23:00', true),
    (4, 'manha',      'Manha PT',        '10:00', '14:30', true),
    (5, 'intermedio', 'Intermedio PT',   '14:00', '18:30', true),
    (6, 'noite',      'Noite PT',        '18:30', '23:00', true);
SELECT setval('public.turnos_id_turno_seq', 6, true);

-- UTILIZADORES ORIGINAIS com IDs exactos (necessario para preferencias-demo.sql)
INSERT INTO public.utilizadores (id_utilizador, nome, email, telemovel, password_hash, estado) OVERRIDING SYSTEM VALUE VALUES
    (2,  'Tiago Costa',     'tiago.costa@levis.com', '912000002', '123456', 'ativo'),
    (7,  'Francisco Gomes', 'francisco@levis.com',   '912000007', '123456', 'ativo'),
    (38, 'Ana Silva',       'ana@levis.com',          '912000038', '123456', 'ativo'),
    (39, 'Bruno Costa',     'bruno@levis.com',        '912000039', '123456', 'ativo'),
    (40, 'Carla Mendes',    'carla@levis.com',        '912000040', '123456', 'ativo'),
    (41, 'David Ferreira',  'david@levis.com',        '912000041', '123456', 'ativo'),
    (42, 'Eva Rodrigues',   'eva@levis.com',          '912000042', '123456', 'ativo'),
    (43, 'Fabio Santos',    'fabio@levis.com',        '912000043', '123456', 'ativo');
SELECT setval('public.utilizadores_id_utilizador_seq', 43, true);

-- LOJAUTILIZADOR (LU1-8, ordem usada pelo junho-2026-dados-completos.sql)
INSERT INTO public.lojautilizador (id_lojautilizador, id_utilizador, id_loja, id_cargo, data_inicio) OVERRIDING SYSTEM VALUE VALUES
    (1, 7,  1, 1, CURRENT_DATE - 400),  -- Francisco Gomes (gerente)
    (2, 2,  1, 3, CURRENT_DATE - 380),  -- Tiago Costa (subgerente)
    (3, 38, 1, 4, CURRENT_DATE - 300),  -- Ana Silva (fulltime)
    (4, 39, 1, 4, CURRENT_DATE - 280),  -- Bruno Costa (fulltime)
    (5, 40, 1, 5, CURRENT_DATE - 250),  -- Carla Mendes (parttime)
    (6, 41, 1, 5, CURRENT_DATE - 230),  -- David Ferreira (parttime)
    (7, 42, 1, 6, CURRENT_DATE - 200),  -- Eva Rodrigues (reforco_parttime)
    (8, 43, 1, 2, CURRENT_DATE - 180);  -- Fabio Santos (supervisor)
SELECT setval('public.lojautilizador_id_lojautilizador_seq', 8, true);

-- REGRAS DA LOJA 1
INSERT INTO public.regras_loja (id_regra_loja, id_loja, id_regra, valor_especifico, observacoes, ativo) OVERRIDING SYSTEM VALUE VALUES
    (1,  1,  1, 3,   'Loja principal com maior afluencia.', true),
    (2,  1,  2, 12,  'Horario mensal antes da segunda semana.', true),
    (3,  1,  3, 8,   'Limite padrao para turnos longos.', true),
    (4,  1,  4, 176, 'Carga mensal gerencia.', true),
    (5,  1,  5, 176, 'Carga mensal full-time.', true),
    (6,  1,  6, 96,  'Carga mensal part-time.', true),
    (7,  1,  7, 64,  'Carga mensal reforco FDS.', true),
    (8,  1,  8, 2,   'Dois dias de descanso por semana.', true),
    (9,  1,  9, 2,   'Rotacao de fins de semana a cada 2 semanas.', true),
    (10, 1, 10, 1,   'Chefia obrigatoria ao sabado.', true);
SELECT setval('public.regras_loja_id_regra_loja_seq', 10, true);

-- PREFERENCIAS INICIAIS DO BRUNO COSTA (pendentes, serao aprovadas pelo preferencias-demo.sql)
-- ids 1 e 3 sao os que preferencias-demo.sql aprova via UPDATE WHERE id_preferencia IN (1,3)
INSERT INTO public.preferencias (id_preferencia, id_utilizador, descricao, tipo, data_inicio, data_fim, prioridade, estado) OVERRIDING SYSTEM VALUE VALUES
    (1, 39, 'Turnos preferidos: manha.',  'turnos',         '2026-07-01', NULL, 3, 'pendente'),
    (2, 39, 'Folga preferida: Sabado.',   'folga_preferida','2026-07-04', NULL, 3, 'pendente'),
    (3, 39, 'Ana Silva',                  'colegas',         NULL,        NULL, 2, 'pendente');
SELECT setval('public.preferencias_id_preferencia_seq', 3, true);

COMMIT;

-- Verificacao final
SELECT 'CARGOS'          AS tabela, COUNT(*) AS total FROM public.cargos
UNION ALL SELECT 'LOJAS',           COUNT(*) FROM public.lojas
UNION ALL SELECT 'REGRAS',          COUNT(*) FROM public.regras
UNION ALL SELECT 'TURNOS',          COUNT(*) FROM public.turnos
UNION ALL SELECT 'UTILIZADORES',    COUNT(*) FROM public.utilizadores
UNION ALL SELECT 'LOJAUTILIZADOR',  COUNT(*) FROM public.lojautilizador
UNION ALL SELECT 'REGRAS_LOJA',     COUNT(*) FROM public.regras_loja
UNION ALL SELECT 'PREFERENCIAS',    COUNT(*) FROM public.preferencias;

SELECT u.nome, c.tipo AS cargo, u.email
FROM public.lojautilizador lu
JOIN public.utilizadores u ON u.id_utilizador = lu.id_utilizador
JOIN public.cargos c ON c.id_cargo = lu.id_cargo
ORDER BY lu.id_lojautilizador;
