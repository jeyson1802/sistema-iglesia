import { ParamMap, Params } from '@angular/router';
import { FiltroIglesias } from '@/app/core/iglesias/models/iglesia';

/** Un código INEI de departamento, provincia o distrito. */
const UBIGEO = /^(\d{2}|\d{4}|\d{6})$/;

/** Parámetros de la URL de `/iglesias`; los mismos nombres que la API. */
export const PARAMETROS_FILTRO_IGLESIAS = { q: 'q', ubigeo: 'ubigeo', activa: 'activa' } as const;

/** Lee el filtro de la URL; lo que falta o no se entiende queda sin filtrar. */
export function filtroIglesiasDesdeUrl(parametros: ParamMap): FiltroIglesias {
    const q = parametros.get(PARAMETROS_FILTRO_IGLESIAS.q)?.trim() || null;
    const ubigeo = parametros.get(PARAMETROS_FILTRO_IGLESIAS.ubigeo);
    const activa = parametros.get(PARAMETROS_FILTRO_IGLESIAS.activa);
    return { q, ubigeo: ubigeo && UBIGEO.test(ubigeo) ? ubigeo : null, activa: activa === 'true' ? true : activa === 'false' ? false : null };
}

/** Parámetros de URL del filtro; los vacíos no se escriben para que la URL quede corta. */
export function filtroIglesiasAUrl(filtro: FiltroIglesias): Params {
    return {
        [PARAMETROS_FILTRO_IGLESIAS.q]: filtro.q ?? undefined,
        [PARAMETROS_FILTRO_IGLESIAS.ubigeo]: filtro.ubigeo ?? undefined,
        [PARAMETROS_FILTRO_IGLESIAS.activa]: filtro.activa === null ? undefined : String(filtro.activa)
    };
}

export const mismoFiltroIglesias = (a: FiltroIglesias, b: FiltroIglesias): boolean => a.q === b.q && a.ubigeo === b.ubigeo && a.activa === b.activa;
