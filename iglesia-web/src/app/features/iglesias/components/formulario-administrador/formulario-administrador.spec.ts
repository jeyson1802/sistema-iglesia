import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { NuevoAdministrador } from '../../models/administrador-iglesia';
import { FormularioAdministradorComponent } from './formulario-administrador';

describe('FormularioAdministradorComponent', () => {
    let fixture: ComponentFixture<FormularioAdministradorComponent>;
    let emitidos: NuevoAdministrador[];

    const NORTE: Iglesia = { id: 2, nombre: 'Iglesia del Norte', creadaEn: new Date(2026, 0, 1) };

    const abrir = async (): Promise<void> => {
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioAdministradorComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioAdministradorComponent);
        fixture.componentRef.setInput('iglesia', NORTE);
        emitidos = [];
        fixture.componentInstance.crear.subscribe((administrador) => emitidos.push(administrador));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement => document.getElementById(id) as HTMLInputElement;
    const boton = (etiqueta: string): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === etiqueta)!;

    const escribir = async (id: string, valor: string): Promise<void> => {
        campo(id).value = valor;
        campo(id).dispatchEvent(new Event('input'));
        campo(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    it('indica la iglesia y el rol que tendrá el administrador', async () => {
        await abrir();

        expect(dialogo().textContent).toContain('Iglesia del Norte');
        expect(dialogo().textContent).toContain('Administrador de iglesia');
    });

    it('«Crear» se habilita solo con correo y contraseña temporal válidos y emite los datos', async () => {
        await abrir();
        expect(boton('Crear').disabled).toBeTrue();

        await escribir('administrador-correo', ' pastor@norte.org ');
        await escribir('administrador-contrasena', 'Corta1');
        expect(boton('Crear').disabled).toBeTrue();

        await escribir('administrador-contrasena', 'Temporal123');
        expect(boton('Crear').disabled).toBeFalse();

        boton('Crear').click();
        await fixture.whenStable();

        expect(emitidos).toEqual([{ correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' }]);
    });

    it('al volver a abrirse empieza vacío', async () => {
        await abrir();
        await escribir('administrador-contrasena', 'Temporal123');
        fixture.componentInstance.visible.set(false);
        await fixture.whenStable();

        await abrir();

        expect(campo('administrador-contrasena').value).toBe('');
    });

    it('muestra el error del contenedor y deshabilita las acciones mientras guarda', async () => {
        fixture.componentRef.setInput('error', 'Ya existe un usuario con ese correo');
        fixture.componentRef.setInput('guardando', true);
        await abrir();

        expect(dialogo().textContent).toContain('Ya existe un usuario con ese correo');
        expect(boton('Cancelar').disabled).toBeTrue();
        expect((dialogo().querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBeTrue();
    });
});
