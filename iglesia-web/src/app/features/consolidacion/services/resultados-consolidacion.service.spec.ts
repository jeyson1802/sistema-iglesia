import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { ResultadosConsolidacionService } from './resultados-consolidacion.service';

describe('ResultadosConsolidacionService', () => {
    let servicio: ResultadosConsolidacionService;
    let http: HttpTestingController;
    const url = `${environment.apiUrl}/consolidacion/resultados`;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(ResultadosConsolidacionService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista, crea, renombra, ordena y desactiva resultados', () => {
        servicio.listar().subscribe();
        http.expectOne(url).flush([]);
        servicio.crear('Volverá a asistir').subscribe();
        expect(http.expectOne(url).request.body).toEqual({ nombre: 'Volverá a asistir' });
        servicio.renombrar(2, 'Vendrá el domingo').subscribe();
        expect(http.expectOne(`${url}/2`).request.method).toBe('PUT');
        servicio.ordenar([3, 1, 2]).subscribe();
        expect(http.expectOne(`${url}/orden`).request.body).toEqual({ ids: [3, 1, 2] });
        servicio.cambiarEstado(2, false).subscribe();
        expect(http.expectOne(`${url}/2/estado`).request.body).toEqual({ activo: false });
    });
});
