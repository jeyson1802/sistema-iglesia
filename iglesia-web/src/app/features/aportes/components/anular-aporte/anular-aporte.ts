import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { MonedaPipe } from '@/app/shared/pipes/moneda.pipe';
import { Aporte, LIMITES_APORTE } from '../../models/aporte';

/** Confirmación para anular un aporte (presentacional): muestra qué se anula y exige el motivo. */
@Component({
    selector: 'app-anular-aporte',
    imports: [DatePipe, ReactiveFormsModule, ButtonModule, DialogModule, MessageModule, TextareaModule, MensajeErrorCampoComponent, MonedaPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './anular-aporte.html'
})
export class AnularAporteComponent {
    readonly visible = model(false);
    readonly aporte = input<Aporte | null>(null);
    readonly moneda = input.required<string>();
    readonly anulando = input(false);
    readonly error = input<string | null>(null);

    readonly anular = output<string>();

    protected readonly motivoMax = LIMITES_APORTE.motivoAnulacion;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        motivo: ['', [Validators.required, Validators.maxLength(LIMITES_APORTE.motivoAnulacion), Validators.pattern(/\S/)]]
    });
    protected readonly mensajesMotivo: MensajesError = {
        required: 'Escribe por qué se anula (p. ej. «Registrado dos veces»)',
        pattern: 'Escribe por qué se anula (p. ej. «Registrado dos veces»)',
        maxlength: `Máximo ${LIMITES_APORTE.motivoAnulacion} caracteres`
    };
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                untracked(() => {
                    this.formulario.reset({ motivo: '' });
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
        this.anular.emit(this.formulario.getRawValue().motivo.trim());
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
