import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { LIMITES_REGLAS, ReglasSeguimiento } from '../../models/seguimiento';

type CampoRegla = keyof typeof LIMITES_REGLAS;

const mensajesDe = (campo: CampoRegla, unidad: string): MensajesError => {
    const { min, max } = LIMITES_REGLAS[campo];
    const rango = `Escribe un número de ${min} a ${max} ${unidad}`;
    return { required: rango, min: rango, max: rango };
};

/**
 * Reglas de seguimiento (presentacional): cuántas faltas seguidas dan la alerta, cuántos puntos de caída cuentan como
 * «asistencia en caída» y hasta cuántos días desde su ingreso alguien se considera nuevo.
 */
@Component({
    selector: 'app-formulario-reglas',
    imports: [ReactiveFormsModule, ButtonModule, InputNumberModule, MessageModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-reglas.html'
})
export class FormularioReglasComponent {
    private readonly fb = inject(NonNullableFormBuilder);

    readonly reglas = input.required<ReglasSeguimiento>();
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly guardar = output<ReglasSeguimiento>();

    protected readonly limites = LIMITES_REGLAS;
    protected readonly formulario = this.fb.group({
        faltasSeguidas: this.controlDe('faltasSeguidas'),
        puntosCaida: this.controlDe('puntosCaida'),
        diasNuevo: this.controlDe('diasNuevo')
    });
    protected readonly mensajesFaltas = mensajesDe('faltasSeguidas', 'faltas');
    protected readonly mensajesCaida = mensajesDe('puntosCaida', 'puntos');
    protected readonly mensajesNuevo = mensajesDe('diasNuevo', 'días');

    constructor() {
        effect(() => {
            this.formulario.reset(this.reglas());
        });
    }

    private controlDe(campo: CampoRegla) {
        const { min, max } = LIMITES_REGLAS[campo];
        return this.fb.control<number>(min, [Validators.required, Validators.min(min), Validators.max(max)]);
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        this.guardar.emit(this.formulario.getRawValue());
    }
}
