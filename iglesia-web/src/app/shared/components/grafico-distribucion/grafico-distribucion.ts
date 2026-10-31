import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ProgressBarModule } from 'primeng/progressbar';

/** Una categoría de la distribución. */
export interface ParteDistribucion {
    readonly clave: string;
    readonly etiqueta: string;
    readonly valor: number;
}

/**
 * Distribución en barras horizontales (presentacional): cada categoría con su cantidad y su parte del total. Es una
 * lista legible por lectores de pantalla, con la descripción como título; respeta el tema (tokens de Sakai).
 */
@Component({
    selector: 'app-grafico-distribucion',
    imports: [ProgressBarModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './grafico-distribucion.html'
})
export class GraficoDistribucionComponent {
    readonly partes = input.required<readonly ParteDistribucion[]>();
    /** Título visible y accesible de la distribución. */
    readonly descripcion = input.required<string>();
    /** Cómo mostrar cada valor (p. ej. como monto); por defecto, el número con separador de miles. */
    readonly formato = input<(valor: number) => string>((valor) => valor.toLocaleString('es-PE'));

    protected readonly total = computed(() => this.partes().reduce((suma, { valor }) => suma + valor, 0));
    protected readonly filas = computed(() => {
        const total = this.total();
        return this.partes().map((parte) => ({ ...parte, porcentaje: total === 0 ? 0 : Math.round((parte.valor / total) * 100) }));
    });
}
