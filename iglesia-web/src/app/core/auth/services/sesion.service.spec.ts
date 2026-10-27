import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom, of, Subject, throwError } from 'rxjs';
import { TokenAcceso } from '../models/token-acceso';
import { UsuarioActual } from '../models/usuario-actual';
import { perfilBase, perfilPersonalizado, usuarioCon } from '../testing/perfiles-prueba';
import { AuthApiService } from './auth-api.service';
import { SesionService } from './sesion.service';

describe('SesionService', () => {
    const CLAVE_PERFIL = 'iglesia.perfilActivo';
    const token = (valor: string): TokenAcceso => ({ tokenAcceso: valor, tipo: 'Bearer', expiraEn: 900 });
    const noAutorizado = new HttpErrorResponse({ status: 401 });
    const secretaria = perfilPersonalizado(10, 'Secretaría', 'ADMIN_IGLESIA', ['miembros.ver', 'miembros.crear']);

    let api: jasmine.SpyObj<AuthApiService>;
    let sesion: SesionService;

    beforeEach(() => {
        sessionStorage.clear();
        api = jasmine.createSpyObj<AuthApiService>('AuthApiService', ['login', 'refrescar', 'cerrarSesion', 'obtenerUsuarioActual', 'cambiarContrasena']);
        api.cerrarSesion.and.returnValue(of(undefined));
        TestBed.configureTestingModule({ providers: [{ provide: AuthApiService, useValue: api }] });
        sesion = TestBed.inject(SesionService);
    });

    afterEach(() => sessionStorage.clear());

    const iniciarSesionCon = async (usuario: UsuarioActual): Promise<void> => {
        api.login.and.returnValue(of(token('acceso')));
        api.obtenerUsuarioActual.and.returnValue(of(usuario));
        await firstValueFrom(sesion.iniciarSesion({ correo: 'u@i.test', contrasena: 'clave' }), { defaultValue: undefined });
    };

    describe('iniciarSesion', () => {
        it('con un solo perfil guarda el token y el usuario y selecciona el perfil', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));

            expect(sesion.autenticado()).toBeTrue();
            expect(sesion.tokenAcceso()).toBe('acceso');
            expect(sesion.usuarioActual()?.correo).toBe('u@i.test');
            expect(sesion.perfilActivo()).toEqual(perfilBase('MIEMBRO'));
            expect(sesion.tieneVariosPerfiles()).toBeFalse();
            expect(sesion.rutaInicio()).toBe('/mi-perfil');
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBe('4');
        });

        it('con un perfil personalizado va al inicio de su rol base', async () => {
            await iniciarSesionCon(usuarioCon(secretaria));

            expect(sesion.perfilActivo()).toEqual(secretaria);
            expect(sesion.rutaInicio()).toBe('/administracion');
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBe('10');
        });

        it('con varios perfiles deja el perfil sin elegir y apunta al selector', async () => {
            await iniciarSesionCon(usuarioCon('LIDER', 'MIEMBRO'));

            expect(sesion.perfilActivo()).toBeNull();
            expect(sesion.tieneVariosPerfiles()).toBeTrue();
            expect(sesion.rutaInicio()).toBe('/auth/seleccionar-perfil');
        });

        it('con credenciales incorrectas propaga el error y no deja sesión', async () => {
            api.login.and.returnValue(throwError(() => noAutorizado));

            await expectAsync(firstValueFrom(sesion.iniciarSesion({ correo: 'u@i.test', contrasena: 'mala' }))).toBeRejectedWith(noAutorizado);

            expect(sesion.autenticado()).toBeFalse();
            expect(sesion.rutaInicio()).toBe('/auth/login');
        });

        it('si falla la carga del usuario descarta el token recibido', async () => {
            api.login.and.returnValue(of(token('acceso')));
            api.obtenerUsuarioActual.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

            await expectAsync(firstValueFrom(sesion.iniciarSesion({ correo: 'u@i.test', contrasena: 'clave' }))).toBeRejected();

            expect(sesion.tokenAcceso()).toBeNull();
        });

        it('no guarda tokens en sessionStorage ni en localStorage', async () => {
            localStorage.clear();

            await iniciarSesionCon(usuarioCon('MIEMBRO'));

            expect(sessionStorage.length).toBe(1);
            expect(localStorage.length).toBe(0);
        });
    });

    describe('refrescar', () => {
        it('comparte una sola petición entre llamadas simultáneas', () => {
            const respuesta = new Subject<TokenAcceso>();
            api.refrescar.and.returnValue(respuesta);
            const recibidos: string[] = [];

            sesion.refrescar().subscribe((t) => recibidos.push(t));
            sesion.refrescar().subscribe((t) => recibidos.push(t));
            sesion.refrescar().subscribe((t) => recibidos.push(t));
            respuesta.next(token('nuevo'));
            respuesta.complete();

            expect(api.refrescar).toHaveBeenCalledTimes(1);
            expect(recibidos).toEqual(['nuevo', 'nuevo', 'nuevo']);
            expect(sesion.tokenAcceso()).toBe('nuevo');
        });

        it('al terminar permite un refresco nuevo', async () => {
            api.refrescar.and.returnValues(of(token('primero')), of(token('segundo')));

            await firstValueFrom(sesion.refrescar());
            const segundo = await firstValueFrom(sesion.refrescar());

            expect(api.refrescar).toHaveBeenCalledTimes(2);
            expect(segundo).toBe('segundo');
        });

        it('comparte también el error y luego permite reintentar', async () => {
            const respuesta = new Subject<TokenAcceso>();
            api.refrescar.and.returnValues(respuesta, of(token('reintento')));
            const errores: unknown[] = [];

            sesion.refrescar().subscribe({ error: (e) => errores.push(e) });
            sesion.refrescar().subscribe({ error: (e) => errores.push(e) });
            respuesta.error(noAutorizado);

            expect(errores).toEqual([noAutorizado, noAutorizado]);
            expect(await firstValueFrom(sesion.refrescar())).toBe('reintento');
        });
    });

    describe('restaurar', () => {
        it('recupera la sesión y el perfil guardado por id si el usuario aún lo tiene', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, '3');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('LIDER', 'MIEMBRO')));

            await sesion.restaurar();

            expect(sesion.autenticado()).toBeTrue();
            expect(sesion.tokenAcceso()).toBe('restaurado');
            expect(sesion.perfilActivo()).toEqual(perfilBase('LIDER'));
            expect(sesion.rutaInicio()).toBe('/mi-red');
        });

        it('recupera un perfil personalizado guardado', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, '10');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon(secretaria, 'MIEMBRO')));

            await sesion.restaurar();

            expect(sesion.perfilActivo()).toEqual(secretaria);
        });

        it('descarta el perfil guardado si el usuario ya no tiene ese rol activo', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, '2');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('LIDER', 'MIEMBRO')));

            await sesion.restaurar();

            expect(sesion.perfilActivo()).toBeNull();
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBeNull();
        });

        it('descarta sin errores el nombre de rol que guardaba la versión anterior', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, 'LIDER');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('LIDER', 'MIEMBRO')));

            await expectAsync(sesion.restaurar()).toBeResolved();

            expect(sesion.autenticado()).toBeTrue();
            expect(sesion.perfilActivo()).toBeNull();
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBeNull();
        });

        it('descarta un valor guardado que no es un id válido', async () => {
            for (const valor of ['', '0', '-3', '2.5', 'HACKER']) {
                sessionStorage.setItem(CLAVE_PERFIL, valor);
                api.refrescar.and.returnValue(of(token('restaurado')));
                api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('LIDER', 'MIEMBRO')));

                await sesion.restaurar();

                expect(sesion.perfilActivo()).withContext(valor).toBeNull();
            }
        });

        it('con un solo perfil lo selecciona aunque el valor guardado no sirva', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, 'MIEMBRO');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('MIEMBRO')));

            await sesion.restaurar();

            expect(sesion.perfilActivo()).toEqual(perfilBase('MIEMBRO'));
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBe('4');
        });

        it('sin cookie de refresco termina sin sesión y sin fallar', async () => {
            sessionStorage.setItem(CLAVE_PERFIL, '4');
            api.refrescar.and.returnValue(throwError(() => noAutorizado));

            await expectAsync(sesion.restaurar()).toBeResolved();

            expect(sesion.autenticado()).toBeFalse();
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBeNull();
        });

        it('funciona aunque sessionStorage no esté disponible', async () => {
            spyOn(Storage.prototype, 'getItem').and.throwError('bloqueado');
            spyOn(Storage.prototype, 'setItem').and.throwError('bloqueado');
            api.refrescar.and.returnValue(of(token('restaurado')));
            api.obtenerUsuarioActual.and.returnValue(of(usuarioCon('MIEMBRO')));

            await sesion.restaurar();

            expect(sesion.autenticado()).toBeTrue();
            expect(sesion.perfilActivo()).toEqual(perfilBase('MIEMBRO'));
        });
    });

    describe('seleccionarPerfil', () => {
        it('activa y recuerda por id un perfil del usuario', async () => {
            await iniciarSesionCon(usuarioCon('LIDER', 'MIEMBRO'));

            sesion.seleccionarPerfil(3);

            expect(sesion.perfilActivo()).toEqual(perfilBase('LIDER'));
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBe('3');
            expect(sesion.rutaInicio()).toBe('/mi-red');
        });

        it('rechaza un perfil que el usuario no tiene', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));

            expect(() => sesion.seleccionarPerfil(2)).toThrowError(/2/);
            expect(sesion.perfilActivo()).toEqual(perfilBase('MIEMBRO'));
        });
    });

    describe('permisos del perfil activo', () => {
        it('solo cuentan los permisos del perfil activo, no la unión de todos', async () => {
            await iniciarSesionCon(usuarioCon(secretaria, 'MIEMBRO'));

            sesion.seleccionarPerfil(10);
            expect(sesion.permisosActivos()).toEqual(new Set(['miembros.ver', 'miembros.crear']));
            expect(sesion.tienePermiso('miembros.crear')).toBeTrue();
            expect(sesion.tienePermiso('roles.ver')).toBeFalse();

            sesion.seleccionarPerfil(4);
            expect(sesion.permisosActivos().size).toBe(0);
            expect(sesion.tienePermiso('miembros.ver')).toBeFalse();
        });

        it('sin perfil activo no hay permisos', async () => {
            await iniciarSesionCon(usuarioCon(secretaria, 'MIEMBRO'));

            expect(sesion.perfilActivo()).toBeNull();
            expect(sesion.tienePermiso('miembros.ver')).toBeFalse();
        });

        it('al cerrar sesión se pierden los permisos', async () => {
            await iniciarSesionCon(usuarioCon(secretaria));

            await firstValueFrom(sesion.cerrarSesion(), { defaultValue: undefined });

            expect(sesion.tienePermiso('miembros.ver')).toBeFalse();
        });
    });

    describe('cerrarSesion', () => {
        it('limpia el estado y el perfil guardado y avisa al backend', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));

            await firstValueFrom(sesion.cerrarSesion(), { defaultValue: undefined });

            expect(api.cerrarSesion).toHaveBeenCalledTimes(1);
            expect(sesion.autenticado()).toBeFalse();
            expect(sesion.tokenAcceso()).toBeNull();
            expect(sesion.perfilActivo()).toBeNull();
            expect(sessionStorage.getItem(CLAVE_PERFIL)).toBeNull();
        });

        it('completa aunque el backend falle', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));
            api.cerrarSesion.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

            await expectAsync(firstValueFrom(sesion.cerrarSesion(), { defaultValue: undefined })).toBeResolved();

            expect(sesion.autenticado()).toBeFalse();
        });
    });

    describe('cambiarContrasena', () => {
        const cambio = { contrasenaActual: 'Actual12345', contrasenaNueva: 'Nueva12345' };

        it('cambia la contraseña y cierra la sesión local', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));
            api.cambiarContrasena.and.returnValue(of(undefined));

            await firstValueFrom(sesion.cambiarContrasena(cambio), { defaultValue: undefined });

            expect(api.cambiarContrasena).toHaveBeenCalledOnceWith(cambio);
            expect(api.cerrarSesion).toHaveBeenCalledTimes(1);
            expect(sesion.autenticado()).toBeFalse();
        });

        it('si el backend lo rechaza conserva la sesión y propaga el error', async () => {
            await iniciarSesionCon(usuarioCon('MIEMBRO'));
            const rechazo = new HttpErrorResponse({ status: 400 });
            api.cambiarContrasena.and.returnValue(throwError(() => rechazo));

            await expectAsync(firstValueFrom(sesion.cambiarContrasena(cambio))).toBeRejectedWith(rechazo);

            expect(api.cerrarSesion).not.toHaveBeenCalled();
            expect(sesion.autenticado()).toBeTrue();
        });
    });
});
