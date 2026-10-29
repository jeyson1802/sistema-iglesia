import { TestBed } from '@angular/core/testing';
import { PantallaEncendida } from './pantalla-encendida';

describe('PantallaEncendida', () => {
    it('pide y suelta el bloqueo de pantalla sin fallar aunque el navegador no lo permita', async () => {
        TestBed.configureTestingModule({ providers: [PantallaEncendida] });
        const pantalla = TestBed.inject(PantallaEncendida);

        expect(() => pantalla.mantener()).not.toThrow();
        await Promise.resolve();
        expect(() => pantalla.liberar()).not.toThrow();
    });
});
