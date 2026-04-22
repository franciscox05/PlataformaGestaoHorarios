BEGIN;

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Dia limite de lancamento do horario mensal', 15, 'administrativo'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Dia limite de lancamento do horario mensal')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Presenca de gerente ou subgerente aos sabados', 1, 'operacional'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Presenca de gerente ou subgerente aos sabados')
);

COMMIT;
