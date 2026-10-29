import { Params } from '@angular/router';
import { CONDICIONES, Condicion, FILTRO_MIEMBROS_INICIAL, FiltroMiembros } from './miembro';

/** Página de impresión de fotochecks: fuera del layout, para que solo se impriman las hojas. */
export const RUTA_IMPRIMIR_FOTOCHECKS = '/imprimir/fotochecks';

/** Qué fotochecks imprimir: los de un miembro (desde su ficha) o los del listado con sus filtros. */
export type SeleccionFotochecks = { readonly miembroId: number } | { readonly filtro: FiltroMiembros };

/** Filtro del listado como parámetros de la URL de impresión (solo los que tienen valor). */
export function filtroFotochecksAUrl({ q, condicion, nivelId, liderId, ubigeo, activo }: FiltroMiembros): Params {
    const parametros: Record<string, string | number | boolean | null> = { q: q.trim() || null, condicion, nivelId, liderId, ubigeo, activo };
    return Object.fromEntries(Object.entries(parametros).filter(([, valor]) => valor !== null));
}

/** Lee la URL de impresión; un valor que no se entiende se ignora (queda sin ese filtro). */
export function seleccionDesdeUrl(parametros: Params): SeleccionFotochecks {
    const miembroId = entero(parametros['miembroId']);
    if (miembroId !== null) {
        return { miembroId };
    }
    const condicion = parametros['condicion'];
    const activo = parametros['activo'];
    return {
        filtro: {
            ...FILTRO_MIEMBROS_INICIAL,
            q: typeof parametros['q'] === 'string' ? parametros['q'] : '',
            condicion: CONDICIONES.includes(condicion) ? (condicion as Condicion) : null,
            nivelId: entero(parametros['nivelId']),
            liderId: entero(parametros['liderId']),
            ubigeo: /^\d{2}(\d{2}(\d{2})?)?$/.test(parametros['ubigeo'] ?? '') ? parametros['ubigeo'] : null,
            activo: activo === 'true' ? true : activo === 'false' ? false : null
        }
    };
}

function entero(valor: unknown): number | null {
    const numero = Number(valor);
    return typeof valor === 'string' && Number.isInteger(numero) && numero > 0 ? numero : null;
}
