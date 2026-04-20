-- Estrutura para suportar validacao da proposta mensal pelo supervisor

ALTER TYPE public.estado_horario_enum
    ADD VALUE IF NOT EXISTS 'rejeitado';

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
