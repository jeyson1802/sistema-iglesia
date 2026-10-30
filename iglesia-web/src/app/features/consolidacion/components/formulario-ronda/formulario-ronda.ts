import { ChangeDetectionStrategy, Component, effect, inject, input, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { MensajeErrorServidorComponent } from '@/app/shared/components/mensaje-error-servidor/mensaje-error-servidor';
import { hoyIso } from '@/app/shared/utils/fecha';
import { DatosRonda, LIMITES_CONSOLIDACION, nombreSugerido } from '../../models/consolidacion';

/** Atajos de duración de la ronda, en días desde el inicio. */
const ATAJOS_DURACION = [
    { etiqueta: '2 semanas', dias: 13 },
    { etiqueta: '1 mes', dias: 29 },
    { etiqueta: '2 meses', dias: 59 }
] as const;
const MS_POR_DIA = 86_400_000;

function finNoAnterior(grupo: AbstractControl): ValidationErrors | null {
    const { fechaInicio, fechaFin } = grupo.value as { fechaInicio: Date | null; fechaFin: Date | null };
    return fechaInicio && fechaFin && fechaFin < fechaInicio ? { finAnterior: true } : null;
}

const aFecha = (iso: string | null): Date | null => {
    if (!iso) {
        return null;
    }
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia);
};

/**
 * Datos de una ronda (presentacional): nombre con sugerencia, fechas con atajos («2 semanas», «1 mes») y descripción.
 * Se usa en el paso 1 del asistente y al editar. Los errores del backend por campo se muestran junto a cada campo.
 */
@Component({
    selector: 'app-formulario-ronda',
    imports: [ReactiveFormsModule, ButtonModule, DatePickerModule, InputTextModule, MessageModule, TextareaModule, MensajeErrorCampoComponent, MensajeErrorServidorComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-ronda.html'
})
export class FormularioRondaComponent {
    /** Datos actuales al editar; sin datos, la ronda nueva con nombre sugerido y empezando hoy. */
    readonly datos = input<DatosRonda | null>(null);
    readonly guardando = input(false);
    readonly error = input<string | null>(null);
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});
    readonly etiquetaGuardar = input('Guardar');
    readonly iconoGuardar = input('pi pi-check');
    readonly mostrarCancelar = input(true);

    readonly guardar = output<DatosRonda>();
    readonly cancelar = output<void>();

    protected readonly limites = LIMITES_CONSOLIDACION;
    protected readonly atajos = ATAJOS_DURACION;
    protected readonly formulario = inject(NonNullableFormBuilder).group(
        {
            nombre: ['', [Validators.required, Validators.pattern(/\S/), Validators.minLength(LIMITES_CONSOLIDACION.nombreRondaMin), Validators.maxLength(LIMITES_CONSOLIDACION.nombreRondaMax)]],
            fechaInicio: [new Date() as Date | null, Validators.required],
            fechaFin: [null as Date | null],
            descripcion: ['', Validators.maxLength(LIMITES_CONSOLIDACION.descripcionMax)]
        },
        { validators: finNoAnterior }
    );
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre de la ronda',
        pattern: 'Escribe el nombre de la ronda',
        minlength: `Usa al menos ${LIMITES_CONSOLIDACION.nombreRondaMin} caracteres`,
        maxlength: `El nombre no puede superar ${LIMITES_CONSOLIDACION.nombreRondaMax} caracteres`
    };
    protected readonly mensajesInicio: MensajesError = { required: 'Elige la fecha de inicio' };
    protected readonly finAnterior = toSignal(this.formulario.statusChanges.pipe(map(() => this.formulario.hasError('finAnterior'))), { initialValue: false });
    protected readonly inicio = toSignal(this.formulario.controls.fechaInicio.valueChanges, { initialValue: new Date() as Date | null });
    protected readonly largoDescripcion = toSignal(this.formulario.controls.descripcion.valueChanges.pipe(map((texto) => texto.length)), { initialValue: 0 });

    constructor() {
        effect(() => {
            const datos = this.datos();
            untracked(() =>
                this.formulario.reset({
                    nombre: datos?.nombre ?? nombreSugerido(),
                    fechaInicio: aFecha(datos?.fechaInicio ?? null) ?? new Date(),
                    fechaFin: aFecha(datos?.fechaFin ?? null),
                    descripcion: datos?.descripcion ?? ''
                })
            );
        });
    }

    protected aplicarAtajo(dias: number): void {
        const inicio = this.formulario.controls.fechaInicio.value ?? new Date();
        this.formulario.controls.fechaFin.setValue(new Date(inicio.getTime() + dias * MS_POR_DIA));
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { nombre, fechaInicio, fechaFin, descripcion } = this.formulario.getRawValue();
        this.guardar.emit({ nombre: nombre.trim(), fechaInicio: hoyIso(fechaInicio ?? new Date()), fechaFin: fechaFin ? hoyIso(fechaFin) : null, descripcion: descripcion.trim() || null });
    }
}
