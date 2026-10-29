import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TooltipModule } from 'primeng/tooltip';
import { SemanaAsistencia, semanaCorta } from '../../models/seguimiento';

/** Porcentajes a partir de los cuales la semana se pinta como completa o parcial. */
const COMPLETA = 100;
const MAYORIA = 50;

/**
 * Las últimas semanas de un miembro en una franja de casillas (presentacional): llena si asistió a todo, a medias si
 * a parte, vacía si faltó a todo y punteada si esa semana no le correspondía nada.
 */
@Component({
    selector: 'app-semanas-asistencia',
    imports: [TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './semanas-asistencia.html'
})
export class SemanasAsistenciaComponent {
    readonly semanas = input.required<readonly SemanaAsistencia[]>();

    protected clase({ porcentaje }: SemanaAsistencia): string {
        if (porcentaje === null) {
            return 'border border-dashed border-surface-300 dark:border-surface-600';
        }
        if (porcentaje >= COMPLETA) {
            return 'bg-primary';
        }
        if (porcentaje >= MAYORIA) {
            return 'bg-primary/60';
        }
        return porcentaje > 0 ? 'bg-primary/25' : 'bg-surface-200 dark:bg-surface-700';
    }

    protected etiqueta(inicio: string): string {
        return semanaCorta(inicio);
    }

    protected texto(semana: SemanaAsistencia): string {
        const cuando = `Semana del ${semanaCorta(semana.inicio)}`;
        return semana.esperadas === 0 ? `${cuando}: sin reuniones` : `${cuando}: ${semana.asistidas} de ${semana.esperadas}`;
    }
}
