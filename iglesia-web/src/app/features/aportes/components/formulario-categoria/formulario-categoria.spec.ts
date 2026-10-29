import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoriaAporte, GuardarCategoriaAporte } from '../../models/configuracion-aportes';
import { DIEZMO } from '../../testing/aportes-prueba';
import { FormularioCategoriaComponent } from './formulario-categoria';

describe('FormularioCategoriaComponent', () => {
    let fixture: ComponentFixture<FormularioCategoriaComponent>;
    let guardadas: GuardarCategoriaAporte[];

    const abrir = async (categoria: CategoriaAporte | null = null): Promise<void> => {
        fixture.componentRef.setInput('categoria', categoria);
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    };
    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (): HTMLInputElement => document.getElementById('categoria-nombre') as HTMLInputElement;
    const guardar = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Guardar') as HTMLButtonElement;
    const escribir = async (texto: string): Promise<void> => {
        campo().value = texto;
        campo().dispatchEvent(new Event('input'));
        campo().dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [FormularioCategoriaComponent] }).compileComponents();
        fixture = TestBed.createComponent(FormularioCategoriaComponent);
        guardadas = [];
        fixture.componentInstance.guardar.subscribe((categoria) => guardadas.push(categoria));
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('al crear admite anónimos por defecto y emite el nombre limpio', async () => {
        await abrir();
        expect(dialogo().textContent).toContain('Admite aportes anónimos');
        await escribir('  Misiones ');

        guardar().click();
        await fixture.whenStable();

        expect(guardadas).toEqual([{ nombre: 'Misiones', exigeAportante: false }]);
    });

    it('un nombre en blanco no es válido y lo explica', async () => {
        await abrir();
        await escribir('   ');

        expect(guardar().disabled).toBeTrue();
        expect(dialogo().textContent).toContain('Escribe el nombre de la categoría');
    });

    it('al editar precarga el nombre y si exige aportante', async () => {
        await abrir(DIEZMO);

        expect(campo().value).toBe('Diezmo');
        expect(dialogo().textContent).toContain('No admite aportes anónimos');
        expect(dialogo().textContent).toContain('Editar «Diezmo»');
    });
});
