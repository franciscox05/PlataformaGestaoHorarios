BEGIN;

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Dia limite de lançamento do horário mensal', 15, 'administrativo'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Dia limite de lançamento do horário mensal')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Presença de gerente ou subgerente aos sábados', 1, 'operacional'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Presença de gerente ou subgerente aos sábados')
);

COMMIT;
