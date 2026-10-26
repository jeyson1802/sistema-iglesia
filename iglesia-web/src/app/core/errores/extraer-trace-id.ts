import { HttpErrorResponse } from '@angular/common/http';

/** Cabecera con la que los backends devuelven el identificador de traza en toda respuesta. */
export const CABECERA_TRACE_ID = 'X-Trace-Id';

/**
 * Código de seguimiento de un error HTTP: el `traceId` del `ProblemDetail` y, si el cuerpo no lo trae
 * (p. ej. un 503 sin cuerpo), la cabecera `X-Trace-Id`. `null` si no hay ninguno (error de red o proxy).
 */
export function extraerTraceId(error: HttpErrorResponse): string | null {
    const cuerpo: unknown = error.error;
    if (typeof cuerpo === 'object' && cuerpo !== null && 'traceId' in cuerpo && typeof cuerpo.traceId === 'string' && cuerpo.traceId) {
        return cuerpo.traceId;
    }
    return error.headers?.get(CABECERA_TRACE_ID) || null;
}
