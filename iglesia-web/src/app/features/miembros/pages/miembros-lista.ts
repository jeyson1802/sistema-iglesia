import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of, Subscription } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { Referencia } from '@/app/core/models/referencia';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { NivelesService } from '@/app/features/niveles/services/niveles.service';
import { FiltrosMiembrosComponent } from '../components/filtros-miembros/filtros-miembros';
import { TablaMiembrosComponent } from '../components/tabla-miembros/tabla-miembros';
import { FILTRO_MIEMBROS_INICIAL, FiltroMiembros, MiembroResumen } from '../models/miembro';
import { MiembrosService } from '../services/miembros.service';

const MENSAJE_ERROR_CARGA = 'No se pudieron cargar los miembros.';

/**
 * Registro de miembros (contenedor): carga paginada y filtrada en el servidor. El backend solo devuelve lo que el
 * usuario puede ver (toda la iglesia o, para un líder, su red y sus superiores).
 */
@Component({
    selector: 'app-miembros-lista',
    imports: [RouterLink, ButtonModule, FiltrosMiembrosComponent, TablaMiembrosComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './miembros-lista.html'
})
export class MiembrosListaComponent {
    private readonly servicio = inject(MiembrosService);
    private readonly nivelesServicio = inject(NivelesService);
    private readonly gruposServicio = inject(GruposService);
    private readonly sesion = inject(SesionService);

    protected readonly nombresGrupos = inject(ConfiguracionIglesiaService).nombresGrupos;
    protected readonly miembros = signal<readonly MiembroResumen[]>([]);
    protected readonly totalRegistros = signal(0);
    protected readonly pagina = signal(0);
    protected readonly cargando = signal(true);
    protected readonly errorCarga = signal<string | null>(null);
    /** Opciones de los filtros de nivel y grupo (los activos, en el orden del backend). */
    protected readonly niveles = signal<readonly Referencia[]>([]);
    protected readonly grupos = signal<readonly Referencia[]>([]);
    private readonly filtro = signal<FiltroMiembros>(FILTRO_MIEMBROS_INICIAL);
    private carga?: Subscription;

    protected readonly puedeCrear = computed(() => this.sesion.tienePermiso('miembros.crear'));

    constructor() {
        inject(DestroyRef).onDestroy(() => this.carga?.unsubscribe());
        this.cargarOpcionesDeFiltro();
        this.cargar();
    }

    protected cargar(): void {
        this.cargarPagina(this.pagina());
    }

    protected filtrar(filtro: FiltroMiembros): void {
        this.filtro.set(filtro);
        this.cargarPagina(0);
    }

    protected cambiarPagina(pagina: number): void {
        this.cargarPagina(pagina);
    }

    /** Pide la página al backend; una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarPagina(pagina: number): void {
        this.carga?.unsubscribe();
        this.pagina.set(pagina);
        this.cargando.set(true);
        this.errorCarga.set(null);
        this.carga = this.servicio
            .listar(this.filtro(), pagina)
            .pipe(finalize(() => this.cargando.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.miembros.set(content);
                    this.totalRegistros.set(page.totalElements);
                },
                error: () => this.errorCarga.set(MENSAJE_ERROR_CARGA)
            });
    }

    /** Los filtros de nivel y grupo son una ayuda: si no cargan, se quedan solo con «Todos» y la tabla sigue funcionando. */
    private cargarOpcionesDeFiltro(): void {
        forkJoin({
            niveles: this.nivelesServicio.listar().pipe(catchError(() => of([]))),
            grupos: this.gruposServicio.listar().pipe(catchError(() => of([])))
        }).subscribe(({ niveles, grupos }) => {
            this.niveles.set(niveles.filter(({ activo }) => activo));
            this.grupos.set(grupos.filter(({ activo }) => activo));
        });
    }
}
