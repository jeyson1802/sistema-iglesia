import { esPermiso } from '@/app/core/auth/models/permiso';
import { esRol } from '@/app/core/auth/models/rol';
import { GrupoPermisos, PermisoCatalogo, PermisoResponse, RolAcceso, RolAccesoResponse } from '../models/rol-acceso';

/** Un rol cuyo rol base la interfaz no conoce no se puede mostrar ni editar: se descarta. */
export function aRolAcceso(respuesta: RolAccesoResponse): RolAcceso | null {
    const { id, nombre, rolBase, base, activo, permisos, cantidadUsuarios, requiereMiembro } = respuesta;
    return esRol(rolBase) ? { id, nombre, rolBase, base, activo, permisos: permisos.filter(esPermiso), cantidadUsuarios, requiereMiembro } : null;
}

export function aRolesAcceso(respuestas: readonly RolAccesoResponse[]): RolAcceso[] {
    return respuestas.map(aRolAcceso).filter((rol): rol is RolAcceso => rol !== null);
}

/** Descarta los permisos que la interfaz no conoce y conserva el orden del backend (módulo y orden). */
export function aPermisosCatalogo(respuestas: readonly PermisoResponse[]): PermisoCatalogo[] {
    return respuestas.filter((permiso): permiso is PermisoCatalogo => esPermiso(permiso.codigo));
}

/** Agrupa por módulo respetando el orden en que llegan los permisos. */
export function agruparPorModulo(permisos: readonly PermisoCatalogo[]): GrupoPermisos[] {
    const grupos = new Map<string, PermisoCatalogo[]>();
    for (const permiso of permisos) {
        grupos.set(permiso.modulo, [...(grupos.get(permiso.modulo) ?? []), permiso]);
    }
    return [...grupos].map(([modulo, delModulo]) => ({ modulo, permisos: delModulo }));
}
