import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { CasosService } from './casos.service';

describe('CasosService', () => {
    let servicio: CasosService;
    let http: HttpTestingController;
    const url = `${environment.apiUrl}/consolidacion`;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(CasosService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide «Mis asignados» por sección', () => {
        servicio.misCasos('PARA_HOY', ' ana ', 0, 10).subscribe();
        const pedido = http.expectOne((req) => req.url === `${url}/mis-casos`);
        expect(pedido.request.params.get('seccion')).toBe('PARA_HOY');
        expect(pedido.request.params.get('q')).toBe('ana');
        servicio.resumenMisCasos().subscribe();
        http.expectOne(`${url}/mis-casos/resumen`);
    });

    it('abre un seguimiento individual, lo cierra y lo reabre', () => {
        servicio.abrir(40).subscribe();
        expect(http.expectOne(`${url}/casos`).request.body).toEqual({ miembroId: 40, consolidadorId: null });
        servicio.cerrar(310, 2, null).subscribe();
        expect(http.expectOne(`${url}/casos/310/cerrar`).request.body).toEqual({ resultadoId: 2, observacion: null });
        servicio.reabrir(310).subscribe();
        expect(http.expectOne(`${url}/casos/310/reabrir`).request.method).toBe('POST');
    });

    it('registra, corrige y borra contactos', () => {
        const datos = { fechaHora: '2026-11-03T19:30:00Z', medio: 'LLAMADA' as const, resultado: 'SE_HABLO' as const, observaciones: null, proximoContacto: null };
        servicio.registrarContacto(310, datos).subscribe();
        expect(http.expectOne(`${url}/casos/310/contactos`).request.body).toEqual(datos);
        servicio.corregirContacto(900, datos).subscribe();
        expect(http.expectOne(`${url}/contactos/900`).request.method).toBe('PUT');
        servicio.borrarContacto(900).subscribe();
        expect(http.expectOne(`${url}/contactos/900`).request.method).toBe('DELETE');
    });

    it('reasigna uno o varios casos', () => {
        servicio.reasignar([310], 25).subscribe();
        expect(http.expectOne(`${url}/casos/310/consolidador`).request.body).toEqual({ consolidadorId: 25 });
        servicio.reasignar([310, 311], 25).subscribe();
        expect(http.expectOne(`${url}/casos/consolidador`).request.body).toEqual({ casoIds: [310, 311], consolidadorId: 25 });
    });

    it('pide el resumen y el historial de la persona', () => {
        servicio.resumenDeMiembro(40).subscribe();
        http.expectOne(`${environment.apiUrl}/miembros/40/consolidacion`);
        servicio.historial(40, 1, 10).subscribe();
        const pedido = http.expectOne((req) => req.url === `${environment.apiUrl}/miembros/40/consolidacion/historial`);
        expect(pedido.request.params.get('page')).toBe('1');
    });
});
