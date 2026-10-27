import { inject } from '@angular/core';
import { CanActivateFn, RedirectFunction, Router } from '@angular/router';
import { Permiso } from '../models/permiso';
import { Rol } from '../models/rol';
import { RUTAS_AUTH } from '../models/rutas-auth';
import { SesionService } from '../services/sesion.service';

/** Sin sesión, redirige al login. */
export const autenticadoGuard: CanActivateFn = () => inject(SesionService).autenticado() || inject(Router).parseUrl(RUTAS_AUTH.login);

/** Para las páginas de invitado (login): con sesión, redirige a la página de inicio. */
export const invitadoGuard: CanActivateFn = () => {
    const sesion = inject(SesionService);
    return !sesion.autenticado() || inject(Router).parseUrl(sesion.rutaInicio());
};

/** Sin perfil activo, redirige al selector de perfil. */
export const perfilSeleccionadoGuard: CanActivateFn = () => inject(SesionService).perfilActivo() !== null || inject(Router).parseUrl(RUTAS_AUTH.seleccionarPerfil);

/**
 * Solo deja pasar si el rol base del perfil activo es uno de los indicados; si no, redirige a acceso denegado.
 * Es para las páginas de inicio, que dependen del ámbito; el resto de pantallas se protege con `permisoGuard`.
 */
export const rolBaseGuard =
    (...roles: Rol[]): CanActivateFn =>
    () => {
        const perfil = inject(SesionService).perfilActivo();
        return (perfil !== null && roles.includes(perfil.rolBase)) || inject(Router).parseUrl(RUTAS_AUTH.accesoDenegado);
    };

/** Solo deja pasar si el perfil activo tiene el permiso; si no (o sin perfil), redirige a acceso denegado. */
export const permisoGuard =
    (permiso: Permiso): CanActivateFn =>
    () =>
        inject(SesionService).tienePermiso(permiso) || inject(Router).parseUrl(RUTAS_AUTH.accesoDenegado);

/** Redirige la ruta raíz a la página de inicio del perfil activo. */
export const inicioRedirect: RedirectFunction = () => inject(SesionService).rutaInicio();
