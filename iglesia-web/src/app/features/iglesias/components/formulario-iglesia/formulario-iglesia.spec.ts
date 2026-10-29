import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { AltaIglesia } from '../../models/administrador-iglesia';
import { FormularioIglesiaComponent } from './formulario-iglesia';

describe('FormularioIglesiaComponent', () => {
    let fixture: ComponentFixture<FormularioIglesiaComponent>;
    let altas: AltaIglesia[];
    let nombres: string[];

    const CENTRAL: Iglesia = { id: 1, nombre: 'Iglesia Central', creadaEn: new Date(2026, 0, 1) };

    const abrir = async (modo: 'crear' | 'editar' = 'crear', iglesia: Iglesia | null = null): Promise<void> => {
        fixture.componentRef.setInput('modo', modo);
        fixture.componentRef.setInput('iglesia', iglesia);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioIglesiaComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioIglesiaComponent);
        altas = [];
        nombres = [];
        fixture.componentInstance.crear.subscribe((alta) => altas.push(alta));
        fixture.componentInstance.renombrar.subscribe((nombre) => nombres.push(nombre));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    // p-dialog puede renderizar su contenido fuera del host: se busca en todo el documento.
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement | null => document.getElementById(id) as HTMLInputElement | null;
    const guardar = (): HTMLButtonElement => dialogo().querySelector('button[type="submit"]') as HTMLButtonElement;
    const cancelar = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Cancelar')!;

    const escribir = async (id: string, valor: string): Promise<void> => {
        const input = campo(id)!;
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        input.dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    const llenarAlta = async (nombre = 'Iglesia del Norte'): Promise<void> => {
        await escribir('iglesia-nombre', nombre);
        await escribir('iglesia-administrador-correo', 'pastor@norte.org');
        await escribir('iglesia-administrador-contrasena', 'Temporal123');
    };

    it('al crear pide el nombre y el correo y la contraseña temporal del primer administrador', async () => {
        await abrir();

        expect(dialogo().textContent).toContain('Nueva iglesia');
        expect(dialogo().textContent).toContain('Primer administrador');
        expect(campo('iglesia-nombre')).not.toBeNull();
        expect(campo('iglesia-administrador-correo')).not.toBeNull();
        expect(campo('iglesia-administrador-contrasena')!.getAttribute('autocomplete')).toBe('new-password');
    });

    it('«Guardar» se habilita solo con todos los datos válidos', async () => {
        await abrir();
        expect(guardar().disabled).toBeTrue();

        await escribir('iglesia-nombre', '   ');
        expect(dialogo().textContent).toContain('Escribe el nombre de la iglesia');

        await llenarAlta('a'.repeat(151));
        expect(dialogo().textContent).toContain('El nombre no puede superar 150 caracteres');
        expect(guardar().disabled).toBeTrue();

        await escribir('iglesia-nombre', 'Iglesia del Norte');
        expect(guardar().disabled).toBeFalse();
    });

    it('emite el alta con el nombre y el correo sin espacios sobrantes', async () => {
        await abrir();
        await llenarAlta('  Iglesia del Norte  ');

        guardar().click();
        await fixture.whenStable();

        expect(altas).toEqual([{ nombre: 'Iglesia del Norte', administrador: { correo: 'pastor@norte.org', contrasenaTemporal: 'Temporal123' } }]);
    });

    it('al editar solo muestra el nombre, precargado, y emite el nuevo', async () => {
        await abrir('editar', CENTRAL);

        expect(dialogo().textContent).toContain('Editar «Iglesia Central»');
        expect(campo('iglesia-administrador-correo')).toBeNull();
        expect(campo('iglesia-nombre')!.value).toBe('Iglesia Central');
        expect(guardar().disabled).toBeFalse();

        await escribir('iglesia-nombre', 'Iglesia Central de Lima');
        guardar().click();
        await fixture.whenStable();

        expect(nombres).toEqual(['Iglesia Central de Lima']);
        expect(altas).toEqual([]);
    });

    it('al volver a abrirse para crear empieza vacío', async () => {
        await abrir();
        await llenarAlta();
        fixture.componentInstance.visible.set(false);
        await fixture.whenStable();

        await abrir();

        expect(campo('iglesia-nombre')!.value).toBe('');
        expect(campo('iglesia-administrador-contrasena')!.value).toBe('');
        expect(guardar().disabled).toBeTrue();
    });

    it('muestra el error del contenedor y deshabilita las acciones mientras guarda', async () => {
        fixture.componentRef.setInput('error', 'Ya existe una iglesia con ese nombre');
        fixture.componentRef.setInput('guardando', true);
        await abrir();

        expect(dialogo().textContent).toContain('Ya existe una iglesia con ese nombre');
        expect(cancelar().disabled).toBeTrue();
        expect(guardar().disabled).toBeTrue();
    });

    it('«Cancelar» cierra el diálogo', async () => {
        await abrir();

        cancelar().click();
        await fixture.whenStable();

        expect(fixture.componentInstance.visible()).toBeFalse();
    });
});
