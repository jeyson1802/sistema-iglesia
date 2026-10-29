import { esPermiso, PERMISOS } from './permiso';

describe('Permiso', () => {
    it('conoce los 13 permisos del catálogo de iglesia-seguridad', () => {
        expect(PERMISOS).toEqual([
            'miembros.ver',
            'miembros.crear',
            'miembros.editar',
            'miembros.desactivar',
            'niveles.administrar',
            'grupos.ver',
            'grupos.administrar',
            'usuarios.ver',
            'usuarios.administrar',
            'roles.ver',
            'roles.administrar',
            'iglesias.ver',
            'iglesias.administrar'
        ]);
    });

    it('reconoce solo los códigos del catálogo', () => {
        expect(esPermiso('roles.ver')).toBeTrue();
        expect(esPermiso('ofrendas.ver')).toBeFalse();
        expect(esPermiso('ROLES.VER')).toBeFalse();
    });
});
