import { ComponentFixture, TestBed } from '@angular/core/testing';
import { resultadoPrueba } from '../../testing/datos-prueba';
import { CerrarCasoComponent, CierreCaso } from './cerrar-caso';

describe('CerrarCasoComponent', () => {
    let fixture: ComponentFixture<CerrarCasoComponent>;
    let cierres: CierreCaso[];
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const radios = (): HTMLButtonElement[] => Array.from(document.body.querySelectorAll('[aria-label="Resultado del seguimiento"] [role="radio"]'));

    beforeEach(async () => {
        fixture = TestBed.createComponent(CerrarCasoComponent);
        cierres = [];
        fixture.componentInstance.cerrar.subscribe((cierre) => cierres.push(cierre));
        fixture.componentRef.setInput('nombre', 'Ana Quispe');
        fixture.componentRef.setInput('resultados', [resultadoPrueba(1, 'Se integró a un grupo'), resultadoPrueba(2, 'Volverá a asistir'), resultadoPrueba(3, 'Ya no asiste', { activo: false })]);
        fixture.componentInstance.visible.set(true);
        await refrescar();
    });

    afterEach(() => fixture.destroy());

    it('ofrece solo los resultados activos, en orden', () => {
        expect(radios().map((radio) => radio.textContent!.trim())).toEqual(['Se integró a un grupo', 'Volverá a asistir']);
    });

    it('muestra el resumen y cierra con el resultado elegido', async () => {
        radios()[1].click();
        await refrescar();
        expect(document.body.textContent).toContain('Cerrarás el seguimiento de Ana con «Volverá a asistir».');
        document.body.querySelector('p-dialog form')!.dispatchEvent(new Event('submit'));
        expect(cierres).toEqual([{ resultadoId: 2, observacion: null }]);
    });

    it('sin resultado no cierra y explica qué falta', async () => {
        document.body.querySelector('p-dialog form')!.dispatchEvent(new Event('submit'));
        await refrescar();
        expect(cierres.length).toBe(0);
        expect(document.body.textContent).toContain('Elige cómo terminó el seguimiento.');
    });
});
