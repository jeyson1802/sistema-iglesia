import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { CABECERA_TRACE_ID } from '../errores/extraer-trace-id';
import { NotificadorErroresService } from '../errores/notificador-errores.service';
import { erroresInterceptor } from './errores.interceptor';

describe('erroresInterceptor', () => {
    const URL = '/api/recurso';
    const TRACE_CUERPO = '4bf92f3577b34da6a3ce929d0e0e4736';
    const TRACE_CABECERA = '0af7651916cd43dd8448eb211c80319c';

    let http: HttpClient;
    let backend: HttpTestingController;
    let notificador: jasmine.SpyObj<NotificadorErroresService>;

    beforeEach(() => {
        notificador = jasmine.createSpyObj<NotificadorErroresService>('NotificadorErroresService', ['errorInesperado', 'sinConexion']);
        TestBed.configureTestingModule({
            providers: [provideHttpClient(withInterceptors([erroresInterceptor])), provideHttpClientTesting(), { provide: NotificadorErroresService, useValue: notificador }]
        });
        http = TestBed.inject(HttpClient);
        backend = TestBed.inject(HttpTestingController);
    });

    afterEach(() => backend.verify());

    /** Hace la petición, responde con `responder` y devuelve el error que recibe quien llamó. */
    async function errorRecibido(responder: (peticion: ReturnType<HttpTestingController['expectOne']>) => void): Promise<HttpErrorResponse> {
        const resultado = firstValueFrom(http.get(URL)).then(
            () => fail('debía fallar') as never,
            (error: HttpErrorResponse) => error
        );
        responder(backend.expectOne(URL));
        return resultado;
    }

    it('un 500 con ProblemDetail notifica con su traceId y relanza el error', async () => {
        const error = await errorRecibido((peticion) => peticion.flush({ title: 'Error interno', traceId: TRACE_CUERPO }, { status: 500, statusText: 'Internal Server Error', headers: { [CABECERA_TRACE_ID]: TRACE_CUERPO } }));

        expect(notificador.errorInesperado).toHaveBeenCalledOnceWith(TRACE_CUERPO);
        expect(error.status).toBe(500);
    });

    it('un 503 sin cuerpo usa el código de la cabecera', async () => {
        await errorRecibido((peticion) => peticion.flush(null, { status: 503, statusText: 'Service Unavailable', headers: { [CABECERA_TRACE_ID]: TRACE_CABECERA } }));

        expect(notificador.errorInesperado).toHaveBeenCalledOnceWith(TRACE_CABECERA);
    });

    it('sin respuesta (status 0) avisa de la conexión sin código y relanza el error', async () => {
        const error = await errorRecibido((peticion) => peticion.error(new ProgressEvent('error')));

        expect(notificador.sinConexion).toHaveBeenCalledTimes(1);
        expect(notificador.errorInesperado).not.toHaveBeenCalled();
        expect(error.status).toBe(0);
    });

    for (const status of [400, 401, 404]) {
        it(`un ${status} no notifica y llega a la página`, async () => {
            const error = await errorRecibido((peticion) => peticion.flush({ title: 'Error', traceId: TRACE_CUERPO }, { status, statusText: 'Error' }));

            expect(notificador.errorInesperado).not.toHaveBeenCalled();
            expect(notificador.sinConexion).not.toHaveBeenCalled();
            expect(error.status).toBe(status);
        });
    }

    it('las respuestas exitosas pasan sin tocar', async () => {
        const respuesta = firstValueFrom(http.get(URL));
        backend.expectOne(URL).flush({ ok: true });

        expect(await respuesta).toEqual({ ok: true });
        expect(notificador.errorInesperado).not.toHaveBeenCalled();
    });
});
