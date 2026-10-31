import { ChangeDetectionStrategy, Component, computed, effect, ElementRef, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map, startWith } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { hoyIso } from '@/app/shared/utils/fecha';
import { ERROR_FECHA_FUTURA, fechaNoFutura } from '@/app/shared/validators/fecha-no-futura';
import { Aporte, DESCRIPCION_MEDIO, GuardarAporte, LIMITES_APORTE, MEDIOS_APORTE, MedioAporte } from '../../models/aporte';
import { CategoriaAporte } from '../../models/configuracion-aportes';
import { leerUltimaSeleccion, recordarSeleccion } from '../../models/ultima-seleccion';

/** Lo que emite el formulario: los datos y si, tras guardar, se sigue registrando otro aporte. */
export interface EnvioAporte {
    readonly datos: GuardarAporte;
    readonly seguir: boolean;
}

/** Medio que se propone si no hay uno recordado. */
const MEDIO_POR_DEFECTO: MedioAporte = 'EFECTIVO';

/**
 * Diálogo para registrar o corregir un aporte (presentacional), pensado para registrar muchos sobres seguidos: el foco
 * va al monto, categoría y medio se eligen con un toque, petición y observación quedan plegadas y «Guardar y
 * registrar otro» (o Ctrl/⌘ + Enter) deja el formulario listo para el siguiente con la misma fecha, categoría y medio.
 */
@Component({
    selector: 'app-formulario-aporte',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputNumberModule, InputTextModule, MessageModule, SelectModule, TextareaModule, MensajeErrorCampoComponent, SelectorMiembroComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-aporte.html'
})
export class FormularioAporteComponent {
    private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
    private readonly fb = inject(NonNullableFormBuilder);

