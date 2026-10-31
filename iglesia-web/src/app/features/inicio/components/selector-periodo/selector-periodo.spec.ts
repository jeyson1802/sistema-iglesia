import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConsultaPeriodo } from '../../models/estadisticas';
import { SelectorPeriodoComponent } from './selector-periodo';

describe('SelectorPeriodoComponent', () => {
    let fixture: ComponentFixture<SelectorPeriodoComponent>;

    const crear = async (periodo: ConsultaPeriodo) => {
        fixture = TestBed.createComponent(SelectorPeriodoComponent);
        fixture.componentRef.setInput('periodo', periodo);
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const botones = (): HTMLElement[] => Array.from(html().querySelectorAll('p-togglebutton'));
    const boton = (etiqueta: string): HTMLElement => botones().find((elemento) => elemento.textContent!.trim() === etiqueta)!;
    const interno = () => fixture.componentInstance as unknown as { elegirRango(fechas: Date[] | null): void };

    it('ofrece los mismos periodos que aportes como botones segmentados, con su título asociado', async () => {
        await crear({ periodo: 'MES' });
        expect(html().querySelector('[aria-labelledby="selector-periodo"]')).not.toBeNull();
        expect(html().querySelector('#selector-periodo')?.textContent?.trim()).toBe('Período');
        expect(botones().map((elemento) => elemento.textContent!.trim())).toEqual(['Hoy', 'Semana', 'Mes', 'Mes anterior', 'Año', 'Personalizado']);
        expect(boton('Mes').getAttribute('aria-pressed')).toBe('true');
        expect(html().querySelector('#selector-rango')).toBeNull();
    });

    it('al elegir un atajo cambia el periodo sin fechas', async () => {
        await crear({ periodo: 'MES' });
        boton('Mes anterior').click();
        await fixture.whenStable();
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'MES_ANTERIOR' });
    });

    it('«Personalizado» muestra el rango y solo cambia el periodo cuando tiene sus dos fechas', async () => {
        await crear({ periodo: 'MES' });
        boton('Personalizado').click();
        await fixture.whenStable();
        expect(html().querySelector('label[for="selector-rango"]')).not.toBeNull();
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'MES' });

        interno().elegirRango([new Date(2026, 8, 1)]);
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'MES' });
        interno().elegirRango([new Date(2026, 8, 1), new Date(2026, 8, 15)]);
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' });
    });

    it('rechaza un rango de más de 366 días con un mensaje junto al campo', async () => {
        await crear({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' });
        interno().elegirRango([new Date(2025, 0, 1), new Date(2026, 8, 15)]);
        await fixture.whenStable();
        expect(html().querySelector('#selector-rango-error')?.textContent).toContain('hasta 366 días');
        expect(fixture.componentInstance.periodo()).toEqual({ periodo: 'PERSONALIZADO', desde: '2026-09-01', hasta: '2026-09-15' });
    });
});
