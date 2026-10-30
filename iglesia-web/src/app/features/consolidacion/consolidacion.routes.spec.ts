import { Route } from '@angular/router';
import rutas from './consolidacion.routes';

describe('consolidacion.routes', () => {
    const ruta = (path: string): Route | undefined => rutas.find((actual) => actual.path === path);

    it('protege cada pantalla con sus permisos', () => {
        ['', 'rondas/nueva', 'rondas/:id', 'mis-asignados', 'casos/:id', 'resultados'].forEach((path) => expect(ruta(path)?.canActivate?.length).withContext(path).toBe(1));
    });

    it('«nueva» va antes que el detalle para no tomarse como id', () => {
        expect(rutas.findIndex(({ path }) => path === 'rondas/nueva')).toBeLessThan(rutas.findIndex(({ path }) => path === 'rondas/:id'));
    });
});
