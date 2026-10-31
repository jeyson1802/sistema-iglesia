import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { FILTRO_SEGUIMIENTO_INICIAL } from '../models/seguimiento';
import { SeguimientoService } from './seguimiento.service';

describe('SeguimientoService', () => {
    let servicio: SeguimientoService;
    let http: HttpTestingController;
    const api = environment.apiUrl;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(SeguimientoService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide las ocasiones de un miembro con el estado si se filtra', () => {
        servicio.ocasiones(12, { estado: 'FALTO' }, 1, 20).subscribe();
        const pedido = http.expectOne((req) => req.url === `${api}/miembros/12/asistencia/ocasiones`);
        expect(pedido.request.params.get('estado')).toBe('FALTO');
        expect(pedido.request.params.get('page')).toBe('1');
        pedido.flush({ content: [], page: { size: 20, number: 1, totalElements: 0, totalPages: 0 } });
    });

    it('envía solo los filtros de la lista que tienen valor', () => {
        servicio.lista({ ...FILTRO_SEGUIMIENTO_INICIAL, alerta: 'NUEVO_SIN_VOLVER', q: ' ana ', grupoId: 4, todos: false }, 0, 10).subscribe();
        const pedido = http.expectOne((req) => req.url === `${api}/seguimiento-asistencia`);
        expect(pedido.request.params.get('alerta')).toBe('NUEVO_SIN_VOLVER');
        expect(pedido.request.params.get('q')).toBe('ana');
        expect(pedido.request.params.get('grupoId')).toBe('4');
        expect(pedido.request.params.has('todos')).toBeFalse();
        pedido.flush({ content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } });
    });

    it('los conteos usan el grupo y el nombre, no la alerta', () => {
        servicio.conteos({ ...FILTRO_SEGUIMIENTO_INICIAL, alerta: 'FALTAS_SEGUIDAS', todos: true }).subscribe();
        const pedido = http.expectOne((req) => req.url === `${api}/seguimiento-asistencia/conteos`);
        expect(pedido.request.params.keys()).toEqual([]);
        pedido.flush({ personas: 0, conAlertas: 0, faltasSeguidas: 0, nuevoSinVolver: 0, asistenciaEnCaida: 0 });
    });

    it('consulta el grupo, los servicios y las reglas, y guarda las reglas', () => {
        servicio.delGrupo(8).subscribe();
        http.expectOne(`${api}/grupos/8/seguimiento`).flush({ semanas: [], integrantes: [] });
        servicio.servicios().subscribe();
        http.expectOne(`${api}/seguimiento-asistencia/servicios`).flush({});
        servicio.reglas().subscribe();
        http.expectOne(`${api}/configuracion-seguimiento`).flush({ faltasSeguidas: 3, puntosCaida: 30, diasNuevo: 90 });
        servicio.guardarReglas({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 }).subscribe();
        const guardar = http.expectOne(`${api}/configuracion-seguimiento`);
        expect(guardar.request.method).toBe('PUT');
        guardar.flush({ faltasSeguidas: 4, puntosCaida: 30, diasNuevo: 90 });
    });
});
