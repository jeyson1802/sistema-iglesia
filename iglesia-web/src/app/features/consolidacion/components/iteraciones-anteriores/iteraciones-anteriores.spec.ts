import { ComponentFixture, TestBed } from '@angular/core/testing';
import { iteracionPrueba } from '../../testing/datos-prueba';
import { IteracionesAnterioresComponent } from './iteraciones-anteriores';

describe('IteracionesAnterioresComponent', () => {
    let fixture: ComponentFixture<IteracionesAnterioresComponent>;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(() => {
        fixture = TestBed.createComponent(IteracionesAnterioresComponent);
    });

    it('muestra cada iteración con su ronda, resultado y contactos', async () => {
        fixture.componentRef.setInput('iteraciones', [iteracionPrueba(205), iteracionPrueba(150, { ronda: null, resultado: null })]);
        await refrescar();
        const texto = fixture.nativeElement.textContent;
        expect(texto).toContain('Agosto');
        expect(texto).toContain('No le interesa por ahora');
        expect(texto).toContain('Seguimiento individual');
        expect(texto).toContain('No concluido');
    });

    it('sin iteraciones anteriores lo explica', async () => {
        fixture.componentRef.setInput('iteraciones', []);
        await refrescar();
        expect(fixture.nativeElement.textContent).toContain('Es su primer seguimiento');
    });
});
