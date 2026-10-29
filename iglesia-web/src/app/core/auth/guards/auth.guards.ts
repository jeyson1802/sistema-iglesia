import { inject } from '@angular/core';
import { CanActivateChildFn, CanActivateFn, RedirectFunction, Router } from '@angular/router';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
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

/**
 * Las páginas de datos de una iglesia (usuarios, roles, miembros) exigen operar en una. El administrador del sistema en
 * su vista global va al catálogo de iglesias para elegir una; cualquier otro usuario sin iglesia, a acceso denegado.
 */
export const iglesiaFijadaGuard: CanActivateFn = () => {
    const sesion = inject(SesionService);
    if (sesion.operaEnIglesia()) {
        return true;
    }
    return inject(Router).parseUrl(sesion.tienePermiso('iglesias.ver') ? RUTA_IGLESIAS : RUTAS_AUTH.accesoDenegado);
};

/**
 * Con una contraseña temporal, cualquier página del layout distinta del cambio de contraseña redirige a él. El
 * backend ya rechaza el resto de operaciones; el guard evita mostrar pantallas que solo darían errores 403.
 */
export const contrasenaVigenteGuard: CanActivateChildFn = (_ruta, estado) =>
    !inject(SesionService).debeCambiarContrasena() || sinConsultaNiFragmento(estado.url) === RUTAS_AUTH.cambiarContrasena || inject(Router).parseUrl(RUTAS_AUTH.cambiarContrasena);

/** Redirige la ruta raíz a la página de inicio del perfil activo. */
export const inicioRedirect: RedirectFunction = () => inject(SesionService).rutaInicio();

function sinConsultaNiFragmento(url: string): string {
    return url.split(/[?#]/)[0];
}
