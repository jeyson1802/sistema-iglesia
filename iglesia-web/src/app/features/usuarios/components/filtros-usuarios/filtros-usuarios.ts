import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { debounceTime, distinctUntilChanged, map, merge, skip, startWith } from 'rxjs';
import { FiltroUsuarios } from '../../models/usuario-admin';

/** Espera tras la última tecla antes de buscar por correo. */
export const ESPERA_BUSQUEDA_MS = 300;

/** Rol por el que se puede filtrar. */
export interface OpcionRolFiltro {
    readonly id: number;
    readonly nombre: string;
}

/** Opción de un selector; `null` = «Todos». */
interface Opcion<T> {
    readonly etiqueta: string;
    readonly valor: T | null;
}

const OPCIONES_ESTADO: readonly Opcion<boolean>[] = [
    { etiqueta: 'Todos', valor: null },
    { etiqueta: 'Activos', valor: true },
    { etiqueta: 'Inactivos', valor: false }
];

const mismoFiltro = (a: FiltroUsuarios, b: FiltroUsuarios): boolean => a.correo === b.correo && a.activo === b.activo && a.rolId === b.rolId;

/**
 * Filtros de la tabla de usuarios (presentacional). El correo se busca al dejar de escribir y los selectores al
 * cambiar; solo emite cuando el filtro resultante cambia.
 */
@Component({
    selector: 'app-filtros-usuarios',
    imports: [ReactiveFormsModule, IconFieldModule, InputIconModule, InputTextModule, SelectModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-usuarios.html'
})
export class FiltrosUsuariosComponent {
    readonly roles = input.required<readonly OpcionRolFiltro[]>();

    readonly filtrar = output<FiltroUsuarios>();

    private readonly fb = inject(NonNullableFormBuilder);

    protected readonly formulario = this.fb.group({
        correo: '',
        activo: this.fb.control<boolean | null>(null),
        rolId: this.fb.control<number | null>(null)
    });

    // `p-select` espera un arreglo mutable.
    protected readonly opcionesEstado = [...OPCIONES_ESTADO];
    protected readonly opcionesRol = computed<Opcion<number>[]>(() => [{ etiqueta: 'Todos', valor: null }, ...this.roles().map((rol) => ({ etiqueta: rol.nombre, valor: rol.id }))]);

    constructor() {
        const { correo, activo, rolId } = this.formulario.controls;
        merge(correo.valueChanges.pipe(debounceTime(ESPERA_BUSQUEDA_MS)), activo.valueChanges, rolId.valueChanges)
            .pipe(
                map(() => this.filtroActual()),
                startWith(this.filtroActual()),
                distinctUntilChanged(mismoFiltro),
                skip(1),
                takeUntilDestroyed()
            )
            .subscribe((filtro) => this.filtrar.emit(filtro));
    }

    private filtroActual(): FiltroUsuarios {
        const { correo, activo, rolId } = this.formulario.getRawValue();
        return { correo: correo.trim(), activo, rolId };
    }
}
