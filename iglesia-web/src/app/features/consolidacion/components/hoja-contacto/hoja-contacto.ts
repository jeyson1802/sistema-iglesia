import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map, startWith } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { Referencia } from '@/app/core/models/referencia';
import { MensajeErrorServidorComponent } from '@/app/shared/components/mensaje-error-servidor/mensaje-error-servidor';
import { hoyIso } from '@/app/shared/utils/fecha';
import { esPantallaPequena } from '@/app/shared/utils/pantalla';
import { Contacto, DatosContacto, enDias, ETIQUETA_MEDIO, etiquetaResultado, ICONO_MEDIO, ICONO_RESULTADO, LIMITES_CONSOLIDACION, MedioContacto, MEDIOS, ResultadoContacto, RESULTADOS_POR_MEDIO, SEVERIDAD_RESULTADO } from '../../models/consolidacion';
import { GrupoOpcionesComponent, OpcionGrupo } from '../grupo-opciones/grupo-opciones';

/** Atajos del próximo contacto, en días desde hoy. */
const ATAJOS_PROXIMO = [
    { etiqueta: 'Mañana', dias: 1 },
    { etiqueta: 'En 3 días', dias: 3 },
    { etiqueta: 'En 1 semana', dias: 7 }
] as const;

const aFecha = (iso: string): Date => {
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia);
};

/**
 * Hoja «Registrar contacto» (presentacional): medio y resultado como botones grandes (solo los resultados que aplican
 * al medio), fecha y hora (ahora por defecto), observaciones y próximo contacto con atajos. Sale desde abajo en el
 * celular y como diálogo en el escritorio. Si el guardado falla, sigue abierta con los datos y el error. Con
 * `contacto` corrige uno existente.
 */
