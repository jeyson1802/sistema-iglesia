import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { IglesiaEdicionComponent } from './pages/iglesia-edicion';
import { IglesiasListaComponent } from './pages/iglesias-lista';

export default [
    { path: '', component: IglesiasListaComponent, title: 'Iglesias', canActivate: [permisoGuard('iglesias.ver')] },
    { path: ':id', component: IglesiaEdicionComponent, title: 'Editar iglesia', canActivate: [permisoGuard('iglesias.administrar')] }
] satisfies Routes;
