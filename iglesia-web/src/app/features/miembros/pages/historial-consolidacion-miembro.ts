import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { SemanasAsistenciaComponent } from '@/app/features/asistencia/components/semanas-asistencia/semanas-asistencia';
import { SemanaAsistencia } from '@/app/features/asistencia/models/seguimiento';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { IteracionesAnterioresComponent } from '@/app/features/consolidacion/components/iteraciones-anteriores/iteraciones-anteriores';
import { ConsolidacionMiembro, Iteracion, resumenContactos } from '@/app/features/consolidacion/models/consolidacion';
import { RUTA_CONSOLIDACION } from '@/app/features/consolidacion/models/rutas-consolidacion';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaACargar, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { MiembrosService } from '../services/miembros.service';

const MENSAJE_NO_ENCONTRADO = 'No encontramos a esta persona o no está en tu ámbito de consolidación.';
const MENSAJE_ERROR = 'No se pudo cargar el historial. Intenta de nuevo.';

/**
 * Historial de consolidación de una persona (contenedor): resumen, asistencia de las últimas semanas con las semanas en
 * que hubo contacto marcadas, y todas sus iteraciones (de la más reciente a la más antigua) paginadas en el servidor.
 */
@Component({
    selector: 'app-historial-consolidacion-miembro',
    imports: [RouterLink, ButtonModule, PaginatorModule, SkeletonModule, SemanasAsistenciaComponent, IteracionesAnterioresComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './historial-consolidacion-miembro.html'
})
export class HistorialConsolidacionMiembroComponent implements OnInit {
    private readonly casos = inject(CasosService);
    private readonly miembros = inject(MiembrosService);
    private readonly asistencias = inject(AsistenciasService);
    private readonly sesion = inject(SesionService);

    protected readonly miembroId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
    protected readonly puedeVerFicha = computed(() => this.sesion.tienePermiso('miembros.ver'));
    protected readonly volver = computed(() => (this.puedeVerFicha() ? { ruta: ['/miembros', this.miembroId], etiqueta: 'Ficha del miembro' } : { ruta: [RUTA_CONSOLIDACION], etiqueta: 'Consolidación' }));
    protected readonly nombre = signal<string | null>(null);
    protected readonly resumen = signal<ConsolidacionMiembro | null>(null);
    protected readonly semanas = signal<readonly SemanaAsistencia[]>([]);
    protected readonly error = signal<string | null>(null);

    protected readonly iteraciones = signal<readonly Iteracion[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly lineaResumen = computed(() => {
        const resumen = this.resumen();
        if (!resumen) {
            return null;
        }
        const iteraciones = resumen.iteraciones === 1 ? '1 iteración' : `${resumen.iteraciones} iteraciones`;
        return `${iteraciones} · ${resumenContactos(resumen.contactos).toLowerCase()}`;
    });

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
    }

    ngOnInit(): void {
        this.cargar();
        this.casos.resumenDeMiembro(this.miembroId).subscribe({ next: (resumen) => this.resumen.set(resumen), error: () => this.resumen.set(null) });
        // La asistencia es contexto: si no tiene permiso para verla, la sección simplemente no aparece.
        this.asistencias.resumenMiembro(this.miembroId).subscribe({ next: ({ semanas }) => this.semanas.set(semanas), error: () => this.semanas.set([]) });
        if (this.puedeVerFicha()) {
            this.miembros.obtener(this.miembroId).subscribe({ next: ({ nombres, apellidos }) => this.nombre.set(`${nombres} ${apellidos}`), error: () => this.nombre.set(null) });
        }
    }

    protected cargar(): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(null);
        this.carga = this.casos
            .historial(this.miembroId, this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.iteraciones.set(content);
                    this.total.set(page.totalElements);
                },
                error: (error: unknown) => this.error.set(error instanceof HttpErrorResponse && [HttpStatusCode.NotFound, HttpStatusCode.Forbidden].includes(error.status) ? MENSAJE_NO_ENCONTRADO : MENSAJE_ERROR)
            });
    }

    protected alPaginar(estado: PaginatorState): void {
        const pedida: PaginaPedida = paginaPedida(estado, this.tamanoPagina());
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargar();
    }
}
