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

        it('muestra el título, el nombre y el selector de alcance, que explica la página de inicio', () => {
            expect(dialogo().textContent).toContain('Nuevo rol');
            expect(document.getElementById('rol-nombre')).not.toBeNull();
            expect(dialogo().textContent).toContain('Alcance');
            expect(dialogo().textContent).toContain('qué página de inicio ve');
        });

        it('agrupa los permisos por módulo', () => {
            const texto = dialogo().textContent ?? '';

            expect(texto.indexOf('Miembros')).toBeLessThan(texto.indexOf('Roles'));
            expect(texto).toContain('Registrar miembros');
        });

        it('se puede marcar cualquier permiso de iglesia sin importar el rol base', async () => {
            const deIglesia = CATALOGO.filter(({ codigo }) => codigo !== 'iglesias.ver');
            expect(deIglesia.every(({ codigo }) => !casilla(codigo).disabled)).toBeTrue();

            await elegirRolBase(3);
            await marcar('miembros.crear');

            expect(casilla('miembros.crear').checked).toBeTrue();
            expect(deIglesia.every(({ codigo }) => !casilla(codigo).disabled)).toBeTrue();
        });

        it('cambiar el rol base no descarta los permisos marcados', async () => {
            await elegirRolBase(2);
            await marcar('miembros.ver');

            await elegirRolBase(4);

            expect(casilla('miembros.ver').checked).toBeTrue();
        });

        it('no ofrece los permisos del sistema, que no se pueden dar a un rol de iglesia', () => {
            expect(casilla('iglesias.ver')).toBeNull();
            expect(dialogo().textContent).not.toContain('Ver el catálogo de iglesias');
        });

        it('el alcance ofrece toda la iglesia, su red o solo lo propio, con su página de inicio, pero no todas las iglesias', () => {
            const opciones = fixture.componentInstance['opcionesRolBase']();

            expect(opciones.map((opcion) => opcion.etiqueta)).toEqual(['Toda la iglesia', 'Su red', 'Solo lo propio']);
            expect(opciones.map((opcion) => opcion.inicio)).toEqual(['inicio de la iglesia', 'inicio del líder', 'inicio del miembro']);
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

        it('emite un rol basado en Líder con permisos de aportes', async () => {
            await escribirNombre('Tesorería');
            await elegirRolBase(3);
            await marcar('roles.ver');

            boton('Guardar')!.click();
            await fixture.whenStable();

            expect(guardados).toEqual([{ nombre: 'Tesorería', rolBaseId: 3, permisos: ['roles.ver'] }]);
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

        it('carga el nombre y los permisos del rol, sin selector de alcance', () => {
            expect(dialogo().textContent).toContain('Editar «Secretaría»');
            expect((document.getElementById('rol-nombre') as HTMLInputElement).value).toBe('Secretaría');
            expect(document.getElementById('rol-base')).toBeNull();
            expect(casilla('miembros.ver').checked).toBeTrue();
            expect(casilla('miembros.editar').checked).toBeFalse();
        });

        it('permite cualquier permiso de iglesia', () => {
            expect(casilla('roles.administrar').disabled).toBeFalse();
            expect(casilla('iglesias.ver')).toBeNull();
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
