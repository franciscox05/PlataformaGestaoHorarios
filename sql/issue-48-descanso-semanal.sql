BEGIN;

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Descanso semanal mínimo (dias)', 2, 'descanso'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Descanso semanal mínimo (dias)')
);

INSERT INTO public.regras (descricao, valor_padrao, tipo)
SELECT 'Janela de rotação de fins de semana (semanas)', 2, 'descanso'
WHERE NOT EXISTS (
    SELECT 1
    FROM public.regras
    WHERE LOWER(descricao) = LOWER('Janela de rotação de fins de semana (semanas)')
);

COMMIT;
