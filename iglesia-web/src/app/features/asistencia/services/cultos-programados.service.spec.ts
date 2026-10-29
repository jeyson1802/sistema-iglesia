import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { CultosProgramadosService } from './cultos-programados.service';

describe('CultosProgramadosService', () => {
    const url = `${environment.apiUrl}/cultos-programados`;
    let servicio: CultosProgramadosService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(CultosProgramadosService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista, crea, edita y cambia el estado', () => {
        const culto = { nombre: 'Dominical', diaSemana: 'DOMINGO' as const, hora: '10:00' };
        servicio.listar().subscribe();
        servicio.crear(culto).subscribe();
        servicio.actualizar(3, culto).subscribe();
        servicio.cambiarEstado(3, false).subscribe();

        expect(http.expectOne({ method: 'GET', url })).toBeTruthy();
        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual(culto);
        expect(http.expectOne({ method: 'PUT', url: `${url}/3` }).request.body).toEqual(culto);
        expect(http.expectOne({ method: 'PATCH', url: `${url}/3/estado` }).request.body).toEqual({ activo: false });
    });
});
