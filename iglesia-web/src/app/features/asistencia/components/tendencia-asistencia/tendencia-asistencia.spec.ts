import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TendenciaAsistenciaComponent } from './tendencia-asistencia';

describe('TendenciaAsistenciaComponent', () => {
    let fixture: ComponentFixture<TendenciaAsistenciaComponent>;

    function con(actual: number | null, anterior: number | null): string {
        fixture.componentRef.setInput('actual', actual);
        fixture.componentRef.setInput('anterior', anterior);
        fixture.detectChanges();
        return fixture.nativeElement.textContent.trim();
    }

    beforeEach(() => {
        fixture = TestBed.createComponent(TendenciaAsistenciaComponent);
    });

    it('dice si sube, baja o se mantiene', () => {
        expect(con(80, 60)).toBe('Subiendo');
        expect(con(40, 75)).toBe('Bajando');
        expect(con(72, 70)).toBe('Estable');
    });

    it('sin uno de los porcentajes no muestra nada', () => {
        expect(con(80, null)).toBe('');
    });
});
