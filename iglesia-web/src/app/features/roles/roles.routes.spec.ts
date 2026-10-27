import rolesRoutes from './roles.routes';
import { RolesListaComponent } from './pages/roles-lista';

describe('Rutas de roles', () => {
    it('la lista es la ruta raíz de la feature y está protegida por un guard', () => {
        expect(rolesRoutes.length).toBe(1);
        expect(rolesRoutes[0].path).toBe('');
        expect(rolesRoutes[0].component).toBe(RolesListaComponent);
        expect(rolesRoutes[0].canActivate?.length).toBe(1);
    });
});
