import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { Marca } from '../../models/asistencia';
import { horaDe } from '../../models/resultado-marca';

/** Últimas personas marcadas desde este equipo, con «Deshacer» por si se escaneó a quien no era (presentacional). */
@Component({
    selector: 'app-ultimas-marcas',
    imports: [ButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './ultimas-marcas.html'
})
export class UltimasMarcasComponent {
    readonly marcas = input.required<readonly Marca[]>();
    /** Miembro cuya marca se está deshaciendo. */
    readonly deshaciendo = input<number | null>(null);

    readonly deshacer = output<Marca>();

    protected readonly hora = horaDe;
}
