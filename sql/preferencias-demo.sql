-- Aprovar preferencias pendentes do Bruno Costa
UPDATE public.preferencias SET
    estado = 'aprovado', decisao = 'Aprovado.', id_decisor = 7,
    data_decisao = CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE id_preferencia IN (1, 3);

-- Francisco Gomes (gerente, id=7)
INSERT INTO public.preferencias (id_utilizador, descricao, tipo, data_inicio, data_fim, prioridade, estado, decisao, id_decisor, data_decisao) VALUES
(7, 'Turnos preferidos: tarde.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '5 days'),
(7, 'Folga preferida: Domingo.', 'folga_preferida', '2026-07-05', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '5 days'),
(7, 'Tiago Costa', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '5 days'),

-- Tiago Costa (sub-gerente, id=2)
(2, 'Turnos preferidos: tarde.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '4 days'),
(2, 'Folga preferida: Segunda-feira.', 'folga_preferida', '2026-07-06', NULL, 4, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '4 days'),
(2, 'Fabio Santos', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '4 days'),

-- Ana Silva (FT, id=38)
(38, 'Turnos preferidos: manha.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '4 days'),
(38, 'Folga preferida: Terca-feira.', 'folga_preferida', '2026-07-07', NULL, 4, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '3 days'),
(38, 'Bruno Costa', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '3 days'),

-- Carla Mendes (PT, id=40)
(40, 'Turnos preferidos: manha.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '3 days'),
(40, 'Folga preferida: Quarta-feira.', 'folga_preferida', '2026-07-01', NULL, 4, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),
(40, 'David Ferreira', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- David Ferreira (PT, id=41)
(41, 'Turnos preferidos: tarde.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),
(41, 'Folga preferida: Sexta-feira.', 'folga_preferida', '2026-07-03', NULL, 4, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),
(41, 'Carla Mendes', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '2 days'),

-- Eva Rodrigues (Reforco FDS, id=42)
(42, 'Turnos preferidos: noite.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(42, 'Folga preferida: Segunda-feira.', 'folga_preferida', '2026-07-06', NULL, 4, 'aprovado', 'Aprovado. Descanso apos FDS.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(42, 'Ana Silva', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Fabio Santos (supervisor, id=43)
(43, 'Turnos preferidos: tarde.', 'turnos', '2026-07-01', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(43, 'Folga preferida: Domingo.', 'folga_preferida', '2026-07-05', NULL, 3, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day'),
(43, 'Tiago Costa', 'colegas', NULL, NULL, 2, 'aprovado', 'Aprovado.', 7, CURRENT_TIMESTAMP - INTERVAL '1 day');

SELECT u.nome, p.tipo, p.descricao, p.estado
FROM public.preferencias p
JOIN public.utilizadores u ON u.id_utilizador = p.id_utilizador
ORDER BY u.nome, p.tipo;
