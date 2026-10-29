import { Apariencia } from '@/app/core/apariencia/models/apariencia';
import { Ubicacion } from '@/app/core/models/ubicacion';

/** Límites de `LimitesIglesia` en iglesia-api. */
export const NOMBRE_COMERCIAL_MIN = 2;
export const NOMBRE_COMERCIAL_MAX = 60;
export const DENOMINACION_MAX = 200;
export const DIRECCION_MAX = 200;
export const TELEFONO_MAX = 20;
export const CORREO_IGLESIA_MAX = 120;
export const SITIO_WEB_MAX = 200;
/** Mismo patrón que el backend: dígitos, espacios, paréntesis, `+` y `-`. */
export const TELEFONO_PATRON = /^[+(0-9][0-9 ()+-]*$/;
export const SITIO_WEB_PATRON = /^https?:\/\/\S+$/;

/** Contrato de `IglesiaResponse` en iglesia-api. */
export interface IglesiaResponse {
    readonly id: number;
    readonly nombreComercial: string;
    readonly denominacion: string;
    readonly direccion: string | null;
    readonly distritoCodigo: string | null;
    readonly ubicacion: Ubicacion | null;
    readonly telefono: string | null;
    readonly correo: string | null;
    readonly sitioWeb: string | null;
    readonly activa: boolean;
    readonly tieneLogo: boolean;
    /** Instante ISO 8601 en UTC de la última subida del logo; `null` sin logo. */
    readonly logoActualizadoEn: string | null;
    readonly apariencia: Apariencia;
    /** Instante ISO 8601 en UTC. */
    readonly creadaEn: string;
}

/** Iglesia con su identidad, tal como la usa la interfaz. */
export interface Iglesia extends Omit<IglesiaResponse, 'creadaEn' | 'logoActualizadoEn'> {
    readonly logoActualizadoEn: Date | null;
    readonly creadaEn: Date;
}

/** Contrato de `GuardarIglesiaRequest`: lo vacío viaja como `null`. */
export interface DatosIglesia {
    readonly nombreComercial: string;
    readonly denominacion: string;
    readonly direccion: string;
    readonly distritoCodigo: string;
    readonly telefono: string | null;
    readonly correo: string | null;
    readonly sitioWeb: string | null;
}

/** Filtros del catálogo (`FiltroIglesias` en iglesia-api). */
export interface FiltroIglesias {
    readonly q: string | null;
    /** Código INEI de 2, 4 o 6 dígitos. */
    readonly ubigeo: string | null;
    readonly activa: boolean | null;
}

export const FILTRO_IGLESIAS_VACIO: FiltroIglesias = { q: null, ubigeo: null, activa: null };

/** Iglesias existentes antes de la SPEC 08 pueden no tener dirección: se avisa sin bloquear. */
export const faltaDireccion = (iglesia: Pick<Iglesia, 'direccion' | 'distritoCodigo'>): boolean => !iglesia.direccion || !iglesia.distritoCodigo;

/** Iniciales del nombre comercial para el monograma cuando no hay logo (máx. 2). */
export function iniciales(nombre: string): string {
    const palabras = nombre
        .trim()
        .split(/\s+/)
        .filter((palabra) => /[\p{L}\p{N}]/u.test(palabra));
    return palabras
        .slice(0, 2)
        .map((palabra) => palabra.match(/[\p{L}\p{N}]/u)?.[0] ?? '')
        .join('')
        .toUpperCase();
}
