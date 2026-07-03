-- ══════════════════════════════════════════════════════════════════════════════
-- PITCH GNG — PlataformaGestaoHorarios Levi's  (2026-06-25)
-- Setup completo para demonstração ao vivo ao investidor:
--   • Junho 2026  → Horário publicado (mês corrente, visível no ecrã inicial)
--   • Julho 2026  → Horário publicado (planeamento antecipado)
--   • Agosto 2026 → Dados preparados, horário para gerar AO VIVO durante o pitch
--   • Todos os 5 tipos de preferências por colaborador
--   • Folgas / férias / permutas com estados mistos (para aprovar em directo)
--   • Evento especial: feriado 15-Ago reforçado (mínimo 3 por turno)
--   • Cenário multi-loja: Braga Parque (1) + NorteShopping (2)
--
-- Contas demo  →  password: 123456
--   francisco.gomes@levis.com    Gerente Braga Parque + NorteShopping
--   francisco@levis.com          Sub-Gerente / acesso total
--   henrique.siano@levis.com     Assistente de Vendas FT
-- ══════════════════════════════════════════════════════════════════════════════

BEGIN;

-- ─── LIMPAR TUDO ──────────────────────────────────────────────────────────────
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

-- ─── CARGOS ───────────────────────────────────────────────────────────────────
INSERT INTO public.cargos (id_cargo, nome, tipo, descricao) VALUES
    (1, 'Gerente de Loja',          'gerente',          'Responsável máximo pela loja'),
    (2, 'Supervisor de Equipa',     'supervisor',        'Validação operacional e apoio à gerência'),
    (3, 'Sub-Gerente',              'subgerente',        'Apoio direto à gerência e gestão diária'),
    (4, 'Assistente de Vendas FT',  'fulltime',          'Vendedor a tempo inteiro — 176 h/mês'),
    (5, 'Assistente de Vendas PT',  'parttime',          'Vendedor a tempo parcial — 88 h/mês'),
    (6, 'Reforço Fim de Semana',    'reforco_parttime',  'Apoio de fim de semana — 64 h/mês');

-- ─── LOJAS ────────────────────────────────────────────────────────────────────
INSERT INTO public.lojas (id_loja, nome, localizacao, hora_abertura, hora_fecho) VALUES
    (1, 'Levi''s Braga Parque',   'Braga Parque',     '10:00', '23:00'),
    (2, 'Levi''s NorteShopping',  'Porto',             '10:00', '23:00'),
    (3, 'Levi''s Colombo',        'Lisboa Colombo',    '10:00', '23:00'),
    (4, 'Levi''s Vasco da Gama',  'Lisboa V. Gama',   '09:00', '22:00');

-- ─── REGRAS ───────────────────────────────────────────────────────────────────
INSERT INTO public.regras (id_regra, descricao, valor_padrao, tipo) VALUES
    (1,  'Mínimo de funcionários por turno',                          2,   'operacional'),
    (2,  'Dia limite de lançamento do horário mensal',                 15,  'administrativo'),
    (3,  'Máximo de dias consecutivos de trabalho',                   6,   'legal'),
    (4,  'Carga contratual mensal gestão (horas)',                    176, 'contratual'),
    (5,  'Carga contratual mensal full-time (horas)',                 176, 'contratual'),
    (6,  'Carga contratual mensal part-time (horas)',                 88,  'contratual'),
    (7,  'Carga contratual mensal reforço fim de semana (horas)',     64,  'contratual'),
    (8,  'Descanso semanal mínimo (dias)',                            2,   'descanso'),
    (9,  'Janela de rotação de fins de semana (semanas)',             7,   'descanso'),
    (10, 'Presença de gerente ou subgerente ao sábado',               1,   'operacional'),
    (11, 'Descanso mínimo entre turnos consecutivos (horas)',         11,  'legal');

INSERT INTO public.regras_loja (id_loja, id_regra, valor_especifico, observacoes, ativo) VALUES
    -- Braga Parque
    (1,  1, 2,   'Mínimo de 2 colaboradores por turno.', true),
    (1,  2, 12,  'Horário fechado até ao dia 12 para comunicação atempada.', true),
    (1,  3, 6,   'Máximo de 6 dias consecutivos (CT art. 211).', true),
    (1,  4, 176, 'Carga mensal gestão: 22 dias × 8 h.', true),
    (1,  5, 176, 'Carga mensal full-time: 22 dias × 8 h.', true),
    (1,  6, 88,  'Carga mensal part-time: 22 dias × 4 h.', true),
    (1,  7, 64,  'Carga mensal reforço FDS.', true),
    (1,  8, 2,   'Dois dias de descanso por semana.', true),
    (1,  9, 7,   'Rotação de fins de semana a cada 7 semanas.', true),
    (1, 10, 1,   'Presença de chefia obrigatória ao sábado.', true),
    (1, 11, 11,  'Descanso mínimo de 11 h entre jornadas (CT art. 214).', true),
    -- NorteShopping (equipa mais reduzida)
    (2,  1, 1,   'Equipa reduzida: mínimo de 1 colaborador por turno.', true),
    (2,  3, 6,   'Máximo de 6 dias consecutivos.', true),
    (2,  5, 176, 'Carga mensal FT.', true),
    (2,  6, 88,  'Carga mensal PT.', true),
    (2,  7, 64,  'Carga mensal reforço FDS.', true),
    (2,  8, 2,   'Descanso semanal mínimo.', true),
    (2,  9, 7,   'Rotação de fins de semana.', true),
    (2, 10, 1,   'Chefia obrigatória ao sábado.', true),
    (2, 11, 11,  'Descanso mínimo 11 h entre turnos.', true);

