import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { miembrosPrueba } from '../../testing/estadisticas-prueba';
import { BloqueMiembrosComponent } from './bloque-miembros';

describe('BloqueMiembrosComponent', () => {
    let fixture: ComponentFixture<BloqueMiembrosComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueMiembrosComponent);
    });

    it('muestra las cifras de la iglesia con su variación y las distribuciones', async () => {
        fixture.componentRef.setInput('datos', miembrosPrueba());
        await fixture.whenStable();
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Activos120');
        expect(texto).toContain('+50%');
        expect(texto).toContain('Singrupo·sinlíder');
        expect(html().querySelectorAll('app-grafico-distribucion').length).toBe(4);
        expect(html().querySelector('app-grafico-evolucion')).not.toBeNull();
    });

    it('en la red no habla de «sin líder»', async () => {
        fixture.componentRef.setInput('datos', miembrosPrueba({ red: true, sinLider: null, directos: 4 }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).not.toContain('sinlíder');
    });

    it('sin movimiento lo explica en lugar del gráfico', async () => {
        fixture.componentRef.setInput('datos', miembrosPrueba({ serie: [{ inicio: '2026-10-05', altas: 0, bajas: 0 }] }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Nohuboaltasnibajasenelperiodo.');
    });

    it('sin datos todavía muestra la carga', async () => {
        await fixture.whenStable();
        expect(html().querySelector('p-skeleton')).not.toBeNull();
    });
});
