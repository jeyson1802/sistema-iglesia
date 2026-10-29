import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ProgressBarModule } from 'primeng/progressbar';
import { Miembro } from '../../models/miembro';
import { calcularProgreso } from '../../models/progreso-ficha';

/** Cuánto de la ficha está completo y qué datos faltan (presentacional). */
@Component({
    selector: 'app-progreso-ficha',
    imports: [ProgressBarModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './progreso-ficha.html'
})
export class ProgresoFichaComponent {
    readonly miembro = input.required<Miembro>();
    /** Nombre singular de los grupos en la iglesia, para la lista de faltantes. */
    readonly nombreGrupo = input('Grupo');

    protected readonly progreso = computed(() => calcularProgreso(this.miembro(), this.nombreGrupo()));
}
