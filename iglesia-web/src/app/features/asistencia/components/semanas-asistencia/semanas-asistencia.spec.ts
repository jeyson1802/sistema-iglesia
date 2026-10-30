import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SemanasAsistenciaComponent } from './semanas-asistencia';

describe('SemanasAsistenciaComponent', () => {
    let fixture: ComponentFixture<SemanasAsistenciaComponent>;

    beforeEach(() => {
        fixture = TestBed.createComponent(SemanasAsistenciaComponent);
        fixture.componentRef.setInput('semanas', [
            { inicio: '2026-10-12', esperadas: 0, asistidas: 0, porcentaje: null },
            { inicio: '2026-10-19', esperadas: 2, asistidas: 1, porcentaje: 50 },
            { inicio: '2026-10-26', esperadas: 2, asistidas: 2, porcentaje: 100 }
        ]);
        fixture.detectChanges();
    });

    it('describe cada semana para los lectores de pantalla', () => {
        const casillas = Array.from(fixture.nativeElement.querySelectorAll('li')).map((li) => (li as HTMLElement).getAttribute('aria-label'));
        expect(casillas).toEqual(['Semana del 12 oct: sin reuniones', 'Semana del 19 oct: 1 de 2', 'Semana del 26 oct: 2 de 2']);
    });

    it('distingue la semana completa, la parcial y la sin reuniones', () => {
        const [sinReuniones, parcial, completa] = fixture.nativeElement.querySelectorAll('li');
        expect(sinReuniones.className).toContain('border-dashed');
        expect(parcial.className).toContain('bg-primary/60');
        expect(completa.className).toContain('bg-primary');
    });

    it('marca las semanas con contacto y lo dice en su texto', () => {
        fixture.componentRef.setInput('marcas', ['2026-10-21']);
        fixture.detectChanges();
        const casillas = Array.from(fixture.nativeElement.querySelectorAll('li')).map((li) => (li as HTMLElement).getAttribute('aria-label'));
        expect(casillas[1]).toBe('Semana del 19 oct: 1 de 2 · hubo contacto');
        expect(fixture.nativeElement.querySelectorAll('.pi-phone').length).toBe(1);
    });
});
