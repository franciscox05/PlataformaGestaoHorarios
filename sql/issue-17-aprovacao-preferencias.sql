-- Evolucao da tabela preferencias para suportar aprovacao pelo gerente

ALTER TABLE public.preferencias
    ADD COLUMN IF NOT EXISTS tipo varchar(50),
    ADD COLUMN IF NOT EXISTS data_inicio date,
    ADD COLUMN IF NOT EXISTS data_fim date,
    ADD COLUMN IF NOT EXISTS prioridade integer,
    ADD COLUMN IF NOT EXISTS estado varchar(50),
    ADD COLUMN IF NOT EXISTS decisao text,
    ADD COLUMN IF NOT EXISTS id_decisor integer,
    ADD COLUMN IF NOT EXISTS data_decisao timestamp without time zone;

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

ALTER TABLE public.preferencias
    DROP CONSTRAINT IF EXISTS fk_preferencias_decisor;

ALTER TABLE public.preferencias
    ADD CONSTRAINT fk_preferencias_decisor
        FOREIGN KEY (id_decisor) REFERENCES public.utilizadores(id_utilizador);

UPDATE public.preferencias
SET decisao = NULL
WHERE decisao IS NOT NULL
  AND BTRIM(decisao) = '';

CREATE INDEX IF NOT EXISTS idx_preferencias_estado
    ON public.preferencias (estado);

CREATE INDEX IF NOT EXISTS idx_preferencias_decisor
    ON public.preferencias (id_decisor);

CREATE INDEX IF NOT EXISTS idx_preferencias_data_decisao
    ON public.preferencias (data_decisao);
