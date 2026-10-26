import { Routes } from '@angular/router';
import { perfilGuard } from '@/app/core/auth/guards/auth.guards';
import { Rol, RUTA_INICIO_POR_ROL } from '@/app/core/auth/models/rol';
import { InicioAdministracionComponent } from './pages/inicio-administracion';
import { InicioLiderComponent } from './pages/inicio-lider';
import { InicioMiembroComponent } from './pages/inicio-miembro';
import { InicioSistemaComponent } from './pages/inicio-sistema';

/** Las rutas salen de `RUTA_INICIO_POR_ROL` (sin la barra inicial) para no duplicarlas. */
const rutaDe = (rol: Rol): string => RUTA_INICIO_POR_ROL[rol].slice(1);

export default [
    { path: rutaDe('ADMIN_SISTEMA'), component: InicioSistemaComponent, title: 'Inicio', canActivate: [perfilGuard('ADMIN_SISTEMA')] },
    { path: rutaDe('ADMIN_IGLESIA'), component: InicioAdministracionComponent, title: 'Inicio', canActivate: [perfilGuard('ADMIN_IGLESIA')] },
    { path: rutaDe('LIDER'), component: InicioLiderComponent, title: 'Inicio', canActivate: [perfilGuard('LIDER')] },
    { path: rutaDe('MIEMBRO'), component: InicioMiembroComponent, title: 'Inicio', canActivate: [perfilGuard('MIEMBRO')] }
] satisfies Routes;
