import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { consolidacionPrueba } from '../../testing/estadisticas-prueba';
import { BloqueConsolidacionComponent } from './bloque-consolidacion';

describe('BloqueConsolidacionComponent', () => {
    let fixture: ComponentFixture<BloqueConsolidacionComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueConsolidacionComponent);
        fixture.componentRef.setInput('datos', consolidacionPrueba());
        await fixture.whenStable();
    });

    it('muestra casos, contactos y las distribuciones; sin cierres anteriores no inventa la variación', () => {
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Casosabiertos7');
        expect(texto).toContain('Contactadasquevolvieron2de6');
        expect(texto).toContain('Sindatosparacomparar');
        expect(texto).toContain('+50%');
        expect(html().querySelectorAll('app-grafico-distribucion').length).toBe(2);
    });
});
