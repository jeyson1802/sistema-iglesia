import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet, Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { crearRutasApp } from './app.routes';
import { TarjetaAccesoComponent } from './app/core/auth/components/tarjeta-acceso/tarjeta-acceso';
import { Rol, RUTA_INICIO_POR_ROL } from './app/core/auth/models/rol';
import { Perfil } from './app/core/auth/models/perfil';
import { SesionService } from './app/core/auth/services/sesion.service';
import { perfilBase } from './app/core/auth/testing/perfiles-prueba';
import { SIN_CONFIGURADOR_DE_TEMA } from './app/core/auth/testing/sin-configurador-de-tema';
import { AppLayout } from './app/layout/component/app.layout';
import { Notfound } from './app/pages/notfound/notfound';

describe('Rutas de la aplicación', () => {
    const RUTAS_DEMO = ['dashboard', 'uikit', 'documentation', 'pages', 'landing'];
    const autenticado = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    let harness: RouterTestingHarness | undefined;

    const configurar = (produccion: boolean): void => {
        const sesion = {
            autenticado,
            perfilActivo,
            usuarioActual: signal({ id: 1, correo: 'u@i.test', iglesiaId: 1, miembroId: 2, perfiles: [perfilBase('LIDER'), perfilBase('MIEMBRO')] }),
            tieneVariosPerfiles: signal(true),
            rutaInicio: () => {
                const perfil = perfilActivo();
                if (!autenticado()) return '/auth/login';
                return perfil ? RUTA_INICIO_POR_ROL[perfil.rolBase] : '/auth/seleccionar-perfil';
            }
        };
        TestBed.configureTestingModule({
            providers: [provideRouter(crearRutasApp(produccion)), provideHttpClient(), provideHttpClientTesting(), { provide: SesionService, useValue: sesion }]
        })
            // El layout y las pantallas de acceso de Sakai no son parte de lo que se prueba aquí.
            .overrideComponent(AppLayout, { set: { imports: [RouterOutlet], template: '<router-outlet />' } })
            .overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA)
            .overrideComponent(Notfound, SIN_CONFIGURADOR_DE_TEMA);
    };

    beforeEach(() => {
        autenticado.set(false);
        perfilActivo.set(null);
    });

    afterEach(() => (harness = undefined));

    const navegarA = async (url: string): Promise<string> => {
        harness ??= await RouterTestingHarness.create();
        await harness.navigateByUrl(url);
        return TestBed.inject(Router).url;
    };

    const iniciarSesionCon = (rol: Rol | null): void => {
        autenticado.set(true);
        perfilActivo.set(rol ? perfilBase(rol) : null);
    };

    const rutasDe = (rutas: Routes): string[] => rutas.flatMap((ruta) => [ruta.path ?? '', ...rutasDe(ruta.children ?? [])]);

    describe('sin sesión', () => {
        beforeEach(() => configurar(true));

        for (const url of ['/', '/mi-perfil', '/administracion', '/miembros', '/cuenta/contrasena']) {
            it(`${url} redirige al login`, async () => {
                expect(await navegarA(url)).toBe('/auth/login');
            });
        }
    });

    describe('con sesión', () => {
        beforeEach(() => configurar(true));

        it('la raíz redirige al inicio del perfil activo', async () => {
            iniciarSesionCon('MIEMBRO');

            expect(await navegarA('/')).toBe('/mi-perfil');
        });

        it('sin perfil elegido redirige al selector de perfil', async () => {
            iniciarSesionCon(null);

            expect(await navegarA('/mi-red')).toBe('/auth/seleccionar-perfil');
        });

        it('con el perfil Miembro, /administracion y /miembros redirigen a acceso denegado', async () => {
            iniciarSesionCon('MIEMBRO');

            expect(await navegarA('/administracion')).toBe('/auth/acceso-denegado');
            expect(await navegarA('/miembros')).toBe('/auth/acceso-denegado');
        });

        it('cualquier perfil puede abrir el cambio de contraseña', async () => {
            iniciarSesionCon('MIEMBRO');

            expect(await navegarA('/cuenta/contrasena')).toBe('/cuenta/contrasena');
        });

        it('una ruta desconocida lleva a la página no encontrada', async () => {
            iniciarSesionCon('MIEMBRO');

            expect(await navegarA('/no-existe')).toBe('/notfound');
        });
    });

    describe('demos de Sakai', () => {
        it('en producción no existen y redirigen a /notfound', async () => {
            configurar(true);
            iniciarSesionCon('MIEMBRO');

            expect(rutasDe(crearRutasApp(true))).not.toContain(jasmine.stringMatching(new RegExp(`^(${RUTAS_DEMO.join('|')})$`)));
            for (const demo of RUTAS_DEMO) {
                expect(await navegarA(`/${demo}`)).toBe('/notfound');
            }
        });

        it('en desarrollo existen y quedan protegidas por el login', async () => {
            const rutas = crearRutasApp(false);

            expect(rutasDe(rutas)).toEqual(jasmine.arrayContaining(RUTAS_DEMO));
            expect(rutas.find((ruta) => ruta.path === 'landing')?.canActivate?.length).toBe(1);
        });
    });
});
