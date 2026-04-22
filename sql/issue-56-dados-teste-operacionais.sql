BEGIN;

-- Seed complementar e repetivel para cenarios operacionais criticos.
-- Este script NAO substitui a demo principal. Foi pensado para ser aplicado
-- depois de sql/demo-entrega.sql e acrescenta apenas registos de teste
-- reservados no intervalo 56001..56099.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM public.lojas
        WHERE id_loja = 1
    ) THEN
        RAISE EXCEPTION 'Executa primeiro sql/demo-entrega.sql antes deste seed operacional.';
    END IF;
END $$;

-- Limpeza controlada apenas do intervalo reservado para esta issue.
DELETE FROM public.historico_horario_estados
WHERE id_registo BETWEEN 56001 AND 56099;

DELETE FROM public.eventos_auditoria
WHERE id_evento BETWEEN 56001 AND 56099;

DELETE FROM public.permutas
WHERE id_permuta BETWEEN 56001 AND 56099;

DELETE FROM public.day_offs
WHERE id_dayoff BETWEEN 56001 AND 56099;

DELETE FROM public.preferencias
WHERE id_preferencia BETWEEN 56001 AND 56099;

DELETE FROM public.horarios_especiais_loja
WHERE id_horario_especial BETWEEN 56001 AND 56099;

DELETE FROM public.horarios
WHERE id_horario BETWEEN 56001 AND 56099;

DELETE FROM public.lojautilizador
WHERE id_lojautilizador BETWEEN 56001 AND 56099;

DELETE FROM public.utilizadores
WHERE id_utilizador BETWEEN 56001 AND 56099;

INSERT INTO public.utilizadores (id_utilizador, nome, email, telemovel, password_hash, estado) VALUES
    (56001, 'Gestor Operacional', 'gestor.operacional@levis.com', '913560001', '123456', 'ativo'),
    (56002, 'Supervisor Operacional', 'supervisor.operacional@levis.com', '913560002', '123456', 'ativo'),
    (56003, 'Sofia Almeida', 'sofia.almeida.operacional@levis.com', '913560003', '123456', 'ativo'),
    (56004, 'Rui Matos', 'rui.matos.operacional@levis.com', '913560004', '123456', 'ativo'),
    (56005, 'Marta Cunha', 'marta.cunha.operacional@levis.com', '913560005', '123456', 'ativo'),
    (56006, 'Diogo Lopes', 'diogo.lopes.operacional@levis.com', '913560006', '123456', 'ativo'),
    (56007, 'Joana Silva', 'joana.silva.operacional@levis.com', '913560007', '123456', 'ativo'),
    (56008, 'Ines Rocha', 'ines.rocha.operacional@levis.com', '913560008', '123456', 'inativo');

INSERT INTO public.lojautilizador (id_lojautilizador, id_utilizador, id_loja, id_cargo, data_inicio, data_fim) VALUES
    (56001, 56001, 1, 1, CURRENT_DATE - 320, NULL),
    (56002, 56002, 1, 2, CURRENT_DATE - 280, NULL),
    (56003, 56003, 1, 4, CURRENT_DATE - 220, NULL),
    (56004, 56004, 1, 4, CURRENT_DATE - 190, NULL),
    (56005, 56005, 1, 5, CURRENT_DATE - 160, NULL),
    (56006, 56006, 1, 6, CURRENT_DATE - 120, NULL),
    (56007, 56007, 1, 4, CURRENT_DATE - 140, NULL),
    (56008, 56008, 1, 5, CURRENT_DATE - 180, CURRENT_DATE - 12);

