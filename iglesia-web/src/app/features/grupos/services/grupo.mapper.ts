import { Grupo, GrupoResponse } from '../models/grupo';

/** Largo de `HH:mm`: la interfaz no usa los segundos que trae el `LocalTime` del backend. */
const LARGO_HORA = 5;

export function aGrupo(respuesta: GrupoResponse): Grupo {
    return { ...respuesta, horaReunion: respuesta.horaReunion?.slice(0, LARGO_HORA) ?? null };
}
