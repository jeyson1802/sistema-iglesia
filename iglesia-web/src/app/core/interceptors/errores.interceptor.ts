import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs';
import { esErrorDeConexion, esErrorInesperado } from '../errores/clasificar-error';
import { extraerTraceId } from '../errores/extraer-trace-id';
import { NotificadorErroresService } from '../errores/notificador-errores.service';

/**
 * Avisa en el toast global de los errores que ninguna página maneja: 5xx (con el código de seguimiento) y
 * falta de conexión. Los 4xx pasan sin aviso. El error siempre se relanza para que la página siga su flujo.
 */
export const erroresInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
    const notificador = inject(NotificadorErroresService);
    return siguiente(peticion).pipe(
        tap({
            error: (error: unknown) => {
                if (esErrorInesperado(error)) {
                    notificador.errorInesperado(extraerTraceId(error));
                } else if (esErrorDeConexion(error)) {
                    notificador.sinConexion();
                }
            }
        })
    );
};
