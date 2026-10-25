import { Rol, ROLES } from '../models/rol';
import { UsuarioActual } from '../models/usuario-actual';
import { UsuarioResponse } from '../models/usuario-response';

const esRol = (valor: string): valor is Rol => (ROLES as readonly string[]).includes(valor);

const porJerarquia = (a: Rol, b: Rol): number => ROLES.indexOf(a) - ROLES.indexOf(b);

/** Adapta la respuesta de la API: descarta roles desconocidos y los ordena por jerarquía (el mayor primero). */
export function aUsuarioActual(respuesta: UsuarioResponse): UsuarioActual {
    return {
        id: respuesta.id,
        correo: respuesta.correo,
        iglesiaId: respuesta.iglesiaId,
        miembroId: respuesta.miembroId,
        roles: respuesta.roles.filter(esRol).sort(porJerarquia)
    };
}
