-- El administrador del sistema distingue las iglesias por su nombre: no puede haber dos iguales, sin distinguir
-- mayúsculas. La tabla iglesia es el catálogo de tenants: no tiene iglesia_id ni RLS.
CREATE UNIQUE INDEX ux_iglesia_nombre ON iglesia (lower(nombre));
