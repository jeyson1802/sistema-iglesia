import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { DESCRIPCION_MEDIO } from '@/app/features/aportes/models/aporte';
import { GraficoDistribucionComponent, ParteDistribucion } from '@/app/shared/components/grafico-distribucion/grafico-distribucion';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { formatearMonto } from '@/app/shared/pipes/moneda.pipe';
import { EstadisticasAportes, etiquetaEje, etiquetaPunto, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Aportes» (presentacional): total del periodo frente al anterior, cantidad de aportes, la serie de totales
 * y el desglose por categoría y por medio. Solo lo crea quien tiene `aportes.ver`.
 */
@Component({
    selector: 'app-bloque-aportes',
    imports: [BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoEvolucionComponent, GraficoDistribucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-aportes.html'
})
export class BloqueAportesComponent {
    readonly datos = input<EstadisticasAportes | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly eje = etiquetaEje;
    protected readonly monto = computed(() => {
        const moneda = this.datos()?.resumen.moneda ?? 'PEN';
        return (valor: number) => formatearMonto(valor, moneda);
    });
    protected readonly etiquetas = computed(() => {
        const datos = this.datos();
        return datos ? datos.serie.map(({ inicio }) => etiquetaPunto(inicio, datos.periodo.granularidad)) : [];
    });
    protected readonly series = computed<readonly SerieGrafico[]>(() => [{ etiqueta: 'Total', valores: (this.datos()?.serie ?? []).map(({ total }) => total), tono: 'primario' }]);
    protected readonly huboAportes = computed(() => (this.datos()?.resumen.cantidad ?? 0) > 0);
    protected readonly porCategoria = computed<readonly ParteDistribucion[]>(() => (this.datos()?.resumen.porCategoria ?? []).map(({ categoria, total }) => ({ clave: String(categoria.id), etiqueta: categoria.nombre, valor: total })));
    protected readonly porMedio = computed<readonly ParteDistribucion[]>(() => (this.datos()?.resumen.porMedio ?? []).map(({ medio, total }) => ({ clave: medio, etiqueta: DESCRIPCION_MEDIO[medio].etiqueta, valor: total })));
}
