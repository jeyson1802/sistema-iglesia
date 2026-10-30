import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { SkeletonModule } from 'primeng/skeleton';
import { fechaConDia, hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { DetalleReunionGrupo, etiquetaLista, PersonaReunionGrupo, ReunionGrupo } from '../../models/asistencia-grupo';
import { AsistenciaGrupoService } from '../../services/asistencia-grupo.service';
import { DetalleReunionGrupoComponent } from '../detalle-reunion-grupo/detalle-reunion-grupo';
import { TablaReunionesGrupoComponent } from '../tabla-reuniones-grupo/tabla-reuniones-grupo';

/**
 * Reuniones de un grupo por día (contenedor): carga la página de reuniones con su resumen y, al elegir una, abre un
 * panel lateral con quiénes asistieron, faltaron y vinieron de visita.
 */
@Component({
    selector: 'app-reuniones-grupo',
    imports: [ButtonModule, DrawerModule, SkeletonModule, DetalleReunionGrupoComponent, TablaReunionesGrupoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './reuniones-grupo.html'
})
export class ReunionesGrupoComponent {
    private readonly servicio = inject(AsistenciaGrupoService);
    private readonly router = inject(Router);

    readonly grupoId = input.required<number>();
    /** Si el nombre de cada persona lleva a su asistencia (exige `asistencia.ver` o `miembros.ver`). */
    readonly puedeVerPersonas = input(false);

    protected readonly reuniones = signal<readonly ReunionGrupo[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);

    protected readonly elegida = signal<ReunionGrupo | null>(null);
    protected readonly detalle = signal<DetalleReunionGrupo | null>(null);
    protected readonly cargandoDetalle = signal(false);
    protected readonly errorDetalle = signal(false);
    protected readonly panelVisible = signal(false);

    protected readonly tituloPanel = computed(() => {
        const reunion = this.elegida();
        return reunion ? `Reunión del ${fechaConDia(reunion.fecha)}` : 'Reunión';
    });
    protected readonly etiquetaFaltaron = computed(() => etiquetaLista('faltaron', this.elegida()?.fecha === hoyIso()));

    private carga?: Subscription;
    private cargaDetalle?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.carga?.unsubscribe();
            this.cargaDetalle?.unsubscribe();
        });
        effect(() => {
            const id = this.grupoId();
            untracked(() => {
                this.pagina.set(0);
                this.cargar(id);
            });
        });
    }

    protected cargar(id: number = this.grupoId()): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.servicio
            .reuniones(id, this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.reuniones.set(content);
                    this.total.set(page.totalElements);
                },
                error: () => this.error.set(true)
            });
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargar();
    }

    protected ver(reunion: ReunionGrupo): void {
        this.elegida.set(reunion);
        this.panelVisible.set(true);
        this.cargarDetalle();
    }

    protected cargarDetalle(): void {
        const reunion = this.elegida();
        if (!reunion) {
            return;
        }
        this.cargaDetalle?.unsubscribe();
        this.detalle.set(null);
        this.errorDetalle.set(false);
        this.cargandoDetalle.set(true);
        this.cargaDetalle = this.servicio
            .detalle(this.grupoId(), reunion.reunionId)
            .pipe(finalize(() => this.cargandoDetalle.set(false)))
            .subscribe({ next: (detalle) => this.detalle.set(detalle), error: () => this.errorDetalle.set(true) });
    }

    protected verPersona({ miembroId }: PersonaReunionGrupo): void {
        this.router.navigate(['/miembros', miembroId, 'asistencia']);
    }
}
