import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FileSelectEvent } from 'primeng/fileupload';
import { SelectorLogoComponent } from './selector-logo';

describe('SelectorLogoComponent', () => {
    let fixture: ComponentFixture<SelectorLogoComponent>;
    let componente: SelectorLogoComponent;

    beforeEach(async () => {
        await TestBed.configureTestingModule({ imports: [SelectorLogoComponent] }).compileComponents();
        fixture = TestBed.createComponent(SelectorLogoComponent);
        componente = fixture.componentInstance;
        await fixture.whenStable();
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const archivo = (tipo: string, bytes: number, nombre = 'logo.png'): File => new File([new Uint8Array(bytes)], nombre, { type: tipo });
    const elegir = async (elegido: File): Promise<void> => {
        (componente as unknown as { elegir(e: FileSelectEvent): void }).elegir({ files: [elegido], currentFiles: [elegido], originalEvent: new Event('change') });
        await fixture.whenStable();
    };
    const boton = (texto: string): HTMLButtonElement | undefined => Array.from(html().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(texto));

    it('sin logo lo dice en ambas vistas previas y no ofrece quitarlo', () => {
        expect(html().textContent).toContain('Sin logo');
        expect(boton('Quitar logo')).toBeUndefined();
    });

    it('muestra el logo guardado sobre fondo claro y oscuro', async () => {
        fixture.componentRef.setInput('urlLogo', 'blob:guardado');
        await fixture.whenStable();

        expect(Array.from(html().querySelectorAll('img')).map((img) => img.getAttribute('src'))).toEqual(['blob:guardado', 'blob:guardado']);
        expect(boton('Quitar logo')).toBeDefined();
    });

    it('rechaza un SVG o una imagen de más de 1 MB antes de subir, diciendo cómo corregirlo', async () => {
        await elegir(archivo('image/svg+xml', 10, 'logo.svg'));
        expect(html().textContent).toContain('Elige una imagen PNG, JPEG o WebP');

        await elegir(archivo('image/png', 2.3 * 1024 * 1024));
        expect(html().textContent).toContain('La imagen pesa 2.3 MB; elige una de hasta 1 MB.');
        expect(boton('Subir logo')).toBeUndefined();
    });

    it('muestra la vista previa del archivo elegido y lo emite al subir', async () => {
        const subir = jasmine.createSpy('subir');
        componente.subir.subscribe(subir);
        const elegido = archivo('image/png', 100);

        await elegir(elegido);
        expect(html().querySelector('img')?.getAttribute('src')).toMatch(/^blob:/);
        boton('Subir logo')!.click();

        expect(subir).toHaveBeenCalledOnceWith(elegido);
    });

    it('al cancelar vuelve al logo guardado', async () => {
        fixture.componentRef.setInput('urlLogo', 'blob:guardado');
        await elegir(archivo('image/webp', 100, 'nuevo.webp'));

        boton('Cancelar')!.click();
        await fixture.whenStable();

        expect(html().querySelector('img')?.getAttribute('src')).toBe('blob:guardado');
    });

    it('muestra el error del backend', async () => {
        fixture.componentRef.setInput('error', 'El archivo no es una imagen PNG, JPEG o WebP');
        await fixture.whenStable();

        expect(html().textContent).toContain('El archivo no es una imagen PNG, JPEG o WebP');
    });
});
