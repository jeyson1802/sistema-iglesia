import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfiguracionAportes } from '../../models/configuracion-aportes';
import { SelectorMonedaComponent } from './selector-moneda';

describe('SelectorMonedaComponent', () => {
    const monedas = [
        { codigo: 'PEN', nombre: 'sol peruano', simbolo: 'S/' },
        { codigo: 'USD', nombre: 'dólar estadounidense', simbolo: 'US$' }
    ];
    let fixture: ComponentFixture<SelectorMonedaComponent>;
    let guardadas: string[];

    const crear = async (configuracion: ConfiguracionAportes): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [SelectorMonedaComponent] }).compileComponents();
        fixture = TestBed.createComponent(SelectorMonedaComponent);
        fixture.componentRef.setInput('configuracion', configuracion);
        fixture.componentRef.setInput('monedas', monedas);
        guardadas = [];
        fixture.componentInstance.guardar.subscribe((moneda) => guardadas.push(moneda));
        await fixture.whenStable();
    };
    const html = (): HTMLElement => fixture.nativeElement;
    const boton = (): HTMLButtonElement | undefined => Array.from(html().querySelectorAll('button')).find((b) => b.textContent?.includes('Guardar moneda'));
    const componente = () => fixture.componentInstance as unknown as { seleccion: { set: (valor: string) => void }; opciones: () => { etiqueta: string }[]; enviar: () => void };

    it('muestra las monedas con símbolo, nombre y código y solo guarda si cambia', async () => {
        await crear({ moneda: 'PEN', monedaEditable: true });

        expect(componente().opciones()[0].etiqueta).toBe('S/ · Sol peruano (PEN)');
        expect(boton()?.disabled).toBeTrue();

        componente().seleccion.set('USD');
        await fixture.whenStable();
        boton()?.click();

        expect(guardadas).toEqual(['USD']);
    });

    it('con aportes registrados la bloquea y explica por qué', async () => {
        await crear({ moneda: 'PEN', monedaEditable: false });

        expect(boton()).toBeUndefined();
        expect(html().textContent).toContain('no se puede cambiar');
    });
});
