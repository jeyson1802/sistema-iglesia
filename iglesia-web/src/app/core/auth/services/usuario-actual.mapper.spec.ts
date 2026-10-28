import { PerfilResponse, UsuarioResponse } from '../models/usuario-response';
import { aUsuarioActual } from './usuario-actual.mapper';

describe('aUsuarioActual', () => {
    const miembro: PerfilResponse = { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', permisos: [] };
    const secretaria: PerfilResponse = { id: 7, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA', permisos: ['miembros.ver', 'miembros.crear'] };
    const respuesta: UsuarioResponse = { id: 7, correo: 'secretaria@iglesia.test', iglesiaId: 1, miembroId: 20, debeCambiarContrasena: false, perfiles: [miembro, secretaria] };

    it('copia los datos del usuario y sus perfiles', () => {
        const usuario = aUsuarioActual(respuesta);

        expect(usuario).toEqual({
            id: 7,
            correo: 'secretaria@iglesia.test',
            iglesiaId: 1,
            miembroId: 20,
            debeCambiarContrasena: false,
            perfiles: [
                { id: 7, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA', permisos: ['miembros.ver', 'miembros.crear'] },
                { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', permisos: [] }
            ]
        });
    });

    it('ordena los perfiles por la jerarquía de su rol base y luego por nombre', () => {
        const tesoreria: PerfilResponse = { id: 8, nombre: 'Tesorería', rolBase: 'ADMIN_IGLESIA', permisos: [] };
        const consejo: PerfilResponse = { id: 9, nombre: 'Consejo', rolBase: 'ADMIN_IGLESIA', permisos: [] };
        const lider: PerfilResponse = { id: 3, nombre: 'Líder', rolBase: 'LIDER', permisos: [] };

        const usuario = aUsuarioActual({ ...respuesta, perfiles: [miembro, tesoreria, lider, consejo] });

        expect(usuario.perfiles.map((perfil) => perfil.nombre)).toEqual(['Consejo', 'Tesorería', 'Líder', 'Miembro']);
    });

    it('descarta los permisos que la interfaz no conoce', () => {
        const conDesconocido: PerfilResponse = { ...secretaria, permisos: ['miembros.ver', 'ofrendas.ver'] };

        const usuario = aUsuarioActual({ ...respuesta, perfiles: [conDesconocido] });

        expect(usuario.perfiles[0].permisos).toEqual(['miembros.ver']);
    });

    it('descarta los perfiles cuyo rol base la interfaz no conoce', () => {
        const desconocido: PerfilResponse = { id: 99, nombre: 'Otro', rolBase: 'DESCONOCIDO', permisos: [] };

        const usuario = aUsuarioActual({ ...respuesta, perfiles: [desconocido, miembro] });

        expect(usuario.perfiles.map((perfil) => perfil.id)).toEqual([4]);
    });

    it('copia si la contraseña es temporal', () => {
        expect(aUsuarioActual({ ...respuesta, debeCambiarContrasena: true }).debeCambiarContrasena).toBeTrue();
    });

    it('no modifica la respuesta original', () => {
        const perfiles = [miembro, secretaria];

        aUsuarioActual({ ...respuesta, perfiles });

        expect(perfiles).toEqual([miembro, secretaria]);
    });
});
