import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Asistente } from '../../models/asistencia';
import { TablaAsistentesComponent } from './tabla-asistentes';

describe('TablaAsistentesComponent', () => {
    const ANA: Asistente = { miembroId: 12, nombreCompleto: 'Ana Torres', nivel: 'Líder', grupoId: 8, medio: 'QR', registradaEn: '2026-10-29T15:02:00Z' };
    let fixture: ComponentFixture<TablaAsistentesComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [TablaAsistentesComponent] }).compileComponents();
        fixture = TestBed.createComponent(TablaAsistentesComponent);
        fixture.componentRef.setInput('asistentes', [ANA]);
        fixture.componentRef.setInput('totalRegistros', 1);
        fixture.detectChanges();
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('muestra nombre, nivel y medio', () => {
        expect(html().textContent).toContain('Ana Torres');
        expect(html().textContent).toContain('Líder');
        expect(html().textContent).toContain('Con QR');
    });

    it('el paginador siempre se ve', () => {
        expect(html().querySelector('p-paginator')?.textContent).toContain('1–1 de 1');
    });

    it('distingue el vacío de la búsqueda sin resultados', () => {
        fixture.componentRef.setInput('asistentes', []);
        fixture.componentRef.setInput('filtrando', true);
        fixture.detectChanges();

        expect(html().textContent).toContain('Nadie con ese nombre asistió a esta reunión.');
    });
});
