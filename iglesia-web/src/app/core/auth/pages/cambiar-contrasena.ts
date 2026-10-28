import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { camposCoinciden, ERROR_CAMPOS_NO_COINCIDEN } from '@/app/shared/validators/campos-coinciden';
import { MOTIVO_LOGIN, RUTAS_AUTH } from '../models/rutas-auth';
import { SesionService } from '../services/sesion.service';
import { esErrorGlobal } from '../../errores/clasificar-error';

/** Límites de iglesia-seguridad (`LimitesUsuario`): BCrypt solo considera los primeros 72 bytes. */
const LONGITUD_MINIMA_CONTRASENA = 8;
const LONGITUD_MAXIMA_CONTRASENA = 72;

const MENSAJE_ERROR_GENERICO = 'No se pudo actualizar la contraseña. Intenta de nuevo.';

@Component({
    selector: 'app-cambiar-contrasena',
    imports: [ReactiveFormsModule, RouterLink, ButtonModule, MessageModule, PasswordModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './cambiar-contrasena.html'
})
export class CambiarContrasenaComponent {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);

    protected readonly formulario = inject(NonNullableFormBuilder).group(
        {
            contrasenaActual: ['', [Validators.required, Validators.maxLength(LONGITUD_MAXIMA_CONTRASENA)]],
            contrasenaNueva: ['', [Validators.required, Validators.minLength(LONGITUD_MINIMA_CONTRASENA), Validators.maxLength(LONGITUD_MAXIMA_CONTRASENA)]],
            confirmacion: ['', [Validators.required]]
        },
        { validators: camposCoinciden('contrasenaNueva', 'confirmacion') }
    );

    protected readonly mensajesActual: MensajesError = {
        required: 'Ingresa tu contraseña actual',
        maxlength: `La contraseña no puede superar ${LONGITUD_MAXIMA_CONTRASENA} caracteres`
    };
    protected readonly mensajesNueva: MensajesError = {
        required: 'Ingresa la nueva contraseña',
        minlength: `Usa al menos ${LONGITUD_MINIMA_CONTRASENA} caracteres`,
        maxlength: `La contraseña no puede superar ${LONGITUD_MAXIMA_CONTRASENA} caracteres`
    };
    protected readonly mensajesConfirmacion: MensajesError = { required: 'Repite la nueva contraseña' };
    protected readonly mensajesCoincidencia: MensajesError = { [ERROR_CAMPOS_NO_COINCIDEN]: 'Las contraseñas no coinciden' };

    protected readonly ayudaNueva = `Entre ${LONGITUD_MINIMA_CONTRASENA} y ${LONGITUD_MAXIMA_CONTRASENA} caracteres.`;
    protected readonly rutaCancelar = this.sesion.rutaInicio();
    protected readonly debeCambiarContrasena = this.sesion.debeCambiarContrasena;

    protected readonly guardando = signal(false);
    protected readonly error = signal<string | null>(null);

    protected cambiar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { contrasenaActual, contrasenaNueva } = this.formulario.getRawValue();
        this.guardando.set(true);
        this.error.set(null);
        this.sesion
            .cambiarContrasena({ contrasenaActual, contrasenaNueva })
            .pipe(finalize(() => this.guardando.set(false)))
            .subscribe({
                complete: () => void this.router.navigate([RUTAS_AUTH.login], { queryParams: { motivo: MOTIVO_LOGIN.contrasenaCambiada } }),
                error: (error: unknown) => this.error.set(mensajeDeError(error))
            });
    }
}

/**
 * Un 400 trae en `detail` del ProblemDetail el motivo en español (p. ej. «La contraseña actual no es correcta»).
 * Los 5xx y la falta de conexión ya los avisa el toast global: la página no repite el mensaje.
 */
function mensajeDeError(error: unknown): string | null {
    if (esErrorGlobal(error)) {
        return null;
    }
    if (error instanceof HttpErrorResponse && error.status === HttpStatusCode.BadRequest && typeof error.error?.detail === 'string') {
        return error.error.detail;
    }
    return MENSAJE_ERROR_GENERICO;
}
