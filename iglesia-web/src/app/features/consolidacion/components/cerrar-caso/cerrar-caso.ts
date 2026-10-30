import { ChangeDetectionStrategy, Component, computed, effect, inject, input, model, output, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { map, startWith } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';
import { esPantallaPequena } from '@/app/shared/utils/pantalla';
import { LIMITES_CONSOLIDACION, ResultadoConsolidacion } from '../../models/consolidacion';
import { GrupoOpcionesComponent, OpcionGrupo } from '../grupo-opciones/grupo-opciones';

/** Lo que se envía al cerrar un caso. */
export interface CierreCaso {
    readonly resultadoId: number;
    readonly observacion: string | null;
}

/**
 * Diálogo «Cerrar caso» (presentacional): los resultados activos del catálogo como botones (en su orden), observación
 * final opcional y el resumen de lo que se hará antes de confirmar.
 */
@Component({
    selector: 'app-cerrar-caso',
    imports: [ReactiveFormsModule, ButtonModule, DialogModule, MessageModule, TextareaModule, GrupoOpcionesComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './cerrar-caso.html'
})
export class CerrarCasoComponent {
    readonly visible = model(false);
    readonly nombre = input.required<string>();
    readonly resultados = input.required<readonly ResultadoConsolidacion[]>();
    readonly guardando = input(false);
    readonly error = input<string | null>(null);

    readonly cerrar = output<CierreCaso>();

    protected readonly limites = LIMITES_CONSOLIDACION;
    protected readonly formulario = inject(NonNullableFormBuilder).group({
        resultadoId: [null as number | null, Validators.required],
        observacion: ['', Validators.maxLength(LIMITES_CONSOLIDACION.observacionesMax)]
    });
    protected readonly abajo = computed(() => this.visible() && esPantallaPequena());
    protected readonly opciones = computed<readonly OpcionGrupo<number>[]>(() =>
        this.resultados()
            .filter(({ activo }) => activo)
            .map(({ id, nombre }) => ({ valor: id, etiqueta: nombre }))
    );
    private readonly resultadoId = toSignal(this.formulario.controls.resultadoId.valueChanges.pipe(startWith(null)), { initialValue: null });
    protected readonly largo = toSignal(this.formulario.controls.observacion.valueChanges.pipe(map((texto) => texto.length)), { initialValue: 0 });
    protected readonly resumen = computed(() => {
        const elegido = this.resultados().find(({ id }) => id === this.resultadoId());
        return elegido ? `Cerrarás el seguimiento de ${this.nombre().split(' ')[0]} con «${elegido.nombre}».` : null;
    });

    constructor() {
        effect(() => {
            if (this.visible()) {
                untracked(() => this.formulario.reset({ resultadoId: null, observacion: '' }));
            }
        });
    }

    protected enviar(): void {
        if (this.formulario.invalid) {
            this.formulario.markAllAsTouched();
            return;
        }
        const { resultadoId, observacion } = this.formulario.getRawValue();
        this.cerrar.emit({ resultadoId: resultadoId!, observacion: observacion.trim() || null });
    }

    protected cancelar(): void {
        this.visible.set(false);
    }
}
