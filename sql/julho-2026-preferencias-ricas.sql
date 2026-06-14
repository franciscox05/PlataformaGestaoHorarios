-- =============================================================================
-- SEED: Preferências ricas para testar o algoritmo de geração — Julho 2026
-- Cobre todos os tipos: folga_preferida (soft), turnos, colegas, ferias (hard).
--
-- Julho 2026: Qua 1 → Sex 31  (1=Qua, 2=Qui, 3=Sex, 6=Seg, 7=Ter)
--
-- Utilizadores Loja 1:
--   1 = Francisco Gomes    (gerente)
--   2 = Tiago Costa        (supervisor)
--   3 = Henrique Siano     (fulltime)
--   4 = Tiago Eiras        (parttime)
--   5 = Afonso Barbosa     (parttime)
--   6 = Micael Martins     (reforco_parttime — só fins de semana)
--   7 = Francisco (Tu)     (subgerente)
-- Decisor para Loja 1: id_utilizador = 1 (gerente)
--
-- Podes correr este script quantas vezes quiseres — limpa os dados anteriores
-- antes de inserir. Não afeta outras preferências nem o horário publicado.
-- =============================================================================

BEGIN;

-- Limpa dados anteriores deste seed (tudo marcado como [TESTE JUL 2026])
DELETE FROM public.preferencias
WHERE descricao LIKE '[TESTE JUL 2026]%';

-- Limpa day_offs de férias inseridos por este seed
DELETE FROM public.day_offs
WHERE motivo LIKE '[TESTE JUL 2026]%';


-- ─────────────────────────────────────────────────────────────────────────────
-- 1. FOLGA_PREFERIDA (soft — 1 por semana, distribuída por dias diferentes)
--    O algoritmo extrai o dia-da-semana a partir de dataInicio e aplica-o a
--    cada semana de Julho. Distribução:
--      Seg → user 1 (gerente) e user 7 (subgerente) — mesmos dias, competição deliberada
--      Ter → user 2 (supervisor)
--      Qua → user 3 (fulltime)
--      Qui → user 4 (parttime)
--      Sex → user 5 (parttime)
--      Dom → user 6 (reforco — só trabalha FDS, prefere folgar aos domingos)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO public.preferencias (id_utilizador, tipo, descricao, data_inicio, data_fim, prioridade, estado, decisao, id_decisor, data_decisao)
VALUES

