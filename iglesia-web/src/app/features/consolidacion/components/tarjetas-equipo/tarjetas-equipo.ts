import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, paginaACargar, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { Consolidador, iniciales } from '../../models/consolidacion';

const TARJETAS_CARGA = 3;

/**
 * Equipo de consolidación en tarjetas (presentacional), paginado en el navegador: iniciales, nombre, casos abiertos y
 * contactos de los últimos 30 días. Activa o desactiva a cada integrante.
 */
@Component({
    selector: 'app-tarjetas-equipo',
    imports: [ButtonModule, PaginatorModule, SkeletonModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjetas-equipo.html'
})
export class TarjetasEquipoComponent {
    readonly equipo = input.required<readonly Consolidador[]>();
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);
    /** Integrante cuyo estado se está cambiando. */
    readonly procesando = input<number | null>(null);

    readonly cambiarEstado = output<Consolidador>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly tarjetasCarga = Array.from({ length: TARJETAS_CARGA }, (_, indice) => indice);
    protected readonly pagina = signal(0);
    protected readonly tamano = signal(FILAS_POR_PAGINA);
    protected readonly visibles = computed(() => this.equipo().slice(this.pagina() * this.tamano(), (this.pagina() + 1) * this.tamano()));
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamano());
    protected readonly iniciales = iniciales;

    protected alPaginar(estado: PaginatorState): void {
        const pedida = paginaPedida(estado, this.tamano());
        this.pagina.set(paginaACargar(pedida, this.tamano()));
        this.tamano.set(pedida.tamano);
    }
}
