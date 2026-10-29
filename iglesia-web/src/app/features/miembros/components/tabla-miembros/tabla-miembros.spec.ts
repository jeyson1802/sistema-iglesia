import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaPedida } from '@/app/shared/utils/paginacion';
import { provideRouter } from '@angular/router';
import { MiembroResumen } from '../../models/miembro';
import { RESUMEN_JUAN, RESUMEN_PASTOR } from '../../testing/miembros-prueba';
import { TablaMiembrosComponent } from './tabla-miembros';

describe('TablaMiembrosComponent', () => {
    let fixture: ComponentFixture<TablaMiembrosComponent>;

    const html = (): HTMLElement => fixture.nativeElement;
    const crear = async (miembros: MiembroResumen[], cargando = false): Promise<void> => {
        fixture.componentRef.setInput('miembros', miembros);
        fixture.componentRef.setInput('totalRegistros', miembros.length);
        fixture.componentRef.setInput('cargando', cargando);
        await fixture.whenStable();
    };

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaMiembrosComponent], providers: [provideRouter([])] }).compileComponents();
        fixture = TestBed.createComponent(TablaMiembrosComponent);
    });

    it('muestra los datos de cada miembro con enlace a su ficha', async () => {
        await crear([RESUMEN_JUAN]);

        const fila = html().querySelector('tbody tr') as HTMLElement;
        expect(fila.textContent).toContain('Pérez Torres, Juan Carlos');
        expect(fila.textContent).toContain('Nuevo');
        expect(fila.textContent).toContain('Ana Ríos');
        expect(fila.querySelector('a')?.getAttribute('href')).toBe('/miembros/22');
    });

    it('separa la dirección de la región (distrito, provincia y departamento), sin columna de grupo', async () => {
        await crear([RESUMEN_JUAN]);

        const cabeceras = Array.from(html().querySelectorAll('thead th')).map((th) => th.textContent?.trim());
        expect(cabeceras).toEqual(['Nombre', 'Condición', 'Nivel', 'Líder', 'Dirección', 'Región', 'Teléfono', 'Estado']);
        const celdas = Array.from(html().querySelectorAll('tbody tr td')).map((td) => td.textContent?.trim());
        expect(celdas[4]).toBe('Av. Perú 123');
        expect(celdas[5]).toBe('San Martín de Porres, Lima, Lima');
    });

    it('sin dirección ni región muestra un guion', async () => {
        await crear([RESUMEN_PASTOR]);

        const celdas = Array.from(html().querySelectorAll('tbody tr td')).map((td) => td.textContent?.trim());
        expect([celdas[4], celdas[5]]).toEqual(['—', '—']);
    });

    it('atenúa las referencias inactivas', async () => {
        await crear([{ ...RESUMEN_JUAN, lider: { id: 10, nombre: 'Ana Ríos', activo: false } }]);

        expect(html().querySelector('span[title="Inactivo"]')?.textContent).toContain('Ana Ríos');
    });

    it('marca al pastor principal', async () => {
        await crear([RESUMEN_PASTOR]);

        expect(html().textContent).toContain('Pastor principal');
    });

    it('muestra skeletons al cargar y el vacío sin datos', async () => {
        await crear([], true);
        expect(html().querySelectorAll('p-skeleton').length).toBeGreaterThan(0);

        await crear([]);
        expect(html().textContent).toContain('No hay miembros para mostrar');
    });

    it('emite la página pedida con su tamaño', async () => {
        await crear([RESUMEN_JUAN]);
        const paginas: PaginaPedida[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((p) => paginas.push(p));

        fixture.componentInstance['alCargar']({ first: 20 });
        fixture.componentInstance['alCargar']({ first: 40, rows: 20 });

        expect(paginas).toEqual([
            { pagina: 2, tamano: 10 },
            { pagina: 2, tamano: 20 }
        ]);
    });

    it('muestra siempre el paginador, con cuántos hay y cuántos por página', async () => {
        await crear([RESUMEN_JUAN]);

        expect(html().querySelector<HTMLElement>('p-paginator')?.style.display).not.toBe('none');
        expect(html().querySelector('p-paginator')?.textContent).toContain('1–1 de 1');
        expect(html().querySelector('p-paginator p-select')).not.toBeNull();
    });
});
