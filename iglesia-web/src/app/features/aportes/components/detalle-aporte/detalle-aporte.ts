import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';
import { MessageModule } from 'primeng/message';
import { TagModule } from 'primeng/tag';
import { MonedaPipe } from '@/app/shared/pipes/moneda.pipe';
import { Aporte, DESCRIPCION_MEDIO } from '../../models/aporte';

/** Panel lateral con todos los datos de un aporte, la petición destacada y la auditoría (presentacional). */
@Component({
    selector: 'app-detalle-aporte',
    imports: [DatePipe, ButtonModule, DrawerModule, MessageModule, TagModule, MonedaPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './detalle-aporte.html'
})
export class DetalleAporteComponent {
    readonly visible = model(false);
    readonly aporte = input<Aporte | null>(null);
    readonly moneda = input.required<string>();
    readonly puedeEditar = input(false);
    readonly puedeAnular = input(false);

    readonly editar = output<Aporte>();
    readonly anular = output<Aporte>();

    protected readonly medio = computed(() => {
        const aporte = this.aporte();
        return aporte ? DESCRIPCION_MEDIO[aporte.medio] : null;
    });
}
