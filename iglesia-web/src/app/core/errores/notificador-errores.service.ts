import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

/** Clave del `<p-toast>` global (en la raíz de la app) que muestra los errores inesperados. */
export const CLAVE_TOAST_ERRORES = 'errores-globales';

/** Datos que el toast global recibe en `message.data` para mostrar el código de seguimiento. */
export interface DatosErrorGlobal {
    readonly traceId: string | null;
}

export const TITULO_ERROR_INESPERADO = 'Error inesperado';
export const DETALLE_ERROR_CON_CODIGO = 'Ocurrió un error inesperado. Si persiste, comunica este código a soporte.';
export const DETALLE_ERROR_SIN_CODIGO = 'Ocurrió un error inesperado. Intenta de nuevo en unos minutos.';
export const TITULO_SIN_CONEXION = 'Sin conexión';
export const DETALLE_SIN_CONEXION = 'No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.';

/** Tiempo visible del aviso de conexión; el de error inesperado queda fijo para poder copiar el código. */
const DURACION_SIN_CONEXION_MS = 6000;

/** Fachada sobre `MessageService` para avisar de los errores que ninguna página maneja (5xx y fallas de red). */
@Injectable({ providedIn: 'root' })
export class NotificadorErroresService {
    private readonly mensajes = inject(MessageService);

    errorInesperado(traceId: string | null): void {
        const datos: DatosErrorGlobal = { traceId };
        this.mensajes.add({
            key: CLAVE_TOAST_ERRORES,
            severity: 'error',
            summary: TITULO_ERROR_INESPERADO,
            detail: traceId ? DETALLE_ERROR_CON_CODIGO : DETALLE_ERROR_SIN_CODIGO,
            data: datos,
            sticky: true
        });
    }

    sinConexion(): void {
        const datos: DatosErrorGlobal = { traceId: null };
        this.mensajes.add({
            key: CLAVE_TOAST_ERRORES,
            severity: 'warn',
            summary: TITULO_SIN_CONEXION,
            detail: DETALLE_SIN_CONEXION,
            data: datos,
            life: DURACION_SIN_CONEXION_MS
        });
    }
}
