import { TestBed } from '@angular/core/testing';
import { AvisosMarca } from './avisos-marca';

describe('AvisosMarca', () => {
    it('vibra distinto según el resultado y no falla aunque no haya audio', () => {
        const vibrar = jasmine.createSpy('vibrate');
        const original = Object.getOwnPropertyDescriptor(Navigator.prototype, 'vibrate');
        Object.defineProperty(navigator, 'vibrate', { value: vibrar, configurable: true });
        const avisos = TestBed.inject(AvisosMarca);

        try {
            avisos.avisar('marcado');
            avisos.avisar('error');

            expect(vibrar).toHaveBeenCalledWith([80]);
            expect(vibrar).toHaveBeenCalledWith([120, 80, 120]);
        } finally {
            delete (navigator as unknown as Record<string, unknown>)['vibrate'];
            if (original) {
                Object.defineProperty(Navigator.prototype, 'vibrate', original);
            }
        }
    });
});
