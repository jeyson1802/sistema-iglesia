import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { hoyIso } from '@/app/shared/utils/fecha';
import { AbrirReunion } from '../../models/asistencia';

/** Culto o grupo que se puede elegir para tomar asistencia en otra fecha. */
export interface OrigenReunion {
    readonly clave: string;
    readonly etiqueta: string;
    readonly cultoProgramadoId: number | null;
    readonly grupoId: number | null;
}

/** Diálogo «Tomar asistencia de otra fecha» (presentacional): elige el culto o grupo y una fecha pasada. */
@Component({
    selector: 'app-abrir-otra-fecha',
    imports: [ReactiveFormsModule, ButtonModule, DatePickerModule, DialogModule, MessageModule, SelectModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './abrir-otra-fecha.html'
})
export class AbrirOtraFechaComponent {
    readonly visible = model(false);
    readonly origenes = input.required<readonly OrigenReunion[]>();
    readonly abriendo = input(false);
    readonly error = input<string | null>(null);

    readonly abrir = output<AbrirReunion>();

    protected readonly hoy = new Date();
    protected readonly opciones = computed(() => [...this.origenes()]);
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        origen: [null as OrigenReunion | null, Validators.required],
        fecha: [new Date() as Date | null, Validators.required]
    });
    protected readonly mensajesOrigen: MensajesError = { required: 'Elige el culto o el grupo' };
    protected readonly mensajesFecha: MensajesError = { required: 'Elige la fecha' };
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                untracked(() => {
                    this.formulario.reset({ origen: null, fecha: new Date() });
                    this.formulario.updateValueAndValidity();
                });
            }
        });
    }

    protected enviar(): void {
        const { origen, fecha } = this.formulario.getRawValue();
        if (this.formulario.invalid || !origen || !fecha) {
            this.formulario.markAllAsTouched();
            return;
        }
        this.abrir.emit({ cultoProgramadoId: origen.cultoProgramadoId, grupoId: origen.grupoId, fecha: hoyIso(fecha) });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
