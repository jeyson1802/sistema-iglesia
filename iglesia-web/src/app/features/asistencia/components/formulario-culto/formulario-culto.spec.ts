import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { GuardarCulto } from '../../models/asistencia';
import { FormularioCultoComponent } from './formulario-culto';

describe('FormularioCultoComponent', () => {
    let fixture: ComponentFixture<FormularioCultoComponent>;
    let guardados: GuardarCulto[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioCultoComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioCultoComponent);
        guardados = [];
        fixture.componentInstance.guardar.subscribe((culto) => guardados.push(culto));
    });

    const abrir = async (): Promise<void> => {
        fixture.componentRef.setInput('visible', true);
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const formulario = (): FormGroup => (fixture.componentInstance as unknown as { formulario: FormGroup }).formulario;

    it('al crear propone el domingo a las 10:00 y emite el nombre limpio', async () => {
        await abrir();
        formulario().patchValue({ nombre: '  Culto dominical ' });

        (fixture.componentInstance as unknown as { enviar: () => void }).enviar();

        expect(guardados).toEqual([{ nombre: 'Culto dominical', diaSemana: 'DOMINGO', hora: '10:00' }]);
    });

    it('al editar carga el culto', async () => {
        fixture.componentRef.setInput('culto', { id: 2, nombre: 'Oración', diaSemana: 'MIERCOLES', hora: '19:30:00', activo: true });
        await abrir();

        expect(formulario().getRawValue().nombre).toBe('Oración');
        expect(formulario().getRawValue().diaSemana).toBe('MIERCOLES');
        expect(document.body.textContent).toContain('Editar «Oración»');
    });

    it('sin nombre no emite y explica cómo corregir', async () => {
        await abrir();

        (fixture.componentInstance as unknown as { enviar: () => void }).enviar();
        fixture.detectChanges();

        expect(guardados).toEqual([]);
        expect(document.body.textContent).toContain('Escribe el nombre del culto');
    });

    it('los días se ven con su inicial y su nombre completo para los lectores de pantalla', async () => {
        await abrir();

        expect(document.body.querySelector('p-selectbutton')?.textContent).toContain('Miércoles');
    });
});
