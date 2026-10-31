import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { asistenciaPrueba } from '../../testing/estadisticas-prueba';
import { BloqueAsistenciaComponent } from './bloque-asistencia';

describe('BloqueAsistenciaComponent', () => {
    let fixture: ComponentFixture<BloqueAsistenciaComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueAsistenciaComponent);
    });

    it('muestra las cifras, la serie y las alertas por tipo', async () => {
        fixture.componentRef.setInput('datos', asistenciaPrueba());
        await fixture.whenStable();
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Asistentesporculto85.5');
        expect(texto).toContain('Nuevosquevolvieron4de6');
        expect(texto).toContain('5personasnecesitanseguimiento');
        expect(html().querySelectorAll('li').length).toBe(3);
        expect(html().querySelector('app-grafico-evolucion')).not.toBeNull();
    });

    it('sin cultos lo explica', async () => {
        fixture.componentRef.setInput('datos', asistenciaPrueba({ serie: [{ inicio: '2026-10-05', cultos: 0, asistentesPromedio: null }] }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Nohubocultosconasistenciaregistradaenelperiodo.');
    });
});
