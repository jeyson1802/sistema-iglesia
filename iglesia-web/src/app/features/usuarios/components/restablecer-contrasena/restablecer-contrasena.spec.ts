import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UsuarioAdmin } from '../../models/usuario-admin';
import { RestablecerContrasenaComponent } from './restablecer-contrasena';

describe('RestablecerContrasenaComponent', () => {
    let fixture: ComponentFixture<RestablecerContrasenaComponent>;
    let emitidas: string[];

    const ANA: UsuarioAdmin = {
        id: 7,
        correo: 'ana@iglesia.test',
        miembroId: null,
        activo: true,
        debeCambiarContrasena: false,
        roles: [{ id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA' }],
        creadoEn: new Date(2026, 0, 1)
    };

    const abrir = async (): Promise<void> => {
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [RestablecerContrasenaComponent] }).compileComponents();
        fixture = TestBed.createComponent(RestablecerContrasenaComponent);
        fixture.componentRef.setInput('usuario', ANA);
        emitidas = [];
        fixture.componentInstance.restablecer.subscribe((contrasena) => emitidas.push(contrasena));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    // p-dialog puede renderizar su contenido fuera del host: se busca en todo el documento.
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (): HTMLInputElement => document.getElementById('restablecer-contrasena') as HTMLInputElement;
    const boton = (etiqueta: string): HTMLButtonElement | undefined => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta);

    const escribir = async (valor: string): Promise<void> => {
        campo().value = valor;
        campo().dispatchEvent(new Event('input'));
        campo().dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    it('explica a quién se le restablece, que deberá cambiarla y que se cerrarán sus sesiones', async () => {
        await abrir();

        const texto = dialogo().textContent ?? '';
        expect(texto).toContain('ana@iglesia.test');
        expect(texto).toContain('Deberá cambiarla al iniciar sesión');
        expect(texto).toContain('se cerrarán todas sus sesiones');
        expect(campo().getAttribute('autocomplete')).toBe('new-password');
    });

    it('«Restablecer» se habilita solo con una contraseña de 8 a 72 caracteres', async () => {
        await abrir();
        expect(boton('Restablecer')!.disabled).toBeTrue();

        await escribir('Corta1');
        expect(boton('Restablecer')!.disabled).toBeTrue();
        expect(dialogo().textContent).toContain('Usa al menos 8 caracteres');

        await escribir('a'.repeat(73));
        expect(boton('Restablecer')!.disabled).toBeTrue();

        await escribir('TemporalNueva123');
        expect(boton('Restablecer')!.disabled).toBeFalse();
    });

    it('emite la contraseña temporal', async () => {
        await abrir();
        await escribir('TemporalNueva123');

        boton('Restablecer')!.click();
        await fixture.whenStable();

        expect(emitidas).toEqual(['TemporalNueva123']);
    });

    it('al volver a abrirse empieza vacío', async () => {
        await abrir();
        await escribir('TemporalNueva123');
        fixture.componentInstance.visible.set(false);
        await fixture.whenStable();

        await abrir();

        expect(campo().value).toBe('');
        expect(boton('Restablecer')!.disabled).toBeTrue();
    });

    it('muestra el error del contenedor y deshabilita las acciones mientras guarda', async () => {
        fixture.componentRef.setInput('error', 'No se encontró el usuario');
        fixture.componentRef.setInput('guardando', true);
        await abrir();

        expect(dialogo().textContent).toContain('No se encontró el usuario');
        expect(boton('Cancelar')!.disabled).toBeTrue();
        expect(dialogo().querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBeTrue();
    });

    it('«Cancelar» cierra el diálogo', async () => {
        await abrir();

        boton('Cancelar')!.click();
        await fixture.whenStable();

        expect(fixture.componentInstance.visible()).toBeFalse();
    });
});
