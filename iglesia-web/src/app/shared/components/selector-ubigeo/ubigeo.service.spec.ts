import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { UbigeoService } from './ubigeo.service';

describe('UbigeoService', () => {
    const url = `${environment.apiUrl}/ubigeo`;
    let servicio: UbigeoService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(UbigeoService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide los departamentos una sola vez', () => {
        const recibidos: number[] = [];

        servicio.departamentos().subscribe((lista) => recibidos.push(lista.length));
        http.expectOne(`${url}/departamentos`).flush([{ codigo: '15', nombre: 'Lima' }]);
        servicio.departamentos().subscribe((lista) => recibidos.push(lista.length));

        http.expectNone(`${url}/departamentos`);
        expect(recibidos).toEqual([1, 1]);
    });

    it('pide provincias por departamento y distritos por provincia', () => {
        servicio.provincias('15').subscribe();
        servicio.distritos('1501').subscribe();

        http.expectOne(`${url}/departamentos/15/provincias`).flush([]);
        http.expectOne(`${url}/provincias/1501/distritos`).flush([]);
    });

    it('si la petición falla se puede reintentar', () => {
        servicio.departamentos().subscribe({ error: () => undefined });
        http.expectOne(`${url}/departamentos`).flush(null, { status: 500, statusText: 'Error' });

        servicio.departamentos().subscribe();

        http.expectOne(`${url}/departamentos`).flush([]);
    });
});
