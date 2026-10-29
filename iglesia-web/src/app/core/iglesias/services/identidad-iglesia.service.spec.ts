import { computed, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { Apariencia, APARIENCIA_SISTEMA } from '@/app/core/apariencia/models/apariencia';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { Iglesia } from '../models/iglesia';
import { iglesiaPrueba } from '../testing/iglesia-prueba';
import { IdentidadIglesiaService } from './identidad-iglesia.service';
import { MiIglesiaService } from './mi-iglesia.service';

describe('IdentidadIglesiaService', () => {
    const LARA: Apariencia = { preset: 'Lara', colorPrimario: 'indigo', colorSuperficie: null, tipografia: 'Cinzel' };
    const iglesiaId = signal<number | null>(null);
    let miIglesia: jasmine.SpyObj<MiIglesiaService>;
    let apariencia: jasmine.SpyObj<AparienciaService>;
    let servicio: IdentidadIglesiaService;

    const crear = (): void => {
        servicio = TestBed.inject(IdentidadIglesiaService);
        TestBed.tick();
    };

    beforeEach(() => {
        iglesiaId.set(null);
        miIglesia = jasmine.createSpyObj<MiIglesiaService>('MiIglesiaService', ['obtener', 'logo']);
        miIglesia.obtener.and.returnValue(of(iglesiaPrueba({ apariencia: LARA })));
        miIglesia.logo.and.returnValue(of(new Blob(['png'], { type: 'image/png' })));
        apariencia = jasmine.createSpyObj<AparienciaService>('AparienciaService', ['aplicar']);
        TestBed.configureTestingModule({
            providers: [
                { provide: SesionService, useValue: { usuarioActual: computed(() => ({ iglesiaId: iglesiaId() })) } },
                { provide: MiIglesiaService, useValue: miIglesia },
                { provide: AparienciaService, useValue: apariencia }
            ]
        });
    });

    it('sin iglesia aplica la apariencia del sistema sin olvidar la recordada', () => {
        crear();

        expect(servicio.identidad()).toBeNull();
        expect(miIglesia.obtener).not.toHaveBeenCalled();
        expect(apariencia.aplicar).toHaveBeenCalledOnceWith(APARIENCIA_SISTEMA, false);
    });

    it('con iglesia carga su identidad y aplica (y recuerda) su apariencia', () => {
        iglesiaId.set(5);
        crear();

        expect(servicio.nombreComercial()).toBe('Sion');
        expect(apariencia.aplicar).toHaveBeenCalledWith(LARA);
        expect(miIglesia.logo).not.toHaveBeenCalled();
        expect(servicio.urlLogo()).toBeNull();
    });

    it('si la iglesia tiene logo lo pide con el token y expone una URL de objeto', () => {
        miIglesia.obtener.and.returnValue(of(iglesiaPrueba({ tieneLogo: true })));
        iglesiaId.set(5);
        crear();

        expect(servicio.urlLogo()).toMatch(/^blob:/);
    });

    it('al salir de la iglesia limpia la identidad y libera el logo', () => {
        miIglesia.obtener.and.returnValue(of(iglesiaPrueba({ tieneLogo: true })));
        iglesiaId.set(5);
        crear();
        const revocar = spyOn(URL, 'revokeObjectURL').and.callThrough();

        iglesiaId.set(null);
        TestBed.tick();

        expect(servicio.identidad()).toBeNull();
        expect(servicio.urlLogo()).toBeNull();
        expect(revocar).toHaveBeenCalled();
    });

    it('muestra el esqueleto mientras carga y, si falla, sigue con la identidad neutra', () => {
        const respuesta = new Subject<Iglesia>();
        miIglesia.obtener.and.returnValue(respuesta);
        iglesiaId.set(5);
        crear();
        expect(servicio.cargando()).toBeTrue();

        respuesta.error(new Error('500'));

        expect(servicio.cargando()).toBeFalse();
        expect(servicio.identidad()).toBeNull();
    });

    it('refresca la identidad solo si es la iglesia de la sesión', () => {
        iglesiaId.set(5);
        crear();

        servicio.refrescar(iglesiaPrueba({ id: 99, nombreComercial: 'Otra' }));
        expect(servicio.nombreComercial()).toBe('Sion');

        servicio.refrescar(iglesiaPrueba({ nombreComercial: 'Sion Norte', apariencia: APARIENCIA_SISTEMA }));
        expect(servicio.nombreComercial()).toBe('Sion Norte');
        expect(apariencia.aplicar).toHaveBeenCalledWith(APARIENCIA_SISTEMA);
    });

    it('recarga el logo tras subirlo o quitarlo', () => {
        iglesiaId.set(5);
        crear();

        servicio.recargarLogo(5, true);
        expect(servicio.urlLogo()).toMatch(/^blob:/);

        servicio.recargarLogo(5, false);
        expect(servicio.urlLogo()).toBeNull();
        miIglesia.logo.and.returnValue(throwError(() => new Error('404')));
        servicio.recargarLogo(5, true);
        expect(servicio.urlLogo()).toBeNull();
    });
});
