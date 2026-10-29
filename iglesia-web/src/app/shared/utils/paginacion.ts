/** Configuración común del paginador: todas las grillas lo muestran siempre, con el mismo formato y opciones. */
export const FILAS_POR_PAGINA = 10;
export const OPCIONES_FILAS_POR_PAGINA = [10, 20, 50];
export const PLANTILLA_REPORTE_PAGINA = '{first}–{last} de {totalRecords}';

/** Página pedida por un paginador en el servidor (índice desde 0) con su tamaño. */
export interface PaginaPedida {
    readonly pagina: number;
    readonly tamano: number;
}

/** Convierte el evento del paginador o de la tabla perezosa (primer registro y filas) en la página pedida. */
export function paginaPedida({ first, rows }: { first?: number | null; rows?: number | null }, tamanoActual: number): PaginaPedida {
    const tamano = rows ?? tamanoActual;
    return { pagina: Math.floor((first ?? 0) / tamano), tamano };
}

/** Al cambiar el tamaño se vuelve a la primera página, para no quedar en una que ya no existe. */
export function paginaACargar({ pagina, tamano }: PaginaPedida, tamanoActual: number): number {
    return tamano === tamanoActual ? pagina : 0;
}
