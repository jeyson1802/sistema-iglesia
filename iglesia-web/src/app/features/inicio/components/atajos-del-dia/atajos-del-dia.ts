import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Atajo } from '../../models/atajo';

/**
 * Atajos a lo que hay que hacer hoy (presentacional): el primero es la acción principal y se destaca; los demás son
 * discretos. Sin atajos no se muestra nada.
 */
@Component({
    selector: 'app-atajos-del-dia',
    imports: [RouterLink],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './atajos-del-dia.html'
})
export class AtajosDelDiaComponent {
    readonly atajos = input.required<readonly Atajo[]>();
}
