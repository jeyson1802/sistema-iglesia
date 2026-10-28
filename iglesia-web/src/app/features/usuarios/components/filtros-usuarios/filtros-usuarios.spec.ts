import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgControl } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Select } from 'primeng/select';
import { FiltroUsuarios } from '../../models/usuario-admin';
import { ESPERA_BUSQUEDA_MS, FiltrosUsuariosComponent, OpcionRolFiltro } from './filtros-usuarios';

describe('FiltrosUsuariosComponent', () => {
    let fixture: ComponentFixture<FiltrosUsuariosComponent>;
    let emitidos: FiltroUsuarios[];

    const ROLES: OpcionRolFiltro[] = [
        { id: 2, nombre: 'Administrador de iglesia' },
        { id: 10, nombre: 'Secretaría' }
    ];

    beforeEach(async () => {
        // `debounceTime` mide con `Date.now()`: el reloj simulado debe moverlo también.
        jasmine.clock().install().mockDate();
        await TestBed.configureTestingModule({ imports: [FiltrosUsuariosComponent] }).compileComponents();
        fixture = TestBed.createComponent(FiltrosUsuariosComponent);
        fixture.componentRef.setInput('roles', ROLES);
        await fixture.whenStable();
        emitidos = [];
        fixture.componentInstance.filtrar.subscribe((filtro) => emitidos.push(filtro));
    });

    afterEach(() => jasmine.clock().uninstall());

    const escribirCorreo = (texto: string): void => {
        const campo: HTMLInputElement = fixture.nativeElement.querySelector('#filtro-correo');
        campo.value = texto;
        campo.dispatchEvent(new Event('input'));
    };
    const selector = (inputId: string) => fixture.debugElement.queryAll(By.directive(Select)).find((select) => select.componentInstance.inputId === inputId)!;
    const elegir = (inputId: string, valor: boolean | number | null): void => selector(inputId).injector.get(NgControl).control!.setValue(valor);

    it('cada campo tiene su etiqueta asociada', () => {
        const etiquetas = Array.from<HTMLLabelElement>(fixture.nativeElement.querySelectorAll('label')).map((etiqueta) => etiqueta.htmlFor);

        expect(etiquetas).toEqual(['filtro-correo', 'filtro-estado', 'filtro-rol']);
    });

    it('ofrece los estados Todos, Activos e Inactivos', () => {
        expect(selector('filtro-estado').componentInstance.options.map((opcion: { etiqueta: string }) => opcion.etiqueta)).toEqual(['Todos', 'Activos', 'Inactivos']);
    });

    it('ofrece «Todos» y los roles recibidos', () => {
        expect(selector('filtro-rol').componentInstance.options).toEqual([
            { etiqueta: 'Todos', valor: null },
            { etiqueta: 'Administrador de iglesia', valor: 2 },
            { etiqueta: 'Secretaría', valor: 10 }
        ]);
    });

    it('busca por correo cuando se deja de escribir durante 300 ms', () => {
        escribirCorreo('an');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS - 1);
        escribirCorreo('ana');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS - 1);

        expect(emitidos).toEqual([]);

        jasmine.clock().tick(1);

        expect(emitidos).toEqual([{ correo: 'ana', activo: null, rolId: null }]);
    });

    it('quita los espacios del correo y no repite una búsqueda que no cambia', () => {
        escribirCorreo('ana');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);
        escribirCorreo(' ana ');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);

        expect(emitidos).toEqual([{ correo: 'ana', activo: null, rolId: null }]);
    });

    it('los selectores filtran en cuanto cambian, conservando el correo', () => {
        escribirCorreo('ana');
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);
        elegir('filtro-estado', false);
        elegir('filtro-rol', 10);
        elegir('filtro-estado', null);

        expect(emitidos).toEqual([
            { correo: 'ana', activo: null, rolId: null },
            { correo: 'ana', activo: false, rolId: null },
            { correo: 'ana', activo: false, rolId: 10 },
            { correo: 'ana', activo: null, rolId: 10 }
        ]);
    });

    it('un selector cambiado mientras se escribe no provoca una búsqueda repetida al terminar la espera', () => {
        escribirCorreo('ana');
        elegir('filtro-rol', 2);
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);

        expect(emitidos).toEqual([{ correo: 'ana', activo: null, rolId: 2 }]);
    });
});
