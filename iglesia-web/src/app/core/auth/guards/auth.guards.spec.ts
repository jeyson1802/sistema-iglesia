import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, provideRouter, RedirectFunction, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { SesionService } from '../services/sesion.service';
import { autenticadoGuard, inicioRedirect, invitadoGuard, perfilGuard, perfilSeleccionadoGuard } from './auth.guards';
import { Perfil } from '@/app/core/auth/models/perfil';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';

describe('Guards de autenticación', () => {
    const autenticado = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    const rutaInicio = jasmine.createSpy('rutaInicio');

    beforeEach(() => {
        autenticado.set(false);
        perfilActivo.set(null);
        rutaInicio.and.returnValue('/mi-perfil');
        TestBed.configureTestingModule({
            providers: [provideRouter([]), { provide: SesionService, useValue: { autenticado, perfilActivo, rutaInicio } }]
        });
    });

    const ejecutar = (guard: CanActivateFn): ReturnType<CanActivateFn> => TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot));

    const urlDe = (resultado: unknown): string => {
        expect(resultado).toBeInstanceOf(UrlTree);
        return TestBed.inject(Router).serializeUrl(resultado as UrlTree);
    };

    describe('autenticadoGuard', () => {
        it('deja pasar con sesión', () => {
            autenticado.set(true);

            expect(ejecutar(autenticadoGuard)).toBeTrue();
        });

        it('sin sesión redirige al login', () => {
            expect(urlDe(ejecutar(autenticadoGuard))).toBe('/auth/login');
        });
    });

    describe('invitadoGuard', () => {
        it('deja pasar sin sesión', () => {
            expect(ejecutar(invitadoGuard)).toBeTrue();
        });

        it('con sesión redirige a la página de inicio', () => {
            autenticado.set(true);
            rutaInicio.and.returnValue('/mi-red');

            expect(urlDe(ejecutar(invitadoGuard))).toBe('/mi-red');
        });
    });

    describe('perfilSeleccionadoGuard', () => {
        it('deja pasar con perfil activo', () => {
            perfilActivo.set(perfilBase('LIDER'));

            expect(ejecutar(perfilSeleccionadoGuard)).toBeTrue();
        });

        it('sin perfil activo redirige al selector', () => {
            expect(urlDe(ejecutar(perfilSeleccionadoGuard))).toBe('/auth/seleccionar-perfil');
        });
    });

    describe('perfilGuard', () => {
        it('deja pasar si el perfil activo está permitido', () => {
            perfilActivo.set(perfilBase('LIDER'));

            expect(ejecutar(perfilGuard('ADMIN_IGLESIA', 'LIDER'))).toBeTrue();
        });

        it('con otro perfil redirige a acceso denegado', () => {
            perfilActivo.set(perfilBase('MIEMBRO'));

            expect(urlDe(ejecutar(perfilGuard('ADMIN_IGLESIA')))).toBe('/auth/acceso-denegado');
        });

        it('sin perfil activo redirige a acceso denegado', () => {
            expect(urlDe(ejecutar(perfilGuard('MIEMBRO')))).toBe('/auth/acceso-denegado');
        });
    });

    describe('inicioRedirect', () => {
        it('redirige a la ruta de inicio del perfil activo', () => {
            rutaInicio.and.returnValue('/administracion');

            const destino = TestBed.runInInjectionContext(() => (inicioRedirect as RedirectFunction)({} as Parameters<RedirectFunction>[0]));

            expect(destino).toBe('/administracion');
        });
    });
});
