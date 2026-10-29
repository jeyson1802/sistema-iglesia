import { inject, Injectable } from '@angular/core';
import { LayoutService } from '@/app/layout/service/layout.service';
import { guardarLocal, leerLocal } from '@/app/shared/utils/almacen-local';

export const CLAVE_MODO_OSCURO = 'apariencia.modoOscuro';
const CONSULTA_MODO_OSCURO = '(prefers-color-scheme: dark)';

/**
 * Modo claro u oscuro de cada persona: se recuerda en su navegador y no se guarda en la base. Sin preferencia
 * guardada sigue la del sistema operativo.
 */
@Injectable({ providedIn: 'root' })
export class PreferenciaModoOscuroService {
    private readonly layout = inject(LayoutService);

    readonly oscuro = this.layout.isDarkTheme;

    /** Aplica la preferencia al arrancar, antes de pintar la primera pantalla. */
    aplicarInicial(): void {
        const guardada = leerLocal(CLAVE_MODO_OSCURO);
        const oscuro = guardada === null ? this.sistemaPrefiereOscuro() : guardada === 'true';
        this.layout.layoutConfig.update((config) => ({ ...config, darkTheme: oscuro }));
        // El efecto del LayoutService ignora su primera ejecución: la clase se pone aquí directamente.
        this.layout.toggleDarkMode(this.layout.layoutConfig());
    }

    alternar(): void {
        const oscuro = !this.layout.isDarkTheme();
        this.layout.layoutConfig.update((config) => ({ ...config, darkTheme: oscuro }));
        guardarLocal(CLAVE_MODO_OSCURO, String(oscuro));
    }

    private sistemaPrefiereOscuro(): boolean {
        return globalThis.matchMedia?.(CONSULTA_MODO_OSCURO).matches ?? false;
    }
}
