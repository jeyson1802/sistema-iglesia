import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { UsuarioActual } from '@/app/core/auth/models/usuario-actual';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { usuarioCon } from '@/app/core/auth/testing/perfiles-prueba';
import { ConfiguracionIglesiaService, NOMBRES_GRUPOS_POR_DEFECTO } from './configuracion-iglesia.service';

describe('ConfiguracionIglesiaService', () => {
    const url = `${environment.apiUrl}/configuracion-iglesia`;
    const hogares = { nombreGrupoSingular: 'Hogar de paz', nombreGrupoPlural: 'Hogares de paz' };
    let usuario: ReturnType<typeof signal<UsuarioActual | null>>;
    let http: HttpTestingController;

    const crear = (): ConfiguracionIglesiaService => {
        const servicio = TestBed.inject(ConfiguracionIglesiaService);
        TestBed.tick();
        return servicio;
    };

    beforeEach(() => {
        usuario = signal<UsuarioActual | null>(usuarioCon('ADMIN_IGLESIA'));
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), { provide: SesionService, useValue: { usuarioActual: usuario } }]
        });
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('carga el nombre de los grupos de la iglesia de la sesión', () => {
        const servicio = crear();

        http.expectOne(url).flush(hogares);

        expect(servicio.nombresGrupos()).toEqual(hogares);
    });

    it('sin iglesia no pide nada y usa los nombres por defecto', () => {
        usuario.set({ ...usuarioCon('ADMIN_SISTEMA'), iglesiaId: null });

        const servicio = crear();

        http.expectNone(url);
        expect(servicio.nombresGrupos()).toEqual(NOMBRES_GRUPOS_POR_DEFECTO);
    });

    it('si la carga falla se queda con los nombres por defecto', () => {
        const servicio = crear();

        http.expectOne(url).flush(null, { status: 500, statusText: 'Error' });

        expect(servicio.nombresGrupos()).toEqual(NOMBRES_GRUPOS_POR_DEFECTO);
    });

    it('al guardar envía los nombres y los comparte', () => {
        const servicio = crear();
        http.expectOne(url).flush(NOMBRES_GRUPOS_POR_DEFECTO);

        servicio.guardar(hogares).subscribe();

        const peticion = http.expectOne(url);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual(hogares);
        peticion.flush(hogares);
        expect(servicio.nombresGrupos()).toEqual(hogares);
    });
});
