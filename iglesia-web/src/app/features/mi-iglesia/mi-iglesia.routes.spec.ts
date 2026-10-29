import miIglesiaRoutes from './mi-iglesia.routes';
import { MiIglesiaComponent } from './pages/mi-iglesia';

describe('Rutas de Mi iglesia', () => {
    it('la página es la raíz de la feature y exige iglesia.personalizar', () => {
        expect(miIglesiaRoutes.length).toBe(1);
        expect(miIglesiaRoutes[0].component).toBe(MiIglesiaComponent);
        expect(miIglesiaRoutes[0].title).toBe('Mi iglesia');
        expect(miIglesiaRoutes[0].canActivate?.length).toBe(1);
    });
});
