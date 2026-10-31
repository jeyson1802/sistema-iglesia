import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { debounceTime, map, merge } from 'rxjs';
import { CAMPO_CONTEO, ConteoAlertas, DESCRIPCION_ALERTA, FiltroSeguimiento, TipoAlerta, TIPOS_ALERTA } from '../../models/seguimiento';

/** Espera tras la última tecla antes de buscar por nombre. */
export const ESPERA_BUSQUEDA_MS = 300;

/** Valor de «Todos» en el formulario (el selector no marca como elegida una opción con valor nulo). */
const TODOS = 'TODOS';
type OpcionValorAlerta = TipoAlerta | typeof TODOS;

/** Grupo que se puede elegir en el filtro. */
export interface OpcionGrupo {
    readonly id: number;
    readonly nombre: string;
}

interface OpcionAlerta {
    readonly valor: OpcionValorAlerta;
    readonly etiqueta: string;
    readonly cantidad: number | null;
}

/**
 * Filtros de la lista de seguimiento (presentacional): «Todos» (todas las personas, con o sin alertas) o una alerta, con
 * la cantidad de cada opción; grupo (si se pueden ver los grupos) y nombre. El nombre se busca al dejar de escribir.
 */
@Component({
    selector: 'app-filtros-seguimiento',
    imports: [ReactiveFormsModule, IconFieldModule, InputIconModule, InputTextModule, SelectModule, SelectButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-seguimiento.html',
    host: { class: 'flex flex-col gap-4' }
})
export class FiltrosSeguimientoComponent {
    private readonly fb = inject(NonNullableFormBuilder);

    readonly filtro = input.required<FiltroSeguimiento>();
    readonly conteos = input<ConteoAlertas | null>(null);
    /** Vacío: no se muestra el filtro por grupo (el usuario no puede ver los grupos). */
    readonly grupos = input<readonly OpcionGrupo[]>([]);
    readonly etiquetaGrupo = input('Grupo');

    readonly filtrar = output<FiltroSeguimiento>();

    protected readonly formulario = this.fb.group({
        alerta: this.fb.control<OpcionValorAlerta>(TODOS),
        q: '',
        grupoId: this.fb.control<number | null>(null)
    });

    protected readonly opcionesGrupo = computed(() => [...this.grupos()]);
    protected readonly opcionesAlerta = computed<OpcionAlerta[]>(() => {
        const conteos = this.conteos();
        return [
            { valor: TODOS, etiqueta: 'Todos', cantidad: conteos?.personas ?? null },
            ...TIPOS_ALERTA.map((alerta) => ({ valor: alerta, etiqueta: DESCRIPCION_ALERTA[alerta].etiqueta, cantidad: conteos ? conteos[CAMPO_CONTEO[alerta]] : null }))
        ];
    });

    constructor() {
        effect(() => {
            const { alerta, q, grupoId } = this.filtro();
            untracked(() => this.formulario.setValue({ alerta: alerta ?? TODOS, q, grupoId }, { emitEvent: false }));
        });
        const { alerta, q, grupoId } = this.formulario.controls;
        merge(q.valueChanges.pipe(debounceTime(ESPERA_BUSQUEDA_MS)), alerta.valueChanges, grupoId.valueChanges)
            .pipe(
                map(() => this.formulario.getRawValue()),
                takeUntilDestroyed()
            )
            .subscribe(({ alerta, q, grupoId }) => {
                const nuevo: FiltroSeguimiento = { alerta: alerta === TODOS ? null : alerta, q: q.trim(), grupoId, todos: alerta === TODOS };
                const actual = this.filtro();
                if (nuevo.alerta !== actual.alerta || nuevo.q !== actual.q || nuevo.grupoId !== actual.grupoId || nuevo.todos !== actual.todos) {
                    this.filtrar.emit(nuevo);
                }
            });
    }
}
