-- El líder ya no ve aportes por defecto: los aportes solo los ve el administrador de la iglesia o un rol que los
-- tenga explícitamente (basado en ADMIN_IGLESIA). Se quita aportes.ver del rol base LIDER y, como los permisos de un
-- rol personalizado son un subconjunto de los de su rol base, también de los roles personalizados basados en LIDER.
DELETE FROM rol_permiso rp
USING rol r
WHERE rp.rol_id = r.id
  AND rp.permiso_codigo LIKE 'aportes.%'
  AND (r.codigo = 'LIDER' OR r.rol_base_id = (SELECT id FROM rol WHERE codigo = 'LIDER'));
