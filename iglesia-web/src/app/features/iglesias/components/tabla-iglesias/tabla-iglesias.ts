import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { Iglesia } from '@/app/core/iglesias/models/iglesia';

/** Fila de relleno que se pinta como skeleton mientras llega el catálogo. */
interface FilaCarga {
    readonly id: number;
}

const FILAS_CARGA: readonly FilaCarga[] = Array.from({ length: 5 }, (_, indice) => ({ id: -indice - 1 }));

/**
 * Tabla del catálogo de iglesias (presentacional). Marca con «Actual» la iglesia en la que se opera y solo ofrece
 * entrar, editar el nombre y crear un administrador a quien puede administrar iglesias.
 */
@Component({
    selector: 'app-tabla-iglesias',
    imports: [DatePipe, ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-iglesias.html'
})
export class TablaIglesiasComponent {
    readonly iglesias = input.required<readonly Iglesia[]>();
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);
    /** Iglesia en la que opera el administrador del sistema; `null` en su vista global. */
    readonly iglesiaActualId = input<number | null>(null);
    /** Mientras se entra a una iglesia se deshabilita «Entrar» en todas las filas. */
    readonly entrando = input(false);

    readonly entrar = output<Iglesia>();
    readonly editar = output<Iglesia>();
    readonly crearAdministrador = output<Iglesia>();

    /** Mientras carga se conserva la cabecera y el cuerpo se rellena con skeletons. */
    protected readonly filas = computed<(Iglesia | FilaCarga)[]>(() => [...(this.cargando() ? FILAS_CARGA : this.iglesias())]);

    protected esActual(iglesia: Iglesia): boolean {
        return iglesia.id === this.iglesiaActualId();
    }
}
