import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { CONTRASENA_TEMPORAL_MAX, CONTRASENA_TEMPORAL_MIN, CORREO_MAX } from '@/app/shared/utils/limites-usuario';

export type GrupoAdministrador = FormGroup<{ correo: FormControl<string>; contrasenaTemporal: FormControl<string> }>;

/** Crea los controles del administrador con sus validaciones; el formulario que lo contiene lo incluye como grupo. */
export function crearGrupoAdministrador(fb: NonNullableFormBuilder): GrupoAdministrador {
    return fb.group({
        correo: ['', [Validators.required, Validators.email, Validators.maxLength(CORREO_MAX)]],
        contrasenaTemporal: ['', [Validators.required, Validators.minLength(CONTRASENA_TEMPORAL_MIN), Validators.maxLength(CONTRASENA_TEMPORAL_MAX)]]
    });
}

/**
 * Campos de correo y contraseña temporal del administrador de una iglesia (presentacional). Los usan el alta de una
 * iglesia y la creación de un administrador para una iglesia existente.
 */
@Component({
    selector: 'app-campos-administrador',
    imports: [ReactiveFormsModule, InputTextModule, PasswordModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './campos-administrador.html',
    host: { class: 'flex flex-col gap-2' }
})
export class CamposAdministradorComponent {
    readonly grupo = input.required<GrupoAdministrador>();
    /** Prefijo de los `id` de los campos, para que no se repitan si hay dos formularios en la página. */
    readonly idPrefijo = input.required<string>();

    protected readonly correoMax = CORREO_MAX;
    protected readonly contrasenaMax = CONTRASENA_TEMPORAL_MAX;
    protected readonly mensajesCorreo: MensajesError = {
        required: 'Escribe el correo del administrador',
        email: 'Escribe un correo válido, por ejemplo pastor@iglesia.org',
        maxlength: `El correo no puede superar ${CORREO_MAX} caracteres`
    };
    protected readonly mensajesContrasena: MensajesError = {
        required: 'Escribe una contraseña temporal',
        minlength: `Usa al menos ${CONTRASENA_TEMPORAL_MIN} caracteres`,
        maxlength: `Usa como máximo ${CONTRASENA_TEMPORAL_MAX} caracteres`
    };
    protected readonly ayudaContrasena = `Entre ${CONTRASENA_TEMPORAL_MIN} y ${CONTRASENA_TEMPORAL_MAX} caracteres. Deberá cambiarla al iniciar sesión.`;
}
