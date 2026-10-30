import { ComponentFixture, TestBed } from '@angular/core/testing';
import { resultadoPrueba } from '../../testing/datos-prueba';
import { FormularioResultadoComponent } from './formulario-resultado';

describe('FormularioResultadoComponent', () => {
    let fixture: ComponentFixture<FormularioResultadoComponent>;
    let guardados: string[];
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const campo = (): HTMLInputElement => document.body.querySelector('#resultado-nombre') as HTMLInputElement;

    beforeEach(async () => {
        fixture = TestBed.createComponent(FormularioResultadoComponent);
        guardados = [];
        fixture.componentInstance.guardar.subscribe((nombre) => guardados.push(nombre));
        fixture.componentInstance.visible.set(true);
        await refrescar();
    });

    afterEach(() => fixture.destroy());

    it('crea un resultado con el nombre sin espacios sobrantes', () => {
        expect(document.body.textContent).toContain('Nuevo resultado');
        campo().value = '  Volverá a asistir ';
        campo().dispatchEvent(new Event('input'));
        document.body.querySelector('p-dialog form')!.dispatchEvent(new Event('submit'));
        expect(guardados).toEqual(['Volverá a asistir']);
    });

    it('al renombrar carga el nombre actual', async () => {
        fixture.componentInstance.visible.set(false);
        await refrescar();
        fixture.componentRef.setInput('resultado', resultadoPrueba(2, 'Volverá a asistir'));
        fixture.componentInstance.visible.set(true);
        await refrescar();
        expect(document.body.textContent).toContain('Renombrar «Volverá a asistir»');
        expect(campo().value).toBe('Volverá a asistir');
    });
});