-- ─── TURNOS ───────────────────────────────────────────────────────────────────
INSERT INTO public.turnos (id_turno, tipo, nome, hora_inicio, hora_fim, ativo) VALUES
    (1, 'manha',      'Manhã FT',       '10:00', '19:00', true),
    (2, 'intermedio', 'Intermédio FT',  '12:00', '21:00', true),
    (3, 'noite',      'Noite FT',       '14:00', '23:00', true),
    (4, 'manha',      'Manhã PT',       '10:00', '14:00', true),
    (5, 'intermedio', 'Intermédio PT',  '14:00', '18:00', true),
    (6, 'noite',      'Noite PT',       '19:00', '23:00', true);

-- ─── UTILIZADORES ─────────────────────────────────────────────────────────────
-- IDs explícitos para manter compatibilidade com referências da aplicação
INSERT INTO public.utilizadores (id_utilizador, nome, email, telemovel, password_hash, estado) VALUES
    -- Braga Parque — gestão
    (1,  'Francisco Gomes',   'francisco.gomes@levis.com',   '912000001', '123456', 'ativo'),
    (2,  'Tiago Costa',       'tiago.costa@levis.com',       '912000002', '123456', 'ativo'),
    (7,  'Francisco (Tu)',    'francisco@levis.com',          '912000007', '123456', 'ativo'),
    -- Braga Parque — FT
    (3,  'Henrique Siano',    'henrique.siano@levis.com',    '912000003', '123456', 'ativo'),
    (15, 'Rita Mendes',       'rita.mendes@levis.com',       '912000015', '123456', 'ativo'),
    (16, 'Pedro Luz',         'pedro.luz@levis.com',         '912000016', '123456', 'ativo'),
    (17, 'Sara Ferreira',     'sara.ferreira@levis.com',     '912000017', '123456', 'ativo'),
    (18, 'Joao Alves',        'joao.alves@levis.com',        '912000018', '123456', 'ativo'),
    (19, 'Catarina Cruz',     'catarina.cruz@levis.com',     '912000019', '123456', 'ativo'),
    (20, 'Miguel Rocha',      'miguel.rocha@levis.com',      '912000020', '123456', 'ativo'),
    (21, 'Ines Silva',        'ines.silva@levis.com',        '912000021', '123456', 'ativo'),
    (22, 'Luis Pinto',        'luis.pinto@levis.com',        '912000022', '123456', 'ativo'),
    (25, 'Marco Dias',        'marco.dias@levis.com',        '912000025', '123456', 'ativo'),
    -- Braga Parque — PT
    (4,  'Tiago Eiras',       'tiago.eiras@levis.com',       '912000004', '123456', 'ativo'),
    (5,  'Afonso Barbosa',    'afonso.barbosa@levis.com',    '912000005', '123456', 'ativo'),
    -- Braga Parque — Reforço FDS
    (6,  'Micael Martins',    'micael.martins@levis.com',    '912000006', '123456', 'ativo'),
    -- NorteShopping
    (8,  'Ana Sousa',         'ana.sousa@levis.com',         '912000008', '123456', 'ativo'),
    (11, 'Sofia Marques',     'sofia.marques@levis.com',     '912000011', '123456', 'ativo'),
    (12, 'Diogo Faria',       'diogo.faria@levis.com',       '912000012', '123456', 'ativo'),
    (13, 'Marta Pinto',       'marta.pinto@levis.com',       '912000013', '123456', 'ativo'),
    (14, 'Rui Castro',        'rui.castro@levis.com',        '912000014', '123456', 'ativo'),
    (23, 'Vera Lopes',        'vera.lopes@levis.com',        '912000023', '123456', 'ativo'),
    (24, 'Nuno Santos',       'nuno.santos@levis.com',       '912000024', '123456', 'ativo');

-- ─── LOJA-UTILIZADOR ──────────────────────────────────────────────────────────
-- lu_id explícitos (mesmos do demo-entrega.sql) para evitar remap nas queries
INSERT INTO public.lojautilizador
    (id_lojautilizador, id_utilizador, id_loja, id_cargo, data_inicio) VALUES
    -- Braga Parque — gestão
    (1,  1,  1, 1, '2024-06-01'),  -- Francisco Gomes   gerente
    (2,  2,  1, 2, '2024-09-01'),  -- Tiago Costa        supervisor
    (3,  3,  1, 4, '2024-10-01'),  -- Henrique Siano     FT
    (4,  4,  1, 5, '2025-01-15'),  -- Tiago Eiras        PT
    (5,  5,  1, 5, '2025-01-15'),  -- Afonso Barbosa     PT
    (6,  6,  1, 6, '2025-03-01'),  -- Micael Martins     reforço FDS
    (7,  7,  1, 3, '2026-06-01'),  -- Francisco (Tu)     sub-gerente
    -- Braga Parque — FT adicional
    (16, 15, 1, 4, '2025-02-01'),  -- Rita Mendes
    (17, 16, 1, 4, '2025-02-15'),  -- Pedro Luz
    (18, 17, 1, 4, '2025-03-01'),  -- Sara Ferreira
    (19, 18, 1, 4, '2025-03-15'),  -- Joao Alves
    (20, 19, 1, 4, '2025-04-01'),  -- Catarina Cruz
    (21, 20, 1, 4, '2025-04-15'),  -- Miguel Rocha
    (22, 21, 1, 4, '2025-05-01'),  -- Ines Silva
    (23, 22, 1, 4, '2025-05-15'),  -- Luis Pinto
    (26, 25, 1, 4, '2025-11-15'),  -- Marco Dias (multi-loja → loja 1)
    -- NorteShopping
    (8,  8,  2, 3, '2025-01-01'),  -- Ana Sousa          sub-gerente loja 2
    (11, 1,  2, 1, '2026-06-01'),  -- Francisco Gomes    gerente loja 2 (multi-loja)
    (12, 11, 2, 2, '2024-08-01'),  -- Sofia Marques      supervisor loja 2
    (13, 12, 2, 4, '2024-09-01'),  -- Diogo Faria        FT loja 2
    (14, 13, 2, 4, '2024-10-01'),  -- Marta Pinto        FT loja 2
    (15, 14, 2, 5, '2025-01-01'),  -- Rui Castro         PT loja 2
    (24, 23, 2, 4, '2025-06-01'),  -- Vera Lopes         FT loja 2
    (25, 24, 2, 4, '2025-06-15'),  -- Nuno Santos        FT loja 2
    (27, 25, 2, 4, '2025-11-15');  -- Marco Dias         FT loja 2 (multi-loja)

