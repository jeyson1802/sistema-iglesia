import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { FiltroAportes, GuardarAporte } from '../models/aporte';
import { AportesService } from './aportes.service';

describe('AportesService', () => {
    const url = `${environment.apiUrl}/aportes`;
    const filtro: FiltroAportes = { desde: '2026-10-01', hasta: '2026-10-31', categoriaId: null, medio: 'YAPE', miembroId: 12, incluirAnulados: true };
    const datos: GuardarAporte = { fecha: '2026-10-25', miembroId: null, categoriaId: 2, monto: 20, medio: 'EFECTIVO', numeroOperacion: null, peticion: null, observacion: null };
    let servicio: AportesService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(AportesService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista con el rango, los filtros con valor, la página y el orden', () => {
        servicio.listar(filtro, 2).subscribe();

        const peticion = http.expectOne((req) => req.url === url);
        const params = peticion.request.params;
        expect(params.get('desde')).toBe('2026-10-01');
        expect(params.get('medio')).toBe('YAPE');
        expect(params.get('miembroId')).toBe('12');
        expect(params.has('categoriaId')).toBeFalse();
        expect(params.get('incluirAnulados')).toBe('true');
        expect(params.get('page')).toBe('2');
        expect(params.getAll('sort')).toEqual(['fecha,desc', 'registradoEn,desc']);
    });

    it('pide el resumen sin paginación ni anulados', () => {
        servicio.resumen(filtro).subscribe();

        const peticion = http.expectOne((req) => req.url === `${url}/resumen`);
        expect(peticion.request.params.has('page')).toBeFalse();
        expect(peticion.request.params.has('incluirAnulados')).toBeFalse();
    });

    it('obtiene, crea, corrige y anula', () => {
        servicio.obtener(5).subscribe();
        servicio.crear(datos).subscribe();
        servicio.actualizar(5, datos).subscribe();
        servicio.anular(5, 'Duplicado').subscribe();

        expect(http.expectOne({ method: 'GET', url: `${url}/5` })).toBeTruthy();
        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual(datos);
        expect(http.expectOne({ method: 'PUT', url: `${url}/5` }).request.body).toEqual(datos);
        expect(http.expectOne({ method: 'PATCH', url: `${url}/5/anulacion` }).request.body).toEqual({ motivo: 'Duplicado' });
    });
});
