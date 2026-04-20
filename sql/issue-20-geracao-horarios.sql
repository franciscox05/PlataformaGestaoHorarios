-- Estrutura para suportar propostas mensais de horario

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

ALTER TABLE public.horarios
    ADD COLUMN IF NOT EXISTS id_proposta_horario integer;

ALTER TABLE public.horarios
    DROP CONSTRAINT IF EXISTS fk_horarios_proposta_horario;

ALTER TABLE public.horarios
    ADD CONSTRAINT fk_horarios_proposta_horario
        FOREIGN KEY (id_proposta_horario) REFERENCES public.propostas_horario_mensal(id_proposta_horario);

CREATE INDEX IF NOT EXISTS idx_proposta_horario_loja_periodo
    ON public.propostas_horario_mensal (id_loja, ano, mes, data_geracao DESC);

CREATE INDEX IF NOT EXISTS idx_horarios_id_proposta_horario
    ON public.horarios (id_proposta_horario);
