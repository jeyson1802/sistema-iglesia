import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { DiaSemana, diaSemanaDe } from '@/app/core/models/dia-semana';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { AbrirReunion } from '../../models/asistencia';
import { PersonaSeguimiento, resumenSerie, SeguimientoGrupo, semanaCorta } from '../../models/seguimiento';
import { RUTA_ASISTENCIA } from '../../models/rutas-asistencia';
import { ReunionesService } from '../../services/reuniones.service';
import { SeguimientoService } from '../../services/seguimiento.service';
import { AbrirOtraFechaComponent, OrigenReunion } from '../abrir-otra-fecha/abrir-otra-fecha';
import { ReunionesGrupoComponent } from '../reuniones-grupo/reuniones-grupo';
import { TablaSeguimientoComponent } from '../tabla-seguimiento/tabla-seguimiento';
import { TendenciaAsistenciaComponent } from '../tendencia-asistencia/tendencia-asistencia';

const MENSAJE_ERROR_ABRIR = 'No se pudo abrir la reunión. Intenta de nuevo.';

/** Lo que la tarjeta necesita del grupo. */
export interface GrupoAsistencia {
    readonly id: number;
    readonly nombre: string;
    readonly diaReunion: DiaSemana | null;
    readonly activo: boolean;
}

/**
 * Asistencia de un grupo (contenedor) para su página de detalle: un resumen de las últimas 12 semanas, la evolución
 * semanal del porcentaje de sus integrantes que asistió, sus reuniones por día (con quiénes asistieron y quiénes no), sus integrantes ordenados por riesgo (faltas seguidas, alertas) y el acceso «Tomar asistencia de hoy»
 * (si hoy es su día) o «de otra fecha».
 */
@Component({
    selector: 'app-asistencia-grupo',
    imports: [ButtonModule, SkeletonModule, ToastModule, AbrirOtraFechaComponent, GraficoEvolucionComponent, ReunionesGrupoComponent, TablaSeguimientoComponent, TendenciaAsistenciaComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './asistencia-grupo.html',
    host: { '[class.card]': 'visible()', '[class.block]': 'visible()' }
})
export class AsistenciaGrupoComponent {
    private readonly servicio = inject(ReunionesService);
    private readonly seguimiento = inject(SeguimientoService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly mensajes = inject(MessageService);

    readonly grupo = input.required<GrupoAsistencia>();

    protected readonly puedeVer = computed(() => this.sesion.tienePermiso('asistencia.ver') || this.sesion.tienePermiso('asistencia.grupo'));
    protected readonly puedeMarcar = computed(() => this.grupo().activo && (this.sesion.tienePermiso('asistencia.registrar') || this.sesion.tienePermiso('asistencia.grupo')));
    protected readonly visible = computed(() => this.puedeVer() || this.puedeMarcar());
    protected readonly esHoy = computed(() => this.grupo().diaReunion === diaSemanaDe(new Date()));
    protected readonly puedeVerPersonas = computed(() => this.sesion.tienePermiso('asistencia.ver') || this.sesion.tienePermiso('miembros.ver'));
    protected readonly datos = signal<SeguimientoGrupo | null>(null);
    protected readonly huboReuniones = computed(() => (this.datos()?.semanas ?? []).some(({ reuniones }) => reuniones > 0));
    protected readonly etiquetasSemanas = computed(() => (this.datos()?.semanas ?? []).map(({ inicio }) => semanaCorta(inicio)));
    protected readonly seriesSemanas = computed<readonly SerieGrafico[]>(() => [{ etiqueta: 'Integrantes que asistieron', valores: (this.datos()?.semanas ?? []).map(({ porcentaje }) => porcentaje), tono: 'primario' }]);
    /** Promedio, semana pasada (frente al promedio de las anteriores) y reuniones de las últimas 12 semanas. */
    protected readonly resumen = computed(() => {
        const semanas = this.datos()?.semanas ?? [];
        const { promedio, semanaPasada, variacion } = resumenSerie(
            semanas.map(({ porcentaje }) => porcentaje),
            true
        );
        return {
            promedio,
            semanaPasada,
            promedioAnterior: semanaPasada === null || variacion === null ? null : semanaPasada - variacion,
            reuniones: semanas.reduce((suma, { reuniones }) => suma + reuniones, 0)
        };
    });
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly abriendo = signal(false);
    protected readonly otraFechaVisible = signal(false);
    protected readonly errorOtraFecha = signal<string | null>(null);
    protected readonly origenes = computed<readonly OrigenReunion[]>(() => [{ clave: `g${this.grupo().id}`, etiqueta: this.grupo().nombre, cultoProgramadoId: null, grupoId: this.grupo().id }]);

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        effect(() => {
            if (this.puedeVer()) {
                const id = this.grupo().id;
                untracked(() => this.cargar(id));
            }
        });
    }

    protected cargar(id: number = this.grupo().id): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.seguimiento
            .delGrupo(id)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (datos) => {
                    this.datos.set(datos);
                    this.pagina.set(0);
                },
                error: () => this.error.set(true)
            });
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
    }

    protected verPersona({ miembroId }: PersonaSeguimiento): void {
        if (this.puedeVerPersonas()) {
            this.router.navigate(['/miembros', miembroId, 'asistencia']);
        }
    }

    protected tomarHoy(): void {
        this.abriendo.set(true);
        this.servicio
            .abrir({ cultoProgramadoId: null, grupoId: this.grupo().id, fecha: hoyIso() })
            .pipe(finalize(() => this.abriendo.set(false)))
            .subscribe({
                next: (reunion) => this.irATomar(reunion.id),
                error: (error: unknown) => {
                    const detalle = detalleDeError(error, MENSAJE_ERROR_ABRIR);
                    if (detalle) {
                        this.mensajes.add({ severity: 'error', summary: 'No se pudo abrir', detail: detalle });
                    }
                }
            });
    }

    protected otraFecha(): void {
        this.errorOtraFecha.set(null);
        this.otraFechaVisible.set(true);
    }

    protected abrirOtraFecha(reunion: AbrirReunion): void {
        this.abriendo.set(true);
        this.errorOtraFecha.set(null);
        this.servicio
            .abrir(reunion)
            .pipe(finalize(() => this.abriendo.set(false)))
            .subscribe({
                next: (abierta) => {
                    this.otraFechaVisible.set(false);
                    this.irATomar(abierta.id);
                },
                error: (error: unknown) => this.errorOtraFecha.set(detalleDeError(error, MENSAJE_ERROR_ABRIR))
            });
    }

    private irATomar(id: number): void {
        this.router.navigate([RUTA_ASISTENCIA, 'reuniones', id]);
    }
}
