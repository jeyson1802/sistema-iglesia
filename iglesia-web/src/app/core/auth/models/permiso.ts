/**
 * Catálogo fijo de permisos de iglesia-seguridad (tabla `permiso`, sembrada por Flyway). Un permiso nuevo se
 * agrega en la misma spec en Flyway y aquí; los códigos que la interfaz no conoce se descartan.
 */
export const PERMISOS = [
    'miembros.ver',
    'miembros.crear',
    'miembros.editar',
    'miembros.desactivar',
    'niveles.administrar',
    'grupos.ver',
    'grupos.administrar',
    'aportes.ver',
    'aportes.registrar',
    'aportes.editar',
    'aportes.anular',
    'aportes.configurar',
    'asistencia.registrar',
    'asistencia.grupo',
    'asistencia.ver',
    'asistencia.configurar',
    'consolidacion.administrar',
    'consolidacion.ver',
    'consolidacion.contactar',
    'usuarios.ver',
    'usuarios.administrar',
    'roles.ver',
    'roles.administrar',
    'iglesias.ver',
    'iglesias.administrar',
    'estadisticas.sistema',
    'iglesia.personalizar'
] as const;
export type Permiso = (typeof PERMISOS)[number];

export const esPermiso = (valor: string): valor is Permiso => (PERMISOS as readonly string[]).includes(valor);
