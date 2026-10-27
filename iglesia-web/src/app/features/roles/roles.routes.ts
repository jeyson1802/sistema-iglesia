import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { RolesListaComponent } from './pages/roles-lista';

export default [{ path: '', component: RolesListaComponent, title: 'Roles', canActivate: [permisoGuard('roles.ver')] }] satisfies Routes;
