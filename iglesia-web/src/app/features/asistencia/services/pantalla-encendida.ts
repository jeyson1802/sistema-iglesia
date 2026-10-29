import { DestroyRef, DOCUMENT, inject, Injectable } from '@angular/core';

/**
 * Mantiene la pantalla encendida mientras la página está abierta (Screen Wake Lock). El navegador lo suelta al
 * ocultar la pestaña, así que se vuelve a pedir al regresar. Sin soporte, no hace nada. Se provee por página.
 */
@Injectable()
export class PantallaEncendida {
    private readonly documento = inject(DOCUMENT);
    private bloqueo: WakeLockSentinel | null = null;
    private activa = false;
    private readonly alVolver = (): void => {
        if (this.activa && this.documento.visibilityState === 'visible') {
            void this.pedir();
        }
    };

    constructor() {
        inject(DestroyRef).onDestroy(() => this.liberar());
    }

    mantener(): void {
        this.activa = true;
        this.documento.addEventListener('visibilitychange', this.alVolver);
        void this.pedir();
    }

    liberar(): void {
        this.activa = false;
        this.documento.removeEventListener('visibilitychange', this.alVolver);
        void this.bloqueo?.release().catch(() => undefined);
        this.bloqueo = null;
    }

    private async pedir(): Promise<void> {
        try {
            this.bloqueo = (await this.documento.defaultView?.navigator.wakeLock?.request('screen')) ?? null;
        } catch {
            // Batería baja o sin permiso: la pantalla puede apagarse, pero la página sigue funcionando.
        }
    }
}
