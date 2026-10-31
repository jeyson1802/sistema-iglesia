import { ChangeDetectionStrategy, Component, computed, linkedSignal, model, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectButtonModule } from 'primeng/selectbutton';
import { hoyIso } from '@/app/shared/utils/fecha';
import { aFecha, diasDe, ETIQUETA_PERIODO, Periodo, PERIODOS } from '@/app/shared/utils/periodo';
import { ConsultaPeriodo, DIAS_MAXIMOS_PERSONALIZADO } from '../../models/estadisticas';

/**
 * Selector del periodo de las estadísticas de inicio (presentacional), igual que el de aportes: atajos (hoy, semana,
 * mes, mes anterior, año) como botones segmentados y, con «Personalizado», un rango de fechas. El rango se aplica
 * cuando tiene sus dos extremos y no supera el máximo que acepta la API.
 */
@Component({
    selector: 'app-selector-periodo',
    imports: [FormsModule, DatePickerModule, SelectButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-periodo.html'
})
export class SelectorPeriodoComponent {
    readonly periodo = model.required<ConsultaPeriodo>();

    protected readonly opciones = PERIODOS.map((valor) => ({ valor, etiqueta: ETIQUETA_PERIODO[valor] }));
    protected readonly hoy = aFecha(hoyIso());

    /** «Personalizado» se elige a mano y se muestra aunque aún no tenga rango (las cifras siguen con el periodo anterior). */
    private readonly personalizado = signal(false);
    protected readonly elegido = computed<Periodo>(() => (this.personalizado() ? 'PERSONALIZADO' : this.periodo().periodo));
    protected readonly rango = linkedSignal<Date[] | null>(() => {
        const { periodo, desde, hasta } = this.periodo();
        return periodo === 'PERSONALIZADO' && desde && hasta ? [aFecha(desde), aFecha(hasta)] : null;
    });
    protected readonly errorRango = signal<string | null>(null);

    protected elegirPeriodo(periodo: Periodo | null): void {
        if (!periodo) {
            return;
        }
        this.personalizado.set(periodo === 'PERSONALIZADO');
        this.errorRango.set(null);
        if (periodo !== 'PERSONALIZADO') {
            this.periodo.set({ periodo });
        }
    }

    protected elegirRango(fechas: Date[] | null): void {
        this.rango.set(fechas);
        const [desde, hasta] = fechas ?? [];
        if (!desde || !hasta) {
            return;
        }
        const rango = { desde: hoyIso(desde), hasta: hoyIso(hasta) };
        if (diasDe(rango) > DIAS_MAXIMOS_PERSONALIZADO) {
            this.errorRango.set(`Elige un rango de hasta ${DIAS_MAXIMOS_PERSONALIZADO} días`);
            return;
        }
        this.errorRango.set(null);
        this.periodo.set({ periodo: 'PERSONALIZADO', ...rango });
    }
}
