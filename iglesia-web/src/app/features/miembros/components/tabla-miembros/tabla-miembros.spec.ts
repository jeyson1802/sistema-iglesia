import { ComponentFixture, TestBed } from '@angular/core/testing';
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
        fixture.componentRef.setInput('nombreGrupo', 'Célula');
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

    it('usa el nombre configurado del grupo en la cabecera y atenúa las referencias inactivas', async () => {
        await crear([RESUMEN_JUAN]);

        expect(html().querySelector('thead')?.textContent).toContain('Célula');
        expect(html().querySelector('span[title="Inactivo"]')?.textContent).toContain('Los Olivos');
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

    it('emite la página pedida', async () => {
        await crear([RESUMEN_JUAN]);
        const paginas: number[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((p) => paginas.push(p));

        fixture.componentInstance['alCargar']({ first: 20 });

        expect(paginas).toEqual([2]);
    });
});
