-- =============================================================================
-- reset-para-testes.sql
-- Apaga TUDO excepto Francisco (Tu) e Tiago Costa.
-- Usar ANTES de executar o guiao-testes-completo.md para partir de estado limpo.
-- =============================================================================
BEGIN;

-- 1. Apagar toda a transacionalidade primeiro (ordem FK-safe)
TRUNCATE TABLE
    public.eventos_auditoria,
    public.notificacao,
    public.historico_horario_estados,
    public.permutas,
    public.permutas_folga,
    public.horarios_especiais_loja,
    public.horarios,
    public.propostas_horario_mensal,
    public.day_offs,
    public.preferencias,
    public.lojautilizador,
    public.regras_loja,
    public.regras,
    public.turnos,
    public.cargos,
    public.lojas
RESTART IDENTITY CASCADE;

-- 2. Apagar todos os utilizadores EXCEPTO Francisco (Tu) e Tiago Costa
-- Francisco (Tu) = francisco@levis.com  (este és TU — o utilizador admin)
-- Tiago Costa    = tiago.costa@levis.com (colega que fez o módulo web)
DELETE FROM public.utilizadores
WHERE email NOT IN ('francisco@levis.com', 'tiago.costa@levis.com');

-- 3. Garantir que os dois utilizadores restantes têm estado ativo e password 123456
UPDATE public.utilizadores
SET estado = 'ativo', password_hash = '123456'
WHERE email IN ('francisco@levis.com', 'tiago.costa@levis.com');

-- 4. Reset das sequências dos utilizadores (os IDs mantêm-se como estavam)
-- Não resetar utilizadores para não partir os IDs dos 2 que ficaram.

COMMIT;

-- Verificação: deve mostrar exatamente 2 utilizadores
-- SELECT id_utilizador, nome, email, estado FROM public.utilizadores;
