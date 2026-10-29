import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { ETIQUETA_TIPO_REUNION, horaCorta, ReunionHoy, SEVERIDAD_TIPO_REUNION } from '../../models/asistencia';

/** Reunión de hoy como una tarjeta grande (presentacional): toda la tarjeta es el botón «Tomar asistencia». */
@Component({
    selector: 'app-tarjeta-reunion-hoy',
    imports: [ProgressSpinnerModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjeta-reunion-hoy.html'
})
export class TarjetaReunionHoyComponent {
    readonly reunion = input.required<ReunionHoy>();
    /** Mientras se abre la reunión (la primera vez del día). */
    readonly abriendo = input(false);

    readonly tomar = output<ReunionHoy>();

    protected readonly tipo = computed(() => ETIQUETA_TIPO_REUNION[this.reunion().tipo]);
    protected readonly severidad = computed(() => SEVERIDAD_TIPO_REUNION[this.reunion().tipo]);
    protected readonly hora = computed(() => horaCorta(this.reunion().hora));
    protected readonly abierta = computed(() => this.reunion().id !== null);
    protected readonly etiquetaAccesible = computed(() => {
        const reunion = this.reunion();
        const presentes = this.abierta() ? `, ${reunion.totalAsistentes} presentes` : '';
        return `Tomar asistencia: ${reunion.nombre}${this.hora() ? ' a las ' + this.hora() : ''}${presentes}`;
    });
}
