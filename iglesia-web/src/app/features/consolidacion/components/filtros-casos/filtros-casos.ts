import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { Referencia } from '@/app/core/models/referencia';
import { EstadoCaso, FiltroCasosRonda, Ronda } from '../../models/consolidacion';

/** Espera tras la última tecla antes de buscar. */
const ESPERA_BUSQUEDA_MS = 300;

type Chip = 'TODOS' | EstadoCaso | 'SIN_ASIGNAR';

/**
 * Filtros de las personas de una ronda (presentacional): chips por estado con su cantidad («Pendientes 12»),
 * consolidador y búsqueda por nombre. La búsqueda se aplica al dejar de escribir.
 */
@Component({
    selector: 'app-filtros-casos',
    imports: [FormsModule, IconFieldModule, InputIconModule, InputTextModule, SelectModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-casos.html'
})
export class FiltrosCasosComponent {
    readonly filtro = input.required<FiltroCasosRonda>();
    readonly ronda = input.required<Ronda>();
    readonly consolidadores = input<readonly Referencia[]>([]);
    protected readonly opcionesConsolidadores = computed(() => [...this.consolidadores()]);

    readonly filtrar = output<FiltroCasosRonda>();

    protected readonly texto = signal('');
    protected readonly chips = computed(() => {
        const { personas, pendientes, enContacto, cerrados, sinAsignar } = this.ronda();
        const chips: { valor: Chip; etiqueta: string; cantidad: number }[] = [
            { valor: 'TODOS', etiqueta: 'Todos', cantidad: personas },
            { valor: 'PENDIENTE', etiqueta: 'Pendientes', cantidad: pendientes },
            { valor: 'EN_CONTACTO', etiqueta: 'En contacto', cantidad: enContacto },
            { valor: 'CERRADO', etiqueta: 'Cerrados', cantidad: cerrados }
        ];
        return sinAsignar > 0 ? [...chips, { valor: 'SIN_ASIGNAR' as const, etiqueta: 'Sin asignar', cantidad: sinAsignar }] : chips;
    });
    protected readonly chipActivo = computed<Chip>(() => (this.filtro().sinAsignar ? 'SIN_ASIGNAR' : (this.filtro().estado ?? 'TODOS')));

    private espera?: ReturnType<typeof setTimeout>;

    constructor() {
        inject(DestroyRef).onDestroy(() => clearTimeout(this.espera));
    }

    protected elegirChip(chip: Chip): void {
        const sinAsignar = chip === 'SIN_ASIGNAR';
        this.filtrar.emit({ ...this.filtro(), estado: chip === 'TODOS' || sinAsignar ? null : chip, sinAsignar, consolidadorId: sinAsignar ? null : this.filtro().consolidadorId });
    }

    protected elegirConsolidador(consolidadorId: number | null): void {
        this.filtrar.emit({ ...this.filtro(), consolidadorId, sinAsignar: consolidadorId === null ? this.filtro().sinAsignar : false });
    }

    protected escribir(texto: string): void {
        this.texto.set(texto);
        clearTimeout(this.espera);
        this.espera = setTimeout(() => this.filtrar.emit({ ...this.filtro(), q: texto }), ESPERA_BUSQUEDA_MS);
    }
}
