import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { ETIQUETA_DIA, Grupo } from '../../models/grupo';

/** Filas de relleno que se muestran como skeleton mientras se cargan los grupos. */
const FILAS_CARGA = 4;

/**
 * Tabla de grupos (presentacional). Muestra el nombre configurado por la iglesia en los textos y solo ofrece editar y
 * desactivar o reactivar a quien administra grupos.
 */
@Component({
    selector: 'app-tabla-grupos',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-grupos.html'
})
export class TablaGruposComponent {
    readonly grupos = input.required<readonly Grupo[]>();
    readonly cargando = input(false);
    readonly puedeAdministrar = input(false);
    /** Nombre plural de los grupos en la iglesia (p. ej. «Células»). */
    readonly nombrePlural = input('Grupos');

    readonly editar = output<Grupo>();
    readonly cambiarEstado = output<Grupo>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    /** `p-table` espera un arreglo mutable. */
    protected readonly filas = computed(() => [...this.grupos()]);

    /** «Jueves 19:30», solo el día, solo la hora o vacío. El cuerpo de `p-table` recibe la fila sin tipo: se tipa aquí. */
    protected reunion(grupo: Grupo): string {
        return [grupo.diaReunion ? ETIQUETA_DIA[grupo.diaReunion] : null, grupo.horaReunion].filter(Boolean).join(' ');
    }
}
