import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RolAcceso } from '../../models/rol-acceso';
import { ROLES_BASE, SECRETARIA } from '../../testing/roles-prueba';
import { TablaRolesComponent } from './tabla-roles';

describe('TablaRolesComponent', () => {
    let fixture: ComponentFixture<TablaRolesComponent>;

    const crear = async (roles: RolAcceso[], { cargando = false, puedeAdministrar = true } = {}): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [TablaRolesComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaRolesComponent);
        fixture.componentRef.setInput('roles', roles);
        fixture.componentRef.setInput('cargando', cargando);
        fixture.componentRef.setInput('puedeAdministrar', puedeAdministrar);
        await fixture.whenStable();
    };

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const botones = (fila: HTMLElement): string[] =>
        Array.from(fila.querySelectorAll('button')).map((boton) => boton.getAttribute('aria-label') ?? boton.textContent?.trim() ?? '');

    it('muestra nombre, rol base, tipo, estado, usuarios y número de permisos', async () => {
        await crear([SECRETARIA]);

        const texto = filas()[0].textContent ?? '';
        expect(texto).toContain('Secretaría');
        expect(texto).toContain('Administrador de iglesia');
        expect(texto).toContain('Personalizado');
        expect(texto).toContain('Activo');
        expect(texto).toContain('3');
        expect(texto).toContain('2');
    });

    it('los roles base llevan la etiqueta «Base» y solo «Ver permisos»', async () => {
        await crear([ROLES_BASE[1]]);

        expect(filas()[0].textContent).toContain('Base');
        expect(botones(filas()[0])).toEqual(['Ver permisos']);
    });

    it('un rol personalizado activo ofrece editar y desactivar a quien administra roles', async () => {
        await crear([SECRETARIA]);

        expect(botones(filas()[0])).toEqual(['Editar Secretaría', 'Desactivar Secretaría']);
    });

    it('un rol personalizado inactivo ofrece reactivarlo', async () => {
        await crear([{ ...SECRETARIA, activo: false }]);

        expect(filas()[0].textContent).toContain('Inactivo');
        expect(botones(filas()[0])).toEqual(['Editar Secretaría', 'Reactivar Secretaría']);
    });

    it('sin roles.administrar, los personalizados también son de solo lectura', async () => {
        await crear([SECRETARIA], { puedeAdministrar: false });

        expect(botones(filas()[0])).toEqual(['Ver permisos']);
    });

    it('emite la acción elegida con el rol', async () => {
        await crear([ROLES_BASE[1], SECRETARIA]);
        const vistos: RolAcceso[] = [];
        const editados: RolAcceso[] = [];
        const cambiados: RolAcceso[] = [];
        fixture.componentInstance.ver.subscribe((rol) => vistos.push(rol));
        fixture.componentInstance.editar.subscribe((rol) => editados.push(rol));
        fixture.componentInstance.cambiarEstado.subscribe((rol) => cambiados.push(rol));

        filas()[0].querySelector('button')!.click();
        filas()[1].querySelectorAll('button')[0].click();
        filas()[1].querySelectorAll('button')[1].click();

        expect(vistos).toEqual([ROLES_BASE[1]]);
        expect(editados).toEqual([SECRETARIA]);
        expect(cambiados).toEqual([SECRETARIA]);
    });

    it('mientras carga muestra skeletons en lugar de la tabla', async () => {
        await crear([], { cargando: true });

        expect(html().querySelector('[role="status"]')).not.toBeNull();
        expect(html().querySelector('p-skeleton')).not.toBeNull();
        expect(html().querySelector('table')).toBeNull();
    });

    it('sin roles muestra el estado vacío', async () => {
        await crear([]);

        expect(html().textContent).toContain('No hay roles para mostrar');
    });
});
