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
