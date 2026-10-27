import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { environment } from '@/environments/environment';
import { TokenAcceso } from '../auth/models/token-acceso';
import { SesionService } from '../auth/services/sesion.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
    const URL_API = `${environment.apiUrl}/miembros`;
    const URL_ME = `${environment.seguridadUrl}/usuarios/me`;
    const URL_LOGIN = `${environment.seguridadUrl}/auth/login`;
    const URL_REFRESH = `${environment.seguridadUrl}/auth/refresh`;
    const URL_LOGOUT = `${environment.seguridadUrl}/auth/logout`;
    const token = (valor: string): TokenAcceso => ({ tokenAcceso: valor, tipo: 'Bearer', expiraEn: 900 });
    const NO_AUTORIZADO = { status: 401, statusText: 'Unauthorized' };

    let http: HttpClient;
    let backend: HttpTestingController;
    let sesion: SesionService;
    let router: jasmine.SpyObj<Router>;

    beforeEach(() => {
        sessionStorage.clear();
        router = jasmine.createSpyObj<Router>('Router', ['navigate']);
        router.navigate.and.resolveTo(true);
        TestBed.configureTestingModule({
            providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting(), { provide: Router, useValue: router }]
        });
        http = TestBed.inject(HttpClient);
        backend = TestBed.inject(HttpTestingController);
        sesion = TestBed.inject(SesionService);
    });

    afterEach(() => {
        backend.verify();
        sessionStorage.clear();
    });

    /** Deja una sesión iniciada con el token de acceso indicado. */
    const conSesion = async (tokenAcceso: string): Promise<void> => {
        const restaurada = sesion.restaurar();
        backend.expectOne(URL_REFRESH).flush(token(tokenAcceso));
        backend.expectOne(URL_ME).flush({
            id: 1,
            correo: 'u@i.test',
            iglesiaId: 1,
            miembroId: 2,
            perfiles: [{ id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', permisos: [] }]
        });
        await restaurada;
    };

    it('agrega el token a las peticiones de iglesia-api y de iglesia-seguridad', async () => {
        await conSesion('t1');

        http.get(URL_API).subscribe();
        http.get(URL_ME).subscribe();

        expect(backend.expectOne(URL_API).request.headers.get('Authorization')).toBe('Bearer t1');
        expect(backend.expectOne(URL_ME).request.headers.get('Authorization')).toBe('Bearer t1');
    });

    it('no agrega el token a URLs de otros dominios', async () => {
        await conSesion('t1');

        http.get('https://externo.test/api/datos').subscribe();
        http.get(`${environment.apiUrl}-falso/datos`).subscribe();

        expect(backend.expectOne('https://externo.test/api/datos').request.headers.has('Authorization')).toBeFalse();
        expect(backend.expectOne(`${environment.apiUrl}-falso/datos`).request.headers.has('Authorization')).toBeFalse();
    });

    it('sin sesión no agrega cabecera', () => {
        http.get(URL_API).subscribe();

        expect(backend.expectOne(URL_API).request.headers.has('Authorization')).toBeFalse();
    });

    it('ante un 401 refresca y reintenta con el token nuevo', async () => {
        await conSesion('vencido');
        let respuesta: unknown;

        http.get(URL_API).subscribe((r) => (respuesta = r));
        backend.expectOne(URL_API).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_REFRESH).flush(token('nuevo'));
        const reintento = backend.expectOne(URL_API);
        reintento.flush([{ id: 1 }]);

        expect(reintento.request.headers.get('Authorization')).toBe('Bearer nuevo');
        expect(respuesta).toEqual([{ id: 1 }]);
    });

    it('varias peticiones con 401 simultáneo hacen un único refresco', async () => {
        await conSesion('vencido');
        const respuestas: unknown[] = [];

        http.get(`${URL_API}/1`).subscribe((r) => respuestas.push(r));
        http.get(`${URL_API}/2`).subscribe((r) => respuestas.push(r));
        backend.expectOne(`${URL_API}/1`).flush(null, NO_AUTORIZADO);
        backend.expectOne(`${URL_API}/2`).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_REFRESH).flush(token('nuevo'));
        backend.expectOne(`${URL_API}/1`).flush({ id: 1 });
        backend.expectOne(`${URL_API}/2`).flush({ id: 2 });

        expect(respuestas).toEqual([{ id: 1 }, { id: 2 }]);
    });

    it('reintenta una sola vez: un segundo 401 llega al llamador', async () => {
        await conSesion('vencido');
        let error: HttpErrorResponse | undefined;

        http.get(URL_API).subscribe({ error: (e) => (error = e) });
        backend.expectOne(URL_API).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_REFRESH).flush(token('nuevo'));
        backend.expectOne(URL_API).flush(null, NO_AUTORIZADO);

        expect(error?.status).toBe(401);
    });

    it('no refresca ante un 401 de los endpoints de autenticación', () => {
        let error: HttpErrorResponse | undefined;

        http.post(URL_LOGIN, {}).subscribe({ error: (e) => (error = e) });
        backend.expectOne(URL_LOGIN).flush(null, NO_AUTORIZADO);

        expect(error?.status).toBe(401);
    });

    it('no refresca ante errores distintos de 401', async () => {
        await conSesion('t1');
        let error: HttpErrorResponse | undefined;

        http.get(URL_API).subscribe({ error: (e) => (error = e) });
        backend.expectOne(URL_API).flush(null, { status: 500, statusText: 'Server Error' });

        expect(error?.status).toBe(500);
    });

    it('si el refresco falla cierra la sesión y lleva al login con el aviso de sesión expirada', async () => {
        await conSesion('vencido');
        let error: HttpErrorResponse | undefined;

        http.get(URL_API).subscribe({ error: (e) => (error = e) });
        backend.expectOne(URL_API).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_REFRESH).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_LOGOUT).flush(null, { status: 204, statusText: 'No Content' });

        expect(sesion.autenticado()).toBeFalse();
        expect(router.navigate).toHaveBeenCalledOnceWith(['/auth/login'], { queryParams: { motivo: 'sesion-expirada' } });
        expect(error?.status).toBe(401);
    });

    it('si el refresco falla para varias peticiones cierra la sesión una sola vez', async () => {
        await conSesion('vencido');
        const errores: unknown[] = [];

        http.get(`${URL_API}/1`).subscribe({ error: (e) => errores.push(e) });
        http.get(`${URL_API}/2`).subscribe({ error: (e) => errores.push(e) });
        backend.expectOne(`${URL_API}/1`).flush(null, NO_AUTORIZADO);
        backend.expectOne(`${URL_API}/2`).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_REFRESH).flush(null, NO_AUTORIZADO);
        backend.expectOne(URL_LOGOUT).flush(null, { status: 204, statusText: 'No Content' });

        expect(errores.length).toBe(2);
        expect(router.navigate).toHaveBeenCalledTimes(1);
    });
});