@Component({
    selector: 'app-hoja-contacto',
    imports: [ReactiveFormsModule, ButtonModule, DatePickerModule, DialogModule, MessageModule, SelectModule, TextareaModule, MensajeErrorServidorComponent, GrupoOpcionesComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './hoja-contacto.html'
})
export class HojaContactoComponent {
    readonly visible = model(false);
    /** Nombre de la persona, para el título («¿Cómo te fue con la llamada a Ana?»). */
    readonly nombre = input.required<string>();
    /** Medio ya elegido (el del botón que se tocó). */
    readonly medioInicial = input<MedioContacto | null>(null);
    /** Contacto a corregir; sin él, se registra uno nuevo. */
    readonly contacto = input<Contacto | null>(null);
    /** Personas en cuyo nombre puede registrar el coordinador; vacío, no se pregunta quién contactó. */
    readonly consolidadores = input<readonly Referencia[]>([]);
    protected readonly opcionesConsolidadores = computed(() => [...this.consolidadores()]);
    readonly consolidadorPorDefecto = input<number | null>(null);
    readonly guardando = input(false);
    readonly error = input<string | null>(null);
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});
    /** Ofrece «¿Terminó el seguimiento? Cerrar caso» cuando se habló con la persona. */
    readonly puedeCerrar = input(false);

    readonly guardar = output<DatosContacto>();
    /** Guarda el contacto y pasa directo al cierre del caso. */
    readonly guardarYCerrar = output<DatosContacto>();

    protected readonly limites = LIMITES_CONSOLIDACION;
    protected readonly atajos = ATAJOS_PROXIMO;
    protected readonly opcionesMedio: readonly OpcionGrupo<MedioContacto>[] = MEDIOS.map((medio) => ({ valor: medio, etiqueta: ETIQUETA_MEDIO[medio], icono: ICONO_MEDIO[medio] }));
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        medio: [null as MedioContacto | null, Validators.required],
        resultado: [null as ResultadoContacto | null, Validators.required],
        fechaHora: [new Date(), Validators.required],
        observaciones: ['', Validators.maxLength(LIMITES_CONSOLIDACION.observacionesMax)],
        proximoContacto: [null as Date | null],
        consolidadorId: [null as number | null]
    });
    protected readonly abajo = computed(() => this.visible() && esPantallaPequena());
    protected readonly medio = toSignal(this.formulario.controls.medio.valueChanges.pipe(startWith(this.formulario.controls.medio.value)), { initialValue: null });
    protected readonly resultado = toSignal(this.formulario.controls.resultado.valueChanges.pipe(startWith(this.formulario.controls.resultado.value)), { initialValue: null });
    protected readonly largoObservaciones = toSignal(this.formulario.controls.observaciones.valueChanges.pipe(map((texto) => texto.length)), { initialValue: 0 });
    protected readonly proximo = toSignal(this.formulario.controls.proximoContacto.valueChanges, { initialValue: null as Date | null });
    protected readonly opcionesResultado = computed<readonly OpcionGrupo<ResultadoContacto>[]>(() => {
        const medio = this.medio();
        return medio ? RESULTADOS_POR_MEDIO[medio].map((resultado) => ({ valor: resultado, etiqueta: etiquetaResultado(resultado, medio), icono: ICONO_RESULTADO[resultado], severidad: SEVERIDAD_RESULTADO[resultado] })) : [];
    });
    protected readonly titulo = computed(() => {
        if (this.contacto()) {
            return 'Corregir contacto';
        }
        const nombre = this.nombre().split(' ')[0];
        switch (this.medioInicial()) {
            case 'LLAMADA':
                return `¿Cómo te fue con la llamada a ${nombre}?`;
            case 'WHATSAPP':
                return `¿Cómo te fue con el mensaje a ${nombre}?`;
            default:
                return `Registrar contacto con ${nombre}`;
        }
    });
    protected readonly ofrecerCierre = computed(() => this.puedeCerrar() && !this.contacto() && this.resultado() === 'SE_HABLO');
    protected readonly ahora = new Date();

    constructor() {
        effect(() => {
            if (this.visible()) {
                const contacto = this.contacto();
                const medio = this.medioInicial();
                untracked(() => this.preparar(contacto, medio));
            }
        });
        // Al cambiar de medio se descarta un resultado que no le corresponde (p. ej. «No estaba» en una llamada).
        this.formulario.controls.medio.valueChanges.subscribe((medio) => {
            const resultado = this.formulario.controls.resultado.value;
            if (medio && resultado && !RESULTADOS_POR_MEDIO[medio].includes(resultado)) {
                this.formulario.controls.resultado.setValue(null);
            }
            if (medio === 'EN_LA_IGLESIA') {
                this.formulario.controls.resultado.setValue('SE_HABLO');
            }
        });
    }

    protected aplicarAtajo(dias: number): void {
        this.formulario.controls.proximoContacto.setValue(aFecha(enDias(dias)));
    }

    protected esAtajo(dias: number): boolean {
        const proximo = this.proximo();
        return proximo !== null && hoyIso(proximo) === enDias(dias);
    }

    protected enviar(cerrar = false): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { medio, resultado, fechaHora, observaciones, proximoContacto, consolidadorId } = this.formulario.getRawValue();
        const datos: DatosContacto = {
            fechaHora: fechaHora.toISOString(),
            medio: medio!,
            resultado: resultado!,
            observaciones: observaciones.trim() || null,
            proximoContacto: proximoContacto ? hoyIso(proximoContacto) : null,
            ...(this.consolidadores().length > 0 ? { consolidadorId } : {})
        };
        (cerrar ? this.guardarYCerrar : this.guardar).emit(datos);
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    private preparar(contacto: Contacto | null, medio: MedioContacto | null): void {
        this.formulario.reset({
            medio: contacto?.medio ?? medio,
            resultado: contacto?.resultado ?? (medio === 'EN_LA_IGLESIA' ? 'SE_HABLO' : null),
            fechaHora: contacto ? new Date(contacto.fechaHora) : new Date(),
            observaciones: contacto?.observaciones ?? '',
            proximoContacto: contacto?.proximoContacto ? aFecha(contacto.proximoContacto) : null,
            consolidadorId: contacto?.consolidador?.id ?? this.consolidadorPorDefecto()
        });
    }
}
