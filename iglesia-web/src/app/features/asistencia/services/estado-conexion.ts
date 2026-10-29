import { DestroyRef, DOCUMENT, inject, Injectable, signal } from '@angular/core';

/** Si el navegador tiene conexión (eventos `online`/`offline`). Se provee por página. */
@Injectable()
export class EstadoConexion {
    private readonly ventana = inject(DOCUMENT).defaultView;
    private readonly estado = signal(this.ventana?.navigator.onLine ?? true);
    readonly enLinea = this.estado.asReadonly();

    constructor() {
        const conectado = (): void => this.estado.set(true);
        const desconectado = (): void => this.estado.set(false);
        this.ventana?.addEventListener('online', conectado);
        this.ventana?.addEventListener('offline', desconectado);
        inject(DestroyRef).onDestroy(() => {
            this.ventana?.removeEventListener('online', conectado);
            this.ventana?.removeEventListener('offline', desconectado);
        });
    }
}
