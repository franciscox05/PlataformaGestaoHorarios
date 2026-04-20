-- Estrutura para suportar auditoria basica de autenticacao e sessao

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

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_data
    ON public.eventos_auditoria (data_evento DESC);

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_utilizador
    ON public.eventos_auditoria (id_utilizador, data_evento DESC);

CREATE INDEX IF NOT EXISTS idx_eventos_auditoria_sessao
    ON public.eventos_auditoria (identificador_sessao);
