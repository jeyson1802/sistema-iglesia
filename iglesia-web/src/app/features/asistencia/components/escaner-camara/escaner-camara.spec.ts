import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModoAsistencia } from '../../models/modo-asistencia';
import { CamaraQr, ErrorCamara, SesionCamara } from '../../services/camara-qr';
import { EscanerCamaraComponent } from './escaner-camara';

describe('EscanerCamaraComponent', () => {
    let fixture: ComponentFixture<EscanerCamaraComponent>;
    let alLeer: (texto: string) => void;
    let sesion: jasmine.SpyObj<SesionCamara>;
    let iniciar: jasmine.Spy;

    const crear = async (): Promise<void> => {
        await TestBed.configureTestingModule({ imports: [EscanerCamaraComponent] })
            .overrideComponent(EscanerCamaraComponent, { set: { providers: [{ provide: CamaraQr, useValue: { iniciar } }] } })
            .compileComponents();
        fixture = TestBed.createComponent(EscanerCamaraComponent);
        fixture.detectChanges();
        await fixture.whenStable();
        fixture.detectChanges();
    };
    const html = (): HTMLElement => fixture.nativeElement;

    beforeEach(() => {
        sesion = jasmine.createSpyObj<SesionCamara>('SesionCamara', ['cambiarCamara', 'alternarLinterna', 'detener'], { variasCamaras: true, tieneLinterna: true });
        sesion.alternarLinterna.and.resolveTo(true);
        iniciar = jasmine.createSpy('iniciar').and.callFake(async (_video: HTMLVideoElement, callback: (texto: string) => void) => {
            alLeer = callback;
            return sesion;
        });
    });

    it('emite cada lectura de la cámara y ofrece cambiar de cámara y linterna', async () => {
        await crear();
        const leidos: string[] = [];
        fixture.componentInstance.leido.subscribe((texto) => leidos.push(texto));

        alLeer('ASIS:7K2QM9XDR4TB');

        expect(leidos).toEqual(['ASIS:7K2QM9XDR4TB']);
        expect(html().querySelector('[aria-label="Cambiar de cámara"]')).toBeTruthy();
        expect(html().querySelector('[aria-label="Encender la linterna"]')).toBeTruthy();
    });

    it('en pausa ignora lo que lee', async () => {
        await crear();
        const leidos: string[] = [];
        fixture.componentInstance.leido.subscribe((texto) => leidos.push(texto));
        fixture.componentRef.setInput('pausado', true);

        alLeer('ASIS:7K2QM9XDR4TB');

        expect(leidos).toEqual([]);
    });

    it('sin permiso explica cómo activarlo y ofrece el lector o el modo manual', async () => {
        iniciar.and.rejectWith(new ErrorCamara('sin-permiso'));
        await crear();
        const modos: ModoAsistencia[] = [];
        fixture.componentInstance.cambiarModo.subscribe((modo) => modos.push(modo));

        expect(html().textContent).toContain('El navegador no dio permiso para usar la cámara');
        (Array.from(html().querySelectorAll('button')).find((boton) => boton.textContent?.includes('Usar lector')) as HTMLButtonElement).click();
        expect(modos).toEqual(['LECTOR']);
    });

    it('apaga la cámara al salir', async () => {
        await crear();

        fixture.destroy();

        expect(sesion.detener).toHaveBeenCalled();
    });
});
