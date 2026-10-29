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
import { DESCRIPCION_GRUPO_MAX, DIAS_SEMANA, DiaSemana, DIRECCION_GRUPO_MAX, ETIQUETA_DIA, Grupo, GuardarGrupo, nombreDeGrupo } from '../../models/grupo';

/**
 * Diálogo para crear o editar un grupo (presentacional). El grupo no tiene nombre: es «el grupo de» su líder, que es
 * el único dato obligatorio. Los errores del backend por campo (p. ej. un líder que ya tiene grupo) se muestran junto
 * al campo.
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

    protected readonly limites = { direccion: DIRECCION_GRUPO_MAX, descripcion: DESCRIPCION_GRUPO_MAX };
    protected readonly opcionesDia = DIAS_SEMANA.map((dia) => ({ etiqueta: ETIQUETA_DIA[dia], valor: dia }));

    private readonly fb = inject(NonNullableFormBuilder);
    protected readonly formulario = this.fb.group({
        lider: new FormControl<OpcionMiembro | null>(null, Validators.required),
        anfitrion: new FormControl<OpcionMiembro | null>(null),
        diaReunion: new FormControl<DiaSemana | null>(null),
        horaReunion: '',
        direccion: ['', Validators.maxLength(DIRECCION_GRUPO_MAX)],
        distritoCodigo: new FormControl<string | null>(null),
        descripcion: ['', Validators.maxLength(DESCRIPCION_GRUPO_MAX)]
    });

    protected readonly mensajesLider: MensajesError = { required: 'Elige al líder: el grupo lleva su nombre' };
    protected readonly mensajesDireccion: MensajesError = { maxlength: `La dirección no puede superar ${DIRECCION_GRUPO_MAX} caracteres` };
    protected readonly mensajesDescripcion: MensajesError = { maxlength: `La descripción no puede superar ${DESCRIPCION_GRUPO_MAX} caracteres` };

    protected readonly titulo = computed(() => {
        const grupo = this.grupo();
        return grupo ? `Editar ${nombreDeGrupo(this.nombreSingular().toLowerCase(), grupo.lider.nombre)}` : `Agregar ${this.nombreSingular().toLowerCase()}`;
    });

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
        const { lider, anfitrion, diaReunion, horaReunion, direccion, distritoCodigo, descripcion } = this.formulario.getRawValue();
        if (!lider) {
            return;
        }
        this.guardar.emit({
            liderId: lider.id,
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
