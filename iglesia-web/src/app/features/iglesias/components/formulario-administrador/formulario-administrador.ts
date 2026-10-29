import { ChangeDetectionStrategy, Component, effect, inject, input, model, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';
import { NuevoAdministrador } from '../../models/administrador-iglesia';
import { CamposAdministradorComponent, crearGrupoAdministrador } from '../campos-administrador/campos-administrador';

/**
 * Diálogo para crear un administrador de una iglesia existente (presentacional), por ejemplo cuando el alta de la
 * iglesia no llegó a crear el suyo. Emite correo y contraseña temporal; el contenedor crea el usuario.
 */
@Component({
    selector: 'app-formulario-administrador',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, MessageModule, CamposAdministradorComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-administrador.html'
})
export class FormularioAdministradorComponent {
    readonly visible = model(false);
    /** Iglesia a la que pertenecerá el administrador. */
    readonly iglesia = input<Iglesia | null>(null);
    readonly guardando = input(false);
    /** Mensaje del backend (400 o 409) que se muestra dentro del diálogo. */
    readonly error = input<string | null>(null);

    readonly crear = output<NuevoAdministrador>();

    protected readonly formulario = crearGrupoAdministrador(inject(NonNullableFormBuilder));

    /** Los controles no son signals: se escucha el estado para habilitar «Crear» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        // Cada vez que se abre empieza vacío: la contraseña anterior no debe quedar en el formulario.
        effect(() => {
            if (this.visible()) {
                this.formulario.reset();
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { correo, contrasenaTemporal } = this.formulario.getRawValue();
        this.crear.emit({ correo: correo.trim(), contrasenaTemporal });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
