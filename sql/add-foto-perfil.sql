-- Adiciona coluna de foto de perfil (base64 text) aos utilizadores
ALTER TABLE utilizadores ADD COLUMN IF NOT EXISTS foto_perfil TEXT;
