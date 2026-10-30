import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AccordionModule } from 'primeng/accordion';
import { TagModule } from 'primeng/tag';
import { fechaHoraCompleta, Iteracion, mesAnio, resumenContactos } from '../../models/consolidacion';
import { LineaTiempoComponent } from '../linea-tiempo/linea-tiempo';

/**
 * Iteraciones anteriores de una persona en un acordeón (presentacional), de la más reciente a la más antigua: ronda,
 * fechas, consolidador, contactos por medio, resultado y observación de cierre; al abrirla, sus contactos.
 */
@Component({
    selector: 'app-iteraciones-anteriores',
    imports: [AccordionModule, TagModule, LineaTiempoComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './iteraciones-anteriores.html'
})
export class IteracionesAnterioresComponent {
    readonly iteraciones = input.required<readonly Iteracion[]>();

    protected readonly resumen = resumenContactos;
    protected readonly mes = mesAnio;
    protected readonly completa = fechaHoraCompleta;
}
