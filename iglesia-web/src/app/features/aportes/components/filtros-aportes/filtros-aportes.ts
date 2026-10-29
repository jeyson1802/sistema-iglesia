import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ChipModule } from 'primeng/chip';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { OpcionMiembro } from '@/app/shared/components/selector-miembro/opciones-miembro.service';
import { SelectorMiembroComponent } from '@/app/shared/components/selector-miembro/selector-miembro';
import { hoyIso } from '@/app/shared/utils/fecha';
import { DESCRIPCION_MEDIO, FiltroAportes, LIMITES_APORTE, MEDIOS_APORTE, MedioAporte } from '../../models/aporte';
import { CategoriaAporte } from '../../models/configuracion-aportes';
import { aFecha, diasDe, ETIQUETA_PERIODO, Periodo, periodoDe, PERIODOS, rangoDe } from '../../models/periodo';

/** Filtro activo que se muestra como chip y se puede quitar. */
interface ChipFiltro {
    readonly clave: 'categoriaId' | 'medio' | 'miembroId' | 'incluirAnulados';
    readonly etiqueta: string;
}

/**
 * Filtros del listado de aportes (presentacional): atajos de período, rango personalizado, categoría, medio,
 * aportante (solo para quien ve toda la iglesia) e «Incluir anulados». Cada cambio emite el filtro completo; los
 * filtros activos se muestran como chips.
 */
@Component({
    selector: 'app-filtros-aportes',
    imports: [FormsModule, ButtonModule, CheckboxModule, ChipModule, DatePickerModule, SelectModule, SelectButtonModule, SelectorMiembroComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-aportes.html'
})
export class FiltrosAportesComponent {
    readonly filtro = input.required<FiltroAportes>();
    readonly categorias = input<readonly CategoriaAporte[]>([]);
    /** Un líder solo ve sus aportes: no tiene sentido filtrar por aportante. */
    readonly mostrarMiembro = input(true);
    /** Nombre del aportante filtrado, si se conoce (para el chip). */
    readonly nombreMiembro = input<string | null>(null);

    readonly cambiar = output<FiltroAportes>();

    protected readonly periodos = PERIODOS.map((valor) => ({ valor, etiqueta: ETIQUETA_PERIODO[valor] }));
    protected readonly medios = MEDIOS_APORTE.map((valor) => ({ valor, etiqueta: DESCRIPCION_MEDIO[valor].etiqueta, icono: DESCRIPCION_MEDIO[valor].icono }));
    protected readonly opcionesCategoria = computed(() => this.categorias().map(({ id, nombre, activo }) => ({ id, etiqueta: activo ? nombre : `${nombre} (inactiva)` })));
    protected readonly hoy = aFecha(hoyIso());

    /** «Personalizado» se elige a mano y se mantiene aunque el rango coincida con un atajo. */
    private readonly personalizado = signal(false);
    protected readonly periodo = computed<Periodo>(() => (this.personalizado() ? 'PERSONALIZADO' : periodoDe(this.filtro())));
    protected readonly rango = signal<Date[] | null>(null);
    protected readonly errorRango = signal<string | null>(null);
    protected readonly miembro = signal<OpcionMiembro | null>(null);

    protected readonly chips = computed<ChipFiltro[]>(() => {
        const { categoriaId, medio, miembroId, incluirAnulados } = this.filtro();
        const chips: ChipFiltro[] = [];
        if (categoriaId !== null) {
            chips.push({ clave: 'categoriaId', etiqueta: `Categoría: ${this.categorias().find(({ id }) => id === categoriaId)?.nombre ?? 'seleccionada'}` });
        }
        if (medio !== null) {
            chips.push({ clave: 'medio', etiqueta: `Medio: ${DESCRIPCION_MEDIO[medio].etiqueta}` });
        }
        if (miembroId !== null && this.mostrarMiembro()) {
            chips.push({ clave: 'miembroId', etiqueta: `Aportante: ${this.nombreMiembro() ?? this.miembro()?.nombre ?? 'seleccionado'}` });
        }
        if (incluirAnulados) {
            chips.push({ clave: 'incluirAnulados', etiqueta: 'Con anulados' });
        }
        return chips;
    });

    constructor() {
        effect(() => {
            const { desde, hasta, miembroId } = this.filtro();
            this.rango.set([aFecha(desde), aFecha(hasta)]);
            if (miembroId === null) {
                this.miembro.set(null);
            } else if (this.miembro()?.id !== miembroId) {
                this.miembro.set({ id: miembroId, nombre: this.nombreMiembro() ?? 'Aportante seleccionado' });
            }
        });
    }

    protected elegirPeriodo(periodo: Periodo | null): void {
        if (!periodo) {
            return;
        }
        this.personalizado.set(periodo === 'PERSONALIZADO');
        this.errorRango.set(null);
        if (periodo !== 'PERSONALIZADO') {
            this.emitir(rangoDe(periodo));
        }
    }

    /** El rango se aplica cuando tiene sus dos extremos y no supera el máximo que acepta la API. */
    protected elegirRango(fechas: Date[] | null): void {
        this.rango.set(fechas);
        const [desde, hasta] = fechas ?? [];
        if (!desde || !hasta) {
            return;
        }
        const rango = { desde: hoyIso(desde), hasta: hoyIso(hasta) };
        if (diasDe(rango) > LIMITES_APORTE.rangoMaximoDias) {
            this.errorRango.set(`Elige un rango de hasta ${LIMITES_APORTE.rangoMaximoDias} días`);
            return;
        }
        this.errorRango.set(null);
        this.emitir(rango);
    }

    protected elegirCategoria(categoriaId: number | null): void {
        this.emitir({ categoriaId });
    }

    protected elegirMedio(medio: MedioAporte | null): void {
        this.emitir({ medio });
    }

    protected elegirMiembro(miembro: OpcionMiembro | null): void {
        this.miembro.set(miembro);
        this.emitir({ miembroId: miembro?.id ?? null });
    }

    protected elegirAnulados(incluirAnulados: boolean): void {
        this.emitir({ incluirAnulados });
    }

    protected quitar(chip: ChipFiltro): void {
        this.emitir({ [chip.clave]: chip.clave === 'incluirAnulados' ? false : null });
    }

    protected limpiar(): void {
        this.personalizado.set(false);
        this.errorRango.set(null);
        this.cambiar.emit({ ...rangoDe('MES'), categoriaId: null, medio: null, miembroId: null, incluirAnulados: false });
    }

    private emitir(cambios: Partial<FiltroAportes>): void {
        this.cambiar.emit({ ...this.filtro(), ...cambios });
    }
}
