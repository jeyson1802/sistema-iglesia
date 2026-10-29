import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { hoyIso } from '@/app/shared/utils/fecha';
import { CultosProgramadosService } from '../services/cultos-programados.service';
import { ReunionesService } from '../services/reuniones.service';
import { CULTO_HOY_POR_ABRIR, GRUPO_HOY_ABIERTO, paginaDe, REUNION_CULTO } from '../testing/asistencia-prueba';
import { AsistenciaInicioComponent } from './asistencia-inicio';

describe('AsistenciaInicioComponent', () => {
    let reuniones: jasmine.SpyObj<ReunionesService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<AsistenciaInicioComponent>;
    let router: Router;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [AsistenciaInicioComponent],
            providers: [
                provideRouter([]),
                { provide: ReunionesService, useValue: reuniones },
                { provide: CultosProgramadosService, useValue: { listar: () => of([]) } },
                { provide: GruposService, useValue: { listar: () => of([]) } },
                { provide: ConfiguracionIglesiaService, useValue: { nombresGrupos: signal({ nombreGrupoSingular: 'Célula', nombreGrupoPlural: 'Células' }) } },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        router = TestBed.inject(Router);
        spyOn(router, 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(AsistenciaInicioComponent);
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        permisos = new Set<Permiso>(['asistencia.registrar', 'asistencia.ver', 'asistencia.configurar']);
        reuniones = jasmine.createSpyObj<ReunionesService>('ReunionesService', ['hoy', 'abrir', 'historial', 'crearEvento']);
        reuniones.hoy.and.returnValue(of([CULTO_HOY_POR_ABRIR, GRUPO_HOY_ABIERTO]));
        reuniones.historial.and.returnValue(of(paginaDe([REUNION_CULTO], 45)));
    });

    it('muestra las reuniones de hoy como tarjetas y el historial', async () => {
        await crear();

        expect(html().querySelectorAll('app-tarjeta-reunion-hoy').length).toBe(2);
        expect(html().textContent).toContain('Historial');
        expect(html().textContent).toContain('Culto dominical');
        expect(html().querySelector('p-paginator')?.textContent).toContain('de 45');
    });

    it('una reunión por abrir se abre con la fecha de hoy y lleva a tomar asistencia', async () => {
        reuniones.abrir.and.returnValue(of({ ...REUNION_CULTO, id: 51 }));
        await crear();

        (html().querySelector('app-tarjeta-reunion-hoy button') as HTMLButtonElement).click();

        expect(reuniones.abrir).toHaveBeenCalledWith({ cultoProgramadoId: 1, grupoId: null, fecha: hoyIso() });
        expect(router.navigate).toHaveBeenCalledWith(['/asistencia', 'reuniones', 51]);
    });

    it('una reunión ya abierta se retoma sin volver a abrirla', async () => {
        await crear();

        (html().querySelectorAll('app-tarjeta-reunion-hoy button')[1] as HTMLButtonElement).click();

        expect(reuniones.abrir).not.toHaveBeenCalled();
        expect(router.navigate).toHaveBeenCalledWith(['/asistencia', 'reuniones', 70]);
    });

    it('sin reuniones hoy lo dice y ofrece crear un evento o configurar horarios', async () => {
        reuniones.hoy.and.returnValue(of([]));
        await crear();

        expect(html().textContent).toContain('Hoy no hay cultos ni reuniones programadas');
        expect(html().textContent).toContain('Configurar horarios de culto');
    });

    it('un ujier solo ve «Hoy»: sin historial', async () => {
        permisos = new Set<Permiso>(['asistencia.registrar']);
        await crear();

        expect(reuniones.historial).not.toHaveBeenCalled();
        expect(html().textContent).not.toContain('Historial');
        expect(html().textContent).not.toContain('Crear evento');
    });

    it('quien solo consulta ve el historial sin «Hoy»', async () => {
        permisos = new Set<Permiso>(['asistencia.ver']);
        await crear();

        expect(reuniones.hoy).not.toHaveBeenCalled();
        expect(html().querySelector('h1')?.textContent).toContain('Asistencia');
    });

    it('al tocar una fila del historial abre su detalle', async () => {
        await crear();

        (html().querySelector('tr[tabindex="0"]') as HTMLElement).click();

        expect(router.navigate).toHaveBeenCalledWith(['/asistencia', 'reuniones', 50, 'detalle']);
    });

    it('si falla la apertura lo avisa', async () => {
        reuniones.abrir.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404, error: { detail: 'Grupo no encontrado' } })));
        await crear();

        (html().querySelector('app-tarjeta-reunion-hoy button') as HTMLButtonElement).click();
        fixture.detectChanges();

        expect(router.navigate).not.toHaveBeenCalled();
    });
});
