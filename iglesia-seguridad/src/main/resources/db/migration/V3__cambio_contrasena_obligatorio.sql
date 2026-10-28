-- Contraseña temporal: el usuario debe cambiarla antes de usar el sistema. Se marca al crearlo y al
-- restablecer su contraseña desde la administración. Los usuarios existentes quedan con FALSE.
ALTER TABLE usuario
    ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE;
