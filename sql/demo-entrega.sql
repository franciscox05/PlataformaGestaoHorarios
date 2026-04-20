BEGIN;

-- Este script prepara um conjunto de dados estavel para demonstracao local.
-- Substitui os dados funcionais atuais da aplicacao.

TRUNCATE TABLE
    public.historico_horario_estados,
    public.permutas,
    public.horarios,
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

INSERT INTO public.cargos (id_cargo, nome, tipo, descricao) VALUES
    (1, 'Gerente de Loja', 'gerente', 'Responsavel maximo pela loja'),
    (2, 'Supervisor de Equipa', 'supervisor', 'Validacao operacional e apoio'),
    (3, 'Sub-Gerente', 'subgerente', 'Apoio direto a gerencia'),
    (4, 'Assistente de Vendas FT', 'fulltime', 'Vendedor a tempo inteiro'),
    (5, 'Assistente de Vendas PT', 'parttime', 'Vendedor a tempo parcial'),
    (6, 'Reforco Fim de Semana', 'reforco_parttime', 'Apoio de fim de semana');

INSERT INTO public.lojas (id_loja, nome, localizacao, hora_abertura, hora_fecho) VALUES
    (1, 'Levi''s Braga Parque', 'Braga Parque', '10:00', '23:00'),
    (2, 'Levi''s NorteShopping', 'Porto', '10:00', '23:00'),
    (3, 'Levi''s Colombo', 'Lisboa', '10:00', '23:00'),
    (4, 'Levi''s Vasco da Gama', 'Lisboa', '09:00', '22:00');

INSERT INTO public.regras (id_regra, descricao, valor_padrao, tipo) VALUES
    (1, 'Minimo de funcionarios por turno', 2, 'operacional'),
    (2, 'Dia limite de lancamento do horario mensal', 15, 'administrativo'),
    (3, 'Maximo de horas consecutivas', 8, 'legal');

INSERT INTO public.regras_loja (id_regra_loja, id_loja, id_regra, valor_especifico, observacoes) VALUES
    (1, 1, 1, 3, 'Loja principal da demonstracao com maior afluencia.'),
    (2, 1, 2, 12, 'Horario mensal deve ser fechado antes da segunda semana.'),
    (3, 1, 3, 8, 'Limite padrao para turnos longos.'),
    (4, 2, 1, 2, 'Configuracao base para loja secundaria.');

INSERT INTO public.turnos (id_turno, tipo, hora_inicio, hora_fim) VALUES
    (1, 'manha', '10:00', '19:00'),
    (2, 'intermedio', '12:00', '21:00'),
    (3, 'noite', '14:00', '23:00'),
    (4, 'manha', '10:00', '14:30'),
    (5, 'intermedio', '14:00', '18:30'),
    (6, 'noite', '18:30', '23:00');

INSERT INTO public.utilizadores (id_utilizador, nome, email, telemovel, password_hash, estado) VALUES
    (1, 'Francisco Gomes', 'francisco.gomes@levis.com', '912000001', '123456', 'ativo'),
    (2, 'Tiago Costa', 'tiago.costa@levis.com', '912000002', '123456', 'ativo'),
    (3, 'Henrique Siano', 'henrique.siano@levis.com', '912000003', '123456', 'ativo'),
    (4, 'Tiago Eiras', 'tiago.eiras@levis.com', '912000004', '123456', 'ativo'),
    (5, 'Afonso Barbosa', 'afonso.barbosa@levis.com', '912000005', '123456', 'ativo'),
    (6, 'Micael Martins', 'micael.martins@levis.com', '912000006', '123456', 'ativo'),
    (7, 'Francisco (Tu)', 'francisco@levis.com', '912000007', '123456', 'ativo'),
    (8, 'Ana Sousa', 'ana@levis.com', '912000008', '123456', 'ativo'),
    (9, 'Carlos Pereira', 'carlos@levis.com', '912000009', '123456', 'ativo'),
    (10, 'Beatriz Santos', 'beatriz@levis.com', '912000010', '123456', 'inativo');

