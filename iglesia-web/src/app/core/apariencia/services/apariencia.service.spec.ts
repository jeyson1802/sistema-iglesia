import { TestBed } from '@angular/core/testing';
import { Apariencia, APARIENCIA_SISTEMA } from '../models/apariencia';
import { AparienciaService, CLAVE_APARIENCIA, VARIABLE_FUENTE_MARCA } from './apariencia.service';

describe('AparienciaService', () => {
    const LARA: Apariencia = { preset: 'Lara', colorPrimario: 'indigo', colorSuperficie: 'zinc', tipografia: 'Cinzel' };
    let servicio: AparienciaService;

    beforeEach(() => {
        localStorage.removeItem(CLAVE_APARIENCIA);
        TestBed.configureTestingModule({});
        servicio = TestBed.inject(AparienciaService);
    });

    afterEach(() => {
        servicio.aplicar(APARIENCIA_SISTEMA, false);
        localStorage.removeItem(CLAVE_APARIENCIA);
    });

    it('aplica preset, colores y tipografía de marca, y la recuerda para la siguiente visita', () => {
        servicio.aplicar(LARA);

        expect(document.documentElement.style.getPropertyValue(VARIABLE_FUENTE_MARCA)).toBe("'Cinzel', serif");
        expect(document.getElementById('fuente-marca-cinzel')?.getAttribute('href')).toContain('family=Cinzel:wght@600&display=swap');
        expect(JSON.parse(localStorage.getItem(CLAVE_APARIENCIA)!)).toEqual(LARA);
        expect(servicio.actual()).toEqual(LARA);
    });

    it('sin recordar no toca la guardada (p. ej. la vista previa o la del sistema en el login)', () => {
        servicio.aplicar(LARA);
        servicio.aplicar(APARIENCIA_SISTEMA, false);

        expect(JSON.parse(localStorage.getItem(CLAVE_APARIENCIA)!)).toEqual(LARA);
        expect(servicio.actual()).toEqual(APARIENCIA_SISTEMA);
    });

    it('al arrancar aplica la última apariencia recordada o, si no hay, la del sistema', () => {
        servicio.aplicarRecordada();
        expect(servicio.actual()).toEqual(APARIENCIA_SISTEMA);

        localStorage.setItem(CLAVE_APARIENCIA, JSON.stringify(LARA));
        servicio.aplicarRecordada();
        expect(servicio.actual()).toEqual(LARA);
    });

    it('un valor desconocido o una caché ilegible cae en el del sistema', () => {
        servicio.aplicar({ preset: 'Material', colorPrimario: 'marron', colorSuperficie: 'azul', tipografia: 'Arial' } as unknown as Apariencia, false);
        expect(servicio.actual()).toEqual(APARIENCIA_SISTEMA);

        localStorage.setItem(CLAVE_APARIENCIA, '{roto');
        servicio.aplicarRecordada();
        expect(servicio.actual()).toEqual(APARIENCIA_SISTEMA);
    });

    it('carga cada fuente una sola vez', () => {
        servicio.cargarFuentes(['Poppins', 'Poppins']);
        servicio.cargarFuentes(['Poppins']);

        expect(document.querySelectorAll('#fuente-marca-poppins').length).toBe(1);
    });
});
