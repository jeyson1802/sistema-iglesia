import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { AsistenciaInicioComponent } from './pages/asistencia-inicio';
import { ConfiguracionAsistenciaComponent } from './pages/configuracion-asistencia';
import { DetalleReunionComponent } from './pages/detalle-reunion';
import { SeguimientoAsistenciaComponent } from './pages/seguimiento-asistencia';

export default [
    { path: '', component: AsistenciaInicioComponent, title: 'Asistencia', canActivate: [permisoGuard('asistencia.registrar', 'asistencia.grupo', 'asistencia.ver')] },
    { path: 'seguimiento', component: SeguimientoAsistenciaComponent, title: 'Seguimiento de asistencia', canActivate: [permisoGuard('asistencia.ver', 'asistencia.grupo')] },
    { path: 'configuracion', component: ConfiguracionAsistenciaComponent, title: 'Horarios de culto', canActivate: [permisoGuard('asistencia.configurar')] },
    // La pantalla de escaneo carga la librería de cámara: se descarga solo al entrar en ella.
    {
        path: 'reuniones/:id',
        title: 'Tomar asistencia',
        canActivate: [permisoGuard('asistencia.registrar', 'asistencia.grupo')],
        loadComponent: () => import('./pages/tomar-asistencia').then((m) => m.TomarAsistenciaComponent)
    },
    { path: 'reuniones/:id/detalle', component: DetalleReunionComponent, title: 'Asistentes de la reunión', canActivate: [permisoGuard('asistencia.ver', 'asistencia.registrar', 'asistencia.grupo')] }
] satisfies Routes;
