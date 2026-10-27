import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ETIQUETA_ROL } from '@/app/core/auth/models/rol';
import { RolAcceso } from '../../models/rol-acceso';

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
    readonly roles = input.required<RolAcceso[]>();
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);

    readonly ver = output<RolAcceso>();
    readonly editar = output<RolAcceso>();
    readonly cambiarEstado = output<RolAcceso>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);

    /** El cuerpo de `p-table` recibe la fila sin tipo: se tipa aquí. */
    protected etiquetaRolBase(rol: RolAcceso): string {
        return ETIQUETA_ROL[rol.rolBase];
    }
}
