import { esPermiso, PERMISOS } from './permiso';

describe('Permiso', () => {
    it('conoce los 19 permisos del catálogo de iglesia-seguridad', () => {
        expect(PERMISOS).toEqual([
            'miembros.ver',
            'miembros.crear',
            'miembros.editar',
            'miembros.desactivar',
            'niveles.administrar',
            'grupos.ver',
            'grupos.administrar',
            'aportes.ver',
            'aportes.registrar',
            'aportes.editar',
            'aportes.anular',
            'aportes.configurar',
            'usuarios.ver',
            'usuarios.administrar',
            'roles.ver',
            'roles.administrar',
            'iglesias.ver',
            'iglesias.administrar',
            'iglesia.personalizar'
        ]);
    });

    it('reconoce solo los códigos del catálogo', () => {
        expect(esPermiso('roles.ver')).toBeTrue();
        expect(esPermiso('ofrendas.ver')).toBeFalse();
        expect(esPermiso('ROLES.VER')).toBeFalse();
    });
});
