import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { EquipoConsolidacionService } from './equipo-consolidacion.service';

describe('EquipoConsolidacionService', () => {
    let servicio: EquipoConsolidacionService;
    let http: HttpTestingController;
    const url = `${environment.apiUrl}/consolidacion/equipo`;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(EquipoConsolidacionService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista, agrega y cambia el estado de un consolidador', () => {
        servicio.listar().subscribe();
        http.expectOne(url).flush([]);
        servicio.agregar(25).subscribe();
        expect(http.expectOne(url).request.body).toEqual({ miembroId: 25 });
        servicio.cambiarEstado(25, false).subscribe();
        const estado = http.expectOne(`${url}/25/estado`);
        expect(estado.request.method).toBe('PATCH');
        expect(estado.request.body).toEqual({ activo: false });
    });
});
