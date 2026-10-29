import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { PAGINA_USUARIOS, UsuarioAdmin } from '../../models/usuario-admin';

/** Fila de relleno que se pinta como skeleton mientras llega la página. */
interface FilaCarga {
    readonly id: number;
}

const FILAS_CARGA: readonly FilaCarga[] = Array.from({ length: PAGINA_USUARIOS }, (_, indice) => ({ id: -indice - 1 }));

/**
 * Tabla de usuarios paginada en el servidor (presentacional). Pide cada página con `cambiarPagina` (índice desde 0) y
 * solo ofrece editar y restablecer contraseña a quien puede administrar usuarios, nunca sobre su propia fila.
 */
@Component({
    selector: 'app-tabla-usuarios',
    imports: [DatePipe, ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-usuarios.html'
})
export class TablaUsuariosComponent {
    readonly usuarios = input.required<readonly UsuarioAdmin[]>();
    readonly totalRegistros = input.required<number>();
    /** Página mostrada, desde 0. */
    readonly pagina = input(0);
    readonly tamanoPagina = input(PAGINA_USUARIOS);
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);
    readonly usuarioActualId = input<number | null>(null);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly editar = output<UsuarioAdmin>();
    readonly restablecer = output<UsuarioAdmin>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    /** Mientras carga se conservan cabecera y paginador, y el cuerpo se rellena con skeletons. */
    protected readonly filas = computed<(UsuarioAdmin | FilaCarga)[]>(() => [...(this.cargando() ? FILAS_CARGA : this.usuarios())]);

    protected alCargar(evento: TableLazyLoadEvent): void {
        this.cambiarPagina.emit(paginaPedida(evento, this.tamanoPagina()));
    }

    protected esPropio(usuario: UsuarioAdmin): boolean {
        return usuario.id === this.usuarioActualId();
    }
}
