import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { IglesiasListaComponent } from './pages/iglesias-lista';

export default [{ path: '', component: IglesiasListaComponent, title: 'Iglesias', canActivate: [permisoGuard('iglesias.ver')] }] satisfies Routes;
