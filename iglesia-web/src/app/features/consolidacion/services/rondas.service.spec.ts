import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { FILTRO_CASOS_RONDA_INICIAL, FILTRO_LOTE_INICIAL, FILTRO_RONDAS_INICIAL } from '../models/consolidacion';
import { RondasService } from './rondas.service';

describe('RondasService', () => {
    let servicio: RondasService;
    let http: HttpTestingController;
    const url = `${environment.apiUrl}/consolidacion/rondas`;
    const PAGINA_VACIA = { content: [], page: { size: 10, number: 0, totalElements: 0, totalPages: 0 } };

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(RondasService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista las rondas enviando solo los filtros con valor', () => {
        servicio.listar({ ...FILTRO_RONDAS_INICIAL, estado: 'EN_CURSO', q: ' nov ' }, 1, 20).subscribe();
        const pedido = http.expectOne((req) => req.url === url);
        expect(pedido.request.params.get('estado')).toBe('EN_CURSO');
        expect(pedido.request.params.get('q')).toBe('nov');
        expect(pedido.request.params.get('page')).toBe('1');
        pedido.flush(PAGINA_VACIA);
        servicio.listar(FILTRO_RONDAS_INICIAL, 0, 10).subscribe();
        const sinFiltros = http.expectOne((req) => req.url === url);
        expect(sinFiltros.request.params.keys()).toEqual(['page', 'size']);
        sinFiltros.flush(PAGINA_VACIA);
    });

    it('cambia el estado de la ronda y la borra', () => {
        servicio.iniciar(7).subscribe();
        expect(http.expectOne(`${url}/7/iniciar`).request.method).toBe('POST');
        servicio.cerrar(7).subscribe();
        expect(http.expectOne(`${url}/7/cerrar`).request.method).toBe('POST');
        servicio.borrar(7).subscribe();
        expect(http.expectOne(`${url}/7`).request.method).toBe('DELETE');
    });

    it('pide la vista previa del lote y lo agrega con el mismo filtro', () => {
        const filtro = { ...FILTRO_LOTE_INICIAL, condicion: 'NUEVO' as const };
        servicio.vistaPreviaLote(7, filtro).subscribe();
        expect(http.expectOne(`${url}/7/lote/vista-previa`).request.body).toEqual(filtro);
        servicio.agregarLote(7, filtro).subscribe();
        expect(http.expectOne(`${url}/7/lote`).request.body).toEqual(filtro);
    });

    it('pide los casos de la ronda con sus filtros', () => {
        servicio.casos(7, { ...FILTRO_CASOS_RONDA_INICIAL, consolidadorId: 25, sinAsignar: true }, 0, 10).subscribe();
        const pedido = http.expectOne((req) => req.url === `${url}/7/casos`);
        expect(pedido.request.params.get('consolidadorId')).toBe('25');
        expect(pedido.request.params.get('sinAsignar')).toBe('true');
        expect(pedido.request.params.has('estado')).toBeFalse();
        pedido.flush(PAGINA_VACIA);
    });

    it('propone y guarda el reparto, quita casos y pide el avance', () => {
        const solicitud = { consolidadorIds: [25], soloSinAsignar: true };
        servicio.vistaPreviaReparto(7, solicitud).subscribe();
        expect(http.expectOne(`${url}/7/repartir/vista-previa`).request.body).toEqual(solicitud);
        servicio.repartir(7, solicitud).subscribe();
        expect(http.expectOne(`${url}/7/repartir`).request.method).toBe('POST');
        servicio.quitarCaso(7, 310).subscribe();
        expect(http.expectOne(`${url}/7/casos/310`).request.method).toBe('DELETE');
        servicio.avance(7).subscribe();
        http.expectOne(`${url}/7/avance`);
    });
});
