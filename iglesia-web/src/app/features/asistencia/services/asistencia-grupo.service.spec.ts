import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { AsistenciaGrupoService } from './asistencia-grupo.service';

describe('AsistenciaGrupoService', () => {
    let servicio: AsistenciaGrupoService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(AsistenciaGrupoService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide la página de reuniones del grupo', () => {
        servicio.reuniones(8, 2, 20).subscribe();
        const pedido = http.expectOne((req) => req.url === `${environment.apiUrl}/grupos/8/reuniones`);
        expect(pedido.request.params.get('page')).toBe('2');
        expect(pedido.request.params.get('size')).toBe('20');
        pedido.flush({ content: [], page: { size: 20, number: 2, totalElements: 0, totalPages: 0 } });
    });

    it('pide el detalle de una reunión del grupo', () => {
        servicio.detalle(8, 120).subscribe();
        http.expectOne(`${environment.apiUrl}/grupos/8/reuniones/120`).flush({});
    });
});
