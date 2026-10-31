import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { SesionService } from '@/app/core/auth/services/sesion.service';
import { ConsultaPeriodo, formatoNumero } from '../../models/estadisticas';
import { EstadisticasService } from '../../services/estadisticas.service';
import { cargaReactiva } from '../../utils/carga-reactiva';
import { BloqueSistemaComponent } from '../bloque-sistema/bloque-sistema';
import { BloqueUsoComponent } from '../bloque-uso/bloque-uso';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Panorama del sistema (contenedor), en la vista global del administrador del sistema: cifras clave, crecimiento de
 * iglesias y miembros y uso del sistema de todas las iglesias. Solo agregados; nunca aportes ni datos de personas.
 */
@Component({
    selector: 'app-panorama-sistema',
    imports: [SkeletonModule, TarjetaIndicadorComponent, BloqueSistemaComponent, BloqueUsoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './panorama-sistema.html'
})
export class PanoramaSistemaComponent {
    private readonly sesion = inject(SesionService);
    private readonly servicio = inject(EstadisticasService);

    readonly periodo = input.required<ConsultaPeriodo>();

    protected readonly numero = formatoNumero;
    protected readonly puedeVerSistema = computed(() => this.sesion.tienePermiso('estadisticas.sistema'));
    protected readonly sistema = cargaReactiva(this.periodo, (periodo) => this.servicio.sistema(periodo), this.puedeVerSistema);
    protected readonly uso = cargaReactiva(this.periodo, (periodo) => this.servicio.uso(periodo), this.puedeVerSistema);
}
