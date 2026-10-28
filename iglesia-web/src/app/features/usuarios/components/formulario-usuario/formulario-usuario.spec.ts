import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Confirmation, ConfirmationService } from 'primeng/api';
import { RolAcceso } from '@/app/features/roles/models/rol-acceso';
import { ROLES_BASE, SECRETARIA } from '@/app/features/roles/testing/roles-prueba';
import { CrearUsuario, EditarUsuario, UsuarioAdmin } from '../../models/usuario-admin';
import { FormularioUsuarioComponent, ModoFormularioUsuario } from './formulario-usuario';

describe('FormularioUsuarioComponent', () => {
    let fixture: ComponentFixture<FormularioUsuarioComponent>;
    let creados: CrearUsuario[];
    let actualizados: EditarUsuario[];

    const TESORERIA_INACTIVA: RolAcceso = { ...SECRETARIA, id: 11, nombre: 'Tesorería', activo: false };
    const LIDER_JOVENES: RolAcceso = { ...SECRETARIA, id: 12, nombre: 'Líder de jóvenes', rolBase: 'LIDER', requiereMiembro: true };
    const ROLES: RolAcceso[] = [...ROLES_BASE, SECRETARIA, TESORERIA_INACTIVA, LIDER_JOVENES];

    const LIDER_MIEMBRO: UsuarioAdmin = {
        id: 20,
        correo: 'lider.miembro@prueba.test',
        miembroId: 55,
        activo: true,
        debeCambiarContrasena: false,
        roles: [
            { id: 3, nombre: 'Líder', rolBase: 'LIDER' },
            { id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA' }
        ],
        creadoEn: new Date(2026, 0, 1)
    };

    const abrir = async (modo: ModoFormularioUsuario, usuario: UsuarioAdmin | null = null): Promise<void> => {
        fixture.componentRef.setInput('modo', modo);
        fixture.componentRef.setInput('usuario', usuario);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioUsuarioComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioUsuarioComponent);
        fixture.componentRef.setInput('roles', ROLES);
        creados = [];
        actualizados = [];
        fixture.componentInstance.crear.subscribe((datos) => creados.push(datos));
        fixture.componentInstance.actualizar.subscribe((datos) => actualizados.push(datos));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    // p-dialog puede renderizar su contenido fuera del host: se busca en todo el documento.
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement => document.getElementById(id) as HTMLInputElement;
    const casilla = (id: number): HTMLInputElement => campo(`usuario-rol-${id}`);
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta);
    const rolesVisibles = (): string[] => Array.from(dialogo().querySelectorAll('fieldset label')).map((etiqueta) => etiqueta.textContent?.trim() ?? '');
    const confirmacion = (): ConfirmationService => fixture.debugElement.injector.get(ConfirmationService);

    const escribir = async (id: string, valor: string): Promise<void> => {
        campo(id).value = valor;
        campo(id).dispatchEvent(new Event('input'));
        campo(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    const marcar = async (id: number): Promise<void> => {
        casilla(id).click();
        casilla(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    const guardar = async (): Promise<void> => {
        boton('Guardar')!.click();
        await fixture.whenStable();
    };

    describe('al crear', () => {
        beforeEach(() => abrir('crear'));

        it('pide correo, contraseña temporal y roles, sin el interruptor de activo', () => {
            expect(dialogo().textContent).toContain('Nuevo usuario');
            expect(campo('usuario-correo')).not.toBeNull();
            expect(campo('usuario-contrasena').getAttribute('autocomplete')).toBe('new-password');
            expect(campo('usuario-activo')).toBeNull();
        });

        it('solo ofrece roles activos que no exigen miembro y nunca el de administrador del sistema', () => {
            expect(rolesVisibles()).toEqual(['Administrador de iglesia', 'Secretaría']);
        });

        it('«Guardar» se habilita cuando el correo es válido, la contraseña tiene 8 caracteres y hay un rol marcado', async () => {
            expect(boton('Guardar')!.disabled).toBeTrue();

            await escribir('usuario-correo', 'nuevo@iglesia.test');
            await escribir('usuario-contrasena', 'Temporal1');
            expect(boton('Guardar')!.disabled).toBeTrue();

            await marcar(10);
            expect(boton('Guardar')!.disabled).toBeFalse();

            await escribir('usuario-contrasena', 'Corta1');
            expect(boton('Guardar')!.disabled).toBeTrue();
        });

        it('muestra en español el error de cada campo al perder el foco', async () => {
            await escribir('usuario-correo', 'no-es-correo');
            await escribir('usuario-contrasena', 'corta');

            expect(dialogo().textContent).toContain('Escribe un correo válido');
            expect(dialogo().textContent).toContain('Usa al menos 8 caracteres');
        });

        it('pide marcar al menos un rol al desmarcar el último', async () => {
            await marcar(10);
            await marcar(10);

            expect(dialogo().textContent).toContain('Marca al menos un rol');
        });

        it('emite el usuario nuevo con el correo sin espacios y los roles marcados', async () => {
            await escribir('usuario-correo', '  nuevo@iglesia.test ');
            await escribir('usuario-contrasena', 'Temporal1');
            await marcar(2);
            await marcar(10);
            await guardar();

            expect(creados).toEqual([{ correo: 'nuevo@iglesia.test', contrasenaTemporal: 'Temporal1', rolIds: [2, 10] }]);
        });
    });

    describe('al editar', () => {
        beforeEach(() => abrir('editar', LIDER_MIEMBRO));

        it('carga el correo editable y el estado, sin campo de contraseña', () => {
            expect(dialogo().textContent).toContain('Editar «lider.miembro@prueba.test»');
            expect(campo('usuario-correo').value).toBe('lider.miembro@prueba.test');
            expect(campo('usuario-correo').disabled).toBeFalse();
            expect(campo('usuario-contrasena')).toBeNull();
            expect(campo('usuario-activo').checked).toBeTrue();
        });

        it('muestra marcados y deshabilitados los roles que exigen miembro, con su aviso', () => {
            expect(rolesVisibles()).toEqual(['Administrador de iglesia', 'Líder', 'Secretaría']);
            expect(casilla(3).checked).toBeTrue();
            expect(casilla(3).disabled).toBeTrue();
            expect(casilla(10).checked).toBeTrue();
            expect(casilla(10).disabled).toBeFalse();
            expect(dialogo().textContent).toContain('Se gestiona al vincular el usuario con un miembro');
        });

        it('los roles bloqueados cuentan para «al menos uno» y se reenvían al guardar', async () => {
            await marcar(10);
            expect(boton('Guardar')!.disabled).toBeFalse();

            await guardar();

            expect(actualizados).toEqual([{ correo: 'lider.miembro@prueba.test', rolIds: [3], activo: true }]);
        });

        it('sin cambios de correo ni de estado guarda sin pedir confirmación', async () => {
            const confirmar = spyOn(confirmacion(), 'confirm');
            await marcar(2);
            await guardar();

            expect(confirmar).not.toHaveBeenCalled();
            expect(actualizados).toEqual([{ correo: 'lider.miembro@prueba.test', rolIds: [2, 3, 10], activo: true }]);
        });

        it('un correo que solo cambia en mayúsculas o espacios no pide confirmación', async () => {
            const confirmar = spyOn(confirmacion(), 'confirm');
            await escribir('usuario-correo', ' Lider.Miembro@Prueba.test ');
            await guardar();

            expect(confirmar).not.toHaveBeenCalled();
            expect(actualizados.length).toBe(1);
        });

        it('cambiar el correo pide confirmación y solo guarda al aceptar', async () => {
            let pedida: Confirmation | undefined;
            spyOn(confirmacion(), 'confirm').and.callFake((confirmation: Confirmation) => {
                pedida = confirmation;
                return confirmacion();
            });
            await escribir('usuario-correo', 'nuevo.correo@prueba.test');
            await guardar();

            expect(pedida?.message).toContain('deberá entrar con el correo nuevo (nuevo.correo@prueba.test)');
            expect(actualizados).toEqual([]);

            pedida!.accept!();

            expect(actualizados).toEqual([{ correo: 'nuevo.correo@prueba.test', rolIds: [3, 10], activo: true }]);
        });

        it('desactivar al usuario pide confirmación y no guarda si se cancela', async () => {
            let pedida: Confirmation | undefined;
            spyOn(confirmacion(), 'confirm').and.callFake((confirmation: Confirmation) => {
                pedida = confirmation;
                return confirmacion();
            });
            campo('usuario-activo').click();
            await fixture.whenStable();
            await guardar();

            expect(pedida?.message).toContain('Al desactivarlo se cerrarán sus sesiones');
            expect(pedida?.message).not.toContain('correo nuevo');
            expect(actualizados).toEqual([]);

            pedida!.accept!();

            expect(actualizados).toEqual([{ correo: 'lider.miembro@prueba.test', rolIds: [3, 10], activo: false }]);
        });

        it('reactivar a un usuario inactivo no pide confirmación', async () => {
            await abrir('editar', { ...LIDER_MIEMBRO, activo: false });
            const confirmar = spyOn(confirmacion(), 'confirm');
            campo('usuario-activo').click();
            await fixture.whenStable();
            await guardar();

            expect(confirmar).not.toHaveBeenCalled();
            expect(actualizados).toEqual([{ correo: 'lider.miembro@prueba.test', rolIds: [3, 10], activo: true }]);
        });
    });

    it('muestra dentro del diálogo el error que recibe del contenedor', async () => {
        fixture.componentRef.setInput('error', 'Ya existe un usuario con ese correo');
        await abrir('crear');

        expect(dialogo().textContent).toContain('Ya existe un usuario con ese correo');
    });

    it('mientras guarda deshabilita «Guardar» y «Cancelar»', async () => {
        fixture.componentRef.setInput('guardando', true);
        await abrir('editar', LIDER_MIEMBRO);

        expect(boton('Cancelar')!.disabled).toBeTrue();
        expect(dialogo().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBeTrue();
    });
});
