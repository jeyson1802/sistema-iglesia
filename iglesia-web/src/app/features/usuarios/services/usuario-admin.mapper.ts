import { esRol } from '@/app/core/auth/models/rol';
import { Page } from '@/app/core/models/page';
import { RolAsignado, RolAsignadoResponse, UsuarioAdmin, UsuarioAdminResponse } from '../models/usuario-admin';

function aRolAsignado({ id, nombre, rolBase }: RolAsignadoResponse): RolAsignado {
    return { id, nombre, rolBase: esRol(rolBase) ? rolBase : null };
}

/** Adapta un usuario de la API: la fecha de creación pasa a `Date` y la iglesia no se usa en la pantalla. */
export function aUsuarioAdmin(respuesta: UsuarioAdminResponse): UsuarioAdmin {
    const { id, correo, miembroId, activo, debeCambiarContrasena, roles, creadoEn } = respuesta;
    return { id, correo, miembroId, activo, debeCambiarContrasena, roles: roles.map(aRolAsignado), creadoEn: new Date(creadoEn) };
}

export function aPaginaUsuarios(respuesta: Page<UsuarioAdminResponse>): Page<UsuarioAdmin> {
    return { content: respuesta.content.map(aUsuarioAdmin), page: respuesta.page };
}
