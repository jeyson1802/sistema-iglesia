import { HttpErrorResponse } from '@angular/common/http';
import { detalleDeError, erroresPorCampo } from './detalle-de-error';

describe('detalleDeError', () => {
    const error = (status: number, cuerpo: unknown = null): HttpErrorResponse => new HttpErrorResponse({ status, error: cuerpo });

    it('muestra el detalle de un 400, 404 o 409', () => {
        expect(detalleDeError(error(409, { detail: 'Ya existe' }), 'Genérico')).toBe('Ya existe');
        expect(detalleDeError(error(404, { detail: 'No existe' }), 'Genérico')).toBe('No existe');
    });

    it('usa el genérico para otros 4xx o sin detalle', () => {
        expect(detalleDeError(error(403, { detail: 'Prohibido' }), 'Genérico')).toBe('Genérico');
        expect(detalleDeError(error(400), 'Genérico')).toBe('Genérico');
    });

    it('no repite los errores que avisa el toast global', () => {
        expect(detalleDeError(error(500, { detail: 'x' }), 'Genérico')).toBeNull();
        expect(detalleDeError(error(0), 'Genérico')).toBeNull();
    });
});

describe('erroresPorCampo', () => {
    it('devuelve los errores por campo del ProblemDetail', () => {
        expect(erroresPorCampo(new HttpErrorResponse({ status: 409, error: { errores: { liderId: 'Ciclo' } } }))).toEqual({ liderId: 'Ciclo' });
    });

    it('sin errores por campo devuelve un objeto vacío', () => {
        expect(erroresPorCampo(new HttpErrorResponse({ status: 409, error: { detail: 'x' } }))).toEqual({});
        expect(erroresPorCampo(new Error('x'))).toEqual({});
    });
});
