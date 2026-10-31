import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { gruposPrueba } from '../../testing/estadisticas-prueba';
import { BloqueGruposComponent } from './bloque-grupos';

describe('BloqueGruposComponent', () => {
    let fixture: ComponentFixture<BloqueGruposComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueGruposComponent);
        fixture.componentRef.setInput('datos', gruposPrueba());
        await fixture.whenStable();
    });

    it('muestra las cifras y los grupos ordenados con su asistencia', () => {
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Miembrosenungrupo14%');
        expect(texto).toContain('Asistenciaalosgrupos75%');
        const filas = Array.from(html().querySelectorAll('tbody tr')).map((fila) => fila.textContent!.replace(/\s+/g, ''));
        expect(filas).toEqual(['CéluladeAna9475%', 'CéluladeLuis80—']);
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('.p-paginator, p-paginator')).not.toBeNull();
    });
});