INSERT INTO public.lojautilizador (id_lojautilizador, id_utilizador, id_loja, id_cargo, data_inicio, data_fim) VALUES
    (1, 1, 1, 1, CURRENT_DATE - 400, NULL),
    (2, 2, 1, 2, CURRENT_DATE - 320, NULL),
    (3, 3, 1, 4, CURRENT_DATE - 240, NULL),
    (4, 4, 1, 5, CURRENT_DATE - 180, NULL),
    (5, 5, 1, 5, CURRENT_DATE - 180, NULL),
    (6, 6, 1, 6, CURRENT_DATE - 150, NULL),
    (7, 7, 1, 3, CURRENT_DATE - 20, NULL),
    (8, 8, 2, 3, CURRENT_DATE - 120, NULL),
    (9, 9, 3, 2, CURRENT_DATE - 90, NULL),
    (10, 10, 1, 5, CURRENT_DATE - 140, CURRENT_DATE - 30);

INSERT INTO public.horarios (id_horario, id_lojautilizador, id_turno, data_turno, estado) VALUES
    (1, 7, 2, CURRENT_DATE + 1, 'aprovado'),
    (2, 7, 1, CURRENT_DATE + 3, 'aprovado'),
    (3, 7, 3, CURRENT_DATE + 6, 'pendente'),
    (4, 3, 1, CURRENT_DATE + 1, 'aprovado'),
    (5, 4, 3, CURRENT_DATE + 1, 'aprovado'),
    (6, 5, 4, CURRENT_DATE + 2, 'aprovado'),
    (7, 6, 6, CURRENT_DATE + 5, 'aprovado'),
    (8, 2, 1, CURRENT_DATE, 'aprovado'),
    (9, 1, 2, CURRENT_DATE, 'aprovado'),
    (10, 3, 2, CURRENT_DATE + 3, 'aprovado'),
    (11, 4, 5, CURRENT_DATE + 2, 'aprovado'),
    (12, 7, 2, CURRENT_DATE - 5, 'aprovado'),
    (13, 3, 1, CURRENT_DATE - 7, 'aprovado'),
    (14, 5, 4, CURRENT_DATE - 2, 'aprovado'),
    (15, 7, 2, CURRENT_DATE + 4, 'aprovado'),
    (16, 3, 3, CURRENT_DATE + 4, 'aprovado'),
    (17, 4, 5, CURRENT_DATE + 6, 'aprovado'),
    (18, 5, 6, CURRENT_DATE + 6, 'aprovado');

INSERT INTO public.day_offs (id_dayoff, id_utilizador, data_ausencia, motivo, tipo, estado) VALUES
    (1, 7, CURRENT_DATE + 10, 'Fim de semana prolongado com a familia.', 'ferias', 'pendente'),
    (2, 3, CURRENT_DATE + 12, 'Consulta medica ja marcada.', 'folgas', 'aprovado'),
    (3, 4, CURRENT_DATE + 8, 'Recuperacao fisica.', 'baixa', 'recusado'),
    (4, 1, CURRENT_DATE + 15, 'Necessidade pessoal.', 'folgas', 'pendente'),
    (5, 7, CURRENT_DATE + 2, 'Assunto pessoal urgente.', 'folgas', 'aprovado'),
    (6, 5, CURRENT_DATE - 10, 'Baixa medica curta.', 'baixa', 'aprovado');

INSERT INTO public.preferencias (id_preferencia, id_utilizador, descricao, tipo, data_inicio, data_fim, prioridade, estado) VALUES
    (1, 7, 'Preferencia por dois dias consecutivos para compromisso familiar.', 'folgas', CURRENT_DATE + 20, CURRENT_DATE + 21, 5, 'pendente'),
    (2, 3, 'Preferencia por turnos da manha durante a semana.', 'turnos', NULL, NULL, 3, 'aprovado'),
    (3, 4, 'Preferencia para trabalhar com Afonso Barbosa no proximo periodo.', 'colegas', NULL, NULL, 2, 'rejeitado');

INSERT INTO public.permutas (id_permuta, id_horario_origem, id_horario_destino, estado, data_pedido) VALUES
    (1, 15, 16, 'pendente', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 17, 18, 'aprovada', CURRENT_TIMESTAMP - INTERVAL '3 days');

COMMIT;
