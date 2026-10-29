import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output, signal, viewChild } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { MonedaPipe } from '@/app/shared/pipes/moneda.pipe';
import { OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { Aporte, APORTES_POR_PAGINA, DESCRIPCION_MEDIO, MedioAporte } from '../../models/aporte';

const FILAS_CARGA = 5;

/**
 * Aportes paginados en el servidor (presentacional). En escritorio, una tabla; en el móvil, tarjetas apiladas con la
 * misma información. Un clic (o Enter) en un aporte abre su detalle; editar y anular van en un menú por fila.
 */
@Component({
    selector: 'app-tabla-aportes',
    imports: [DatePipe, ButtonModule, MenuModule, PaginatorModule, SkeletonModule, TableModule, TagModule, TooltipModule, MonedaPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-aportes.html'
})
export class TablaAportesComponent {
    readonly aportes = input.required<readonly Aporte[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(APORTES_POR_PAGINA);
    readonly cargando = input(false);
    readonly moneda = input.required<string>();
    readonly puedeEditar = input(false);
    readonly puedeAnular = input(false);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<Aporte>();
    readonly editar = output<Aporte>();
    readonly anular = output<Aporte>();

    private readonly menu = viewChild<Menu>('menuFila');

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.aportes()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly hayAcciones = computed(() => this.puedeEditar() || this.puedeAnular());
    protected readonly accionesFila = signal<MenuItem[]>([]);

    protected medio(medio: MedioAporte) {
        return DESCRIPCION_MEDIO[medio];
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }

    /** El menú se arma por fila: un aporte anulado ya no se edita ni se anula. */
    protected abrirAcciones(evento: Event, aporte: Aporte): void {
        evento.stopPropagation();
        this.accionesFila.set([
            { label: 'Ver detalle', icon: 'pi pi-eye', command: () => this.ver.emit(aporte) },
            ...(this.puedeEditar() ? [{ label: 'Editar', icon: 'pi pi-pencil', disabled: aporte.anulado, command: () => this.editar.emit(aporte) }] : []),
            ...(this.puedeAnular() ? [{ label: 'Anular', icon: 'pi pi-ban', disabled: aporte.anulado, command: () => this.anular.emit(aporte) }] : [])
        ]);
        this.menu()?.toggle(evento);
    }
}
