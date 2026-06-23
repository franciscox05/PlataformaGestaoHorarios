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
-- "prioridade" das preferencias removida do modelo de negocio (nunca foi usada pelo
-- motor de geracao). DROP idempotente para limpar o schema em qualquer ambiente.
ALTER TABLE public.preferencias DROP COLUMN IF EXISTS prioridade;

ALTER TABLE public.preferencias
    ADD COLUMN IF NOT EXISTS tipo varchar(50),
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS data_fim date,
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

-- Compatibilidade com gestao de turnos e regras livres (junho 2026).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'turnos'
          AND column_name = 'tipo' AND udt_name = 'tipo_turno_enum'
    ) THEN
        EXECUTE 'ALTER TABLE public.turnos ALTER COLUMN tipo TYPE VARCHAR(50)';
    END IF;
END $$;

ALTER TABLE public.turnos ADD COLUMN IF NOT EXISTS nome VARCHAR(100);
-- Turnos por loja + versionamento copy-on-write (junho 2026):
--   id_loja NULL          = turno global (visivel a todas as lojas)
--   data_inicio/fim_vigencia NULL = sempre vigente; data_fim preenchida = versao arquivada
ALTER TABLE public.turnos ADD COLUMN IF NOT EXISTS id_loja INTEGER REFERENCES public.lojas(id_loja);
ALTER TABLE public.turnos ADD COLUMN IF NOT EXISTS data_inicio_vigencia DATE;
ALTER TABLE public.turnos ADD COLUMN IF NOT EXISTS data_fim_vigencia DATE;
ALTER TABLE public.regras_loja ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE public.regras ADD COLUMN IF NOT EXISTS id_loja_privada INTEGER REFERENCES public.lojas(id_loja);

