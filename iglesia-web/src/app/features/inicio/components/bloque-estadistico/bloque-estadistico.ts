import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';

/**
 * Marco de un bloque del inicio (presentacional): título, «Ver más» hacia su módulo y los estados de carga
 * (esqueleto con la forma del bloque) y de error (con «Reintentar»). El contenido se proyecta cuando hay datos, así
 * cada bloque carga, falla y se reintenta solo.
 */
@Component({
    selector: 'app-bloque-estadistico',
    imports: [RouterLink, ButtonModule, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-estadistico.html',
    host: { class: 'card block mb-0!' }
})
export class BloqueEstadisticoComponent {
    readonly titulo = input.required<string>();
    readonly idTitulo = input.required<string>();
    readonly descripcion = input<string | null>(null);
    readonly ruta = input<string | null>(null);
    readonly etiquetaRuta = input('Ver más');
    readonly cargando = input(false);
    readonly error = input(false);
    readonly reintentar = output<void>();
}
