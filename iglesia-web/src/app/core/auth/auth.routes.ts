import { Routes } from '@angular/router';
import { autenticadoGuard, invitadoGuard } from './guards/auth.guards';
import { AccesoDenegadoComponent } from './pages/acceso-denegado';
import { LoginComponent } from './pages/login';
import { SeleccionarPerfilComponent } from './pages/seleccionar-perfil';

export default [
    { path: 'login', component: LoginComponent, title: 'Iniciar sesión', canActivate: [invitadoGuard] },
    { path: 'seleccionar-perfil', component: SeleccionarPerfilComponent, title: 'Elegir perfil', canActivate: [autenticadoGuard] },
    { path: 'acceso-denegado', component: AccesoDenegadoComponent, title: 'Acceso denegado', canActivate: [autenticadoGuard] }
] satisfies Routes;
