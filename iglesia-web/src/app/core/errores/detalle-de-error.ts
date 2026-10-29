import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { esErrorGlobal } from './clasificar-error';

/** Errores de negocio cuyo `detail` (ProblemDetail, en español) se muestra tal cual al usuario. */
const ESTADOS_CON_DETALLE: readonly number[] = [HttpStatusCode.BadRequest, HttpStatusCode.NotFound, HttpStatusCode.Conflict];

/**
 * Mensaje para mostrar en la página: el `detail` de un 400, 404 o 409, o el genérico para otros 4xx. Devuelve `null`
 * para los 5xx y la falta de conexión, que ya avisa el toast global.
 */
export function detalleDeError(error: unknown, generico: string): string | null {
    if (esErrorGlobal(error)) {
        return null;
    }
    if (error instanceof HttpErrorResponse && ESTADOS_CON_DETALLE.includes(error.status) && typeof error.error?.detail === 'string') {
        return error.error.detail;
    }
    return generico;
}

/** Mensajes por campo (`errores` del ProblemDetail de iglesia-api), para mostrarlos junto a cada campo. */
export function erroresPorCampo(error: unknown): Readonly<Record<string, string>> {
    const errores = error instanceof HttpErrorResponse ? error.error?.errores : null;
    return errores && typeof errores === 'object' ? (errores as Record<string, string>) : {};
}
