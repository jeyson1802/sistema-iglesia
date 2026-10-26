import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AbstractControl } from '@angular/forms';
import { map, startWith, switchMap } from 'rxjs';
import { MessageModule } from 'primeng/message';

/** Mensaje por clave de error de validación; el orden de las claves define la prioridad. */
export type MensajesError = Readonly<Record<string, string>>;

/**
 * Muestra bajo un campo el mensaje del primer error de validación (según el orden de `mensajes`),
 * solo después de que el usuario tocó el campo. Reserva su espacio para que el formulario no salte.
 */
@Component({
    selector: 'app-mensaje-error-campo',
    imports: [MessageModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mensaje-error-campo.html',
    host: { class: 'block mt-1', '[class.min-h-6]': 'reservarEspacio()' }
})
export class MensajeErrorCampoComponent {
    readonly control = input.required<AbstractControl>();
    readonly mensajes = input.required<MensajesError>();
    /** Desactivar cuando otro mensaje ya reserva el espacio bajo el mismo campo. */
    readonly reservarEspacio = input(true);

    /** Los controles no son signals: se escuchan sus eventos (valor, estado y tocado) para reaccionar en una app zoneless. */
    private readonly estado = toSignal(
        toObservable(this.control).pipe(
            switchMap((control) =>
                control.events.pipe(
                    startWith(null),
                    map(() => ({ tocado: control.touched, errores: control.errors }))
                )
            )
        )
    );

    protected readonly mensaje = computed(() => {
        const estado = this.estado();
        if (!estado?.tocado || !estado.errores) {
            return null;
        }
        const errores = estado.errores;
        const clave = Object.keys(this.mensajes()).find((nombre) => nombre in errores);
        return clave ? this.mensajes()[clave] : null;
    });
}
