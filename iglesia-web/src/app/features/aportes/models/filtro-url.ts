import { convertToParamMap, ParamMap, Params } from '@angular/router';
import { esMedioAporte, FiltroAportes } from './aporte';
import { rangoDe } from './periodo';

const FECHA_ISO = /^\d{4}-\d{2}-\d{2}$/;

/** Parámetros de la URL de `/aportes`; los mismos nombres que la API para que los enlaces se lean igual. */
export const PARAMETROS_FILTRO = { desde: 'desde', hasta: 'hasta', categoriaId: 'categoriaId', medio: 'medio', miembroId: 'miembroId', incluirAnulados: 'incluirAnulados' } as const;

/**
 * Lee el filtro de la URL. Lo que falta o no se entiende toma su valor por defecto: el mes en curso, sin más
 * filtros. Así un enlace incompleto (p. ej. desde la ficha, solo con `miembroId`) sigue funcionando.
 */
export function filtroDesdeUrl(parametros: ParamMap, hoy: Date = new Date()): FiltroAportes {
    const mes = rangoDe('MES', hoy);
    const desde = fecha(parametros.get(PARAMETROS_FILTRO.desde));
    const hasta = fecha(parametros.get(PARAMETROS_FILTRO.hasta));
    const medio = parametros.get(PARAMETROS_FILTRO.medio);
    const rangoValido = desde !== null && hasta !== null && desde <= hasta;
    return {
        desde: rangoValido ? desde : mes.desde,
        hasta: rangoValido ? hasta : mes.hasta,
        categoriaId: id(parametros.get(PARAMETROS_FILTRO.categoriaId)),
        medio: esMedioAporte(medio) ? medio : null,
        miembroId: id(parametros.get(PARAMETROS_FILTRO.miembroId)),
        incluirAnulados: parametros.get(PARAMETROS_FILTRO.incluirAnulados) === 'true'
    };
}

/** Parámetros de URL del filtro; los vacíos no se escriben para que la URL quede corta. */
export function filtroAUrl(filtro: FiltroAportes): Params {
    return {
        [PARAMETROS_FILTRO.desde]: filtro.desde,
        [PARAMETROS_FILTRO.hasta]: filtro.hasta,
        [PARAMETROS_FILTRO.categoriaId]: filtro.categoriaId ?? undefined,
        [PARAMETROS_FILTRO.medio]: filtro.medio ?? undefined,
        [PARAMETROS_FILTRO.miembroId]: filtro.miembroId ?? undefined,
        [PARAMETROS_FILTRO.incluirAnulados]: filtro.incluirAnulados ? 'true' : undefined
    };
}

/** Atajo para pruebas y enlaces: el filtro por defecto (mes en curso). */
export const filtroInicial = (hoy: Date = new Date()): FiltroAportes => filtroDesdeUrl(convertToParamMap({}), hoy);

function fecha(valor: string | null): string | null {
    return valor && FECHA_ISO.test(valor) ? valor : null;
}

function id(valor: string | null): number | null {
    const numero = Number(valor);
    return valor && Number.isInteger(numero) && numero > 0 ? numero : null;
}
