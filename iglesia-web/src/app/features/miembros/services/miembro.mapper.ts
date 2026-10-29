import { Miembro, MiembroResponse } from '../models/miembro';

export function aMiembro(respuesta: MiembroResponse): Miembro {
    return { ...respuesta, creadoEn: new Date(respuesta.creadoEn), actualizadoEn: new Date(respuesta.actualizadoEn) };
}
