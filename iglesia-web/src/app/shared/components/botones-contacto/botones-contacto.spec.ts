import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BotonesContactoComponent, MedioBoton } from './botones-contacto';

describe('BotonesContactoComponent', () => {
    let fixture: ComponentFixture<BotonesContactoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(async () => {
        fixture = TestBed.createComponent(BotonesContactoComponent);
        fixture.componentRef.setInput('nombre', 'Ana Quispe');
        fixture.componentRef.setInput('telefono', '987 654 321');
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('enlaza la llamada y WhatsApp con el número normalizado', () => {
        const enlaces = Array.from(html().querySelectorAll('a')).map((enlace) => enlace.getAttribute('href'));
        expect(enlaces).toEqual(['tel:+51987654321', 'https://wa.me/51987654321']);
        expect(html().querySelector('a')?.getAttribute('aria-label')).toBe('Llamar a Ana Quispe');
    });

    it('avisa qué medio se tocó', () => {
        const medios: MedioBoton[] = [];
        fixture.componentInstance.contactar.subscribe((medio) => medios.push(medio));
        html()
            .querySelectorAll('a')
            .forEach((enlace) => enlace.addEventListener('click', (evento) => evento.preventDefault()));
        html()
            .querySelectorAll('a')
            .forEach((enlace) => enlace.click());
        expect(medios).toEqual(['LLAMADA', 'WHATSAPP']);
    });

    it('sin teléfono sugiere coordinar una visita', async () => {
        fixture.componentRef.setInput('telefono', null);
        fixture.detectChanges();
        await fixture.whenStable();
        expect(html().querySelector('a')).toBeNull();
        expect(html().textContent).toContain('Sin teléfono: coordina una visita');
    });
});
