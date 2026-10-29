import { Rol } from '@/app/core/auth/models/rol';

/** Contrato de `RolAsignadoResponse` en iglesia-seguridad. */
export interface RolAsignadoResponse {
    readonly id: number;
    readonly nombre: string;
    readonly rolBase: string;
}

/** Contrato de `UsuarioResponse` en iglesia-seguridad. */
export interface UsuarioAdminResponse {
    readonly id: number;
    readonly correo: string;
    readonly iglesiaId: number | null;
    readonly miembroId: number | null;
    readonly activo: boolean;
    readonly debeCambiarContrasena: boolean;
    readonly roles: readonly RolAsignadoResponse[];
    /** Instante ISO 8601 en UTC. */
    readonly creadoEn: string;
}

export interface RolAsignado {
    readonly id: number;
    readonly nombre: string;
    /** `null` si la interfaz no conoce el rol base: el rol se conserva para no quitárselo al usuario al guardar. */
    readonly rolBase: Rol | null;
}

/** Usuario de la iglesia tal como lo administra la pantalla `/usuarios`. */
export interface UsuarioAdmin {
    readonly id: number;
    readonly correo: string;
    readonly miembroId: number | null;
    readonly activo: boolean;
    readonly debeCambiarContrasena: boolean;
    readonly roles: readonly RolAsignado[];
    readonly creadoEn: Date;
}

export interface FiltroUsuarios {
    /** Parte del correo, sin distinguir mayúsculas; vacío = sin filtro. */
    readonly correo: string;
    /** `null` = todos. */
    readonly activo: boolean | null;
    readonly rolId: number | null;
}

export interface CrearUsuario {
    readonly correo: string;
    readonly contrasenaTemporal: string;
    readonly rolIds: readonly number[];
}

export interface EditarUsuario {
    readonly correo: string;
    readonly rolIds: readonly number[];
    readonly activo: boolean;
}

/** Usuarios por página en la tabla. */
export const PAGINA_USUARIOS = 10;
