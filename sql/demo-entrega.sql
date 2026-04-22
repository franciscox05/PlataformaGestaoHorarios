BEGIN;

-- Este script prepara um conjunto de dados estavel para demonstracao local.
-- Substitui os dados funcionais atuais da aplicacao.

-- Compatibilidade com seguranca e auditoria (#23).
CREATE TABLE IF NOT EXISTS public.eventos_auditoria (
    id_evento serial PRIMARY KEY,
    tipo_evento varchar(80) NOT NULL,
    resultado varchar(20) NOT NULL,
    origem varchar(80) NOT NULL,
    id_utilizador integer,
    email_referencia varchar(150),
    identificador_sessao varchar(64),
    data_evento timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalhes text,
    CONSTRAINT fk_eventos_auditoria_utilizador
        FOREIGN KEY (id_utilizador) REFERENCES public.utilizadores(id_utilizador)
);

ALTER TABLE public.eventos_auditoria
    ADD COLUMN IF NOT EXISTS origem varchar(80);

ALTER TABLE public.eventos_auditoria
    ADD COLUMN IF NOT EXISTS detalhes text;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'eventos_auditoria'
          AND column_name = 'detalhe'
    ) THEN
        EXECUTE '
            UPDATE public.eventos_auditoria
            SET detalhes = COALESCE(detalhes, detalhe)
        ';
    END IF;
END $$;

UPDATE public.eventos_auditoria
SET origem = COALESCE(NULLIF(origem, ''), 'sistema');

ALTER TABLE public.eventos_auditoria
    ALTER COLUMN origem SET DEFAULT 'sistema';

ALTER TABLE public.eventos_auditoria
    ALTER COLUMN origem SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_data
    ON public.eventos_auditoria (data_evento DESC);

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_utilizador
    ON public.eventos_auditoria (id_utilizador, data_evento DESC);

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_sessao
    ON public.eventos_auditoria (identificador_sessao);

