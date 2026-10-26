import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CodigoSeguimientoComponent, DURACION_AVISO_COPIA_MS } from './codigo-seguimiento';

describe('CodigoSeguimientoComponent', () => {
    const CODIGO = '4bf92f3577b34da6a3ce929d0e0e4736';
    let fixture: ComponentFixture<CodigoSeguimientoComponent>;
    let escribir: jasmine.Spy<(texto: string) => Promise<void>>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [CodigoSeguimientoComponent] }).compileComponents();
        escribir = spyOn(navigator.clipboard, 'writeText').and.resolveTo();
        fixture = TestBed.createComponent(CodigoSeguimientoComponent);
        fixture.componentRef.setInput('codigo', CODIGO);
        await fixture.whenStable();
    });

    const host = (): HTMLElement => fixture.nativeElement;
    const boton = (): HTMLButtonElement => host().querySelector('button')!;
    const anuncio = (): string => host().querySelector('[aria-live]')!.textContent!.trim();

    async function pulsarCopiar(): Promise<void> {
        boton().click();
        await fixture.whenStable();
    }

    it('muestra el código y un botón «Copiar» accesible', () => {
        expect(host().querySelector('code')?.textContent).toBe(CODIGO);
        expect(boton().textContent).toContain('Copiar');
        expect(boton().getAttribute('aria-label')).toBe('Copiar código de seguimiento');
    });

    it('copia el código al portapapeles y avisa «Copiado»', async () => {
        await pulsarCopiar();

        expect(escribir).toHaveBeenCalledOnceWith(CODIGO);
        expect(boton().textContent).toContain('Copiado');
        expect(anuncio()).toBe('Código copiado al portapapeles');
    });

    it('vuelve a «Copiar» pasado el aviso', async () => {
        jasmine.clock().install();
        try {
            await pulsarCopiar();
            jasmine.clock().tick(DURACION_AVISO_COPIA_MS);
            await fixture.whenStable();

            expect(boton().textContent).toContain('Copiar');
            expect(boton().textContent).not.toContain('Copiado');
        } finally {
            jasmine.clock().uninstall();
        }
    });

    it('si el portapapeles falla, pide copiarlo a mano', async () => {
        escribir.and.rejectWith(new DOMException('Sin permiso', 'NotAllowedError'));

        await pulsarCopiar();

        expect(anuncio()).toContain('No se pudo copiar');
        expect(boton().textContent).toContain('Copiar');
    });
});
