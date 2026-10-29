import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { MiIglesiaComponent } from './pages/mi-iglesia';

export default [{ path: '', component: MiIglesiaComponent, title: 'Mi iglesia', canActivate: [permisoGuard('iglesia.personalizar')] }] satisfies Routes;
