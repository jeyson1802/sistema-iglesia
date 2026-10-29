import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { Iglesia, NOMBRE_IGLESIA_MAX } from '@/app/core/iglesias/models/iglesia';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { AltaIglesia, ModoFormularioIglesia } from '../../models/administrador-iglesia';
import { CamposAdministradorComponent, crearGrupoAdministrador } from '../campos-administrador/campos-administrador';

/** Un nombre hecho solo de espacios no es válido: el backend lo guarda sin espacios al inicio ni al final. */
const NOMBRE_CON_TEXTO = /\S/;

/**
 * Diálogo para dar de alta una iglesia con su primer administrador, o para editar su nombre (presentacional). Al
 * editar, los campos del administrador se deshabilitan para que no cuenten en la validación.
 */
@Component({
    selector: 'app-formulario-iglesia',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, CamposAdministradorComponent, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-iglesia.html'
})
export class FormularioIglesiaComponent {
    private readonly fb = inject(NonNullableFormBuilder);

    readonly visible = model(false);
    readonly modo = input<ModoFormularioIglesia>('crear');
    /** Iglesia que se edita; `null` al crear. */
    readonly iglesia = input<Iglesia | null>(null);
    readonly guardando = input(false);
    /** Mensaje del backend (400, 404 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly crear = output<AltaIglesia>();
    readonly renombrar = output<string>();

    protected readonly nombreMax = NOMBRE_IGLESIA_MAX;

    protected readonly formulario = this.fb.group({
        nombre: ['', [Validators.required, Validators.pattern(NOMBRE_CON_TEXTO), Validators.maxLength(NOMBRE_IGLESIA_MAX)]],
        administrador: crearGrupoAdministrador(this.fb)
    });

    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre de la iglesia',
        pattern: 'Escribe el nombre de la iglesia',
        maxlength: `El nombre no puede superar ${NOMBRE_IGLESIA_MAX} caracteres`
    };

    protected readonly titulo = computed(() => (this.modo() === 'crear' ? 'Nueva iglesia' : `Editar «${this.iglesia()?.nombre ?? ''}»`));

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Al abrir el diálogo (o cambiar de iglesia) se reinicia el formulario con sus datos.
        effect(() => {
            if (this.visible()) {
                const [iglesia, modo] = [this.iglesia(), this.modo()];
                untracked(() => this.reiniciar(iglesia, modo));
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { nombre, administrador } = this.formulario.getRawValue();
        if (this.modo() === 'crear') {
            this.crear.emit({ nombre: nombre.trim(), administrador: { correo: administrador.correo.trim(), contrasenaTemporal: administrador.contrasenaTemporal } });
        } else {
            this.renombrar.emit(nombre.trim());
        }
    }

    protected cerrar(): void {
        this.visible.set(false);
    }

    private reiniciar(iglesia: Iglesia | null, modo: ModoFormularioIglesia): void {
        this.formulario.reset({ nombre: modo === 'editar' ? (iglesia?.nombre ?? '') : '' });
        const { administrador } = this.formulario.controls;
        if (modo === 'crear') {
            administrador.enable();
        } else {
            administrador.disable();
        }
    }
}
