import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { diaSemanaDe, DIAS_SEMANA } from '@/app/core/models/dia-semana';
import { hoyIso } from '@/app/shared/utils/fecha';
import { ReunionesService } from '../../services/reuniones.service';
import { SeguimientoService } from '../../services/seguimiento.service';
import { REUNION_GRUPO } from '../../testing/asistencia-prueba';
import { AsistenciaGrupoComponent, GrupoAsistencia } from './asistencia-grupo';
import { SeguimientoGrupo } from '../../models/seguimiento';

describe('AsistenciaGrupoComponent', () => {
    const HOY = diaSemanaDe(new Date());
    const OTRO_DIA = DIAS_SEMANA.find((dia) => dia !== HOY) ?? HOY;
    const GRUPO: GrupoAsistencia = { id: 8, nombre: 'Célula de Ana Torres', diaReunion: HOY, activo: true };
    const SEGUIMIENTO: SeguimientoGrupo = {
        semanas: [
            { inicio: '2026-10-19', reuniones: 1, asistentes: 6, porcentaje: 75 },
            { inicio: '2026-10-26', reuniones: 0, asistentes: 0, porcentaje: null }
        ],
        integrantes: [
            { miembroId: 3, nombreCompleto: 'Rosa Díaz', nivel: null, grupoId: 8, grupo: 'Célula de Ana Torres', lider: 'Ana Torres', porcentaje: 25, porcentajeAnterior: 80, faltasSeguidas: 4, ultimaAsistencia: null, alertas: ['FALTAS_SEGUIDAS'] }
        ]
    };
    let servicio: jasmine.SpyObj<ReunionesService>;
    let seguimiento: jasmine.SpyObj<SeguimientoService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<AsistenciaGrupoComponent>;
    let router: Router;

    const crear = async (grupo: GrupoAsistencia = GRUPO): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [AsistenciaGrupoComponent],
            providers: [provideRouter([]), { provide: ReunionesService, useValue: servicio }, { provide: SeguimientoService, useValue: seguimiento }, { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }]
        }).compileComponents();
        router = TestBed.inject(Router);
        spyOn(router, 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(AsistenciaGrupoComponent);
        fixture.componentRef.setInput('grupo', grupo);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (texto: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.includes(texto));

    beforeEach(() => {
        permisos = new Set<Permiso>(['asistencia.grupo']);
        servicio = jasmine.createSpyObj<ReunionesService>('ReunionesService', ['abrir']);
        seguimiento = jasmine.createSpyObj<SeguimientoService>('SeguimientoService', ['delGrupo']);
        seguimiento.delGrupo.and.returnValue(of(SEGUIMIENTO));
    });

    it('muestra la evolución semanal y a los integrantes por riesgo, con el paginador', async () => {
        await crear();

        expect(seguimiento.delGrupo).toHaveBeenCalledWith(8);
        expect(html().querySelector('app-grafico-evolucion')).not.toBeNull();
        expect(html().querySelector('app-tabla-seguimiento')?.textContent).toContain('Rosa Díaz');
        expect(html().querySelector('app-tabla-seguimiento')?.textContent).toContain('4 faltas seguidas');
        expect(html().querySelector('app-tabla-seguimiento p-paginator')).not.toBeNull();
    });

    it('sin reuniones en las 12 semanas lo dice en lugar del gráfico', async () => {
        seguimiento.delGrupo.and.returnValue(of({ ...SEGUIMIENTO, semanas: [{ inicio: '2026-10-26', reuniones: 0, asistentes: 0, porcentaje: null }] }));
        await crear();

        expect(html().querySelector('app-grafico-evolucion')).toBeNull();
        expect(html().textContent).toContain('Aún no hay reuniones con asistencia');
    });

    it('al elegir a un integrante abre su seguimiento si puede ver su ficha', async () => {
        permisos.add('miembros.ver');
        await crear();

        (html().querySelector('app-tabla-seguimiento p-table tbody tr') as HTMLElement).click();

        expect(router.navigate).toHaveBeenCalledWith(['/miembros', 3, 'asistencia']);
    });

    it('si hoy es su día ofrece tomar la asistencia de hoy y la abre', async () => {
        servicio.abrir.and.returnValue(of({ ...REUNION_GRUPO, id: 90 }));
        await crear();

        boton('Tomar asistencia de hoy')?.click();

        expect(servicio.abrir).toHaveBeenCalledWith({ cultoProgramadoId: null, grupoId: 8, fecha: hoyIso() });
        expect(router.navigate).toHaveBeenCalledWith(['/asistencia', 'reuniones', 90]);
    });

    it('otro día solo ofrece «de otra fecha»', async () => {
        await crear({ ...GRUPO, diaReunion: OTRO_DIA });

        expect(boton('Tomar asistencia de hoy')).toBeUndefined();
        expect(boton('Tomar asistencia de otra fecha')).toBeDefined();
    });

    it('sin permisos de asistencia no aparece', async () => {
        permisos = new Set<Permiso>(['grupos.ver']);
        await crear();

        expect(seguimiento.delGrupo).not.toHaveBeenCalled();
        expect(html().textContent?.trim()).toBe('');
    });
});
