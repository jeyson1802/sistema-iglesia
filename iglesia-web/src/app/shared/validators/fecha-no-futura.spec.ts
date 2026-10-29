import { FormControl } from '@angular/forms';
import { ERROR_FECHA_FUTURA, fechaNoFutura } from './fecha-no-futura';

describe('fechaNoFutura', () => {
    const validar = (valor: string): unknown =>
        new FormControl(
            valor,
            fechaNoFutura(() => '2026-10-28')
        ).errors;

    it('acepta hoy, el pasado y el vacío', () => {
        expect(validar('2026-10-28')).toBeNull();
        expect(validar('1990-05-14')).toBeNull();
        expect(validar('')).toBeNull();
    });

    it('rechaza una fecha futura', () => {
        expect(validar('2026-10-29')).toEqual({ [ERROR_FECHA_FUTURA]: true });
    });
});
