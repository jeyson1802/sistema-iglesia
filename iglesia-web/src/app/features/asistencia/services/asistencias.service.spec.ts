import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { AsistenciasService } from './asistencias.service';

describe('AsistenciasService', () => {
    const reuniones = `${environment.apiUrl}/reuniones`;
    let servicio: AsistenciasService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(AsistenciasService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('marca con el código o a mano y quita', () => {
        servicio.marcarPorCodigo(50, 'ASIS:7K2QM9XDR4TB').subscribe();
        servicio.marcarMiembro(50, 12).subscribe();
        servicio.quitar(50, 12).subscribe();

        expect(http.expectOne({ method: 'POST', url: `${reuniones}/50/asistencias/codigo` }).request.body).toEqual({ codigo: 'ASIS:7K2QM9XDR4TB' });
        expect(http.expectOne({ method: 'POST', url: `${reuniones}/50/asistencias` }).request.body).toEqual({ miembroId: 12 });
        expect(http.expectOne({ method: 'DELETE', url: `${reuniones}/50/asistencias/12` })).toBeTruthy();
    });

    it('pide los asistentes paginados y filtra por nombre solo si hay texto', () => {
        servicio.asistentes(50, '  ana ', 1, 20).subscribe();
        servicio.asistentes(50, '', 0, 10).subscribe();

        const [conTexto, sinTexto] = http.match((req) => req.url === `${reuniones}/50/asistentes`);
        expect(conTexto.request.params.get('q')).toBe('ana');
        expect(conTexto.request.params.get('page')).toBe('1');
        expect(conTexto.request.params.get('sort')).toBe('registradaEn,desc');
        expect(sinTexto.request.params.has('q')).toBeFalse();
    });

    it('pide los integrantes, busca miembros y la asistencia de un miembro', () => {
        servicio.integrantes(70).subscribe();
        servicio.buscarMiembros(50, ' to ').subscribe();
        servicio.resumenMiembro(12).subscribe();

        expect(http.expectOne({ method: 'GET', url: `${reuniones}/70/integrantes` })).toBeTruthy();
        expect(http.expectOne((req) => req.url === `${reuniones}/50/miembros`).request.params.get('q')).toBe('to');
        expect(http.expectOne({ method: 'GET', url: `${environment.apiUrl}/miembros/12/asistencia` })).toBeTruthy();
    });
});
