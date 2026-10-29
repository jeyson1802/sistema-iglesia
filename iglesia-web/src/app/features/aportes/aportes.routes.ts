import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { AportesListaComponent } from './pages/aportes-lista';
import { ConfiguracionAportesComponent } from './pages/configuracion-aportes';

export default [
    { path: '', component: AportesListaComponent, title: 'Aportes', canActivate: [permisoGuard('aportes.ver')] },
    { path: 'configuracion', component: ConfiguracionAportesComponent, title: 'Configuración de aportes', canActivate: [permisoGuard('aportes.configurar')] }
] satisfies Routes;
