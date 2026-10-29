import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Apariencia, APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';
import { SelectorAparienciaComponent } from './selector-apariencia';

describe('SelectorAparienciaComponent', () => {
    let fixture: ComponentFixture<SelectorAparienciaComponent>;
    let apariencia: jasmine.SpyObj<AparienciaService>;
    let emitidas: Apariencia[];

    beforeEach(async () => {
        apariencia = jasmine.createSpyObj<AparienciaService>('AparienciaService', ['cargarFuentes']);
        await TestBed.configureTestingModule({ imports: [SelectorAparienciaComponent], providers: [{ provide: AparienciaService, useValue: apariencia }] }).compileComponents();
        fixture = TestBed.createComponent(SelectorAparienciaComponent);
        fixture.componentRef.setInput('apariencia', APARIENCIA_SISTEMA);
        fixture.componentRef.setInput('nombreComercial', 'MMM Lima Centro');
        emitidas = [];
        fixture.componentInstance.apariencia.subscribe((valor) => emitidas.push(valor));
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const radio = (etiqueta: string): HTMLButtonElement => html().querySelector<HTMLButtonElement>(`button[aria-label="${etiqueta}"]`)!;

    it('carga las cinco fuentes para la vista previa', () => {
        expect(apariencia.cargarFuentes).toHaveBeenCalledOnceWith(['Playfair Display', 'Cinzel', 'Montserrat', 'Merriweather', 'Poppins']);
    });

    it('nombra cada color para los lectores de pantalla y marca el elegido', () => {
        expect(radio('Esmeralda').getAttribute('aria-checked')).toBe('true');
        expect(radio('Índigo').getAttribute('aria-checked')).toBe('false');
        expect(html().querySelectorAll('[role="radiogroup"]').length).toBe(3);
    });

    it('emite cada cambio al momento para la vista previa en vivo', async () => {
        radio('Índigo').click();
        radio('Cinc').click();
        await fixture.whenStable();

        expect(emitidas.at(-1)).toEqual({ ...APARIENCIA_SISTEMA, colorPrimario: 'indigo', colorSuperficie: 'zinc' });
    });

    it('elegir la superficie ya elegida vuelve a la del estilo', async () => {
        radio('Cinc').click();
        radio('Cinc').click();
        await fixture.whenStable();

        expect(emitidas.at(-1)?.colorSuperficie).toBeNull();
    });

    it('muestra el nombre comercial escrito con cada fuente y permite elegirla', async () => {
        const tarjetas = Array.from(html().querySelectorAll<HTMLButtonElement>('[aria-labelledby="apariencia-tipografia"] button'));

        expect(tarjetas.length).toBe(5);
        expect(tarjetas.every((tarjeta) => tarjeta.textContent?.includes('MMM Lima Centro'))).toBeTrue();
        tarjetas.find((tarjeta) => tarjeta.textContent?.includes('Cinzel'))!.click();

        expect(emitidas.at(-1)?.tipografia).toBe('Cinzel');
    });

    it('deshabilitado no cambia nada', async () => {
        fixture.componentRef.setInput('deshabilitado', true);
        await fixture.whenStable();

        (fixture.componentInstance as unknown as { elegirPrimario(c: string): void }).elegirPrimario('rose');

        expect(emitidas).toEqual([]);
    });
});
