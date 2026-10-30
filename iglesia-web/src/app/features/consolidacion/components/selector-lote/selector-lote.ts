import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, model, signal, untracked } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { catchError, debounceTime, of, switchMap, tap } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';
import { Referencia } from '@/app/core/models/referencia';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { enDias, FILTRO_LOTE_INICIAL, FiltroLote, iniciales, LIMITES_CONSOLIDACION, VistaPreviaLote } from '../../models/consolidacion';
import { RondasService } from '../../services/rondas.service';

/** Espera tras el último cambio de filtro antes de pedir la vista previa. */
export const ESPERA_VISTA_PREVIA_MS = 300;
/** «Ingresaron en los últimos 90 días». */
const DIAS_RECIENTES = 90;

type Modo = 'filtros' | 'manual';
type Chip = 'nuevos' | 'recientes' | 'nuevoSinVolver' | 'faltasSeguidas' | 'sinGrupo' | 'nuncaConsolidados';

const CHIPS: readonly { readonly valor: Chip; readonly etiqueta: string; readonly icono: string }[] = [
    { valor: 'nuevos', etiqueta: 'Nuevos', icono: 'pi pi-star' },
    { valor: 'recientes', etiqueta: `Ingresaron en los últimos ${DIAS_RECIENTES} días`, icono: 'pi pi-calendar-plus' },
    { valor: 'nuevoSinVolver', etiqueta: 'Nuevo sin volver', icono: 'pi pi-user-minus' },
    { valor: 'faltasSeguidas', etiqueta: 'Faltas seguidas', icono: 'pi pi-exclamation-circle' },
    { valor: 'sinGrupo', etiqueta: 'Sin grupo', icono: 'pi pi-users' },
    { valor: 'nuncaConsolidados', etiqueta: 'Nunca consolidados', icono: 'pi pi-heart' }
];

const aFecha = (iso: string | null): Date | null => {
    if (!iso) {
        return null;
    }
    const [anio, mes, dia] = iso.split('-').map(Number);
    return new Date(anio, mes - 1, dia);
};

/**
 * Elige a quiénes entran en una ronda: con filtros en chips (más grupo y rango de ingreso) o eligiendo personas a mano.
 * La vista previa («Entrarían 37 personas», con las primeras caras) se pide 300 ms después del último cambio y cancela
 * la anterior. Expone el filtro como `model` para que el contenedor lo agregue.
 */
