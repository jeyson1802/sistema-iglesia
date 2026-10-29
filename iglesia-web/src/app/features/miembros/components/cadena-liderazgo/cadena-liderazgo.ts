import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { EslabonCadena } from '../../models/miembro';

/** Filas de relleno mientras se carga la cadena. */
const FILAS_CARGA = [0, 1, 2];

/** Cadena de liderazgo del miembro hasta el pastor principal y a cuántos lidera directamente (presentacional). */
@Component({
    selector: 'app-cadena-liderazgo',
    imports: [RouterLink, SkeletonModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './cadena-liderazgo.html'
})
export class CadenaLiderazgoComponent {
    /** Líderes del directo al pastor principal. */
    readonly cadena = input.required<readonly EslabonCadena[]>();
    readonly cantidadLiderados = input(0);
    readonly esPastorPrincipal = input(false);
    readonly cargando = input(false);

    protected readonly filasCarga = FILAS_CARGA;
    protected readonly textoLiderados = computed(() => {
        const cantidad = this.cantidadLiderados();
        if (cantidad === 0) {
            return 'No lidera a nadie directamente.';
        }
        return cantidad === 1 ? 'Lidera a 1 persona directamente.' : `Lidera a ${cantidad} personas directamente.`;
    });
}