-- Compatibilidade com a aprovacao de preferencias (#17).
ALTER TABLE public.preferencias
    ADD COLUMN IF NOT EXISTS tipo varchar(50),
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS prioridade integer,
    ADD COLUMN IF NOT EXISTS estado varchar(50),
    ADD COLUMN IF NOT EXISTS decisao text,
    ADD COLUMN IF NOT EXISTS id_decisor integer,
    ADD COLUMN IF NOT EXISTS data_decisao timestamp without time zone;

ALTER TABLE public.preferencias
    DROP CONSTRAINT IF EXISTS fk_preferencias_decisor;

ALTER TABLE public.preferencias
    ADD CONSTRAINT fk_preferencias_decisor
        FOREIGN KEY (id_decisor) REFERENCES public.utilizadores(id_utilizador);

-- Compatibilidade com a geracao de horarios (#20).
CREATE TABLE IF NOT EXISTS public.propostas_horario_mensal (
    id_proposta_horario serial PRIMARY KEY,
    id_loja integer NOT NULL,
    id_utilizador_geracao integer NOT NULL,
    ano integer NOT NULL,
    mes integer NOT NULL,
    estado varchar(50) NOT NULL DEFAULT 'pendente',
    resumo_geracao text,
    data_geracao timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proposta_horario_loja
        FOREIGN KEY (id_loja) REFERENCES public.lojas(id_loja),
    CONSTRAINT fk_proposta_horario_utilizador
        FOREIGN KEY (id_utilizador_geracao) REFERENCES public.utilizadores(id_utilizador)
);

ALTER TABLE public.propostas_horario_mensal
    ADD COLUMN IF NOT EXISTS id_utilizador_decisao integer;

ALTER TABLE public.propostas_horario_mensal
    ADD COLUMN IF NOT EXISTS data_decisao timestamp without time zone;

ALTER TABLE public.propostas_horario_mensal
    ADD COLUMN IF NOT EXISTS observacoes_supervisor text;

ALTER TABLE public.propostas_horario_mensal
    DROP CONSTRAINT IF EXISTS fk_proposta_horario_decisao_utilizador;

ALTER TABLE public.propostas_horario_mensal
    ADD CONSTRAINT fk_proposta_horario_decisao_utilizador
        FOREIGN KEY (id_utilizador_decisao) REFERENCES public.utilizadores(id_utilizador);

ALTER TABLE public.horarios
    ADD COLUMN IF NOT EXISTS id_proposta_horario integer;

ALTER TABLE public.horarios
    DROP CONSTRAINT IF EXISTS fk_horarios_proposta_horario;

ALTER TABLE public.horarios
    ADD CONSTRAINT fk_horarios_proposta_horario
        FOREIGN KEY (id_proposta_horario) REFERENCES public.propostas_horario_mensal(id_proposta_horario);

CREATE TABLE IF NOT EXISTS public.horarios_especiais_loja (
    id_horario_especial SERIAL PRIMARY KEY,
    id_loja INTEGER NOT NULL,
    descricao VARCHAR(160) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    hora_abertura TIME NULL,
    hora_fecho TIME NULL,
    minimo_colaboradores_turno INTEGER NULL,
    loja_encerrada BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes TEXT NULL,
    CONSTRAINT fk_horarios_especiais_loja
        FOREIGN KEY (id_loja) REFERENCES public.lojas(id_loja),
    CONSTRAINT ck_horarios_especiais_periodo
        CHECK (data_inicio <= data_fim),
    CONSTRAINT ck_horarios_especiais_horas
        CHECK (
            (hora_abertura IS NULL AND hora_fecho IS NULL)
            OR (hora_abertura IS NOT NULL AND hora_fecho IS NOT NULL AND hora_abertura < hora_fecho)
        ),
    CONSTRAINT ck_horarios_especiais_minimo
        CHECK (minimo_colaboradores_turno IS NULL OR minimo_colaboradores_turno > 0)
);

CREATE INDEX IF NOT EXISTS idx_horarios_especiais_loja_periodo
    ON public.horarios_especiais_loja (id_loja, data_inicio, data_fim);

TRUNCATE TABLE
    public.eventos_auditoria,
    public.historico_horario_estados,
    public.permutas,
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
    (3, 'Maximo de horas consecutivas', 8, 'legal'),
    (4, 'Carga contratual mensal gestao (horas)', 176, 'contratual'),
    (5, 'Carga contratual mensal full-time (horas)', 176, 'contratual'),
    (6, 'Carga contratual mensal part-time (horas)', 96, 'contratual'),
    (7, 'Carga contratual mensal reforco de fim de semana (horas)', 64, 'contratual');

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

INSERT INTO public.preferencias (
    id_preferencia,
    id_utilizador,
    descricao,
    tipo,
    data_inicio,
    data_fim,
    prioridade,
    estado,
    decisao,
    id_decisor,
    data_decisao
) VALUES
    (1, 7, 'Preferencia por dois dias consecutivos para compromisso familiar.', 'folgas', CURRENT_DATE + 20, CURRENT_DATE + 21, 5, 'pendente', NULL, NULL, NULL),
    (2, 3, 'Preferencia por turnos da manha durante a semana.', 'turnos', NULL, NULL, 3, 'aprovado', 'Aprovado para equilibrar a distribuicao dos turnos da equipa.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 4, 'Preferencia para trabalhar com Afonso Barbosa no proximo periodo.', 'colegas', NULL, NULL, 2, 'rejeitado', 'Nao foi possivel acomodar esta preferencia sem comprometer a cobertura da loja.', 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (4, 5, 'Pedido de ferias para ponte familiar do proximo mes.', 'ferias', CURRENT_DATE + 25, CURRENT_DATE + 27, 4, 'pendente', NULL, NULL, NULL);

INSERT INTO public.permutas (id_permuta, id_horario_origem, id_horario_destino, estado, data_pedido) VALUES
    (1, 15, 16, 'pendente', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 17, 18, 'aprovada', CURRENT_TIMESTAMP - INTERVAL '3 days');

INSERT INTO public.eventos_auditoria (
    id_evento,
    tipo_evento,
    resultado,
    origem,
    id_utilizador,
    email_referencia,
    identificador_sessao,
    data_evento,
    detalhes
) VALUES
    (1, 'login', 'sucesso', 'autenticacao', 7, 'francisco@levis.com', 'sess-demo-001', CURRENT_TIMESTAMP - INTERVAL '4 hours', 'Autenticacao concluida com sucesso.'),
    (2, 'login', 'falha', 'autenticacao', NULL, 'francisco@levis.com', NULL, CURRENT_TIMESTAMP - INTERVAL '3 hours', 'Credenciais invalidas.'),
    (3, 'alteracao_password', 'sucesso', 'perfil', 7, 'francisco@levis.com', 'sess-demo-001', CURRENT_TIMESTAMP - INTERVAL '2 hours', 'Password atualizada com sucesso.'),
    (4, 'colaborador_criado', 'sucesso', 'gestao_funcionarios', 1, 'francisco.gomes@levis.com', 'sess-demo-gestor', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Colaborador beatriz@levis.com criado na loja Levi''s Braga Parque.'),
    (5, 'logout', 'sucesso', 'sessao', 7, 'francisco@levis.com', 'sess-demo-001', CURRENT_TIMESTAMP - INTERVAL '90 minutes', 'Sessao terminada manualmente.'),
    (6, 'sessao_expirada', 'sucesso', 'sessao', 3, 'henrique.siano@levis.com', 'sess-demo-004', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'Sessao terminada por inatividade.');

DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('public.cargos', 'id_cargo'), COALESCE((SELECT MAX(id_cargo) FROM public.cargos), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.lojas', 'id_loja'), COALESCE((SELECT MAX(id_loja) FROM public.lojas), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.regras', 'id_regra'), COALESCE((SELECT MAX(id_regra) FROM public.regras), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.turnos', 'id_turno'), COALESCE((SELECT MAX(id_turno) FROM public.turnos), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.utilizadores', 'id_utilizador'), COALESCE((SELECT MAX(id_utilizador) FROM public.utilizadores), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.lojautilizador', 'id_lojautilizador'), COALESCE((SELECT MAX(id_lojautilizador) FROM public.lojautilizador), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.regras_loja', 'id_regra_loja'), COALESCE((SELECT MAX(id_regra_loja) FROM public.regras_loja), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios', 'id_horario'), COALESCE((SELECT MAX(id_horario) FROM public.horarios), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.day_offs', 'id_dayoff'), COALESCE((SELECT MAX(id_dayoff) FROM public.day_offs), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.preferencias', 'id_preferencia'), COALESCE((SELECT MAX(id_preferencia) FROM public.preferencias), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios_especiais_loja', 'id_horario_especial'), COALESCE((SELECT MAX(id_horario_especial) FROM public.horarios_especiais_loja), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.permutas', 'id_permuta'), COALESCE((SELECT MAX(id_permuta) FROM public.permutas), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.propostas_horario_mensal', 'id_proposta_horario'), COALESCE((SELECT MAX(id_proposta_horario) FROM public.propostas_horario_mensal), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.eventos_auditoria', 'id_evento'), COALESCE((SELECT MAX(id_evento) FROM public.eventos_auditoria), 1), true);
END $$;

COMMIT;
