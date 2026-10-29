import { ComponentFixture, TestBed } from '@angular/core/testing';
import { rangoDe } from '@/app/shared/utils/periodo';
import { FiltroReuniones } from '../../models/asistencia';
import { FiltrosReunionesComponent } from './filtros-reuniones';

describe('FiltrosReunionesComponent', () => {
    let fixture: ComponentFixture<FiltrosReunionesComponent>;
    let emitidos: FiltroReuniones[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FiltrosReunionesComponent] }).compileComponents();
        fixture = TestBed.createComponent(FiltrosReunionesComponent);
        fixture.componentRef.setInput('filtro', { ...rangoDe('MES'), tipo: null });
        emitidos = [];
        fixture.componentInstance.cambiar.subscribe((filtro) => emitidos.push(filtro));
        fixture.detectChanges();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('ofrece los atajos de período y marca el actual', async () => {
        await fixture.whenStable();
        fixture.detectChanges();

        expect(html().textContent).toContain('Semana');
        expect(html().textContent).toContain('Mes anterior');
        expect(html().querySelector('p-togglebutton.p-togglebutton-checked')?.textContent?.trim()).toBe('Mes');
    });

    it('al elegir un atajo emite su rango conservando el tipo', () => {
        const semana = Array.from(html().querySelectorAll<HTMLElement>('p-togglebutton')).find((boton) => boton.textContent?.trim() === 'Semana') as HTMLElement;

        semana.click();

        expect(emitidos).toEqual([{ ...rangoDe('SEMANA'), tipo: null }]);
    });

    it('«Personalizado» muestra el selector de rango', () => {
        fixture.componentRef.setInput('filtro', { desde: '2026-01-03', hasta: '2026-01-09', tipo: null });
        fixture.detectChanges();

        expect(html().querySelector('#historial-rango')).toBeTruthy();
    });
});
