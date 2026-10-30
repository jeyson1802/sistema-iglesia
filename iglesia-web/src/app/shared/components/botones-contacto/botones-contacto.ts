import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { enlaceLlamada, enlaceWhatsapp, tieneTelefono } from '@/app/shared/utils/telefono';

/** Medio con que se abrió el contacto desde los botones. */
export type MedioBoton = 'LLAMADA' | 'WHATSAPP';

/**
 * Botones «Llamar» y «WhatsApp» (presentacional) con el teléfono normalizado (`tel:` y `https://wa.me/…`). Son enlaces
 * reales, para que el celular abra su aplicación; además avisan qué medio se tocó. Sin teléfono, muestran el aviso
 * para coordinar una visita.
 */
@Component({
    selector: 'app-botones-contacto',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './botones-contacto.html'
})
export class BotonesContactoComponent {
    readonly telefono = input<string | null>(null);
    /** Nombre de la persona, para las etiquetas accesibles. */
    readonly nombre = input.required<string>();
    /** Botones a todo el ancho (barra fija del móvil). */
    readonly anchos = input(false);

    readonly contactar = output<MedioBoton>();

    protected readonly disponible = computed(() => tieneTelefono(this.telefono()));
    protected readonly llamada = computed(() => enlaceLlamada(this.telefono()));
    protected readonly whatsapp = computed(() => enlaceWhatsapp(this.telefono()));
}
