-- Massa de dados para testar as funcionalidades de maio de 2026.
-- Pode ser executado varias vezes: remove apenas os dados marcados como TESTE MAIO 2026.

BEGIN;

DELETE FROM public.preferencias
WHERE descricao LIKE '[TESTE MAIO 2026]%';

WITH ligacoes_ativas AS (
    SELECT
        lu.id_loja,
        u.id_utilizador,
        u.nome,
        COALESCE(c.tipo::text, '') AS tipo_cargo,
        ROW_NUMBER() OVER (PARTITION BY lu.id_loja ORDER BY LOWER(u.nome), u.id_utilizador) AS ordem_loja
    FROM public.lojautilizador lu
    JOIN public.utilizadores u ON u.id_utilizador = lu.id_utilizador
    JOIN public.cargos c ON c.id_cargo = lu.id_cargo
    WHERE lu.data_fim IS NULL
      AND LOWER(COALESCE(u.estado::text, 'ativo')) = 'ativo'
),
decisores AS (
    SELECT DISTINCT ON (id_loja)
        id_loja,
        id_utilizador AS id_decisor
    FROM ligacoes_ativas
    WHERE LOWER(tipo_cargo) IN ('gerente', 'subgerente', 'supervisor')
    ORDER BY id_loja,
             CASE LOWER(tipo_cargo)
                 WHEN 'gerente' THEN 1
                 WHEN 'subgerente' THEN 2
                 ELSE 3
             END,
             id_utilizador
),
colegas AS (
    SELECT
        l.id_utilizador,
        c.nome AS nome_colega
    FROM ligacoes_ativas l
    LEFT JOIN LATERAL (
        SELECT l2.nome
        FROM ligacoes_ativas l2
        WHERE l2.id_loja = l.id_loja
          AND l2.id_utilizador <> l.id_utilizador
        ORDER BY ABS(l2.ordem_loja - l.ordem_loja), LOWER(l2.nome)
        LIMIT 1
    ) c ON true
)
INSERT INTO public.preferencias (
    id_utilizador,
    descricao,
    tipo,
    data_inicio,
    data_fim,
    prioridade,
    estado,
    decisao,
    id_decisor,
    data_decisao
)
SELECT
    l.id_utilizador,
    '[TESTE MAIO 2026] Prefere folga operacional no dia ' || TO_CHAR(DATE '2026-05-05' + (((l.ordem_loja - 1) % 10)::int), 'DD/MM/YYYY') || '.',
    'folgas',
    DATE '2026-05-05' + (((l.ordem_loja - 1) % 10)::int),
    DATE '2026-05-05' + (((l.ordem_loja - 1) % 10)::int),
    3,
    CASE WHEN MOD(l.ordem_loja, 3) = 0 THEN 'pendente' ELSE 'aprovado' END,
    CASE WHEN MOD(l.ordem_loja, 3) = 0 THEN NULL ELSE 'Preferencia aceite para validar a geracao de maio.' END,
    CASE WHEN MOD(l.ordem_loja, 3) = 0 THEN NULL ELSE d.id_decisor END,
    CASE WHEN MOD(l.ordem_loja, 3) = 0 THEN NULL ELSE CURRENT_TIMESTAMP - INTERVAL '2 days' END
FROM ligacoes_ativas l
LEFT JOIN decisores d ON d.id_loja = l.id_loja

UNION ALL

SELECT
    l.id_utilizador,
    '[TESTE MAIO 2026] Preferencia de ferias curtas para testar bloqueios no planeamento.',
    'ferias',
    DATE '2026-05-20' + (((l.ordem_loja - 1) % 5)::int),
    DATE '2026-05-21' + (((l.ordem_loja - 1) % 5)::int),
    4,
    CASE WHEN MOD(l.ordem_loja, 4) = 0 THEN 'pendente' ELSE 'aprovado' END,
    CASE WHEN MOD(l.ordem_loja, 4) = 0 THEN NULL ELSE 'Ferias aprovadas para testar ausencias no horario.' END,
    CASE WHEN MOD(l.ordem_loja, 4) = 0 THEN NULL ELSE d.id_decisor END,
    CASE WHEN MOD(l.ordem_loja, 4) = 0 THEN NULL ELSE CURRENT_TIMESTAMP - INTERVAL '1 day' END
FROM ligacoes_ativas l
LEFT JOIN decisores d ON d.id_loja = l.id_loja

UNION ALL

SELECT
    l.id_utilizador,
    '[TESTE MAIO 2026] Preferencia permanente por turnos da manha durante maio.',
    'turnos',
    DATE '2026-05-01',
    NULL,
    2,
    'aprovado',
    'Preferencia permanente aprovada para testar prioridades suaves do algoritmo.',
    d.id_decisor,
    CURRENT_TIMESTAMP - INTERVAL '3 days'
FROM ligacoes_ativas l
LEFT JOIN decisores d ON d.id_loja = l.id_loja

UNION ALL

SELECT
    l.id_utilizador,
    '[TESTE MAIO 2026] Preferencia por trabalhar com ' || COALESCE(c.nome_colega, 'um colega da loja') || ' em dias de maior movimento.',
    'colegas',
    DATE '2026-05-01',
    NULL,
    2,
    CASE WHEN MOD(l.ordem_loja, 5) = 0 THEN 'rejeitado' ELSE 'aprovado' END,
    CASE WHEN MOD(l.ordem_loja, 5) = 0
        THEN 'Nao foi possivel acomodar sem afetar a cobertura da loja.'
        ELSE 'Preferencia aprovada para testar afinidade entre colegas.'
    END,
    d.id_decisor,
    CURRENT_TIMESTAMP - INTERVAL '1 day'
FROM ligacoes_ativas l
LEFT JOIN decisores d ON d.id_loja = l.id_loja
LEFT JOIN colegas c ON c.id_utilizador = l.id_utilizador;

COMMIT;
