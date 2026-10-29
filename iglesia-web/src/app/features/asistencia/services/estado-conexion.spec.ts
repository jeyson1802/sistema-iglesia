import { TestBed } from '@angular/core/testing';
import { EstadoConexion } from './estado-conexion';

describe('EstadoConexion', () => {
    it('sigue los eventos de conexión del navegador', () => {
        TestBed.configureTestingModule({ providers: [EstadoConexion] });
        const conexion = TestBed.inject(EstadoConexion);

        window.dispatchEvent(new Event('offline'));
        expect(conexion.enLinea()).toBeFalse();

        window.dispatchEvent(new Event('online'));
        expect(conexion.enLinea()).toBeTrue();
    });
});
