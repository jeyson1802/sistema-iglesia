import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { PanelModule } from 'primeng/panel';
import { SkeletonModule } from 'primeng/skeleton';
import { MedioBoton } from '@/app/shared/components/botones-contacto/botones-contacto';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { CasoResumen } from '../../models/consolidacion';
import { ContactoEnCurso } from '../../services/contacto-en-curso';
import { TarjetaCasoComponent } from '../tarjeta-caso/tarjeta-caso';

/** Contacto pedido desde una tarjeta. */
export interface ContactoPedido {
    readonly caso: CasoResumen;
    readonly medio: MedioBoton;
}

const TARJETAS_CARGA = 2;

/**
 * Una sección de «Mis asignados» (presentacional): título con su cantidad, plegable, tarjetas de persona y su propio
 * paginador (paginado en el servidor). Tiene sus estados de carga, vacío y error.
 */
@Component({
    selector: 'app-seccion-casos',
    imports: [ButtonModule, PaginatorModule, PanelModule, SkeletonModule, TarjetaCasoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './seccion-casos.html'
})
export class SeccionCasosComponent {
    readonly titulo = input.required<string>();
    readonly icono = input.required<string>();
    readonly cantidad = input(0);
    readonly casos = input.required<readonly CasoResumen[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly mensajeVacio = input.required<string>();
    readonly sinRegistrar = input<ReadonlyMap<number, ContactoEnCurso>>(new Map());
    readonly plegada = input(false);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly contactar = output<ContactoPedido>();
    readonly registrar = output<CasoResumen>();
    readonly ver = output<CasoResumen>();
    readonly descartar = output<CasoResumen>();
    readonly reintentar = output<void>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly tarjetasCarga = Array.from({ length: TARJETAS_CARGA }, (_, indice) => indice);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
