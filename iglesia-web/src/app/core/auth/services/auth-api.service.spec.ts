import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '@/environments/environment';
import { TokenAcceso } from '../models/token-acceso';
import { UsuarioActual } from '../models/usuario-actual';
import { AuthApiService } from './auth-api.service';

describe('AuthApiService', () => {
    const authUrl = `${environment.seguridadUrl}/auth`;
    const usuariosUrl = `${environment.seguridadUrl}/usuarios`;
    const token: TokenAcceso = { tokenAcceso: 'jwt', tipo: 'Bearer', expiraEn: 900 };

    let servicio: AuthApiService;
    let http: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
        servicio = TestBed.inject(AuthApiService);
        http = TestBed.inject(HttpTestingController);
    });

    afterEach(() => http.verify());

    it('inicia sesión enviando las credenciales con cookies habilitadas', () => {
        let respuesta: TokenAcceso | undefined;

        servicio.login({ correo: 'a@i.test', contrasena: 'secreta' }).subscribe((r) => (respuesta = r));

        const peticion = http.expectOne(`${authUrl}/login`);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toEqual({ correo: 'a@i.test', contrasena: 'secreta' });
        expect(peticion.request.withCredentials).toBeTrue();
        peticion.flush(token);
        expect(respuesta).toEqual(token);
    });

    it('refresca sin cuerpo y con cookies habilitadas', () => {
        let respuesta: TokenAcceso | undefined;

        servicio.refrescar().subscribe((r) => (respuesta = r));

        const peticion = http.expectOne(`${authUrl}/refresh`);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toBeNull();
        expect(peticion.request.withCredentials).toBeTrue();
        peticion.flush(token);
        expect(respuesta).toEqual(token);
    });

    it('cierra sesión sin cuerpo y con cookies habilitadas', () => {
        let completado = false;

        servicio.cerrarSesion().subscribe({ complete: () => (completado = true) });

        const peticion = http.expectOne(`${authUrl}/logout`);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toBeNull();
        expect(peticion.request.withCredentials).toBeTrue();
        peticion.flush(null, { status: 204, statusText: 'No Content' });
        expect(completado).toBeTrue();
    });

    it('entra a una iglesia enviando su id con cookies habilitadas', () => {
        let respuesta: TokenAcceso | undefined;

        servicio.entrarAIglesia(5).subscribe((r) => (respuesta = r));

        const peticion = http.expectOne(`${authUrl}/iglesia-actuando`);
        expect(peticion.request.method).toBe('POST');
        expect(peticion.request.body).toEqual({ iglesiaId: 5 });
        expect(peticion.request.withCredentials).toBeTrue();
        peticion.flush(token);
        expect(respuesta).toEqual(token);
    });

    it('sale de la iglesia con cookies habilitadas', () => {
        let respuesta: TokenAcceso | undefined;

        servicio.salirDeIglesia().subscribe((r) => (respuesta = r));

        const peticion = http.expectOne(`${authUrl}/iglesia-actuando`);
        expect(peticion.request.method).toBe('DELETE');
        expect(peticion.request.withCredentials).toBeTrue();
        peticion.flush(token);
        expect(respuesta).toEqual(token);
    });

    it('obtiene el usuario actual adaptado al modelo de la UI', () => {
        let usuario: UsuarioActual | undefined;

        servicio.obtenerUsuarioActual().subscribe((u) => (usuario = u));

        const peticion = http.expectOne(`${usuariosUrl}/me`);
        expect(peticion.request.method).toBe('GET');
        expect(peticion.request.withCredentials).toBeFalse();
        peticion.flush({
            id: 3,
            correo: 'l@i.test',
            iglesiaId: 1,
            miembroId: 9,
            debeCambiarContrasena: false,
            actuandoEnIglesia: false,
            perfiles: [
                { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', permisos: [] },
                { id: 3, nombre: 'Líder', rolBase: 'LIDER', permisos: [] }
            ]
        });
        expect(usuario).toEqual({
            id: 3,
            correo: 'l@i.test',
            iglesiaId: 1,
            miembroId: 9,
            debeCambiarContrasena: false,
            actuandoEnIglesia: false,
            perfiles: [
                { id: 3, nombre: 'Líder', rolBase: 'LIDER', permisos: [] },
                { id: 4, nombre: 'Miembro', rolBase: 'MIEMBRO', permisos: [] }
            ]
        });
    });

    it('cambia la contraseña del usuario actual', () => {
        let completado = false;

        servicio.cambiarContrasena({ contrasenaActual: 'vieja', contrasenaNueva: 'NuevaClave123' }).subscribe({ complete: () => (completado = true) });

        const peticion = http.expectOne(`${usuariosUrl}/me/contrasena`);
        expect(peticion.request.method).toBe('PUT');
        expect(peticion.request.body).toEqual({ contrasenaActual: 'vieja', contrasenaNueva: 'NuevaClave123' });
        peticion.flush(null, { status: 204, statusText: 'No Content' });
        expect(completado).toBeTrue();
    });
});
