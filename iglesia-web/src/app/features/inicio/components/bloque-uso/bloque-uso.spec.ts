import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { usoPrueba } from '../../testing/estadisticas-prueba';
import { BloqueUsoComponent } from './bloque-uso';

describe('BloqueUsoComponent', () => {
    let fixture: ComponentFixture<BloqueUsoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueUsoComponent);
    });

    it('en la iglesia muestra activos, cuentas y los activos por perfil', async () => {
        fixture.componentRef.setInput('datos', usoPrueba());
        await fixture.whenStable();
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('Activosenelperiodo12');
        expect(texto).toContain('Cuentashabilitadas20');
        expect(texto).toContain('Administradordeiglesia');
        expect(texto).not.toContain('Iglesiasconactividad');
    });

    it('en la vista global muestra las iglesias con actividad', async () => {
        fixture.componentRef.setInput('datos', usoPrueba({ global: true, iglesiasActivas: 3, iglesiasActivasAnterior: 2 }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Iglesiasconactividad');
    });

    it('sin actividad lo explica', async () => {
        fixture.componentRef.setInput('datos', usoPrueba({ activosPeriodo: 0 }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Nadieentróalsistemaenelperiodo.');
    });
});
