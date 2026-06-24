BEGIN;

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Carga contratual mensal gestão (horas)', 176, 'contratual'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Carga contratual mensal gestão (horas)')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Carga contratual mensal full-time (horas)', 176, 'contratual'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Carga contratual mensal full-time (horas)')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Carga contratual mensal part-time (horas)', 96, 'contratual'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Carga contratual mensal part-time (horas)')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Carga contratual mensal reforço de fim de semana (horas)', 64, 'contratual'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Carga contratual mensal reforço de fim de semana (horas)')
);

COMMIT;