-- Optimistic locking (#2, #3, #11): coluna de versao para deteccao de escritas
-- concorrentes em decisoes de folga e permuta. Hibernate @Version mapeia esta coluna.
ALTER TABLE public.day_offs ADD COLUMN IF NOT EXISTS versao INTEGER NOT NULL DEFAULT 0;
ALTER TABLE public.permutas ADD COLUMN IF NOT EXISTS versao INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS public.permutas_folga (
    id_permuta_folga  SERIAL PRIMARY KEY,
    id_horario_d      INTEGER NOT NULL REFERENCES public.horarios(id_horario),
    id_horario_y      INTEGER NOT NULL REFERENCES public.horarios(id_horario),
    estado            VARCHAR(20) NOT NULL DEFAULT 'pendente',
    data_pedido       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pf_estado CHECK (estado IN ('pendente','aprovado','rejeitado','cancelado')),
    CONSTRAINT chk_pf_diferente CHECK (id_horario_d <> id_horario_y)
);

CREATE INDEX IF NOT EXISTS idx_pf_horario_d ON public.permutas_folga(id_horario_d);
CREATE INDEX IF NOT EXISTS idx_pf_horario_y ON public.permutas_folga(id_horario_y);
CREATE INDEX IF NOT EXISTS idx_pf_estado    ON public.permutas_folga(estado);

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
    (1,  'Minimo de funcionarios por turno',                        2,   'operacional'),
    (2,  'Dia limite de lancamento do horario mensal',              15,  'administrativo'),
    (3,  'Maximo de dias consecutivos de trabalho',                 6,   'legal'),
    (4,  'Carga contratual mensal gestao (horas)',                  176, 'contratual'),
    (5,  'Carga contratual mensal full-time (horas)',               176, 'contratual'),
    (6,  'Carga contratual mensal part-time (horas)',               88,  'contratual'),
    (7,  'Carga contratual mensal reforco de fim de semana (horas)',64,  'contratual'),
    (8,  'Descanso semanal minimo (dias)',                          2,   'descanso'),
    (9,  'Janela de rotacao de fins de semana (semanas)',           7,   'descanso'),
    (10, 'Presenca de gerente ou subgerente aos sabados',           1,   'operacional'),
    (11, 'Descanso minimo entre turnos (horas)',                    11,  'legal');

INSERT INTO public.regras_loja (id_regra_loja, id_loja, id_regra, valor_especifico, observacoes) VALUES
    -- Loja 1 (Braga Parque) — regras completas, loja principal da demo
    -- Equipa: 3 gestao + 9 FT + 2 PT + 1 reforco = 15 workers; 3 tipos x minimo=2 x 31 dias = 186 slots;
    -- capacidade FT/gestao: 12 x 22 turnos = 264 > 186 (42% de folga para restricoes).
    (1,  1,  1, 2,   'Minimo de 2 colaboradores por turno para loja de media dimensao.'),
    (2,  1,  2, 12,  'Horario mensal fechado ate ao dia 12 para garantir comunicacao atempada.'),
    (3,  1,  3, 6,   'Maximo de 6 dias consecutivos de trabalho (lei do trabalho portugues).'),
    (4,  1,  4, 176, 'Carga mensal gestao: 22 dias x 8h = 176h.'),
    (5,  1,  5, 176, 'Carga mensal full-time: 22 dias x 8h = 176h.'),
    (6,  1,  6, 88,  'Carga mensal part-time: 22 dias x 4h = 88h (sem pausa de almoco).'),
    (7,  1,  7, 64,  'Carga mensal reforco FDS: fins de semana do mes.'),
    (8,  1,  8, 2,   'Dois dias de descanso por semana.'),
    (9,  1,  9, 7,   'Rotacao de fins de semana a cada 7 semanas (ciclo completo).'),
    (10, 1, 10, 1,   'Garantir presenca de gerente ou subgerente aos sabados com loja aberta.'),
    (11, 1, 11, 11,  'Descanso minimo de 11h entre jornadas (CT art. 214).'),
    -- Loja 2 (NorteShopping) — equipa de 8 (3 gestao + 4 FT + 1 PT); minimo=1/turno:
    -- 3 tipos x 1 x 31 = 93 slots; capacidade FT/gestao: 7 x 22 = 154 > 93 (65% de folga).
    (12, 2,  1, 1,   'Loja com equipa mais reduzida; minimo de 1 colaborador por turno.'),
    (13, 2,  6, 88,  'Carga mensal part-time: 22 dias x 4h = 88h.'),
    (14, 2,  8, 2,   'Descanso semanal minimo.'),
    (15, 2,  9, 7,   'Rotacao de fins de semana a cada 7 semanas (ciclo completo).'),
    (16, 2, 10, 1,   'Chefia obrigatoria ao sabado.');

-- Turnos globais (id_loja NULL = visivel a todas as lojas).
-- FT: 9h de turno = 8h de trabalho + 1h almoco (a 1h e descontada na carga contratual).
-- PT: 4h exactas = metade das 8h de trabalho diarias; sem pausa de almoco, sem desconto.
INSERT INTO public.turnos (id_turno, tipo, nome, hora_inicio, hora_fim, ativo) VALUES
    (1, 'manha',      'Manha FT',        '10:00', '19:00', true),
    (2, 'intermedio', 'Intermedio FT',   '12:00', '21:00', true),
    (3, 'noite',      'Noite FT',        '14:00', '23:00', true),
    (4, 'manha',      'Manha PT',        '10:00', '14:00', true),
    (5, 'intermedio', 'Intermedio PT',   '14:00', '18:00', true),
    (6, 'noite',      'Noite PT',        '19:00', '23:00', true);

INSERT INTO public.utilizadores (id_utilizador, nome, email, telemovel, password_hash, estado) VALUES
    -- Equipa Levi's Braga Parque (loja 1)
    (1,  'Francisco Gomes',   'francisco.gomes@levis.com',   '912000001', '123456', 'ativo'),
    (2,  'Tiago Costa',       'tiago.costa@levis.com',        '912000002', '123456', 'ativo'),
    (3,  'Henrique Siano',    'henrique.siano@levis.com',     '912000003', '123456', 'ativo'),
    (4,  'Tiago Eiras',       'tiago.eiras@levis.com',        '912000004', '123456', 'ativo'),
    (5,  'Afonso Barbosa',    'afonso.barbosa@levis.com',     '912000005', '123456', 'ativo'),
    (6,  'Micael Martins',    'micael.martins@levis.com',     '912000006', '123456', 'ativo'),
    (7,  'Francisco (Tu)',    'francisco@levis.com',           '912000007', '123456', 'ativo'),
    (8,  'Ana Sousa',         'ana@levis.com',                 '912000008', '123456', 'ativo'),
    (9,  'Carlos Pereira',    'carlos@levis.com',              '912000009', '123456', 'ativo'),
    (10, 'Beatriz Santos',    'beatriz@levis.com',             '912000010', '123456', 'inativo'),
    -- Equipa Levi's NorteShopping (loja 2)
    (11, 'Sofia Marques',     'sofia.marques@levis.com',      '912000011', '123456', 'ativo'),
    (12, 'Diogo Faria',       'diogo.faria@levis.com',        '912000012', '123456', 'ativo'),
    (13, 'Marta Pinto',       'marta.pinto@levis.com',        '912000013', '123456', 'ativo'),
    (14, 'Rui Castro',        'rui.castro@levis.com',         '912000014', '123456', 'ativo'),
    -- Colaboradores FT adicionais loja 1 (necessarios para geracao com minimo=2)
    (15, 'Rita Mendes',       'rita.mendes@levis.com',        '912000015', '123456', 'ativo'),
    (16, 'Pedro Luz',         'pedro.luz@levis.com',          '912000016', '123456', 'ativo'),
    (17, 'Sara Ferreira',     'sara.ferreira@levis.com',      '912000017', '123456', 'ativo'),
    (18, 'Joao Alves',        'joao.alves@levis.com',         '912000018', '123456', 'ativo'),
    (19, 'Catarina Cruz',     'catarina.cruz@levis.com',      '912000019', '123456', 'ativo'),
    (20, 'Miguel Rocha',      'miguel.rocha@levis.com',       '912000020', '123456', 'ativo'),
    (21, 'Ines Silva',        'ines.silva@levis.com',         '912000021', '123456', 'ativo'),
    (22, 'Luis Pinto',        'luis.pinto@levis.com',         '912000022', '123456', 'ativo'),
    -- Colaboradores FT adicionais loja 2
    (23, 'Vera Lopes',        'vera.lopes@levis.com',         '912000023', '123456', 'ativo'),
    (24, 'Nuno Santos',       'nuno.santos@levis.com',        '912000024', '123456', 'ativo'),
    -- Trabalhador multi-loja: tem ligacao ativa simultanea em loja 1 e loja 2
    (25, 'Marco Dias',        'marco.dias@levis.com',         '912000025', '123456', 'ativo');

INSERT INTO public.lojautilizador (id_lojautilizador, id_utilizador, id_loja, id_cargo, data_inicio, data_fim) VALUES
    -- Loja 1 — Braga Parque: 3 gestao + 9 FT + 2 PT + 1 reforco = 15 workers ativos
    (1,  1,  1, 1, CURRENT_DATE - 400, NULL),  -- Francisco Gomes   gerente
    (2,  2,  1, 2, CURRENT_DATE - 320, NULL),  -- Tiago Costa        supervisor
    (3,  3,  1, 4, CURRENT_DATE - 240, NULL),  -- Henrique Siano     fulltime
    (4,  4,  1, 5, CURRENT_DATE - 180, NULL),  -- Tiago Eiras        parttime
    (5,  5,  1, 5, CURRENT_DATE - 180, NULL),  -- Afonso Barbosa     parttime
    (6,  6,  1, 6, CURRENT_DATE - 150, NULL),  -- Micael Martins     reforco
    (7,  7,  1, 3, CURRENT_DATE - 20,  NULL),  -- Francisco (admin)  subgerente
    (8,  8,  2, 3, CURRENT_DATE - 120, NULL),  -- Ana Sousa          subgerente loja 2
    (9,  9,  3, 2, CURRENT_DATE - 90,  NULL),  -- Carlos Pereira     supervisor loja 3
    (10, 10, 1, 5, CURRENT_DATE - 140, CURRENT_DATE - 30), -- Beatriz Santos (inativa)
    -- Loja 1 extra FT (necessarios para geracao minimo=2 com 3 tipos de turno)
    (16, 15, 1, 4, CURRENT_DATE - 120, NULL),  -- Rita Mendes        fulltime
    (17, 16, 1, 4, CURRENT_DATE - 110, NULL),  -- Pedro Luz          fulltime
    (18, 17, 1, 4, CURRENT_DATE - 100, NULL),  -- Sara Ferreira      fulltime
    (19, 18, 1, 4, CURRENT_DATE - 90,  NULL),  -- Joao Alves         fulltime
    (20, 19, 1, 4, CURRENT_DATE - 80,  NULL),  -- Catarina Cruz      fulltime
    (21, 20, 1, 4, CURRENT_DATE - 70,  NULL),  -- Miguel Rocha       fulltime
    (22, 21, 1, 4, CURRENT_DATE - 60,  NULL),  -- Ines Silva         fulltime
    (23, 22, 1, 4, CURRENT_DATE - 50,  NULL),  -- Luis Pinto         fulltime
    -- Loja 2 — NorteShopping: 1 gerente + 1 subgerente + 1 supervisor + 4 FT + 1 PT = 8 workers
    (11, 1,  2, 1, CURRENT_DATE - 5,   NULL),  -- Francisco Gomes    gerente (multi-loja)
    (12, 11, 2, 2, CURRENT_DATE - 200, NULL),  -- Sofia Marques      supervisor
    (13, 12, 2, 4, CURRENT_DATE - 180, NULL),  -- Diogo Faria        fulltime
    (14, 13, 2, 4, CURRENT_DATE - 150, NULL),  -- Marta Pinto        fulltime
    (15, 14, 2, 5, CURRENT_DATE - 120, NULL),  -- Rui Castro         parttime
    (24, 23, 2, 4, CURRENT_DATE - 90,  NULL),  -- Vera Lopes         fulltime
    (25, 24, 2, 4, CURRENT_DATE - 80,  NULL),  -- Nuno Santos        fulltime
    -- Marco Dias: ligacao ativa em ambas as lojas (cenario multi-loja)
    (26, 25, 1, 4, CURRENT_DATE - 40,  NULL),  -- Marco Dias         fulltime loja 1
    (27, 25, 2, 4, CURRENT_DATE - 40,  NULL);  -- Marco Dias         fulltime loja 2

INSERT INTO public.horarios (id_horario, id_lojautilizador, id_turno, data_turno, estado) VALUES
    (1,  7, 2, CURRENT_DATE + 1, 'aprovado'),
    (2,  7, 1, CURRENT_DATE + 3, 'aprovado'),
    (3,  7, 3, CURRENT_DATE + 6, 'pendente'),
    (4,  3, 1, CURRENT_DATE + 1, 'aprovado'),
    (5,  4, 3, CURRENT_DATE + 1, 'aprovado'),
    (6,  5, 1, CURRENT_DATE + 2, 'aprovado'),
    (7,  6, 3, CURRENT_DATE + 5, 'aprovado'),
    (8,  2, 1, CURRENT_DATE,     'aprovado'),
    (9,  1, 2, CURRENT_DATE,     'aprovado'),
    (10, 3, 2, CURRENT_DATE + 3, 'aprovado'),
    (11, 4, 2, CURRENT_DATE + 2, 'aprovado'),
    (12, 7, 2, CURRENT_DATE - 5, 'aprovado'),
    (13, 3, 1, CURRENT_DATE - 7, 'aprovado'),
    (14, 5, 1, CURRENT_DATE - 2, 'aprovado'),
    (15, 7, 2, CURRENT_DATE + 4, 'aprovado'),
    (16, 3, 3, CURRENT_DATE + 4, 'aprovado'),
    (17, 4, 2, CURRENT_DATE + 6, 'aprovado'),
    (18, 5, 3, CURRENT_DATE + 6, 'aprovado'),
    -- Escala publicada da Levi's NorteShopping (loja 2) — para a demo multi-loja ter
    -- equipa, horario individual e validacao de folgas tambem nesta loja.
    -- id_lojautilizador: 8=Ana(subg) 11=Francisco(ger) 12=Sofia(sup) 13=Diogo(FT) 14=Marta(FT) 15=Rui(PT)
    (19,  8, 2, CURRENT_DATE,     'aprovado'),
    (20, 11, 1, CURRENT_DATE,     'aprovado'),
    (21, 12, 3, CURRENT_DATE,     'aprovado'),
    (22, 13, 1, CURRENT_DATE + 1, 'aprovado'),
    (23, 14, 2, CURRENT_DATE + 1, 'aprovado'),
    (24, 15, 4, CURRENT_DATE + 1, 'aprovado'),
    (25,  8, 1, CURRENT_DATE + 2, 'aprovado'),
    (26, 12, 2, CURRENT_DATE + 2, 'aprovado'),
    (27, 13, 3, CURRENT_DATE + 3, 'aprovado'),
    (28, 14, 1, CURRENT_DATE + 3, 'aprovado'),
    (29, 15, 5, CURRENT_DATE + 3, 'aprovado'),
    (30, 11, 2, CURRENT_DATE + 4, 'aprovado'),
    (31,  8, 3, CURRENT_DATE + 4, 'aprovado'),
    (32, 13, 2, CURRENT_DATE + 5, 'aprovado');

INSERT INTO public.day_offs (id_dayoff, id_utilizador, data_ausencia, motivo, tipo, estado) VALUES
    (1, 7, CURRENT_DATE + 10, 'Fim de semana prolongado com a familia.', 'ferias', 'pendente'),
    (2, 3, CURRENT_DATE + 12, 'Consulta medica ja marcada.', 'folgas', 'aprovado'),
    (3, 4, CURRENT_DATE + 8, 'Recuperacao fisica.', 'baixa', 'recusado'),
    (4, 1, CURRENT_DATE + 15, 'Necessidade pessoal.', 'folgas', 'pendente'),
    (5, 7, CURRENT_DATE + 2, 'Assunto pessoal urgente.', 'folgas', 'aprovado'),
    (6, 5, CURRENT_DATE - 10, 'Baixa medica curta.', 'baixa', 'aprovado'),
    -- Pedido pendente de um colaborador da NorteShopping (Diogo, id 12) — para o gerente
    -- multi-loja poder APROVAR um pedido da loja secundaria ao vivo (prova do bug 14.2 corrigido).
    (7, 12, CURRENT_DATE + 3, 'Compromisso pessoal na NorteShopping.', 'folgas', 'pendente');

INSERT INTO public.preferencias (
    id_preferencia,
    id_utilizador,
    descricao,
    tipo,
    data_inicio,
    data_fim,
    estado,
    decisao,
    id_decisor,
    data_decisao
) VALUES
    -- Francisco Gomes (id=1) — gerente Braga + NorteShopping
    (1,  1, 'Folga para evento familiar.',                                        'folgas',  CURRENT_DATE + 35, CURRENT_DATE + 35, 'aprovado',  'Aprovada. Cobertura assegurada.',                           1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (2,  1, 'Preferencia por turnos da manha nos dias de semana.',                'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (3,  1, 'Prefere trabalhar com Tiago Costa sempre que possivel.',             'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Tiago Costa (id=2) — supervisor loja 1
    (4,  2, 'Pedido de folga para compromisso pessoal.',                          'folgas',  CURRENT_DATE + 42, CURRENT_DATE + 42, 'pendente',  NULL, NULL, NULL),
    (5,  2, 'Preferencia por turnos intermedios.',                                'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (6,  2, 'Gostava de trabalhar com Francisco Gomes nos fins de semana.',       'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Henrique Siano (id=3) — FT loja 1
    (7,  3, 'Folga para tratamento medico.',                                      'folgas',  CURRENT_DATE + 20, CURRENT_DATE + 20, 'aprovado',  'Aprovada. Cobertura assegurada.',                           1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (8,  3, 'Preferencia por turnos da manha.',                                   'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (9,  3, 'Prefere trabalhar com Tiago Eiras.',                                 'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    -- Tiago Eiras (id=4) — PT loja 1
    (10, 4, 'Pedido de folga para evento escolar.',                               'folgas',  CURRENT_DATE + 18, CURRENT_DATE + 18, 'pendente',  NULL, NULL, NULL),
    (11, 4, 'Preferencia por turnos manha part-time.',                            'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (12, 4, 'Prefere trabalhar com Afonso Barbosa.',                              'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    -- Afonso Barbosa (id=5) — PT loja 1
    (13, 5, 'Folga para compromisso familiar.',                                   'folgas',  CURRENT_DATE + 28, CURRENT_DATE + 28, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (14, 5, 'Preferencia por turnos intermedio part-time.',                       'turnos',  NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    (15, 5, 'Gostava de trabalhar com Henrique Siano.',                           'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Micael Martins (id=6) — reforco loja 1
    (16, 6, 'Pedido de folga para o proximo fim de semana.',                      'folgas',  CURRENT_DATE + 7,  CURRENT_DATE + 7,  'pendente',  NULL, NULL, NULL),
    (17, 6, 'Preferencia por turnos manha ao fim de semana.',                     'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (18, 6, 'Prefere trabalhar com Tiago Costa.',                                 'colegas', NULL, NULL,                            'rejeitado', 'Nao compativel com a distribuicao atual.',                  1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    -- Francisco admin (id=7) — subgerente loja 1
    (19, 7, 'Folga para fim de semana prolongado.',                               'folgas',  CURRENT_DATE + 21, CURRENT_DATE + 21, 'pendente',  NULL, NULL, NULL),
    (20, 7, 'Preferencia por turnos da manha.',                                   'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (21, 7, 'Prefere trabalhar com Francisco Gomes.',                             'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Ana Sousa (id=8) — subgerente loja 2
    (22, 8, 'Pedido de folga para compromisso profissional.',                     'folgas',  CURRENT_DATE + 33, CURRENT_DATE + 33, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (23, 8, 'Preferencia por turnos da noite durante a semana.',                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (24, 8, 'Prefere trabalhar com Sofia Marques.',                               'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Sofia Marques (id=11) — supervisor loja 2
    (25, 11, 'Folga pretendida para descanso.',                                   'folgas',  CURRENT_DATE + 40, CURRENT_DATE + 40, 'pendente',  NULL, NULL, NULL),
    (26, 11, 'Preferencia por turnos intermedios.',                               'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (27, 11, 'Gostava de trabalhar com Diogo Faria.',                             'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    -- Diogo Faria (id=12) — FT loja 2
    (28, 12, 'Folga para compromisso pessoal.',                                   'folgas',  CURRENT_DATE + 15, CURRENT_DATE + 15, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (29, 12, 'Preferencia por turnos da manha na NorteShopping.',                 'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (30, 12, 'Prefere trabalhar com Marta Pinto.',                                'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Marta Pinto (id=13) — FT loja 2
    (31, 13, 'Pedido de folga familiar.',                                         'folgas',  CURRENT_DATE + 26, CURRENT_DATE + 26, 'pendente',  NULL, NULL, NULL),
    (32, 13, 'Preferencia por turnos da tarde (intermedio).',                     'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (33, 13, 'Prefere trabalhar com Rui Castro.',                                 'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Rui Castro (id=14) — PT loja 2
    (34, 14, 'Folga para consulta medica.',                                       'folgas',  CURRENT_DATE + 12, CURRENT_DATE + 12, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (35, 14, 'Preferencia por turnos manha part-time.',                           'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (36, 14, 'Prefere trabalhar com Vera Lopes.',                                 'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Rita Mendes (id=15) — FT loja 1
    (37, 15, 'Folga para evento pessoal.',                                        'folgas',  CURRENT_DATE + 22, CURRENT_DATE + 22, 'pendente',  NULL, NULL, NULL),
    (38, 15, 'Preferencia por turnos da noite.',                                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (39, 15, 'Prefere trabalhar com Pedro Luz.',                                  'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    -- Pedro Luz (id=16) — FT loja 1
    (40, 16, 'Pedido de folga por motivos familiares.',                           'folgas',  CURRENT_DATE + 38, CURRENT_DATE + 38, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (41, 16, 'Preferencia por turnos intermedios.',                               'turnos',  NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    (42, 16, 'Prefere trabalhar com Sara Ferreira.',                              'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Sara Ferreira (id=17) — FT loja 1
    (43, 17, 'Folga para compromisso educativo.',                                 'folgas',  CURRENT_DATE + 17, CURRENT_DATE + 17, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (44, 17, 'Preferencia por turnos da manha durante a semana.',                 'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (45, 17, 'Prefere trabalhar com Joao Alves.',                                 'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Joao Alves (id=18) — FT loja 1
    (46, 18, 'Pedido de folga para descanso.',                                    'folgas',  CURRENT_DATE + 45, CURRENT_DATE + 45, 'pendente',  NULL, NULL, NULL),
    (47, 18, 'Preferencia por turnos da noite.',                                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (48, 18, 'Prefere trabalhar com Catarina Cruz.',                              'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    -- Catarina Cruz (id=19) — FT loja 1
    (49, 19, 'Folga para viagem pessoal.',                                        'folgas',  CURRENT_DATE + 30, CURRENT_DATE + 30, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (50, 19, 'Preferencia por turnos da tarde.',                                  'turnos',  NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    (51, 19, 'Prefere trabalhar com Miguel Rocha.',                               'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Miguel Rocha (id=20) — FT loja 1
    (52, 20, 'Pedido de folga por motivos pessoais.',                             'folgas',  CURRENT_DATE + 23, CURRENT_DATE + 23, 'pendente',  NULL, NULL, NULL),
    (53, 20, 'Preferencia por turnos da manha.',                                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (54, 20, 'Prefere trabalhar com Ines Silva.',                                 'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    -- Ines Silva (id=21) — FT loja 1
    (55, 21, 'Folga para evento familiar.',                                       'folgas',  CURRENT_DATE + 36, CURRENT_DATE + 36, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (56, 21, 'Preferencia por turnos intermedios da tarde.',                      'turnos',  NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    (57, 21, 'Prefere trabalhar com Luis Pinto.',                                 'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Luis Pinto (id=22) — FT loja 1
    (58, 22, 'Pedido de folga para consulta medica.',                             'folgas',  CURRENT_DATE + 11, CURRENT_DATE + 11, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '4 days'),
    (59, 22, 'Preferencia por turnos da noite.',                                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (60, 22, 'Prefere trabalhar com Rita Mendes.',                                'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Vera Lopes (id=23) — FT loja 2
    (61, 23, 'Folga para reuniao escolar dos filhos.',                            'folgas',  CURRENT_DATE + 19, CURRENT_DATE + 19, 'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (62, 23, 'Preferencia por turnos da manha.',                                  'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (63, 23, 'Prefere trabalhar com Nuno Santos.',                                'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    -- Nuno Santos (id=24) — FT loja 2
    (64, 24, 'Pedido de folga para evento desportivo.',                           'folgas',  CURRENT_DATE + 44, CURRENT_DATE + 44, 'pendente',  NULL, NULL, NULL),
    (65, 24, 'Preferencia por turnos intermedios.',                               'turnos',  NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '3 days'),
    (66, 24, 'Prefere trabalhar com Vera Lopes.',                                 'colegas', NULL, NULL,                            'aprovado',  'Aprovada.',                                                 1, CURRENT_TIMESTAMP - INTERVAL '2 days'),
    -- Marco Dias (id=25) — FT multi-loja (loja 1 + loja 2)
    (67, 25, 'Folga para assunto pessoal urgente.',                               'folgas',  CURRENT_DATE + 29, CURRENT_DATE + 29, 'pendente',  NULL, NULL, NULL),
    (68, 25, 'Preferencia por turnos da manha em ambas as lojas.',                'turnos',  NULL, NULL,                            'pendente',  NULL, NULL, NULL),
    (69, 25, 'Prefere trabalhar com Francisco Gomes.',                            'colegas', NULL, NULL,                            'pendente',  NULL, NULL, NULL);

INSERT INTO public.horarios_especiais_loja (id_horario_especial, id_loja, descricao, data_inicio, data_fim, loja_encerrada, observacoes) VALUES
    (1, 1, 'Encerramento para inventario anual', CURRENT_DATE + 30, CURRENT_DATE + 30, TRUE, 'Encerramento total para contagem de stock.');

INSERT INTO public.permutas (id_permuta, id_horario_origem, id_horario_destino, estado, data_pedido) VALUES
    (1, 15, 16, 'pendente', CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (2, 17, 18, 'aprovada', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    -- Permuta pendente na NorteShopping (Diogo h27 <-> Marta h28, CURRENT_DATE+3) — para o
    -- gerente multi-loja aprovar uma permuta da loja secundaria ao vivo na demo.
    (3, 27, 28, 'pendente', CURRENT_TIMESTAMP - INTERVAL '2 hours');

INSERT INTO public.notificacao (id, id_utilizador, mensagem, lida, data_envio) VALUES
    (1, 7, 'O teu pedido de folga para ' || (CURRENT_DATE + 10)::text || ' foi recebido e esta pendente de aprovacao.', false, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (2, 3, 'A tua preferencia de turno da manha foi aprovada pelo gestor.', true,  CURRENT_TIMESTAMP - INTERVAL '2 days'),
    (3, 4, 'A tua preferencia de colega foi rejeitada. Motivo: incompatibilidade de cobertura.', false, CURRENT_TIMESTAMP - INTERVAL '1 day'),
    (4, 7, 'A permuta de turno com Henrique Siano esta pendente de aprovacao.', false, CURRENT_TIMESTAMP - INTERVAL '30 minutes');

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
    PERFORM setval(pg_get_serial_sequence('public.regras', 'id_regra'), COALESCE((SELECT MAX(id_regra) FROM public.regras), 11), true);
    PERFORM setval(pg_get_serial_sequence('public.turnos', 'id_turno'), COALESCE((SELECT MAX(id_turno) FROM public.turnos), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.utilizadores', 'id_utilizador'), COALESCE((SELECT MAX(id_utilizador) FROM public.utilizadores), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.lojautilizador', 'id_lojautilizador'), COALESCE((SELECT MAX(id_lojautilizador) FROM public.lojautilizador), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.regras_loja', 'id_regra_loja'), COALESCE((SELECT MAX(id_regra_loja) FROM public.regras_loja), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios', 'id_horario'), COALESCE((SELECT MAX(id_horario) FROM public.horarios), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.day_offs', 'id_dayoff'), COALESCE((SELECT MAX(id_dayoff) FROM public.day_offs), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.preferencias', 'id_preferencia'), COALESCE((SELECT MAX(id_preferencia) FROM public.preferencias), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios_especiais_loja', 'id_horario_especial'), COALESCE((SELECT MAX(id_horario_especial) FROM public.horarios_especiais_loja), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.permutas', 'id_permuta'), COALESCE((SELECT MAX(id_permuta) FROM public.permutas), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.permutas_folga', 'id_permuta_folga'), COALESCE((SELECT MAX(id_permuta_folga) FROM public.permutas_folga), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.propostas_horario_mensal', 'id_proposta_horario'), COALESCE((SELECT MAX(id_proposta_horario) FROM public.propostas_horario_mensal), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.eventos_auditoria', 'id_evento'), COALESCE((SELECT MAX(id_evento) FROM public.eventos_auditoria), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.notificacao', 'id'), COALESCE((SELECT MAX(id) FROM public.notificacao), 1), true);
END $$;

COMMIT;
