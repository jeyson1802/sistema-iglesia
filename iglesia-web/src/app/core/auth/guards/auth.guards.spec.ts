import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateChildFn, CanActivateFn, provideRouter, RedirectFunction, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { SesionService } from '../services/sesion.service';
import { autenticadoGuard, contrasenaVigenteGuard, iglesiaFijadaGuard, inicioRedirect, invitadoGuard, perfilSeleccionadoGuard, permisoGuard, rolBaseGuard } from './auth.guards';
import { Perfil } from '@/app/core/auth/models/perfil';
import { Permiso } from '@/app/core/auth/models/permiso';
import { perfilBase, perfilPersonalizado } from '@/app/core/auth/testing/perfiles-prueba';

describe('Guards de autenticación', () => {
    const autenticado = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    const debeCambiarContrasena = signal(false);
    const operaEnIglesia = signal(true);
    const rutaInicio = jasmine.createSpy('rutaInicio');
    const permisosActivos = computed(() => new Set(perfilActivo()?.permisos ?? []));
    const tienePermiso = (permiso: Permiso): boolean => permisosActivos().has(permiso);

    beforeEach(() => {
        autenticado.set(false);
        perfilActivo.set(null);
        debeCambiarContrasena.set(false);
        operaEnIglesia.set(true);
        rutaInicio.and.returnValue('/mi-perfil');
        TestBed.configureTestingModule({
            providers: [provideRouter([]), { provide: SesionService, useValue: { autenticado, perfilActivo, rutaInicio, tienePermiso, debeCambiarContrasena, operaEnIglesia } }]
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

    describe('rolBaseGuard', () => {
        it('deja pasar si el rol base del perfil activo está permitido', () => {
            perfilActivo.set(perfilBase('LIDER'));

            expect(ejecutar(rolBaseGuard('ADMIN_IGLESIA', 'LIDER'))).toBeTrue();
        });

        it('un perfil personalizado pasa por el rol base del que hereda', () => {
            perfilActivo.set(perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver']));

            expect(ejecutar(rolBaseGuard('ADMIN_IGLESIA'))).toBeTrue();
        });

        it('con otro rol base redirige a acceso denegado', () => {
            perfilActivo.set(perfilBase('MIEMBRO'));

            expect(urlDe(ejecutar(rolBaseGuard('ADMIN_IGLESIA')))).toBe('/auth/acceso-denegado');
        });

        it('sin perfil activo redirige a acceso denegado', () => {
            expect(urlDe(ejecutar(rolBaseGuard('MIEMBRO')))).toBe('/auth/acceso-denegado');
        });
    });

    describe('permisoGuard', () => {
        it('deja pasar si el perfil activo tiene el permiso', () => {
            perfilActivo.set(perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver']));

            expect(ejecutar(permisoGuard('miembros.ver'))).toBeTrue();
        });

        it('sin el permiso redirige a acceso denegado aunque el rol base lo tenga', () => {
            perfilActivo.set(perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver']));

            expect(urlDe(ejecutar(permisoGuard('roles.ver')))).toBe('/auth/acceso-denegado');
        });

        it('sin perfil activo redirige a acceso denegado', () => {
            expect(urlDe(ejecutar(permisoGuard('miembros.ver')))).toBe('/auth/acceso-denegado');
        });
    });

    describe('iglesiaFijadaGuard', () => {
        it('deja pasar si se opera en una iglesia', () => {
            perfilActivo.set(perfilBase('ADMIN_IGLESIA'));

            expect(ejecutar(iglesiaFijadaGuard)).toBeTrue();
        });

        it('el administrador del sistema en su vista global va al catálogo de iglesias', () => {
            operaEnIglesia.set(false);
            perfilActivo.set(perfilBase('ADMIN_SISTEMA'));

            expect(urlDe(ejecutar(iglesiaFijadaGuard))).toBe('/iglesias');
        });

        it('sin iglesia ni acceso al catálogo redirige a acceso denegado', () => {
            operaEnIglesia.set(false);
            perfilActivo.set(perfilBase('ADMIN_IGLESIA'));

            expect(urlDe(ejecutar(iglesiaFijadaGuard))).toBe('/auth/acceso-denegado');
        });
    });

    describe('contrasenaVigenteGuard', () => {
        const ejecutarHacia = (url: string): ReturnType<CanActivateChildFn> => TestBed.runInInjectionContext(() => contrasenaVigenteGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

        it('sin cambio pendiente deja pasar a cualquier página', () => {
            expect(ejecutarHacia('/miembros')).toBeTrue();
        });

        it('con cambio pendiente redirige al cambio de contraseña', () => {
            debeCambiarContrasena.set(true);

            expect(urlDe(ejecutarHacia('/miembros'))).toBe('/cuenta/contrasena');
            expect(urlDe(ejecutarHacia('/administracion'))).toBe('/cuenta/contrasena');
        });

        it('con cambio pendiente deja abrir el cambio de contraseña, aunque lleve consulta o fragmento', () => {
            debeCambiarContrasena.set(true);

            expect(ejecutarHacia('/cuenta/contrasena')).toBeTrue();
            expect(ejecutarHacia('/cuenta/contrasena?origen=menu#formulario')).toBeTrue();
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
