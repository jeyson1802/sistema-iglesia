import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import authRoutes from './auth.routes';
import { TarjetaAccesoComponent } from './components/tarjeta-acceso/tarjeta-acceso';
import { Rol } from './models/rol';
import { SesionService } from './services/sesion.service';
import { SIN_CONFIGURADOR_DE_TEMA } from './testing/sin-configurador-de-tema';

describe('Rutas de autenticación', () => {
    const autenticado = signal(false);

    beforeEach(() => {
        autenticado.set(false);
        const sesion = {
            autenticado,
            usuarioActual: signal({ id: 1, correo: 'u@i.test', iglesiaId: 1, miembroId: 2, roles: ['MIEMBRO'] as Rol[] }),
            perfilActivo: signal<Rol | null>('MIEMBRO'),
            tieneVariosPerfiles: signal(false),
            rutaInicio: () => '/mi-perfil'
        };
        TestBed.configureTestingModule({
            providers: [provideRouter([{ path: 'auth', children: authRoutes }, { path: '**', children: [] }]), { provide: SesionService, useValue: sesion }]
        }).overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA);
    });

    let harness: RouterTestingHarness | undefined;

    const navegarA = async (url: string): Promise<string> => {
        harness ??= await RouterTestingHarness.create();
        await harness.navigateByUrl(url);
        return TestBed.inject(Router).url;
    };

    afterEach(() => (harness = undefined));

    it('sin sesión muestra el login', async () => {
        expect(await navegarA('/auth/login')).toBe('/auth/login');
    });

    it('con sesión el login redirige a la página de inicio', async () => {
        autenticado.set(true);

        expect(await navegarA('/auth/login')).toBe('/mi-perfil');
    });

    it('sin sesión el selector de perfil y el acceso denegado redirigen al login', async () => {
        expect(await navegarA('/auth/seleccionar-perfil')).toBe('/auth/login');
        expect(await navegarA('/auth/acceso-denegado')).toBe('/auth/login');
    });

    it('con sesión muestra el selector de perfil y el acceso denegado', async () => {
        autenticado.set(true);

        expect(await navegarA('/auth/seleccionar-perfil')).toBe('/auth/seleccionar-perfil');
        expect(await navegarA('/auth/acceso-denegado')).toBe('/auth/acceso-denegado');
    });
});