@Component({
    selector: 'app-selector-lote',
    imports: [FormsModule, ButtonModule, DatePickerModule, PaginatorModule, SelectModule, SelectButtonModule, SkeletonModule, TagModule, SelectorMiembroComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-lote.html'
})
export class SelectorLoteComponent {
    private readonly rondas = inject(RondasService);

    readonly rondaId = input.required<number>();
    readonly grupos = input<readonly Referencia[]>([]);
    protected readonly opcionesGrupos = computed(() => [...this.grupos()]);
    readonly etiquetaGrupo = input('Grupo');
    readonly filtro = model<FiltroLote>(FILTRO_LOTE_INICIAL);
    /** Cantidad de personas que se agregarían (para que el contenedor habilite «Agregar»). */
    readonly entrarian = model(0);

    protected readonly chips = CHIPS;
    protected readonly loteMax = LIMITES_CONSOLIDACION.loteMax;
    protected readonly modos = [
        { label: 'Con filtros', value: 'filtros' as Modo },
        { label: 'Elegir a mano', value: 'manual' as Modo }
    ];
    protected readonly modo = signal<Modo>('filtros');
    protected readonly elegidos = signal<readonly OpcionMiembro[]>([]);
    protected readonly buscado = signal<OpcionMiembro | null>(null);
    protected readonly rango = signal<Date[] | null>(null);
    protected readonly vistaPrevia = signal<VistaPreviaLote | null>(null);
    protected readonly cargando = signal(false);
    protected readonly error = signal(false);
    protected readonly verConCaso = signal(false);
    protected readonly iniciales = iniciales;
    protected readonly activos = computed(() => {
        const filtro = this.filtro();
        const activos = new Set<Chip>();
        if (filtro.condicion === 'NUEVO') {
            activos.add('nuevos');
        }
        if (filtro.ingresoDesde === enDias(-DIAS_RECIENTES) && filtro.ingresoHasta === null) {
            activos.add('recientes');
        }
        if (filtro.alerta === 'NUEVO_SIN_VOLVER') {
            activos.add('nuevoSinVolver');
        }
        if (filtro.alerta === 'FALTAS_SEGUIDAS') {
            activos.add('faltasSeguidas');
        }
        if (filtro.sinGrupo) {
            activos.add('sinGrupo');
        }
        if (filtro.nuncaConsolidados) {
            activos.add('nuncaConsolidados');
        }
        return activos;
    });
    protected readonly sinCriterio = computed(() => {
        const filtro = this.filtro();
        return this.modo() === 'manual' ? this.elegidos().length === 0 : JSON.stringify(filtro) === JSON.stringify(FILTRO_LOTE_INICIAL);
    });
    protected readonly filasCarga = [1, 2, 3, 4];
    /** Lista de quienes se agregarían, paginada en el navegador (llega completa). */
    protected readonly primeraFila = signal(0);
    protected readonly filasPorPagina = signal(FILAS_POR_PAGINA);
    protected readonly opcionesFilas = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly personasPagina = computed(() => (this.vistaPrevia()?.personas ?? []).slice(this.primeraFila(), this.primeraFila() + this.filasPorPagina()));
    protected readonly excedeMaximo = computed(() => (this.vistaPrevia()?.entrarian ?? 0) > LIMITES_CONSOLIDACION.loteMax);

    constructor() {
        toObservable(this.filtro)
            .pipe(
                debounceTime(ESPERA_VISTA_PREVIA_MS),
                tap(() => {
                    this.cargando.set(true);
                    this.error.set(false);
                }),
                switchMap((filtro) =>
                    this.rondas.vistaPreviaLote(this.rondaId(), filtro).pipe(
                        catchError(() => {
                            this.error.set(true);
                            return of(null);
                        })
                    )
                ),
                takeUntilDestroyed(inject(DestroyRef))
            )
            .subscribe((vista) => {
                this.cargando.set(false);
                this.vistaPrevia.set(vista);
                this.primeraFila.set(0);
                this.entrarian.set(vista?.entrarian ?? 0);
            });
        effect(() => {
            const rango = this.rango();
            untracked(() => this.filtro.update((filtro) => ({ ...filtro, ingresoDesde: rango?.[0] ? hoyIso(rango[0]) : null, ingresoHasta: rango?.[1] ? hoyIso(rango[1]) : null })));
        });
    }

    protected paginarPersonas({ first, rows }: PaginatorState): void {
        this.primeraFila.set(rows !== this.filasPorPagina() ? 0 : (first ?? 0));
        this.filasPorPagina.set(rows ?? FILAS_POR_PAGINA);
    }

    protected cambiarModo(modo: Modo | null): void {
        if (!modo) {
            return;
        }
        this.modo.set(modo);
        this.rango.set(null);
        this.filtro.set(modo === 'manual' ? { ...FILTRO_LOTE_INICIAL, miembroIds: this.elegidos().map(({ id }) => id) } : FILTRO_LOTE_INICIAL);
    }

    protected alternar(chip: Chip): void {
        const activo = this.activos().has(chip);
        this.filtro.update((filtro) => {
            switch (chip) {
                case 'nuevos':
                    return { ...filtro, condicion: activo ? null : 'NUEVO' };
                case 'recientes':
                    return { ...filtro, ingresoDesde: activo ? null : enDias(-DIAS_RECIENTES), ingresoHasta: null };
                case 'nuevoSinVolver':
                    return { ...filtro, alerta: activo ? null : 'NUEVO_SIN_VOLVER' };
                case 'faltasSeguidas':
                    return { ...filtro, alerta: activo ? null : 'FALTAS_SEGUIDAS' };
                case 'sinGrupo':
                    return { ...filtro, sinGrupo: !activo, grupoId: null };
                case 'nuncaConsolidados':
                    return { ...filtro, nuncaConsolidados: !activo };
            }
        });
        if (chip === 'recientes') {
            this.rango.set(activo ? null : [aFecha(enDias(-DIAS_RECIENTES))!]);
        }
    }

    protected elegirGrupo(grupoId: number | null): void {
        this.filtro.update((filtro) => ({ ...filtro, grupoId, sinGrupo: grupoId === null ? filtro.sinGrupo : false }));
    }

    protected agregarPersona(opcion: OpcionMiembro | null): void {
        if (!opcion || this.elegidos().some(({ id }) => id === opcion.id)) {
            this.buscado.set(null);
            return;
        }
        this.elegidos.update((elegidos) => [...elegidos, opcion]);
        this.buscado.set(null);
        this.actualizarManual();
    }

    protected quitarPersona(id: number): void {
        this.elegidos.update((elegidos) => elegidos.filter((elegido) => elegido.id !== id));
        this.actualizarManual();
    }

    protected reintentar(): void {
        this.filtro.update((filtro) => ({ ...filtro }));
    }

    private actualizarManual(): void {
        this.filtro.set({ ...FILTRO_LOTE_INICIAL, miembroIds: this.elegidos().map(({ id }) => id) });
    }
}
