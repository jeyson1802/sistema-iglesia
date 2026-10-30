import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, model, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, debounceTime, of, switchMap, tap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { ProgressBarModule } from 'primeng/progressbar';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { coincideBusqueda } from '@/app/shared/utils/texto';
import { Asignacion, Consolidador, iniciales, porcentaje, PropuestaReparto, SolicitudReparto } from '../../models/consolidacion';
import { RondasService } from '../../services/rondas.service';
import { ESPERA_VISTA_PREVIA_MS } from '../selector-lote/selector-lote';

/** Fila de la tabla del equipo: su carga y lo que recibiría con la propuesta. */
interface FilaEquipo {
    readonly miembroId: number;
    readonly nombre: string;
    readonly elegido: boolean;
    readonly cargaActual: number;
    /** Personas que recibe en esta propuesta; `null` si no está elegido. */
    readonly recibe: number | null;
    readonly quedaCon: number;
}

/** Fila de la tabla de personas: a quién queda asignada y si se movió a mano. */
interface FilaPersona {
    readonly casoId: number;
    readonly nombre: string;
    readonly consolidadorId: number;
    readonly propuesto: number;
    readonly movida: boolean;
}

/** Opción «todos» del filtro por consolidador de la tabla de personas. */
const TODOS = 0;

/**
 * Reparto de una ronda, pensado para equipos grandes y lotes de miles: el equipo en una tabla con búsqueda (elegir a
 * quiénes, cuánto tiene y cuánto recibiría), un resumen del equilibrio y, plegada, la tabla de personas para ajustar
 * a mano una a una o en bloque. Expone la solicitud como `model` para que el contenedor la aplique.
 */
