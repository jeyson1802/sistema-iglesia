-- SPEC 11: actividad diaria de los usuarios (base de «uso del sistema») y permiso de las estadísticas entre iglesias.

-- Una fila por usuario y día con actividad: inicio de sesión o renovación del token. No guarda IP, navegador ni
-- acciones; solo que el usuario usó el sistema ese día.
CREATE TABLE actividad_usuario (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    fecha      DATE        NOT NULL,
    -- Iglesia del usuario; NULL para el administrador del sistema (no pertenece a ninguna).
    iglesia_id BIGINT,
    -- Rol base principal del usuario ese día (el de mayor alcance).
    rol_base   VARCHAR(20) NOT NULL CHECK (rol_base IN ('ADMIN_SISTEMA', 'ADMIN_IGLESIA', 'LIDER', 'MIEMBRO')),
    PRIMARY KEY (usuario_id, fecha)
);
CREATE INDEX ix_actividad_usuario_fecha_iglesia ON actividad_usuario (fecha, iglesia_id);

-- Solo el administrador del sistema ve las cifras de todas las iglesias.
INSERT INTO permiso (codigo, modulo, descripcion, orden) VALUES
    ('estadisticas.sistema', 'Iglesias', 'Ver las estadísticas de todas las iglesias y el uso del sistema', 3);

INSERT INTO rol_permiso (rol_id, permiso_codigo)
SELECT r.id, 'estadisticas.sistema' FROM rol r WHERE r.codigo = 'ADMIN_SISTEMA';
