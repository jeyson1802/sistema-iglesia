import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { CrearUsuario } from '@/app/features/usuarios/models/usuario-admin';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { CONTRASENA_TEMPORAL_MAX, CONTRASENA_TEMPORAL_MIN, CORREO_MAX } from '@/app/shared/utils/limites-usuario';

/** Rol que se puede dar desde la ficha: el rol base «Líder» o «Miembro» de la iglesia. */
export interface RolAccesoMiembro {
    readonly id: number;
    readonly nombre: string;
}

/**
 * Diálogo «Dar acceso al sistema» (presentacional): correo (precargado con el del miembro), contraseña temporal y rol.
 * Emite los datos del usuario; el contenedor lo crea vinculado al miembro.
 */
@Component({
    selector: 'app-dar-acceso',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, PasswordModule, SelectModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './dar-acceso.html'
})
export class DarAccesoComponent {
    readonly visible = model(false);
    readonly nombreMiembro = input('');
    /** Correo de la ficha, si tiene; se puede cambiar. */
    readonly correoSugerido = input<string | null>(null);
    readonly roles = input.required<readonly RolAccesoMiembro[]>();
    readonly guardando = input(false);
    /** Mensaje del backend (400 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly darAcceso = output<CrearUsuario>();

    /** `p-select` espera un arreglo mutable. */
    protected readonly opcionesRol = computed(() => [...this.roles()]);
    protected readonly limites = { correo: CORREO_MAX, contrasena: CONTRASENA_TEMPORAL_MAX };
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        correo: ['', [Validators.required, Validators.email, Validators.maxLength(CORREO_MAX)]],
        contrasenaTemporal: ['', [Validators.required, Validators.minLength(CONTRASENA_TEMPORAL_MIN), Validators.maxLength(CONTRASENA_TEMPORAL_MAX)]],
        rolId: new FormControl<number | null>(null, Validators.required)
    });
    protected readonly mensajesCorreo: MensajesError = {
        required: 'Escribe el correo con el que iniciará sesión',
        email: 'Escribe un correo válido, p. ej. nombre@correo.com',
        maxlength: `Usa como máximo ${CORREO_MAX} caracteres`
    };
    protected readonly mensajesContrasena: MensajesError = {
        required: 'Escribe una contraseña temporal',
        minlength: `Usa al menos ${CONTRASENA_TEMPORAL_MIN} caracteres`,
        maxlength: `Usa como máximo ${CONTRASENA_TEMPORAL_MAX} caracteres`
    };
    protected readonly mensajesRol: MensajesError = { required: 'Elige el rol' };
    protected readonly ayudaContrasena = `Entre ${CONTRASENA_TEMPORAL_MIN} y ${CONTRASENA_TEMPORAL_MAX} caracteres. Deberá cambiarla al iniciar sesión.`;

    /** Los controles no son signals: se escucha el estado para habilitar el botón en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Cada vez que se abre empieza con el correo de la ficha y sin contraseña.
        effect(() => {
            if (this.visible()) {
                const correo = this.correoSugerido();
                untracked(() => {
                    this.formulario.reset({ correo: correo ?? '', contrasenaTemporal: '', rolId: null });
                    this.formulario.updateValueAndValidity();
                });
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { correo, contrasenaTemporal, rolId } = this.formulario.getRawValue();
        this.darAcceso.emit({ correo: correo.trim(), contrasenaTemporal, rolIds: rolId === null ? [] : [rolId] });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
