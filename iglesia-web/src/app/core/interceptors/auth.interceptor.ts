import { HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, concat, defer, ignoreElements, Observable, switchMap, throwError } from 'rxjs';
import { environment } from '@/environments/environment';
import { MOTIVO_LOGIN, RUTAS_AUTH } from '../auth/models/rutas-auth';
import { SesionService } from '../auth/services/sesion.service';

const URLS_PROPIAS = [environment.apiUrl, environment.seguridadUrl];
const URL_AUTH = `${environment.seguridadUrl}/auth/`;

const empiezaPor = (url: string, base: string): boolean => url === base || url.startsWith(`${base}/`);
const esUrlPropia = (url: string): boolean => URLS_PROPIAS.some((base) => empiezaPor(url, base));

const conToken = (peticion: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> =>
    token ? peticion.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : peticion;

/**
 * Agrega el token de acceso solo a los backends propios. Ante un 401 (salvo de `/auth/*`) refresca una vez,
 * compartiendo el refresco entre peticiones simultáneas, y reintenta. Si el refresco falla, cierra la sesión.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
    if (!esUrlPropia(peticion.url)) {
        return siguiente(peticion);
    }
    const sesion = inject(SesionService);
    const router = inject(Router);

    return siguiente(conToken(peticion, sesion.tokenAcceso())).pipe(
        catchError((error: unknown) => {
            if (!esNoAutorizado(error) || peticion.url.startsWith(URL_AUTH)) {
                return throwError(() => error);
            }
            return refrescarYReintentar(peticion, siguiente, sesion, router);
        })
    );
};

function refrescarYReintentar(peticion: HttpRequest<unknown>, siguiente: HttpHandlerFn, sesion: SesionService, router: Router): Observable<HttpEvent<unknown>> {
    return sesion.refrescar().pipe(
        catchError((errorRefresco: unknown) => expulsar(sesion, router, errorRefresco)),
        switchMap((token) => siguiente(conToken(peticion, token)))
    );
}

/** Cierra la sesión una sola vez (las demás peticiones fallidas ya la encuentran cerrada) y propaga el error. */
function expulsar(sesion: SesionService, router: Router, error: unknown): Observable<never> {
    const fallar = throwError(() => error);
    if (!sesion.autenticado()) {
        return fallar;
    }
    return concat(
        sesion.cerrarSesion().pipe(ignoreElements()),
        defer(() => {
            void router.navigate([RUTAS_AUTH.login], { queryParams: { motivo: MOTIVO_LOGIN.sesionExpirada } });
            return fallar;
        })
    );
}

function esNoAutorizado(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized;
}
