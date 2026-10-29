import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ResultadoMarcaComponent } from './resultado-marca';

describe('ResultadoMarcaComponent', () => {
    let fixture: ComponentFixture<ResultadoMarcaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [ResultadoMarcaComponent] }).compileComponents();
        fixture = TestBed.createComponent(ResultadoMarcaComponent);
    });

    const html = (): HTMLElement => fixture.nativeElement;

    it('una marca nueva es verde con ícono y texto', () => {
        fixture.componentRef.setInput('resultado', { tipo: 'marcado', titulo: 'Ana Torres', detalle: 'Marcado · Líder' });
        fixture.detectChanges();

        expect(html().querySelector('.bg-green-600')?.textContent).toContain('Ana Torres');
        expect(html().querySelector('.pi-check-circle')).toBeTruthy();
        expect(html().querySelector('[role="status"]')).toBeTruthy();
    });

    it('un error es rojo y se anuncia como alerta', () => {
        fixture.componentRef.setInput('resultado', { tipo: 'error', titulo: 'Código no encontrado en esta iglesia', detalle: null });
        fixture.detectChanges();

        expect(html().querySelector('[role="alert"].bg-red-600')?.textContent).toContain('Código no encontrado en esta iglesia');
    });

    it('sin resultado deja el espacio reservado para que la pantalla no salte', () => {
        fixture.detectChanges();

        expect(html().querySelector('[aria-live="assertive"]')?.children.length).toBe(0);
    });
});
