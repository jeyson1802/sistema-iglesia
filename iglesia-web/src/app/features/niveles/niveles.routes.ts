import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { NivelesListaComponent } from './pages/niveles-lista';

export default [{ path: '', component: NivelesListaComponent, title: 'Niveles', canActivate: [permisoGuard('niveles.administrar')] }] satisfies Routes;
