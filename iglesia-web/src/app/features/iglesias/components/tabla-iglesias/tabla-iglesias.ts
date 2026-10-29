import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { faltaDireccion, Iglesia } from '@/app/core/iglesias/models/iglesia';
import { RUTA_IGLESIAS } from '@/app/core/iglesias/models/rutas-iglesias';
import { describirUbicacion } from '@/app/core/models/ubicacion';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { MiniaturaIglesiaComponent } from '../miniatura-iglesia/miniatura-iglesia';

/** Fila de relleno que se pinta como skeleton mientras llega la página. */
interface FilaCarga {
    readonly id: number;
}

const FILAS_CARGA: readonly FilaCarga[] = Array.from({ length: 5 }, (_, indice) => ({ id: -indice - 1 }));

/**
 * Tabla del catálogo de iglesias, paginada en el servidor (presentacional): logo, nombres, ubicación y estado.
 * Marca con «Actual» la iglesia en la que se opera y avisa las que no tienen dirección. Solo ofrece entrar, editar y
 * crear un administrador a quien puede administrar iglesias.
 */
@Component({
    selector: 'app-tabla-iglesias',
    imports: [RouterLink, ButtonModule, PaginatorModule, SkeletonModule, TableModule, TagModule, TooltipModule, MiniaturaIglesiaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-iglesias.html'
})
export class TablaIglesiasComponent {
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly rutaIglesias = RUTA_IGLESIAS;

    readonly iglesias = input.required<readonly Iglesia[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);
    /** Iglesia en la que opera el administrador del sistema; `null` en su vista global. */
    readonly iglesiaActualId = input<number | null>(null);
    /** Mientras se entra a una iglesia se deshabilita «Entrar» en todas las filas. */
    readonly entrando = input(false);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly entrar = output<Iglesia>();
    readonly crearAdministrador = output<Iglesia>();

    /** Mientras carga se conserva la cabecera y el cuerpo se rellena con skeletons. */
    protected readonly filas = computed<(Iglesia | FilaCarga)[]>(() => [...(this.cargando() ? FILAS_CARGA : this.iglesias())]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());

    protected esActual(iglesia: Iglesia): boolean {
        return iglesia.id === this.iglesiaActualId();
    }

    protected sinDireccion(iglesia: Iglesia): boolean {
        return faltaDireccion(iglesia);
    }

    protected ubicacion(iglesia: Iglesia): string {
        return describirUbicacion(iglesia.ubicacion);
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
