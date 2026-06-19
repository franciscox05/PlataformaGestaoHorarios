-- Migração de esquema — Junho 2026
-- Aplica as alterações de schema sem apagar dados existentes.
-- Seguro para correr múltiplas vezes (idempotente).

BEGIN;

-- 1. tipo_turno_enum → VARCHAR(50) (só executa se o tipo ainda for enum)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'turnos'
          AND column_name = 'tipo' AND udt_name = 'tipo_turno_enum'
    ) THEN
        EXECUTE 'ALTER TABLE public.turnos ALTER COLUMN tipo TYPE VARCHAR(50)';
        RAISE NOTICE 'turnos.tipo convertido de enum para VARCHAR(50).';
    ELSE
        RAISE NOTICE 'turnos.tipo ja e VARCHAR — nada a fazer.';
    END IF;
END $$;

-- 2. Nome do turno
ALTER TABLE public.turnos
    ADD COLUMN IF NOT EXISTS nome VARCHAR(100);

-- 3. Preenche nome nos turnos existentes que ainda nao tenham (usa o tipo como fallback)
UPDATE public.turnos
SET nome = CASE tipo
    WHEN 'manha'      THEN 'Manhã'
    WHEN 'tarde'      THEN 'Tarde'
    WHEN 'intermedio' THEN 'Intermédio'
    WHEN 'noite'      THEN 'Noite'
    ELSE initcap(tipo)
END
WHERE nome IS NULL OR nome = '';

-- 4. Flag de ativo nas regras_loja
ALTER TABLE public.regras_loja
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- 5. Regras privadas por loja
ALTER TABLE public.regras
    ADD COLUMN IF NOT EXISTS id_loja_privada INTEGER
        REFERENCES public.lojas(id_loja) ON DELETE CASCADE;

-- 6. Tabela de permutas de folga
CREATE TABLE IF NOT EXISTS public.permutas_folga (
    id_permuta_folga  SERIAL PRIMARY KEY,
    id_horario_d      INTEGER NOT NULL REFERENCES public.horarios(id_horario),
    id_horario_y      INTEGER NOT NULL REFERENCES public.horarios(id_horario),
    estado            VARCHAR(20) NOT NULL DEFAULT 'pendente',
    data_pedido       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pf_estado    CHECK (estado IN ('pendente','aprovado','rejeitado','cancelado')),
    CONSTRAINT chk_pf_diferente CHECK (id_horario_d <> id_horario_y)
);

CREATE INDEX IF NOT EXISTS idx_pf_horario_d ON public.permutas_folga(id_horario_d);
CREATE INDEX IF NOT EXISTS idx_pf_horario_y ON public.permutas_folga(id_horario_y);
CREATE INDEX IF NOT EXISTS idx_pf_estado    ON public.permutas_folga(estado);

-- 7. Flag de ativo/inativo nos turnos (entidade Turno.java, commit 3fc24a8)
ALTER TABLE public.turnos
    ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

COMMIT;
