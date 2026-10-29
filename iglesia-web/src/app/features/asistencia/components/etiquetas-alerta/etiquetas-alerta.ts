import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DESCRIPCION_ALERTA, TipoAlerta } from '../../models/seguimiento';

/** Alertas de seguimiento de una persona como etiquetas (presentacional); sin alertas, opcionalmente «Al día». */
@Component({
    selector: 'app-etiquetas-alerta',
    imports: [TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './etiquetas-alerta.html'
})
export class EtiquetasAlertaComponent {
    readonly alertas = input.required<readonly TipoAlerta[]>();
    /** Para escribir «4 faltas seguidas» en lugar de «Faltas seguidas». */
    readonly faltasSeguidas = input(0);
    readonly mostrarAlDia = input(false);

    protected readonly descripcion = DESCRIPCION_ALERTA;

    protected etiqueta(alerta: TipoAlerta): string {
        return alerta === 'FALTAS_SEGUIDAS' && this.faltasSeguidas() > 0 ? `${this.faltasSeguidas()} faltas seguidas` : DESCRIPCION_ALERTA[alerta].etiqueta;
    }
}
