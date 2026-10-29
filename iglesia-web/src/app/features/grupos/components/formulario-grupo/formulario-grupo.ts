import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormControl, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { MensajeErrorServidorComponent } from '@/app/shared/components/mensaje-error-servidor/mensaje-error-servidor';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { SelectorUbigeoComponent } from '@/app/shared/components/selector-ubigeo/selector-ubigeo';
import { textoOpcional } from '@/app/shared/utils/texto';
import { DESCRIPCION_GRUPO_MAX, DIAS_SEMANA, DiaSemana, DIRECCION_GRUPO_MAX, ETIQUETA_DIA, Grupo, GuardarGrupo, NOMBRE_GRUPO_MAX } from '../../models/grupo';

/** Exige al menos un carácter que no sea espacio. */
const CON_TEXTO = /\S/;

/**
 * Diálogo para crear o editar un grupo (presentacional). Solo el nombre es obligatorio. Los errores del backend por
 * campo (p. ej. un líder inactivo) se muestran junto al campo.
 */
@Component({
    selector: 'app-formulario-grupo',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, SelectModule, TextareaModule, MensajeErrorCampoComponent, MensajeErrorServidorComponent, SelectorMiembroComponent, SelectorUbigeoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-grupo.html'
})
export class FormularioGrupoComponent {
    readonly visible = model(false);
    /** Grupo que se edita; `null` al crear. */
    readonly grupo = input<Grupo | null>(null);
    /** Nombre singular de los grupos en la iglesia (p. ej. «Célula»). */
    readonly nombreSingular = input('Grupo');
    readonly guardando = input(false);
    /** Mensaje general del backend que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);
    /** Mensajes del backend por campo (`errores` del ProblemDetail). */
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});

    readonly guardar = output<GuardarGrupo>();

    protected readonly limites = { nombre: NOMBRE_GRUPO_MAX, direccion: DIRECCION_GRUPO_MAX, descripcion: DESCRIPCION_GRUPO_MAX };
    protected readonly opcionesDia = DIAS_SEMANA.map((dia) => ({ etiqueta: ETIQUETA_DIA[dia], valor: dia }));

    private readonly fb = inject(NonNullableFormBuilder);
    protected readonly formulario = this.fb.group({
        nombre: ['', [Validators.required, Validators.pattern(CON_TEXTO), Validators.maxLength(NOMBRE_GRUPO_MAX)]],
        lider: new FormControl<OpcionMiembro | null>(null),
        anfitrion: new FormControl<OpcionMiembro | null>(null),
        diaReunion: new FormControl<DiaSemana | null>(null),
        horaReunion: '',
        direccion: ['', Validators.maxLength(DIRECCION_GRUPO_MAX)],
        distritoCodigo: new FormControl<string | null>(null),
        descripcion: ['', Validators.maxLength(DESCRIPCION_GRUPO_MAX)]
    });

    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre',
        pattern: 'Escribe el nombre',
        maxlength: `El nombre no puede superar ${NOMBRE_GRUPO_MAX} caracteres`
    };
    protected readonly mensajesDireccion: MensajesError = { maxlength: `La dirección no puede superar ${DIRECCION_GRUPO_MAX} caracteres` };
    protected readonly mensajesDescripcion: MensajesError = { maxlength: `La descripción no puede superar ${DESCRIPCION_GRUPO_MAX} caracteres` };

    protected readonly titulo = computed(() => (this.grupo() ? `Editar «${this.grupo()?.nombre}»` : `Agregar ${this.nombreSingular().toLowerCase()}`));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const grupo = this.grupo();
                untracked(() => this.reiniciar(grupo));
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { nombre, lider, anfitrion, diaReunion, horaReunion, direccion, distritoCodigo, descripcion } = this.formulario.getRawValue();
        this.guardar.emit({
            nombre: nombre.trim(),
            liderId: lider?.id ?? null,
            anfitrionId: anfitrion?.id ?? null,
            diaReunion,
            horaReunion: horaReunion || null,
            direccion: textoOpcional(direccion),
            distritoCodigo,
            descripcion: textoOpcional(descripcion)
        });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    private reiniciar(grupo: Grupo | null): void {
        this.formulario.reset({
            nombre: grupo?.nombre ?? '',
            lider: grupo?.lider ?? null,
            anfitrion: grupo?.anfitrion ?? null,
            diaReunion: grupo?.diaReunion ?? null,
            horaReunion: grupo?.horaReunion ?? '',
            direccion: grupo?.direccion ?? '',
            distritoCodigo: grupo?.distritoCodigo ?? null,
            descripcion: grupo?.descripcion ?? ''
        });
        this.formulario.updateValueAndValidity();
    }
}
