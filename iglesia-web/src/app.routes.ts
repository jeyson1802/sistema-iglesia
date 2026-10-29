import { Routes } from '@angular/router';
import { environment } from './environments/environment';
import { autenticadoGuard, contrasenaVigenteGuard, iglesiaFijadaGuard, inicioRedirect, perfilSeleccionadoGuard, permisoGuard } from './app/core/auth/guards/auth.guards';
import { AppLayout } from './app/layout/component/app.layout';
import { Notfound } from './app/pages/notfound/notfound';

/** Demos de Sakai dentro del layout: referencia de UI solo en desarrollo, siempre detrás del login. */
const RUTAS_DEMO_EN_LAYOUT: Routes = [
    { path: 'dashboard', loadComponent: () => import('./app/pages/dashboard/dashboard').then((m) => m.Dashboard) },
    { path: 'uikit', loadChildren: () => import('./app/pages/uikit/uikit.routes') },
    { path: 'documentation', loadComponent: () => import('./app/pages/documentation/documentation').then((m) => m.Documentation) },
    { path: 'pages', loadChildren: () => import('./app/pages/pages.routes') }
];

const RUTAS_DEMO_FUERA_DEL_LAYOUT: Routes = [{ path: 'landing', canActivate: [autenticadoGuard], loadComponent: () => import('./app/pages/landing/landing').then((m) => m.Landing) }];

/** Arma las rutas de la aplicación; en producción excluye las demos de Sakai. */
export function crearRutasApp(produccion: boolean): Routes {
    const soloEnDesarrollo = (rutas: Routes): Routes => (produccion ? [] : rutas);
    return [
        {
            path: '',
            component: AppLayout,
            canActivate: [autenticadoGuard, perfilSeleccionadoGuard],
            canActivateChild: [contrasenaVigenteGuard],
            children: [
                { path: '', pathMatch: 'full', redirectTo: inicioRedirect },
                { path: '', loadChildren: () => import('./app/features/inicio/inicio.routes') },
                { path: 'cuenta/contrasena', title: 'Cambiar contraseña', loadComponent: () => import('./app/core/auth/pages/cambiar-contrasena').then((m) => m.CambiarContrasenaComponent) },
                { path: 'iglesias', loadChildren: () => import('./app/features/iglesias/iglesias.routes') },
                // Datos de una iglesia: sin iglesia fijada (vista global del administrador del sistema) no hay qué mostrar.
                { path: 'miembros', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/miembros/miembros.routes') },
                { path: 'usuarios', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/usuarios/usuarios.routes') },
                { path: 'roles', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/roles/roles.routes') },
                { path: 'niveles', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/niveles/niveles.routes') },
                { path: 'grupos', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/grupos/grupos.routes') },
                { path: 'aportes', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/aportes/aportes.routes') },
                { path: 'asistencia', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/asistencia/asistencia.routes') },
                { path: 'mi-iglesia', canActivate: [iglesiaFijadaGuard], loadChildren: () => import('./app/features/mi-iglesia/mi-iglesia.routes') },
                ...soloEnDesarrollo(RUTAS_DEMO_EN_LAYOUT)
            ]
        },
        // Impresión fuera del layout: solo salen las hojas, sin menú ni barra superior.
        {
            path: 'imprimir/fotochecks',
            title: 'Imprimir fotochecks',
            canActivate: [autenticadoGuard, perfilSeleccionadoGuard, iglesiaFijadaGuard, permisoGuard('miembros.ver')],
            loadComponent: () => import('./app/features/miembros/pages/imprimir-fotochecks').then((m) => m.ImprimirFotochecksComponent)
        },
        ...soloEnDesarrollo(RUTAS_DEMO_FUERA_DEL_LAYOUT),
        { path: 'notfound', component: Notfound },
        { path: 'auth', loadChildren: () => import('./app/core/auth/auth.routes') },
        { path: '**', redirectTo: '/notfound' }
    ];
}

export const appRoutes: Routes = crearRutasApp(environment.production);
