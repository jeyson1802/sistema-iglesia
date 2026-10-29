import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CategoriaAporte } from '../../models/configuracion-aportes';
import { DIEZMO, OFRENDA } from '../../testing/aportes-prueba';
import { TablaCategoriasComponent } from './tabla-categorias';

describe('TablaCategoriasComponent', () => {
    let fixture: ComponentFixture<TablaCategoriasComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaCategoriasComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaCategoriasComponent);
        fixture.componentRef.setInput('categorias', [DIEZMO, { ...OFRENDA, activo: false }]);
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (etiqueta: string): HTMLButtonElement => html().querySelector(`button[aria-label="${etiqueta}"]`) as HTMLButtonElement;

    it('muestra si exige aportante y su estado', () => {
        const filas = html().querySelectorAll('tbody tr');
        expect(filas[0].textContent).toContain('Exige aportante');
        expect(filas[1].textContent).toContain('Admite anónimos');
        expect(filas[1].textContent).toContain('Inactiva');
    });

    it('no deja subir la primera ni bajar la última y emite las acciones', () => {
        const emitidas: CategoriaAporte[] = [];
        fixture.componentInstance.bajar.subscribe((categoria) => emitidas.push(categoria));
        fixture.componentInstance.cambiarEstado.subscribe((categoria) => emitidas.push(categoria));

        expect(boton('Subir Diezmo').disabled).toBeTrue();
        expect(boton('Bajar Ofrenda').disabled).toBeTrue();
        boton('Bajar Diezmo').click();
        boton('Reactivar Ofrenda').click();

        expect(emitidas.map(({ id }) => id)).toEqual([1, 2]);
    });

    it('muestra el esqueleto mientras carga', async () => {
        fixture.componentRef.setInput('cargando', true);
        await fixture.whenStable();

        expect(html().querySelector('[aria-label="Cargando categorías"]')).not.toBeNull();
    });

    it('muestra siempre el paginador con cuántas hay', () => {
        expect(html().querySelector<HTMLElement>('p-paginator')?.style.display).not.toBe('none');
        expect(html().querySelector('p-paginator')?.textContent).toContain('1–2 de 2');
    });
});
