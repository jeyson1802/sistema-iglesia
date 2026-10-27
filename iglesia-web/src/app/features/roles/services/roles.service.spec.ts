import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { PermisoCatalogo, RolAcceso, RolAccesoResponse } from '../models/rol-acceso';
import { RolesService } from './roles.service';

describe('RolesService', () => {
    const rolesUrl = `${environment.seguridadUrl}/roles`;
    const secretaria: RolAccesoResponse = { id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA', base: false, activo: true, permisos: ['miembros.ver'], cantidadUsuarios: 0 };

    let servicio: RolesService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(RolesService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista los roles adaptados y sin los de rol base desconocido', () => {
        let roles: RolAcceso[] | undefined;

        servicio.listar().subscribe((r) => (roles = r));

        const peticion = http.expectOne(rolesUrl);
        expect(peticion.request.method).toBe('GET');
        peticion.flush([secretaria, { ...secretaria, id: 11, rolBase: 'OTRO' }]);
        expect(roles?.map((rol) => rol.id)).toEqual([10]);
    });

    it('obtiene un rol por id', () => {
        let rol: RolAcceso | undefined;

        servicio.obtener(10).subscribe((r) => (rol = r));

        http.expectOne(`${rolesUrl}/10`).flush(secretaria);
        expect(rol?.nombre).toBe('Secretaría');
    });

    it('obtiene el catálogo de permisos', () => {
        let catalogo: PermisoCatalogo[] | undefined;

        servicio.permisos().subscribe((p) => (catalogo = p));

        http.expectOne(`${environment.seguridadUrl}/permisos`).flush([{ codigo: 'roles.ver', modulo: 'Roles', descripcion: 'Ver roles' }]);
        expect(catalogo).toEqual([{ codigo: 'roles.ver', modulo: 'Roles', descripcion: 'Ver roles' }]);
    });

    it('crea un rol con nombre, rol base y permisos', () => {
        servicio.crear({ nombre: 'Secretaría', rolBaseId: 2, permisos: ['miembros.ver'] }).subscribe();

        const peticion = http.expectOne(rolesUrl);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toEqual({ nombre: 'Secretaría', rolBaseId: 2, permisos: ['miembros.ver'] });
        peticion.flush(secretaria);
    });

    it('al actualizar no envía el rol base', () => {
        servicio.actualizar(10, { nombre: 'Secretaría general', rolBaseId: null, permisos: ['miembros.ver'] }).subscribe();

        const peticion = http.expectOne(`${rolesUrl}/10`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ nombre: 'Secretaría general', permisos: ['miembros.ver'] });
        peticion.flush(secretaria);
    });

    it('cambia el estado de un rol', () => {
        let rol: RolAcceso | undefined;

        servicio.cambiarEstado(10, false).subscribe((r) => (rol = r));

        const peticion = http.expectOne(`${rolesUrl}/10/estado`);
        expect(peticion.request.method).toBe('PATCH');
        expect(peticion.request.body).toEqual({ activo: false });
        peticion.flush({ ...secretaria, activo: false });
        expect(rol?.activo).toBeFalse();
    });
});
