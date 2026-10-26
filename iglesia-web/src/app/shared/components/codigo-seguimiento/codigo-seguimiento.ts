import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, input, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';

export type EstadoCopia = 'inicial' | 'copiado' | 'fallido';

/** Tiempo que se muestra «Copiado» (o el fallo) antes de volver al botón «Copiar». */
export const DURACION_AVISO_COPIA_MS = 2000;

/** Texto que se anuncia a los lectores de pantalla tras intentar copiar. */
const ANUNCIOS: Readonly<Record<EstadoCopia, string>> = {
    inicial: '',
    copiado: 'Código copiado al portapapeles',
    fallido: 'No se pudo copiar. Selecciona el código y cópialo a mano.'
};

/** Muestra el código de seguimiento de un error y permite copiarlo para reportarlo a soporte. */
@Component({
    selector: 'app-codigo-seguimiento',
    imports: [ButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './codigo-seguimiento.html',
    host: { class: 'flex flex-wrap items-center gap-2' }
})
export class CodigoSeguimientoComponent {
    readonly codigo = input.required<string>();

    protected readonly estado = signal<EstadoCopia>('inicial');
    protected readonly copiado = computed(() => this.estado() === 'copiado');
    protected readonly anuncio = computed(() => ANUNCIOS[this.estado()]);

    private temporizador?: ReturnType<typeof setTimeout>;

    constructor() {
        inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizador));
    }

    protected async copiar(): Promise<void> {
        try {
            await navigator.clipboard.writeText(this.codigo());
            this.mostrar('copiado');
        } catch {
            // Sin permiso o fuera de un contexto seguro: el código sigue visible para copiarlo a mano.
            this.mostrar('fallido');
        }
    }

    private mostrar(estado: EstadoCopia): void {
        this.estado.set(estado);
        clearTimeout(this.temporizador);
        this.temporizador = setTimeout(() => this.estado.set('inicial'), DURACION_AVISO_COPIA_MS);
    }
}
