import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { UsuariosListaComponent } from './pages/usuarios-lista';

export default [{ path: '', component: UsuariosListaComponent, title: 'Usuarios', canActivate: [permisoGuard('usuarios.ver')] }] satisfies Routes;
