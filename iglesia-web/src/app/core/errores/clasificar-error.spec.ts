import { HttpErrorResponse } from '@angular/common/http';
import { esErrorDeConexion, esErrorGlobal, esErrorInesperado } from './clasificar-error';

describe('clasificar-error', () => {
    const conStatus = (status: number): HttpErrorResponse => new HttpErrorResponse({ status });

    it('los 5xx son errores inesperados', () => {
        expect(esErrorInesperado(conStatus(500))).toBeTrue();
        expect(esErrorInesperado(conStatus(503))).toBeTrue();
        expect(esErrorInesperado(conStatus(404))).toBeFalse();
    });

    it('status 0 es un error de conexión', () => {
        expect(esErrorDeConexion(conStatus(0))).toBeTrue();
        expect(esErrorDeConexion(conStatus(500))).toBeFalse();
    });

    it('solo los 5xx y la falta de conexión son globales', () => {
        expect(esErrorGlobal(conStatus(500))).toBeTrue();
        expect(esErrorGlobal(conStatus(0))).toBeTrue();
        [400, 401, 403, 404, 409].forEach((status) =>
            expect(esErrorGlobal(conStatus(status)))
                .withContext(`${status}`)
                .toBeFalse()
        );
        expect(esErrorGlobal(new Error('no es HTTP'))).toBeFalse();
    });
});
