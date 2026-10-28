import { Page } from '@/app/core/models/page';
import { UsuarioAdminResponse } from '../models/usuario-admin';
import { aPaginaUsuarios, aUsuarioAdmin } from './usuario-admin.mapper';

describe('Mapper de usuarios administrados', () => {
    const respuesta: UsuarioAdminResponse = {
        id: 12,
        correo: 'secretaria@iglesia.test',
        iglesiaId: 1,
        miembroId: null,
        activo: true,
        debeCambiarContrasena: true,
        roles: [{ id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA' }],
        creadoEn: '2026-10-26T14:30:00Z'
    };

    it('adapta un usuario de la API con la fecha de creación como Date y sin la iglesia', () => {
        expect(aUsuarioAdmin(respuesta)).toEqual({
            id: 12,
            correo: 'secretaria@iglesia.test',
            miembroId: null,
            activo: true,
            debeCambiarContrasena: true,
            roles: [{ id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA' }],
            creadoEn: new Date(Date.UTC(2026, 9, 26, 14, 30))
        });
    });

    it('conserva un rol con rol base desconocido, sin rol base, para no perderlo al guardar', () => {
        const usuario = aUsuarioAdmin({ ...respuesta, roles: [{ id: 99, nombre: 'Otro', rolBase: 'DESCONOCIDO' }] });

        expect(usuario.roles).toEqual([{ id: 99, nombre: 'Otro', rolBase: null }]);
    });

    it('adapta el contenido de una página y conserva sus metadatos', () => {
        const pagina: Page<UsuarioAdminResponse> = { content: [respuesta], page: { size: 10, number: 2, totalElements: 21, totalPages: 3 } };

        const adaptada = aPaginaUsuarios(pagina);

        expect(adaptada.content.map((usuario) => usuario.id)).toEqual([12]);
        expect(adaptada.page).toEqual({ size: 10, number: 2, totalElements: 21, totalPages: 3 });
    });
});
