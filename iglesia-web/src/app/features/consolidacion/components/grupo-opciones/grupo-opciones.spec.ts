import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { GrupoOpcionesComponent, OpcionGrupo } from './grupo-opciones';

@Component({
    imports: [ReactiveFormsModule, GrupoOpcionesComponent],
    template: `<app-grupo-opciones [formControl]="control" [opciones]="opciones" etiqueta="Medio" />`
})
class AnfitrionComponent {
    readonly control = new FormControl<string | null>(null);
    readonly opciones: OpcionGrupo<string>[] = [
        { valor: 'LLAMADA', etiqueta: 'Llamada', icono: 'pi pi-phone' },
        { valor: 'VISITA', etiqueta: 'Visita', icono: 'pi pi-home', severidad: 'success' },
        { valor: 'WHATSAPP', etiqueta: 'WhatsApp' }
    ];
}

describe('GrupoOpcionesComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    const radios = (): HTMLButtonElement[] => Array.from(fixture.nativeElement.querySelectorAll('[role="radio"]'));
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(AnfitrionComponent);
        await refrescar();
    });

    it('es un radiogroup con nombre y un solo botón en el orden de tabulación', () => {
        expect(fixture.nativeElement.querySelector('[role="radiogroup"]').getAttribute('aria-label')).toBe('Medio');
        expect(radios().map((radio) => radio.tabIndex)).toEqual([0, -1, -1]);
    });

    it('al hacer clic elige la opción y la marca', async () => {
        radios()[1].click();
        await refrescar();
        expect(fixture.componentInstance.control.value).toBe('VISITA');
        expect(radios()[1].getAttribute('aria-checked')).toBe('true');
        expect(radios().map((radio) => radio.tabIndex)).toEqual([-1, 0, -1]);
    });

    it('las flechas mueven y eligen, dando la vuelta al final', async () => {
        fixture.componentInstance.control.setValue('WHATSAPP');
        await refrescar();
        radios()[2].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight' }));
        await refrescar();
        expect(fixture.componentInstance.control.value).toBe('LLAMADA');
        radios()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowUp' }));
        await refrescar();
        expect(fixture.componentInstance.control.value).toBe('WHATSAPP');
    });

    it('deshabilitado no cambia el valor', async () => {
        fixture.componentInstance.control.disable();
        await refrescar();
        radios()[0].click();
        expect(fixture.componentInstance.control.value).toBeNull();
    });
});
