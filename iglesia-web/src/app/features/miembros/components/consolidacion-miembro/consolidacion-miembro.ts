import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { Permiso } from '@/app/core/auth/models/permiso';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { ConsolidacionMiembro, ETIQUETA_ESTADO_CASO, fechaHoraCompleta, mesAnio, resumenContactos, SEVERIDAD_ESTADO_CASO } from '@/app/features/consolidacion/models/consolidacion';
import { RUTA_CONSOLIDACION } from '@/app/features/consolidacion/models/rutas-consolidacion';
import { CasosService } from '@/app/features/consolidacion/services/casos.service';

/** Respuestas con las que la persona queda fuera de su ámbito: la tarjeta no se muestra. */
const FUERA_DE_AMBITO: readonly number[] = [HttpStatusCode.NotFound, HttpStatusCode.Forbidden];

/** Cualquiera de estos permisos muestra la tarjeta. */
const PERMISOS_CONSOLIDACION: readonly Permiso[] = ['consolidacion.ver', 'consolidacion.administrar', 'consolidacion.contactar'];

/**
 * Tarjeta «Consolidación» de la ficha (contenedor): iteraciones y contactos en una línea, último resultado, el caso
 * abierto (con «Ver caso») y las acciones «Ver historial» e «Iniciar seguimiento». Aparece con cualquier permiso de
 * consolidación; si la persona está fuera de su ámbito (404 o 403), no se muestra.
 */
@Component({
    selector: 'app-consolidacion-miembro',
    imports: [RouterLink, ButtonModule, MessageModule, SkeletonModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './consolidacion-miembro.html',
    host: { '[class.card]': 'visible()', '[class.block]': 'visible()' }
})
export class ConsolidacionMiembroComponent {
    private readonly sesion = inject(SesionService);
    private readonly casos = inject(CasosService);
    private readonly router = inject(Router);

    readonly miembroId = input.required<number>();

    private readonly conPermiso = computed(() => PERMISOS_CONSOLIDACION.some((permiso) => this.sesion.tienePermiso(permiso)));
    private readonly fueraDeAmbito = signal(false);
    protected readonly visible = computed(() => this.conPermiso() && !this.fueraDeAmbito());
    protected readonly resumen = signal<ConsolidacionMiembro | null>(null);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly iniciando = signal(false);
    protected readonly errorInicio = signal<string | null>(null);
    protected readonly linea = computed(() => {
        const resumen = this.resumen();
        if (!resumen || resumen.iteraciones === 0) {
            return null;
        }
        const iteraciones = resumen.iteraciones === 1 ? '1 iteración' : `${resumen.iteraciones} iteraciones`;
        return resumen.contactos.total === 0 ? iteraciones : `${iteraciones} · ${resumenContactos(resumen.contactos).toLowerCase()}`;
    });
    protected readonly rutaConsolidacion = RUTA_CONSOLIDACION;
    protected readonly etiquetaEstado = ETIQUETA_ESTADO_CASO;
    protected readonly severidadEstado = SEVERIDAD_ESTADO_CASO;
    protected readonly mes = mesAnio;
    protected readonly completa = fechaHoraCompleta;

    private carga?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        effect(() => {
            if (this.conPermiso()) {
                const miembroId = this.miembroId();
                untracked(() => this.cargar(miembroId));
            }
        });
    }

    protected cargar(miembroId: number = this.miembroId()): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.casos
            .resumenDeMiembro(miembroId)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: (resumen) => this.resumen.set(resumen),
                error: (error: unknown) => (error instanceof HttpErrorResponse && FUERA_DE_AMBITO.includes(error.status) ? this.fueraDeAmbito.set(true) : this.error.set(true))
            });
    }

    protected iniciar(): void {
        this.iniciando.set(true);
        this.errorInicio.set(null);
        this.casos
            .abrir(this.miembroId())
            .pipe(finalize(() => this.iniciando.set(false)))
            .subscribe({
                next: (caso) => void this.router.navigate([RUTA_CONSOLIDACION, 'casos', caso.id]),
                error: (error: unknown) => this.errorInicio.set(detalleDeError(error, 'No se pudo iniciar el seguimiento. Intenta de nuevo.'))
            });
    }
}
