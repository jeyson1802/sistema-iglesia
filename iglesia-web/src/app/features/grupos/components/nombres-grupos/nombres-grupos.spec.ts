import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NombresGrupos } from '@/app/core/configuracion/configuracion-iglesia.service';
import { NombresGruposComponent } from './nombres-grupos';

describe('NombresGruposComponent', () => {
    let fixture: ComponentFixture<NombresGruposComponent>;
    let guardados: NombresGrupos[];

    const dialogo = (): HTMLElement => document.querySelector('.p-dialog') as HTMLElement;
    const campo = (id: string): HTMLInputElement => document.getElementById(id) as HTMLInputElement;
    const guardar = (): HTMLButtonElement => Array.from(dialogo().querySelectorAll('button')).find((b) => b.textContent?.trim() === 'Guardar') as HTMLButtonElement;
    const escribir = async (id: string, texto: string): Promise<void> => {
        campo(id).value = texto;
        campo(id).dispatchEvent(new Event('input'));
        campo(id).dispatchEvent(new Event('blur'));
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [NombresGruposComponent] }).compileComponents();
        fixture = TestBed.createComponent(NombresGruposComponent);
        fixture.componentRef.setInput('nombres', { nombreGrupoSingular: 'Grupo', nombreGrupoPlural: 'Grupos' });
        guardados = [];
        fixture.componentInstance.guardar.subscribe((n) => guardados.push(n));
        fixture.componentInstance.visible.set(true);
        await fixture.whenStable();
    });

    afterEach(() => fixture.componentInstance.visible.set(false));

    it('precarga los nombres actuales', () => {
        expect(campo('grupos-singular').value).toBe('Grupo');
        expect(campo('grupos-plural').value).toBe('Grupos');
    });

    it('emite los nombres sin espacios sobrantes', async () => {
        await escribir('grupos-singular', ' Hogar de paz ');
        await escribir('grupos-plural', 'Hogares de paz');

        guardar().click();
        await fixture.whenStable();

        expect(guardados).toEqual([{ nombreGrupoSingular: 'Hogar de paz', nombreGrupoPlural: 'Hogares de paz' }]);
    });

    it('los dos nombres son obligatorios', async () => {
        await escribir('grupos-plural', '');

        expect(guardar().disabled).toBeTrue();
        expect(dialogo().textContent).toContain('Escribe el nombre en plural');
    });
});
