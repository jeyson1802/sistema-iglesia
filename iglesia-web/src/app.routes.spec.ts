import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet, Routes } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { crearRutasApp } from './app.routes';
import { TarjetaAccesoComponent } from './app/core/auth/components/tarjeta-acceso/tarjeta-acceso';
import { Rol, RUTA_INICIO_POR_ROL } from './app/core/auth/models/rol';
import { Perfil } from './app/core/auth/models/perfil';
import { Permiso } from './app/core/auth/models/permiso';
import { SesionService } from './app/core/auth/services/sesion.service';
import { perfilBase, perfilPersonalizado } from './app/core/auth/testing/perfiles-prueba';
import { SIN_CONFIGURADOR_DE_TEMA } from './app/core/auth/testing/sin-configurador-de-tema';
import { AppLayout } from './app/layout/component/app.layout';
import { Notfound } from './app/pages/notfound/notfound';

describe('Rutas de la aplicación', () => {
    const RUTAS_DEMO = ['dashboard', 'uikit', 'documentation', 'pages', 'landing'];
    const autenticado = signal(false);
    const perfilActivo = signal<Perfil | null>(null);
    const debeCambiarContrasena = signal(false);
    const operaEnIglesia = signal(true);
    const permisosActivos = computed(() => new Set(perfilActivo()?.permisos ?? []));
    let harness: RouterTestingHarness | undefined;

    const configurar = (produccion: boolean): void => {
        const sesion = {
            autenticado,
            perfilActivo,
            usuarioActual: signal({ id: 1, correo: 'u@i.test', iglesiaId: 1, miembroId: 2, perfiles: [perfilBase('LIDER'), perfilBase('MIEMBRO')] }),
            tieneVariosPerfiles: signal(true),
            debeCambiarContrasena,
            operaEnIglesia,
            actuandoEnIglesia: signal(false),
            tienePermiso: (permiso: Permiso) => permisosActivos().has(permiso),
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
        debeCambiarContrasena.set(false);
        operaEnIglesia.set(true);
    });

    afterEach(() => (harness = undefined));

    const navegarA = async (url: string): Promise<string> => {
        harness ??= await RouterTestingHarness.create();
        await harness.navigateByUrl(url);
        return TestBed.inject(Router).url;
    };

    const iniciarSesionCon = (perfil: Rol | Perfil | null): void => {
        autenticado.set(true);
        perfilActivo.set(typeof perfil === 'string' ? perfilBase(perfil) : perfil);
    };

    const rutasDe = (rutas: Routes): string[] => rutas.flatMap((ruta) => [ruta.path ?? '', ...rutasDe(ruta.children ?? [])]);

    describe('sin sesión', () => {
        beforeEach(() => configurar(true));

        for (const url of ['/', '/mi-perfil', '/administracion', '/miembros', '/usuarios', '/roles', '/iglesias', '/cuenta/contrasena']) {
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

        it('un perfil personalizado con miembros.ver abre /miembros', async () => {
            iniciarSesionCon(perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver']));

            expect(await navegarA('/miembros')).toBe('/miembros');
        });

        it('sin miembros.ver, /miembros redirige a acceso denegado aunque el rol base sea ADMIN_IGLESIA', async () => {
            iniciarSesionCon(perfilPersonalizado(11, 'Tesorería', 'ADMIN_IGLESIA', []));

            expect(await navegarA('/miembros')).toBe('/auth/acceso-denegado');
        });

        it('con el perfil Líder, /roles redirige a acceso denegado', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/roles')).toBe('/auth/acceso-denegado');
        });

        it('con roles.ver, /roles abre la administración de roles', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/roles')).toBe('/roles');
        });

        it('con el perfil Líder, /usuarios redirige a acceso denegado', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/usuarios')).toBe('/auth/acceso-denegado');
        });

        it('con usuarios.ver, /usuarios abre la administración de usuarios', async () => {
            iniciarSesionCon(perfilPersonalizado(12, 'Soporte', 'ADMIN_IGLESIA', ['usuarios.ver']));

            expect(await navegarA('/usuarios')).toBe('/usuarios');
        });

        it('sin iglesias.ver, /iglesias redirige a acceso denegado', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/iglesias')).toBe('/auth/acceso-denegado');
        });

        it('el administrador del sistema abre /iglesias', async () => {
            operaEnIglesia.set(false);
            iniciarSesionCon('ADMIN_SISTEMA');

            expect(await navegarA('/iglesias')).toBe('/iglesias');
        });

        it('el administrador del sistema sin iglesia fijada va de /usuarios, /roles y /miembros al catálogo de iglesias', async () => {
            operaEnIglesia.set(false);
            iniciarSesionCon('ADMIN_SISTEMA');

            expect(await navegarA('/usuarios')).toBe('/iglesias');
            expect(await navegarA('/roles')).toBe('/iglesias');
            expect(await navegarA('/miembros')).toBe('/iglesias');
        });

        it('con niveles.administrar, /niveles abre el catálogo de niveles', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/niveles')).toBe('/niveles');
        });

        it('con el perfil Líder, /niveles redirige a acceso denegado', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/niveles')).toBe('/auth/acceso-denegado');
        });

        it('con grupos.ver, /grupos abre los grupos', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/grupos')).toBe('/grupos');
            expect(await navegarA('/grupos/4')).toBe('/grupos/4');
        });

        it('con el perfil Líder, /grupos y los miembros de un grupo redirigen a acceso denegado', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/grupos')).toBe('/auth/acceso-denegado');
            expect(await navegarA('/grupos/4')).toBe('/auth/acceso-denegado');
        });

        it('con aportes.ver y aportes.configurar, el administrador abre /aportes y su configuración', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/aportes')).toBe('/aportes');
            expect(await navegarA('/aportes/configuracion')).toBe('/aportes/configuracion');
        });

        it('el Líder abre sus aportes, pero no la configuración', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/aportes')).toBe('/aportes');
            expect(await navegarA('/aportes/configuracion')).toBe('/auth/acceso-denegado');
        });

        it('el administrador del sistema sin iglesia fijada va de /aportes al catálogo de iglesias', async () => {
            operaEnIglesia.set(false);
            iniciarSesionCon('ADMIN_SISTEMA');

            expect(await navegarA('/aportes')).toBe('/iglesias');
        });

        it('el Líder abre /miembros y una ficha, pero no el alta', async () => {
            iniciarSesionCon('LIDER');

            expect(await navegarA('/miembros')).toBe('/miembros');
            expect(await navegarA('/miembros/5')).toBe('/miembros/5');
            expect(await navegarA('/miembros/nuevo')).toBe('/auth/acceso-denegado');
        });

        it('con miembros.crear, /miembros/nuevo abre el alta', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/miembros/nuevo')).toBe('/miembros/nuevo');
        });

        it('el administrador del sistema sin iglesia fijada va de /niveles y /grupos al catálogo de iglesias', async () => {
            operaEnIglesia.set(false);
            iniciarSesionCon(perfilPersonalizado(1, 'Administrador del sistema', 'ADMIN_SISTEMA', ['iglesias.ver', 'niveles.administrar', 'grupos.ver']));

            expect(await navegarA('/niveles')).toBe('/iglesias');
            expect(await navegarA('/grupos')).toBe('/iglesias');
        });

        it('dentro de una iglesia, el administrador del sistema abre /usuarios', async () => {
            iniciarSesionCon('ADMIN_SISTEMA');

            expect(await navegarA('/usuarios')).toBe('/usuarios');
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

    describe('con la contraseña temporal', () => {
        beforeEach(() => {
            configurar(true);
            debeCambiarContrasena.set(true);
        });

        it('la raíz y cualquier página del layout redirigen al cambio de contraseña', async () => {
            iniciarSesionCon('ADMIN_IGLESIA');

            expect(await navegarA('/')).toBe('/cuenta/contrasena');
            expect(await navegarA('/miembros')).toBe('/cuenta/contrasena');
            expect(await navegarA('/roles')).toBe('/cuenta/contrasena');
            expect(await navegarA('/usuarios')).toBe('/cuenta/contrasena');
        });

        it('el cambio de contraseña se abre', async () => {
            iniciarSesionCon('MIEMBRO');

            expect(await navegarA('/cuenta/contrasena')).toBe('/cuenta/contrasena');
        });

        it('sin perfil elegido sigue yendo primero al selector de perfil', async () => {
            iniciarSesionCon(null);

            expect(await navegarA('/miembros')).toBe('/auth/seleccionar-perfil');
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
