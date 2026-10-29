import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TabsModule } from 'primeng/tabs';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { FiltrosSeguimientoComponent, OpcionGrupo } from '../components/filtros-seguimiento/filtros-seguimiento';
import { ServiciosSeguimientoComponent } from '../components/servicios-seguimiento/servicios-seguimiento';
import { TablaSeguimientoComponent } from '../components/tabla-seguimiento/tabla-seguimiento';
import { ConteoAlertas, FILTRO_SEGUIMIENTO_INICIAL, FiltroSeguimiento, PersonaSeguimiento, ServiciosSeguimiento } from '../models/seguimiento';
import { SeguimientoService } from '../services/seguimiento.service';

type Pestana = 'personas' | 'servicios';

/**
 * Seguimiento de asistencia (contenedor). «Personas»: a quién hay que contactar, con filtros por alerta, grupo y nombre,
 * paginado en el servidor; un líder ve aquí solo a su red. «Servicios» (con `asistencia.ver`): evolución de los cultos
 * y de los grupos.
 */
@Component({
    selector: 'app-seguimiento-asistencia',
    imports: [ButtonModule, SkeletonModule, TabsModule, FiltrosSeguimientoComponent, ServiciosSeguimientoComponent, TablaSeguimientoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './seguimiento-asistencia.html'
})
export class SeguimientoAsistenciaComponent implements OnInit {
    private readonly servicio = inject(SeguimientoService);
    private readonly grupos = inject(GruposService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly configuracion = inject(ConfiguracionIglesiaService);

    protected readonly puedeVerServicios = computed(() => this.sesion.tienePermiso('asistencia.ver'));
    protected readonly esLider = computed(() => !this.sesion.tienePermiso('asistencia.ver'));
    protected readonly nombreGrupo = computed(() => this.configuracion.nombresGrupos().nombreGrupoSingular);
    protected readonly nombreGrupos = computed(() => this.configuracion.nombresGrupos().nombreGrupoPlural);
    protected readonly pestana = signal<Pestana>('personas');

    protected readonly filtro = signal<FiltroSeguimiento>(FILTRO_SEGUIMIENTO_INICIAL);
    protected readonly personas = signal<readonly PersonaSeguimiento[]>([]);
    protected readonly total = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargando = signal(true);
    protected readonly error = signal(false);
    protected readonly conteos = signal<ConteoAlertas | null>(null);
    protected readonly opcionesGrupo = signal<readonly OpcionGrupo[]>([]);

    protected readonly servicios = signal<ServiciosSeguimiento | null>(null);
    protected readonly cargandoServicios = signal(false);
    protected readonly errorServicios = signal(false);

    private carga?: Subscription;
    private cargaConteos?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => {
            this.carga?.unsubscribe();
            this.cargaConteos?.unsubscribe();
        });
    }

    ngOnInit(): void {
        this.cargar();
        this.cargarConteos();
        if (this.sesion.tienePermiso('grupos.ver')) {
            this.grupos.listar().subscribe({
                next: (grupos) => this.opcionesGrupo.set(grupos.filter(({ activo }) => activo).map(({ id, lider }) => ({ id, nombre: `${this.nombreGrupo()} de ${lider.nombre}` }))),
                error: () => this.opcionesGrupo.set([])
            });
        }
    }

    protected filtrar(filtro: FiltroSeguimiento): void {
        const cambiaConteo = filtro.grupoId !== this.filtro().grupoId || filtro.q !== this.filtro().q;
        this.filtro.set(filtro);
        this.pagina.set(0);
        this.cargar();
        if (cambiaConteo) {
            this.cargarConteos();
        }
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        this.pagina.set(paginaACargar(pedida, this.tamanoPagina()));
        this.tamanoPagina.set(pedida.tamano);
        this.cargar();
    }

    protected ver({ miembroId }: PersonaSeguimiento): void {
        this.router.navigate(['/miembros', miembroId, 'asistencia']);
    }

    protected cambiarPestana(valor: string | number | undefined): void {
        const pestana: Pestana = valor === 'servicios' ? 'servicios' : 'personas';
        this.pestana.set(pestana);
        if (pestana === 'servicios' && this.servicios() === null && !this.cargandoServicios()) {
            this.cargarServicios();
        }
    }

    protected cargar(): void {
        this.carga?.unsubscribe();
        this.cargando.set(true);
        this.error.set(false);
        this.carga = this.servicio
            .lista(this.filtro(), this.pagina(), this.tamanoPagina())
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.personas.set(content);
                    this.total.set(page.totalElements);
                },
                error: () => this.error.set(true)
            });
    }

    protected cargarServicios(): void {
        this.cargandoServicios.set(true);
        this.errorServicios.set(false);
        this.servicio
            .servicios()
            .pipe(finalize(() => this.cargandoServicios.set(false)))
            .subscribe({ next: (servicios) => this.servicios.set(servicios), error: () => this.errorServicios.set(true) });
    }

    private cargarConteos(): void {
        this.cargaConteos?.unsubscribe();
        this.cargaConteos = this.servicio.conteos(this.filtro()).subscribe({ next: (conteos) => this.conteos.set(conteos), error: () => this.conteos.set(null) });
    }
}
