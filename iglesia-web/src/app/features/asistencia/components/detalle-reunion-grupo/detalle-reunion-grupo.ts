import { ChangeDetectionStrategy, Component, computed, input, linkedSignal, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TableModule } from 'primeng/table';
import { hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { DetalleReunionGrupo, etiquetaLista, LISTAS_REUNION_GRUPO, ListaReunionGrupo, PersonaReunionGrupo } from '../../models/asistencia-grupo';

/** Mensaje cuando la lista elegida está vacía. */
const VACIO: Readonly<Record<ListaReunionGrupo, string>> = {
    asistieron: 'Ningún integrante asistió a esta reunión.',
    faltaron: 'Asistieron todos los integrantes.',
    visitantes: 'No hubo visitantes.'
};

/**
 * Quiénes asistieron, faltaron y vinieron de visita a una reunión de grupo (presentacional): un selector con el total
 * de cada lista y la lista elegida, paginada en el navegador. Si se pueden ver las personas, cada nombre lleva a su
 * asistencia.
 */
@Component({
    selector: 'app-detalle-reunion-grupo',
    imports: [FormsModule, SelectButtonModule, TableModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './detalle-reunion-grupo.html'
})
export class DetalleReunionGrupoComponent {
    readonly detalle = input.required<DetalleReunionGrupo>();
    readonly puedeVerPersonas = input(false);

    readonly verPersona = output<PersonaReunionGrupo>();

    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    protected readonly esHoy = computed(() => this.detalle().fecha === hoyIso());
    /** Al cambiar de reunión se vuelve a la primera lista. */
    protected readonly lista = linkedSignal<DetalleReunionGrupo, ListaReunionGrupo>({ source: this.detalle, computation: () => 'asistieron' });
    protected readonly opciones = computed(() => LISTAS_REUNION_GRUPO.map((valor) => ({ valor, etiqueta: `${etiquetaLista(valor, this.esHoy())} (${this.detalle()[valor].length})` })));
    protected readonly personas = computed(() => [...this.detalle()[this.lista()]]);
    protected readonly mensajeVacio = computed(() => (this.lista() === 'faltaron' && this.esHoy() ? 'No queda nadie sin marcar.' : VACIO[this.lista()]));
}
