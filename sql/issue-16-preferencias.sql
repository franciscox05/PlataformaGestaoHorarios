-- Evolucao da tabela preferencias para suportar o modulo desktop

ALTER TABLE public.preferencias
    ADD COLUMN IF NOT EXISTS tipo varchar(50),
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS prioridade integer,
    ADD COLUMN IF NOT EXISTS estado varchar(50);

UPDATE public.preferencias
SET tipo = COALESCE(tipo, 'folgas'),
    prioridade = COALESCE(prioridade, 3),
    estado = COALESCE(estado, 'pendente');

ALTER TABLE public.preferencias
    ALTER COLUMN tipo SET DEFAULT 'folgas',
    ALTER COLUMN prioridade SET DEFAULT 3,
    ALTER COLUMN estado SET DEFAULT 'pendente';

ALTER TABLE public.preferencias
    ALTER COLUMN tipo SET NOT NULL,
    ALTER COLUMN prioridade SET NOT NULL,
    ALTER COLUMN estado SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_preferencias_tipo'
    ) THEN
        ALTER TABLE public.preferencias
            ADD CONSTRAINT chk_preferencias_tipo
            CHECK (LOWER(tipo) IN ('folgas', 'ferias', 'colegas', 'turnos'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_preferencias_prioridade'
    ) THEN
        ALTER TABLE public.preferencias
            ADD CONSTRAINT chk_preferencias_prioridade
            CHECK (prioridade BETWEEN 1 AND 5);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_preferencias_estado'
    ) THEN
        ALTER TABLE public.preferencias
            ADD CONSTRAINT chk_preferencias_estado
            CHECK (LOWER(estado) IN ('pendente', 'aprovado', 'rejeitado'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_preferencias_utilizador
    ON public.preferencias (id_utilizador);

CREATE INDEX IF NOT EXISTS idx_preferencias_utilizador_tipo
    ON public.preferencias (id_utilizador, tipo);
