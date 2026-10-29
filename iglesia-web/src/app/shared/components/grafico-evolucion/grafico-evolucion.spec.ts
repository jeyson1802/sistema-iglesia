import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GraficoEvolucionComponent } from './grafico-evolucion';

describe('GraficoEvolucionComponent', () => {
    let fixture: ComponentFixture<GraficoEvolucionComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(GraficoEvolucionComponent);
        fixture.componentRef.setInput('etiquetas', ['5 oct', '12 oct']);
        fixture.componentRef.setInput('series', [
            { etiqueta: 'Asistió', valores: [3, null], tono: 'exito' },
            { etiqueta: 'Faltó', valores: [1, 2], tono: 'peligro' }
        ]);
        fixture.componentRef.setInput('descripcion', 'Asistencia por semana');
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('dibuja el gráfico', () => {
        expect(fixture.nativeElement.querySelector('p-chart canvas')).not.toBeNull();
    });

    it('ofrece una tabla accesible con los mismos datos y un guion donde no hay dato', () => {
        const tabla = fixture.nativeElement.querySelector('table') as HTMLTableElement;
        expect(tabla.caption?.textContent).toContain('Asistencia por semana');
        expect(tabla.tBodies[0].rows.length).toBe(2);
        expect(tabla.tBodies[0].rows[1].textContent).toContain('—');
    });

    it('muestra los porcentajes con su signo', () => {
        fixture.componentRef.setInput('porcentaje', true);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('tbody td').textContent).toContain('3 %');
    });
});
