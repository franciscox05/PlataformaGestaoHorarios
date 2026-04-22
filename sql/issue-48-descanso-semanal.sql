BEGIN;

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Descanso semanal minimo (dias)', 2, 'descanso'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Descanso semanal minimo (dias)')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Janela de rotacao de fins de semana (semanas)', 2, 'descanso'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Janela de rotacao de fins de semana (semanas)')
);

COMMIT;
