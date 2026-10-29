import iglesiasRoutes from './iglesias.routes';
import { IglesiaEdicionComponent } from './pages/iglesia-edicion';
import { IglesiasListaComponent } from './pages/iglesias-lista';

describe('Rutas de iglesias', () => {
    it('la lista es la ruta raíz de la feature y está protegida por un guard', () => {
        expect(iglesiasRoutes[0].path).toBe('');
        expect(iglesiasRoutes[0].component).toBe(IglesiasListaComponent);
        expect(iglesiasRoutes[0].title).toBe('Iglesias');
        expect(iglesiasRoutes[0].canActivate?.length).toBe(1);
    });

    it('/iglesias/:id edita una iglesia y está protegida por un guard', () => {
        expect(iglesiasRoutes[1].path).toBe(':id');
        expect(iglesiasRoutes[1].component).toBe(IglesiaEdicionComponent);
        expect(iglesiasRoutes[1].canActivate?.length).toBe(1);
    });
});
