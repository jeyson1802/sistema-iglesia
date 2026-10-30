import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FiltroLote } from '../../models/consolidacion';
import { RondasService } from '../../services/rondas.service';
import { SelectorLoteComponent } from './selector-lote';

describe('SelectorLoteComponent', () => {
    let fixture: ComponentFixture<SelectorLoteComponent>;
    let rondas: jasmine.SpyObj<RondasService>;
    const html = (): HTMLElement => fixture.nativeElement;
    const refrescar = async (): Promise<void> => {
        fixture.detectChanges();
        await fixture.whenStable();
    };
    const chip = (texto: string): HTMLButtonElement => Array.from(html().querySelectorAll<HTMLButtonElement>('button[aria-pressed]')).find((boton) => boton.textContent!.includes(texto))!;

    beforeEach(async () => {
        jasmine.clock().install();
        jasmine.clock().mockDate(new Date(2026, 10, 10, 12));
        rondas = jasmine.createSpyObj<RondasService>('RondasService', ['vistaPreviaLote']);
        rondas.vistaPreviaLote.and.returnValue(of({ entrarian: 37, conCasoAbierto: 4, personas: [{ miembroId: 1, nombreCompleto: 'Ana Quispe' }] }));
        TestBed.configureTestingModule({ providers: [{ provide: RondasService, useValue: rondas }] });
        fixture = TestBed.createComponent(SelectorLoteComponent);
        fixture.componentRef.setInput('rondaId', 7);
        await refrescar();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('pide la vista previa una sola vez tras dejar de cambiar los filtros', async () => {
        jasmine.clock().tick(300);
        chip('Nuevos').click();
        chip('Sin grupo').click();
        await refrescar();
        jasmine.clock().tick(299);
        expect(rondas.vistaPreviaLote).toHaveBeenCalledTimes(1);
        jasmine.clock().tick(1);
        expect(rondas.vistaPreviaLote).toHaveBeenCalledTimes(2);
        const filtro = rondas.vistaPreviaLote.calls.mostRecent().args[1] as FiltroLote;
        expect(filtro.condicion).toBe('NUEVO');
        expect(filtro.sinGrupo).toBeTrue();
    });

    it('muestra cuántas entrarían y cuántas ya tienen seguimiento', async () => {
        jasmine.clock().tick(300);
        await refrescar();
        expect(html().textContent).toContain('37');
        expect(html().textContent).toContain('4 ya tienen un seguimiento abierto (no se agregan).');
        expect(fixture.componentInstance.entrarian()).toBe(37);
    });

    it('las alertas se excluyen entre sí', async () => {
        chip('Nuevo sin volver').click();
        chip('Faltas seguidas').click();
        await refrescar();
        expect(fixture.componentInstance.filtro().alerta).toBe('FALTAS_SEGUIDAS');
        expect(chip('Nuevo sin volver').getAttribute('aria-pressed')).toBe('false');
    });

    it('muestra el nombre completo de cada persona que se agregaría, paginado', async () => {
        const personas = Array.from({ length: 12 }, (_, indice) => ({ miembroId: indice + 1, nombreCompleto: `Persona Número ${indice + 1}` }));
        rondas.vistaPreviaLote.and.returnValue(of({ entrarian: 12, conCasoAbierto: 0, personas }));
        chip('Nuevos').click();
        await refrescar();
        jasmine.clock().tick(300);
        await refrescar();
        const nombres = Array.from(html().querySelectorAll('ul[aria-labelledby="lote-personas"] li')).map((li) => li.textContent!.trim());
        expect(nombres.length).toBe(10);
        expect(nombres[0]).toContain('Persona Número 1');
        expect(html().textContent).toContain('Se agregarán:');
    });

    it('muestra siempre el paginador de la lista', async () => {
        jasmine.clock().tick(300);
        await refrescar();
        expect(html().querySelector('p-paginator')).not.toBeNull();
    });
});
