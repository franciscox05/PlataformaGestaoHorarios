-- ──────────────────────────────────────────────────────────────────────────────
-- Permuta de Folga — troca coordenada de um turno por uma folga entre dois
-- colaboradores do mesmo mês.
--
-- Lógica:
--   Func1 tem turno no dia D e quer ficar de folga.
--   Func2 tem folga no dia D mas tem turno num dia Y em que Func1 tem folga.
--   Ao aprovar:
--     • id_horario_d.idLojautilizador  → Func2  (Func2 fica com o turno do dia D)
--     • id_horario_y.idLojautilizador  → Func1  (Func1 fica com o turno do dia Y)
--   Resultado: ambos mantêm o mesmo nº de dias de trabalho e de folga.
-- ──────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS permutas_folga (
    id_permuta_folga  SERIAL PRIMARY KEY,
    id_horario_d      INTEGER NOT NULL REFERENCES horarios(id_horario),
    id_horario_y      INTEGER NOT NULL REFERENCES horarios(id_horario),
    estado            VARCHAR(20) NOT NULL DEFAULT 'pendente',
    data_pedido       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pf_estado CHECK (estado IN ('pendente','aprovado','rejeitado','cancelado')),
    CONSTRAINT chk_pf_diferente CHECK (id_horario_d <> id_horario_y)
);

CREATE INDEX IF NOT EXISTS idx_pf_horario_d ON permutas_folga(id_horario_d);
CREATE INDEX IF NOT EXISTS idx_pf_horario_y ON permutas_folga(id_horario_y);
CREATE INDEX IF NOT EXISTS idx_pf_estado    ON permutas_folga(estado);
