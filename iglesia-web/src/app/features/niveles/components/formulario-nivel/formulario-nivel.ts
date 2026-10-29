import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { Nivel, NOMBRE_NIVEL_MAX } from '../../models/nivel';

/** Diálogo para crear un nivel (al final del orden) o renombrarlo (presentacional). Emite el nombre sin espacios sobrantes. */
@Component({
    selector: 'app-formulario-nivel',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-nivel.html'
})
export class FormularioNivelComponent {
    readonly visible = model(false);
    /** Nivel que se renombra; `null` al crear. */
    readonly nivel = input<Nivel | null>(null);
    readonly guardando = input(false);
    /** Mensaje del backend (400, 404 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly guardar = output<string>();

    protected readonly nombreMax = NOMBRE_NIVEL_MAX;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombre: ['', [Validators.required, Validators.maxLength(NOMBRE_NIVEL_MAX), Validators.pattern(/\S/)]]
    });
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre del nivel',
        pattern: 'Escribe el nombre del nivel',
        maxlength: `El nombre no puede superar ${NOMBRE_NIVEL_MAX} caracteres`
    };

    protected readonly titulo = computed(() => (this.nivel() ? `Renombrar «${this.nivel()?.nombre}»` : 'Nuevo nivel'));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const nivel = this.nivel();
                untracked(() => {
                    this.formulario.reset({ nombre: nivel?.nombre ?? '' });
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
        this.guardar.emit(this.formulario.getRawValue().nombre.trim());
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
