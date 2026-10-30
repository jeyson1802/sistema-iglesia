import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';
import { iteracionPrueba, SIN_CONTACTOS } from '@/app/features/consolidacion/testing/datos-prueba';
import { MiembrosService } from '../services/miembros.service';
import { HistorialConsolidacionMiembroComponent } from './historial-consolidacion-miembro';

describe('HistorialConsolidacionMiembroComponent', () => {
    let fixture: ComponentFixture<HistorialConsolidacionMiembroComponent>;
    let casos: jasmine.SpyObj<CasosService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const crear = async (): Promise<void> => {
        fixture = TestBed.createComponent(HistorialConsolidacionMiembroComponent);
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(() => {
        casos = jasmine.createSpyObj<CasosService>('CasosService', ['historial', 'resumenDeMiembro']);
        casos.historial.and.returnValue(of({ content: [iteracionPrueba(1), iteracionPrueba(2)], page: { size: 10, number: 0, totalElements: 2, totalPages: 1 } }));
        casos.resumenDeMiembro.and.returnValue(
            of({ iteraciones: 2, contactos: { ...SIN_CONTACTOS, total: 3, llamadas: 3 }, ultimoResultado: null, ultimoResultadoEn: null, casoAbierto: null, fechasContactoRecientes: ['2026-10-21'], puedeIniciar: true })
        );
        TestBed.configureTestingModule({
            providers: [
                provideRouter([]),
                { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '40' }) } } },
                { provide: CasosService, useValue: casos },
                { provide: MiembrosService, useValue: { obtener: () => of({ nombres: 'Ana', apellidos: 'Quispe' }) } },
                { provide: AsistenciasService, useValue: { resumenMiembro: () => of({ semanas: [{ inicio: '2026-10-19', esperadas: 2, asistidas: 1, porcentaje: 50 }] }) } },
                { provide: SesionService, useValue: { tienePermiso: () => true } }
            ]
        });
    });

    it('muestra el resumen, la asistencia con contactos marcados y las iteraciones', async () => {
        await crear();
        expect(casos.historial).toHaveBeenCalledWith(40, 0, 10);
        expect(html().textContent).toContain('Ana Quispe');
        expect(html().textContent).toContain('2 iteraciones · 3 llamadas');
        expect(html().querySelectorAll('app-semanas-asistencia .pi-phone').length).toBe(1);
        expect(html().querySelectorAll('p-accordion-panel').length).toBe(2);
    });

    it('muestra siempre el paginador', async () => {
        await crear();
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });

    it('fuera de su ámbito lo explica', async () => {
        casos.historial.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
        await crear();
        expect(html().textContent).toContain('No encontramos a esta persona');
    });
});
