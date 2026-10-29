import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { Iglesia, IglesiaResponse } from '../models/iglesia';
import { IglesiasService } from './iglesias.service';

describe('IglesiasService', () => {
    const iglesiasUrl = `${environment.apiUrl}/iglesias`;
    const central: IglesiaResponse = { id: 3, nombre: 'Iglesia Central', creadaEn: '2026-10-28T15:30:00Z' };
    const centralAdaptada: Iglesia = { id: 3, nombre: 'Iglesia Central', creadaEn: new Date(Date.UTC(2026, 9, 28, 15, 30)) };

    let servicio: IglesiasService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(IglesiasService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista las iglesias adaptadas al modelo de la UI', () => {
        let iglesias: Iglesia[] | undefined;

        servicio.listar().subscribe((respuesta) => (iglesias = respuesta));

        const peticion = http.expectOne(iglesiasUrl);
        expect(peticion.request.method).toBe('GET');
        peticion.flush([central]);
        expect(iglesias).toEqual([centralAdaptada]);
    });

    it('obtiene una iglesia por su id', () => {
        let iglesia: Iglesia | undefined;

        servicio.obtener(3).subscribe((respuesta) => (iglesia = respuesta));

        const peticion = http.expectOne(`${iglesiasUrl}/3`);
        expect(peticion.request.method).toBe('GET');
        peticion.flush(central);
        expect(iglesia).toEqual(centralAdaptada);
    });

    it('da de alta una iglesia enviando solo su nombre', () => {
        let iglesia: Iglesia | undefined;

        servicio.crear('Iglesia Central').subscribe((respuesta) => (iglesia = respuesta));

        const peticion = http.expectOne(iglesiasUrl);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toEqual({ nombre: 'Iglesia Central' });
        peticion.flush(central, { status: 201, statusText: 'Created' });
        expect(iglesia).toEqual(centralAdaptada);
    });

    it('renombra una iglesia', () => {
        let iglesia: Iglesia | undefined;

        servicio.renombrar(3, 'Iglesia Central').subscribe((respuesta) => (iglesia = respuesta));

        const peticion = http.expectOne(`${iglesiasUrl}/3`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ nombre: 'Iglesia Central' });
        peticion.flush(central);
        expect(iglesia).toEqual(centralAdaptada);
    });
});
