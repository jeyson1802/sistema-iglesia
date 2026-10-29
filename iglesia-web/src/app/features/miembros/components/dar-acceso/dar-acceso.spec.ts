import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CrearUsuario } from '@/app/features/usuarios/models/usuario-admin';
import { DarAccesoComponent } from './dar-acceso';

describe('DarAccesoComponent', () => {
    let fixture: ComponentFixture<DarAccesoComponent>;
    let emitidos: CrearUsuario[];

    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement => document.getElementById(id) as HTMLInputElement;
    const boton = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Dar acceso') as HTMLButtonElement;
    const formulario = () => fixture.componentInstance['formulario'];

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [DarAccesoComponent] }).compileComponents();
        fixture = TestBed.createComponent(DarAccesoComponent);
        fixture.componentRef.setInput('nombreMiembro', 'Juan Pérez');
        fixture.componentRef.setInput('correoSugerido', 'juan@correo.test');
        fixture.componentRef.setInput('roles', [
            { id: 3, nombre: 'Líder' },
            { id: 4, nombre: 'Miembro' }
        ]);
        emitidos = [];
        fixture.componentInstance.darAcceso.subscribe((d) => emitidos.push(d));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('precarga el correo del miembro y nombra a quién se da acceso', () => {
        expect(campo('acceso-correo').value).toBe('juan@correo.test');
        expect(dialogo().textContent).toContain('Juan Pérez');
        expect(boton().disabled).toBeTrue();
    });

    it('emite correo, contraseña temporal y el rol elegido', async () => {
        formulario().controls.contrasenaTemporal.setValue('Temporal123');
        formulario().controls.rolId.setValue(3);
        await fixture.whenStable();

        boton().click();
        await fixture.whenStable();

        expect(emitidos).toEqual([{ correo: 'juan@correo.test', contrasenaTemporal: 'Temporal123', rolIds: [3] }]);
    });

    it('una contraseña corta no es válida', async () => {
        formulario().controls.contrasenaTemporal.setValue('corta');
        formulario().controls.contrasenaTemporal.markAsTouched();
        formulario().controls.rolId.setValue(4);
        await fixture.whenStable();

        expect(boton().disabled).toBeTrue();
        expect(dialogo().textContent).toContain('Usa al menos 8 caracteres');
    });

    it('muestra el error del backend', async () => {
        fixture.componentRef.setInput('error', 'Ya existe un usuario con ese correo');
        await fixture.whenStable();

        expect(dialogo().textContent).toContain('Ya existe un usuario con ese correo');
    });
});
