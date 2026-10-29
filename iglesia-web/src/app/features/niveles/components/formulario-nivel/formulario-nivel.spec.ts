import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Nivel } from '../../models/nivel';
import { FormularioNivelComponent } from './formulario-nivel';

describe('FormularioNivelComponent', () => {
    let fixture: ComponentFixture<FormularioNivelComponent>;
    let guardados: string[];

    const abrir = async (nivel: Nivel | null = null): Promise<void> => {
        fixture.componentRef.setInput('nivel', nivel);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (): HTMLInputElement => document.getElementById('nivel-nombre') as HTMLInputElement;
    const guardar = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Guardar') as HTMLButtonElement;
    const escribir = async (texto: string): Promise<void> => {
        campo().value = texto;
        campo().dispatchEvent(new Event('input'));
        campo().dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioNivelComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioNivelComponent);
        guardados = [];
        fixture.componentInstance.guardar.subscribe((nombre) => guardados.push(nombre));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('al crear empieza vacío y no deja guardar', async () => {
        await abrir();

        expect(dialogo().textContent).toContain('Nuevo nivel');
        expect(campo().value).toBe('');
        expect(guardar().disabled).toBeTrue();
    });

    it('emite el nombre sin espacios sobrantes', async () => {
        await abrir();
        await escribir('  Diácono ');

        guardar().click();
        await fixture.whenStable();

        expect(guardados).toEqual(['Diácono']);
    });

    it('un nombre en blanco no es válido y lo explica', async () => {
        await abrir();
        await escribir('   ');

        expect(guardar().disabled).toBeTrue();
        expect(dialogo().textContent).toContain('Escribe el nombre del nivel');
    });

    it('al renombrar precarga el nombre actual', async () => {
        await abrir({ id: 1, nombre: 'Pastor', orden: 1, activo: true });

        expect(dialogo().textContent).toContain('Renombrar «Pastor»');
        expect(campo().value).toBe('Pastor');
    });

    it('muestra el error del backend', async () => {
        fixture.componentRef.setInput('error', 'Ya existe un nivel con ese nombre');
        await abrir();

        expect(dialogo().textContent).toContain('Ya existe un nivel con ese nombre');
    });
});
