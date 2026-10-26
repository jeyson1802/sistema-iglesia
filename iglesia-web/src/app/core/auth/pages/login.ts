import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { LogoIglesiaComponent } from '@/app/shared/components/logo-iglesia/logo-iglesia';
import { TarjetaAccesoComponent } from '../components/tarjeta-acceso/tarjeta-acceso';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { AVISO_POR_MOTIVO, AvisoLogin } from '../models/rutas-auth';
import { SesionService } from '../services/sesion.service';

const LONGITUD_MAXIMA_CORREO = 254;
const LONGITUD_MAXIMA_CONTRASENA = 72;

const MENSAJE_CREDENCIALES_INVALIDAS = 'Correo o contraseña incorrectos';
const MENSAJE_ERROR_GENERICO = 'No se pudo iniciar sesión. Intenta de nuevo.';

@Component({
    selector: 'app-login',
    imports: [ReactiveFormsModule, ButtonModule, InputTextModule, PasswordModule, MessageModule, LogoIglesiaComponent, MensajeErrorCampoComponent, TarjetaAccesoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './login.html'
})
export class LoginComponent {
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);

    protected readonly mensajesCorreo: MensajesError = {
        required: 'Ingresa tu correo electrónico',
        maxlength: `El correo no puede superar ${LONGITUD_MAXIMA_CORREO} caracteres`,
        email: 'Ingresa un correo válido, por ejemplo nombre@ejemplo.com'
    };
    protected readonly mensajesContrasena: MensajesError = {
        required: 'Ingresa tu contraseña',
        maxlength: `La contraseña no puede superar ${LONGITUD_MAXIMA_CONTRASENA} caracteres`
    };

    protected readonly formulario = inject(NonNullableFormBuilder).group({
        correo: ['', [Validators.required, Validators.email, Validators.maxLength(LONGITUD_MAXIMA_CORREO)]],
        contrasena: ['', [Validators.required, Validators.maxLength(LONGITUD_MAXIMA_CONTRASENA)]]
    });

    protected readonly cargando = signal(false);
    protected readonly error = signal<string | null>(null);
    protected readonly aviso = avisoDe(inject(ActivatedRoute).snapshot.queryParamMap.get('motivo'));

    protected iniciarSesion(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        this.cargando.set(true);
        this.error.set(null);
        this.sesion
            .iniciarSesion(this.formulario.getRawValue())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                // Con un solo rol va a su inicio; con varios, al selector de perfil.
                next: () => void this.router.navigateByUrl(this.sesion.rutaInicio()),
                error: (error: unknown) => this.error.set(mensajeDeError(error))
            });
    }
}

function avisoDe(motivo: string | null): AvisoLogin | null {
    return motivo !== null && Object.hasOwn(AVISO_POR_MOTIVO, motivo) ? AVISO_POR_MOTIVO[motivo] : null;
}

function mensajeDeError(error: unknown): string {
    return error instanceof HttpErrorResponse && error.status === HttpStatusCode.Unauthorized ? MENSAJE_CREDENCIALES_INVALIDAS : MENSAJE_ERROR_GENERICO;
}
