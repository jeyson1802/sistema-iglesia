import { ChangeDetectionStrategy, Component, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { map } from 'rxjs';
import { CamposIglesiaComponent, crearGrupoIglesia, datosDelGrupo } from '@/app/shared/components/campos-iglesia/campos-iglesia';
import { AltaIglesia } from '../../models/administrador-iglesia';
import { CamposAdministradorComponent, crearGrupoAdministrador } from '../campos-administrador/campos-administrador';

/**
 * Diálogo para dar de alta una iglesia con su identidad y su primer administrador (presentacional). El logo y la
 * apariencia se eligen después en la página de la iglesia.
 */
@Component({
    selector: 'app-formulario-iglesia',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, MessageModule, CamposAdministradorComponent, CamposIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-iglesia.html'
})
export class FormularioIglesiaComponent {
    private readonly fb = inject(NonNullableFormBuilder);

    readonly visible = model(false);
    readonly guardando = input(false);
    /** Mensaje del backend (400 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);
    /** Mensajes del backend por campo. */
    readonly erroresCampo = input<Readonly<Record<string, string>>>({});

    readonly crear = output<AltaIglesia>();

    protected readonly formulario = this.fb.group({
        iglesia: crearGrupoIglesia(this.fb),
        administrador: crearGrupoAdministrador(this.fb)
    });

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Cada vez que se abre, el formulario empieza vacío.
        effect(() => {
            if (this.visible()) {
                untracked(() => this.formulario.reset());
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { administrador } = this.formulario.getRawValue();
        this.crear.emit({ datos: datosDelGrupo(this.formulario.controls.iglesia), administrador: { correo: administrador.correo.trim(), contrasenaTemporal: administrador.contrasenaTemporal } });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
