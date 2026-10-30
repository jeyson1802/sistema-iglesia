import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { LIMITES_CONSOLIDACION, ResultadoConsolidacion } from '../../models/consolidacion';

/** Diálogo para crear un resultado (al final del orden) o renombrarlo (presentacional). Emite el nombre sin espacios sobrantes. */
@Component({
    selector: 'app-formulario-resultado',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-resultado.html'
})
export class FormularioResultadoComponent {
    readonly visible = model(false);
    /** Resultado que se renombra; `null` al crear. */
    readonly resultado = input<ResultadoConsolidacion | null>(null);
    readonly guardando = input(false);
    /** Mensaje del backend (400, 404 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly guardar = output<string>();

    protected readonly nombreMax = LIMITES_CONSOLIDACION.nombreResultadoMax;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombre: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(LIMITES_CONSOLIDACION.nombreResultadoMin), Validators.maxLength(LIMITES_CONSOLIDACION.nombreResultadoMax)]]
    });
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre del resultado',
        pattern: 'Escribe el nombre del resultado',
        minlength: `Usa al menos ${LIMITES_CONSOLIDACION.nombreResultadoMin} caracteres`,
        maxlength: `El nombre no puede superar ${LIMITES_CONSOLIDACION.nombreResultadoMax} caracteres`
    };

    protected readonly titulo = computed(() => (this.resultado() ? `Renombrar «${this.resultado()?.nombre}»` : 'Nuevo resultado'));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const resultado = this.resultado();
                untracked(() => {
                    this.formulario.reset({ nombre: resultado?.nombre ?? '' });
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