-- ─── PROPOSTAS DE HORÁRIO ──────────────────────────────────────────────────────
-- proposta 1: Junho 2026 loja 1 (mês corrente → visível no ecrã inicial)
-- proposta 2: Julho 2026 loja 1 (mês seguinte, já publicado)
-- proposta 3: Julho 2026 loja 2 (demonstração multi-loja)
INSERT INTO public.propostas_horario_mensal
    (id_proposta_horario, id_loja, id_utilizador_geracao, ano, mes, estado,
     resumo_geracao, data_geracao, id_utilizador_decisao, data_decisao, observacoes_supervisor)
VALUES
    (1, 1, 1, 2026, 6, 'aprovado',
     'Geração automática concluída. 16 colaboradores, 30 dias, min 2/turno respeitado.',
     '2026-05-28 10:14:00', 2, '2026-05-30 15:22:00',
     'Horário aprovado sem alterações. Boa distribuição dos turnos.'),

    (2, 1, 1, 2026, 7, 'aprovado',
     'Geração automática concluída. 16 colaboradores, 31 dias, incluídas férias de Pedro Luz (14-18 Jul).',
     '2026-06-10 09:05:00', 2, '2026-06-12 11:40:00',
     'Aprovado. Férias de Pedro Luz devidamente acomodadas na geração.'),

    (3, 2, 1, 2026, 7, 'aprovado',
     'Geração automática NorteShopping. 8 colaboradores, 31 dias.',
     '2026-06-10 09:30:00', 11, '2026-06-12 14:00:00',
     'Horário NorteShopping aprovado. Cobertura mínima garantida em todos os turnos.');

-- ══════════════════════════════════════════════════════════════════════════════
-- HORÁRIOS — JUNHO 2026 — LOJA 1 (proposta 1)
-- DOW: 0=Dom  1=Seg  2=Ter  3=Qua  4=Qui  5=Sex  6=Sáb
-- Junho 2026 começa a Segunda-feira (1)
-- ══════════════════════════════════════════════════════════════════════════════

-- lu1 FG  (gerente, manhã FT turno 1)  — descanso Seg+Ter
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 1, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 2);

-- lu2 TC  (supervisor, intermédio FT turno 2)  — descanso Dom+Seg
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 2, 2, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (0, 1);

-- lu3 HS  (FT, manhã turno 1)  — descanso Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 3, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

-- lu4 TE  (PT, manhã PT turno 4)  — descanso Qui+Sex
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 4, 4, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (4, 5);

-- lu5 AB  (PT, intermédio PT turno 5)  — descanso Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 5, 5, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

-- lu6 MM  (reforço FDS, manhã turno 1)  — APENAS Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 6, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) IN (6, 0);

-- lu7 FT  (sub-gerente, manhã turno 1)  — descanso Ter+Qua
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 7, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 3);

-- lu16 RM (FT, noite turno 3)  — descanso Sex+Sáb
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 16, 3, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- lu17 PL (FT, intermédio turno 2)  — descanso Qua+Qui
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 17, 2, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (3, 4);

-- lu18 SF (FT, manhã turno 1)  — descanso Ter+Qua
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 18, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 3);

-- lu19 JA (FT, noite turno 3)  — descanso Seg+Qua
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 19, 3, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 3);

-- lu20 CC (FT, intermédio turno 2)  — descanso Qui+Sex
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 20, 2, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (4, 5);

-- lu21 MR (FT, manhã turno 1)  — descanso Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 21, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

-- lu22 IS (FT, intermédio turno 2)  — descanso Seg+Ter
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 22, 2, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 2);

-- lu23 LP (FT, noite turno 3)  — descanso Ter+Qui
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 23, 3, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 4);

