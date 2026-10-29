import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { ChartModule } from 'primeng/chart';
import { LayoutService } from '@/app/layout/service/layout.service';

/** Tono de una serie: sale de los tokens del tema, así respeta el color de la iglesia y el modo oscuro. */
export type TonoSerie = 'primario' | 'exito' | 'peligro' | 'suave';

export interface SerieGrafico {
    readonly etiqueta: string;
    /** Alineados con las etiquetas; `null` es un hueco (sin dato esa semana). */
    readonly valores: readonly (number | null)[];
    readonly tono: TonoSerie;
}

const VARIABLE_TONO: Readonly<Record<TonoSerie, string>> = {
    primario: '--p-primary-500',
    exito: '--p-green-500',
    peligro: '--p-red-400',
    suave: '--p-surface-400'
};

/** Para las líneas con más de cuatro series se alternan tonos de la paleta primaria. */
const TONOS_PRIMARIOS = ['--p-primary-700', '--p-primary-500', '--p-primary-300', '--p-surface-500', '--p-surface-400'];

const MAXIMO_PORCENTAJE = 100;

/**
 * Gráfico de evolución (presentacional) sobre `p-chart`: barras (apiladas o no) o líneas por semana. Toma los colores
 * de los tokens del tema y se vuelve a pintar al cambiar el modo oscuro. Para los lectores de pantalla incluye una
 * tabla con los mismos datos.
 */
@Component({
    selector: 'app-grafico-evolucion',
    imports: [ChartModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './grafico-evolucion.html'
})
export class GraficoEvolucionComponent {
    private readonly layout = inject(LayoutService);

    readonly tipo = input<'bar' | 'line'>('bar');
    readonly etiquetas = input.required<readonly string[]>();
    readonly series = input.required<readonly SerieGrafico[]>();
    /** Barras apiladas (p. ej. asistió / faltó). */
    readonly apilado = input(false);
    /** Eje de 0 a 100 con «%». */
    readonly porcentaje = input(false);
    /** Título accesible del gráfico y de su tabla. */
    readonly descripcion = input.required<string>();
    /** Clase de alto del contenedor (Tailwind). */
    readonly alto = input('h-64');

    protected readonly datos = computed(() => {
        this.layout.layoutConfig();
        const estilo = getComputedStyle(document.documentElement);
        const color = (variable: string) => estilo.getPropertyValue(variable).trim();
        const muchas = this.series().length > 1 && this.tipo() === 'line';
        return {
            labels: [...this.etiquetas()],
            datasets: this.series().map((serie, indice) => {
                const tono = color(muchas ? TONOS_PRIMARIOS[indice % TONOS_PRIMARIOS.length] : VARIABLE_TONO[serie.tono]);
                return {
                    label: serie.etiqueta,
                    data: [...serie.valores],
                    backgroundColor: tono,
                    borderColor: tono,
                    borderWidth: this.tipo() === 'line' ? 2 : 0,
                    borderRadius: this.tipo() === 'bar' ? 4 : 0,
                    tension: 0.3,
                    spanGaps: true,
                    pointRadius: 3
                };
            })
        };
    });

    protected readonly opciones = computed(() => {
        this.layout.layoutConfig();
        const estilo = getComputedStyle(document.documentElement);
        const texto = estilo.getPropertyValue('--text-color').trim();
        const textoSecundario = estilo.getPropertyValue('--text-color-secondary').trim();
        const borde = estilo.getPropertyValue('--surface-border').trim();
        const porcentaje = this.porcentaje();
        return {
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { display: this.series().length > 1, labels: { color: texto, usePointStyle: true } },
                tooltip: { callbacks: porcentaje ? { label: (contexto: { dataset: { label: string }; formattedValue: string }) => `${contexto.dataset.label}: ${contexto.formattedValue} %` } : {} }
            },
            scales: {
                x: { stacked: this.apilado(), ticks: { color: textoSecundario }, grid: { display: false } },
                y: {
                    stacked: this.apilado(),
                    beginAtZero: true,
                    max: porcentaje ? MAXIMO_PORCENTAJE : undefined,
                    ticks: { color: textoSecundario, precision: 0, callback: (valor: number | string) => (porcentaje ? `${valor} %` : valor) },
                    grid: { color: borde }
                }
            }
        };
    });

    protected valor(valor: number | null): string {
        return valor === null ? '—' : this.porcentaje() ? `${valor} %` : `${valor}`;
    }
}
