-- Setup completo para guiao de testes (blocos 2, 3)
BEGIN;

-- T06: Loja
INSERT INTO public.lojas (nome, localizacao, hora_abertura, hora_fecho)
VALUES ('Levis Braga Parque', 'Braga', '10:00', '23:00');

-- T07: Turnos (os 4 ja foram inseridos antes deste script -- ignorar se ja existem)

-- T08/T09: Regras globais (ja inseridas)

-- Associar regras a loja
INSERT INTO public.regras_loja (id_loja, id_regra, valor_especifico, ativo)
SELECT
  (SELECT id_loja FROM public.lojas LIMIT 1),
  id_regra,
  valor_padrao,
  true
FROM public.regras;

-- T10: Horario especial (dia encerrado)
INSERT INTO public.horarios_especiais_loja
  (id_loja, descricao, data_inicio, data_fim, loja_encerrada, minimo_colaboradores_turno, observacoes)
VALUES (
  (SELECT id_loja FROM public.lojas LIMIT 1),
  'Inventario',
  (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '29 days')::date,
  (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '29 days')::date,
  true,
  NULL,
  'Loja encerrada para inventario anual'
);

-- T11: Francisco como Gerente
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'francisco@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'gerente'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);

-- T12: Tiago como Sub-Gerente
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'tiago.costa@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'subgerente'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);

-- T14: Ana, Bruno (FT), Carla, David (PT), Eva (Reforco), Fabio (Supervisor)
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'ana@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'fulltime'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'bruno@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'fulltime'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'carla@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'parttime'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'david@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'parttime'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'eva@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'reforco_parttime'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);
INSERT INTO public.lojautilizador (id_utilizador, id_loja, id_cargo, data_inicio)
VALUES (
  (SELECT id_utilizador FROM public.utilizadores WHERE email = 'fabio@levis.com'),
  (SELECT id_loja FROM public.lojas LIMIT 1),
  (SELECT id_cargo FROM public.cargos WHERE tipo = 'supervisor'::tipo_cargo_enum LIMIT 1),
  CURRENT_DATE
);

COMMIT;

-- Verificacao final
SELECT 'LOJA' as entidade, COUNT(*)::text as total FROM public.lojas
UNION ALL SELECT 'TURNOS', COUNT(*)::text FROM public.turnos
UNION ALL SELECT 'REGRAS_LOJA', COUNT(*)::text FROM public.regras_loja
UNION ALL SELECT 'HORARIOS_ESPECIAIS', COUNT(*)::text FROM public.horarios_especiais_loja
UNION ALL SELECT 'LOJAUTILIZADOR', COUNT(*)::text FROM public.lojautilizador;

SELECT u.nome, c.nome as cargo, c.tipo FROM public.lojautilizador lu
JOIN public.utilizadores u ON u.id_utilizador = lu.id_utilizador
JOIN public.cargos c ON c.id_cargo = lu.id_cargo
ORDER BY lu.id_lojautilizador;