-- Horarios do dia e dos dias seguintes para testar dashboard, contexto da loja
-- e pedidos pendentes sem depender apenas dos utilizadores demo base.
INSERT INTO public.horarios (id_horario, id_lojautilizador, id_turno, data_turno, estado) VALUES
    (56001, 56001, 2, CURRENT_DATE, 'aprovado'),
    (56002, 56002, 1, CURRENT_DATE, 'aprovado'),
    (56003, 56003, 1, CURRENT_DATE, 'aprovado'),
    (56004, 56004, 2, CURRENT_DATE, 'aprovado'),
    (56005, 56005, 4, CURRENT_DATE, 'aprovado'),
    (56006, 56006, 6, CURRENT_DATE, 'aprovado'),
    (56007, 56007, 3, CURRENT_DATE, 'aprovado'),

    (56008, 56001, 2, CURRENT_DATE + 1, 'aprovado'),
    (56009, 56002, 1, CURRENT_DATE + 1, 'aprovado'),
    (56010, 56003, 1, CURRENT_DATE + 1, 'aprovado'),
    (56011, 56004, 3, CURRENT_DATE + 1, 'aprovado'),
    (56012, 56005, 4, CURRENT_DATE + 1, 'aprovado'),
    (56013, 56006, 6, CURRENT_DATE + 1, 'aprovado'),
    (56014, 56007, 2, CURRENT_DATE + 1, 'aprovado'),

    (56015, 56001, 1, CURRENT_DATE + 2, 'aprovado'),
    (56016, 56002, 2, CURRENT_DATE + 2, 'aprovado'),
    (56017, 56003, 2, CURRENT_DATE + 2, 'aprovado'),
    (56018, 56004, 3, CURRENT_DATE + 2, 'aprovado'),
    (56019, 56007, 5, CURRENT_DATE + 2, 'aprovado'),

    (56020, 56001, 2, CURRENT_DATE + 4, 'aprovado'),
    (56021, 56002, 1, CURRENT_DATE + 4, 'aprovado'),
    (56022, 56003, 2, CURRENT_DATE + 4, 'aprovado'),
    (56023, 56004, 3, CURRENT_DATE + 4, 'aprovado'),
    (56024, 56005, 5, CURRENT_DATE + 4, 'aprovado'),
    (56025, 56006, 6, CURRENT_DATE + 4, 'aprovado'),
    (56026, 56007, 1, CURRENT_DATE + 4, 'aprovado'),

    (56027, 56003, 1, CURRENT_DATE + 6, 'aprovado'),
    (56028, 56004, 2, CURRENT_DATE + 6, 'aprovado'),
    (56029, 56005, 4, CURRENT_DATE + 6, 'aprovado'),
    (56030, 56007, 3, CURRENT_DATE + 6, 'aprovado');

-- Pedidos de folga e ausencias em estados diferentes para testar contexto
-- operacional do gerente e o historico do colaborador.
INSERT INTO public.day_offs (id_dayoff, id_utilizador, data_ausencia, motivo, tipo, estado) VALUES
    (56001, 56005, CURRENT_DATE + 2, 'Consulta medica com follow-up.', 'folgas', 'aprovado'),
    (56002, 56007, CURRENT_DATE + 11, 'Fim de semana familiar prolongado.', 'folgas', 'pendente'),
    (56003, 56004, CURRENT_DATE + 15, 'Formacao interna da marca.', 'folgas', 'aprovado'),
    (56004, 56006, CURRENT_DATE + 18, 'Recuperacao fisica apos atividade desportiva.', 'baixa', 'recusado');

