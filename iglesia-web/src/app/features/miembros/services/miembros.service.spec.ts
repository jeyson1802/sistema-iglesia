import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { FILTRO_MIEMBROS_INICIAL, GuardarMiembro, Miembro, MiembroResumen } from '../models/miembro';
import { RESPUESTA_JUAN, RESUMEN_JUAN, RESUMEN_PASTOR } from '../testing/miembros-prueba';
import { MiembrosService } from './miembros.service';

describe('MiembrosService', () => {
    const url = `${environment.apiUrl}/miembros`;
    let servicio: MiembrosService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(MiembrosService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('lista la página con orden y solo los filtros con valor', () => {
        servicio.listar({ ...FILTRO_MIEMBROS_INICIAL, q: ' juan ', nivelId: 3, ubigeo: '1501' }, 2).subscribe();

        const peticion = http.expectOne((r) => r.url === url);
        const parametros = peticion.request.params;
        expect(parametros.get('page')).toBe('2');
        expect(parametros.getAll('sort')).toEqual(['apellidos,asc', 'nombres,asc']);
        expect(parametros.get('q')).toBe('juan');
        expect(parametros.get('nivelId')).toBe('3');
        expect(parametros.get('ubigeo')).toBe('1501');
        expect(parametros.get('activo')).toBe('true');
        expect(parametros.has('condicion')).toBeFalse();
        expect(parametros.has('liderId')).toBeFalse();
        peticion.flush({ content: [], page: { size: 10, number: 2, totalElements: 0, totalPages: 0 } });
    });

    it('obtiene la ficha adaptada, la cadena y los liderados', () => {
        let miembro: Miembro | undefined;

        servicio.obtener(22).subscribe((m) => (miembro = m));
        servicio.cadena(22).subscribe();
        servicio.liderados(22).subscribe();

        http.expectOne(`${url}/22`).flush(RESPUESTA_JUAN);
        http.expectOne(`${url}/22/cadena`).flush([]);
        http.expectOne(`${url}/22/liderados`).flush([]);
        expect(miembro?.creadoEn).toEqual(jasmine.any(Date));
    });

    it('busca posibles duplicados por nombres y apellidos', () => {
        servicio.posiblesDuplicados('Juan', 'Pérez').subscribe();

        const peticion = http.expectOne((r) => r.url === `${url}/posibles-duplicados`);
        expect(peticion.request.params.get('nombres')).toBe('Juan');
        expect(peticion.request.params.get('apellidos')).toBe('Pérez');
        peticion.flush([]);
    });

    it('obtiene el pastor principal o nulo si no hay', () => {
        const pastores: (MiembroResumen | null)[] = [];

        servicio.pastorPrincipal().subscribe((p) => pastores.push(p));
        servicio.pastorPrincipal().subscribe((p) => pastores.push(p));

        const [con, sin] = http.match((r) => r.url === url && r.params.get('sort') === 'pastorPrincipal,desc');
        con.flush({ content: [RESUMEN_PASTOR], page: { size: 1, number: 0, totalElements: 9, totalPages: 9 } });
        sin.flush({ content: [RESUMEN_JUAN], page: { size: 1, number: 0, totalElements: 9, totalPages: 9 } });
        expect(pastores).toEqual([RESUMEN_PASTOR, null]);
    });

    it('crea, actualiza, cambia el estado y designa pastor', () => {
        const datos = { nombres: 'Rosa', apellidos: 'Flores' } as GuardarMiembro;

        servicio.crear(datos).subscribe();
        servicio.actualizar(22, datos).subscribe();
        servicio.cambiarEstado(22, false).subscribe();
        servicio.designarPastorPrincipal(22).subscribe();

        expect(http.expectOne({ method: 'POST', url }).request.body).toEqual(datos);
        expect(http.expectOne({ method: 'PUT', url: `${url}/22` }).request.body).toEqual(datos);
        expect(http.expectOne({ method: 'PATCH', url: `${url}/22/estado` }).request.body).toEqual({ activo: false });
        http.expectOne({ method: 'PUT', url: `${url}/22/pastor-principal` });
    });
});
