import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MedidorRondaComponent } from './medidor-ronda';

describe('MedidorRondaComponent', () => {
    let fixture: ComponentFixture<MedidorRondaComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(MedidorRondaComponent);
        fixture.componentRef.setInput('avance', { personas: 10, pendientes: 5, enContacto: 3, cerrados: 2 });
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('describe el avance para los lectores de pantalla y muestra la leyenda', () => {
        const html: HTMLElement = fixture.nativeElement;
        expect(html.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('2 cerrados, 3 en contacto y 5 pendientes de 10 personas');
        expect(html.textContent).toContain('Cerrados 2');
        expect(html.textContent).toContain('Pendientes 5');
    });

    it('compacto no muestra la leyenda', async () => {
        fixture.componentRef.setInput('compacto', true);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(fixture.nativeElement.textContent).not.toContain('Cerrados');
    });
});
