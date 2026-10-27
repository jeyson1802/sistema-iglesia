import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Rol } from '@/app/core/auth/models/rol';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import inicioRoutes from './inicio.routes';
import { perfilBase } from '@/app/core/auth/testing/perfiles-prueba';
import { Perfil } from '@/app/core/auth/models/perfil';

describe('Rutas de inicio', () => {
    const perfilActivo = signal<Perfil | null>(null);
    let harness: RouterTestingHarness | undefined;

    beforeEach(() => {
        perfilActivo.set(null);
        const sesion = { perfilActivo, usuarioActual: signal({ id: 1, correo: 'u@i.test', iglesiaId: 1, miembroId: 2, perfiles: [perfilBase('MIEMBRO')] }) };
        TestBed.configureTestingModule({
            providers: [provideRouter([...inicioRoutes, { path: '**', children: [] }]), { provide: SesionService, useValue: sesion }]
        });
    });

    afterEach(() => (harness = undefined));

    const navegarA = async (url: string): Promise<string> => {
        harness ??= await RouterTestingHarness.create();
        await harness.navigateByUrl(url);
        return TestBed.inject(Router).url;
    };

    const casos: [Rol, string][] = [
        ['ADMIN_SISTEMA', '/sistema'],
        ['ADMIN_IGLESIA', '/administracion'],
        ['LIDER', '/mi-red'],
        ['MIEMBRO', '/mi-perfil']
    ];

    for (const [rol, ruta] of casos) {
        it(`con el perfil ${rol} abre ${ruta}`, async () => {
            perfilActivo.set(perfilBase(rol));

            expect(await navegarA(ruta)).toBe(ruta);
        });
    }

    it('con el perfil Miembro, /administracion redirige a acceso denegado', async () => {
        perfilActivo.set(perfilBase('MIEMBRO'));

        expect(await navegarA('/administracion')).toBe('/auth/acceso-denegado');
    });
});
