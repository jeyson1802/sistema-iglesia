import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, finalize, forkJoin, of, Subscription } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { ToastModule } from 'primeng/toast';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConfiguracionIglesiaService } from '@/app/core/configuracion/configuracion-iglesia.service';
import { detalleDeError } from '@/app/core/errores/detalle-de-error';
import { nombreDeGrupo } from '@/app/features/grupos/models/grupo';
import { GruposService } from '@/app/features/grupos/services/grupos.service';
import { hoyEnPalabras, hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, PaginaPedida, paginaACargar } from '@/app/shared/utils/paginacion';
import { rangoDe } from '@/app/shared/utils/periodo';
import { AbrirOtraFechaComponent, OrigenReunion } from '../components/abrir-otra-fecha/abrir-otra-fecha';
import { FiltrosReunionesComponent } from '../components/filtros-reuniones/filtros-reuniones';
import { FormularioEventoComponent } from '../components/formulario-evento/formulario-evento';
import { TablaReunionesComponent } from '../components/tabla-reuniones/tabla-reuniones';
import { TarjetaReunionHoyComponent } from '../components/tarjeta-reunion-hoy/tarjeta-reunion-hoy';
import { AbrirReunion, CrearEvento, FiltroReuniones, Reunion, ReunionHoy } from '../models/asistencia';
import { CultosProgramadosService } from '../services/cultos-programados.service';
import { RUTA_ASISTENCIA } from '../models/rutas-asistencia';
import { ReunionesService } from '../services/reuniones.service';

const MENSAJE_ERROR_HOY = 'No se pudieron cargar las reuniones de hoy.';
const MENSAJE_ERROR_HISTORIAL = 'No se pudo cargar el historial.';
const MENSAJE_ERROR_ABRIR = 'No se pudo abrir la reunión. Intenta de nuevo.';
const MENSAJE_ERROR_EVENTO = 'No se pudo crear el evento. Intenta de nuevo.';

/**
 * Inicio de Asistencia (contenedor): las reuniones de hoy como tarjetas para tomar asistencia y, para quien puede
 * verlo, el historial paginado. Las reuniones de cultos y grupos se abren al tocarlas (la primera vez del día).
 */
@Component({
    selector: 'app-asistencia-inicio',
    imports: [RouterLink, ButtonModule, SkeletonModule, ToastModule, AbrirOtraFechaComponent, FiltrosReunionesComponent, FormularioEventoComponent, TablaReunionesComponent, TarjetaReunionHoyComponent],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './asistencia-inicio.html'
})
export class AsistenciaInicioComponent {
    private readonly reunionesServicio = inject(ReunionesService);
    private readonly cultosServicio = inject(CultosProgramadosService);
    private readonly gruposServicio = inject(GruposService);
    private readonly configuracion = inject(ConfiguracionIglesiaService);
    private readonly sesion = inject(SesionService);
    private readonly router = inject(Router);
    private readonly mensajes = inject(MessageService);

    protected readonly puedeMarcar = computed(() => this.sesion.tienePermiso('asistencia.registrar') || this.sesion.tienePermiso('asistencia.grupo'));
    protected readonly puedeVerHistorial = computed(() => this.sesion.tienePermiso('asistencia.ver') || this.sesion.tienePermiso('asistencia.grupo'));
    protected readonly puedeConfigurar = computed(() => this.sesion.tienePermiso('asistencia.configurar'));
    protected readonly hoyTexto = hoyEnPalabras();

    protected readonly hoy = signal<readonly ReunionHoy[]>([]);
    protected readonly cargandoHoy = signal(true);
    protected readonly errorHoy = signal<string | null>(null);
    /** Clave de la tarjeta que se está abriendo. */
    protected readonly abriendo = signal<string | null>(null);

    protected readonly filtro = signal<FiltroReuniones>({ ...rangoDe('MES'), tipo: null });
    protected readonly historial = signal<readonly Reunion[]>([]);
    protected readonly totalHistorial = signal(0);
    protected readonly pagina = signal(0);
    protected readonly tamanoPagina = signal(FILAS_POR_PAGINA);
    protected readonly cargandoHistorial = signal(true);
    protected readonly errorHistorial = signal<string | null>(null);

    protected readonly eventoVisible = signal(false);
    protected readonly creandoEvento = signal(false);
    protected readonly errorEvento = signal<string | null>(null);

    protected readonly otraFechaVisible = signal(false);
    protected readonly origenes = signal<readonly OrigenReunion[]>([]);
    protected readonly abriendoOtraFecha = signal(false);
    protected readonly errorOtraFecha = signal<string | null>(null);

    private cargaHistorial?: Subscription;

    constructor() {
        inject(DestroyRef).onDestroy(() => this.cargaHistorial?.unsubscribe());
        if (this.puedeMarcar()) {
            this.cargarHoy();
        }
        if (this.puedeVerHistorial()) {
            this.cargarHistorial(0);
        }
    }

    protected clave(reunion: ReunionHoy): string {
        return reunion.id !== null ? `r${reunion.id}` : reunion.cultoProgramadoId !== null ? `c${reunion.cultoProgramadoId}` : `g${reunion.grupoId}`;
    }

    protected cargarHoy(): void {
        this.cargandoHoy.set(true);
        this.errorHoy.set(null);
        this.reunionesServicio
            .hoy()
            .pipe(finalize(() => this.cargandoHoy.set(false)))
            .subscribe({ next: (reuniones) => this.hoy.set(reuniones), error: () => this.errorHoy.set(MENSAJE_ERROR_HOY) });
    }

