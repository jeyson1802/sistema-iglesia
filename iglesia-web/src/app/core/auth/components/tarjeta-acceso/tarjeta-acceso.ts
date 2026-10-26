import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AppFloatingConfigurator } from '@/app/layout/component/app.floatingconfigurator';

export type TonoTarjetaAcceso = 'primario' | 'advertencia';

/** Clases completas (no interpoladas) para que Tailwind las genere. */
const DEGRADADO_POR_TONO: Record<TonoTarjetaAcceso, string> = {
    primario: 'bg-[linear-gradient(180deg,var(--primary-color)_10%,rgba(33,150,243,0)_30%)]',
    advertencia: 'bg-[linear-gradient(180deg,rgba(247,149,48,0.4)_10%,rgba(247,149,48,0)_30%)]'
};

/** Marco visual de las pantallas de acceso de Sakai (fondo, borde con degradado y tarjeta central). */
@Component({
    selector: 'app-tarjeta-acceso',
    imports: [AppFloatingConfigurator],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tarjeta-acceso.html'
})
export class TarjetaAccesoComponent {
    readonly tono = input<TonoTarjetaAcceso>('primario');

    protected readonly degradado = computed(() => DEGRADADO_POR_TONO[this.tono()]);
}
