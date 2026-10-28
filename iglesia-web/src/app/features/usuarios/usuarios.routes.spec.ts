import usuariosRoutes from './usuarios.routes';
import { UsuariosListaComponent } from './pages/usuarios-lista';

describe('Rutas de usuarios', () => {
    it('la lista es la ruta raíz de la feature y está protegida por un guard', () => {
        expect(usuariosRoutes.length).toBe(1);
        expect(usuariosRoutes[0].path).toBe('');
        expect(usuariosRoutes[0].component).toBe(UsuariosListaComponent);
        expect(usuariosRoutes[0].title).toBe('Usuarios');
        expect(usuariosRoutes[0].canActivate?.length).toBe(1);
    });
});
