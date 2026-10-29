import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SkeletonModule } from 'primeng/skeleton';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { EtiquetasAlertaComponent } from '@/app/features/asistencia/components/etiquetas-alerta/etiquetas-alerta';
import { IndicadoresAsistenciaComponent } from '@/app/features/asistencia/components/indicadores-asistencia/indicadores-asistencia';
import { TablaOcasionesComponent } from '@/app/features/asistencia/components/tabla-ocasiones/tabla-ocasiones';
import { AsistenciaMiembro } from '@/app/features/asistencia/models/asistencia';
import { RUTA_SEGUIMIENTO } from '@/app/features/asistencia/models/rutas-asistencia';
import { EstadoOcasion, Ocasion, semanaCorta } from '@/app/features/asistencia/models/seguimiento';
import { AsistenciasService } from '@/app/features/asistencia/services/asistencias.service';
import { SeguimientoService } from '@/app/features/asistencia/services/seguimiento.service';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { MiembrosService } from '../services/miembros.service';

const MENSAJE_NO_ENCONTRADO = 'No encontramos a este miembro o no forma parte de tu red.';
const MENSAJE_ERROR = 'No se pudo cargar el seguimiento. Intenta de nuevo.';

/** Filtro del historial: `null` muestra todas las ocasiones. */
interface OpcionEstado {
    readonly etiqueta: string;
    readonly valor: EstadoOcasion | null;
}

const OPCIONES_ESTADO: readonly OpcionEstado[] = [
    { etiqueta: 'Todas', valor: null },
    { etiqueta: 'Asistió', valor: 'ASISTIO' },
    { etiqueta: 'Faltó', valor: 'FALTO' },
    { etiqueta: 'Eventos', valor: 'EXTRA' }
];

/**
 * Seguimiento de asistencia de un miembro (contenedor): indicadores, alertas, evolución semanal (asistió / faltó) y el
 * historial de ocasiones paginado en el servidor. Con `asistencia.ver`, cualquier miembro; con `miembros.ver`, los de
 * su red (el backend responde 404 con los demás).
 */
@Component({
    selector: 'app-seguimiento-asistencia-miembro',
    imports: [ReactiveFormsModule, RouterLink, ButtonModule, SelectButtonModule, SkeletonModule, EtiquetasAlertaComponent, GraficoEvolucionComponent, IndicadoresAsistenciaComponent, TablaOcasionesComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './seguimiento-asistencia-miembro.html'
})
export class SeguimientoAsistenciaMiembroComponent implements OnInit {
    private readonly asistencias = inject(AsistenciasService);
    private readonly seguimiento = inject(SeguimientoService);
    private readonly miembros = inject(MiembrosService);
    private readonly sesion = inject(SesionService);

    protected readonly miembroId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
    protected readonly puedeVerFicha = computed(() => this.sesion.tienePermiso('miembros.ver'));
    protected readonly volver = computed(() => (this.puedeVerFicha() ? { ruta: ['/miembros', this.miembroId], etiqueta: 'Ficha del miembro' } : { ruta: [RUTA_SEGUIMIENTO], etiqueta: 'Seguimiento' }));
    protected readonly nombre = signal<string | null>(null);
    protected readonly resumen = signal<AsistenciaMiembro | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal<string | null>(null);

    protected readonly opcionesEstado = [...OPCIONES_ESTADO];
    protected readonly estado = new FormControl<EstadoOcasion | null>(null);
    protected readonly ocasiones = signal<readonly Ocasion[]>([]);
    protected readonly totalOcasiones = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargandoOcasiones = signal(true);
    protected readonly errorOcasiones = signal(false);

    protected readonly etiquetasSemanas = computed(() => (this.resumen()?.semanas ?? []).map(({ inicio }) => semanaCorta(inicio)));
    protected readonly seriesSemanas = computed<readonly SerieGrafico[]>(() => {
        const semanas = this.resumen()?.semanas ?? [];
        return [
            { etiqueta: 'Asistió', valores: semanas.map(({ asistidas }) => asistidas), tono: 'primario' },
            { etiqueta: 'Faltó', valores: semanas.map(({ esperadas, asistidas }) => esperadas - asistidas), tono: 'suave' }
        ];
    });

    private cargaOcasiones?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cargaOcasiones?.unsubscribe());
        this.estado.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
            this.pagina.set(0);
            this.cargarOcasiones();
        });
    }

    ngOnInit(): void {
        this.cargar();
        this.cargarOcasiones();
        if (this.puedeVerFicha()) {
            this.miembros.obtener(this.miembroId).subscribe({ next: ({ nombres, apellidos }) => this.nombre.set(`${nombres} ${apellidos}`), error: () => this.nombre.set(null) });
        }
    }

    protected cargar(): void {
        this.cargando.set(true);
        this.error.set(null);
        this.asistencias
            .resumenMiembro(this.miembroId)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (resumen) => this.resumen.set(resumen),
                error: (error: unknown) => this.error.set(error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound ? MENSAJE_NO_ENCONTRADO : MENSAJE_ERROR)
            });
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargarOcasiones();
    }

    protected cargarOcasiones(): void {
        this.cargaOcasiones?.unsubscribe();
        this.cargandoOcasiones.set(true);
        this.errorOcasiones.set(false);
        this.cargaOcasiones = this.seguimiento
            .ocasiones(this.miembroId, { estado: this.estado.value }, this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargandoOcasiones.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.ocasiones.set(content);
                    this.totalOcasiones.set(page.totalElements);
                },
                error: () => this.errorOcasiones.set(true)
            });
    }
}
