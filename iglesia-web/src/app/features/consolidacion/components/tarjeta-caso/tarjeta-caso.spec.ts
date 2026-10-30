import { ComponentFixture, TestBed } from '@angular/core/testing';
import { casoPrueba } from '../../testing/datos-prueba';
import { TarjetaCasoComponent } from './tarjeta-caso';

describe('TarjetaCasoComponent', () => {
    let fixture: ComponentFixture<TarjetaCasoComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(TarjetaCasoComponent);
        fixture.componentRef.setInput(
            'caso',
            casoPrueba(1, { persona: { ...casoPrueba(1).persona, nombreCompleto: 'Ana Quispe' }, iteracion: 2, anterior: { resultado: { id: 4, nombre: 'No le interesa por ahora', activo: true }, cerradoEn: '2026-08-20T15:00:00Z' } })
        );
        await refrescar();
    });

    it('muestra iniciales, etiquetas y el resultado de la iteración anterior', () => {
        expect(html().textContent).toContain('AQ');
        expect(html().textContent).toContain('Nuevo');
        expect(html().textContent).toContain('2.ª iteración');
        expect(html().textContent).toContain('Antes: No le interesa por ahora (ago 2026)');
    });

    it('ofrece llamar, escribir y registrar', () => {
        const registrar = jasmine.createSpy('registrar');
        fixture.componentInstance.registrar.subscribe(registrar);
        expect(html().querySelector('a[href^="tel:"]')).not.toBeNull();
        (html().querySelector('#registrar-1 button') as HTMLElement).click();
        expect(registrar).toHaveBeenCalled();
    });

    it('tocar el nombre abre el caso', () => {
        const ver = jasmine.createSpy('ver');
        fixture.componentInstance.ver.subscribe(ver);
        (html().querySelector('#caso-nombre-1') as HTMLElement).click();
        expect(ver).toHaveBeenCalled();
    });

    it('recuerda la llamada sin registrar y permite descartarla', async () => {
        fixture.componentRef.setInput('sinRegistrar', { casoId: 1, nombre: 'Ana', medio: 'LLAMADA' });
        await refrescar();
        expect(html().textContent).toContain('Llamada sin registrar');
        const descartar = jasmine.createSpy('descartar');
        fixture.componentInstance.descartar.subscribe(descartar);
        (Array.from(html().querySelectorAll('button')).find((boton) => boton.textContent?.includes('Descartar')) as HTMLElement).click();
        expect(descartar).toHaveBeenCalled();
    });
});
