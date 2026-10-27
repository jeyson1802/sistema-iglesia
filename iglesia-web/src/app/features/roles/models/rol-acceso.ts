import { Permiso } from '@/app/core/auth/models/permiso';
import { Rol } from '@/app/core/auth/models/rol';

/** Contrato de `RolResponse` en iglesia-seguridad. */
export interface RolAccesoResponse {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: string;
    readonly base: boolean;
    readonly activo: boolean;
    readonly permisos: readonly string[];
    readonly cantidadUsuarios: number;
}

/** Contrato de `PermisoResponse` en iglesia-seguridad. */
export interface PermisoResponse {
    readonly codigo: string;
    readonly modulo: string;
    readonly descripcion: string;
}

/** Rol de acceso (base o personalizado) adaptado a la UI. */
export interface RolAcceso {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: Rol;
    readonly base: boolean;
    readonly activo: boolean;
    readonly permisos: readonly Permiso[];
    readonly cantidadUsuarios: number;
}

/** Permiso del catálogo fijo, con el módulo («Miembros») por el que se agrupa en el formulario. */
export interface PermisoCatalogo {
    readonly codigo: Permiso;
    readonly modulo: string;
    readonly descripcion: string;
}

export interface GrupoPermisos {
    readonly modulo: string;
    readonly permisos: readonly PermisoCatalogo[];
}

/** Datos del formulario. `rolBaseId` solo se envía al crear: el rol base de un rol existente no cambia. */
export interface GuardarRol {
    readonly nombre: string;
    readonly rolBaseId: number | null;
    readonly permisos: readonly Permiso[];
}

/** Roles base de los que una iglesia puede heredar (el administrador del sistema no es heredable). */
export const ROLES_BASE_HEREDABLES: readonly Rol[] = ['ADMIN_IGLESIA', 'LIDER', 'MIEMBRO'];

/** Límite de iglesia-seguridad (`LimitesRol.NOMBRE_MAX`). */
export const NOMBRE_ROL_MAX = 60;
