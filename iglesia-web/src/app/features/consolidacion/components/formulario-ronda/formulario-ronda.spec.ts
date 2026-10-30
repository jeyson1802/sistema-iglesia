import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DatosRonda } from '../../models/consolidacion';
import { FormularioRondaComponent } from './formulario-ronda';

describe('FormularioRondaComponent', () => {
    let fixture: ComponentFixture<FormularioRondaComponent>;
    let enviados: DatosRonda[];
    const html = (): HTMLElement => fixture.nativeElement;
    const enviar = (): void => {
        html().querySelector('form')!.dispatchEvent(new Event('submit'));
    };

    beforeEach(async () => {
        fixture = TestBed.createComponent(FormularioRondaComponent);
        enviados = [];
        fixture.componentInstance.guardar.subscribe((datos) => enviados.push(datos));
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('sugiere un nombre con el mes y empieza hoy', () => {
        expect((html().querySelector('#ronda-nombre') as HTMLInputElement).value).toMatch(/^Consolidación /);
        enviar();
        expect(enviados.length).toBe(1);
        expect(enviados[0].fechaFin).toBeNull();
    });

    it('carga los datos al editar y los envía sin espacios sobrantes', async () => {
        fixture.componentRef.setInput('datos', { nombre: 'Noviembre', descripcion: '  Nuevos  ', fechaInicio: '2026-11-02', fechaFin: '2026-11-30' });
        fixture.detectChanges();
        await fixture.whenStable();
        enviar();
        expect(enviados[0]).toEqual({ nombre: 'Noviembre', descripcion: 'Nuevos', fechaInicio: '2026-11-02', fechaFin: '2026-11-30' });
    });

    it('no envía un nombre demasiado corto y explica cómo corregirlo', async () => {
        const nombre = html().querySelector('#ronda-nombre') as HTMLInputElement;
        nombre.value = 'ab';
        nombre.dispatchEvent(new Event('input'));
        enviar();
        fixture.detectChanges();
        await fixture.whenStable();
        expect(enviados.length).toBe(0);
        expect(html().textContent).toContain('Usa al menos 3 caracteres');
    });

    it('muestra junto al campo el error del servidor', async () => {
        fixture.componentRef.setInput('erroresCampo', { nombre: 'Ya existe una ronda con ese nombre' });
        fixture.detectChanges();
        await fixture.whenStable();
        expect(html().textContent).toContain('Ya existe una ronda con ese nombre');
    });
});
