import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { ReunionesService } from './reuniones.service';

describe('ReunionesService', () => {
    const url = `${environment.apiUrl}/reuniones`;
    let servicio: ReunionesService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(ReunionesService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide las de hoy, abre, consulta y crea o elimina eventos', () => {
        servicio.hoy().subscribe();
        servicio.abrir({ cultoProgramadoId: 1, grupoId: null, fecha: '2026-10-29' }).subscribe();
        servicio.obtener(50).subscribe();
        servicio.crearEvento({ nombre: 'Vigilia', fechaInicio: '2026-10-30', fechaFin: null, hora: null }).subscribe();
        servicio.eliminarEvento(60).subscribe();

        expect(http.expectOne({ method: 'GET', url: `${url}/hoy` })).toBeTruthy();
        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual({ cultoProgramadoId: 1, grupoId: null, fecha: '2026-10-29' });
        expect(http.expectOne({ method: 'GET', url: `${url}/50` })).toBeTruthy();
        expect(http.expectOne({ method: 'POST', url: `${url}/eventos` }).request.body.nombre).toBe('Vigilia');
        expect(http.expectOne({ method: 'DELETE', url: `${url}/60` })).toBeTruthy();
    });

    it('pide el historial paginado con el rango, el tipo y el orden', () => {
        servicio.historial({ desde: '2026-10-01', hasta: '2026-10-31', tipo: 'CULTO' }, 2, 20).subscribe();

        const pedido = http.expectOne((req) => req.url === url);
        expect(pedido.request.params.get('desde')).toBe('2026-10-01');
        expect(pedido.request.params.get('tipo')).toBe('CULTO');
        expect(pedido.request.params.get('page')).toBe('2');
        expect(pedido.request.params.get('size')).toBe('20');
        expect(pedido.request.params.getAll('sort')).toEqual(['fecha,desc', 'hora,desc']);
    });

    it('sin tipo no lo envía', () => {
        servicio.historial({ desde: '2026-10-01', hasta: '2026-10-31', tipo: null }, 0, 10).subscribe();

        expect(http.expectOne((req) => req.url === url).request.params.has('tipo')).toBeFalse();
    });
});
