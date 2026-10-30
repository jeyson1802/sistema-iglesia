import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FILTRO_CASOS_RONDA_INICIAL, FiltroCasosRonda } from '../../models/consolidacion';
import { rondaPrueba } from '../../testing/datos-prueba';
import { FiltrosCasosComponent } from './filtros-casos';

describe('FiltrosCasosComponent', () => {
    let fixture: ComponentFixture<FiltrosCasosComponent>;
    let filtros: FiltroCasosRonda[];
    const chip = (texto: string): HTMLButtonElement => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('[role="radio"]')).find((boton) => boton.textContent!.includes(texto))!;

    beforeEach(async () => {
        fixture = TestBed.createComponent(FiltrosCasosComponent);
        filtros = [];
        fixture.componentInstance.filtrar.subscribe((filtro) => filtros.push(filtro));
        fixture.componentRef.setInput('filtro', FILTRO_CASOS_RONDA_INICIAL);
        fixture.componentRef.setInput('ronda', rondaPrueba(7, { personas: 40, pendientes: 12, enContacto: 18, cerrados: 10, sinAsignar: 3 }));
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('muestra los chips con su cantidad y marca «Todos»', () => {
        expect(chip('Pendientes').textContent).toContain('12');
        expect(chip('Sin asignar').textContent).toContain('3');
        expect(chip('Todos').getAttribute('aria-checked')).toBe('true');
    });

    it('filtra por estado o por sin asignar', () => {
        chip('Pendientes').click();
        chip('Sin asignar').click();
        expect(filtros[0]).toEqual(jasmine.objectContaining({ estado: 'PENDIENTE', sinAsignar: false }));
        expect(filtros[1]).toEqual(jasmine.objectContaining({ estado: null, sinAsignar: true }));
    });

    it('busca al dejar de escribir', async () => {
        jasmine.clock().install();
        const buscador = fixture.nativeElement.querySelector('input[type="search"]') as HTMLInputElement;
        buscador.value = 'ana';
        buscador.dispatchEvent(new Event('input'));
        expect(filtros.length).toBe(0);
        jasmine.clock().tick(300);
        jasmine.clock().uninstall();
        expect(filtros[0].q).toBe('ana');
    });
});
