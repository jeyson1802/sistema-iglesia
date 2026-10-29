import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged, map, merge, skip, startWith } from 'rxjs';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { CONDICIONES, Condicion, ETIQUETA_CONDICION, FILTRO_MIEMBROS_INICIAL, FiltroMiembros } from '../../models/miembro';

/** Espera tras la última tecla antes de buscar por nombre. */
export const ESPERA_BUSQUEDA_MS = 300;

/** Nivel o grupo por el que se puede filtrar. */
export interface OpcionFiltro {
    readonly id: number;
    readonly nombre: string;
}

/** Opción de un selector; `null` = «Todos». */
interface Opcion<T> {
    readonly etiqueta: string;
    readonly valor: T | null;
}

const TODOS = 'Todos';
const OPCIONES_ESTADO: readonly Opcion<boolean>[] = [
    { etiqueta: TODOS, valor: null },
    { etiqueta: 'Activos', valor: true },
    { etiqueta: 'Inactivos', valor: false }
];

const conTodos = <T>(opciones: Opcion<T>[]): Opcion<T>[] => [{ etiqueta: TODOS, valor: null }, ...opciones];

const mismoFiltro = (a: FiltroMiembros, b: FiltroMiembros): boolean => a.q === b.q && a.condicion === b.condicion && a.nivelId === b.nivelId && a.grupoId === b.grupoId && a.liderId === b.liderId && a.activo === b.activo;

/**
 * Búsqueda y filtros del listado de miembros (presentacional). El nombre se busca al dejar de escribir y los
 * selectores al cambiar; solo emite cuando el filtro resultante cambia. Empieza mostrando solo los activos.
 */
@Component({
    selector: 'app-filtros-miembros',
    imports: [ReactiveFormsModule, IconFieldModule, InputIconModule, InputTextModule, SelectModule, SelectorMiembroComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-miembros.html'
})
export class FiltrosMiembrosComponent {
    readonly niveles = input.required<readonly OpcionFiltro[]>();
    readonly grupos = input.required<readonly OpcionFiltro[]>();
    /** Nombre singular de los grupos en la iglesia, para la etiqueta del filtro. */
    readonly nombreGrupo = input('Grupo');

    readonly filtrar = output<FiltroMiembros>();

    private readonly fb = inject(NonNullableFormBuilder);
    protected readonly formulario = this.fb.group({
        q: FILTRO_MIEMBROS_INICIAL.q,
        condicion: this.fb.control<Condicion | null>(FILTRO_MIEMBROS_INICIAL.condicion),
        nivelId: this.fb.control<number | null>(FILTRO_MIEMBROS_INICIAL.nivelId),
        grupoId: this.fb.control<number | null>(FILTRO_MIEMBROS_INICIAL.grupoId),
        lider: this.fb.control<OpcionMiembro | null>(null),
        activo: this.fb.control<boolean | null>(FILTRO_MIEMBROS_INICIAL.activo)
    });

    // `p-select` espera arreglos mutables.
    protected readonly opcionesEstado = [...OPCIONES_ESTADO];
    protected readonly opcionesCondicion = conTodos(CONDICIONES.map((condicion) => ({ etiqueta: ETIQUETA_CONDICION[condicion], valor: condicion })));
    protected readonly opcionesNivel = computed(() => conTodos(this.niveles().map(({ id, nombre }) => ({ etiqueta: nombre, valor: id }))));
    protected readonly opcionesGrupo = computed(() => conTodos(this.grupos().map(({ id, nombre }) => ({ etiqueta: nombre, valor: id }))));

    constructor() {
        const { q, condicion, nivelId, grupoId, lider, activo } = this.formulario.controls;
        merge(q.valueChanges.pipe(debounceTime(ESPERA_BUSQUEDA_MS)), condicion.valueChanges, nivelId.valueChanges, grupoId.valueChanges, lider.valueChanges, activo.valueChanges)
            .pipe(
                map(() => this.filtroActual()),
                startWith(this.filtroActual()),
                distinctUntilChanged(mismoFiltro),
                skip(1),
                takeUntilDestroyed()
            )
            .subscribe((filtro) => this.filtrar.emit(filtro));
    }

    private filtroActual(): FiltroMiembros {
        const { q, condicion, nivelId, grupoId, lider, activo } = this.formulario.getRawValue();
        return { q: q.trim(), condicion, nivelId, grupoId, liderId: lider?.id ?? null, activo };
    }
}
