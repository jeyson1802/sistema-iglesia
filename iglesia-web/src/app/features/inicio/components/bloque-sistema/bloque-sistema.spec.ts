import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { sistemaPrueba } from '../../testing/estadisticas-prueba';
import { BloqueSistemaComponent } from './bloque-sistema';

describe('BloqueSistemaComponent', () => {
    let fixture: ComponentFixture<BloqueSistemaComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueSistemaComponent);
        fixture.componentRef.setInput('datos', sistemaPrueba());
        await fixture.whenStable();
    });

    it('muestra el crecimiento, las distribuciones y las iglesias con más miembros', () => {
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Crecimientoneto22');
        expect(html().querySelectorAll('app-grafico-evolucion').length).toBe(2);
        expect(html().querySelector('p-table tbody')?.textContent?.replace(/\s+/g, '')).toContain('IglesiaCentral');
    });

    it('muestra siempre el paginador de las iglesias', () => {
        expect(html().querySelector('.p-paginator, p-paginator')).not.toBeNull();
    });
});
