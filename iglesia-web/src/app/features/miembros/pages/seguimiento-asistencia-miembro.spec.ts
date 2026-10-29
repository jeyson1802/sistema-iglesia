import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { Ocasion } from '@/app/features/asistencia/models/seguimiento';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { SeguimientoService } from '@/app/features/asistencia/services/seguimiento.service';
import { Miembro } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';
import { SeguimientoAsistenciaMiembroComponent } from './seguimiento-asistencia-miembro';

describe('SeguimientoAsistenciaMiembroComponent', () => {
    const RESUMEN: AsistenciaMiembro = {
        reunionesQueCuentan: 10,
        asistidas: 4,
        porcentaje: 40,
        porcentajeAnterior: 85,
        faltasSeguidas: 4,
        ultimaAsistencia: '2026-10-04',
        alertas: ['FALTAS_SEGUIDAS', 'ASISTENCIA_EN_CAIDA'],
        semanas: Array.from({ length: 12 }, (_, indice) => ({ inicio: `2026-08-${String(10 + indice).padStart(2, '0')}`, esperadas: 2, asistidas: indice % 2, porcentaje: indice % 2 ? 50 : 0 })),
        ultimas: []
    };
    const OCASIONES: Ocasion[] = [{ fecha: '2026-10-25', hora: null, tipo: 'CULTO', descripcion: 'Culto dominical', asistioA: null, estado: 'FALTO' }];
    let asistencias: jasmine.SpyObj<AsistenciasService>;
    let seguimiento: jasmine.SpyObj<SeguimientoService>;
    let miembros: jasmine.SpyObj<MiembrosService>;
    let permisos: Set<Permiso>;
    let fixture: ComponentFixture<SeguimientoAsistenciaMiembroComponent>;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [SeguimientoAsistenciaMiembroComponent],
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '12' }) } } },
                { provide: AsistenciasService, useValue: asistencias },
                { provide: SeguimientoService, useValue: seguimiento },
                { provide: MiembrosService, useValue: miembros },
                { provide: SesionService, useValue: { tienePermiso: (permiso: Permiso) => permisos.has(permiso) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(SeguimientoAsistenciaMiembroComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        permisos = new Set<Permiso>(['miembros.ver']);
        asistencias = jasmine.createSpyObj<AsistenciasService>('AsistenciasService', ['resumenMiembro']);
        asistencias.resumenMiembro.and.returnValue(of(RESUMEN));
        seguimiento = jasmine.createSpyObj<SeguimientoService>('SeguimientoService', ['ocasiones']);
        seguimiento.ocasiones.and.returnValue(of({ content: OCASIONES, page: { size: 10, number: 0, totalElements: 1, totalPages: 1 } }));
        miembros = jasmine.createSpyObj<MiembrosService>('MiembrosService', ['obtener']);
        miembros.obtener.and.returnValue(of({ nombres: 'Ana', apellidos: 'Quispe' } as Miembro));
    });

    it('muestra el nombre, los indicadores, las alertas, la evolución y el historial', async () => {
        await crear();

        expect(asistencias.resumenMiembro).toHaveBeenCalledWith(12);
        expect(html().textContent).toContain('Ana Quispe');
        expect(html().textContent).toContain('40 %');
        expect(html().textContent).toContain('4 faltas seguidas');
        expect(html().querySelector('app-grafico-evolucion')).not.toBeNull();
        expect(html().querySelector('app-tabla-ocasiones p-paginator')).not.toBeNull();
        expect(seguimiento.ocasiones).toHaveBeenCalledWith(12, { estado: null }, 0, 10);
    });

    it('al filtrar por «Faltó» vuelve a la primera página y pide solo las faltas', async () => {
        await crear();
        const opciones = html().querySelectorAll('p-selectbutton p-togglebutton');
        (opciones[2] as HTMLElement).click();
        fixture.detectChanges();

        expect(seguimiento.ocasiones).toHaveBeenCalledWith(12, { estado: 'FALTO' }, 0, 10);
    });

    it('un miembro fuera de su red muestra el aviso y sin miembros.ver no pide la ficha', async () => {
        permisos = new Set<Permiso>(['asistencia.ver']);
        asistencias.resumenMiembro.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
        await crear();

        expect(html().textContent).toContain('No encontramos a este miembro');
        expect(miembros.obtener).not.toHaveBeenCalled();
        expect(html().textContent).toContain('Seguimiento');
    });
});
