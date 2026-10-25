-- Crea o actualiza el rol con el que se conecta iglesia-api. Se ejecuta una vez por entorno,
-- con un superusuario, ANTES del primer arranque (la contraseña no se guarda en el repositorio):
--
--   psql -h localhost -U postgres -d iglesia_negocio -v contrasena='...' -f scripts/configurar-rol-aplicacion.sql
--
-- El rol no es superusuario, ni dueño de las tablas, ni tiene BYPASSRLS: la RLS siempre le aplica.
-- Los permisos sobre las tablas los otorgan las migraciones de Flyway.
\set ON_ERROR_STOP on

SELECT 'CREATE ROLE iglesia_app NOLOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'iglesia_app') \gexec

SELECT format('ALTER ROLE iglesia_app LOGIN NOSUPERUSER NOBYPASSRLS PASSWORD %L', :'contrasena') \gexec
