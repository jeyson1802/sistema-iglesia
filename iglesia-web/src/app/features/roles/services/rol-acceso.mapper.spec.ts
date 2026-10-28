import { PermisoResponse, RolAccesoResponse } from '../models/rol-acceso';
import { CATALOGO } from '../testing/roles-prueba';
import { agruparPorModulo, aPermisosCatalogo, aRolAcceso, aRolesAcceso } from './rol-acceso.mapper';

describe('Mapper de roles de acceso', () => {
    const respuesta: RolAccesoResponse = {
        id: 10,
        nombre: 'Secretaría',
        rolBase: 'ADMIN_IGLESIA',
        base: false,
        activo: true,
        permisos: ['miembros.ver', 'miembros.crear'],
        cantidadUsuarios: 3,
        requiereMiembro: false
    };

    it('adapta un rol de la API', () => {
        expect(aRolAcceso(respuesta)).toEqual({ ...respuesta, rolBase: 'ADMIN_IGLESIA', permisos: ['miembros.ver', 'miembros.crear'] });
    });

    it('descarta los permisos que la interfaz no conoce', () => {
        expect(aRolAcceso({ ...respuesta, permisos: ['miembros.ver', 'ofrendas.ver'] })?.permisos).toEqual(['miembros.ver']);
    });

    it('descarta los roles con un rol base desconocido', () => {
        expect(aRolAcceso({ ...respuesta, rolBase: 'OTRO' })).toBeNull();
        expect(aRolesAcceso([respuesta, { ...respuesta, id: 11, rolBase: 'OTRO' }]).map((rol) => rol.id)).toEqual([10]);
    });

    it('descarta del catálogo los permisos desconocidos y conserva el orden', () => {
        const permisos: PermisoResponse[] = [
            { codigo: 'miembros.ver', modulo: 'Miembros', descripcion: 'Ver' },
            { codigo: 'ofrendas.ver', modulo: 'Ofrendas', descripcion: 'Ver ofrendas' },
            { codigo: 'roles.ver', modulo: 'Roles', descripcion: 'Ver roles' }
        ];

        expect(aPermisosCatalogo(permisos).map((permiso) => permiso.codigo)).toEqual(['miembros.ver', 'roles.ver']);
    });

    it('agrupa los permisos por módulo en el orden en que llegan', () => {
        const grupos = agruparPorModulo(CATALOGO);

        expect(grupos.map((grupo) => grupo.modulo)).toEqual(['Miembros', 'Roles', 'Usuarios']);
        expect(grupos[0].permisos.length).toBe(4);
        expect(grupos[2].permisos.map((permiso) => permiso.codigo)).toEqual(['usuarios.ver', 'usuarios.administrar']);
    });
});
