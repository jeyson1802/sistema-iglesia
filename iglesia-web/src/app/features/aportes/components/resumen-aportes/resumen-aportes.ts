import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { SkeletonModule } from 'primeng/skeleton';
import { LayoutService } from '@/app/layout/service/layout.service';
import { formatearMonto, MonedaPipe } from '@/app/shared/pipes/moneda.pipe';
import { DESCRIPCION_MEDIO, MedioAporte, ResumenAportes } from '../../models/aporte';

/** Colores de los gráficos: tokens del tema, para que respeten el preset y el modo oscuro. */
const TOKENS_SERIE = ['--p-primary-500', '--p-cyan-500', '--p-orange-500', '--p-purple-500', '--p-teal-500', '--p-pink-500', '--p-indigo-500', '--p-yellow-500'];

/** Espera a que el tema aplique sus variables antes de leer los colores (igual que los widgets del dashboard). */
const ESPERA_TEMA_MS = 150;

interface ColoresTema {
    readonly serie: readonly string[];
    readonly texto: string;
    readonly textoSecundario: string;
    readonly borde: string;
}

/** Variación del total frente al período anterior. */
interface Variacion {
    readonly sentido: 'sube' | 'baja' | 'igual';
    readonly porcentaje: number | null;
}

/**
 * Resumen del período (presentacional): tarjetas al estilo del dashboard de Sakai (total con su variación, cantidad
 * y promedio) y gráficos por categoría y por medio. Un clic en una porción o barra pide filtrar por ella.
 */
@Component({
    selector: 'app-resumen-aportes',
    imports: [ChartModule, SkeletonModule, MonedaPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './resumen-aportes.html'
})
export class ResumenAportesComponent {
    private readonly layout = inject(LayoutService);

    readonly resumen = input<ResumenAportes | null>(null);
    readonly cargando = input(false);

    readonly filtrarCategoria = output<number>();
    readonly filtrarMedio = output<MedioAporte>();

    private readonly colores = signal<ColoresTema>(leerColores());

    protected readonly variacion = computed<Variacion | null>(() => {
        const resumen = this.resumen();
        if (!resumen) {
            return null;
        }
        const { total, totalPeriodoAnterior: anterior } = resumen;
        const sentido = total > anterior ? 'sube' : total < anterior ? 'baja' : 'igual';
        return { sentido, porcentaje: anterior > 0 ? Math.round(((total - anterior) / anterior) * 100) : null };
    });
    protected readonly promedio = computed(() => {
        const resumen = this.resumen();
        return resumen && resumen.cantidad > 0 ? resumen.total / resumen.cantidad : 0;
    });
    protected readonly hayDatos = computed(() => (this.resumen()?.cantidad ?? 0) > 0);

    protected readonly datosCategorias = computed(() => {
        const porCategoria = this.resumen()?.porCategoria ?? [];
        return {
            labels: porCategoria.map(({ categoria }) => categoria.nombre),
            datasets: [{ data: porCategoria.map(({ total }) => total), backgroundColor: porCategoria.map((_, indice) => this.colorSerie(indice)), borderWidth: 0 }]
        };
    });
    protected readonly datosMedios = computed(() => {
        const porMedio = this.resumen()?.porMedio ?? [];
        return {
            labels: porMedio.map(({ medio }) => DESCRIPCION_MEDIO[medio].etiqueta),
            datasets: [{ label: 'Total', data: porMedio.map(({ total }) => total), backgroundColor: this.colorSerie(0), borderRadius: 6, barThickness: 28 }]
        };
    });
    protected readonly opcionesAnillo = computed(() => ({
        cutout: '65%',
        maintainAspectRatio: false,
        plugins: {
            legend: { position: 'bottom', labels: { color: this.colores().texto, usePointStyle: true } },
            tooltip: { callbacks: { label: (contexto: { label: string; raw: number }) => `${contexto.label}: ${this.formatear(contexto.raw)}` } }
        }
    }));
    protected readonly opcionesBarras = computed(() => ({
        maintainAspectRatio: false,
        plugins: { legend: { display: false }, tooltip: { callbacks: { label: (contexto: { raw: number }) => this.formatear(contexto.raw) } } },
        scales: {
            x: { ticks: { color: this.colores().textoSecundario }, grid: { display: false } },
            y: { ticks: { color: this.colores().textoSecundario, callback: (valor: number) => this.formatear(valor) }, grid: { color: this.colores().borde } }
        }
    }));

    constructor() {
        effect((alLimpiar) => {
            this.layout.layoutConfig();
            const espera = setTimeout(() => this.colores.set(leerColores()), ESPERA_TEMA_MS);
            alLimpiar(() => clearTimeout(espera));
        });
    }

    protected elegirCategoria(evento: { element?: { index?: number } }): void {
        const total = this.resumen()?.porCategoria[evento.element?.index ?? -1];
        if (total) {
            this.filtrarCategoria.emit(total.categoria.id);
        }
    }

    protected elegirMedio(evento: { element?: { index?: number } }): void {
        const total = this.resumen()?.porMedio[evento.element?.index ?? -1];
        if (total) {
            this.filtrarMedio.emit(total.medio);
        }
    }

    protected etiquetaMedio(medio: MedioAporte): string {
        return DESCRIPCION_MEDIO[medio].etiqueta;
    }

    private colorSerie(indice: number): string {
        const serie = this.colores().serie;
        return serie[indice % serie.length];
    }

    private formatear(monto: number): string {
        return formatearMonto(monto, this.resumen()?.moneda ?? 'PEN');
    }
}

function leerColores(): ColoresTema {
    const estilo = getComputedStyle(document.documentElement);
    const variable = (nombre: string): string => estilo.getPropertyValue(nombre).trim();
    return { serie: TOKENS_SERIE.map(variable), texto: variable('--text-color'), textoSecundario: variable('--text-color-secondary'), borde: variable('--surface-border') };
}
