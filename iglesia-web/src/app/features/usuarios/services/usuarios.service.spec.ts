import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { usuarioCon } from '@/app/core/auth/testing/perfiles-prueba';
import { UsuarioActual } from '@/app/core/auth/models/usuario-actual';
import { Page } from '@/app/core/models/page';
import { FiltroUsuarios, UsuarioAdmin, UsuarioAdminResponse } from '../models/usuario-admin';
import { UsuariosService } from './usuarios.service';

describe('UsuariosService', () => {
    const usuariosUrl = `${environment.seguridadUrl}/usuarios`;
    const sinFiltros: FiltroUsuarios = { correo: '', activo: null, rolId: null };
    const respuesta: UsuarioAdminResponse = {
        id: 12,
        correo: 'secretaria@iglesia.test',
        iglesiaId: 1,
        miembroId: null,
        activo: true,
        debeCambiarContrasena: true,
        roles: [{ id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA' }],
        creadoEn: '2026-10-26T14:30:00Z'
    };
    const usuarioActual = signal<UsuarioActual | null>({ ...usuarioCon('ADMIN_IGLESIA'), iglesiaId: 7 });

    let servicio: UsuariosService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: SesionService, useValue: { usuarioActual } }] });
        servicio = TestBed.inject(UsuariosService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    const pedirPagina = (filtro: FiltroUsuarios, pagina: number): ReturnType<HttpTestingController['expectOne']> => {
        servicio.listar(filtro, pagina).subscribe();
        return http.expectOne((peticion) => peticion.url === usuariosUrl);
    };

    describe('listar', () => {
        it('pide la página indicada, de 10 en 10 y ordenada por correo, y la adapta', () => {
            let pagina: Page<UsuarioAdmin> | undefined;

            servicio.listar(sinFiltros, 2).subscribe((p) => (pagina = p));

            const peticion = http.expectOne((p) => p.url === usuariosUrl);
            expect(peticion.request.method).toBe('GET');
            expect(peticion.request.params.get('page')).toBe('2');
            expect(peticion.request.params.get('size')).toBe('10');
            expect(peticion.request.params.get('sort')).toBe('correo,asc');
            peticion.flush({ content: [respuesta], page: { size: 10, number: 2, totalElements: 21, totalPages: 3 } });
            expect(pagina?.content[0].creadoEn).toEqual(new Date(Date.UTC(2026, 9, 26, 14, 30)));
            expect(pagina?.page.totalElements).toBe(21);
        });

        it('sin filtros no envía correo, activo ni rolId', () => {
            const peticion = pedirPagina(sinFiltros, 0);

            expect(peticion.request.params.keys()).toEqual(['page', 'size', 'sort']);
            peticion.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
        });

        it('envía los filtros con valor, con el correo sin espacios alrededor', () => {
            const peticion = pedirPagina({ correo: '  prueba ', activo: false, rolId: 10 }, 0);

            expect(peticion.request.params.get('correo')).toBe('prueba');
            expect(peticion.request.params.get('activo')).toBe('false');
            expect(peticion.request.params.get('rolId')).toBe('10');
            peticion.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
        });

        it('un correo solo con espacios no filtra', () => {
            const peticion = pedirPagina({ ...sinFiltros, correo: '   ' }, 0);

            expect(peticion.request.params.has('correo')).toBeFalse();
            peticion.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
        });
    });

    it('crea el usuario en la iglesia del usuario autenticado, sin miembro y con la contraseña temporal', () => {
        let creado: UsuarioAdmin | undefined;

        servicio.crear({ correo: 'secretaria@iglesia.test', contrasenaTemporal: 'Temporal123', rolIds: [2] }).subscribe((u) => (creado = u));

        const peticion = http.expectOne(usuariosUrl);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toEqual({ correo: 'secretaria@iglesia.test', contrasena: 'Temporal123', iglesiaId: 7, miembroId: null, rolIds: [2] });
        peticion.flush(respuesta);
        expect(creado?.debeCambiarContrasena).toBeTrue();
    });

    it('crea el usuario en la iglesia indicada', () => {
        servicio.crearEnIglesia(3, { correo: 'pastor@norte.test', contrasenaTemporal: 'Temporal123', rolIds: [2] }).subscribe();

        const peticion = http.expectOne(usuariosUrl);
        expect(peticion.request.body).toEqual({ correo: 'pastor@norte.test', contrasena: 'Temporal123', iglesiaId: 3, miembroId: null, rolIds: [2] });
        peticion.flush(respuesta);
    });

    it('crea para un miembro el usuario vinculado, en la iglesia del usuario autenticado', () => {
        servicio.crearParaMiembro(25, { correo: 'juan@correo.test', contrasenaTemporal: 'Temporal123', rolIds: [3] }).subscribe();

        const peticion = http.expectOne(usuariosUrl);
        expect(peticion.request.body).toEqual({ correo: 'juan@correo.test', contrasena: 'Temporal123', iglesiaId: 7, miembroId: 25, rolIds: [3] });
        peticion.flush({ ...respuesta, miembroId: 25 });
    });

    it('busca el usuario vinculado a un miembro, o nulo si no tiene', () => {
        const encontrados: (UsuarioAdmin | null)[] = [];

        servicio.usuarioDeMiembro(25).subscribe((u) => encontrados.push(u));
        servicio.usuarioDeMiembro(26).subscribe((u) => encontrados.push(u));

        const [con, sin] = http.match((r) => r.url === usuariosUrl);
        expect(con.request.params.get('miembroId')).toBe('25');
        con.flush({ content: [{ ...respuesta, miembroId: 25 }], page: { size: 1, number: 0, totalElements: 1, totalPages: 1 } });
        sin.flush({ content: [], page: { size: 1, number: 0, totalElements: 0, totalPages: 0 } });
        expect(encontrados.map((u) => u?.miembroId ?? null)).toEqual([25, null]);
    });

    it('al actualizar reenvía el miembro que ya tenía', () => {
        servicio.actualizar(12, { correo: 'nuevo@iglesia.test', rolIds: [2, 3], activo: false }, 25).subscribe();

        const peticion = http.expectOne(`${usuariosUrl}/12`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ correo: 'nuevo@iglesia.test', rolIds: [2, 3], miembroId: 25, activo: false });
        peticion.flush(respuesta);
    });

    it('restablece la contraseña con la temporal indicada', () => {
        let completado = false;

        servicio.restablecerContrasena(12, 'Temporal123').subscribe({ complete: () => (completado = true) });

        const peticion = http.expectOne(`${usuariosUrl}/12/contrasena`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ contrasenaTemporal: 'Temporal123' });
        peticion.flush(null, { status: 204, statusText: 'No Content' });
        expect(completado).toBeTrue();
    });
});
