import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { NivelesService } from './niveles.service';

describe('NivelesService', () => {
    const url = `${environment.apiUrl}/niveles`;
    const diacono = { id: 2, nombre: 'Diácono', orden: 2, activo: true };
    let servicio: NivelesService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(NivelesService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista los niveles', () => {
        servicio.listar().subscribe();

        expect(http.expectOne(url).request.method).toBe('GET');
    });

    it('crea y renombra con el nombre', () => {
        servicio.crear('Diácono').subscribe();
        servicio.renombrar(2, 'Diácono mayor').subscribe();

        const alta = http.expectOne({ method: 'POST', url });
        expect(alta.request.body).toEqual({ nombre: 'Diácono' });
        alta.flush(diacono);
        expect(http.expectOne({ method: 'PUT', url: `${url}/2` }).request.body).toEqual({ nombre: 'Diácono mayor' });
    });

    it('ordena enviando todos los ids', () => {
        servicio.ordenar([2, 1]).subscribe();

        expect(http.expectOne({ method: 'PUT', url: `${url}/orden` }).request.body).toEqual({ ids: [2, 1] });
    });

    it('cambia el estado', () => {
        servicio.cambiarEstado(2, false).subscribe();

        expect(http.expectOne({ method: 'PATCH', url: `${url}/2/estado` }).request.body).toEqual({ activo: false });
    });
});
