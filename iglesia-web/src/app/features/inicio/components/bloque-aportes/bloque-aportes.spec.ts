import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { aportesPrueba } from '../../testing/estadisticas-prueba';
import { BloqueAportesComponent } from './bloque-aportes';

describe('BloqueAportesComponent', () => {
    let fixture: ComponentFixture<BloqueAportesComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideRouter([])] });
        fixture = TestBed.createComponent(BloqueAportesComponent);
    });

    it('muestra el total en la moneda de la iglesia, su variación y el desglose', async () => {
        fixture.componentRef.setInput('datos', aportesPrueba());
        await fixture.whenStable();
        const texto = html().textContent!.replace(/\s+/g, '');
        expect(texto).toContain('S/1,250.00');
        expect(texto).toContain('+25%');
        expect(texto).toContain('Diezmo');
        expect(texto).toContain('Yape');
    });

    it('sin aportes lo explica', async () => {
        fixture.componentRef.setInput('datos', aportesPrueba({ resumen: { ...aportesPrueba().resumen, cantidad: 0, total: 0, porCategoria: [], porMedio: [] } }));
        await fixture.whenStable();
        expect(html().textContent!.replace(/\s+/g, '')).toContain('Nohayaportesregistradosenelperiodo.');
    });
});
