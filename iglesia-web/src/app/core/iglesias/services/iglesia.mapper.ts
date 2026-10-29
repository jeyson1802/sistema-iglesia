import { Iglesia, IglesiaResponse } from '../models/iglesia';

/** Adapta una iglesia de la API: las fechas pasan a `Date`. */
export function aIglesia(respuesta: IglesiaResponse): Iglesia {
    return { ...respuesta, logoActualizadoEn: respuesta.logoActualizadoEn ? new Date(respuesta.logoActualizadoEn) : null, creadaEn: new Date(respuesta.creadaEn) };
}
