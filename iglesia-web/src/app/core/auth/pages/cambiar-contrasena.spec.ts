import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { SesionService } from '../services/sesion.service';
import { CambiarContrasenaComponent } from './cambiar-contrasena';

describe('CambiarContrasenaComponent', () => {
    let sesion: jasmine.SpyObj<SesionService>;
    let navegar: jasmine.Spy;
    let fixture: ComponentFixture<CambiarContrasenaComponent>;

    beforeEach(async () => {
        sesion = jasmine.createSpyObj<SesionService>('SesionService', ['cambiarContrasena', 'rutaInicio']);
        sesion.rutaInicio.and.returnValue('/mi-perfil');
        await TestBed.configureTestingModule({ imports: [CambiarContrasenaComponent], providers: [provideRouter([]), { provide: SesionService, useValue: sesion }] }).compileComponents();
        navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
        fixture = TestBed.createComponent(CambiarContrasenaComponent);
        await fixture.whenStable();
    });

    const texto = (): string => fixture.nativeElement.textContent;

    const escribir = (id: string, valor: string): void => {
        const input: HTMLInputElement = fixture.nativeElement.querySelector(`#${id}`);
        input.value = valor;
        input.dispatchEvent(new Event('input'));
        input.dispatchEvent(new Event('blur'));
    };

    const enviar = async (actual: string, nueva: string, confirmacion: string): Promise<void> => {
        escribir('contrasena-actual', actual);
        escribir('contrasena-nueva', nueva);
        escribir('confirmacion', confirmacion);
        fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
        await fixture.whenStable();
    };

    it('con el formulario vacío pide los tres campos y no envía nada', async () => {
        await enviar('', '', '');

        expect(sesion.cambiarContrasena).not.toHaveBeenCalled();
        expect(texto()).toContain('Ingresa tu contraseña actual');
        expect(texto()).toContain('Ingresa la nueva contraseña');
        expect(texto()).toContain('Repite la nueva contraseña');
    });

    it('exige una longitud mínima para la nueva contraseña', async () => {
        await enviar('Actual12345', 'corta', 'corta');

        expect(sesion.cambiarContrasena).not.toHaveBeenCalled();
        expect(texto()).toContain('Usa al menos 8 caracteres');
    });

    it('valida que la confirmación coincida con la nueva contraseña', async () => {
        await enviar('Actual12345', 'Nueva12345', 'Distinta123');

        expect(sesion.cambiarContrasena).not.toHaveBeenCalled();
        expect(texto()).toContain('Las contraseñas no coinciden');
    });

    it('si el cambio sale bien lleva al login con el aviso de contraseña actualizada', async () => {
        sesion.cambiarContrasena.and.returnValue(of(undefined));

        await enviar('Actual12345', 'Nueva12345', 'Nueva12345');

        expect(sesion.cambiarContrasena).toHaveBeenCalledOnceWith({ contrasenaActual: 'Actual12345', contrasenaNueva: 'Nueva12345' });
        expect(navegar).toHaveBeenCalledOnceWith(['/auth/login'], { queryParams: { motivo: 'contrasena-cambiada' } });
    });

    it('si el backend responde 400 muestra el detalle del ProblemDetail', async () => {
        sesion.cambiarContrasena.and.returnValue(throwError(() => new HttpErrorResponse({ status: 400, error: { title: 'Regla de negocio incumplida', detail: 'La contraseña actual no es correcta' } })));

        await enviar('Incorrecta1', 'Nueva12345', 'Nueva12345');

        expect(texto()).toContain('La contraseña actual no es correcta');
        expect(navegar).not.toHaveBeenCalled();
    });

    it('ante otro error muestra un mensaje genérico', async () => {
        sesion.cambiarContrasena.and.returnValue(throwError(() => new HttpErrorResponse({ status: 0 })));

        await enviar('Actual12345', 'Nueva12345', 'Nueva12345');

        expect(texto()).toContain('No se pudo actualizar la contraseña. Intenta de nuevo.');
    });

    it('deshabilita el botón mientras se guarda', async () => {
        const respuesta = new Subject<void>();
        sesion.cambiarContrasena.and.returnValue(respuesta);
        const boton = (): HTMLButtonElement => fixture.nativeElement.querySelector('button[type="submit"]');

        await enviar('Actual12345', 'Nueva12345', 'Nueva12345');
        expect(boton().disabled).toBeTrue();

        respuesta.error(new HttpErrorResponse({ status: 0 }));
        await fixture.whenStable();
        expect(boton().disabled).toBeFalse();
    });

    it('ofrece cancelar y volver a la página de inicio', () => {
        expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe('/mi-perfil');
    });
});
