import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { PreferenciaModoOscuroService } from '@/app/core/apariencia/services/preferencia-modo-oscuro.service';

/**
 * Botón flotante de las pantallas de acceso. Solo cambia entre modo claro y oscuro: los colores los define el
 * administrador de cada iglesia en «Mi iglesia».
 */
@Component({
    selector: 'app-floating-configurator',
    imports: [ButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './app.floatingconfigurator.html'
})
export class AppFloatingConfigurator {
    private readonly modoOscuro = inject(PreferenciaModoOscuroService);

    readonly float = input<boolean>(true);

    protected readonly oscuro = this.modoOscuro.oscuro;

    protected alternar(): void {
        this.modoOscuro.alternar();
    }
}
