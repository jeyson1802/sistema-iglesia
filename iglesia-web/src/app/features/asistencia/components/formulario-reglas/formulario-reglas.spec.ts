import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { ReglasSeguimiento } from '../../models/seguimiento';
import { FormularioReglasComponent } from './formulario-reglas';

describe('FormularioReglasComponent', () => {
    const REGLAS: ReglasSeguimiento = { faltasSeguidas: 3, puntosCaida: 30, diasNuevo: 90 };
    let fixture: ComponentFixture<FormularioReglasComponent>;
    let guardadas: ReglasSeguimiento[];
    const formulario = (): FormGroup => (fixture.componentInstance as unknown as { formulario: FormGroup }).formulario;

    beforeEach(() => {
        fixture = TestBed.createComponent(FormularioReglasComponent);
        fixture.componentRef.setInput('reglas', REGLAS);
        guardadas = [];
        fixture.componentInstance.guardar.subscribe((reglas) => guardadas.push(reglas));
        fixture.detectChanges();
    });

    const enviar = () => (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit'));

    it('carga las reglas actuales y guarda las cambiadas', () => {
        expect(formulario().getRawValue()).toEqual(REGLAS);
        formulario().patchValue({ faltasSeguidas: 4 });
        enviar();
        expect(guardadas).toEqual([{ ...REGLAS, faltasSeguidas: 4 }]);
    });

    it('fuera de rango no guarda y dice cómo corregirlo', () => {
        formulario().patchValue({ puntosCaida: 90 });
        enviar();
        fixture.detectChanges();
        expect(guardadas).toEqual([]);
        expect(fixture.nativeElement.textContent).toContain('Escribe un número de 10 a 80 puntos');
    });

    it('sin cambios el botón de guardar está deshabilitado', () => {
        expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
    });
});
