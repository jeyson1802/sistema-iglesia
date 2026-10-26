import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { SesionService } from '../services/sesion.service';
import { SIN_CONFIGURADOR_DE_TEMA } from '../testing/sin-configurador-de-tema';
import { LoginComponent } from './login';

describe('LoginComponent', () => {
    let sesion: jasmine.SpyObj<SesionService>;
    let navegar: jasmine.Spy;
    let fixture: ComponentFixture<LoginComponent>;

    const crear = async (motivo: string | null = null): Promise<void> => {
        sesion = jasmine.createSpyObj<SesionService>('SesionService', ['iniciarSesion', 'rutaInicio']);
        await TestBed.configureTestingModule({
            imports: [LoginComponent],
            providers: [
                provideRouter([]),
                { provide: SesionService, useValue: sesion },
                { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap(motivo ? { motivo } : {}) } } }
            ]
        })
            .overrideComponent(TarjetaAccesoComponent, SIN_CONFIGURADOR_DE_TEMA)
            .compileComponents();
        navegar = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
        fixture = TestBed.createComponent(LoginComponent);
        await fixture.whenStable();
    };

    const texto = (): string => fixture.nativeElement.textContent;

    const escribir = (id: string, valor: string): void => {
        const input: HTMLInputElement = fixture.nativeElement.querySelector(`#${id}`);
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        input.dispatchEvent(new Event('blur'));
    };

    const enviar = async (correo: string, contrasena: string): Promise<void> => {
        escribir('correo', correo);
        escribir('contrasena', contrasena);
        fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
        await fixture.whenStable();
    };

    it('con el formulario vacío muestra los campos obligatorios y no intenta iniciar sesión', async () => {
        await crear();

        await enviar('', '');

        expect(sesion.iniciarSesion).not.toHaveBeenCalled();
        expect(texto()).toContain('Ingresa tu correo electrónico');
        expect(texto()).toContain('Ingresa tu contraseña');
    });

    it('valida el formato y la longitud máxima de los campos', async () => {
        await crear();

        await enviar('no-es-correo', 'x'.repeat(73));

        expect(sesion.iniciarSesion).not.toHaveBeenCalled();
        expect(texto()).toContain('Ingresa un correo válido');
        expect(texto()).toContain('La contraseña no puede superar 72 caracteres');

        await enviar(`${'a'.repeat(250)}@i.test`, 'clave');

        expect(texto()).toContain('El correo no puede superar 254 caracteres');
    });

    it('con credenciales válidas inicia sesión y navega a la ruta de inicio', async () => {
        await crear();
        sesion.iniciarSesion.and.returnValue(of(undefined));
        sesion.rutaInicio.and.returnValue('/mi-perfil');

        await enviar('miembro@iglesia.test', 'clave');

        expect(sesion.iniciarSesion).toHaveBeenCalledOnceWith({ correo: 'miembro@iglesia.test', contrasena: 'clave' });
        expect(navegar).toHaveBeenCalledOnceWith('/mi-perfil');
    });

    it('con varios roles navega al selector de perfil', async () => {
        await crear();
        sesion.iniciarSesion.and.returnValue(of(undefined));
        sesion.rutaInicio.and.returnValue('/auth/seleccionar-perfil');

        await enviar('lider@iglesia.test', 'clave');

        expect(navegar).toHaveBeenCalledOnceWith('/auth/seleccionar-perfil');
    });

    it('con credenciales incorrectas muestra el mensaje y no navega', async () => {
        await crear();
        sesion.iniciarSesion.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));

        await enviar('miembro@iglesia.test', 'mala');

        expect(texto()).toContain('Correo o contraseña incorrectos');
        expect(navegar).not.toHaveBeenCalled();
    });

    it('ante otro error muestra un mensaje genérico', async () => {
        await crear();
        sesion.iniciarSesion.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

        await enviar('miembro@iglesia.test', 'clave');

        expect(texto()).toContain('No se pudo iniciar sesión. Intenta de nuevo.');
    });

    it('mientras espera la respuesta muestra el botón en estado de carga', async () => {
        await crear();
        const respuesta = new Subject<void>();
        sesion.iniciarSesion.and.returnValue(respuesta);
        const boton = (): HTMLButtonElement => fixture.nativeElement.querySelector('button[type="submit"]');

        await enviar('miembro@iglesia.test', 'clave');
        expect(boton().disabled).toBeTrue();

        respuesta.error(new HttpErrorResponse({ status: 401 }));
        await fixture.whenStable();
        expect(boton().disabled).toBeFalse();
    });

    it('muestra el aviso de sesión expirada', async () => {
        await crear('sesion-expirada');

        expect(texto()).toContain('Tu sesión expiró. Inicia sesión de nuevo.');
    });

    it('ignora motivos desconocidos', async () => {
        await crear('toString');

        expect(fixture.nativeElement.querySelector('p-message')).toBeNull();
    });

    it('muestra la identidad del sistema y asocia cada etiqueta con su campo', async () => {
        await crear();

        expect(fixture.nativeElement.querySelector('app-logo-iglesia')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('label[for="correo"]')).not.toBeNull();
        expect(fixture.nativeElement.querySelector('#contrasena')).not.toBeNull();
    });

    it('marca el correo como inválido para tecnologías de asistencia', async () => {
        await crear();

        await enviar('no-es-correo', 'clave');

        expect(fixture.nativeElement.querySelector('#correo').getAttribute('aria-invalid')).toBe('true');
    });

    it('no ofrece «Recordarme» ni «olvidé mi contraseña»', async () => {
        await crear();

        expect(texto()).not.toMatch(/recordarme|olvid/i);
    });
});
