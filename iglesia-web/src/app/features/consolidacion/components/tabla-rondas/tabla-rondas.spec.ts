import { ComponentFixture, TestBed } from '@angular/core/testing';
import { rondaPrueba } from '../../testing/datos-prueba';
import { TablaRondasComponent } from './tabla-rondas';

describe('TablaRondasComponent', () => {
    let fixture: ComponentFixture<TablaRondasComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        fixture = TestBed.createComponent(TablaRondasComponent);
        fixture.componentRef.setInput('rondas', [rondaPrueba(1), rondaPrueba(2, { estado: 'CERRADA' })]);
        fixture.componentRef.setInput('totalRegistros', 2);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra cada ronda con su estado, personas y avance', () => {
        const texto = html().querySelector('p-table')?.textContent ?? '';
        expect(texto).toContain('Ronda 1');
        expect(texto).toContain('En curso');
        expect(texto).toContain('Cerrada');
        expect(texto).toContain('20 %');
    });

    it('abre la ronda al hacer clic en la fila', () => {
        const visto = jasmine.createSpy('ver');
        fixture.componentInstance.ver.subscribe(visto);
        (html().querySelector('p-table tbody tr') as HTMLElement).click();
        expect(visto).toHaveBeenCalledWith(jasmine.objectContaining({ id: 1 }));
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });

    it('sin rondas lo dice y conserva el paginador', async () => {
        fixture.componentRef.setInput('rondas', []);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(html().textContent).toContain('No hay rondas con estos filtros.');
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
