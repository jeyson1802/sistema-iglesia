import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { of } from 'rxjs';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IglesiasService } from '@/app/core/iglesias/services/iglesias.service';
import { iglesiaPrueba } from '@/app/core/iglesias/testing/iglesia-prueba';
import { PaginaPedida } from '@/app/shared/utils/paginacion';
import { TablaIglesiasComponent } from './tabla-iglesias';

describe('TablaIglesiasComponent', () => {
    let fixture: ComponentFixture<TablaIglesiasComponent>;

    const CENTRAL = iglesiaPrueba({ id: 1, nombreComercial: 'Iglesia Central', denominacion: 'Iglesia Evangélica Central' });
    const NORTE = iglesiaPrueba({ id: 2, nombreComercial: 'Iglesia del Norte', direccion: null, distritoCodigo: null, ubicacion: null, activa: false });

    interface Opciones {
        readonly cargando?: boolean;
        readonly puedeAdministrar?: boolean;
        readonly iglesiaActualId?: number | null;
        readonly entrando?: boolean;
        readonly total?: number;
    }

    const crear = async (iglesias: Iglesia[], { cargando = false, puedeAdministrar = true, iglesiaActualId = null, entrando = false, total = iglesias.length }: Opciones = {}): Promise<void> => {
        await TestBed.configureTestingModule({
            imports: [TablaIglesiasComponent],
            providers: [provideRouter([]), { provide: IglesiasService, useValue: { logo: () => of(new Blob()) } }]
        }).compileComponents();
        fixture = TestBed.createComponent(TablaIglesiasComponent);
        fixture.componentRef.setInput('iglesias', iglesias);
        fixture.componentRef.setInput('totalRegistros', total);
        fixture.componentRef.setInput('cargando', cargando);
        fixture.componentRef.setInput('puedeAdministrar', puedeAdministrar);
        fixture.componentRef.setInput('iglesiaActualId', iglesiaActualId);
        fixture.componentRef.setInput('entrando', entrando);
        await fixture.whenStable();
    };

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLTableRowElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const etiquetas = (fila: HTMLElement): string[] => Array.from(fila.querySelectorAll('button')).map((boton) => boton.getAttribute('aria-label') ?? '');

    it('muestra el monograma, los nombres, la ubicación y el estado de cada iglesia', async () => {
        await crear([CENTRAL, NORTE]);

        expect(filas().length).toBe(2);
        expect(filas()[0].textContent).toContain('IC');
        expect(filas()[0].textContent).toContain('Iglesia Evangélica Central');
        expect(filas()[0].textContent).toContain('San Juan de Lurigancho, Lima, Lima');
        expect(filas()[0].textContent).toContain('Activa');
        expect(filas()[1].textContent).toContain('Inactiva');
    });

    it('avisa las iglesias que no tienen dirección', async () => {
        await crear([NORTE]);

        expect(filas()[0].textContent).toContain('Sin dirección');
    });

    it('con iglesias.administrar ofrece entrar, editar y crear administrador', async () => {
        await crear([CENTRAL]);
        const emitidas: string[] = [];
        fixture.componentInstance.entrar.subscribe(() => emitidas.push('entrar'));
        fixture.componentInstance.crearAdministrador.subscribe((iglesia) => emitidas.push(`administrador ${iglesia.id}`));

        expect(etiquetas(filas()[0])).toEqual(['Entrar a Iglesia Central', 'Editar Iglesia Central', 'Crear administrador de Iglesia Central']);
        filas()[0]
            .querySelectorAll('button')
            .forEach((boton) => boton.click());

        expect(emitidas).toEqual(['entrar', 'administrador 1']);
    });

    it('«Editar» lleva a la página de la iglesia', async () => {
        await crear([CENTRAL]);
        const navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);

        filas()[0].querySelector<HTMLButtonElement>('[aria-label="Editar Iglesia Central"]')!.click();

        expect(String(navegar.calls.mostRecent().args[0] as UrlTree)).toBe('/iglesias/1');
    });

    it('marca la iglesia actual y no ofrece entrar a ella', async () => {
        await crear([CENTRAL, NORTE], { iglesiaActualId: 1 });

        expect(filas()[0].textContent).toContain('Actual');
        expect(etiquetas(filas()[0])).not.toContain('Entrar a Iglesia Central');
    });

    it('sin iglesias.administrar no ofrece acciones', async () => {
        await crear([CENTRAL], { puedeAdministrar: false });

        expect(etiquetas(filas()[0])).toEqual([]);
    });

    it('mientras carga muestra skeletons en lugar de filas', async () => {
        await crear([], { cargando: true });

        expect(html().querySelectorAll('p-skeleton').length).toBeGreaterThan(0);
    });

    it('sin resultados lo dice y sugiere cambiar los filtros', async () => {
        await crear([]);

        expect(html().textContent).toContain('No hay iglesias que mostrar');
    });

    it('el paginador siempre se ve, dice cuántas hay y emite la página con su tamaño', async () => {
        await crear([CENTRAL], { total: 45 });
        const pedidas: PaginaPedida[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((pedida) => pedidas.push(pedida));

        expect(html().querySelector('p-paginator')?.textContent).toContain('1–10 de 45');
        html().querySelector<HTMLButtonElement>('.p-paginator-next')!.click();

        expect(pedidas).toEqual([{ pagina: 1, tamano: 10 }]);
    });

    it('el paginador se ve aunque haya una sola página', async () => {
        await crear([CENTRAL]);

        expect(html().querySelector('p-paginator')?.textContent).toContain('1–1 de 1');
    });
});
