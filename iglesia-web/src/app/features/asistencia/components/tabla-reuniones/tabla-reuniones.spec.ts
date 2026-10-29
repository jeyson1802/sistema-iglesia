import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PaginaPedida } from '@/app/shared/utils/paginacion';
import { Reunion } from '../../models/asistencia';
import { TablaReunionesComponent } from './tabla-reuniones';

describe('TablaReunionesComponent', () => {
    const DOMINICAL: Reunion = { id: 50, tipo: 'CULTO', nombre: 'Culto dominical', fecha: '2026-10-25', hora: '10:00:00', cultoProgramadoId: 1, grupoId: null, totalAsistentes: 128 };
    let fixture: ComponentFixture<TablaReunionesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaReunionesComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaReunionesComponent);
        fixture.componentRef.setInput('reuniones', [DOMINICAL]);
        fixture.componentRef.setInput('totalRegistros', 45);
        fixture.componentRef.setInput('tamanoPagina', 20);
        fixture.detectChanges();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('muestra fecha, reunión, tipo y asistentes', () => {
        expect(html().textContent).toContain('25/10/2026');
        expect(html().textContent).toContain('Culto dominical');
        expect(html().textContent).toContain('128');
    });

    it('el paginador siempre se ve, dice cuántos hay y emite la página con su tamaño', () => {
        const pedidas: PaginaPedida[] = [];
        fixture.componentInstance.cambiarPagina.subscribe((pedida) => pedidas.push(pedida));

        expect(html().querySelector('p-paginator')?.textContent).toContain('1–20 de 45');
        (fixture.componentInstance as unknown as { alPaginar: (estado: { first: number; rows: number }) => void }).alPaginar({ first: 20, rows: 20 });
        expect(pedidas.at(-1)).toEqual({ pagina: 1, tamano: 20 });
    });

    it('al hacer clic en una fila emite la reunión para ver su detalle', () => {
        const vistas: Reunion[] = [];
        fixture.componentInstance.ver.subscribe((reunion) => vistas.push(reunion));

        (html().querySelector('tr[tabindex="0"]') as HTMLElement).click();

        expect(vistas).toEqual([DOMINICAL]);
    });

    it('sin reuniones lo dice y el paginador sigue visible', () => {
        fixture.componentRef.setInput('reuniones', []);
        fixture.componentRef.setInput('totalRegistros', 0);
        fixture.detectChanges();

        expect(html().textContent).toContain('No hay reuniones con asistencia en este período.');
        expect(html().querySelector('p-paginator')).toBeTruthy();
    });
});