-- lu26 MD (FT, manhã turno 1)  — descanso Sex+Sáb
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 26, 1, d::date, 'aprovado', 1
FROM generate_series('2026-06-01'::date,'2026-06-30'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- ══════════════════════════════════════════════════════════════════════════════
-- HORÁRIOS — JULHO 2026 — LOJA 1 (proposta 2)
-- Julho 2026 começa a Quarta-feira (3)
-- Pedro Luz tem férias aprovadas: 14-18 Jul → excluídos do horário
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 1, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 2);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 2, 2, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (0, 1);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 3, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 4, 4, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (4, 5);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 5, 5, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 6, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) IN (6, 0);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 7, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 3);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 16, 3, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- Pedro Luz — excluir dias de férias 14-18 Jul adicionalmente
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 17, 2, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (3, 4)
  AND d NOT BETWEEN '2026-07-14' AND '2026-07-18';

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 18, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 3);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 19, 3, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 3);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 20, 2, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (4, 5);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 21, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 22, 2, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 2);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 23, 3, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 4);

INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 26, 1, d::date, 'aprovado', 2
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- ══════════════════════════════════════════════════════════════════════════════
-- HORÁRIOS — JULHO 2026 — LOJA 2 (proposta 3)
-- Equipa NorteShopping — cobertura mínima 1 por turno
-- ══════════════════════════════════════════════════════════════════════════════

-- lu8 Ana (sub-gerente, manhã turno 1) — descanso Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 8, 1, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

-- lu12 Sofia (supervisor, noite turno 3) — descanso Seg+Ter
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 12, 3, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (1, 2);

-- lu13 Diogo (FT, manhã turno 1) — descanso Qua+Qui
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 13, 1, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (3, 4);

-- lu14 Marta (FT, intermédio turno 2) — descanso Sex+Sáb
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 14, 2, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- lu15 Rui (PT, manhã PT turno 4) — descanso Sáb+Dom
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 15, 4, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (6, 0);

-- lu24 Vera (FT, manhã turno 1) — descanso Ter+Qua
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 24, 1, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (2, 3);

-- lu25 Nuno (FT, intermédio turno 2) — descanso Qui+Sex
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 25, 2, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (4, 5);

-- lu27 Marco (FT, manhã turno 1) — descanso Sex+Sáb
INSERT INTO public.horarios (id_lojautilizador, id_turno, data_turno, estado, id_proposta_horario)
SELECT 27, 1, d::date, 'aprovado', 3
FROM generate_series('2026-07-01'::date,'2026-07-31'::date,'1 day') d
WHERE EXTRACT(DOW FROM d) NOT IN (5, 6);

-- ══════════════════════════════════════════════════════════════════════════════
-- PREFERÊNCIAS  —  mínimo 1 de cada tipo por colaborador
-- Tipos válidos: folga_preferida | turnos | colegas | folgas | ferias
-- SEM coluna prioridade (foi removida)
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO public.preferencias
    (id_utilizador, tipo, data_inicio, data_fim, descricao, estado, decisao, id_decisor, data_decisao)
VALUES
-- ── Francisco Gomes (id=1) ────────────────────────────────────────────────────
(1,'folga_preferida','2026-06-01','2026-08-31',
 'Prefiro descansar às segundas-feiras para acompanhar reuniões de abertura semanais.',
 'aprovado','Folga preferida de segunda-feira confirmada.',1,'2026-05-20 10:00:00'),

(1,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h) para gerir a loja durante a maior afluência de clientes.',
 'aprovado','Preferência de turno manhã aprovada para a equipa de gestão.',1,'2026-05-20 10:00:00'),

(1,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Tiago Costa nas manhãs de sábado para garantir supervisão reforçada.',
 'aprovado','Pareamento preferencial aprovado.',1,'2026-05-20 10:00:00'),

(1,'folgas','2026-08-25','2026-08-25',
 'Pedido de folga a 25 de Agosto (pont antes do feriado de fim de semana).',
 'pendente',NULL,NULL,NULL),

(1,'ferias','2026-09-07','2026-09-11',
 'Férias de verão: 7 a 11 de Setembro de 2026.',
 'aprovado','Férias de Setembro aprovadas.',1,'2026-06-01 09:00:00'),

-- ── Tiago Costa (id=2) ────────────────────────────────────────────────────────
(2,'folga_preferida','2026-06-07','2026-08-31',
 'Prefiro descansar ao domingo para acompanhar a família.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(2,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos intermédios (12h-21h) para supervisionar o fecho da loja.',
 'aprovado','Preferência de intermédio aprovada.',1,'2026-05-20 10:00:00'),

