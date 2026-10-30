import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatosContacto } from '../../models/consolidacion';
import { contactoPrueba } from '../../testing/datos-prueba';
import { HojaContactoComponent } from './hoja-contacto';

describe('HojaContactoComponent', () => {
    let fixture: ComponentFixture<HojaContactoComponent>;
    let guardados: DatosContacto[];
    const cuerpo = (): HTMLElement => document.body;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const radio = (texto: string): HTMLButtonElement => Array.from(cuerpo().querySelectorAll<HTMLButtonElement>('[role="radio"]')).find((boton) => boton.textContent?.includes(texto))!;
    const enviar = (): void => {
        cuerpo().querySelector('p-dialog form')!.dispatchEvent(new Event('submit'));
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(HojaContactoComponent);
        guardados = [];
        fixture.componentInstance.guardar.subscribe((datos) => guardados.push(datos));
        fixture.componentRef.setInput('nombre', 'Ana Quispe');
        fixture.componentRef.setInput('medioInicial', 'LLAMADA');
        fixture.componentInstance.visible.set(true);
        await refrescar();
    });

    afterEach(() => fixture.destroy());

    it('al volver de una llamada pregunta cómo le fue con el medio ya elegido', () => {
        expect(cuerpo().textContent).toContain('¿Cómo te fue con la llamada a Ana?');
        expect(radio('Llamada').getAttribute('aria-checked')).toBe('true');
    });

    it('solo ofrece los resultados del medio elegido', async () => {
        const textos = (): string[] => Array.from(cuerpo().querySelectorAll('[aria-label="Resultado del contacto"] [role="radio"]')).map((boton) => boton.textContent!.trim());
        expect(textos()).toEqual(['Hablamos', 'No contestó', 'Número equivocado']);
        radio('Visita').click();
        await refrescar();
        expect(textos()).toEqual(['Hablamos', 'No estaba', 'Dirección equivocada']);
    });

    it('no guarda sin resultado y avisa cómo corregirlo', async () => {
        enviar();
        await refrescar();
        expect(guardados.length).toBe(0);
        expect(cuerpo().textContent).toContain('Elige qué pasó en el contacto.');
    });

    it('guarda con el atajo de próximo contacto', async () => {
        radio('No contestó').click();
        await refrescar();
        const manana = Array.from(cuerpo().querySelectorAll<HTMLButtonElement>('p-button button')).find((boton) => boton.textContent?.includes('Mañana'))!;
        manana.click();
        await refrescar();
        enviar();
        expect(guardados.length).toBe(1);
        expect(guardados[0].medio).toBe('LLAMADA');
        expect(guardados[0].resultado).toBe('NO_RESPONDIO');
        expect(guardados[0].proximoContacto).not.toBeNull();
        expect(guardados[0].observaciones).toBeNull();
        expect('consolidadorId' in guardados[0]).toBeFalse();
    });

    it('si se habló con la persona ofrece cerrar el caso', async () => {
        fixture.componentRef.setInput('puedeCerrar', true);
        radio('Hablamos').click();
        await refrescar();
        expect(cuerpo().textContent).toContain('¿Terminó el seguimiento?');
    });

    it('al corregir carga el contacto existente', async () => {
        fixture.componentInstance.visible.set(false);
        await refrescar();
        fixture.componentRef.setInput('contacto', contactoPrueba(900, { medio: 'VISITA', resultado: 'NO_ESTABA', observaciones: 'Volver el sábado' }));
        fixture.componentInstance.visible.set(true);
        await refrescar();
        expect(cuerpo().textContent).toContain('Corregir contacto');
        expect(radio('No estaba').getAttribute('aria-checked')).toBe('true');
        expect((cuerpo().querySelector('#contacto-observaciones') as HTMLTextAreaElement).value).toBe('Volver el sábado');
    });
});
