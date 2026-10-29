import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { DIAS_SEMANA, DiaSemana, ETIQUETA_DIA } from '@/app/core/models/dia-semana';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { CultoProgramado, GuardarCulto, LIMITES_ASISTENCIA } from '../../models/asistencia';
import { dateAHora, horaADate } from '../../models/hora';

/** Hora con la que se propone un culto nuevo. */
const HORA_SUGERIDA = '10:00';

/** Diálogo para crear o editar un horario de culto (presentacional): nombre, día (iniciales) y hora. */
@Component({
    selector: 'app-formulario-culto',
    imports: [ReactiveFormsModule, ButtonModule, DatePickerModule, DialogModule, InputTextModule, MessageModule, SelectButtonModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-culto.html'
})
export class FormularioCultoComponent {
    readonly visible = model(false);
    /** Culto que se edita; `null` al crear. */
    readonly culto = input<CultoProgramado | null>(null);
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly guardar = output<GuardarCulto>();

    protected readonly nombreMax = LIMITES_ASISTENCIA.nombreCultoMax;
    /** «L M M J V S D» con el nombre completo para los lectores de pantalla. */
    protected readonly dias = DIAS_SEMANA.map((dia) => ({ valor: dia, inicial: ETIQUETA_DIA[dia].charAt(0), etiqueta: ETIQUETA_DIA[dia] }));
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombre: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(LIMITES_ASISTENCIA.nombreCultoMin), Validators.maxLength(LIMITES_ASISTENCIA.nombreCultoMax)]],
        diaSemana: ['DOMINGO' as DiaSemana, Validators.required],
        hora: [horaADate(HORA_SUGERIDA) as Date | null, Validators.required]
    });
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre del culto',
        pattern: 'Escribe el nombre del culto',
        minlength: `Usa al menos ${LIMITES_ASISTENCIA.nombreCultoMin} caracteres`,
        maxlength: `El nombre no puede superar ${LIMITES_ASISTENCIA.nombreCultoMax} caracteres`
    };
    protected readonly mensajesHora: MensajesError = { required: 'Elige la hora del culto' };

    protected readonly titulo = computed(() => (this.culto() ? `Editar «${this.culto()?.nombre}»` : 'Nuevo culto'));
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const culto = this.culto();
                untracked(() => {
                    this.formulario.reset({ nombre: culto?.nombre ?? '', diaSemana: culto?.diaSemana ?? 'DOMINGO', hora: horaADate(culto?.hora ?? HORA_SUGERIDA) });
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
        const { nombre, diaSemana, hora } = this.formulario.getRawValue();
        this.guardar.emit({ nombre: nombre.trim(), diaSemana, hora: dateAHora(hora) ?? HORA_SUGERIDA });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
