import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GraficoDistribucionComponent } from './grafico-distribucion';

describe('GraficoDistribucionComponent', () => {
    let fixture: ComponentFixture<GraficoDistribucionComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        fixture = TestBed.createComponent(GraficoDistribucionComponent);
        fixture.componentRef.setInput('descripcion', 'Miembros por condición');
    });

    it('muestra cada categoría con su cantidad y su parte del total', async () => {
        fixture.componentRef.setInput('partes', [
            { clave: 'NUEVO', etiqueta: 'Nuevo', valor: 30 },
            { clave: 'CONSOLIDADO', etiqueta: 'Consolidado', valor: 90 }
        ]);
        await fixture.whenStable();
        expect(html().querySelector('figcaption')?.textContent?.replace(/\s+/g, '')).toContain('Miembrosporcondición');
        const filas = Array.from(html().querySelectorAll('li')).map((li) => li.textContent!.replace(/\s+/g, ''));
        expect(filas).toEqual(['Nuevo30·25%', 'Consolidado90·75%']);
    });

    it('usa el formato indicado y avisa cuando no hay datos', async () => {
        fixture.componentRef.setInput('partes', [{ clave: 'A', etiqueta: 'A', valor: 0 }]);
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Aúnnohaydatos.');
        fixture.componentRef.setInput('partes', [{ clave: 'A', etiqueta: 'A', valor: 5 }]);
        fixture.componentRef.setInput('formato', (valor: number) => `S/ ${valor}`);
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('S/5');
    });
});
