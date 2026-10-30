import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { plazoRonda, Ronda } from '../../models/consolidacion';
import { MedidorRondaComponent } from '../medidor-ronda/medidor-ronda';

/**
 * Ronda en curso en una tarjeta (presentacional): nombre, días que faltan, medidor de avance y avisos accionables
 * («5 con contacto vencido», «3 sin asignar»). Un toque abre el detalle.
 */
@Component({
    selector: 'app-tarjeta-ronda',
    imports: [TagModule, MedidorRondaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjeta-ronda.html'
})
export class TarjetaRondaComponent {
    readonly ronda = input.required<Ronda>();

    readonly ver = output<Ronda>();

    protected readonly plazo = computed(() => plazoRonda(this.ronda().fechaFin));
    protected readonly vencida = computed(() => this.plazo()?.startsWith('venció') ?? false);
}
