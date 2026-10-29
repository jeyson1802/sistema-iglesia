import { Iglesia, IglesiaResponse } from '../models/iglesia';

/** Adapta una iglesia de la API: la fecha de alta pasa a `Date`. */
export function aIglesia({ id, nombre, creadaEn }: IglesiaResponse): Iglesia {
    return { id, nombre, creadaEn: new Date(creadaEn) };
}
