import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { MensajeErrorCampoComponent, MensajesError } from '@/app/shared/components/mensaje-error-campo/mensaje-error-campo';
import { CategoriaAporte, GuardarCategoriaAporte, NOMBRE_CATEGORIA_MAX } from '../../models/configuracion-aportes';

/** Diálogo para crear o editar una categoría de aporte (presentacional). Emite el nombre limpio y si exige aportante. */
@Component({
    selector: 'app-formulario-categoria',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputTextModule, MessageModule, ToggleSwitchModule, MensajeErrorCampoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './formulario-categoria.html'
})
export class FormularioCategoriaComponent {
    readonly visible = model(false);
    /** Categoría que se edita; `null` al crear. */
    readonly categoria = input<CategoriaAporte | null>(null);
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly guardar = output<GuardarCategoriaAporte>();

    protected readonly nombreMax = NOMBRE_CATEGORIA_MAX;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        nombre: ['', [Validators.required, Validators.maxLength(NOMBRE_CATEGORIA_MAX), Validators.pattern(/\S/)]],
        exigeAportante: [false]
    });
    protected readonly mensajesNombre: MensajesError = {
        required: 'Escribe el nombre de la categoría',
        pattern: 'Escribe el nombre de la categoría',
        maxlength: `El nombre no puede superar ${NOMBRE_CATEGORIA_MAX} caracteres`
    };

    protected readonly titulo = computed(() => (this.categoria() ? `Editar «${this.categoria()?.nombre}»` : 'Nueva categoría'));
    protected readonly valido = toSignal(this.formulario.statusChanges.pipe(map((estado) => estado === 'VALID')), { initialValue: false });
    protected readonly exige = toSignal(this.formulario.controls.exigeAportante.valueChanges, { initialValue: false });

    constructor() {
        effect(() => {
            if (this.visible()) {
                const categoria = this.categoria();
                untracked(() => {
                    this.formulario.reset({ nombre: categoria?.nombre ?? '', exigeAportante: categoria?.exigeAportante ?? false });
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
        const { nombre, exigeAportante } = this.formulario.getRawValue();
        this.guardar.emit({ nombre: nombre.trim(), exigeAportante });
    }

    protected cerrar(): void {
        this.visible.set(false);
    }
}
