import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { MiembroFichaComponent } from './pages/miembro-ficha';
import { MiembrosListaComponent } from './pages/miembros-lista';
import { SeguimientoAsistenciaMiembroComponent } from './pages/seguimiento-asistencia-miembro';

export default [
    { path: '', component: MiembrosListaComponent, title: 'Miembros', canActivate: [permisoGuard('miembros.ver')] },
    { path: 'nuevo', component: MiembroFichaComponent, title: 'Nuevo miembro', canActivate: [permisoGuard('miembros.crear')] },
    { path: ':id', component: MiembroFichaComponent, title: 'Ficha del miembro', canActivate: [permisoGuard('miembros.ver')] },
    { path: ':id/asistencia', component: SeguimientoAsistenciaMiembroComponent, title: 'Seguimiento de asistencia', canActivate: [permisoGuard('asistencia.ver', 'miembros.ver')] }
] satisfies Routes;
