import { TestBed } from '@angular/core/testing';
import { AyudaEquiposComponent } from './ayuda-equipos';

describe('AyudaEquiposComponent', () => {
    it('compara el celular con el lector 2D y advierte que el lector 1D no lee QR', async () => {
        await TestBed.configureTestingModule({ imports: [AyudaEquiposComponent] }).compileComponents();
        const fixture = TestBed.createComponent(AyudaEquiposComponent);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();

        const dialogo = document.querySelector('.p-dialog') as HTMLElement;
        expect(dialogo.textContent).toContain('Cámara del celular');
        expect(dialogo.textContent).toContain('Lector de códigos 2D');
        expect(dialogo.textContent).toContain('no leen QR');
        expect(dialogo.querySelectorAll('tbody tr').length).toBe(6);
        fixture.destroy();
    });
});
