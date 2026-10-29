import { ChangeDetectionStrategy, Component, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { NombresGrupos } from '@/app/core/configuracion/configuracion-iglesia.service';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { NOMBRE_TIPO_GRUPO_MAX } from '../../models/grupo';

const VALIDADORES = [Validators.required, Validators.pattern(/\S/), Validators.maxLength(NOMBRE_TIPO_GRUPO_MAX)];

/** Diálogo «Cómo se llaman los grupos» (presentacional): nombre singular y plural del grupo pequeño en la iglesia. */
@Component({
    selector: 'app-nombres-grupos',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './nombres-grupos.html'
})
export class NombresGruposComponent {
    readonly visible = model(false);
    readonly nombres = input.required<NombresGrupos>();
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly guardar = output<NombresGrupos>();

    protected readonly nombreMax = NOMBRE_TIPO_GRUPO_MAX;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombreGrupoSingular: ['', VALIDADORES],
        nombreGrupoPlural: ['', VALIDADORES]
    });
    protected readonly mensajesSingular: MensajesError = {
        required: 'Escribe el nombre en singular',
        pattern: 'Escribe el nombre en singular',
        maxlength: `Usa como máximo ${NOMBRE_TIPO_GRUPO_MAX} caracteres`
    };
    protected readonly mensajesPlural: MensajesError = {
        required: 'Escribe el nombre en plural',
        pattern: 'Escribe el nombre en plural',
        maxlength: `Usa como máximo ${NOMBRE_TIPO_GRUPO_MAX} caracteres`
    };

    /** Los controles no son signals: se escucha el estado para habilitar «Guardar» en una app zoneless. */
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const nombres = this.nombres();
                untracked(() => {
                    this.formulario.reset(nombres);
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
        const { nombreGrupoSingular, nombreGrupoPlural } = this.formulario.getRawValue();
        this.guardar.emit({ nombreGrupoSingular: nombreGrupoSingular.trim(), nombreGrupoPlural: nombreGrupoPlural.trim() });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