    readonly visible = model(false);
    /** Aporte que se corrige; `null` al registrar. */
    readonly aporte = input<Aporte | null>(null);
    /** Todas las categorías de la iglesia; se ofrecen las activas y, al corregir, también la que ya tiene. */
    readonly categorias = input<readonly CategoriaAporte[]>([]);
    readonly moneda = input.required<string>();
    readonly guardando = input(false);
    readonly error = input<string | null>(null);
    /** Mensajes del backend por campo (`errores` del ProblemDetail). */
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});
    /** Aportes registrados seguidos en esta sesión del diálogo; al aumentar, el formulario se prepara para el siguiente. */
    readonly registrados = input(0);

    readonly guardar = output<EnvioAporte>();

    protected readonly hoy = hoyIso();
    protected readonly limites = LIMITES_APORTE;
    protected readonly medios = MEDIOS_APORTE.map((valor) => ({ valor, etiqueta: DESCRIPCION_MEDIO[valor].etiqueta, icono: DESCRIPCION_MEDIO[valor].icono }));

    protected readonly formulario = this.fb.group({
        monto: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01), Validators.max(LIMITES_APORTE.montoMaximo)]),
        categoriaId: this.fb.control<number | null>(null, Validators.required),
        medio: this.fb.control<MedioAporte>(MEDIO_POR_DEFECTO, Validators.required),
        fecha: [hoyIso(), [Validators.required, fechaNoFutura()]],
        miembro: this.fb.control<OpcionMiembro | null>(null),
        numeroOperacion: ['', Validators.maxLength(LIMITES_APORTE.numeroOperacion)],
        peticion: ['', Validators.maxLength(LIMITES_APORTE.peticion)],
        observacion: ['', Validators.maxLength(LIMITES_APORTE.observacion)]
    });

    protected readonly mensajesMonto: MensajesError = { required: 'Escribe el monto del aporte', min: 'Escribe un monto mayor a 0', max: 'El monto es demasiado grande' };
    protected readonly mensajesCategoria: MensajesError = { required: 'Elige la categoría del aporte' };
    protected readonly mensajesFecha: MensajesError = { required: 'Elige la fecha del aporte', [ERROR_FECHA_FUTURA]: 'La fecha no puede ser posterior a hoy' };
    protected readonly mensajesMiembro: MensajesError = { required: 'Esta categoría exige indicar quién aporta: búscalo por su nombre' };
    protected readonly mensajesOperacion: MensajesError = { maxlength: `Máximo ${LIMITES_APORTE.numeroOperacion} caracteres` };
    protected readonly mensajesPeticion: MensajesError = { maxlength: `Máximo ${LIMITES_APORTE.peticion} caracteres` };
    protected readonly mensajesObservacion: MensajesError = { maxlength: `Máximo ${LIMITES_APORTE.observacion} caracteres` };

    private readonly categoriaId = toSignal(this.formulario.controls.categoriaId.valueChanges.pipe(startWith(this.formulario.controls.categoriaId.value)), { initialValue: null });
    private readonly medio = toSignal(this.formulario.controls.medio.valueChanges.pipe(startWith(this.formulario.controls.medio.value)), { initialValue: MEDIO_POR_DEFECTO });
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    protected readonly editando = computed(() => this.aporte() !== null);
    protected readonly titulo = computed(() => (this.editando() ? 'Corregir aporte' : 'Registrar aporte'));
    protected readonly opcionesCategoria = computed(() => {
        const actual = this.aporte()?.categoria.id;
        return this.categorias()
            .filter(({ activo, id }) => activo || id === actual)
            .map(({ id, nombre }) => ({ id, nombre }));
    });
    protected readonly categoria = computed(() => this.categorias().find(({ id }) => id === this.categoriaId()) ?? null);
    protected readonly exigeAportante = computed(() => this.categoria()?.exigeAportante ?? false);
    protected readonly ayudaOperacion = computed(() => DESCRIPCION_MEDIO[this.medio()].ayudaOperacion);

    constructor() {
        effect(() => {
            if (this.visible()) {
                const aporte = this.aporte();
                untracked(() => this.preparar(aporte));
            }
        });
        effect(() => {
            if (this.registrados() > 0) {
                untracked(() => this.prepararSiguiente());
            }
        });
        effect(() => {
            const exige = this.exigeAportante();
            untracked(() => {
                const miembro = this.formulario.controls.miembro;
                miembro.setValidators(exige ? Validators.required : null);
                miembro.updateValueAndValidity();
            });
        });
        effect(() => {
            const errores = this.erroresCampo();
            untracked(() => this.marcarErroresDelServidor(errores));
        });
    }

    /** Ctrl/⌘ + Enter guarda y sigue registrando (al corregir, solo guarda). */
    protected atajo(evento: KeyboardEvent): void {
        if ((evento.ctrlKey || evento.metaKey) && evento.key === 'Enter') {
            evento.preventDefault();
            this.enviar(!this.editando());
        }
    }

    protected enviar(seguir: boolean): void {
        if (this.formulario.invalid || this.guardando()) {
            this.formulario.markAllAsTouched();
            return;
        }
        const valor = this.formulario.getRawValue();
        const medio = valor.medio;
        recordarSeleccion({ categoriaId: valor.categoriaId, medio });
        this.guardar.emit({
            seguir,
            datos: {
                fecha: valor.fecha,
                miembroId: valor.miembro?.id ?? null,
                categoriaId: valor.categoriaId as number,
                monto: valor.monto as number,
                medio,
                numeroOperacion: medio === 'EFECTIVO' ? null : vacioANulo(valor.numeroOperacion),
                peticion: vacioANulo(valor.peticion),
                observacion: vacioANulo(valor.observacion)
            }
        });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    protected enfocarMonto(): void {
        queueMicrotask(() => this.host.nativeElement.ownerDocument.getElementById('aporte-monto')?.focus());
    }

    /** Al abrir: los datos del aporte que se corrige, o hoy con la última categoría y medio usados. */
    private preparar(aporte: Aporte | null): void {
        if (aporte) {
            this.formulario.reset({
                monto: aporte.monto,
                categoriaId: aporte.categoria.id,
                medio: aporte.medio,
                fecha: aporte.fecha,
                miembro: aporte.miembro ? { id: aporte.miembro.id, nombre: aporte.miembro.nombre } : null,
                numeroOperacion: aporte.numeroOperacion ?? '',
                peticion: aporte.peticion ?? '',
                observacion: aporte.observacion ?? ''
            });
        } else {
            const ultima = leerUltimaSeleccion();
            this.formulario.reset({ monto: null, categoriaId: this.categoriaInicial(ultima.categoriaId), medio: ultima.medio ?? MEDIO_POR_DEFECTO, fecha: hoyIso(), miembro: null, numeroOperacion: '', peticion: '', observacion: '' });
        }
        this.formulario.updateValueAndValidity();
        this.enfocarMonto();
    }

    /** Tras «Guardar y registrar otro»: conserva fecha, categoría y medio y limpia lo demás. */
    private prepararSiguiente(): void {
        const { fecha, categoriaId, medio } = this.formulario.getRawValue();
        this.formulario.reset({ monto: null, categoriaId, medio, fecha, miembro: null, numeroOperacion: '', peticion: '', observacion: '' });
        this.formulario.updateValueAndValidity();
        this.enfocarMonto();
    }

    /** La recordada si sigue activa; si no, la primera activa (normalmente Diezmo). */
    private categoriaInicial(recordada: number | null): number | null {
        const activas = this.categorias().filter(({ activo }) => activo);
        return activas.find(({ id }) => id === recordada)?.id ?? activas[0]?.id ?? null;
    }

    private marcarErroresDelServidor(errores: Readonly<Record<string, string>>): void {
        const campos: Readonly<Record<string, keyof typeof this.formulario.controls>> = { miembroId: 'miembro', categoriaId: 'categoriaId', monto: 'monto', fecha: 'fecha', numeroOperacion: 'numeroOperacion' };
        Object.keys(errores).forEach((campo) => {
            const control = campos[campo] ? this.formulario.controls[campos[campo]] : null;
            control?.markAsTouched();
        });
    }
}

function vacioANulo(texto: string): string | null {
    const limpio = texto.trim();
    return limpio ? limpio : null;
}