-- Preferencias pensadas para testar cenarios permanentes, temporarios,
-- colegas, turnos e pedidos ainda por decidir.
INSERT INTO public.preferencias (
    id_preferencia,
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
) VALUES
    (56001, 56003, 'Preferencia permanente por turnos da manha e abertura da loja.', 'turnos', CURRENT_DATE - 30, NULL, 1, 'aprovado', 'Mantida como caso de teste para preferencia sem data fim.', 56001, CURRENT_TIMESTAMP - INTERVAL '7 days'),
    (56002, 56005, 'Preferencia permanente para trabalhar com Rui Matos sempre que possivel.', 'colegas', CURRENT_DATE - 25, NULL, 1, 'aprovado', 'Util para testar colegas e preferencias permanentes.', 56001, CURRENT_TIMESTAMP - INTERVAL '6 days'),
    (56003, 56007, 'Pedido de folga para o fim de semana do festival da cidade.', 'folgas', CURRENT_DATE + 11, CURRENT_DATE + 12, 2, 'pendente', NULL, NULL, NULL),
    (56004, 56006, 'Ferias planeadas para a primeira semana do proximo mes.', 'ferias', (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '3 day')::date, (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month' + INTERVAL '6 day')::date, 3, 'aprovado', 'Ferias aprovadas para testar bloqueios na geracao.', 56001, CURRENT_TIMESTAMP - INTERVAL '5 days'),
    (56005, 56004, 'Preferencia por turnos intermedios durante a campanha especial.', 'turnos', CURRENT_DATE + 14, CURRENT_DATE + 18, 2, 'pendente', NULL, NULL, NULL),
    (56006, 56002, 'Preferencia para trabalhar com Joana Silva nos inventarios mensais.', 'colegas', CURRENT_DATE - 10, NULL, 2, 'rejeitado', 'Nao foi possivel acomodar esta preferencia sem comprometer a cobertura.', 56001, CURRENT_TIMESTAMP - INTERVAL '2 days');

-- Permutas para testar pendentes e aprovadas com contexto real no mesmo dia.
INSERT INTO public.permutas (id_permuta, id_horario_origem, id_horario_destino, estado, data_pedido) VALUES
    (56001, 56022, 56023, 'pendente', CURRENT_TIMESTAMP - INTERVAL '12 hours'),
    (56002, 56029, 56030, 'aprovada', CURRENT_TIMESTAMP - INTERVAL '3 days');

-- Horarios especiais para testar dashboard de gestao, contexto do painel e motor.
INSERT INTO public.horarios_especiais_loja (
    id_horario_especial,
    id_loja,
    descricao,
    data_inicio,
    data_fim,
    hora_abertura,
    hora_fecho,
    minimo_colaboradores_turno,
    loja_encerrada,
    observacoes
) VALUES
    (56001, 1, 'Encerramento tecnico para manutencao de rede', CURRENT_DATE + 14, CURRENT_DATE + 14, NULL, NULL, NULL, TRUE, 'A loja fica totalmente encerrada neste dia.'),
    (56002, 1, 'Campanha especial de tarde', CURRENT_DATE + 15, CURRENT_DATE + 15, '12:00', '21:00', 2, FALSE, 'Ideal para testar filtragem de turnos compativeis e contexto do gerente.'),
    (56003, 1, 'Fim de semana com afluencia reforcada', CURRENT_DATE + 16, CURRENT_DATE + 17, NULL, NULL, 4, FALSE, 'Usado para testar minimos especiais por turno.');

-- Auditoria e estados para ter contexto adicional nas vistas de gestao.
INSERT INTO public.eventos_auditoria (
    id_evento,
    tipo_evento,
    resultado,
    origem,
    id_utilizador,
    email_referencia,
    identificador_sessao,
    data_evento,
    detalhes
) VALUES
    (56001, 'login', 'sucesso', 'autenticacao', 56001, 'gestor.operacional@levis.com', 'sess-op-001', CURRENT_TIMESTAMP - INTERVAL '5 hours', 'Login do gestor operacional para validar dashboard e painel do gerente.'),
    (56002, 'login', 'sucesso', 'autenticacao', 56003, 'sofia.almeida.operacional@levis.com', 'sess-op-002', CURRENT_TIMESTAMP - INTERVAL '4 hours', 'Login da colaboradora full-time com preferencia permanente.'),
    (56003, 'pedido_folga', 'sucesso', 'folgas', 56007, 'joana.silva.operacional@levis.com', 'sess-op-003', CURRENT_TIMESTAMP - INTERVAL '2 hours', 'Pedido pendente criado para teste do painel do gerente.'),
    (56004, 'permuta', 'sucesso', 'permutas', 56003, 'sofia.almeida.operacional@levis.com', 'sess-op-004', CURRENT_TIMESTAMP - INTERVAL '90 minutes', 'Permuta pendente criada com contexto operacional no mesmo dia.');

-- Historico minimo para cenarios de validacao visual.
INSERT INTO public.historico_horario_estados (id_registo, id_horario, estado_novo, data_registo, observacoes) VALUES
    (56001, 56022, 'aprovado', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Horario confirmado no seed operacional.'),
    (56002, 56023, 'aprovado', CURRENT_TIMESTAMP - INTERVAL '1 day', 'Horario confirmado no seed operacional.');

DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('public.utilizadores', 'id_utilizador'), COALESCE((SELECT MAX(id_utilizador) FROM public.utilizadores), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.lojautilizador', 'id_lojautilizador'), COALESCE((SELECT MAX(id_lojautilizador) FROM public.lojautilizador), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios', 'id_horario'), COALESCE((SELECT MAX(id_horario) FROM public.horarios), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.day_offs', 'id_dayoff'), COALESCE((SELECT MAX(id_dayoff) FROM public.day_offs), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.preferencias', 'id_preferencia'), COALESCE((SELECT MAX(id_preferencia) FROM public.preferencias), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.permutas', 'id_permuta'), COALESCE((SELECT MAX(id_permuta) FROM public.permutas), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.horarios_especiais_loja', 'id_horario_especial'), COALESCE((SELECT MAX(id_horario_especial) FROM public.horarios_especiais_loja), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.eventos_auditoria', 'id_evento'), COALESCE((SELECT MAX(id_evento) FROM public.eventos_auditoria), 1), true);
    PERFORM setval(pg_get_serial_sequence('public.historico_horario_estados', 'id_registo'), COALESCE((SELECT MAX(id_registo) FROM public.historico_horario_estados), 1), true);
END $$;

COMMIT;
