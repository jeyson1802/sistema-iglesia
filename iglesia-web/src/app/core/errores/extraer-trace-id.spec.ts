import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { CABECERA_TRACE_ID, extraerTraceId } from './extraer-trace-id';

describe('extraerTraceId', () => {
    const TRACE_CUERPO = '4bf92f3577b34da6a3ce929d0e0e4736';
    const TRACE_CABECERA = '0af7651916cd43dd8448eb211c80319c';
    const conCabecera = new HttpHeaders({ [CABECERA_TRACE_ID]: TRACE_CABECERA });

    it('prefiere el traceId del ProblemDetail', () => {
        const error = new HttpErrorResponse({ status: 500, error: { title: 'Error interno', traceId: TRACE_CUERPO }, headers: conCabecera });

        expect(extraerTraceId(error)).toBe(TRACE_CUERPO);
    });

    it('usa la cabecera si el cuerpo no trae traceId', () => {
        expect(extraerTraceId(new HttpErrorResponse({ status: 503, error: null, headers: conCabecera }))).toBe(TRACE_CABECERA);
        expect(extraerTraceId(new HttpErrorResponse({ status: 502, error: '<html>Bad Gateway</html>', headers: conCabecera }))).toBe(TRACE_CABECERA);
    });

    it('devuelve null si no hay código en el cuerpo ni en la cabecera', () => {
        expect(extraerTraceId(new HttpErrorResponse({ status: 0, error: new ProgressEvent('error') }))).toBeNull();
        expect(extraerTraceId(new HttpErrorResponse({ status: 500, error: { traceId: '' } }))).toBeNull();
    });
});
