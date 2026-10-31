import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ETIQUETA_ROL } from '@/app/core/auth/models/rol';
import { GraficoDistribucionComponent, ParteDistribucion } from '@/app/shared/components/grafico-distribucion/grafico-distribucion';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { EstadisticasUso, etiquetaEje, etiquetaPunto, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Uso del sistema» (presentacional): usuarios, activos del periodo, diarios, semanales y mensuales; en la
 * vista global, también las iglesias con actividad. Debajo, la serie de activos y los activos por perfil.
 */
@Component({
    selector: 'app-bloque-uso',
    imports: [BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoEvolucionComponent, GraficoDistribucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-uso.html'
})
export class BloqueUsoComponent {
    readonly datos = input<EstadisticasUso | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly ruta = input<string | null>('/usuarios');
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly eje = etiquetaEje;
    protected readonly etiquetas = computed(() => {
        const datos = this.datos();
        return datos ? datos.serie.map(({ inicio }) => etiquetaPunto(inicio, datos.periodo.granularidad)) : [];
    });
    protected readonly series = computed<readonly SerieGrafico[]>(() => [{ etiqueta: 'Usuarios activos', valores: (this.datos()?.serie ?? []).map(({ valor }) => valor), tono: 'primario' }]);
    protected readonly huboActividad = computed(() => (this.datos()?.activosPeriodo ?? 0) > 0);
    protected readonly porRol = computed<readonly ParteDistribucion[]>(() => (this.datos()?.porRolBase ?? []).map(({ clave, valor }) => ({ clave, etiqueta: ETIQUETA_ROL[clave], valor })));
}
