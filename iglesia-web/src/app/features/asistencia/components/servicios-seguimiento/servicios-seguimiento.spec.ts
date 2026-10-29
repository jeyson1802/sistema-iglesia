import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiciosSeguimiento } from '../../models/seguimiento';
import { ServiciosSeguimientoComponent } from './servicios-seguimiento';

describe('ServiciosSeguimientoComponent', () => {
    const SERVICIOS: ServiciosSeguimiento = {
        semanas: ['2026-10-05', '2026-10-12', '2026-10-19', '2026-10-26'],
        cultos: [{ cultoProgramadoId: 3, nombre: 'Culto dominical', asistentes: [40, 50, 60, 10] }],
        personasEnCultos: [80, 90, 100, 20],
        presentesEnGrupos: [30, 30, 36, 5],
        porcentajeGrupos: [70, 80, 60, null]
    };
    let fixture: ComponentFixture<ServiciosSeguimientoComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(ServiciosSeguimientoComponent);
        fixture.componentRef.setInput('servicios', SERVICIOS);
        fixture.componentRef.setInput('nombreGrupos', 'Células');
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('dibuja los gráficos de cultos y de grupos', () => {
        expect(fixture.nativeElement.querySelectorAll('app-grafico-evolucion').length).toBe(2);
        expect(fixture.nativeElement.textContent).toContain('Células: porcentaje de asistencia');
    });

    it('resume cada servicio con su variación sin contar la semana actual', () => {
        const filas = Array.from(fixture.nativeElement.querySelectorAll('p-table tbody tr')).map((fila) => Array.from((fila as HTMLElement).querySelectorAll('td')).map((celda) => celda.textContent?.trim()));
        expect(filas[1]).toEqual(['Culto dominical', '50', '60', '60', '+33 %']);
        expect(filas[3]).toEqual(['Porcentaje de asistencia en células', '70 %', '80 %', '60 %', '-15 pts']);
    });

    it('muestra siempre el paginador', () => {
        expect(fixture.nativeElement.querySelector('p-table .p-paginator')).not.toBeNull();
    });
});
