import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroup } from '@angular/forms';
import { AbrirReunion } from '../../models/asistencia';
import { AbrirOtraFechaComponent, OrigenReunion } from './abrir-otra-fecha';

describe('AbrirOtraFechaComponent', () => {
    const DOMINICAL: OrigenReunion = { clave: 'c1', etiqueta: 'Culto dominical', cultoProgramadoId: 1, grupoId: null };
    const CELULA: OrigenReunion = { clave: 'g8', etiqueta: 'Célula de Ana Torres', cultoProgramadoId: null, grupoId: 8 };
    let fixture: ComponentFixture<AbrirOtraFechaComponent>;
    let abiertas: AbrirReunion[];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [AbrirOtraFechaComponent] }).compileComponents();
        fixture = TestBed.createComponent(AbrirOtraFechaComponent);
        fixture.componentRef.setInput('origenes', [DOMINICAL, CELULA]);
        abiertas = [];
        fixture.componentInstance.abrir.subscribe((reunion) => abiertas.push(reunion));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    const formulario = (): FormGroup => (fixture.componentInstance as unknown as { formulario: FormGroup }).formulario;
    const enviar = (): void => (fixture.componentInstance as unknown as { enviar: () => void }).enviar();

    it('emite el grupo elegido con la fecha', () => {
        formulario().patchValue({ origen: CELULA, fecha: new Date(2026, 9, 22) });

        enviar();

        expect(abiertas).toEqual([{ cultoProgramadoId: null, grupoId: 8, fecha: '2026-10-22' }]);
    });

    it('sin elegir culto o grupo no emite y lo pide', () => {
        enviar();
        fixture.detectChanges();

        expect(abiertas).toEqual([]);
        expect(document.body.textContent).toContain('Elige el culto o el grupo');
    });
});
