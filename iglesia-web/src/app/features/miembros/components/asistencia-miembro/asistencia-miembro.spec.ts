import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { AsistenciaMiembroComponent } from './asistencia-miembro';

describe('AsistenciaMiembroComponent', () => {
    const SEMANAS = Array.from({ length: 12 }, (_, indice) => ({ inicio: `2026-08-${String(10 + indice).padStart(2, '0')}`, esperadas: 2, asistidas: 1, porcentaje: 50 }));
    const RESUMEN: AsistenciaMiembro = {
        reunionesQueCuentan: 10,
        asistidas: 8,
        porcentaje: 80,
        porcentajeAnterior: 50,
        faltasSeguidas: 3,
        ultimaAsistencia: null,
        alertas: ['FALTAS_SEGUIDAS'],
        semanas: SEMANAS,
        ultimas: [{ id: 50, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-10-25' }]
    };
    let servicio: jasmine.SpyObj<AsistenciasService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<AsistenciaMiembroComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [AsistenciaMiembroComponent],
            providers: [provideRouter([]), { provide: AsistenciasService, useValue: servicio }, { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }]
        }).compileComponents();
        fixture = TestBed.createComponent(AsistenciaMiembroComponent);
        fixture.componentRef.setInput('miembroId', 12);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        permisos = new Set<Permiso>(['asistencia.ver']);
        servicio = jasmine.createSpyObj<AsistenciasService>('AsistenciasService', ['resumenMiembro']);
        servicio.resumenMiembro.and.returnValue(of(RESUMEN));
    });

    it('muestra el porcentaje con su tendencia, las alertas, las 12 semanas y las últimas reuniones', async () => {
        await crear();

        expect(servicio.resumenMiembro).toHaveBeenCalledWith(12);
        expect(html().textContent).toContain('80 %');
        expect(html().textContent).toContain('Subiendo');
        expect(html().textContent).toContain('8 de 10 ocasiones');
        expect(html().textContent).toContain('3 faltas seguidas');
        expect(html().querySelectorAll('app-semanas-asistencia li').length).toBe(12);
        expect(html().querySelector('a')?.textContent).toContain('Culto dominical');
    });

    it('ofrece ver el seguimiento completo del miembro', async () => {
        await crear();

        expect(html().querySelector('header p-button')?.textContent).toContain('Ver seguimiento');
    });

    it('sin asistencias muestra el estado vacío', async () => {
        servicio.resumenMiembro.and.returnValue(of({ ...RESUMEN, reunionesQueCuentan: 0, asistidas: 0, porcentaje: null, porcentajeAnterior: null, faltasSeguidas: 0, alertas: [], ultimas: [] }));
        await crear();

        expect(html().textContent).toContain('Aún no tiene asistencias registradas.');
    });

    it('un líder con miembros.ver también la ve, pero sin enlace al detalle', async () => {
        permisos = new Set<Permiso>(['miembros.ver']);
        await crear();

        expect(html().textContent).toContain('Culto dominical');
        expect(html().querySelector('a')).toBeNull();
    });

    it('sin permisos no aparece ni pide nada', async () => {
        permisos = new Set<Permiso>();
        await crear();

        expect(servicio.resumenMiembro).not.toHaveBeenCalled();
        expect(html().textContent?.trim()).toBe('');
    });

    it('si falla ofrece reintentar', async () => {
        servicio.resumenMiembro.and.returnValue(throwError(() => new Error('caída')));
        await crear();

        expect(html().textContent).toContain('No se pudo cargar la asistencia.');
    });
});
