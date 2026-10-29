import { Perfil } from '../models/perfil';
import { esPermiso } from '../models/permiso';
import { esRol, ROLES } from '../models/rol';
import { PerfilResponse, UsuarioResponse } from '../models/usuario-response';
import { UsuarioActual } from '../models/usuario-actual';

const porJerarquiaYNombre = (a: Perfil, b: Perfil): number =>
    ROLES.indexOf(a.rolBase) - ROLES.indexOf(b.rolBase) || a.nombre.localeCompare(b.nombre, 'es');

/** Un perfil cuyo rol base la interfaz no conoce no tiene página de inicio: se descarta. */
function aPerfil({ id, nombre, rolBase, permisos }: PerfilResponse): Perfil | null {
    return esRol(rolBase) ? { id, nombre, rolBase, permisos: permisos.filter(esPermiso) } : null;
}

/**
 * Adapta la respuesta de la API: descarta perfiles con rol base desconocido y permisos que la interfaz no
 * conoce, y ordena los perfiles por la jerarquía de su rol base (el mayor primero) y luego por nombre.
 */
export function aUsuarioActual(respuesta: UsuarioResponse): UsuarioActual {
    return {
        id: respuesta.id,
        correo: respuesta.correo,
        iglesiaId: respuesta.iglesiaId,
        miembroId: respuesta.miembroId,
        debeCambiarContrasena: respuesta.debeCambiarContrasena,
        actuandoEnIglesia: respuesta.actuandoEnIglesia,
        perfiles: respuesta.perfiles
            .map(aPerfil)
            .filter((perfil): perfil is Perfil => perfil !== null)
            .sort(porJerarquiaYNombre)
    };
}
