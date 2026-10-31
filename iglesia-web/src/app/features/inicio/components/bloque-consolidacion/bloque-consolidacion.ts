import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { GraficoDistribucionComponent } from '@/app/shared/components/grafico-distribucion/grafico-distribucion';
import { EstadisticasConsolidacion, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Consolidación» (presentacional): rondas en curso, casos abiertos y cerrados, contactos y personas
 * contactadas que volvieron a asistir; contactos por medio y cierres por resultado.
 */
@Component({
    selector: 'app-bloque-consolidacion',
    imports: [BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoDistribucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-consolidacion.html'
})
export class BloqueConsolidacionComponent {
    readonly datos = input<EstadisticasConsolidacion | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
}
