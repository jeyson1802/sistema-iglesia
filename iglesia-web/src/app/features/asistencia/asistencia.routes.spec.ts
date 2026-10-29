import { Route } from '@angular/router';
import rutas from './asistencia.routes';

describe('asistencia.routes', () => {
    const ruta = (path: string): Route | undefined => rutas.find((actual) => actual.path === path);

    it('protege cada pantalla con sus permisos', () => {
        expect(ruta('')?.canActivate?.length).toBe(1);
        expect(ruta('configuracion')?.canActivate?.length).toBe(1);
        expect(ruta('seguimiento')?.canActivate?.length).toBe(1);
        expect(ruta('reuniones/:id/detalle')?.canActivate?.length).toBe(1);
    });

    it('la pantalla de escaneo se carga aparte para no engordar el resto', async () => {
        const escaneo = ruta('reuniones/:id');

        expect(escaneo?.component).toBeUndefined();
        expect(await escaneo?.loadComponent?.()).toBeDefined();
    });
});