(2,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Francisco Gomes nas manhãs para manter alinhamento operacional.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(2,'folgas','2026-08-04','2026-08-04',
 'Pedido de folga a 4 de Agosto — consulta médica agendada.',
 'aprovado','Folga aprovada.',1,'2026-06-15 11:00:00'),

(2,'ferias','2026-08-25','2026-08-29',
 'Férias: 25 a 29 de Agosto de 2026.',
 'aprovado','Férias de Agosto aprovadas.',1,'2026-06-10 09:00:00'),

-- ── Henrique Siano (id=3) ─────────────────────────────────────────────────────
(3,'folga_preferida','2026-06-06','2026-08-31',
 'Prefiro descansar ao sábado para compromissos desportivos.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(3,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h).',
 'aprovado','Preferência de manhã aprovada.',1,'2026-05-20 10:00:00'),

(3,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Sara Ferreira — boa dinâmica de equipa.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(3,'folgas','2026-07-02','2026-07-02',
 'Folga a 2 de Julho — aniversário.',
 'aprovado','Folga aprovada.',1,'2026-06-05 10:00:00'),

(3,'ferias','2026-08-18','2026-08-21',
 'Férias: 18 a 21 de Agosto de 2026.',
 'aprovado','Férias de Agosto aprovadas. Motor de geração irá considerar ausência.',1,'2026-06-10 10:00:00'),

-- ── Tiago Eiras (id=4) ────────────────────────────────────────────────────────
(4,'folga_preferida','2026-06-04','2026-08-31',
 'Prefiro descansar às quintas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(4,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã part-time (10h-14h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(4,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Afonso Barbosa — já temos coordenação no turno da tarde.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(4,'folgas','2026-08-03','2026-08-03',
 'Folga a 3 de Agosto — inicio das aulas na universidade.',
 'pendente',NULL,NULL,NULL),

(4,'ferias','2026-09-14','2026-09-18',
 'Férias de Setembro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Afonso Barbosa (id=5) ─────────────────────────────────────────────────────
(5,'folga_preferida','2026-06-06','2026-08-31',
 'Prefiro descansar ao sábado.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(5,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos intermédios part-time (14h-18h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(5,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Tiago Eiras — boa coordenação.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(5,'folgas','2026-08-17','2026-08-17',
 'Folga a 17 de Agosto.',
 'pendente',NULL,NULL,NULL),

(5,'ferias','2026-10-05','2026-10-09',
 'Férias de Outubro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Micael Martins (id=6) ─────────────────────────────────────────────────────
(6,'folga_preferida','2026-06-02','2026-08-31',
 'Prefiro descansar às terças-feiras durante a semana.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(6,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã nos fins de semana (10h-19h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(6,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Miguel Rocha nos fins de semana.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(6,'folgas','2026-08-09','2026-08-09',
 'Folga a 9 de Agosto.',
 'pendente',NULL,NULL,NULL),

(6,'ferias','2026-09-21','2026-09-25',
 'Férias de Setembro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Francisco (Tu) (id=7) ─────────────────────────────────────────────────────
(7,'folga_preferida','2026-06-02','2026-08-31',
 'Prefiro descansar às terças-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(7,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h) como sub-gerente.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(7,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Francisco Gomes para manter continuidade de gestão.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(7,'folgas','2026-08-14','2026-08-14',
 'Folga a 14 de Agosto (véspera do feriado da Assunção).',
 'aprovado','Folga aprovada.',1,'2026-07-01 09:00:00'),

(7,'ferias','2026-08-25','2026-08-28',
 'Férias: 25 a 28 de Agosto.',
 'aprovado','Aprovado.',1,'2026-06-10 09:00:00'),

-- ── Rita Mendes (id=15) ───────────────────────────────────────────────────────
(15,'folga_preferida','2026-06-05','2026-08-31',
 'Prefiro descansar às sextas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(15,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de noite (14h-23h) — acordo com vida pessoal.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(15,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Luis Pinto nos turnos de noite.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(15,'folgas','2026-08-11','2026-08-11',
 'Folga a 11 de Agosto.',
 'aprovado','Aprovado.',1,'2026-07-01 09:00:00'),

(15,'ferias','2026-08-11','2026-08-14',
 'Férias: 11 a 14 de Agosto.',
 'aprovado','Férias aprovadas. Motor considerará ausência em Agosto.',1,'2026-06-10 09:00:00'),

-- ── Pedro Luz (id=16) ─────────────────────────────────────────────────────────
(16,'folga_preferida','2026-06-03','2026-08-31',
 'Prefiro descansar às quartas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(16,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos intermédios (12h-21h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(16,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Catarina Cruz — boa dinâmica de equipa.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(16,'folgas','2026-08-20','2026-08-20',
 'Folga a 20 de Agosto.',
 'pendente',NULL,NULL,NULL),

(16,'ferias','2026-07-14','2026-07-18',
 'Férias de verão: 14 a 18 de Julho de 2026.',
 'aprovado','Férias aprovadas. Horário de Julho gerado sem Pedro Luz nessas datas.',1,'2026-06-05 10:00:00'),

-- ── Sara Ferreira (id=17) ─────────────────────────────────────────────────────
(17,'folga_preferida','2026-06-02','2026-08-31',
 'Prefiro descansar às terças-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(17,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(17,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Henrique Siano — boa coordenação nas manhãs.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(17,'folgas','2026-08-06','2026-08-06',
 'Folga a 6 de Agosto.',
 'aprovado','Aprovado.',1,'2026-07-01 09:00:00'),

(17,'ferias','2026-08-11','2026-08-14',
 'Férias: 11 a 14 de Agosto.',
 'aprovado','Aprovado. Motor de geração irá respeitar ausência.',1,'2026-06-10 09:00:00'),

-- ── Joao Alves (id=18) ────────────────────────────────────────────────────────
(18,'folga_preferida','2026-06-01','2026-08-31',
 'Prefiro descansar às segundas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(18,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de noite (14h-23h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(18,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Rita Mendes nos turnos de noite.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(18,'folgas','2026-08-03','2026-08-03',
 'Folga a 3 de Agosto.',
 'pendente',NULL,NULL,NULL),

(18,'ferias','2026-08-04','2026-08-07',
 'Férias: 4 a 7 de Agosto.',
 'aprovado','Aprovado. Motor irá considerar ausência em Agosto.',1,'2026-06-10 09:00:00'),

-- ── Catarina Cruz (id=19) ─────────────────────────────────────────────────────
(19,'folga_preferida','2026-06-04','2026-08-31',
 'Prefiro descansar às quintas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(19,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos intermédios (12h-21h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(19,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Pedro Luz.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(19,'folgas','2026-08-21','2026-08-21',
 'Folga a 21 de Agosto.',
 'pendente',NULL,NULL,NULL),

(19,'ferias','2026-09-28','2026-10-02',
 'Férias de final de Setembro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Miguel Rocha (id=20) ──────────────────────────────────────────────────────
(20,'folga_preferida','2026-06-06','2026-08-31',
 'Prefiro descansar ao sábado.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(20,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(20,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Henrique Siano nas manhãs.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(20,'folgas','2026-08-13','2026-08-13',
 'Folga a 13 de Agosto.',
 'pendente',NULL,NULL,NULL),

(20,'ferias','2026-09-14','2026-09-18',
 'Férias de Setembro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Ines Silva (id=21) ────────────────────────────────────────────────────────
(21,'folga_preferida','2026-06-01','2026-08-31',
 'Prefiro descansar às segundas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(21,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos intermédios (12h-21h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(21,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Catarina Cruz.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(21,'folgas','2026-08-07','2026-08-07',
 'Folga a 7 de Agosto.',
 'pendente',NULL,NULL,NULL),

(21,'ferias','2026-10-12','2026-10-16',
 'Férias de Outubro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00'),

-- ── Luis Pinto (id=22) ────────────────────────────────────────────────────────
(22,'folga_preferida','2026-06-03','2026-08-31',
 'Prefiro descansar às quartas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(22,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de noite (14h-23h) — acordo familiar.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(22,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Rita Mendes e Joao Alves nos turnos de noite.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(22,'folgas','2026-08-20','2026-08-20',
 'Folga a 20 de Agosto — consulta médica.',
 'aprovado','Aprovado.',1,'2026-07-01 09:00:00'),

(22,'ferias','2026-08-24','2026-08-28',
 'Férias: 24 a 28 de Agosto.',
 'aprovado','Aprovado.',1,'2026-06-10 09:00:00'),

-- ── Marco Dias (id=25) ────────────────────────────────────────────────────────
(25,'folga_preferida','2026-06-05','2026-08-31',
 'Prefiro descansar às sextas-feiras.',
 'aprovado','Confirmado.',1,'2026-05-20 10:00:00'),

(25,'turnos','2026-06-01','2026-08-31',
 'Prefiro turnos de manhã (10h-19h).',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(25,'colegas','2026-06-01','2026-08-31',
 'Prefiro trabalhar com Francisco Gomes — experiência de aprendizagem com a gerência.',
 'aprovado','Aprovado.',1,'2026-05-20 10:00:00'),

(25,'folgas','2026-08-27','2026-08-27',
 'Folga a 27 de Agosto.',
 'pendente',NULL,NULL,NULL),

(25,'ferias','2026-09-07','2026-09-11',
 'Férias de Setembro.',
 'aprovado','Aprovado.',1,'2026-06-15 09:00:00');

-- ══════════════════════════════════════════════════════════════════════════════
-- DAY_OFFS
-- Férias de Pedro Luz em Julho (hard-block já reflectido no horário)
-- Férias e folgas em Agosto para alimentar a geração ao vivo
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO public.day_offs
    (id_utilizador, data_ausencia, motivo, tipo, estado)
VALUES
    -- Pedro Luz — férias 14-18 Jul (justificam ausência no horário de Julho)
    (16, '2026-07-14', 'Férias de verão aprovadas', 'ferias', 'aprovado'),
    (16, '2026-07-15', 'Férias de verão aprovadas', 'ferias', 'aprovado'),
    (16, '2026-07-16', 'Férias de verão aprovadas', 'ferias', 'aprovado'),
    (16, '2026-07-17', 'Férias de verão aprovadas', 'ferias', 'aprovado'),
    (16, '2026-07-18', 'Férias de verão aprovadas', 'ferias', 'aprovado'),

    -- Tiago Costa — folga aprovada Ago 4
    (2,  '2026-08-04', 'Consulta médica agendada', 'folga', 'aprovado'),
    -- Tiago Costa — férias Ago 25-29
    (2,  '2026-08-25', 'Férias de Agosto', 'ferias', 'aprovado'),
    (2,  '2026-08-26', 'Férias de Agosto', 'ferias', 'aprovado'),
    (2,  '2026-08-27', 'Férias de Agosto', 'ferias', 'aprovado'),
    (2,  '2026-08-28', 'Férias de Agosto', 'ferias', 'aprovado'),
    (2,  '2026-08-29', 'Férias de Agosto', 'ferias', 'aprovado'),

    -- Henrique Siano — férias Ago 18-21
    (3,  '2026-08-18', 'Férias de Agosto', 'ferias', 'aprovado'),
    (3,  '2026-08-19', 'Férias de Agosto', 'ferias', 'aprovado'),
    (3,  '2026-08-20', 'Férias de Agosto', 'ferias', 'aprovado'),
    (3,  '2026-08-21', 'Férias de Agosto', 'ferias', 'aprovado'),

    -- Rita Mendes — férias Ago 11-14
    (15, '2026-08-11', 'Férias de Agosto', 'ferias', 'aprovado'),
    (15, '2026-08-12', 'Férias de Agosto', 'ferias', 'aprovado'),
    (15, '2026-08-13', 'Férias de Agosto', 'ferias', 'aprovado'),
    (15, '2026-08-14', 'Férias de Agosto', 'ferias', 'aprovado'),

    -- Sara Ferreira — férias Ago 11-14
    (17, '2026-08-11', 'Férias de Agosto', 'ferias', 'aprovado'),
    (17, '2026-08-12', 'Férias de Agosto', 'ferias', 'aprovado'),
    (17, '2026-08-13', 'Férias de Agosto', 'ferias', 'aprovado'),
    (17, '2026-08-14', 'Férias de Agosto', 'ferias', 'aprovado'),

    -- Joao Alves — férias Ago 4-7
    (18, '2026-08-04', 'Férias de Agosto', 'ferias', 'aprovado'),
    (18, '2026-08-05', 'Férias de Agosto', 'ferias', 'aprovado'),
    (18, '2026-08-06', 'Férias de Agosto', 'ferias', 'aprovado'),
    (18, '2026-08-07', 'Férias de Agosto', 'ferias', 'aprovado'),

    -- Francisco (Tu) — folga aprovada Ago 14
    (7,  '2026-08-14', 'Véspera do feriado da Assunção — folga planeada', 'folga', 'aprovado'),

    -- Luis Pinto — folga aprovada Ago 20
    (22, '2026-08-20', 'Consulta médica', 'folga', 'aprovado'),

    -- Sara Ferreira — folga aprovada Ago 6
    (17, '2026-08-06', 'Folga de agosto', 'folga', 'aprovado'),

    -- Pedidos PENDENTES (para aprovar durante a demo)
    (4,  '2026-08-03', 'Inicio das aulas na universidade', 'folga', 'pendente'),
    (5,  '2026-08-17', 'Folga pessoal',                   'folga', 'pendente'),
    (6,  '2026-08-09', 'Folga pessoal',                   'folga', 'pendente'),
    (18, '2026-08-03', 'Folga pessoal',                   'folga', 'pendente'),
    (19, '2026-08-21', 'Compromisso familiar',             'folga', 'pendente'),
    (20, '2026-08-13', 'Consulta médica',                  'folga', 'pendente'),
    (21, '2026-08-07', 'Folga pessoal',                   'folga', 'pendente'),
    (25, '2026-08-27', 'Evento pessoal',                  'folga', 'pendente');

-- ══════════════════════════════════════════════════════════════════════════════
-- PERMUTAS  — exemplo aprovado (Junho) e pendente (Julho, para aprovar no pitch)
-- Usa subquery para encontrar IDs dos horários sem hardcode
-- ══════════════════════════════════════════════════════════════════════════════

-- Permuta APROVADA (histórico — Junho)
-- Rita Mendes (noite Jun 24) ↔ Luis Pinto (noite Jun 19)
-- NOTA: 24/06 e não 26/06 — a Rita descansa Sex+Sáb e 26/06/2026 é sexta-feira,
-- logo não tem horário nesse dia e o guard EXISTS deixava esta permuta de fora.
INSERT INTO public.permutas (id_horario_origem, id_horario_destino, estado, data_pedido)
SELECT
    (SELECT h.id_horario FROM public.horarios h WHERE h.id_lojautilizador = 16 AND h.data_turno = '2026-06-24' LIMIT 1),
    (SELECT h.id_horario FROM public.horarios h WHERE h.id_lojautilizador = 23 AND h.data_turno = '2026-06-19' LIMIT 1),
    'aprovado',
    CURRENT_TIMESTAMP - INTERVAL '15 days'
WHERE EXISTS (SELECT 1 FROM public.horarios WHERE id_lojautilizador=16 AND data_turno='2026-06-24')
  AND EXISTS (SELECT 1 FROM public.horarios WHERE id_lojautilizador=23 AND data_turno='2026-06-19');

-- Permuta PENDENTE (Julho — para demonstrar aprovação ao vivo durante o pitch)
-- Henrique Siano (manhã Jul 31) ↔ Miguel Rocha (manhã Jul 29)
INSERT INTO public.permutas (id_horario_origem, id_horario_destino, estado, data_pedido)
SELECT
    (SELECT h.id_horario FROM public.horarios h WHERE h.id_lojautilizador = 3  AND h.data_turno = '2026-07-31' LIMIT 1),
    (SELECT h.id_horario FROM public.horarios h WHERE h.id_lojautilizador = 21 AND h.data_turno = '2026-07-29' LIMIT 1),
    'pendente',
    CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE EXISTS (SELECT 1 FROM public.horarios WHERE id_lojautilizador=3  AND data_turno='2026-07-31')
  AND EXISTS (SELECT 1 FROM public.horarios WHERE id_lojautilizador=21 AND data_turno='2026-07-29');

-- ══════════════════════════════════════════════════════════════════════════════
-- HORÁRIOS ESPECIAIS — Agosto 2026
-- Feriado 15 Ago (Assunção de Nossa Senhora) — mínimo reforçado para 3
-- Inventário 31 Ago — loja encerrada
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO public.horarios_especiais_loja
    (id_loja, descricao, data_inicio, data_fim, hora_abertura, hora_fecho,
     minimo_colaboradores_turno, loja_encerrada, observacoes)
VALUES
    (1, 'Feriado Nacional — Assunção de Nossa Senhora',
     '2026-08-15', '2026-08-15', '10:00', '23:00', 3, FALSE,
     'Feriado nacional. Maior afluência esperada. Mínimo reforçado de 3 colaboradores por turno.'),

    (1, 'Inventário Anual',
     '2026-08-31', '2026-08-31', NULL, NULL, NULL, TRUE,
     'Loja encerrada para contagem de stock. Colaboradores convocados para inventário interno.'),

    (2, 'Feriado Nacional — Assunção de Nossa Senhora',
     '2026-08-15', '2026-08-15', '10:00', '23:00', 2, FALSE,
     'Feriado nacional. Cobertura mínima reforçada para 2 colaboradores por turno.');

-- ══════════════════════════════════════════════════════════════════════════════
-- NOTIFICAÇÕES — para encher o sino de alertas durante a demo
-- ══════════════════════════════════════════════════════════════════════════════

INSERT INTO public.notificacao (data_envio, lida, mensagem, id_utilizador, id_loja, arquivada)
VALUES
    -- Francisco Gomes (gerente)
    (CURRENT_TIMESTAMP - INTERVAL '25 days', TRUE,
     'O horário de Junho de 2026 foi aprovado pelo Supervisor Tiago Costa e publicado.',
     1, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'O horário de Julho de 2026 foi aprovado pelo Supervisor Tiago Costa e publicado.',
     1, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '20 days', TRUE,
     'Pedro Luz solicitou férias de 14 a 18 de Julho de 2026.',
     1, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '1 day', FALSE,
     'Permuta pendente: Henrique Siano e Miguel Rocha pretendem trocar os turnos de 31 e 29 de Julho. Aguarda a sua aprovação.',
     1, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '2 days', FALSE,
     'Existem 8 pedidos de folga para Agosto pendentes de aprovação.',
     1, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '5 days', FALSE,
     'Lembre-se: o prazo para lançar o horário de Agosto é dia 12 de Julho. Já pode gerar o horário.',
     1, 1, FALSE),
    -- Henrique Siano (colaborador)
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'O horário de Julho de 2026 está disponível. Consulte o seu calendário no portal.',
     3, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '1 day', FALSE,
     'O seu pedido de permuta com Miguel Rocha (Jul 31 ↔ Jul 29) está pendente de aprovação do gerente.',
     3, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '10 days', TRUE,
     'As suas férias de 18 a 21 de Agosto de 2026 foram aprovadas por Francisco Gomes.',
     3, 1, FALSE),
    -- Pedro Luz (colaborador)
    (CURRENT_TIMESTAMP - INTERVAL '20 days', TRUE,
     'As suas férias de 14 a 18 de Julho de 2026 foram aprovadas por Francisco Gomes.',
     16, 1, FALSE),
    -- Tiago Costa (supervisor)
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'O horário de Julho de 2026 que aprovou foi publicado com sucesso.',
     2, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '2 days', FALSE,
     'As suas férias de 25 a 29 de Agosto foram confirmadas.',
     2, 1, FALSE),
    -- Rita Mendes
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'O horário de Julho de 2026 está disponível. Consulte o seu calendário.',
     15, 1, FALSE),
    -- Broadcast geral (todos os colaboradores loja 1 — amostra)
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'Horário de Julho publicado. Confirme a sua disponibilidade no portal.',
     17, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'Horário de Julho publicado. Confirme a sua disponibilidade no portal.',
     18, 1, FALSE),
    (CURRENT_TIMESTAMP - INTERVAL '12 days', FALSE,
     'Horário de Julho publicado. Confirme a sua disponibilidade no portal.',
     22, 1, FALSE);

-- ══════════════════════════════════════════════════════════════════════════════
-- CORRIGIR SEQUÊNCIAS  (após INSERT com IDs explícitos)
-- ══════════════════════════════════════════════════════════════════════════════

SELECT setval('public.cargos_id_cargo_seq',                         (SELECT MAX(id_cargo)               FROM public.cargos));
SELECT setval('public.lojas_id_loja_seq',                           (SELECT MAX(id_loja)                FROM public.lojas));
SELECT setval('public.regras_id_regra_seq',                         (SELECT MAX(id_regra)               FROM public.regras));
SELECT setval('public.turnos_id_turno_seq',                         (SELECT MAX(id_turno)               FROM public.turnos));
SELECT setval('public.utilizadores_id_utilizador_seq',              (SELECT MAX(id_utilizador)          FROM public.utilizadores));
SELECT setval('public.lojautilizador_id_lojautilizador_seq',        (SELECT MAX(id_lojautilizador)      FROM public.lojautilizador));
SELECT setval('public.propostas_horario_mensal_id_proposta_horario_seq',
                                                                    (SELECT MAX(id_proposta_horario)    FROM public.propostas_horario_mensal));

COMMIT;

-- ══════════════════════════════════════════════════════════════════════════════
-- VERIFICAÇÃO RÁPIDA (opcional — correr depois do COMMIT)
-- ══════════════════════════════════════════════════════════════════════════════
/*
SELECT 'utilizadores' AS tabela, COUNT(*) FROM public.utilizadores
UNION ALL SELECT 'lojautilizador',     COUNT(*) FROM public.lojautilizador
UNION ALL SELECT 'horarios',           COUNT(*) FROM public.horarios
UNION ALL SELECT 'preferencias',       COUNT(*) FROM public.preferencias
UNION ALL SELECT 'day_offs',           COUNT(*) FROM public.day_offs
UNION ALL SELECT 'permutas',           COUNT(*) FROM public.permutas
UNION ALL SELECT 'notificacao',        COUNT(*) FROM public.notificacao
UNION ALL SELECT 'horarios_especiais', COUNT(*) FROM public.horarios_especiais_loja;
*/
