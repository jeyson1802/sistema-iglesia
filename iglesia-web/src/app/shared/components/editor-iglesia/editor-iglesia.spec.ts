import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Apariencia, APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';
import { FuenteIglesia } from '@/app/core/iglesias/models/fuente-iglesia';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { iglesiaPrueba } from '@/app/core/iglesias/testing/iglesia-prueba';
import { UbigeoService } from '@/app/shared/components/selector-ubigeo/ubigeo.service';
import { EditorIglesiaComponent } from './editor-iglesia';

describe('EditorIglesiaComponent', () => {
    const LARA: Apariencia = { preset: 'Lara', colorPrimario: 'indigo', colorSuperficie: null, tipografia: 'Cinzel' };
    let fuente: jasmine.SpyObj<FuenteIglesia>;
    let temas: jasmine.SpyObj<AparienciaService>;
    let identidad: { identidad: ReturnType<typeof signal<Iglesia | null>>; refrescar: jasmine.Spy; recargarLogo: jasmine.Spy; nombreComercial: () => null; urlLogo: () => null; cargando: () => boolean };
    let fixture: ComponentFixture<EditorIglesiaComponent>;

    const crear = async (iglesia: Iglesia = iglesiaPrueba()): Promise<void> => {
        fuente.obtener.and.returnValue(of(iglesia));
        await TestBed.configureTestingModule({
            imports: [EditorIglesiaComponent],
            providers: [
                { provide: AparienciaService, useValue: temas },
                { provide: IdentidadIglesiaService, useValue: identidad },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(EditorIglesiaComponent);
        fixture.componentRef.setInput('fuente', fuente);
        await fixture.whenStable();
    };

    beforeEach(() => {
        fuente = jasmine.createSpyObj<FuenteIglesia>('FuenteIglesia', ['obtener', 'actualizar', 'cambiarApariencia', 'logo', 'subirLogo', 'quitarLogo']);
        fuente.logo.and.returnValue(of(new Blob(['png'], { type: 'image/png' })));
        temas = jasmine.createSpyObj<AparienciaService>('AparienciaService', ['aplicar', 'cargarFuentes'], { actual: signal(APARIENCIA_SISTEMA) });
        identidad = { identidad: signal<Iglesia | null>(iglesiaPrueba()), refrescar: jasmine.createSpy('refrescar'), recargarLogo: jasmine.createSpy('recargarLogo'), nombreComercial: () => null, urlLogo: () => null, cargando: () => false };
    });

    const html = (): HTMLElement => fixture.nativeElement;
    const componente = () => fixture.componentInstance;
    const boton = (texto: string): HTMLButtonElement => Array.from(html().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent?.includes(texto))!;

    it('carga la iglesia en sus tres secciones con el formulario lleno', async () => {
        await crear();

        expect(Array.from(html().querySelectorAll('h2')).map((h) => h.textContent?.trim())).toEqual(['Datos de la iglesia', 'Logo', 'Apariencia']);
        expect((html().querySelector('#editor-iglesia-nombre-comercial') as HTMLInputElement).value).toBe('Sion');
    });

    it('a una iglesia sin dirección le muestra el aviso sin bloquear la edición', async () => {
        await crear(iglesiaPrueba({ direccion: null, distritoCodigo: null, ubicacion: null }));

        expect(html().textContent).toContain('Completa la dirección de tu iglesia');
        expect(boton('Guardar datos').disabled).toBeFalse();
    });

    it('guarda los datos y refresca la identidad de la sesión', async () => {
        await crear();
        const actualizada = iglesiaPrueba({ nombreComercial: 'Sion Norte' });
        fuente.actualizar.and.returnValue(of(actualizada));

        componente()['guardarDatos']();

        expect(fuente.actualizar).toHaveBeenCalledWith(jasmine.objectContaining({ nombreComercial: 'Sion', distritoCodigo: '150132' }));
        expect(identidad.refrescar).toHaveBeenCalledOnceWith(actualizada);
    });

    it('muestra los errores del backend junto a cada campo', async () => {
        await crear();
        fuente.actualizar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409, error: { detail: 'Conflicto', errores: { nombreComercial: 'Ya existe una iglesia con ese nombre comercial' } } })));

        componente()['guardarDatos']();
        await fixture.whenStable();

        expect(html().textContent).toContain('Ya existe una iglesia con ese nombre comercial');
    });

    it('aplica la apariencia en vivo y, al descartar, vuelve a la guardada', async () => {
        await crear();

        componente()['apariencia'].set(LARA);
        await fixture.whenStable();
        expect(temas.aplicar).toHaveBeenCalledWith(LARA, false);
        expect(boton('Guardar apariencia').disabled).toBeFalse();

        boton('Descartar cambios').click();
        await fixture.whenStable();
        expect(temas.aplicar).toHaveBeenCalledWith(APARIENCIA_SISTEMA, false);
        expect(boton('Guardar apariencia').disabled).toBeTrue();
    });

    it('al salir sin guardar vuelve la apariencia que había al entrar', async () => {
        await crear();
        componente()['apariencia'].set(LARA);
        await fixture.whenStable();
        temas.aplicar.calls.reset();

        fixture.destroy();

        expect(temas.aplicar).toHaveBeenCalledOnceWith(APARIENCIA_SISTEMA, false);
    });

    it('guardar la apariencia de la iglesia de la sesión la deja para todos sus usuarios', async () => {
        await crear();
        fuente.cambiarApariencia.and.returnValue(of(LARA));
        componente()['apariencia'].set(LARA);

        componente()['guardarApariencia']();

        expect(fuente.cambiarApariencia).toHaveBeenCalledOnceWith(LARA);
        expect(identidad.refrescar).toHaveBeenCalledWith(jasmine.objectContaining({ apariencia: LARA }));
    });

    it('sube el logo, lo muestra y avisa a la identidad de la sesión', async () => {
        await crear();
        fuente.subirLogo.and.returnValue(of(undefined));

        componente()['subirLogo'](new File([new Uint8Array(10)], 'logo.png', { type: 'image/png' }));
        await fixture.whenStable();

        expect(identidad.recargarLogo).toHaveBeenCalledOnceWith(5, true);
        expect(html().querySelector('app-selector-logo img')?.getAttribute('src')).toMatch(/^blob:/);
    });

    it('un logo demasiado grande para el servidor (413) se explica junto al logo', async () => {
        await crear();
        fuente.subirLogo.and.returnValue(throwError(() => new HttpErrorResponse({ status: 413 })));

        componente()['subirLogo'](new File([new Uint8Array(10)], 'logo.png', { type: 'image/png' }));
        await fixture.whenStable();

        expect(html().textContent).toContain('La imagen pesa más de 1 MB; elige una de hasta 1 MB.');
    });

    it('si no puede cargar la iglesia permite reintentar', async () => {
        fuente.obtener.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
        await TestBed.configureTestingModule({
            imports: [EditorIglesiaComponent],
            providers: [
                { provide: AparienciaService, useValue: temas },
                { provide: IdentidadIglesiaService, useValue: identidad },
                { provide: UbigeoService, useValue: { departamentos: () => of([]), provincias: () => of([]), distritos: () => of([]) } }
            ]
        }).compileComponents();
        fixture = TestBed.createComponent(EditorIglesiaComponent);
        fixture.componentRef.setInput('fuente', fuente);
        await fixture.whenStable();

        expect(html().querySelector('[role="alert"]')?.textContent).toContain('No se pudo cargar la iglesia.');
    });
});
