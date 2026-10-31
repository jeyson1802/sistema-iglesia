import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { GraficoDistribucionComponent } from '@/app/shared/components/grafico-distribucion/grafico-distribucion';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { EstadisticasMiembros, etiquetaEje, etiquetaPunto, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Miembros» (presentacional): activos, altas y bajas frente al periodo anterior, sin grupo y sin líder (o
 * directos, en la red), la serie de altas y bajas y las distribuciones por condición, nivel, género y edad.
 */
@Component({
    selector: 'app-bloque-miembros',
    imports: [BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoEvolucionComponent, GraficoDistribucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-miembros.html'
})
export class BloqueMiembrosComponent {
    readonly datos = input<EstadisticasMiembros | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly titulo = input('Miembros');
    readonly ruta = input<string | null>('/miembros');
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly eje = etiquetaEje;
    protected readonly etiquetas = computed(() => {
        const datos = this.datos();
        return datos ? datos.serie.map(({ inicio }) => etiquetaPunto(inicio, datos.periodo.granularidad)) : [];
    });
    protected readonly series = computed<readonly SerieGrafico[]>(() => {
        const serie = this.datos()?.serie ?? [];
        return [
            { etiqueta: 'Altas', valores: serie.map(({ altas }) => altas), tono: 'exito' },
            { etiqueta: 'Bajas', valores: serie.map(({ bajas }) => bajas), tono: 'peligro' }
        ];
    });
    protected readonly huboMovimiento = computed(() => (this.datos()?.serie ?? []).some(({ altas, bajas }) => altas + bajas > 0));
}
