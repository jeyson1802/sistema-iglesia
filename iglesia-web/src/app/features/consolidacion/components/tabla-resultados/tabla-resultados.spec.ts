import { ComponentFixture, TestBed } from '@angular/core/testing';
import { resultadoPrueba } from '../../testing/datos-prueba';
import { TablaResultadosComponent } from './tabla-resultados';

describe('TablaResultadosComponent', () => {
    let fixture: ComponentFixture<TablaResultadosComponent>;
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement => html().querySelector(`button[aria-label="${etiqueta}"]`) as HTMLButtonElement;

    beforeEach(async () => {
        fixture = TestBed.createComponent(TablaResultadosComponent);
        fixture.componentRef.setInput('resultados', [resultadoPrueba(1, 'Se integró a un grupo'), resultadoPrueba(2, 'Ya no asiste', { activo: false })]);
        await fixture.whenStable();
    });

    it('muestra los resultados en orden con su estado', () => {
        const filas = Array.from(html().querySelectorAll('tbody tr')).map((fila) => fila.textContent);
        expect(filas[0]).toContain('Se integró a un grupo');
        expect(filas[1]).toContain('Inactivo');
    });

    it('no deja subir el primero ni bajar el último y emite las acciones', () => {
        expect(boton('Subir Se integró a un grupo').disabled).toBeTrue();
        expect(boton('Bajar Ya no asiste').disabled).toBeTrue();
        const emitidos: string[] = [];
        fixture.componentInstance.bajar.subscribe(({ id }) => emitidos.push(`bajar ${id}`));
        fixture.componentInstance.cambiarEstado.subscribe(({ id }) => emitidos.push(`estado ${id}`));
        boton('Bajar Se integró a un grupo').click();
        boton('Reactivar Ya no asiste').click();
        expect(emitidos).toEqual(['bajar 1', 'estado 2']);
    });

    it('muestra siempre el paginador', () => {
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
