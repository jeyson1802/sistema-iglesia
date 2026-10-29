import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePickerModule } from 'primeng/datepicker';
import { SelectModule } from 'primeng/select';
import { SelectButtonModule } from 'primeng/selectbutton';
import { hoyIso } from '@/app/shared/utils/fecha';
import { aFecha, ETIQUETA_PERIODO, Periodo, periodoDe, rangoDe } from '@/app/shared/utils/periodo';
import { ETIQUETA_TIPO_REUNION, FiltroReuniones, TIPOS_REUNION, TipoReunion } from '../../models/asistencia';

/** Atajos que se ofrecen en el historial de asistencia. */
const PERIODOS_HISTORIAL: readonly Periodo[] = ['SEMANA', 'MES', 'MES_ANTERIOR', 'PERSONALIZADO'];

/** Filtros del historial de reuniones (presentacional): atajos de período, rango personalizado y tipo. */
@Component({
    selector: 'app-filtros-reuniones',
    imports: [FormsModule, DatePickerModule, SelectModule, SelectButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './filtros-reuniones.html'
})
export class FiltrosReunionesComponent {
    readonly filtro = input.required<FiltroReuniones>();

    readonly cambiar = output<FiltroReuniones>();

    protected readonly hoy = new Date();
    protected readonly periodos = PERIODOS_HISTORIAL.map((valor) => ({ valor, etiqueta: ETIQUETA_PERIODO[valor] }));
    protected readonly tipos = TIPOS_REUNION.map((valor) => ({ valor, etiqueta: ETIQUETA_TIPO_REUNION[valor] }));
    protected readonly periodo = computed(() => {
        const actual = periodoDe(this.filtro());
        return PERIODOS_HISTORIAL.includes(actual) ? actual : 'PERSONALIZADO';
    });
    protected readonly rango = computed(() => [aFecha(this.filtro().desde), aFecha(this.filtro().hasta)]);

    protected elegirPeriodo(periodo: Periodo): void {
        if (periodo !== 'PERSONALIZADO') {
            this.cambiar.emit({ ...this.filtro(), ...rangoDe(periodo) });
        }
    }

    /** El rango solo se aplica cuando tiene las dos fechas. */
    protected elegirRango(rango: (Date | null)[] | null): void {
        const [desde, hasta] = rango ?? [];
        if (desde && hasta) {
            this.cambiar.emit({ ...this.filtro(), desde: hoyIso(desde), hasta: hoyIso(hasta) });
        }
    }

    protected elegirTipo(tipo: TipoReunion | null): void {
        this.cambiar.emit({ ...this.filtro(), tipo });
    }
}
