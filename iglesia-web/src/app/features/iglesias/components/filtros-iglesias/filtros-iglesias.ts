import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { debounceTime, map, merge } from 'rxjs';
import { FILTRO_IGLESIAS_VACIO, FiltroIglesias } from '@/app/core/iglesias/models/iglesia';
import { SelectorUbigeoComponent } from '@/app/shared/components/selector-ubigeo/selector-ubigeo';
import { mismoFiltroIglesias } from '../../models/filtro-url';

/** Espera tras la última tecla antes de buscar por nombre. */
export const ESPERA_BUSQUEDA_MS = 300;

type ClaveFiltro = keyof FiltroIglesias;

interface ChipFiltro {
    readonly clave: ClaveFiltro;
    readonly etiqueta: string;
}

const OPCIONES_ESTADO = [
    { etiqueta: 'Todas', valor: null },
    { etiqueta: 'Activas', valor: true },
    { etiqueta: 'Inactivas', valor: false }
];

/**
 * Búsqueda y filtros del catálogo de iglesias (presentacional). Refleja el filtro de la URL, busca el texto al dejar
 * de escribir y los selectores al cambiar, y muestra los filtros activos como chips que se pueden quitar.
 */
@Component({
    selector: 'app-filtros-iglesias',
    imports: [ReactiveFormsModule, ButtonModule, ChipModule, IconFieldModule, InputIconModule, InputTextModule, SelectModule, SelectorUbigeoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-iglesias.html',
    host: { class: 'flex flex-col gap-4' }
})
export class FiltrosIglesiasComponent {
    private readonly fb = inject(NonNullableFormBuilder);

    readonly filtro = input.required<FiltroIglesias>();
    readonly filtrar = output<FiltroIglesias>();

    protected readonly formulario = this.fb.group({
        q: '',
        ubigeo: this.fb.control<string | null>(null),
        activa: this.fb.control<boolean | null>(null)
    });
    protected readonly opcionesEstado = OPCIONES_ESTADO;

    protected readonly chips = computed<ChipFiltro[]>(() => {
        const { q, ubigeo, activa } = this.filtro();
        return [
            ...(q ? [{ clave: 'q' as const, etiqueta: `«${q}»` }] : []),
            ...(ubigeo ? [{ clave: 'ubigeo' as const, etiqueta: `Ubicación ${ubigeo}` }] : []),
            ...(activa !== null ? [{ clave: 'activa' as const, etiqueta: activa ? 'Activas' : 'Inactivas' }] : [])
        ];
    });

    constructor() {
        // El formulario sigue a la URL (enlaces, atrás y adelante) sin volver a emitir.
        effect(() => {
            const { q, ubigeo, activa } = this.filtro();
            untracked(() => this.formulario.setValue({ q: q ?? '', ubigeo, activa }, { emitEvent: false }));
        });
        const { q, ubigeo, activa } = this.formulario.controls;
        merge(q.valueChanges.pipe(debounceTime(ESPERA_BUSQUEDA_MS)), ubigeo.valueChanges, activa.valueChanges)
            .pipe(
                map(() => this.filtroActual()),
                takeUntilDestroyed()
            )
            .subscribe((filtro) => {
                if (!mismoFiltroIglesias(filtro, this.filtro())) {
                    this.filtrar.emit(filtro);
                }
            });
    }

    protected quitar(chip: ChipFiltro): void {
        this.filtrar.emit({ ...this.filtro(), [chip.clave]: null });
    }

    protected limpiar(): void {
        this.filtrar.emit(FILTRO_IGLESIAS_VACIO);
    }

    private filtroActual(): FiltroIglesias {
        const { q, ubigeo, activa } = this.formulario.getRawValue();
        return { q: q.trim() || null, ubigeo, activa };
    }
}
