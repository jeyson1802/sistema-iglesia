import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';
import { variacion } from '../../models/estadisticas';

/**
 * Tarjeta con una cifra clave (presentacional): etiqueta, valor ya formateado, ayuda corta y, si se dan `actual` y
 * `anterior`, la variación frente al periodo anterior. Se usa como fila de una lista de definiciones:
 * `<dl><div appTarjetaIndicador ...></div></dl>`.
 */
@Component({
    selector: '[appTarjetaIndicador]',
    imports: [TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjeta-indicador.html',
    host: { class: 'flex flex-col gap-1 min-w-0', '[class]': 'marco()' }
})
export class TarjetaIndicadorComponent {
    readonly etiqueta = input.required<string>();
    readonly valor = input.required<string>();
    readonly ayuda = input<string | null>(null);
    readonly icono = input<string | null>(null);
    readonly actual = input<number | null>(null);
    readonly anterior = input<number | null | undefined>(undefined);
    /** `false` si conviene que la cifra baje (p. ej. bajas): invierte el color de la variación. */
    readonly subirEsBueno = input(true);
    /** Tarjeta suelta en la fila de cifras clave (con fondo de tarjeta) o dentro de un bloque (solo borde). */
    readonly destacada = input(false);

    protected readonly marco = computed(() => (this.destacada() ? 'card mb-0!' : 'rounded-border border border-surface p-4'));

    /** `undefined`: la tarjeta no compara; `null`: compara, pero el periodo anterior no tiene datos. */
    protected readonly cambio = computed(() => (this.anterior() === undefined ? undefined : variacion(this.actual(), this.anterior() ?? null, this.subirEsBueno())));
    protected readonly claseCambio = computed(() => {
        const cambio = this.cambio();
        if (!cambio || cambio.favorable === null) {
            return 'text-muted-color';
        }
        return cambio.favorable ? 'text-green-600 dark:text-green-400' : 'text-red-600 dark:text-red-400';
    });
    protected readonly iconoCambio = computed(() => {
        const tendencia = this.cambio()?.tendencia;
        return tendencia === 'sube' ? 'pi pi-arrow-up' : tendencia === 'baja' ? 'pi pi-arrow-down' : 'pi pi-minus';
    });
}
