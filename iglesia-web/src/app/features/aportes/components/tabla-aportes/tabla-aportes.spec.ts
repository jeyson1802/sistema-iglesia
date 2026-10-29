import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaPedida } from '@/app/shared/utils/paginacion';
import { Aporte } from '../../models/aporte';
import { aporte } from '../../testing/aportes-prueba';
import { TablaAportesComponent } from './tabla-aportes';

describe('TablaAportesComponent', () => {
    let fixture: ComponentFixture<TablaAportesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaAportesComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaAportesComponent);
        fixture.componentRef.setInput('moneda', 'PEN');
        fixture.componentRef.setInput('aportes', [aporte(), aporte({ id: 46, miembro: null, anulado: true, motivoAnulacion: 'Duplicado', monto: 20, medio: 'EFECTIVO' })]);
        fixture.componentRef.setInput('totalRegistros', 2);
        fixture.componentRef.setInput('puedeEditar', true);
        fixture.componentRef.setInput('puedeAnular', true);
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const filas = (): HTMLElement[] => Array.from(html().querySelectorAll('tbody tr'));
    const componente = () => fixture.componentInstance as unknown as { accionesFila: () => { label: string; disabled?: boolean }[]; abrirAcciones: (evento: Event, aporte: Aporte) => void };

    it('muestra el monto con la moneda, el medio, el anónimo y el anulado', () => {
        expect(filas()[0].textContent).toContain('S/\u00a0150.50');
        expect(filas()[0].textContent).toContain('Yape');
        expect(filas()[1].textContent).toContain('Anónimo');
        expect(filas()[1].textContent).toContain('Anulado');
    });

    it('tiene también la vista en tarjetas para el móvil', () => {
        expect(html().querySelectorAll('ul.md\\:hidden > li').length).toBe(2);
    });

    it('un clic o Enter en la fila abre el detalle', () => {
        const vistos: number[] = [];
        fixture.componentInstance.ver.subscribe(({ id }) => vistos.push(id));

        filas()[0].click();
        filas()[1].dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

        expect(vistos).toEqual([45, 46]);
    });

    it('el menú de un anulado no deja editar ni anular', () => {
        componente().abrirAcciones(new MouseEvent('click'), aporte({ anulado: true }));

        expect(
            componente()
                .accionesFila()
                .map(({ label, disabled }) => [label, !!disabled])
        ).toEqual([
            ['Ver detalle', false],
            ['Editar', true],
            ['Anular', true]
        ]);
    });

    it('sin permisos el menú solo tiene el detalle', async () => {
        fixture.componentRef.setInput('puedeEditar', false);
        fixture.componentRef.setInput('puedeAnular', false);
        await fixture.whenStable();

        componente().abrirAcciones(new MouseEvent('click'), aporte());

        expect(
            componente()
                .accionesFila()
                .map(({ label }) => label)
        ).toEqual(['Ver detalle']);
    });

    it('el paginador siempre se ve, dice cuántos hay y emite la página con su tamaño', async () => {
        fixture.componentRef.setInput('totalRegistros', 45);
        await fixture.whenStable();
        const pedidas: PaginaPedida[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((pedida) => pedidas.push(pedida));

        expect(html().querySelector('p-paginator')?.textContent).toContain('1–20 de 45');
        (fixture.componentInstance as unknown as { alPaginar: (estado: { first: number; rows: number }) => void }).alPaginar({ first: 40, rows: 20 });

        expect(pedidas).toEqual([{ pagina: 2, tamano: 20 }]);
    });

    it('mientras carga muestra el esqueleto', async () => {
        fixture.componentRef.setInput('cargando', true);
        await fixture.whenStable();

        expect(html().querySelector('[aria-label="Cargando aportes"]')).not.toBeNull();
    });
});
