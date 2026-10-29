import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FILTRO_SEGUIMIENTO_INICIAL, FiltroSeguimiento } from '../../models/seguimiento';
import { ESPERA_BUSQUEDA_MS, FiltrosSeguimientoComponent } from './filtros-seguimiento';

describe('FiltrosSeguimientoComponent', () => {
    let fixture: ComponentFixture<FiltrosSeguimientoComponent>;
    let emitidos: FiltroSeguimiento[];

    beforeEach(() => {
        fixture = TestBed.createComponent(FiltrosSeguimientoComponent);
        fixture.componentRef.setInput('filtro', FILTRO_SEGUIMIENTO_INICIAL);
        fixture.componentRef.setInput('conteos', { conAlertas: 12, faltasSeguidas: 9, nuevoSinVolver: 2, asistenciaEnCaida: 4 });
        emitidos = [];
        fixture.componentInstance.filtrar.subscribe((filtro) => emitidos.push(filtro));
        fixture.detectChanges();
    });

    afterEach(() => jasmine.clock().uninstall());

    it('muestra una opción por alerta con su cantidad y avisa la elegida', async () => {
        await fixture.whenStable();
        fixture.detectChanges();
        const opciones = Array.from(fixture.nativeElement.querySelectorAll('p-selectbutton p-togglebutton')) as HTMLElement[];
        expect(opciones.map((opcion) => opcion.textContent?.replace(/\s+/g, '').trim())).toEqual(['Todaslasalertas12', 'Faltasseguidas9', 'Nuevosinvolver2', 'Asistenciaencaída4']);
        expect(opciones[0].getAttribute('aria-pressed')).toBe('true');
        opciones[1].click();
        expect(emitidos).toEqual([{ ...FILTRO_SEGUIMIENTO_INICIAL, alerta: 'FALTAS_SEGUIDAS' }]);
        fixture.componentRef.setInput('filtro', emitidos[0]);
        fixture.detectChanges();
        await fixture.whenStable();
        opciones[0].click();
        expect(emitidos[1]).toEqual(FILTRO_SEGUIMIENTO_INICIAL);
    });

    it('busca el nombre al dejar de escribir', () => {
        jasmine.clock().install().mockDate();
        const campo = fixture.nativeElement.querySelector('#filtro-seguimiento-nombre') as HTMLInputElement;
        campo.value = 'ana ';
        campo.dispatchEvent(new Event('input'));
        expect(emitidos).toEqual([]);
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS);
        expect(emitidos).toEqual([{ ...FILTRO_SEGUIMIENTO_INICIAL, q: 'ana' }]);
    });

    it('solo ofrece el filtro por grupo si hay grupos', () => {
        expect(fixture.nativeElement.querySelector('#filtro-seguimiento-grupo')).toBeNull();
        fixture.componentRef.setInput('grupos', [{ id: 1, nombre: 'Célula de Ana' }]);
        fixture.detectChanges();
        expect(fixture.nativeElement.querySelector('p-select')).not.toBeNull();
    });
});
