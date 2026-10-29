import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { OpcionesMiembroService, OpcionMiembro } from './opciones-miembro.service';

describe('OpcionesMiembroService', () => {
    const url = `${environment.apiUrl}/miembros/opciones`;
    let servicio: OpcionesMiembroService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(OpcionesMiembroService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('busca por el texto sin espacios sobrantes y adapta el nombre', () => {
        let opciones: OpcionMiembro[] | undefined;

        servicio.buscar('  ana ').subscribe((o) => (opciones = o));

        http.expectOne(`${url}?q=ana`).flush([{ id: 3, nombreCompleto: 'Ana Ríos' }]);
        expect(opciones).toEqual([{ id: 3, nombre: 'Ana Ríos' }]);
    });

    it('sin texto no envía el filtro', () => {
        servicio.buscar(' ').subscribe();

        http.expectOne(url).flush([]);
    });
});
