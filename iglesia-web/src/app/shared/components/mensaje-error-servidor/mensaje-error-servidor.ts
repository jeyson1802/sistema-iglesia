import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MessageModule } from 'primeng/message';

/** Mensaje del backend para un campo (`errores` del ProblemDetail), con el mismo aspecto que los de validación. */
@Component({
    selector: 'app-mensaje-error-servidor',
    imports: [MessageModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mensaje-error-servidor.html'
})
export class MensajeErrorServidorComponent {
    readonly mensaje = input<string | null | undefined>(null);
}
