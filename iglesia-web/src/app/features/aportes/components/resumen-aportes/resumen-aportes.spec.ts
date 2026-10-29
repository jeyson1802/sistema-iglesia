import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MedioAporte } from '../../models/aporte';
import { RESUMEN } from '../../testing/aportes-prueba';
import { ResumenAportesComponent } from './resumen-aportes';

describe('ResumenAportesComponent', () => {
    let fixture: ComponentFixture<ResumenAportesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [ResumenAportesComponent] }).compileComponents();
        fixture = TestBed.createComponent(ResumenAportesComponent);
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance as unknown as Record<string, (...args: unknown[]) => unknown>;

    it('muestra el total, la variación con flecha y texto, la cantidad y el promedio', async () => {
        fixture.componentRef.setInput('resumen', RESUMEN);
        await fixture.whenStable();

        expect(html().textContent).toContain('S/\u00a0300.00');
        expect(html().textContent).toContain('50% más');
        expect(html().querySelector('.pi-arrow-up')).not.toBeNull();
        expect(html().textContent).toContain('S/\u00a075.00');
        expect(html().querySelectorAll('p-chart').length).toBe(2);
    });

    it('una baja se muestra en rojo con flecha hacia abajo', async () => {
        fixture.componentRef.setInput('resumen', { ...RESUMEN, total: 100 });
        await fixture.whenStable();

        expect(html().textContent).toContain('50% menos');
        expect(html().querySelector('.pi-arrow-down')).not.toBeNull();
    });

    it('sin aportes no dibuja gráficos y mientras carga muestra esqueletos', async () => {
        fixture.componentRef.setInput('resumen', { ...RESUMEN, total: 0, cantidad: 0, porCategoria: [], porMedio: [] });
        await fixture.whenStable();
        expect(html().querySelector('p-chart')).toBeNull();

        fixture.componentRef.setInput('cargando', true);
        await fixture.whenStable();
        expect(html().querySelector('p-skeleton')).not.toBeNull();
    });

    it('al tocar una porción o una barra pide filtrar por ella', async () => {
        fixture.componentRef.setInput('resumen', RESUMEN);
        await fixture.whenStable();
        const categorias: number[] = [];
        const medios: MedioAporte[] = [];
        fixture.componentInstance.filtrarCategoria.subscribe((id) => categorias.push(id));
        fixture.componentInstance.filtrarMedio.subscribe((medio) => medios.push(medio));

        componente()['elegirCategoria']({ element: { index: 1 } });
        componente()['elegirMedio']({ element: { index: 1 } });

        expect(categorias).toEqual([2]);
        expect(medios).toEqual(['YAPE']);
    });
});
