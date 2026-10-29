import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { hoyIso } from '@/app/shared/utils/fecha';
import { CrearEvento } from '../../models/asistencia';
import { FormularioEventoComponent } from './formulario-evento';

describe('FormularioEventoComponent', () => {
    let fixture: ComponentFixture<FormularioEventoComponent>;
    let creados: CrearEvento[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioEventoComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioEventoComponent);
        creados = [];
        fixture.componentInstance.guardar.subscribe((evento) => creados.push(evento));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    const formulario = (): FormGroup => (fixture.componentInstance as unknown as { formulario: FormGroup }).formulario;
    const enviar = (): void => (fixture.componentInstance as unknown as { enviar: () => void }).enviar();

    it('por defecto es de una sola fecha, hoy, sin hora', () => {
        formulario().patchValue({ nombre: ' Vigilia ' });

        enviar();

        expect(creados).toEqual([{ nombre: 'Vigilia', fechaInicio: hoyIso(), fechaFin: null, hora: null }]);
    });

    it('con varias fechas envía el fin y la hora', () => {
        formulario().patchValue({ nombre: 'Convención', fechaInicio: new Date(2026, 10, 6), unaSolaFecha: false, fechaFin: new Date(2026, 10, 8), hora: new Date(2026, 0, 1, 18, 0) });

        enviar();

        expect(creados).toEqual([{ nombre: 'Convención', fechaInicio: '2026-11-06', fechaFin: '2026-11-08', hora: '18:00' }]);
    });

    it('un evento de más de 7 días no se envía y explica el límite', () => {
        formulario().patchValue({ nombre: 'Retiro', fechaInicio: new Date(2026, 10, 1), unaSolaFecha: false, fechaFin: new Date(2026, 10, 8) });
        fixture.detectChanges();

        enviar();
        fixture.detectChanges();

        expect(creados).toEqual([]);
        expect(document.body.textContent).toContain('La fecha de fin debe estar entre la de inicio y 6 días después.');
    });
});