    /** Una reunión ya abierta se retoma; una por abrir se abre primero (si otro equipo la abrió, devuelve la misma). */
    protected tomar(reunion: ReunionHoy): void {
        if (reunion.id !== null) {
            this.irATomar(reunion.id);
            return;
        }
        this.abriendo.set(this.clave(reunion));
        this.reunionesServicio
            .abrir({ cultoProgramadoId: reunion.cultoProgramadoId, grupoId: reunion.grupoId, fecha: hoyIso() })
            .pipe(finalize(() => this.abriendo.set(null)))
            .subscribe({
                next: (abierta) => this.irATomar(abierta.id),
                error: (error: unknown) => {
                    const detalle = detalleDeError(error, MENSAJE_ERROR_ABRIR);
                    if (detalle) {
                        this.mensajes.add({ severity: 'error', summary: 'No se pudo abrir', detail: detalle });
                    }
                }
            });
    }

    protected filtrar(filtro: FiltroReuniones): void {
        this.filtro.set(filtro);
        this.cargarHistorial(0);
    }

    protected cambiarPagina(pedida: PaginaPedida): void {
        const pagina = paginaACargar(pedida, this.tamanoPagina());
        this.tamanoPagina.set(pedida.tamano);
        this.cargarHistorial(pagina);
    }

    protected recargarHistorial(): void {
        this.cargarHistorial(this.pagina());
    }

    protected verDetalle(reunion: Reunion): void {
        this.router.navigate([RUTA_ASISTENCIA, 'reuniones', reunion.id, 'detalle']);
    }

    protected nuevoEvento(): void {
        this.errorEvento.set(null);
        this.eventoVisible.set(true);
    }

    protected crearEvento(evento: CrearEvento): void {
        this.creandoEvento.set(true);
        this.errorEvento.set(null);
        this.reunionesServicio
            .crearEvento(evento)
            .pipe(finalize(() => this.creandoEvento.set(false)))
            .subscribe({
                next: (creadas) => {
                    this.eventoVisible.set(false);
                    const dias = creadas.length === 1 ? 'un día' : `${creadas.length} días`;
                    this.mensajes.add({ severity: 'success', summary: 'Evento creado', detail: `«${evento.nombre}», ${dias}. Aparecerá en «Hoy» cada día del evento.` });
                    if (this.puedeMarcar()) {
                        this.cargarHoy();
                    }
                },
                error: (error: unknown) => this.errorEvento.set(detalleDeError(error, MENSAJE_ERROR_EVENTO))
            });
    }

    /** Ofrece los cultos (quien marca en toda la iglesia) y los grupos que puede ver o que se reúnen hoy. */
    protected otraFecha(): void {
        this.errorOtraFecha.set(null);
        this.otraFechaVisible.set(true);
        const cultos$ = this.sesion.tienePermiso('asistencia.registrar') ? this.cultosServicio.listar().pipe(catchError(() => of([]))) : of([]);
        const grupos$ = this.sesion.tienePermiso('grupos.ver') ? this.gruposServicio.listar().pipe(catchError(() => of([]))) : of([]);
        forkJoin({ cultos: cultos$, grupos: grupos$ }).subscribe(({ cultos, grupos }) => {
            const singular = this.configuracion.nombresGrupos().nombreGrupoSingular;
            const deCultos = cultos.filter(({ activo }) => activo).map((culto): OrigenReunion => ({ clave: `c${culto.id}`, etiqueta: culto.nombre, cultoProgramadoId: culto.id, grupoId: null }));
            const deGrupos = grupos.filter(({ activo }) => activo).map((grupo): OrigenReunion => ({ clave: `g${grupo.id}`, etiqueta: nombreDeGrupo(singular, grupo.lider.nombre), cultoProgramadoId: null, grupoId: grupo.id }));
            const deHoy = this.hoy()
                .filter((reunion) => reunion.grupoId !== null && !deGrupos.some(({ grupoId }) => grupoId === reunion.grupoId))
                .map((reunion): OrigenReunion => ({ clave: `g${reunion.grupoId}`, etiqueta: reunion.nombre, cultoProgramadoId: null, grupoId: reunion.grupoId }));
            this.origenes.set([...deCultos, ...deGrupos, ...deHoy]);
        });
    }

    protected abrirOtraFecha(reunion: AbrirReunion): void {
        this.abriendoOtraFecha.set(true);
        this.errorOtraFecha.set(null);
        this.reunionesServicio
            .abrir(reunion)
            .pipe(finalize(() => this.abriendoOtraFecha.set(false)))
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

    /** Una petición nueva cancela la anterior para que no llegue una respuesta vieja. */
    private cargarHistorial(pagina: number): void {
        this.cargaHistorial?.unsubscribe();
        this.pagina.set(pagina);
        this.cargandoHistorial.set(true);
        this.errorHistorial.set(null);
        this.cargaHistorial = this.reunionesServicio
            .historial(this.filtro(), pagina, this.tamanoPagina())
            .pipe(finalize(() => this.cargandoHistorial.set(false)))
            .subscribe({
                next: ({ content, page }) => {
                    this.historial.set(content);
                    this.totalHistorial.set(page.totalElements);
                },
                error: () => this.errorHistorial.set(MENSAJE_ERROR_HISTORIAL)
            });
    }
}
