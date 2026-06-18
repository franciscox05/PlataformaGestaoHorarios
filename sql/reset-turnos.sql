DELETE FROM historico_horario_estados;
DELETE FROM permutas_folga;
DELETE FROM permutas;
DELETE FROM horarios;
DELETE FROM turnos;
UPDATE lojas SET hora_abertura = '10:00', hora_fecho = '22:00';
INSERT INTO turnos (tipo, nome, hora_inicio, hora_fim) VALUES
    ('manha', 'Manha', '10:00', '14:00'),
    ('tarde', 'Tarde', '14:00', '18:00'),
    ('noite', 'Noite', '18:00', '22:00');
