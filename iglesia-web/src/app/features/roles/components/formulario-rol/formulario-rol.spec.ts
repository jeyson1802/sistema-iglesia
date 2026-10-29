import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuardarRol, RolAcceso } from '../../models/rol-acceso';
import { CATALOGO, ROLES_BASE, SECRETARIA } from '../../testing/roles-prueba';
import { FormularioRolComponent, ModoFormularioRol } from './formulario-rol';

describe('FormularioRolComponent', () => {
    let fixture: ComponentFixture<FormularioRolComponent>;
    let guardados: GuardarRol[];

    const abrir = async (modo: ModoFormularioRol, rol: RolAcceso | null = null): Promise<void> => {
        fixture.componentRef.setInput('modo', modo);
        fixture.componentRef.setInput('rol', rol);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioRolComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioRolComponent);
        fixture.componentRef.setInput('rolesBase', ROLES_BASE);
        fixture.componentRef.setInput('catalogo', CATALOGO);
        guardados = [];
        fixture.componentInstance.guardar.subscribe((datos) => guardados.push(datos));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    // p-dialog puede renderizar su contenido fuera del host: se busca en todo el documento.
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const casilla = (codigo: string): HTMLInputElement => document.getElementById(`permiso-${codigo}`) as HTMLInputElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta);
    const formulario = () => fixture.componentInstance['formulario'];

    const escribirNombre = async (nombre: string): Promise<void> => {
        const input = document.getElementById('rol-nombre') as HTMLInputElement;
        input.value = nombre;
        input.dispatchEvent(new Event('input'));
        input.dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    const elegirRolBase = async (id: number): Promise<void> => {
        formulario().controls.rolBaseId.setValue(id);
        await fixture.whenStable();
    };

    const marcar = async (codigo: string): Promise<void> => {
        casilla(codigo).click();
        casilla(codigo).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    describe('al crear', () => {
        beforeEach(() => abrir('crear'));

        it('muestra el título, el nombre y el selector de rol base', () => {
            expect(dialogo().textContent).toContain('Nuevo rol');
            expect(document.getElementById('rol-nombre')).not.toBeNull();
            expect(dialogo().textContent).toContain('Hereda de');
        });

        it('agrupa los permisos por módulo', () => {
            const texto = dialogo().textContent ?? '';

            expect(texto.indexOf('Miembros')).toBeLessThan(texto.indexOf('Roles'));
            expect(texto).toContain('Registrar miembros');
        });

        it('sin rol base elegido no se puede marcar ningún permiso', () => {
            expect(CATALOGO.every(({ codigo }) => casilla(codigo).disabled)).toBeTrue();
        });

        it('solo se pueden marcar los permisos del rol base elegido', async () => {
            await elegirRolBase(2);

            expect(CATALOGO.every(({ codigo }) => !casilla(codigo).disabled)).toBeTrue();

            await elegirRolBase(4);

            expect(CATALOGO.every(({ codigo }) => casilla(codigo).disabled)).toBeTrue();
        });

        it('al cambiar a un rol base con menos permisos se descartan los que ya no aplican', async () => {
            await elegirRolBase(2);
            await marcar('miembros.ver');
            expect(casilla('miembros.ver').checked).toBeTrue();

            await elegirRolBase(3);

            expect(casilla('miembros.ver').checked).toBeFalse();
            expect(formulario().getRawValue().permisos['miembros.ver']).toBeFalse();
        });

        it('el rol base no ofrece al administrador del sistema', () => {
            const opciones = fixture.componentInstance['opcionesRolBase']();

            expect(opciones.map((opcion) => opcion.etiqueta)).toEqual(['Administrador de iglesia', 'Líder', 'Miembro']);
        });

        it('sin nombre o sin permisos «Guardar» queda deshabilitado', async () => {
            expect(boton('Guardar')?.disabled).toBeTrue();

            await escribirNombre('Secretaría');
            await elegirRolBase(2);
            expect(boton('Guardar')?.disabled).toBeTrue();

            await marcar('miembros.ver');
            expect(boton('Guardar')?.disabled).toBeFalse();

            await escribirNombre('');
            expect(boton('Guardar')?.disabled).toBeTrue();
        });

        it('muestra junto al campo cómo corregir el nombre y los permisos', async () => {
            await escribirNombre('');
            await elegirRolBase(2);
            await marcar('miembros.ver');
            await marcar('miembros.ver');

            expect(dialogo().textContent).toContain('Escribe el nombre del rol');
            expect(dialogo().textContent).toContain('Marca al menos un permiso');
        });

        it('emite el nombre sin espacios, el rol base y los permisos en el orden del catálogo', async () => {
            await escribirNombre('  Secretaría  ');
            await elegirRolBase(2);
            await marcar('roles.ver');
            await marcar('miembros.crear');

            boton('Guardar')!.click();
            await fixture.whenStable();

            expect(guardados).toEqual([{ nombre: 'Secretaría', rolBaseId: 2, permisos: ['miembros.crear', 'roles.ver'] }]);
        });

        it('al reabrir el diálogo y elegir el mismo rol base vuelve a habilitar sus permisos', async () => {
            await elegirRolBase(2);
            fixture.componentInstance.visible.set(false);
            await fixture.whenStable();

            await abrir('crear');
            expect(casilla('miembros.ver').disabled).toBeTrue();
            await elegirRolBase(2);

            expect(casilla('miembros.ver').disabled).toBeFalse();
        });

        it('muestra el error del backend dentro del diálogo', async () => {
            fixture.componentRef.setInput('error', 'Ya existe un rol con el nombre «Secretaría»');
            await fixture.whenStable();

            expect(dialogo().textContent).toContain('Ya existe un rol con el nombre «Secretaría»');
        });

        it('mientras guarda deshabilita los botones', async () => {
            fixture.componentRef.setInput('guardando', true);
            await fixture.whenStable();

            expect(boton('Cancelar')?.disabled).toBeTrue();
        });
    });

    describe('al editar', () => {
        beforeEach(() => abrir('editar', SECRETARIA));

        it('carga el nombre y los permisos del rol, sin selector de rol base', () => {
            expect(dialogo().textContent).toContain('Editar «Secretaría»');
            expect((document.getElementById('rol-nombre') as HTMLInputElement).value).toBe('Secretaría');
            expect(dialogo().textContent).not.toContain('Hereda de');
            expect(casilla('miembros.ver').checked).toBeTrue();
            expect(casilla('miembros.editar').checked).toBeFalse();
        });

        it('permite los permisos del rol base del que hereda', () => {
            expect(casilla('roles.administrar').disabled).toBeFalse();
        });

        it('emite los cambios sin rol base', async () => {
            await escribirNombre('Secretaría general');
            await marcar('miembros.editar');

            boton('Guardar')!.click();
            await fixture.whenStable();

            expect(guardados).toEqual([{ nombre: 'Secretaría general', rolBaseId: null, permisos: ['miembros.ver', 'miembros.crear', 'miembros.editar'] }]);
        });
    });

    describe('al ver un rol base', () => {
        beforeEach(() => abrir('ver', ROLES_BASE[0]));

        it('muestra sus permisos marcados y sin poder cambiarlos', () => {
            expect(dialogo().textContent).toContain('Permisos de «Administrador del sistema»');
            expect(casilla('usuarios.ver').checked).toBeTrue();
            expect(casilla('miembros.ver').checked).toBeFalse();
            expect(CATALOGO.every(({ codigo }) => casilla(codigo).disabled)).toBeTrue();
        });

        it('no ofrece editar el nombre ni guardar, solo cerrar', () => {
            expect(document.getElementById('rol-nombre')).toBeNull();
            expect(boton('Guardar')).toBeUndefined();

            boton('Cerrar')!.click();

            expect(fixture.componentInstance.visible()).toBeFalse();
        });

        it('al pasar después a editar, el formulario vuelve a estar habilitado', async () => {
            fixture.componentInstance.visible.set(false);
            await fixture.whenStable();

            await abrir('editar', SECRETARIA);

            expect((document.getElementById('rol-nombre') as HTMLInputElement).disabled).toBeFalse();
            expect(casilla('miembros.ver').disabled).toBeFalse();
        });
    });
});