-- User 1 (gerente): folga preferida às segundas
(1, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às segundas-feiras — dias de menor afluência na loja.',
 '2026-07-06', '2026-07-31', 2, 'aprovado',
 'Aprovado: compatível com a cobertura mínima.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- User 2 (supervisor): folga preferida às terças
(2, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às terças-feiras para consultas médicas recorrentes.',
 '2026-07-07', '2026-07-31', 2, 'aprovado',
 'Aprovado: sem conflito com cobertura.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- User 3 (fulltime): folga preferida às quartas
(3, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às quartas-feiras — compromisso semanal fixo.',
 '2026-07-01', '2026-07-31', 2, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- User 4 (parttime): folga preferida às quintas
(4, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às quintas-feiras — aulas à tarde.',
 '2026-07-02', '2026-07-31', 2, 'aprovado',
 'Aprovado: perfil part-time, sem impacto na cobertura mínima.', 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- User 5 (parttime): folga preferida às sextas
(5, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às sextas-feiras — ponte para o fim de semana com a família.',
 '2026-07-03', '2026-07-31', 2, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- User 6 (reforco): folga preferida aos domingos (1 em cada 2 semanas seria ideal mas aqui é semanal)
(6, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar aos domingos quando possível — trabalho extra-curricular.',
 '2026-07-05', '2026-07-31', 1, 'aprovado',
 'Aprovado com reserva: rotação de FDS pode não permitir todas as semanas.', 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- User 7 (subgerente): folga preferida às segundas — mesmo dia que user 1
-- Cria pressão de cobertura às segundas (dois gestores querem o mesmo dia)
(7, 'folga_preferida',
 '[TESTE JUL 2026] Prefere folgar às segundas-feiras — ginásio e compromissos pessoais.',
 '2026-07-06', '2026-07-31', 2, 'aprovado',
 'Aprovado: o gestor garante que pelo menos 1 dos dois ficará disponível.', 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),


-- ─────────────────────────────────────────────────────────────────────────────
-- 2. PREFERÊNCIAS DE TURNO (shift type preferences)
--    Palavras-chave reconhecidas pelo AvaliadorAtribuicao:
--      manha, tarde, intermedio, noite, curto (<5h), longo (>=5h)
-- ─────────────────────────────────────────────────────────────────────────────

-- User 1 (gerente): prefere turnos longos de manhã (10-19)
(1, 'turnos',
 '[TESTE JUL 2026] Prefere turno longo de manha para coordenar abertura da loja.',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado: compatível com perfil gerente.', 1, CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- User 2 (supervisor): prefere turnos intermédios (12-21) — controlo de fluxo tarde
(2, 'turnos',
 '[TESTE JUL 2026] Prefere turno intermedio para supervisionar o periodo de maior movimento.',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- User 3 (fulltime): prefere turnos de manhã longos
(3, 'turnos',
 '[TESTE JUL 2026] Prefere turno longo de manha — tem filhos para buscar ao fim da tarde.',
 '2026-07-01', NULL, 3, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- User 4 (parttime): prefere turnos curtos intermédios (14-18:30)
(4, 'turnos',
 '[TESTE JUL 2026] Prefere turno curto de intermedio — disponível apenas a partir das 14h.',
 '2026-07-01', NULL, 3, 'aprovado',
 'Aprovado: perfil part-time, turno curto adequado.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- User 5 (parttime): prefere turnos curtos de noite (18:30-23) — estudante noturno
(5, 'turnos',
 '[TESTE JUL 2026] Prefere turno curto de noite — manhãs ocupadas com formação.',
 '2026-07-01', NULL, 3, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- User 6 (reforco): prefere turnos de manhã nos fins de semana
(6, 'turnos',
 '[TESTE JUL 2026] Prefere turno de manha ao fim de semana — transporte público.',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado: perfil reforço FDS, turno manhã disponível.', 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- User 7 (subgerente): prefere turnos de noite longos (14-23) — rende ao gerente
(7, 'turnos',
 '[TESTE JUL 2026] Prefere turno longo de noite para coordenar encerramento da loja.',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado: complementa o turno do gerente.', 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),


-- ─────────────────────────────────────────────────────────────────────────────
-- 3. PREFERÊNCIAS DE COLEGAS (colleague preferences)
--    A descricao deve conter o nome do colega. O algoritmo resolve por
--    contenção case-insensitive contra os nomes dos colaboradores ativos.
--    Para que o par seja reconhecido, AMBOS devem ter preferência um pelo outro.
--
--    Pares criados:
--      (1, 7)  Francisco Gomes <-> Francisco (Tu)   — dois gestores preferem coincdir
--      (3, 2)  Henrique Siano  <-> Tiago Costa      — equipa de abertura
--      (4, 5)  Tiago Eiras     <-> Afonso Barbosa   — dupla part-time
-- ─────────────────────────────────────────────────────────────────────────────

-- Par 1 <-> 7
(1, 'colegas',
 '[TESTE JUL 2026] Francisco (Tu)',
 '2026-07-01', NULL, 1, 'aprovado',
 'Aprovado: preferência de gestão interna.', 1, CURRENT_TIMESTAMP - INTERVAL '7 days'),

(7, 'colegas',
 '[TESTE JUL 2026] Francisco Gomes',
 '2026-07-01', NULL, 1, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '7 days'),

-- Par 3 <-> 2
(3, 'colegas',
 '[TESTE JUL 2026] Tiago Costa',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado: equipa de abertura funciona bem em conjunto.', 1, CURRENT_TIMESTAMP - INTERVAL '6 days'),

(2, 'colegas',
 '[TESTE JUL 2026] Henrique Siano',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '6 days'),

-- Par 4 <-> 5
(4, 'colegas',
 '[TESTE JUL 2026] Afonso Barbosa',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado: melhor dinâmica nas tardes partilhadas.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

(5, 'colegas',
 '[TESTE JUL 2026] Tiago Eiras',
 '2026-07-01', NULL, 2, 'aprovado',
 'Aprovado.', 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Preferência unilateral sem reciprocidade (para testar que o par não é criado)
(6, 'colegas',
 '[TESTE JUL 2026] Micael prefere trabalhar com quem quer que esteja de manhã no FDS — sem nenhum nome específico.',
 '2026-07-01', NULL, 1, 'aprovado',
 'Aprovado mas não resolve para nenhum colega específico.', 1, CURRENT_TIMESTAMP - INTERVAL '3 days');


-- ─────────────────────────────────────────────────────────────────────────────
-- 4. FÉRIAS HARD (bloqueiam completamente a geração nessas datas)
--    Inseridas em preferencias com tipo='ferias' (bloqueio hard reconhecido
--    pelo PreferenciasGeracaoBuilder.construirBloqueiosPorUtilizador).
--
--    User 3 (Henrique Siano):  semana de 14-18 Jul (folga de verão)
--    User 5 (Afonso Barbosa):  3 dias 22-24 Jul (ponte de verão)
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO public.preferencias (id_utilizador, tipo, descricao, data_inicio, data_fim, prioridade, estado, decisao, id_decisor, data_decisao)
VALUES
(3, 'ferias',
 '[TESTE JUL 2026] Férias de verão — semana completa.',
 '2026-07-14', '2026-07-18', 5, 'aprovado',
 'Férias aprovadas. Cobertura redistribuída.', 1, CURRENT_TIMESTAMP - INTERVAL '10 days'),

(5, 'ferias',
 '[TESTE JUL 2026] Férias curtas de verão — 3 dias.',
 '2026-07-22', '2026-07-24', 4, 'aprovado',
 'Aprovado: período de menor afluência.', 1, CURRENT_TIMESTAMP - INTERVAL '8 days');


-- ─────────────────────────────────────────────────────────────────────────────
-- 5. PEDIDO DE FOLGA PONTUAL (day_offs hard) — via tabela day_offs
--    Complementa as férias em preferencias; testa que AMBAS as fontes
--    são respeitadas no bloqueio.
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO public.day_offs (id_utilizador, data_ausencia, motivo, tipo, estado)
VALUES
-- User 4 (Tiago Eiras): folga aprovada no dia 11 Jul (dia de consulta)
(4, '2026-07-11', '[TESTE JUL 2026] Consulta médica agendada.', 'folgas', 'aprovado'),
-- User 7 (Francisco Tu): folga aprovada no dia 25 Jul
(7, '2026-07-25', '[TESTE JUL 2026] Assunto pessoal urgente.', 'folgas', 'aprovado');


COMMIT;
