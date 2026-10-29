import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Ocasion } from '../../models/seguimiento';
import { TablaOcasionesComponent } from './tabla-ocasiones';

describe('TablaOcasionesComponent', () => {
    const OCASIONES: Ocasion[] = [
        { fecha: '2026-10-25', hora: null, tipo: 'CULTO', descripcion: 'Primer servicio · Segundo servicio', asistioA: 'Segundo servicio', estado: 'ASISTIO' },
        { fecha: '2026-10-22', hora: '19:30:00', tipo: 'GRUPO', descripcion: 'Célula de Ana', asistioA: null, estado: 'FALTO' }
    ];
    let fixture: ComponentFixture<TablaOcasionesComponent>;

    beforeEach(async () => {
        fixture = TestBed.createComponent(TablaOcasionesComponent);
        fixture.componentRef.setInput('ocasiones', OCASIONES);
        fixture.componentRef.setInput('totalRegistros', 25);
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra cada ocasión con su estado y a qué reunión fue', () => {
        const texto = fixture.nativeElement.querySelector('p-table').textContent;
        expect(texto).toContain('Día de culto');
        expect(texto).toContain('Asistió');
        expect(texto).toContain('Segundo servicio');
        expect(texto).toContain('Faltó');
    });

    it('muestra siempre el paginador y avisa la página pedida', () => {
        const emitida = jasmine.createSpy('cambiarPagina');
        fixture.componentInstance.cambiarPagina.subscribe(emitida);
        expect(fixture.nativeElement.querySelector('p-paginator')).not.toBeNull();
        (fixture.componentInstance as unknown as { alPaginar(e: object): void }).alPaginar({ first: 20, rows: 10 });
        expect(emitida).toHaveBeenCalledWith({ pagina: 2, tamano: 10 });
    });

    it('sin ocasiones muestra el estado vacío', () => {
        fixture.componentRef.setInput('ocasiones', []);
        fixture.detectChanges();
        expect(fixture.nativeElement.textContent).toContain('No hay ocasiones en este período.');
    });
});
