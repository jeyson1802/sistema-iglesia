import { inject } from '@angular/core';
import { ResolveFn, Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GrupoMiembrosComponent } from './pages/grupo-miembros';
import { GruposListaComponent } from './pages/grupos-lista';

/** El título de la pestaña usa el nombre que la iglesia da a sus grupos (p. ej. «Células»). */
export const tituloGrupos: ResolveFn<string> = () => inject(ConfiguracionIglesiaService).nombresGrupos().nombreGrupoPlural;

export default [
    { path: '', component: GruposListaComponent, title: tituloGrupos, canActivate: [permisoGuard('grupos.ver')] },
    { path: ':id', component: GrupoMiembrosComponent, title: tituloGrupos, canActivate: [permisoGuard('grupos.ver')] }
] satisfies Routes;
