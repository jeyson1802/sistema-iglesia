import { UsuarioResponse } from '../models/usuario-response';
import { aUsuarioActual } from './usuario-actual.mapper';

describe('aUsuarioActual', () => {
    const respuesta: UsuarioResponse = { id: 7, correo: 'lider@iglesia.test', iglesiaId: 1, miembroId: 20, roles: ['MIEMBRO', 'LIDER'] };

    it('copia los datos del usuario', () => {
        const usuario = aUsuarioActual(respuesta);

        expect(usuario).toEqual({ id: 7, correo: 'lider@iglesia.test', iglesiaId: 1, miembroId: 20, roles: ['LIDER', 'MIEMBRO'] });
    });

    it('ordena los roles por jerarquía, del mayor al menor', () => {
        const usuario = aUsuarioActual({ ...respuesta, roles: ['MIEMBRO', 'ADMIN_IGLESIA', 'LIDER'] });

        expect(usuario.roles).toEqual(['ADMIN_IGLESIA', 'LIDER', 'MIEMBRO']);
    });

    it('descarta roles que la interfaz no conoce', () => {
        const usuario = aUsuarioActual({ ...respuesta, roles: ['MIEMBRO', 'DESCONOCIDO'] });

        expect(usuario.roles).toEqual(['MIEMBRO']);
    });

    it('no modifica la respuesta original', () => {
        const roles = ['MIEMBRO', 'LIDER'];

        aUsuarioActual({ ...respuesta, roles });

        expect(roles).toEqual(['MIEMBRO', 'LIDER']);
    });
});
