import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MiembroReunion } from '../../models/asistencia';
import { BETO, CARLA } from '../../testing/asistencia-prueba';
import { ESPERA_BUSQUEDA_MS, MarcadoManualComponent } from './marcado-manual';

describe('MarcadoManualComponent', () => {
    let fixture: ComponentFixture<MarcadoManualComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [MarcadoManualComponent] }).compileComponents();
        fixture = TestBed.createComponent(MarcadoManualComponent);
        fixture.componentRef.setInput('resultados', []);
        fixture.detectChanges();
        jasmine.clock().install().mockDate();
    });

    afterEach(() => jasmine.clock().uninstall());

    const tick = (ms: number): void => jasmine.clock().tick(ms);
    const html = (): HTMLElement => fixture.nativeElement;
    const escribir = (texto: string): void => {
        const campo = html().querySelector('input') as HTMLInputElement;
        campo.value = texto;
        campo.dispatchEvent(new Event('input'));
    };

    it('busca al dejar de escribir y solo con 2 letras o más', () => {
        const buscados: string[] = [];
        fixture.componentInstance.buscar.subscribe((texto) => buscados.push(texto));

        escribir('a');
        tick(ESPERA_BUSQUEDA_MS);
        escribir(' an');
        tick(100);
        escribir(' ana ');
        tick(ESPERA_BUSQUEDA_MS);

        expect(buscados).toEqual(['ana']);
    });

    it('cada resultado ofrece «Marcar» o, si ya está, «Presente» para quitarlo, con paginador', () => {
        const marcados: MiembroReunion[] = [];
        const quitados: MiembroReunion[] = [];
        fixture.componentInstance.marcar.subscribe((miembro) => marcados.push(miembro));
        fixture.componentInstance.quitar.subscribe((miembro) => quitados.push(miembro));
        escribir('ruiz');
        tick(ESPERA_BUSQUEDA_MS);
        fixture.componentRef.setInput('resultados', [BETO, CARLA]);
        fixture.detectChanges();

        (html().querySelector('button[aria-label="Marcar a Beto Ruiz"]') as HTMLButtonElement).click();
        (html().querySelector('button[aria-label="Carla Díaz está presente. Quitar"]') as HTMLButtonElement).click();

        expect(marcados).toEqual([BETO]);
        expect(quitados).toEqual([CARLA]);
        expect(html().querySelector('.p-paginator')).toBeTruthy();
    });

    it('sin resultados lo dice', () => {
        escribir('zzz');
        tick(ESPERA_BUSQUEDA_MS);
        fixture.detectChanges();

        expect(html().textContent).toContain('No se encontró a nadie activo con «zzz».');
    });
});
