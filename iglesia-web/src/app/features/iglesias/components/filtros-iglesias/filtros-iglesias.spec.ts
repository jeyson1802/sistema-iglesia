import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { FiltroIglesias } from '@/app/core/iglesias/models/iglesia';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { ESPERA_BUSQUEDA_MS, FiltrosIglesiasComponent } from './filtros-iglesias';

describe('FiltrosIglesiasComponent', () => {
    let fixture: ComponentFixture<FiltrosIglesiasComponent>;
    let emitidos: FiltroIglesias[];

    beforeEach(async () => {
        // `debounceTime` mide con `Date.now()`: el reloj simulado debe moverlo también.
        jasmine.clock().install().mockDate();
        const ubigeo = jasmine.createSpyObj<UbigeoService>('UbigeoService', ['departamentos', 'provincias', 'distritos']);
        ubigeo.departamentos.and.returnValue(of([]));
        ubigeo.provincias.and.returnValue(of([]));
        ubigeo.distritos.and.returnValue(of([]));
        await TestBed.configureTestingModule({ imports: [FiltrosIglesiasComponent], providers: [{ provide: UbigeoService, useValue: ubigeo }] }).compileComponents();
        fixture = TestBed.createComponent(FiltrosIglesiasComponent);
        fixture.componentRef.setInput('filtro', { q: null, ubigeo: null, activa: null });
        emitidos = [];
        fixture.componentInstance.filtrar.subscribe((filtro) => emitidos.push(filtro));
        await fixture.whenStable();
    });

    afterEach(() => jasmine.clock().uninstall());

    const html = (): HTMLElement => fixture.nativeElement;
    const buscador = (): HTMLInputElement => html().querySelector('#filtro-iglesia-nombre')!;

    it('busca el texto 300 ms después de la última tecla', () => {
        buscador().value = 'sion';
        buscador().dispatchEvent(new Event('input'));
        jasmine.clock().tick(ESPERA_BUSQUEDA_MS - 1);
        expect(emitidos).toEqual([]);

        jasmine.clock().tick(1);
        expect(emitidos).toEqual([{ q: 'sion', ubigeo: null, activa: null }]);
    });

    it('refleja el filtro de la URL sin volver a emitirlo', async () => {
        fixture.componentRef.setInput('filtro', { q: 'betel', ubigeo: '15', activa: true });
        await fixture.whenStable();

        expect(buscador().value).toBe('betel');
        expect(emitidos).toEqual([]);
    });

    it('muestra los filtros activos como chips que se pueden quitar o limpiar', async () => {
        fixture.componentRef.setInput('filtro', { q: 'betel', ubigeo: '15', activa: false });
        await fixture.whenStable();

        const chips = Array.from(html().querySelectorAll('p-chip')).map((chip) => chip.textContent?.trim());
        expect(chips).toEqual(['«betel»', 'Ubicación 15', 'Inactivas']);

        Array.from(html().querySelectorAll<HTMLButtonElement>('button'))
            .find((b) => b.textContent?.includes('Limpiar filtros'))!
            .click();
        expect(emitidos.at(-1)).toEqual({ q: null, ubigeo: null, activa: null });
    });
});
