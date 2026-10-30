import { Routes } from '@angular/router';
import { permisoGuard } from '@/app/core/auth/guards/auth.guards';
import { CasoConsolidacionComponent } from './pages/caso-consolidacion';
import { ConsolidacionInicioComponent } from './pages/consolidacion-inicio';
import { DetalleRondaComponent } from './pages/detalle-ronda';
import { MisAsignadosComponent } from './pages/mis-asignados';
import { NuevaRondaComponent } from './pages/nueva-ronda';
import { ResultadosConsolidacionComponent } from './pages/resultados-consolidacion';

// El coordinador (`consolidacion.administrar`) puede todo lo que hace un consolidador, sin restricciones.
export default [
    { path: '', component: ConsolidacionInicioComponent, title: 'Consolidación', canActivate: [permisoGuard('consolidacion.ver', 'consolidacion.administrar')] },
    { path: 'rondas/nueva', component: NuevaRondaComponent, title: 'Nueva ronda', canActivate: [permisoGuard('consolidacion.administrar')] },
    { path: 'rondas/:id', component: DetalleRondaComponent, title: 'Ronda de consolidación', canActivate: [permisoGuard('consolidacion.ver', 'consolidacion.administrar')] },
    { path: 'mis-asignados', component: MisAsignadosComponent, title: 'Mis asignados', canActivate: [permisoGuard('consolidacion.contactar', 'consolidacion.administrar')] },
    { path: 'casos/:id', component: CasoConsolidacionComponent, title: 'Caso de consolidación', canActivate: [permisoGuard('consolidacion.contactar', 'consolidacion.ver', 'consolidacion.administrar')] },
    { path: 'resultados', component: ResultadosConsolidacionComponent, title: 'Resultados de consolidación', canActivate: [permisoGuard('consolidacion.administrar')] }
] satisfies Routes;
