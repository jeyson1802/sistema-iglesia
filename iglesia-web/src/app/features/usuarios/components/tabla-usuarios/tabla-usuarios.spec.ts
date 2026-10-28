import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UsuarioAdmin } from '../../models/usuario-admin';
import { TablaUsuariosComponent } from './tabla-usuarios';

describe('TablaUsuariosComponent', () => {
    let fixture: ComponentFixture<TablaUsuariosComponent>;

    const ANA: UsuarioAdmin = {
        id: 7,
        correo: 'ana@iglesia.test',
        miembroId: null,
        activo: true,
        debeCambiarContrasena: false,
        roles: [
            { id: 2, nombre: 'Administrador de iglesia', rolBase: 'ADMIN_IGLESIA' },
            { id: 10, nombre: 'Secretaría', rolBase: 'ADMIN_IGLESIA' }
        ],
        creadoEn: new Date(2026, 2, 15, 10, 30)
    };
    const LUIS: UsuarioAdmin = { ...ANA, id: 8, correo: 'luis@iglesia.test', activo: false, debeCambiarContrasena: true, roles: [] };

    interface Opciones {
        readonly totalRegistros?: number;
        readonly pagina?: number;
        readonly cargando?: boolean;
        readonly puedeAdministrar?: boolean;
        readonly usuarioActualId?: number | null;
    }

    const crear = async (usuarios: UsuarioAdmin[], { totalRegistros = usuarios.length, pagina = 0, cargando = false, puedeAdministrar = true, usuarioActualId = 1 }: Opciones = {}): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [TablaUsuariosComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaUsuariosComponent);
        fixture.componentRef.setInput('usuarios', usuarios);
        fixture.componentRef.setInput('totalRegistros', totalRegistros);
        fixture.componentRef.setInput('pagina', pagina);
        fixture.componentRef.setInput('cargando', cargando);
        fixture.componentRef.setInput('puedeAdministrar', puedeAdministrar);
        fixture.componentRef.setInput('usuarioActualId', usuarioActualId);
        await fixture.whenStable();
    };

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const botones = (fila: HTMLElement): string[] => Array.from(fila.querySelectorAll('button')).map((boton) => boton.getAttribute('aria-label') ?? '');
    const paginaDe = (indice: number): UsuarioAdmin[] => Array.from({ length: 10 }, (_, i) => ({ ...ANA, id: 100 + indice * 10 + i, correo: `u${indice * 10 + i}@iglesia.test` }));

    it('muestra correo, roles, estado y fecha de creación', async () => {
        await crear([ANA]);

        const texto = filas()[0].textContent ?? '';
        expect(texto).toContain('ana@iglesia.test');
        expect(texto).toContain('Administrador de iglesia');
        expect(texto).toContain('Secretaría');
        expect(texto).toContain('Activo');
        expect(texto).not.toContain('Debe cambiar contraseña');
        expect(texto).toContain('15/03/2026');
    });

    it('un usuario inactivo con contraseña temporal muestra ambos estados', async () => {
        await crear([LUIS]);

        const texto = filas()[0].textContent ?? '';
        expect(texto).toContain('Inactivo');
        expect(texto).toContain('Debe cambiar contraseña');
    });

    it('con usuarios.administrar ofrece editar y restablecer contraseña y emite el usuario', async () => {
        await crear([ANA]);
        const editados: UsuarioAdmin[] = [];
        const restablecidos: UsuarioAdmin[] = [];
        fixture.componentInstance.editar.subscribe((usuario) => editados.push(usuario));
        fixture.componentInstance.restablecer.subscribe((usuario) => restablecidos.push(usuario));

        expect(botones(filas()[0])).toEqual(['Editar ana@iglesia.test', 'Restablecer contraseña de ana@iglesia.test']);
        filas()[0].querySelectorAll('button')[0].click();
        filas()[0].querySelectorAll('button')[1].click();

        expect(editados).toEqual([ANA]);
        expect(restablecidos).toEqual([ANA]);
    });

    it('sin usuarios.administrar la tabla es de solo lectura', async () => {
        await crear([ANA, LUIS], { puedeAdministrar: false });

        expect(html().querySelectorAll('tbody button').length).toBe(0);
    });

    it('la fila propia muestra «Tú» y no tiene acciones', async () => {
        await crear([ANA, LUIS], { usuarioActualId: ANA.id });

        expect(filas()[0].textContent).toContain('Tú');
        expect(botones(filas()[0])).toEqual([]);
        expect(filas()[1].textContent).not.toContain('Tú');
        expect(botones(filas()[1]).length).toBe(2);
    });

    it('mientras carga muestra skeletons en lugar de las filas y conserva la cabecera', async () => {
        await crear([ANA], { cargando: true });

        expect(html().querySelector('thead')?.textContent).toContain('Correo');
        expect(html().querySelectorAll('tbody p-skeleton').length).toBeGreaterThan(0);
        expect(html().textContent).not.toContain('ana@iglesia.test');
        expect(html().querySelector('p-table')?.getAttribute('aria-busy')).toBe('true');
    });

    it('sin resultados muestra el estado vacío', async () => {
        await crear([]);

        expect(html().textContent).toContain('No hay usuarios que coincidan con los filtros');
    });

    it('con una sola página no muestra el paginador', async () => {
        await crear([ANA, LUIS]);

        expect(html().querySelector<HTMLElement>('p-paginator')?.style.display).toBe('none');
    });

    it('al pasar de página pide al contenedor la siguiente', async () => {
        await crear(paginaDe(0), { totalRegistros: 25 });
        const pedidas: number[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((pagina) => pedidas.push(pagina));

        html().querySelector<HTMLButtonElement>('.p-paginator-next')!.click();

        expect(pedidas).toEqual([1]);
    });

    it('marca en el paginador la página que indica el contenedor', async () => {
        await crear(paginaDe(2), { totalRegistros: 25, pagina: 2 });

        expect(html().querySelector('.p-paginator-page-selected')?.textContent?.trim()).toBe('3');
        expect(html().querySelector<HTMLButtonElement>('.p-paginator-next')!.disabled).toBeTrue();
    });
});
