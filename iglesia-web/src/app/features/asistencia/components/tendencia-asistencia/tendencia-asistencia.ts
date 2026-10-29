import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { DESCRIPCION_TENDENCIA, tendencia } from '../../models/seguimiento';

/** Tendencia del porcentaje frente al período anterior (presentacional): «Subiendo», «Bajando» o «Estable». */
@Component({
    selector: 'app-tendencia-asistencia',
    imports: [TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tendencia-asistencia.html'
})
export class TendenciaAsistenciaComponent {
    readonly actual = input.required<number | null>();
    readonly anterior = input.required<number | null>();

    protected readonly descripcion = computed(() => {
        const valor = tendencia(this.actual(), this.anterior());
        return valor === null ? null : DESCRIPCION_TENDENCIA[valor];
    });
}
