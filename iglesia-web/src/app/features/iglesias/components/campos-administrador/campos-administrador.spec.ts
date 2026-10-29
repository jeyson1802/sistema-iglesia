import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NonNullableFormBuilder } from '@angular/forms';
import { CamposAdministradorComponent, crearGrupoAdministrador, GrupoAdministrador } from './campos-administrador';

@Component({
    imports: [CamposAdministradorComponent],
    template: '<app-campos-administrador [grupo]="grupo" idPrefijo="prueba" />'
})
class AnfitrionComponent {
    readonly grupo: GrupoAdministrador = crearGrupoAdministrador(TestBed.inject(NonNullableFormBuilder));
}

describe('CamposAdministradorComponent', () => {
    let fixture: ComponentFixture<AnfitrionComponent>;
    let grupo: GrupoAdministrador;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [AnfitrionComponent] }).compileComponents();
        fixture = TestBed.createComponent(AnfitrionComponent);
        grupo = fixture.componentInstance.grupo;
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const campo = (id: string): HTMLInputElement => html().querySelector(`#${id}`) as HTMLInputElement;

    const escribir = async (id: string, valor: string): Promise<void> => {
        campo(id).value = valor;
        campo(id).dispatchEvent(new Event('input'));
        campo(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    it('asocia cada etiqueta a su campo con el prefijo indicado y el autocomplete correcto', () => {
        expect(html().querySelector('label[for="prueba-correo"]')).not.toBeNull();
        expect(html().querySelector('label[for="prueba-contrasena"]')).not.toBeNull();
        expect(campo('prueba-correo').getAttribute('autocomplete')).toBe('off');
        expect(campo('prueba-contrasena').getAttribute('autocomplete')).toBe('new-password');
    });

    it('valida el correo: obligatorio, con formato y como máximo 254 caracteres', async () => {
        await escribir('prueba-correo', 'no-es-correo');
        expect(html().textContent).toContain('Escribe un correo válido');

        grupo.controls.correo.setValue(`${'a'.repeat(250)}@i.org`);
        expect(grupo.controls.correo.hasError('maxlength')).toBeTrue();

        await escribir('prueba-correo', '');
        expect(html().textContent).toContain('Escribe el correo del administrador');
    });

    it('valida la contraseña temporal entre 8 y 72 caracteres', async () => {
        await escribir('prueba-contrasena', 'Corta1');
        expect(html().textContent).toContain('Usa al menos 8 caracteres');

        grupo.controls.contrasenaTemporal.setValue('a'.repeat(73));
        expect(grupo.controls.contrasenaTemporal.hasError('maxlength')).toBeTrue();

        grupo.setValue({ correo: 'pastor@iglesia.org', contrasenaTemporal: 'Temporal123' });
        expect(grupo.valid).toBeTrue();
    });
});