@Component({
    selector: 'app-reparto',
    imports: [FormsModule, ButtonModule, CheckboxModule, IconFieldModule, InputIconModule, InputTextModule, PanelModule, ProgressBarModule, SelectModule, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './reparto.html'
})
export class RepartoComponent {
    private readonly rondas = inject(RondasService);

    readonly rondaId = input.required<number>();
    readonly equipo = input.required<readonly Consolidador[]>();
    /** Solo reparte a quienes aún no tienen consolidador (si no, rehace todo el reparto de lo no contactado). */
    readonly soloSinAsignar = input(true);
    readonly solicitud = model<SolicitudReparto | null>(null);

    protected readonly elegidos = signal<ReadonlySet<number>>(new Set());
    protected readonly propuesta = signal<PropuestaReparto | null>(null);
    protected readonly ajustes = signal<ReadonlyMap<number, number>>(new Map());
    protected readonly cargando = signal(false);
    protected readonly error = signal(false);
    protected readonly iniciales = iniciales;
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    protected readonly buscarEquipo = signal('');
    protected readonly buscarPersona = signal('');
    protected readonly filtroConsolidador = signal(TODOS);
    protected readonly seleccionPersonas = signal<ReadonlySet<number>>(new Set());
    protected readonly destinoBloque = signal<number | null>(null);

    protected readonly activos = computed(() => this.equipo().filter(({ activo }) => activo));
    protected readonly opcionesMover = computed(() =>
        this.activos()
            .filter(({ miembroId }) => this.elegidos().has(miembroId))
            .map(({ miembroId, nombreCompleto }) => ({ id: miembroId, nombre: nombreCompleto }))
    );
    protected readonly opcionesFiltro = computed(() => [{ id: TODOS, nombre: 'Todos los consolidadores' }, ...this.opcionesMover()]);

    /** A quién queda cada caso, con los ajustes hechos a mano. */
    private readonly asignacionFinal = computed(() => {
        const ajustes = this.ajustes();
        return (this.propuesta()?.asignaciones ?? []).map((asignacion) => ({ ...asignacion, final: ajustes.get(asignacion.casoId) ?? asignacion.consolidadorId }));
    });
    private readonly recibePor = computed(() => {
        const conteo = new Map<number, number>();
        this.asignacionFinal().forEach(({ final }) => conteo.set(final, (conteo.get(final) ?? 0) + 1));
        return conteo;
    });

    protected readonly total = computed(() => this.propuesta()?.asignaciones.length ?? 0);
    protected readonly movidas = computed(() => this.ajustes().size);
    protected readonly todosElegidos = computed(() => this.activos().length > 0 && this.activos().every(({ miembroId }) => this.elegidos().has(miembroId)));

    protected readonly filasEquipo = computed<FilaEquipo[]>(() => {
        const recibe = this.recibePor();
        const hayPropuesta = this.propuesta() !== null;
        return this.activos()
            .filter(({ nombreCompleto }) => coincideBusqueda(nombreCompleto, this.buscarEquipo()))
            .map(({ miembroId, nombreCompleto, casosAbiertos }) => {
                const elegido = this.elegidos().has(miembroId);
                const nuevos = elegido && hayPropuesta ? (recibe.get(miembroId) ?? 0) : null;
                return { miembroId, nombre: nombreCompleto, elegido, cargaActual: casosAbiertos, recibe: nuevos, quedaCon: casosAbiertos + (nuevos ?? 0) };
            });
    });
    /** Referencia para las barras de «Quedará con»: la carga más alta del equipo. */
    private readonly cargaMaxima = computed(() => Math.max(1, ...this.activos().map(({ miembroId, casosAbiertos }) => casosAbiertos + (this.elegidos().has(miembroId) ? (this.recibePor().get(miembroId) ?? 0) : 0))));

    /** Equilibrio de la propuesta: cuántos recibe cada uno, del que menos al que más. */
    protected readonly equilibrio = computed(() => {
        const elegidos = [...this.elegidos()];
        if (elegidos.length === 0 || this.total() === 0) {
            return null;
        }
        const recibidos = elegidos.map((id) => this.recibePor().get(id) ?? 0);
        return { promedio: Math.round(this.total() / elegidos.length), minimo: Math.min(...recibidos), maximo: Math.max(...recibidos), consolidadores: elegidos.length };
    });

    /** Ordenadas por consolidador (y dentro, por persona), para revisar de corrido lo que recibe cada uno. */
    protected readonly filasPersonas = computed<FilaPersona[]>(() => {
        const filtro = this.filtroConsolidador();
        const nombres = new Map(this.activos().map(({ miembroId, nombreCompleto }) => [miembroId, nombreCompleto]));
        return this.asignacionFinal()
            .filter(({ final, persona }) => (filtro === TODOS || final === filtro) && coincideBusqueda(persona.nombreCompleto, this.buscarPersona()))
            .map(({ casoId, persona, consolidadorId, final }) => ({ casoId, nombre: persona.nombreCompleto, consolidadorId: final, propuesto: consolidadorId, movida: final !== consolidadorId }))
            .sort((a, b) => (nombres.get(a.consolidadorId) ?? '').localeCompare(nombres.get(b.consolidadorId) ?? '', 'es') || a.nombre.localeCompare(b.nombre, 'es'));
    });
    protected readonly todasLasVisiblesElegidas = computed(() => this.filasPersonas().length > 0 && this.filasPersonas().every(({ casoId }) => this.seleccionPersonas().has(casoId)));

    constructor() {
        effect(() => {
            const ids = this.activos().map(({ miembroId }) => miembroId);
            untracked(() => this.elegidos.set(new Set(ids)));
        });
        toObservable(this.elegidos)
            .pipe(
                debounceTime(ESPERA_VISTA_PREVIA_MS),
                tap(() => {
                    this.ajustes.set(new Map());
                    this.seleccionPersonas.set(new Set());
                    this.filtroConsolidador.set(TODOS);
                    this.error.set(false);
                    this.cargando.set(true);
                }),
                switchMap((elegidos) =>
                    elegidos.size === 0
                        ? of(null)
                        : this.rondas.vistaPreviaReparto(this.rondaId(), { consolidadorIds: [...elegidos], soloSinAsignar: this.soloSinAsignar() }).pipe(
                              catchError(() => {
                                  this.error.set(true);
                                  return of(null);
                              })
                          )
                ),
                takeUntilDestroyed(inject(DestroyRef))
            )
            .subscribe((propuesta) => {
                this.cargando.set(false);
                this.propuesta.set(propuesta);
                this.actualizarSolicitud();
            });
    }

    protected proporcion(carga: number): number {
        return porcentaje(carga, this.cargaMaxima());
    }

    protected alternar(miembroId: number, marcado: boolean): void {
        this.elegidos.update((elegidos) => {
            const nuevos = new Set(elegidos);
            if (marcado) {
                nuevos.add(miembroId);
            } else {
                nuevos.delete(miembroId);
            }
            return nuevos;
        });
    }

    /** Marca o desmarca a todos los que se ven con la búsqueda actual. */
    protected alternarVisibles(marcado: boolean): void {
        const visibles = this.filasEquipo().map(({ miembroId }) => miembroId);
        this.elegidos.update((elegidos) => {
            const nuevos = new Set(elegidos);
            visibles.forEach((id) => (marcado ? nuevos.add(id) : nuevos.delete(id)));
            return nuevos;
        });
    }

    protected mover(casoId: number, consolidadorId: number): void {
        this.aplicarAjustes([casoId], consolidadorId);
    }

    protected deshacerMovimiento(casoId: number): void {
        this.ajustes.update((ajustes) => {
            const nuevos = new Map(ajustes);
            nuevos.delete(casoId);
            return nuevos;
        });
        this.actualizarSolicitud();
    }

    protected alternarPersona(casoId: number, marcado: boolean): void {
        this.seleccionPersonas.update((seleccion) => {
            const nueva = new Set(seleccion);
            if (marcado) {
                nueva.add(casoId);
            } else {
                nueva.delete(casoId);
            }
            return nueva;
        });
    }

    protected alternarPersonasVisibles(marcado: boolean): void {
        const visibles = this.filasPersonas().map(({ casoId }) => casoId);
        this.seleccionPersonas.update((seleccion) => {
            const nueva = new Set(seleccion);
            visibles.forEach((id) => (marcado ? nueva.add(id) : nueva.delete(id)));
            return nueva;
        });
    }

    protected moverSeleccionadas(): void {
        const destino = this.destinoBloque();
        if (destino === null) {
            return;
        }
        this.aplicarAjustes([...this.seleccionPersonas()], destino);
        this.seleccionPersonas.set(new Set());
        this.destinoBloque.set(null);
    }

    protected reintentar(): void {
        this.elegidos.update((elegidos) => new Set(elegidos));
    }

    /** Guarda solo lo que difiere de la propuesta: volver al propuesto equivale a deshacer. */
    private aplicarAjustes(casoIds: readonly number[], consolidadorId: number): void {
        const propuestos = new Map((this.propuesta()?.asignaciones ?? []).map(({ casoId, consolidadorId: propuesto }) => [casoId, propuesto]));
        this.ajustes.update((ajustes) => {
            const nuevos = new Map(ajustes);
            casoIds.forEach((casoId) => (propuestos.get(casoId) === consolidadorId ? nuevos.delete(casoId) : nuevos.set(casoId, consolidadorId)));
            return nuevos;
        });
        this.actualizarSolicitud();
    }

    private actualizarSolicitud(): void {
        const propuesta = this.propuesta();
        if (!propuesta || propuesta.asignaciones.length === 0) {
            this.solicitud.set(null);
            return;
        }
        const base: SolicitudReparto = { consolidadorIds: [...this.elegidos()], soloSinAsignar: this.soloSinAsignar() };
        // Sin ajustes basta con repetir el criterio; con ajustes se envía el reparto exacto que se ve en pantalla.
        const asignaciones: Asignacion[] = this.asignacionFinal().map(({ casoId, final }) => ({ casoId, consolidadorId: final }));
        this.solicitud.set(this.ajustes().size === 0 ? base : { ...base, asignaciones });
    }
}
