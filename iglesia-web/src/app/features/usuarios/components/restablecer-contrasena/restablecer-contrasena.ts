import { ChangeDetectionStrategy, Component, effect, inject, input, model, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { CONTRASENA_TEMPORAL_MAX, CONTRASENA_TEMPORAL_MIN } from '@/app/shared/utils/limites-usuario';
import { UsuarioAdmin } from '../../models/usuario-admin';

/**
 * Diálogo para restablecer la contraseña de otro usuario con una temporal (presentacional). Emite la contraseña
 * elegida; el contenedor la envía y muestra el resultado.
 */
@Component({
    selector: 'app-restablecer-contrasena',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, MessageModule, PasswordModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './restablecer-contrasena.html'
})
export class RestablecerContrasenaComponent {
    readonly visible = model(false);
    /** Usuario al que se le restablece la contraseña. */
    readonly usuario = input<UsuarioAdmin | null>(null);
    readonly guardando = input(false);
    /** Mensaje del backend (400 o 404) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly restablecer = output<string>();

    protected readonly contrasenaMax = CONTRASENA_TEMPORAL_MAX;

    protected readonly formulario = inject(NonNullableFormBuilder).group({
        contrasenaTemporal: ['', [Validators.required, Validators.minLength(CONTRASENA_TEMPORAL_MIN), Validators.maxLength(CONTRASENA_TEMPORAL_MAX)]]
    });

    protected readonly mensajesContrasena: MensajesError = {
        required: 'Escribe una contraseña temporal',
        minlength: `Usa al menos ${CONTRASENA_TEMPORAL_MIN} caracteres`,
        maxlength: `Usa como máximo ${CONTRASENA_TEMPORAL_MAX} caracteres`
    };
    protected readonly ayudaContrasena = `Entre ${CONTRASENA_TEMPORAL_MIN} y ${CONTRASENA_TEMPORAL_MAX} caracteres.`;

    /** Los controles no son signals: se escucha el estado para habilitar el botón en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Cada vez que se abre empieza vacío: la contraseña anterior no debe quedar en el formulario.
        effect(() => {
            if (this.visible()) {
                this.formulario.reset();
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        this.restablecer.emit(this.formulario.getRawValue().contrasenaTemporal);
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
