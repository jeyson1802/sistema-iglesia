import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { describirUbicacion } from '@/app/core/models/ubicacion';
import { ETIQUETA_CONDICION, MiembroResumen, PAGINA_MIEMBROS } from '../../models/miembro';

/** Fila de relleno que se pinta como skeleton mientras llega la página. */
interface FilaCarga {
    readonly id: number;
}

const FILAS_CARGA: readonly FilaCarga[] = Array.from({ length: PAGINA_MIEMBROS }, (_, indice) => ({ id: -indice - 1 }));

/**
 * Tabla de miembros paginada en el servidor (presentacional). El nombre lleva a la ficha; pide cada página con
 * `cambiarPagina` (índice desde 0). Las referencias inactivas (nivel o líder) se muestran atenuadas.
 */
@Component({
    selector: 'app-tabla-miembros',
    imports: [RouterLink, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-miembros.html'
})
export class TablaMiembrosComponent {
    readonly miembros = input.required<readonly MiembroResumen[]>();
    readonly totalRegistros = input.required<number>();
    /** Página mostrada, desde 0. */
    readonly pagina = input(0);
    readonly cargando = input(false);

    readonly cambiarPagina = output<number>();

    protected readonly filasPorPagina = PAGINA_MIEMBROS;
    protected readonly primerRegistro = computed(() => this.pagina() * PAGINA_MIEMBROS);
    /** Mientras carga se conservan cabecera y paginador, y el cuerpo se rellena con skeletons. */
    protected readonly filas = computed<(MiembroResumen | FilaCarga)[]>(() => [...(this.cargando() ? FILAS_CARGA : this.miembros())]);

    /** El cuerpo de `p-table` recibe la fila sin tipo: se tipa aquí. */
    protected condicion({ condicion }: MiembroResumen): string {
        return condicion ? ETIQUETA_CONDICION[condicion] : '—';
    }

    /** Distrito, provincia y departamento en una sola celda. */
    protected region({ ubicacion }: MiembroResumen): string {
        return describirUbicacion(ubicacion) || '—';
    }

    protected alCargar(evento: TableLazyLoadEvent): void {
        this.cambiarPagina.emit(Math.floor((evento.first ?? 0) / PAGINA_MIEMBROS));
    }
}
