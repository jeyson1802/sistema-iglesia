import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { ETIQUETA_ALCANCE, RolAcceso } from '../../models/rol-acceso';

/** Filas de relleno que se muestran como skeleton mientras se cargan los roles. */
const FILAS_CARGA = 4;

/**
 * Tabla de roles (presentacional). Los roles base solo ofrecen «Ver permisos»; los personalizados, editar y
 * desactivar o reactivar cuando quien mira puede administrar roles.
 */
@Component({
    selector: 'app-tabla-roles',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-roles.html'
})
export class TablaRolesComponent {
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    readonly roles = input.required<RolAcceso[]>();
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);

    readonly ver = output<RolAcceso>();
    readonly editar = output<RolAcceso>();
    readonly cambiarEstado = output<RolAcceso>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);

    /** El cuerpo de `p-table` recibe la fila sin tipo: se tipa aquí. */
    protected etiquetaAlcance(rol: RolAcceso): string {
        return ETIQUETA_ALCANCE[rol.rolBase];
    }
}
