import { ChangeDetectionStrategy, Component, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { hoyIso } from '@/app/shared/utils/fecha';
import { CrearEvento, LIMITES_ASISTENCIA } from '../../models/asistencia';
import { dateAHora } from '../../models/hora';

const MS_POR_DIA = 86_400_000;
/** Último día admitido después del inicio (un evento dura como mucho una semana). */
const DIAS_DESPUES_MAX = LIMITES_ASISTENCIA.diasEventoMax - 1;

/** La fecha de fin, si se indica, va del día de inicio a 6 días después. */
function finDentroDeLaSemana(grupo: AbstractControl): ValidationErrors | null {
    const { fechaInicio, unaSolaFecha, fechaFin } = grupo.value as { fechaInicio: Date | null; unaSolaFecha: boolean; fechaFin: Date | null };
    if (unaSolaFecha || !fechaInicio || !fechaFin) {
        return null;
    }
    const dias = Math.round((fechaFin.getTime() - fechaInicio.getTime()) / MS_POR_DIA);
    return dias < 0 || dias > DIAS_DESPUES_MAX ? { finFueraDeRango: true } : null;
}

/** Diálogo «Crear evento» (presentacional): nombre, fecha de inicio, fecha de fin opcional y hora opcional. */
@Component({
    selector: 'app-formulario-evento',
    imports: [ReactiveFormsModule, ButtonModule, DatePickerModule, DialogModule, InputTextModule, MessageModule, ToggleSwitchModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-evento.html'
})
export class FormularioEventoComponent {
    readonly visible = model(false);
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly guardar = output<CrearEvento>();

    protected readonly nombreMax = LIMITES_ASISTENCIA.nombreEventoMax;
    protected readonly diasMax = LIMITES_ASISTENCIA.diasEventoMax;
    protected readonly formulario = inject(NonNullableFormBuilder).group(
        {
            nombre: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(LIMITES_ASISTENCIA.nombreEventoMin), Validators.maxLength(LIMITES_ASISTENCIA.nombreEventoMax)]],
            fechaInicio: [new Date() as Date | null, Validators.required],
            unaSolaFecha: [true],
            fechaFin: [null as Date | null],
            hora: [null as Date | null]
        },
        { validators: finDentroDeLaSemana }
    );
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre del evento',
        pattern: 'Escribe el nombre del evento',
        minlength: `Usa al menos ${LIMITES_ASISTENCIA.nombreEventoMin} caracteres`,
        maxlength: `El nombre no puede superar ${LIMITES_ASISTENCIA.nombreEventoMax} caracteres`
    };
    protected readonly mensajesInicio: MensajesError = { required: 'Elige la fecha de inicio' };

    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });
    protected readonly unaSolaFecha = toSignal(this.formulario.controls.unaSolaFecha.valueChanges, { initialValue: true });
    protected readonly inicio = toSignal(this.formulario.controls.fechaInicio.valueChanges, { initialValue: null as Date | null });
    protected readonly finFueraDeRango = toSignal(this.formulario.statusChanges.pipe(map(() => this.formulario.hasError('finFueraDeRango'))), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                untracked(() => {
                    this.formulario.reset({ nombre: '', fechaInicio: new Date(), unaSolaFecha: true, fechaFin: null, hora: null });
                    this.formulario.updateValueAndValidity();
                });
            }
        });
    }

    /** Último día que se puede elegir como fin. */
    protected finMaximo(inicio: Date | null): Date | null {
        return inicio ? new Date(inicio.getTime() + DIAS_DESPUES_MAX * MS_POR_DIA) : null;
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { nombre, fechaInicio, unaSolaFecha, fechaFin, hora } = this.formulario.getRawValue();
        const inicio = fechaInicio ?? new Date();
        this.guardar.emit({ nombre: nombre.trim(), fechaInicio: hoyIso(inicio), fechaFin: unaSolaFecha || !fechaFin ? null : hoyIso(fechaFin), hora: dateAHora(hora) });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
