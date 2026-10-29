import { Marca } from './asistencia';

export type TipoResultado = 'marcado' | 'repetido' | 'error';

/** Franja que se muestra tras cada lectura. */
export interface ResultadoMarca {
    readonly tipo: TipoResultado;
    readonly titulo: string;
    readonly detalle: string | null;
}

/** Lo que dura la franja verde o ámbar; la roja se queda hasta la siguiente lectura. */
export const DURACION_RESULTADO_MS = 1500;
export const NO_ES_CODIGO = 'Este QR no es un código de asistencia';

/** Hora local `HH:mm` de un instante ISO. */
export function horaDe(instante: string): string {
    return new Date(instante).toLocaleTimeString('es', { hour: '2-digit', minute: '2-digit', hour12: false });
}

export function resultadoDeMarca(marca: Marca): ResultadoMarca {
    return marca.yaRegistrado
        ? { tipo: 'repetido', titulo: marca.nombreCompleto, detalle: `Ya estaba registrado a las ${horaDe(marca.registradaEn)}` }
        : { tipo: 'marcado', titulo: marca.nombreCompleto, detalle: marca.nivel ? `Marcado · ${marca.nivel}` : 'Marcado' };
}

export function resultadoDeError(motivo: string): ResultadoMarca {
    return { tipo: 'error', titulo: motivo, detalle: null };
}
