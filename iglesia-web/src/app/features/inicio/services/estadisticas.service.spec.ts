import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { EstadisticasService } from './estadisticas.service';

describe('EstadisticasService', () => {
    let servicio: EstadisticasService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(EstadisticasService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('pide las estadísticas de cada módulo con el periodo', () => {
        const rutas: [() => void, string][] = [
            [() => servicio.miembros({ periodo: 'MES' }).subscribe(), 'miembros'],
            [() => servicio.grupos({ periodo: 'MES' }).subscribe(), 'grupos'],
            [() => servicio.asistencia({ periodo: 'MES' }).subscribe(), 'asistencia'],
            [() => servicio.consolidacion({ periodo: 'MES' }).subscribe(), 'consolidacion'],
            [() => servicio.aportes({ periodo: 'MES' }).subscribe(), 'aportes'],
            [() => servicio.sistema({ periodo: 'MES' }).subscribe(), 'sistema']
        ];
        for (const [pedir, ruta] of rutas) {
            pedir();
            http.expectOne(`${environment.apiUrl}/estadisticas/${ruta}?periodo=MES`).flush({});
        }
    });

    it('pide el uso del sistema al backend de seguridad', () => {
        servicio.uso({ periodo: 'ANIO' }).subscribe();
        http.expectOne(`${environment.seguridadUrl}/estadisticas/uso?periodo=ANIO`).flush({});
    });

    it('con «Personalizado» envía el rango, y con un atajo no envía fechas', () => {
        servicio.miembros({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' }).subscribe();
        http.expectOne(`${environment.apiUrl}/estadisticas/miembros?periodo=PERSONALIZADO&desde=2026-09-01&hasta=2026-09-15`).flush({});
        servicio.miembros({ periodo: 'HOY', desde: '2026-09-01', hasta: '2026-09-15' }).subscribe();
        http.expectOne(`${environment.apiUrl}/estadisticas/miembros?periodo=HOY`).flush({});
    });

    it('pide la información propia sin enviar ningún identificador', () => {
        servicio.miPerfil().subscribe();
        servicio.miAsistencia().subscribe();
        servicio.miCodigoAsistencia().subscribe();
        http.expectOne(`${environment.apiUrl}/mi-perfil`).flush({});
        http.expectOne(`${environment.apiUrl}/mi-perfil/asistencia`).flush({});
        http.expectOne(`${environment.apiUrl}/mi-perfil/codigo-asistencia`).flush({});
    });
});
